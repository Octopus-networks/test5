"use strict";

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentCreated, onDocumentWritten } = require("firebase-functions/v2/firestore");
// firebase-admin v14 dropped the legacy namespaced API (admin.firestore() etc.);
// only the modular entry points exist now.
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getAuth } = require("firebase-admin/auth");
const { getMessaging } = require("firebase-admin/messaging");
const { getStorage } = require("firebase-admin/storage");

initializeApp();

const db = getFirestore();
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
    request.auth.token.email_verified === true &&
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
    createdAt: FieldValue.serverTimestamp(),
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

// Accepted interest is the prerequisite for a chat request, in either direction.
async function acceptedInterestRequestIdBetween(uidA, uidB) {
  const sentId = `${uidA}_${uidB}`;
  const sent = await db.collection("interestRequests").doc(sentId).get();
  if (sent.exists && sent.get("status") === "accepted") {
    return sentId;
  }
  const receivedId = `${uidB}_${uidA}`;
  const received = await db.collection("interestRequests").doc(receivedId).get();
  if (received.exists && received.get("status") === "accepted") {
    return receivedId;
  }
  return null;
}

// Server-authoritative free-tier daily chat-initiation limit. This callable both checks
// the quota AND creates the chatRequests document in one transaction - Firestore rules
// deny client creates, so the cap cannot be bypassed by writing chatRequests directly,
// and a failed create can no longer consume an attempt. Premium users are unlimited;
// free users get FREE_DAILY_CHAT_LIMIT per UTC day (counter at
// users/{uid}/chatLimits/{yyyy-mm-dd}, Admin-SDK-only so it cannot be forged).
const FREE_DAILY_CHAT_LIMIT = 3;
exports.recordChatInitiation = onCall(secureCallable, async (request) => {
  const uid = requireAuth(request);
  const toUserId = requireString(request.data || {}, "toUserId");
  if (toUserId === uid) {
    throw new HttpsError("invalid-argument", "Cannot request a chat with yourself.");
  }
  const userSnap = await getUser(uid);
  const isPremium = userSnap.get("isPremium") === true;

  const relatedInterestRequestId = await acceptedInterestRequestIdBetween(uid, toUserId);
  if (!relatedInterestRequestId) {
    throw new HttpsError(
      "failed-precondition",
      "Accepted interest is required before requesting a chat.",
    );
  }

  // UTC day key so the cap cannot be gamed by changing the device clock/timezone.
  const dateKey = new Date().toISOString().slice(0, 10);
  const limitRef = db.collection("users").doc(uid).collection("chatLimits").doc(dateKey);
  const requestRef = db.collection("chatRequests").doc(`${uid}_${toUserId}`);

  const outcome = await db.runTransaction(async (tx) => {
    const existing = await tx.get(requestRef);
    if (existing.exists && existing.get("status") === "pending") {
      // Re-sending a pending request must not burn quota.
      return { alreadyPending: true, remaining: -1 };
    }

    let remaining = -1;
    if (!isPremium) {
      const snap = await tx.get(limitRef);
      const current = Number(snap.get("count")) || 0;
      if (current >= FREE_DAILY_CHAT_LIMIT) {
        throw new HttpsError(
          "resource-exhausted",
          "Daily chat limit reached. Upgrade to Premium for unlimited chats.",
        );
      }
      tx.set(limitRef, {
        count: current + 1,
        updatedAt: FieldValue.serverTimestamp(),
      }, { merge: true });
      remaining = FREE_DAILY_CHAT_LIMIT - (current + 1);
    }

    tx.set(requestRef, {
      requestId: requestRef.id,
      fromUserId: uid,
      toUserId,
      status: "pending",
      relatedInterestRequestId,
      requiresGuardianApproval: false,
      guardianApprovalStatus: "not_required",
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
    return { alreadyPending: false, remaining };
  });

  return {
    allowed: true,
    requestId: requestRef.id,
    alreadyPending: outcome.alreadyPending,
    remaining: outcome.remaining,
    isPremium,
  };
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

// Heavy accounts (many photos/subcollections) can exceed the default 60s budget.
exports.deleteUserProfile = onCall({ ...secureCallable, timeoutSeconds: 300 }, async (request) => {
  await requireAdmin(request);
  const targetUid = requireString(request.data, "targetUid");
  if (targetUid === request.auth.uid) {
    throw new HttpsError("failed-precondition", "Admins cannot delete their own account here.");
  }

  // Auth first: kills sign-in immediately so the client cannot recreate data while
  // the cleanup below runs. A missing Auth user (already deleted) is fine.
  try {
    await getAuth().deleteUser(targetUid);
  } catch (error) {
    if (error.code !== "auth/user-not-found") {
      throw error;
    }
  }

  // Identity documents, including subcollections (users/{uid}/chatLimits,
  // userPhotos/{uid}/photos). Deleting users/{uid} also triggers the public-profile
  // mirror, which is idempotent with the direct publicProfiles delete below.
  await db.recursiveDelete(db.collection("users").doc(targetUid));
  await db.recursiveDelete(db.collection("profiles").doc(targetUid));
  await db.recursiveDelete(db.collection("userPhotos").doc(targetUid));
  await db.collection("publicProfiles").doc(targetUid).delete();

  // Stored media: private photos, legacy profile images ({uid}.jpg and
  // {uid}_additional_N.jpg share the uid prefix), voice intro, verification evidence.
  const bucket = getStorage().bucket();
  await Promise.all([
    bucket.deleteFiles({ prefix: `user_photos/${targetUid}/` }),
    bucket.deleteFiles({ prefix: `profiles/${targetUid}` }),
    bucket.deleteFiles({ prefix: `voices/${targetUid}` }),
    bucket.deleteFiles({ prefix: `verification/${targetUid}/` }),
  ]);

  await db.collection("adminAuditLogs").add(auditPayload(request, "deleteUserProfile", targetUid));
  return { ok: true };
});

// FCM error codes that mean a token is permanently dead and should be removed.
const INVALID_TOKEN_CODES = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
  "messaging/invalid-argument",
]);

// FCM data payload values must be strings. Coerce + drop null/undefined.
function sanitizeData(data) {
  const out = {};
  Object.keys(data || {}).forEach((key) => {
    const value = data[key];
    if (value === undefined || value === null) {
      return;
    }
    out[key] = typeof value === "string" ? value : String(value);
  });
  return out;
}

// Respect blocks in either direction. Best-effort: a read failure here must not
// silently drop every notification, so we fail open (treat as not blocked).
async function isBlockedBetween(uidA, uidB) {
  if (!uidA || !uidB) {
    return false;
  }
  try {
    const [forward, backward] = await Promise.all([
      db.collection("blocks").doc(`${uidA}_${uidB}`).get(),
      db.collection("blocks").doc(`${uidB}_${uidA}`).get(),
    ]);
    return forward.exists || backward.exists;
  } catch (error) {
    console.error("Block lookup failed", { error: error && error.message ? error.message : error });
    return false;
  }
}

// Collect the recipient's push targets. Phase 13A stores per-device tokens under
// users/{uid}/fcmTokens/{tokenId}; a token is active unless explicitly active === false
// (Phase 13A docs have no active field and are therefore treated as active). The legacy
// flat users/{uid}.fcmToken is used only as a fallback when no subcollection token exists.
// Each target carries a `ref` (subcollection doc) or `legacy` flag so dead tokens can be
// cleaned up later. Token values are never logged.
async function collectRecipientTokens(recipientUid) {
  const targets = [];
  const seen = new Set();

  const tokensSnap = await db.collection("users").doc(recipientUid).collection("fcmTokens").get();
  tokensSnap.forEach((doc) => {
    const data = doc.data() || {};
    const token = typeof data.token === "string" ? data.token.trim() : "";
    const isActive = data.active !== false;
    if (token && isActive && !seen.has(token)) {
      seen.add(token);
      targets.push({ token, ref: doc.ref });
    }
  });

  if (targets.length === 0) {
    const userSnap = await db.collection("users").doc(recipientUid).get();
    const legacy = userSnap.exists ? userSnap.get("fcmToken") : null;
    if (typeof legacy === "string" && legacy.trim().length > 0) {
      targets.push({ token: legacy.trim(), ref: null, legacy: true });
    }
  }
  return targets;
}

// Fan a push out to every active device of a recipient and prune dead tokens.
async function sendPushToRecipient(recipientUid, { title, body, data }) {
  const targets = await collectRecipientTokens(recipientUid);
  if (targets.length === 0) {
    return { successCount: 0, failureCount: 0, attempted: 0 };
  }

  const response = await getMessaging().sendEachForMulticast({
    tokens: targets.map((target) => target.token),
    notification: { title, body },
    data: sanitizeData(data),
    android: {
      priority: "high",
      notification: {
        channelId: "mithaq_urgent_channel_v1",
        sound: "default",
      },
    },
  });

  // Remove tokens FCM reports as permanently invalid. Never log the token itself.
  const cleanups = [];
  response.responses.forEach((resp, index) => {
    if (resp.success) {
      return;
    }
    const code = resp.error && resp.error.code;
    if (!INVALID_TOKEN_CODES.has(code)) {
      return;
    }
    const target = targets[index];
    if (target.ref) {
      cleanups.push(target.ref.delete().catch(() => {}));
    } else if (target.legacy) {
      cleanups.push(
        db.collection("users").doc(recipientUid)
          .update({ fcmToken: FieldValue.delete() })
          .catch(() => {})
      );
    }
  });
  if (cleanups.length > 0) {
    await Promise.all(cleanups);
  }

  return {
    successCount: response.successCount,
    failureCount: response.failureCount,
    attempted: targets.length,
  };
}

// Phase 13C: map a notification type to its per-category preference flag. Types without
// an entry (e.g. like / mutual_like) are governed only by the master notificationsEnabled.
const TYPE_TO_PREF = {
  interest_request: "interestRequestNotifications",
  photo_request: "photoRequestNotifications",
  chat_request: "chatRequestNotifications",
  chat_message: "messageNotifications",
  photo_approved: "photoModerationNotifications",
  photo_rejected: "photoModerationNotifications",
};

// Respect the recipient's notification settings (users/{uid}/notificationSettings/preferences).
// Safe-by-default: a missing document, missing field, or read error all mean "allowed", so a
// user is never silently muted by an error. The master notificationsEnabled gates everything.
async function notificationsAllowed(recipientUid, type) {
  try {
    const snap = await db.collection("users").doc(recipientUid)
      .collection("notificationSettings").doc("preferences").get();
    if (!snap.exists) {
      return true;
    }
    const data = snap.data() || {};
    if (data.notificationsEnabled === false) {
      return false;
    }
    const prefKey = TYPE_TO_PREF[type];
    if (prefKey && data[prefKey] === false) {
      return false;
    }
    return true;
  } catch (error) {
    console.error("Notification settings lookup failed", {
      recipientUid,
      error: error && error.message ? error.message : error,
    });
    return true;
  }
}

// Writes a notifications/{id} record (server-owned; the Android WorkManager fallback
// reads PENDING ones) and pushes to the recipient's devices. `senderUid` is optional —
// omit it for system notifications (e.g. photo moderation). Notification bodies must never
// contain sensitive/private profile data or message content.
async function createNotification({ senderUid = null, recipientUid, title, body, type = "general", extraData = {} }) {
  if (!recipientUid) {
    return;
  }
  if (senderUid && senderUid === recipientUid) {
    return;
  }
  if (senderUid && (await isBlockedBetween(senderUid, recipientUid))) {
    return;
  }
  if (!(await notificationsAllowed(recipientUid, type))) {
    return;
  }

  const notificationRef = await db.collection("notifications").add({
    senderUid: senderUid || "system",
    recipientUid,
    title,
    body,
    type,
    status: "PENDING",
    timestamp: FieldValue.serverTimestamp(),
  });

  try {
    const result = await sendPushToRecipient(recipientUid, {
      title,
      body,
      data: {
        notificationId: notificationRef.id,
        senderUid: senderUid || "system",
        recipientUid,
        type,
        title,
        body,
        ...extraData,
      },
    });

    if (result.successCount > 0) {
      await notificationRef.update({
        status: "PUSH_SENT",
        pushedAt: FieldValue.serverTimestamp(),
      });
    }
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
    // Notification body is intentionally generic — message content is never surfaced in
    // push payloads for privacy.
    await createNotification({
      senderUid: message.senderId,
      recipientUid,
      title: "Mithaq - New message",
      body: "You have a new message.",
      type: "chat_message",
    });
  }
);

// ── Phase 13B: notification triggers for core Mithaq events ──────────────────
// Each trigger reacts to a server-visible document write and pushes a privacy-safe,
// generic notification via createNotification (active-token fan-out + block-respect).
// Titles/bodies never contain message content or sensitive profile data.

exports.onInterestRequestCreated = onDocumentCreated(
  { document: "interestRequests/{requestId}", region },
  async (event) => {
    const data = event.data && event.data.data();
    if (!data || data.status !== "pending") {
      return;
    }
    await createNotification({
      senderUid: data.fromUserId,
      recipientUid: data.toUserId,
      title: "Mithaq",
      body: "You have a new interest request.",
      type: "interest_request",
      extraData: { requestId: event.params.requestId },
    });
  }
);

exports.onPhotoRequestCreated = onDocumentCreated(
  { document: "photoRequests/{requestId}", region },
  async (event) => {
    const data = event.data && event.data.data();
    if (!data || data.status !== "pending") {
      return;
    }
    await createNotification({
      senderUid: data.fromUserId,
      recipientUid: data.toUserId,
      title: "Mithaq",
      body: "You have a new photo access request.",
      type: "photo_request",
      extraData: { requestId: event.params.requestId },
    });
  }
);

exports.onChatRequestCreated = onDocumentCreated(
  { document: "chatRequests/{requestId}", region },
  async (event) => {
    const data = event.data && event.data.data();
    if (!data || data.status !== "pending") {
      return;
    }
    await createNotification({
      senderUid: data.fromUserId,
      recipientUid: data.toUserId,
      title: "Mithaq",
      body: "You have a new chat request.",
      type: "chat_request",
      extraData: { requestId: event.params.requestId },
    });
  }
);

// New message in the Phase 11+ chat system (chats/{chatId}/messages). The legacy
// chatRooms path is handled separately by onChatMessageCreated above. Message text is
// never placed in the push payload — the body stays generic for privacy.
exports.onDirectMessageCreated = onDocumentCreated(
  { document: "chats/{chatId}/messages/{messageId}", region },
  async (event) => {
    const message = event.data && event.data.data();
    if (!message || !message.senderId) {
      return;
    }
    const chatSnap = await db.collection("chats").doc(event.params.chatId).get();
    if (!chatSnap.exists) {
      return;
    }
    const participantIds = chatSnap.get("participantIds") || [];
    const recipientUid = participantIds.find((uid) => uid !== message.senderId);
    if (!recipientUid) {
      return;
    }
    // Type-aware but content-free body (no message text or media is ever surfaced).
    const body = message.type === "image"
      ? "📷 Photo"
      : message.type === "voice"
        ? "🎤 Voice message"
        : "You have a new message.";
    await createNotification({
      senderUid: message.senderId,
      recipientUid,
      title: "Mithaq - New message",
      body,
      type: "chat_message",
      extraData: { chatId: event.params.chatId, messageId: event.params.messageId },
    });
  }
);

// Photo moderation outcome -> notify the photo owner. System notification (no sender).
// The admin rejection reason is intentionally NOT included in the push.
exports.onUserPhotoModerated = onDocumentWritten(
  { document: "userPhotos/{userId}/photos/{photoId}", region },
  async (event) => {
    const before = event.data && event.data.before;
    const after = event.data && event.data.after;
    if (!after || !after.exists) {
      return;
    }
    const beforeStatus = before && before.exists ? before.get("status") : null;
    const afterStatus = after.get("status");
    if (beforeStatus === afterStatus) {
      return;
    }
    if (afterStatus !== "approved" && afterStatus !== "rejected") {
      return;
    }
    const body = afterStatus === "approved"
      ? "Your photo was approved."
      : "Your photo needs attention. Please review it.";
    await createNotification({
      recipientUid: event.params.userId,
      title: "Mithaq - Photo review",
      body,
      type: afterStatus === "approved" ? "photo_approved" : "photo_rejected",
      extraData: { photoId: event.params.photoId, status: afterStatus },
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

function buildPublicProfile(userId, profile, isEmailVerified, userMeta = {}) {
  const basicInfo = profile.basicInfo || {};
  const location = profile.location || {};
  const personalStatus = profile.personalStatus || {};
  const marriageIntent = profile.marriageIntent || {};
  // Per-field privacy: profiles/{uid}.privacyTrust holds the owner's per-field visibility flags
  // (the privacyTrust group is already allow-listed by Firestore rules, so no rules change).
  // Missing/undefined defaults to visible (true) so existing profiles are unaffected.
  const privacy = profile.privacyTrust || {};
  const showAge = privacy.showAge !== false;
  const showLocation = privacy.showLocation !== false;
  const showMaritalStatus = privacy.showMaritalStatus !== false;
  const showMarriageTimeline = privacy.showMarriageTimeline !== false;
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
  // Identity verification and guardian status live on the server-owned users/{uid} doc,
  // not on profiles/{uid}. Mirror them so discovery badges reflect reality. (Both are
  // gated by Cloud Functions / rules; clients can never self-set them.)
  const verificationStatus = String(userMeta.verificationStatus || "NONE").toUpperCase();
  const guardianStatus = String(userMeta.guardianStatus || "NONE").toUpperCase();
  return {
    userId,
    displayName,
    age: showAge ? age : null,
    city: showLocation ? city : "",
    country: showLocation ? country : "",
    accountType: humanizeLabel(basicInfo.accountType),
    maritalStatus: showMaritalStatus ? maritalStatus : "",
    marriageTimeline: showMarriageTimeline ? humanizeLabel(marriageIntent.timeline) : "",
    prayerHabitPublicLabel: "Not shared",
    prayerRoutineShared: false,
    localTimeEnabled: false,
    hasGuardian: guardianStatus == "VERIFIED",
    isEmailVerified: !!isEmailVerified,
    isIdentityVerified: verificationStatus == "VERIFIED",
    // Incognito members must not appear in public discovery. The flag lives on the
    // server-owned users/{uid} doc; mirroring it here lets Discover filter honestly.
    isIncognito: userMeta.isIncognito == true,
    photoPrivacyMode: "blurred_by_default",
    profileCompletionPercent:
      typeof profile.profileCompletionPercent === "number" ? profile.profileCompletionPercent : 0,
    lastActiveAt: FieldValue.serverTimestamp(),
    updatedAt: FieldValue.serverTimestamp(),
  };
}

// Rebuilds publicProfiles/{userId} from the current profiles/{userId} + users/{userId}
// state. Shared by both the profile and user-doc triggers so the discovery mirror stays
// consistent regardless of which source document changed.
async function syncPublicProfile(userId) {
  const profileSnap = await db.collection("profiles").doc(userId).get();
  if (!profileSnap.exists) {
    // No onboarding profile -> no public discovery entry.
    await db.collection("publicProfiles").doc(userId).delete().catch(() => {});
    return;
  }
  const profile = profileSnap.data() || {};

  let isEmailVerified = false;
  try {
    const userRecord = await getAuth().getUser(userId);
    isEmailVerified = !!userRecord.emailVerified;
  } catch (error) {
    isEmailVerified = false;
  }

  let userMeta = {};
  try {
    const userSnap = await db.collection("users").doc(userId).get();
    if (userSnap.exists) {
      userMeta = userSnap.data() || {};
    }
  } catch (error) {
    userMeta = {};
  }

  const publicData = buildPublicProfile(userId, profile, isEmailVerified, userMeta);
  await db.collection("publicProfiles").doc(userId).set(publicData, { merge: true });
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

    await syncPublicProfile(userId);
  }
);

// users/{uid} owns identity verification + guardian status. Re-mirror the public profile
// when those surface-able fields change so discovery badges (isIdentityVerified, hasGuardian)
// don't go stale waiting for the next profiles/{uid} write. Every users write invokes this,
// so we early-return unless a mirrored field actually changed.
exports.mirrorPublicProfileOnUserChange = onDocumentWritten(
  { document: "users/{userId}", region },
  async (event) => {
    const userId = event.params.userId;
    const before = event.data && event.data.before;
    const after = event.data && event.data.after;

    // User doc removed: the profiles trigger handles mirror cleanup on profile deletion.
    if (!after || !after.exists) {
      return;
    }

    const beforeData = before && before.exists ? before.data() || {} : {};
    const afterData = after.data() || {};
    const mirroredFields = ["verificationStatus", "guardianStatus"];
    const changed = mirroredFields.some((key) => beforeData[key] !== afterData[key]);
    if (!changed) {
      return;
    }

    await syncPublicProfile(userId);
  }
);

// ── Mirror onboarding answers (profiles/{uid}) into the matchable UserProfile (users/{uid}) ──
// The onboarding engine writes structured groups to profiles/{uid}, but matching/search read the
// flat UserProfile at users/{uid}. This translates the onboarding option-ids into the UserProfile
// enum/value vocabulary and writes the matchable fields. Display-only fields are copied as-is.
// Writing users/{uid} triggers mirrorPublicProfileOnUserChange (publicProfiles rebuild) — no loop,
// because this function only fires on profiles writes.
const ONBOARDING_VALUE_MAP = {
  gender: { male: "MALE", female: "FEMALE" },
  sect: { sunni: "SUNNI", just_muslim: "OTHER", other: "OTHER" },
  prayerFrequency: { five_daily: "ALWAYS", most: "USUALLY", sometimes: "SOMETIMES", working_on: "SOMETIMES" },
  religiousValues: { very: "very_religious", practicing: "religious", moderate: "religious", learning: "not_religious" },
  familyValue: { traditional: "conservative", balanced: "moderate", modern: "liberal" },
  modestyPreference: { niqab: "NIQAB", hijab: "HIJAB", modest_dress: "HIJAB", beard_sunnah: "NONE" },
  relocationWillingness: { yes: "YES", no: "NO", maybe: "OPEN" },
};

exports.mirrorProfileToUser = onDocumentWritten(
  { document: "profiles/{userId}", region },
  async (event) => {
    const userId = event.params.userId;
    const after = event.data && event.data.after;
    if (!after || !after.exists) return;

    const p = after.data() || {};
    const basic = p.basicInfo || {};
    const loc = p.location || {};
    const rel = p.religiousPractice || {};
    const marr = p.marriageIntent || {};
    const fam = p.family || {};
    const edu = p.educationWork || {};
    const app = p.appearance || {};
    const life = p.lifestyle || {};
    const per = p.personality || {};

    const update = {};
    const str = (k, v) => { if (typeof v === "string" && v.trim() !== "") update[k] = v.trim(); };
    const num = (k, v) => { if (typeof v === "number") update[k] = v; };
    const arr = (k, v) => { if (Array.isArray(v)) update[k] = v; };
    const enumMap = (k, table, v) => { if (v != null && table[v] != null) update[k] = table[v]; };

    // Identity / basic
    str("name", basic.firstName);
    str("username", basic.displayName);
    str("nationality", basic.nationality);
    num("age", basic.age);
    enumMap("gender", ONBOARDING_VALUE_MAP.gender, basic.gender);

    // Location
    str("country", loc.country);
    str("city", loc.city);
    enumMap("relocationWillingness", ONBOARDING_VALUE_MAP.relocationWillingness, loc.willingToRelocate);

    // Religious practice (matching-critical)
    enumMap("sect", ONBOARDING_VALUE_MAP.sect, rel.sect);
    enumMap("prayerFrequency", ONBOARDING_VALUE_MAP.prayerFrequency, rel.prayerHabit);
    enumMap("religiousValues", ONBOARDING_VALUE_MAP.religiousValues, rel.commitment);
    enumMap("familyValue", ONBOARDING_VALUE_MAP.familyValue, rel.familyValues);
    enumMap("modestyPreference", ONBOARDING_VALUE_MAP.modestyPreference, rel.modesty);

    // Marriage / family
    str("maritalStatus", marr.maritalStatus); // single/divorced/widowed already valid
    str("weddingTimeline", marr.timeline);
    if (typeof marr.polygamyStance === "string") update.polygamyAcceptance = marr.polygamyStance === "open";
    num("numberOfChildren", fam.numberOfChildren);

    // Education / work (display)
    str("educationLevel", edu.educationLevel);
    str("occupation", edu.occupation);
    str("employmentStatus", edu.employmentStatus);
    str("incomeLevel", edu.incomeRange);

    // Appearance (display)
    num("height", app.heightCm);
    num("weight", app.weightKg);
    str("bodyType", app.bodyType);

    // Lifestyle (display)
    str("smokeStatus", life.smoking);
    str("livingSituation", life.livingSituation);

    // Personality
    str("aboutYourself", per.idealDay);
    arr("languagesSpoken", per.languagesSpoken);
    arr("interestsEntertainments", per.hobbies);

    // Oath
    const oath = (p.privacyTrust || {}).oathAccepted;
    if (oath === "yes" || oath === true) update.oathChecked = true;

    if (Object.keys(update).length === 0) return;
    await db.collection("users").doc(userId).set(update, { merge: true });
  },
);

// Admin moderation gets teeth: suspending or banning a member disables their Firebase
// Auth account (no new sign-ins) and revokes their refresh tokens (existing sessions
// die at the next ID-token refresh, <= 1 hour). Firestore rules deny their interactive
// writes in the meantime (notSuspendedOrBanned). Setting the status back to
// active/warned re-enables the account.
exports.enforceModerationStatus = onDocumentWritten(
  { document: "userModeration/{userId}", region },
  async (event) => {
    const userId = event.params.userId;
    const after = event.data && event.data.after;
    const status = after && after.exists ? String(after.get("status") || "active") : "active";
    const shouldDisable = status === "suspended" || status === "banned";
    try {
      await getAuth().updateUser(userId, { disabled: shouldDisable });
      if (shouldDisable) {
        await getAuth().revokeRefreshTokens(userId);
      }
    } catch (error) {
      // Moderation records can outlive the Auth account (e.g. after deletion).
      if (error.code === "auth/user-not-found") {
        return;
      }
      throw error;
    }
  },
);
