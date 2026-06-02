#!/usr/bin/env node
/**
 * One-off backfill for the server-owned public discovery mirror.
 *
 * Mirrors every existing profiles/{userId} into publicProfiles/{userId} using the same
 * sanitized allow-list as the mirrorPublicProfile Cloud Function. Run this once after
 * deploying the function and tightening the publicProfiles rules, so members who
 * onboarded before the server mirror existed still appear in Discover/Search.
 *
 * Sensitive data (income, health, fertility, weight, raw prayer logs, guardian contacts,
 * private photo URLs, chat data, reports, blocks) is NEVER copied.
 *
 * Usage (locally, with Admin credentials — never commit the service account):
 *   GOOGLE_APPLICATION_CREDENTIALS=service-account.json \
 *     node scripts/backfill_public_profiles.js
 */
"use strict";

const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

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
  const personalStatus = profile.personalStatus || {};
  const marriageIntent = profile.marriageIntent || {};
  const firstName = String(basicInfo.name || "").trim().split(/\s+/)[0] || "";
  const ageRaw = basicInfo.age;
  const age = typeof ageRaw === "number" ? ageRaw : (parseInt(ageRaw, 10) || null);
  return {
    userId,
    displayName: firstName.slice(0, 30),
    age,
    city: String(basicInfo.city || "").trim(),
    country: humanizeLabel(basicInfo.country),
    accountType: humanizeLabel(basicInfo.accountType),
    maritalStatus: humanizeLabel(personalStatus.maritalStatus),
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

async function main() {
  const snapshot = await db.collection("profiles").get();
  let count = 0;
  for (const doc of snapshot.docs) {
    const userId = doc.id;
    let isEmailVerified = false;
    try {
      isEmailVerified = !!(await admin.auth().getUser(userId)).emailVerified;
    } catch (error) {
      isEmailVerified = false;
    }
    const publicData = buildPublicProfile(userId, doc.data() || {}, isEmailVerified);
    await db.collection("publicProfiles").doc(userId).set(publicData, { merge: true });
    count += 1;
  }
  console.log(`Backfilled ${count} public profile(s).`);
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error("Backfill failed:", error);
    process.exit(1);
  });
