"use strict";

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentCreated, onDocumentWritten } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();
const region = "us-central1";
const secureCallable = {
  region,
  enforceAppCheck: true,
};

function requireAuth(request) {
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError("unauthenticated", "Sign in is required.");
  }
  return request.auth.uid;
}

async function getUser(uid) {
  const snap = await db.collection("users").doc(uid).get();
  if (!snap.exists) {
    throw new HttpsError("not-found", "User profile was not found.");
  }
  return snap;
}

async function requireAdmin(request) {
  const callerUid = requireAuth(request);
  const callerSnap = await getUser(callerUid);
  if (callerSnap.get("isAdmin") !== true) {
    throw new HttpsError("permission-denied", "Admin privileges are required.");
  }
  return callerUid;
}

async function requireAdminOrAssignedWali(request, targetUid) {
  const callerUid = requireAuth(request);
  const callerSnap = await getUser(callerUid);
  if (callerSnap.get("isAdmin") === true) {
    return callerUid;
  }
  const targetSnap = await getUser(targetUid);
  const callerEmail = String(request.auth.token.email || "").toLowerCase();
  const targetGuardianEmail = String(targetSnap.get("guardianEmail") || "").toLowerCase();
  const isAssignedWali =
    callerSnap.get("isWaliAccount") === true &&
    callerSnap.get("wardUid") === targetUid &&
    callerEmail &&
    callerEmail === targetGuardianEmail;
  if (!isAssignedWali) {
    throw new HttpsError("permission-denied", "Assigned wali or admin privileges are required.");
  }
  return callerUid;
}

function requireString(data, field) {
  const value = data[field];
  if (typeof value !== "string" || value.trim().length === 0) {
    throw new HttpsError("invalid-argument", `${field} must be a non-empty string.`);
  }
  return value.trim();
}

function requireBoolean(data, field) {
  const value = data[field];
  if (typeof value !== "boolean") {
    throw new HttpsError("invalid-argument", `${field} must be a boolean.`);
  }
  return value;
}

function auditPayload(request, action, targetUid, extra = {}) {
  return {
    action,
    targetUid,
    actorUid: request.auth.uid,
    actorEmail: request.auth.token.email || null,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    ...extra,
  };
}

exports.setVerificationStatus = onCall(secureCallable, async (request) => {
  const targetUid = requireString(request.data, "targetUid");
  const status = requireString(request.data, "status").toUpperCase();
  if (!["NONE", "PENDING", "VERIFIED"].includes(status)) {
    throw new HttpsError("invalid-argument", "Unsupported verification status.");
  }

  await requireAdminOrAssignedWali(request, targetUid);
  await db.collection("users").doc(targetUid).update({
    verificationStatus: status,
  });
  await db.collection("adminAuditLogs").add(auditPayload(request, "setVerificationStatus", targetUid, { status }));
  return { ok: true };
});

exports.setUserPremium = onCall(secureCallable, async (request) => {
  await requireAdmin(request);
  const targetUid = requireString(request.data, "targetUid");
  const isPremium = requireBoolean(request.data, "isPremium");
  const plan = requireString(request.data, "plan").toUpperCase();
  if (!["FREE", "GOLD", "PLATINUM"].includes(plan)) {
    throw new HttpsError("invalid-argument", "Unsupported subscription plan.");
  }
  if (!isPremium && plan !== "FREE") {
    throw new HttpsError("invalid-argument", "Free users must use the FREE plan.");
  }

  await db.collection("users").doc(targetUid).update({
    isPremium,
    subscriptionPlan: isPremium ? plan : "FREE",
    premiumExpiry: 0,
  });
  await db.collection("adminAuditLogs").add(auditPayload(request, "setUserPremium", targetUid, {
    isPremium,
    plan: isPremium ? plan : "FREE",
  }));
  return { ok: true };
});

exports.setUserRole = onCall(secureCallable, async (request) => {
  await requireAdmin(request);
  const targetUid = requireString(request.data, "targetUid");
  const isWali = requireBoolean(request.data, "isWali");
  const isAdmin = requireBoolean(request.data, "isAdmin");

  await db.collection("users").doc(targetUid).update({
    isWaliAccount: isWali,
    isAdmin,
  });
  await db.collection("adminAuditLogs").add(auditPayload(request, "setUserRole", targetUid, {
    isWali,
    isAdmin,
  }));
  return { ok: true };
});

exports.deleteUserProfile = onCall(secureCallable, async (request) => {
  await requireAdmin(request);
  const targetUid = requireString(request.data, "targetUid");
  if (targetUid === request.auth.uid) {
    throw new HttpsError("failed-precondition", "Admins cannot delete their own account here.");
  }

  await db.collection("users").doc(targetUid).delete();
  await db.collection("adminAuditLogs").add(auditPayload(request, "deleteUserProfile", targetUid));
  return { ok: true };
});

async function userName(uid) {
  try {
    const snap = await db.collection("users").doc(uid).get();
    return snap.get("name") || "Mithaq member";
  } catch (error) {
    return "Mithaq member";
  }
}

async function createNotification({ senderUid, recipientUid, title, body, type = "general" }) {
  if (!senderUid || !recipientUid || senderUid === recipientUid) {
    return;
  }
  const notificationRef = await db.collection("notifications").add({
    senderUid,
    recipientUid,
    title,
    body,
    type,
    status: "PENDING",
    timestamp: admin.firestore.FieldValue.serverTimestamp(),
  });

  try {
    const recipientSnap = await db.collection("users").doc(recipientUid).get();
    const token = recipientSnap.exists ? recipientSnap.get("fcmToken") : null;
    if (typeof token !== "string" || token.trim().length === 0) {
      return;
    }

    await admin.messaging().send({
      token,
      notification: { title, body },
      data: {
        notificationId: notificationRef.id,
        senderUid,
        recipientUid,
        type,
        title,
        body,
      },
      android: {
        priority: "high",
        notification: {
          channelId: "mithaq_urgent_channel_v1",
          sound: "default",
        },
      },
    });

    await notificationRef.update({
      status: "PUSH_SENT",
      pushedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  } catch (error) {
    console.error("Failed to send FCM notification", {
      notificationId: notificationRef.id,
      recipientUid,
      error: error && error.message ? error.message : error,
    });
  }
}

exports.onLikeCreated = onDocumentCreated(
  { document: "likes/{likeId}", region },
  async (event) => {
    const like = event.data && event.data.data();
    if (!like || !like.fromUid || !like.toUid || like.fromUid === like.toUid) {
      return;
    }

    const inverseId = `${like.toUid}_${like.fromUid}`;
    const inverseRef = db.collection("likes").doc(inverseId);
    const currentRef = event.data.ref;
    const inverseSnap = await inverseRef.get();
    const isMutual = inverseSnap.exists && inverseSnap.get("fromUid") === like.toUid && inverseSnap.get("toUid") === like.fromUid;

    if (isMutual) {
      await db.runTransaction(async (transaction) => {
        const freshInverse = await transaction.get(inverseRef);
        const freshCurrent = await transaction.get(currentRef);
        if (!freshInverse.exists || !freshCurrent.exists) {
          return;
        }
        transaction.update(currentRef, { isMutual: true });
        transaction.update(inverseRef, { isMutual: true });
      });

      await createNotification({
        senderUid: like.fromUid,
        recipientUid: like.toUid,
        title: "Mithaq - Mutual interest",
        body: "You have a new mutual interest.",
        type: "mutual_like",
      });
      await createNotification({
        senderUid: like.toUid,
        recipientUid: like.fromUid,
        title: "Mithaq - Mutual interest",
        body: "You have a new mutual interest.",
        type: "mutual_like",
      });
      return;
    }

    await createNotification({
      senderUid: like.fromUid,
      recipientUid: like.toUid,
      title: "Mithaq - New interest",
      body: "Someone liked your profile.",
      type: "like",
    });
  }
);

exports.onChatMessageCreated = onDocumentCreated(
  { document: "chatRooms/{roomId}/messages/{messageId}", region },
  async (event) => {
    const message = event.data && event.data.data();
    if (!message || !message.senderId) {
      return;
    }
    const roomSnap = await db.collection("chatRooms").doc(event.params.roomId).get();
    const memberIds = roomSnap.exists ? roomSnap.get("memberIds") || [] : event.params.roomId.split("_");
    const recipientUid = memberIds.find((uid) => uid !== message.senderId);
    const senderName = await userName(message.senderId);
    const content = typeof message.content === "string" ? message.content.trim().slice(0, 80) : "";
    await createNotification({
      senderUid: message.senderId,
      recipientUid,
      title: "Mithaq - New message",
      body: "You have a new message.",
      type: "chat_message",
    });
  }
);

// ── Public discovery mirror (server-owned) ───────────────────────────────────
// publicProfiles is owned by the server. On every write to profiles/{userId}, mirror
// a sanitized, allow-listed subset to publicProfiles/{userId}. Sensitive data (income,
// health, fertility, weight, raw prayer logs, guardian contacts, private photo URLs,
// chat data, reports, blocks) is NEVER copied. Clients are denied direct writes by rules.
function humanizeLabel(value) {
  return String(value || "")
    .trim()
    .replace(/-/g, "_")
    .split("_")
    .filter(Boolean)
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase())
    .join(" ");
}

function buildPublicProfile(userId, profile, isEmailVerified) {
  const basicInfo = profile.basicInfo || {};
  const location = profile.location || {};
  const personalStatus = profile.personalStatus || {};
  const marriageIntent = profile.marriageIntent || {};
  // Phase 11.12A writes a dedicated public display name; fall back to the legacy first-name-only
  // value for older profile documents. (basicInfo.firstName stays private and is never mirrored.)
  const legacyFirstName = String(basicInfo.name || "").trim().split(/\s+/)[0] || "";
  const displayName = String(basicInfo.displayName || legacyFirstName || "").trim().slice(0, 30);
  const ageRaw = basicInfo.age;
  const age = typeof ageRaw === "number" ? ageRaw : (parseInt(ageRaw, 10) || null);
  // 11.12A moved location into its own group and marital status into marriageIntent.
  // Keep legacy fallbacks so both old and new profile shapes mirror correctly.
  const city = String(location.city || basicInfo.city || "").trim();
  const country = humanizeLabel(location.country || basicInfo.country);
  const maritalStatus = humanizeLabel(marriageIntent.maritalStatus || personalStatus.maritalStatus);
  return {
    userId,
    displayName,
    age,
    city,
    country,
    accountType: humanizeLabel(basicInfo.accountType),
    maritalStatus,
    marriageTimeline: humanizeLabel(marriageIntent.timeline),
    prayerHabitPublicLabel: "Not shared",
    prayerRoutineShared: false,
    localTimeEnabled: false,
    hasGuardian: false,
    isEmailVerified: !!isEmailVerified,
    isIdentityVerified: false,
    photoPrivacyMode: "blurred_by_default",
    profileCompletionPercent:
      typeof profile.profileCompletionPercent === "number" ? profile.profileCompletionPercent : 0,
    lastActiveAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  };
}

exports.mirrorPublicProfile = onDocumentWritten(
  { document: "profiles/{userId}", region },
  async (event) => {
    const userId = event.params.userId;
    const after = event.data && event.data.after;

    // Profile deleted -> remove the public mirror.
    if (!after || !after.exists) {
      await db.collection("publicProfiles").doc(userId).delete().catch(() => {});
      return;
    }

    const profile = after.data() || {};

    let isEmailVerified = false;
    try {
      const userRecord = await admin.auth().getUser(userId);
      isEmailVerified = !!userRecord.emailVerified;
    } catch (error) {
      isEmailVerified = false;
    }

    const publicData = buildPublicProfile(userId, profile, isEmailVerified);
    await db.collection("publicProfiles").doc(userId).set(publicData, { merge: true });
  }
);
