# Mithaq — Islamic Marriage App

<div align="center">

<!-- ── Language & Platform ─────────────────────────────────────────────── -->
![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-API%2024--36-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![JDK](https://img.shields.io/badge/JDK-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)

<!-- ── UI & Architecture ────────────────────────────────────────────────── -->
![Jetpack Compose](https://img.shields.io/badge/Compose%20BOM-2026.03.01-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Material 3](https://img.shields.io/badge/Material%203-UI-757575?style=for-the-badge&logo=materialdesign&logoColor=white)
![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean-FF6F00?style=for-the-badge&logo=android&logoColor=white)
![RTL](https://img.shields.io/badge/Layout-RTL%20Ready-009688?style=for-the-badge&logo=googletranslate&logoColor=white)

<!-- ── Backend & Services ──────────────────────────────────────────────── -->
![Firebase](https://img.shields.io/badge/Firebase%20BOM-33.1.0-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Firebase Services](https://img.shields.io/badge/Firebase-Auth%20%7C%20Firestore%20%7C%20Storage%20%7C%20FCM%20%7C%20Functions-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Cloud Functions](https://img.shields.io/badge/Cloud%20Functions-Node.js%2020-4285F4?style=for-the-badge&logo=googlecloud&logoColor=white)
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
![Debug APK](https://img.shields.io/badge/Debug%20APK-29.3%20MB-success?style=for-the-badge&logo=android&logoColor=white)
![Version](https://img.shields.io/badge/Version-2.0-blue?style=for-the-badge)
![License](https://img.shields.io/badge/License-Proprietary-lightgrey?style=for-the-badge)

</div>

---

Mithaq is a Kotlin Android application for privacy-first Islamic matchmaking. It uses Jetpack Compose, Firebase, Room, ML Kit, and Android security APIs to support serious marriage workflows with profile verification, guardian oversight, modest photo controls, search filters, and secure chat.


## Current Status

- Package: `com.mithaq.app`
- Platform: Android
- Language: Kotlin
- UI: Jetpack Compose + Material 3
- Backend: Firebase Auth, Firestore, Storage, and Cloud Messaging
- Local cache: Room
- Verification: Google ML Kit Face Detection
- AI features: Gemini SDK hooks for guided chat assistance

## Key Features

- Profile onboarding with religious, demographic, lifestyle, and guardian fields.
- Compatibility scoring in `com.mithaq.app.ui.match`.
- Guardian invitation and wali monitoring flows.
- Chaperoned chat with optional wali logs.
- Photo privacy requests and approved-viewer lists.
- Identity verification status and trust badges.
- Advanced search filters for age, sect, prayer habits, modesty, relocation, and other preferences.
- Adhan scheduling with exact-alarm support.
- Biometric app lock and screenshot protection for sensitive screens.

## Security Updates

The latest cleanup hardens the app and repository:

- Firebase App Check is initialized in Android; release builds use Play Integrity.
- Cloud Functions now own privileged admin changes and server-created notifications.
- Firestore rules now block client-side privilege escalation for admin, premium, verification, wali, and ward fields.
- Premium upgrades are no longer granted directly by the client in production mode.
- Guardian status values are normalized to `NONE`, `PENDING`, and `VERIFIED`.
- Chat rooms, messages, notifications, favorites, photo requests, and wali access now have stricter Firestore validation.
- Unsupported Adhan receiver broadcasts are ignored.
- Generated APKs, IDE metadata, Kotlin error logs, and old template code were removed from version control.

## Repository Layout

```text
app/src/main/java/com/mithaq/app/
  model/                  Core data models
  data/                   Repositories and local database mapping
  receiver/               Android broadcast receivers
  notification/           Firebase and local notification handling
  security/               Biometric, screenshot, and blur helpers
  ui/auth/                Login, registration, onboarding, profile auth state
  ui/chat/                Secure chat, translation, ice breakers, voice call UI
  ui/filter/              Search and filtering logic
  ui/guardian/            Guardian invitation flow
  ui/limit/               Free/premium chat limit logic
  ui/match/               Compatibility score and match detail UI
  util/                   Prayer, adhan, country, and reward utilities

firestore.rules           Firestore access rules
storage.rules             Firebase Storage rules
app/proguard-rules.pro    Release shrinker rules
functions/                Firebase Cloud Functions for privileged backend operations
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

Release artifacts should be uploaded through GitHub Releases or CI output, not stored in the source tree.
