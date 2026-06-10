# Mithaq — Islamic Marriage App

<div align="center">

<!-- ── Language & Platform ─────────────────────────────────────────────── -->
![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-API%2024--36-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![JDK](https://img.shields.io/badge/JDK-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)

<!-- ── UI & Architecture ────────────────────────────────────────────────── -->
![Jetpack Compose](https://img.shields.io/badge/Compose%20BOM-2026.03.01-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Material 3](https://img.shields.io/badge/Material%203-UI-757575?style=for-the-badge&logo=materialdesign&logoColor=white)
![Architecture](https://img.shields.io/badge/Architecture-MVVM%20(partial)-FF6F00?style=for-the-badge&logo=android&logoColor=white)
![RTL](https://img.shields.io/badge/Layout-RTL%20Ready-009688?style=for-the-badge&logo=googletranslate&logoColor=white)

<!-- ── Backend & Services ──────────────────────────────────────────────── -->
![Firebase](https://img.shields.io/badge/Firebase%20BOM-33.1.0-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Firebase Services](https://img.shields.io/badge/Firebase-Auth%20%7C%20Firestore%20%7C%20Storage%20%7C%20FCM%20%7C%20Functions-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Cloud Functions](https://img.shields.io/badge/Cloud%20Functions-Node.js%2022-4285F4?style=for-the-badge&logo=googlecloud&logoColor=white)
![Room](https://img.shields.io/badge/Room-2.8.4-6200EE?style=for-the-badge&logo=sqlite&logoColor=white)

<!-- ── AI & ML ──────────────────────────────────────────────────────────── -->
![Gemini](https://img.shields.io/badge/Gemini%20SDK-0.9.0-8E24AA?style=for-the-badge&logo=google&logoColor=white)
![ML Kit](https://img.shields.io/badge/ML%20Kit%20Face%20Detection-17.1.0-34A853?style=for-the-badge&logo=google&logoColor=white)

<!-- ── Security ────────────────────────────────────────────────────────── -->
![App Check](https://img.shields.io/badge/App%20Check-Play%20Integrity-EA4335?style=for-the-badge&logo=googlechrome&logoColor=white)
![Biometric](https://img.shields.io/badge/Biometric-1.1.0-607D8B?style=for-the-badge&logo=android&logoColor=white)

<!-- ── Build & CI ──────────────────────────────────────────────────────── -->
![Gradle](https://img.shields.io/badge/Gradle-9.4.1-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![Android Gradle Plugin](https://img.shields.io/badge/AGP-9.2.1-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![CI](https://img.shields.io/badge/CI-GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)
![Branch Protection](https://img.shields.io/badge/Branch-Protected%20%7C%20No%20Force%20Push-red?style=for-the-badge&logo=git&logoColor=white)

<!-- ── App Metadata ────────────────────────────────────────────────────── -->
![Version](https://img.shields.io/badge/Version-2.1.0-blue?style=for-the-badge)
![License](https://img.shields.io/badge/License-Proprietary-lightgrey?style=for-the-badge)

</div>

---

Mithaq is a Kotlin Android application for privacy-first Islamic matchmaking. It uses Jetpack Compose, Firebase, Room, ML Kit, and Android security APIs to support serious marriage workflows with profile verification, guardian oversight, modest photo controls, search filters, and secure chat.

## Documentation

| Doc | Purpose |
|---|---|
| [`ROADMAP.md`](ROADMAP.md) | Phase-based roadmap (Phase 0–21), current focus, and what's next |
| [`HANDOFF.md`](HANDOFF.md) | Current release/main state, roles, recent merged work, and operational follow-ups |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | System design — `chats/*` is the canonical messaging stack (`chatRooms/*` is legacy) |
| [`docs/FEATURE_STATUS.md`](docs/FEATURE_STATUS.md) | Honest per-feature status (Done / Foundation / UI-only / Planned) |
| [`docs/TECH_DEBT.md`](docs/TECH_DEBT.md) | Engineering backlog from architecture + code reviews |
| [`docs/security/firestore-rules.md`](docs/security/firestore-rules.md) | Deployed Firestore/Storage rules rationale |
| [`docs/admin/`](docs/admin/) · [`docs/auth/`](docs/auth/) · [`docs/ops/`](docs/ops/) · [`docs/qa/`](docs/qa/) | Admin moderation, auth flows, deploy, QA reports |

> The "MVVM + (partial)" badge is deliberate: the newer feature slices follow MVVM with a
> repository layer, but there is no DI framework or use-case layer yet, and some screens call
> Firestore directly. See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) and
> [`docs/TECH_DEBT.md`](docs/TECH_DEBT.md).

## Current Status

- Latest release: `v2.1.0` (versionCode 21); `main` includes post-release PRs #94-#96
- Package: `com.mithaq.app`
- Platform: Android
- Language: Kotlin
- UI: Jetpack Compose + Material 3
- Backend: Firebase Auth, Firestore, Storage, Cloud Messaging, and modular Cloud Functions v2
- Functions runtime: Node 22, `firebase-admin ^13.10.0`, `firebase-functions ^7.2.5`
- Local cache: Room
- Verification: Google ML Kit Face Detection
- AI features: Gemini SDK hooks for guided chat assistance

## Key Features

> For the accurate, honest status of each feature (some are foundation-only or UI-only), see
> [`docs/FEATURE_STATUS.md`](docs/FEATURE_STATUS.md).

- Mandatory 58-step onboarding, loaded from JSON behind a consistency gate and mirrored
  server-side into the matchable `users/{uid}` profile.
- Discovery and advanced search filters (age, sect, prayer habits, modesty, relocation, and more) from a server-owned `publicProfiles` mirror.
- Like / unlike toggle with server-computed mutual matches.
- Compatibility scoring in `com.mithaq.app.ui.match`.
- Interest → photo → server-created chat request flow with deterministic, rules-gated transitions
  and a server-authoritative free daily chat cap.
- Secure chat on the canonical `chats/*` stack with **image attachments, voice notes, emoji
  reactions, and premium read receipts**.
- Push notifications (FCM) with **per-type sounds** and granular, server-honored preferences.
- Identity verification (ID + selfie + ML Kit) with reusable Verified / guardian trust badges.
- Guardian (Wali) invitation and oversight flows.
- Photo privacy: blurred-by-default with per-viewer request / approve / revoke control enforced
  for both Firestore metadata and Storage bytes.
- **Account hub** (`ui/profile`): edit profile, per-field public-visibility privacy, photo-access management, Adhan/prayer settings, notification settings, language & theme, biometric app lock, support, and guardian invite.
- Location-based Adhan scheduling with exact-alarm support.
- Five active premium perks: **Incognito, Boost, Read receipts, Profile Highlight, and 2x
  Exposure**. The store remains simulated and live billing is still OFF.
- Opt-in biometric app lock and screenshot protection for sensitive screens.

## Security Updates

The PR #84-#92 security sweep and follow-up quality work harden the app and repository:

- Firebase App Check is initialized in Android; release builds use Play Integrity.
- Cloud Functions use the modular v2 API on Node 22 and own privileged admin changes,
  server-created notifications, onboarding mirroring, and chat-request quota enforcement.
- Firestore rules block client-side privilege escalation for admin, premium, verification,
  wali, and ward fields.
- Premium upgrades are no longer granted directly by the client in production mode.
- Revoking photo access deletes the approval that Storage rules consult, immediately stopping
  private-photo downloads.
- Incognito profiles are excluded from discovery, and bidirectional blocks are enforced in
  Firestore and Storage rules.
- Admin deletion removes the Firebase Auth account, Firestore identity data/subcollections,
  private media, and records an audit entry.
- SUSPENDED/BANNED moderation disables Auth accounts, revokes refresh tokens, and blocks
  interactive writes while the token expires.
- The biometric app lock now prompts only when the signed-in user enabled it.
- Direct client chat-request creation is denied; the callable creates requests and enforces the
  free-tier daily limit atomically.
- Android catch paths report exceptions through Firebase Crashlytics instead of
  `printStackTrace()`.

## Repository Layout

> ⚠️ This layout is mid-migration. Some large screens still live at the package root and
> there are two `model` packages; see [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) §3–4 and
> [`docs/TECH_DEBT.md`](docs/TECH_DEBT.md) for the reorganization plan.

```text
app/src/main/java/com/mithaq/app/
  MainActivity.kt         App entry + navigation host (oversized; root-level)
  Tabs.kt SearchTab.kt ChatTab.kt ProfileSettings.kt WaliDashboard.kt
                          Legacy large screens still at package root (to be moved)
  model/                  Legacy data models (UserProfile god class, legacy ChatRoom)
  domain/model/           Newer focused domain models (source of truth)
  data/local/             Room database + DAOs
  data/repository/         Repositories (Firestore <-> domain)
  service/                GeminiService, BackendFunctions (remote data sources)
  navigation/             Routes, AuthGate
  receiver/               Android broadcast receivers (Adhan, Boot)
  notification/           Firebase + local notification handling
  security/               Biometric, screenshot, and blur helpers
  util/                   Prayer, adhan, country, and reward utilities
  ui/auth/  ui/onboarding/  ui/home/  ui/search/  ui/filter/  ui/match/
  ui/messages/  ui/chat/  ui/requests/  ui/photo/  ui/guardian/  ui/limit/
  ui/profile/  ui/settings/  ui/stats/  ui/admin/  ui/verification/
  ui/splash/  ui/components/  ui/common/  ui/theme/

firestore.rules           Firestore access rules
storage.rules             Firebase Storage rules
app/proguard-rules.pro    Release shrinker rules
functions/                Firebase Cloud Functions for privileged backend operations
docs/                     Architecture, roadmap, security, admin, auth, ops, QA docs
```

## Build Requirements

- Android Studio with JDK 17
- Android SDK installed locally
- A valid Firebase Android config at `app/google-services.json`
- Optional local `GEMINI_API_KEY` in `local.properties` or environment variables for debug AI features

## Common Commands

On Windows PowerShell:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:PATH="$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:PATH"

.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleRelease
```

Firebase backend checks and deploys require the Firebase CLI:

```powershell
firebase emulators:start --only auth,firestore,storage,functions
firebase deploy --only firestore:rules,storage,functions
```

Before enforcing App Check in the Firebase console, register the Android app with Play Integrity and add debug App Check tokens for local test devices.

## Verification

The current codebase was verified with:

- `:app:assembleDebug`
- `:app:testDebugUnitTest`
- `:app:lintDebug`
- `:app:assembleRelease`

`lintDebug` currently reports warnings and hints only. There are no lint errors.

## Repository Hygiene

Do not commit generated build outputs or local IDE state. The repository ignores:

- APK/AAB release artifacts
- Gradle and Kotlin build directories
- Android Studio `.idea/` metadata
- Local machine files such as `local.properties`

Release artifacts should be uploaded through GitHub Releases or CI output, not stored in the source tree. To publish a downloadable APK, push a `v*` tag (or run the **Release APK** workflow manually) — see [`docs/ops/release.md`](docs/ops/release.md).
