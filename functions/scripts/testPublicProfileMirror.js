"use strict";

const assert = require("node:assert/strict");
const { buildPublicProfileFromPrivateProfile } = require("../publicProfileMirror");

const timestamp = { __serverTimestamp: true };

const privateProfile = {
  profileCompletionPercent: 87,
  basicInfo: {
    name: "Ahmed Example Private Family Name",
    age: "40",
    country: "egypt",
    city: "Cairo",
    accountType: "single_man",
    income: "private",
    weight: 90,
  },
  personalStatus: {
    maritalStatus: "never_married",
    health: "private",
    fertility: "private",
  },
  religiousPractice: {
    exactPrayerLogs: ["fajr"],
  },
  marriageIntent: {
    timeline: "within_1_year",
  },
  guardianEmail: "guardian@example.com",
  privatePhotoUrl: "https://private.example/photo.jpg",
  publicSettings: {
    prayerRoutineShared: true,
    localTimeEnabled: true,
    hasGuardian: true,
    photoPrivacyMode: "approved_users_only",
  },
};

const publicProfile = buildPublicProfileFromPrivateProfile("uid123", privateProfile, {
  isEmailVerified: true,
  serverTimestamp: timestamp,
});

assert.equal(publicProfile.userId, "uid123");
assert.equal(publicProfile.displayName, "Ahmed");
assert.equal(publicProfile.age, 40);
assert.equal(publicProfile.city, "Cairo");
assert.equal(publicProfile.country, "Egypt");
assert.equal(publicProfile.accountType, "Single Man");
assert.equal(publicProfile.maritalStatus, "Never Married");
assert.equal(publicProfile.marriageTimeline, "Within 1 Year");
assert.equal(publicProfile.isEmailVerified, true);
assert.equal(publicProfile.photoPrivacyMode, "approved_users_only");
assert.equal(publicProfile.updatedAt, timestamp);

for (const forbidden of [
  "income",
  "health",
  "fertility",
  "weight",
  "guardianEmail",
  "guardianPhone",
  "privatePhotoUrl",
  "photoUrl",
  "exactPrayerLogs",
  "chatData",
  "blocks",
  "reports",
]) {
  assert.equal(Object.prototype.hasOwnProperty.call(publicProfile, forbidden), false, `${forbidden} leaked`);
}

const missing = buildPublicProfileFromPrivateProfile("uid456", {}, { serverTimestamp: timestamp });
assert.equal(missing.userId, "uid456");
assert.equal(missing.displayName, "");
assert.equal(missing.age, null);
assert.equal(missing.photoPrivacyMode, "blurred_by_default");
assert.equal(missing.profileCompletionPercent, 0);
assert.equal(missing.isEmailVerified, false);

console.log("publicProfileMirror sanitizer tests passed");
