"use strict";

const SAFE_DEFAULT_PHOTO_PRIVACY_MODE = "blurred_by_default";

function stringValue(value, fallback = "") {
  return typeof value === "string" ? value.trim() : fallback;
}

function booleanValue(value, fallback = false) {
  return typeof value === "boolean" ? value : fallback;
}

function intValue(value, fallback = null) {
  if (typeof value === "number" && Number.isFinite(value)) {
    return Math.trunc(value);
  }
  if (typeof value === "string" && value.trim().length > 0) {
    const parsed = Number.parseInt(value, 10);
    return Number.isFinite(parsed) ? parsed : fallback;
  }
  return fallback;
}

function objectValue(value) {
  return value && typeof value === "object" && !Array.isArray(value) ? value : {};
}

function humanize(value) {
  return stringValue(value)
    .replace(/-/g, "_")
    .split("_")
    .filter((part) => part.trim().length > 0)
    .map((part) => {
      const lower = part.trim().toLowerCase();
      return lower.charAt(0).toUpperCase() + lower.slice(1);
    })
    .join(" ");
}

function sanitizeDisplayName(value) {
  return stringValue(value)
    .split(/\s+/)
    .filter(Boolean)[0]
    ?.slice(0, 30) || "";
}

function safeTimestamp(value, fallback) {
  return value || fallback;
}

function buildPublicProfileFromPrivateProfile(userId, profileData = {}, options = {}) {
  const basicInfo = objectValue(profileData.basicInfo);
  const personalStatus = objectValue(profileData.personalStatus);
  const marriageIntent = objectValue(profileData.marriageIntent);
  const publicSettings = objectValue(profileData.publicSettings);
  const verification = objectValue(profileData.verification);
  const now = options.serverTimestamp;

  return {
    userId,
    displayName: sanitizeDisplayName(basicInfo.name || profileData.displayName),
    age: intValue(basicInfo.age),
    city: stringValue(basicInfo.city),
    country: humanize(basicInfo.country),
    accountType: humanize(basicInfo.accountType),
    maritalStatus: humanize(personalStatus.maritalStatus),
    marriageTimeline: humanize(marriageIntent.timeline),
    prayerHabitPublicLabel: stringValue(publicSettings.prayerHabitPublicLabel, "Not shared"),
    prayerRoutineShared: booleanValue(publicSettings.prayerRoutineShared, false),
    localTimeEnabled: booleanValue(publicSettings.localTimeEnabled, false),
    hasGuardian: booleanValue(publicSettings.hasGuardian, false),
    isEmailVerified: booleanValue(options.isEmailVerified, booleanValue(profileData.isEmailVerified, false)),
    isIdentityVerified: booleanValue(verification.isIdentityVerified, booleanValue(profileData.isIdentityVerified, false)),
    photoPrivacyMode: stringValue(publicSettings.photoPrivacyMode, SAFE_DEFAULT_PHOTO_PRIVACY_MODE) || SAFE_DEFAULT_PHOTO_PRIVACY_MODE,
    profileCompletionPercent: intValue(profileData.profileCompletionPercent, 0) || 0,
    lastActiveAt: safeTimestamp(profileData.lastActiveAt, now),
    updatedAt: now,
  };
}

module.exports = {
  buildPublicProfileFromPrivateProfile,
};
