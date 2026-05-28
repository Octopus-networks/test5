# Mithaq

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
