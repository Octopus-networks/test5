# CLAUDE.md — Project memory for Mithaq

> Auto-loaded each session by Claude Code / Codex. Keep it short and current.
> Last updated: **2026-06-01** (v2.1.0).

## What this is
**Mithaq** (ميثاق) — a privacy-first Islamic marriage Android app. Kotlin + Jetpack Compose, Firebase backend, Room cache, ML Kit, Gemini AI. Package `com.mithaq.app`. Still under active development.

## Tech stack
- **Lang/UI:** Kotlin 2.0, Jetpack Compose + Material 3, MVVM, RTL Arabic.
- **Backend:** Firebase (Auth, Firestore, Storage, FCM, Functions, App Check / Play Integrity).
- **Local:** Room. **AI:** Gemini SDK. **Verify:** ML Kit face detection.
- **Min SDK 24 · Target/Compile SDK 36 · JDK 17.**

## Build & verify (Windows PowerShell)
```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:PATH="$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:PATH"
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

## Conventions
- **Commits:** Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`…). Enforced by `protect-main.yml`.
- **Never commit:** `local.properties`, `*.apk`/`*.aab`, `build/`, `.idea/`, `google-services.json` keys aside. Gemini key lives in `local.properties` (debug) — release ships empty and proxies via backend.
- **Localization:** currently in-code via an `isArabic` boolean (Arabic/English) across ~30 files. Full Android-resource i18n is a planned refactor — see `ROADMAP.md`.
- **Versioning:** SemVer. Bump `versionCode`+`versionName` in `app/build.gradle.kts`, add a section to `CHANGELOG.md`, update README version badge.
- **Privileged ops** (admin/premium/verify/assign-guardian) go through **Cloud Functions**, never client writes. Firestore Rules block client-side privilege escalation — edit rules carefully.

## ⚠️ Known issues / TODO (see ROADMAP.md)
- 🔴 **Leaked Gemini key in git history** (`AIzaSy…EWs`) — must be revoked/rotated in Google Cloud Console; history rewrite alone won't undo exposure.
- Low test coverage (1 test file) — add unit tests.
- `MainActivity.kt` ~1,689 lines — needs splitting.
- No rate limiting on `onLikeCreated` / `onChatMessageCreated` Cloud Functions.

## Session log
- **2026-06-01 (v2.1.0):** Fixed `Config.isMock()` to fail-closed; rewrote invalid `android.yml` CI (was broken YAML); bumped 2.0→2.1.0; added `ROADMAP.md`; removed stray `scratch/*.py` and a tracked debug APK; flagged the leaked Gemini key. Changes are local only (not committed/pushed).
