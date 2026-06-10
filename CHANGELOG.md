# Changelog

All notable changes to **Mithaq** are documented here.
Format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow [Semantic Versioning](https://semver.org/).

---

## [Unreleased]

> `v2.1.0` is a real tag at PR #93. The older dated sections remain a documentation
> narrative and may not have matching tags.

### Added
- **Premium Profile Highlight (PRs #94-#95):** premium cards render with a reusable gold border
  across profile surfaces.
- **2x Exposure (PR #96):** Discover uses a deterministic 2:1 premium/free interleave while
  preserving recency within each group.

### Changed
- **Crash reporting (PR #94):** Android `printStackTrace()` calls in production sources now
  report non-fatal exceptions through Firebase Crashlytics.

---

## [2.1.0] - 2026-06-10

### Added
- **Profile hub:** all account-settings cards are wired, including edit profile, per-field
  privacy, photo-access management, prayer/notification settings, language/theme, app lock,
  support, and guardian invite.
- **Per-field public-profile privacy:** age, location, marital status, and marriage timeline
  visibility are stored in `profiles/{uid}.privacyTrust` and honored by `buildPublicProfile`.
- **Photo-access management:** owners can approve, reject, and revoke photo-view requests.
- **Public-profile trust refresh:** `mirrorPublicProfileOnUserChange` re-mirrors discovery data
  when verification, guardian, Incognito, or Premium state changes.
- **Boot recovery and release tooling:** `BootReceiver` restores Adhan/WorkManager scheduling,
  and the `Release APK` workflow publishes testable APK artifacts from `v*` tags.
- **Mandatory unified onboarding (PRs #75, #82-#83):** one 58-step flow now gates entry to the
  app; all sections load from JSON with a consistency check against the static fallback.
- **Onboarding → user mirror (PR #77):** `mirrorProfileToUser` maps structured onboarding
  answers into the matchable `users/{uid}` document.
- **Location-based Adhan (PR #76):** app-open location permission and per-location prayer-time
  scheduling.
- **Premium perks:** Incognito browsing (PR #78), bounded Boost re-ranking (PR #79), and
  premium sender Read receipts (PR #80). Premium remains admin/server granted; billing is OFF.
- **Verified badge (PR #81):** reusable trust badge shown on verified member profiles.

### Changed
- **Likes (PR #74):** the heart action now toggles like / unlike instead of being like-only.
- **Cloud Functions runtime (PRs #84, #90):** Node 22, modular Firebase Admin APIs,
  `firebase-admin ^13.10.0`, and `firebase-functions ^7.2.5`.
- `AdhanReceiver` handles only `ADHAN_ALARM`; reboot recovery belongs to `BootReceiver`.
- The last signed-in UID is persisted so background services can recover after reboot.
- Version bumped to `2.1.0` / versionCode 21 and released as `v2.1.0` (PR #93).

### Security
- Guardian/wali binding requires verified email ownership in rules and Functions.
- Rules for reports, profile views, and favorites use stricter field allow-lists and
  self-target/length validation.
- **Photo-access revocation (PR #85):** revoking approval now removes the document checked by
  Storage rules, immediately cutting off private-photo downloads.
- **Incognito discovery privacy (PR #86):** Incognito members are mirrored and excluded from
  Discover.
- **Bidirectional block enforcement (PR #87):** Firestore and Storage rules enforce blocks on
  protected interactions and media.
- **Real account deletion (PR #88):** admin deletion removes Firebase Auth, Firestore identity
  data/subcollections, Storage media, and records an audit entry.
- **App lock enforcement (PR #89):** biometric launch gating respects the signed-in user's
  per-account setting.
- **Moderation enforcement (PR #91):** SUSPENDED/BANNED disables Auth, revokes refresh tokens,
  and denies interactive writes.
- **Server-created chat requests (PR #92):** the callable atomically validates accepted
  interest, enforces the free daily quota, and creates the request; direct client creates are
  denied.

---

## [2.0.0] - 2026-05-28

### Added
- **Biometric app lock**: `BiometricAuthManager` wraps BiometricPrompt API; prompts on every cold start. Debug builds bypass on failure.
- **Adhan (Prayer Times) system**: `AdhanScheduler` uses `AlarmManager.setAlarmClock()` (Doze-exempt) to fire `AdhanReceiver` at precise prayer times (FAJR, DHUHR, ASR, MAGHRIB, ISHA).
- **Adhan sound channels**: separate `NotificationChannel` per sound pattern (TAKBEER, SILENT, etc.) for Android 8+ compliance.
- **`FullScreenIntent`** on Adhan notifications - wakes the screen like a real alarm even when phone is locked.
- **`WorkManager` background sync** (`NotificationSyncWorker`) - periodic 15-minute task checks pending notifications and new chat messages in both Mock and Production modes.
- **BOOT_COMPLETED receiver** - Adhan alarms and WorkManager rescheduled automatically after device restart.
- **`CompleteProfileScreen`** - mandatory step for Google Sign-In users who have not filled their profile; guards the main navigation graph.
- **Compatibility questionnaire** - multi-step personality and lifestyle questionnaire with Gemini AI scoring.
- **AI Matchmaker screen** (`AiMatchmakerScreen`) - Gemini-powered compatibility report between two profiles.
- **Guardian (Wali) flow**: invitation email, acceptance, and real-time wali chat log view.
- **Photo privacy**: users mark photos as "awaiting approval"; only approved viewers see full images.
- **Identity verification status badge** displayed on profile cards and match detail screen.
- **Advanced search filters**: age range, governorate, prayer frequency, beard/hijab level, relocation willingness, marital status, education level.
- **Firebase App Check** in `MainActivity`; release builds use Play Integrity, debug builds use `DebugAppCheckProviderFactory`.
- **Firebase Cloud Functions** (`functions/index.js`) for privileged operations and triggers: `setVerificationStatus`, `setUserPremium`, `setUserRole`, `deleteUserProfile` (callable, App Check enforced), plus `onLikeCreated`, `onChatMessageCreated`, and `mirrorPublicProfile` (Firestore triggers).
- **Firestore Security Rules** v2 - granular per-collection rules blocking client-side privilege escalation.
- **Storage Rules** - users can only write to their own `profileImages/{uid}/` path; reads require authentication.
- **Block and Report** in `MatchDetailScreen` - writes to `blocks/{uid}/blockedUsers` and `reports` collections; blocked users hidden from search.
- **Phone number filter** in `ChaperonedChatViewModel` - regex strips phone patterns from outgoing messages without blocking normal Arabic text.
- **`MithaqFirebaseMessagingService`** - handles FCM data payloads and shows local notifications with correct channel.
- **Splash screen** with animated logo transition.
- **`SettingsScreen`**: language toggle (Arabic/English), dark mode, notification preferences, Adhan configuration.
- **Premium store** (`PremiumStoreScreen`): Gold and Platinum subscription tiers with feature comparison.
- **Reward system**: daily prayer and mosque visit streaks tracked in Firestore.

### Changed
- Adhan scheduling migrated from `setExactAndAllowWhileIdle` to `setAlarmClock` for reliable Doze-mode delivery.
- `ChaperonedChatViewModel` sender ID reads from Firebase Auth in production (was hardcoded to `"mock_user"`).
- `LikesRepository` mutual-match detection uses a Firestore transaction to prevent race conditions.
- Premium upgrades delegated to Cloud Functions (client can no longer self-grant in production).
- Guardian status values normalized: NONE, PENDING, VERIFIED (removed legacy "none" string).

### Fixed
- FCM token now synced to Firestore immediately after Google Sign-In (was previously missed).
- Adhan sound null fallback now correctly uses system alarm ringtone.
- `AdhanReceiver` no longer crashes on unsupported broadcast actions.
- Firestore batch.commit() race condition in notification delivery resolved.

### Security
- Client-side admin/premium/verification field writes blocked in Firestore rules.
- `exported="false"` on receivers that do not need external app access.
- Screenshot protection active on sensitive screens (MatchDetailScreen, ChaperonedChatScreen).
- `allowBackup="false"` in AndroidManifest.xml.
- `usesCleartextTraffic="false"` - HTTPS only.

### Removed
- Hardcoded mock users from production code paths.
- Old template Activity boilerplate replaced by Compose-only navigation.
- APK/AAB artifacts removed from version control.

---

## [1.5.0] - 2026-04-15

### Added
- Room database for local chat message caching (`ChaperonedChatDao`, `ChatMessageEntity`).
- Real-time Firestore listener in `ChaperonedChatViewModel` with Room write-through cache.
- Ice-breaker suggestions powered by Gemini (first message prompts).
- Translation toggle for chat messages (Arabic / English via ML Kit or Gemini).
- Voice call UI scaffolding (WebRTC integration placeholder).
- `ProfileSettingsScreen` with editable fields: bio, city, education, marital status, children preferences.

### Changed
- Navigation graph refactored from string routes to typed sealed class routes.
- Profile image upload migrated to Firebase Storage with progress indicator.

### Fixed
- Bottom navigation badge count not updating after reading messages.
- Search filter reset button not clearing all active filter chips.

---

## [1.2.0] - 2026-03-10

### Added
- `LikesRepository` with atomic mutual-match detection via Firestore transaction.
- Match detail screen (`MatchDetailScreen`) showing full profile, compatibility score, and action buttons.
- Favorites collection per user.
- Push notification for new mutual match event.

### Changed
- Profile card redesign: photo grid replaced by single primary image with blur-on-request.
- Compatibility score algorithm updated to weight prayer habits and relocation preference more heavily.

### Fixed
- Memory leak in `SearchViewModel` (Flow collector not cancelled on ViewModel clear).
- Incorrect Arabic string pluralization for match count label.

---

## [1.0.0] - 2026-02-01

### Added
- Initial release of Mithaq Android application.
- Firebase Auth (email/password + Google Sign-In).
- Firestore-backed user profile model (`UserProfile`).
- Basic search with age and gender filters.
- Chaperoned chat room creation.
- Guardian invitation via email.
- Jetpack Compose UI with Material 3 theming.
- Arabic RTL layout support.
- Dark mode support.
- ProGuard/R8 release configuration.

---

<!--
  These compare links assume git tags v1.0.0…v2.0.0 exist. They do NOT exist yet — cut real
  tags before relying on them. Repo slug corrected to the actual remote.
-->
[Unreleased]: https://github.com/Octopus-networks/test5/compare/v2.1.0...HEAD
[2.1.0]: https://github.com/Octopus-networks/test5/releases/tag/v2.1.0
[2.0.0]: https://github.com/Octopus-networks/test5/compare/v1.5.0...v2.0.0
[1.5.0]: https://github.com/Octopus-networks/test5/compare/v1.2.0...v1.5.0
[1.2.0]: https://github.com/Octopus-networks/test5/compare/v1.0.0...v1.2.0
[1.0.0]: https://github.com/Octopus-networks/test5/releases/tag/v1.0.0
