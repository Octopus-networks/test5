# Changelog

All notable changes to **Mithaq** are documented here.
Format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow [Semantic Versioning](https://semver.org/).

---

## [Unreleased]

> **Note on versions:** the dated `[1.0.0]`–`[2.0.0]` sections below are a documentation
> narrative. The repository currently has **no matching git tags**, and day-to-day work is
> tracked by the phase-based [`ROADMAP.md`](./ROADMAP.md). Treat the compare links at the
> bottom as indicative until real tags are cut.

### Added
- **Profile hub activated (PR #60):** all 10 account-settings cards in `ui/profile` are now wired —
  edit profile, per-field **Privacy**, photo-access management, Adhan/prayer settings, notification
  settings, language & theme, biometric app-lock setting, support (FAQ + contact), and guardian
  (wali) invite. New screens: `PrivacySettingsScreen`, `PhotoPrivacyScreen`, `PrayerSettingsScreen`,
  `SecuritySettingsScreen`, `SupportScreen`, `GuardianScreen` (Support & Security built with coding
  agents). The hub raises `onOpenXxx` callbacks → `MithaqMainExperience` → `MainActivity` routes.
- **Per-field public-profile privacy (full-stack):** users can hide age / location / marital status /
  marriage timeline from discovery. Flags stored at `profiles/{uid}.privacyTrust`; the
  `mirrorPublicProfile` Cloud Function now blanks the hidden fields in `publicProfiles` (deployed).
- **Photo-access management screen:** owners can approve / reject / revoke photo-view requests
  (`PhotoAccessManager.rejectPhotoAccess` added; owner-write only, no rules change).
- **Security hardening (guardian/wali):** guardian/wali binding now requires a **verified**
  email (`email_verified == true`) in both Firestore rules and the
  `requireAdminOrAssignedWali` Cloud Function, so a wali must prove inbox ownership.
- **`mirrorPublicProfileOnUserChange` Cloud Function:** re-mirrors `publicProfiles` when a
  user's `verificationStatus`/`guardianStatus` changes, so discovery trust badges stay current
  (previously hardcoded to `false`).
- **Tightened Firestore rules** for `reports`, `profile_views`, and `favorites` (strict field
  allow-lists, self-target prevention, length caps).
- Project documentation: [`ROADMAP.md`](./ROADMAP.md), [`docs/ARCHITECTURE.md`](./docs/ARCHITECTURE.md),
  [`docs/FEATURE_STATUS.md`](./docs/FEATURE_STATUS.md), [`docs/TECH_DEBT.md`](./docs/TECH_DEBT.md);
  per-topic docs reorganized under `docs/`.
- `BootReceiver` - dedicated broadcast receiver for `BOOT_COMPLETED` and `LOCKED_BOOT_COMPLETED` that restores Adhan alarms and WorkManager sync automatically after device reboot.
- `ensureBackgroundServicesRunning()` in `MainActivity` - silently re-queues WorkManager and reschedules Adhan alarms every time the app opens, recovering gracefully from a Force Stop.
- `FOREGROUND_SERVICE_DATA_SYNC` permission for future foreground worker support.
- `directBootAware="true"` on `BootReceiver` so it fires before the first unlock screen on Android 7+.
- Language badges and technology stack shields in `README.md`.
- Branch protection workflow in `.github/workflows/protect-main.yml` to block force-pushes on main.
- **`Release APK` workflow** (`.github/workflows/release.yml`): builds and attaches a downloadable APK to a GitHub Release on a `v*` tag or manual dispatch (debug-signed pre-release for now; see [`docs/ops/release.md`](./docs/ops/release.md)).

### Changed
- `AdhanReceiver` is now `exported="false"` and only handles `ADHAN_ALARM` actions (Boot recovery delegated to `BootReceiver`).
- Last logged-in UID is now persisted in `mithaq_prefs` so `BootReceiver` can restart WorkManager without requiring the user to re-open the app.

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
[Unreleased]: https://github.com/Octopus-networks/test5/compare/v2.0.0...HEAD
[2.0.0]: https://github.com/Octopus-networks/test5/compare/v1.5.0...v2.0.0
[1.5.0]: https://github.com/Octopus-networks/test5/compare/v1.2.0...v1.5.0
[1.2.0]: https://github.com/Octopus-networks/test5/compare/v1.0.0...v1.2.0
[1.0.0]: https://github.com/Octopus-networks/test5/releases/tag/v1.0.0
