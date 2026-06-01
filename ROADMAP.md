# Mithaq — Roadmap / خارطة الطريق

> Status as of **2026-06-01** · Current version **2.1.0** (`versionCode 21`)
> Target: a hardened **1.0 public launch** on Google Play.

This roadmap tracks what is **done**, what is **in progress**, and what is **remaining**.
Arabic summary is provided under each section. خلاصة عربية تحت كل قسم.

---

## ✅ Completed / تم إنجازه

### Core product
- [x] Email/password + Google Sign-In auth (`ui/auth`)
- [x] Profile onboarding wizard (religious, demographic, lifestyle, guardian fields)
- [x] Compatibility scoring (`ui/match`) + Gemini-powered AI matchmaker
- [x] Advanced search & filters (`ui/filter`, `ui/search`)
- [x] Guardian (Wali) invitation, acceptance, and chat-log monitoring
- [x] Chaperoned chat with phone-number stripping + Room write-through cache
- [x] Photo privacy (awaiting-approval + approved-viewer lists)
- [x] Identity verification status badge + ML Kit face detection
- [x] Adhan (prayer times) scheduling with Doze-exempt alarms
- [x] Premium tiers (Gold / Platinum) + reward streaks

### Platform & security
- [x] Firebase App Check (Play Integrity in release, debug provider in debug)
- [x] Firestore Security Rules v2 (blocks client-side privilege escalation)
- [x] Storage Rules scoped to `profileImages/{uid}/`
- [x] Cloud Functions own all privileged operations + audit logging
- [x] Biometric app lock + screenshot protection on sensitive screens
- [x] `allowBackup=false`, HTTPS-only (`usesCleartextTraffic=false`)
- [x] Force Stop / reboot recovery (`BootReceiver` + `ensureBackgroundServicesRunning()`)
- [x] Branch-protection workflow + valid Android CI (build + unit tests + lint)
- [x] `Config.isMock()` fails closed on error (no accidental mock bypass)

> **عربي:** المنتج الأساسي (تسجيل، ملف شخصي، بحث، توافق، ولي، دردشة محمية، صور، تحقق، أذان، اشتراكات) مكتمل. الأمان متقدّم: App Check، قواعد Firestore، Cloud Functions، قفل بيومتري، واسترجاع بعد Force Stop.

---

## 🚧 In progress / قيد العمل

- [ ] **Repo hygiene** — remove leftover `scratch/*.py` helper scripts from the Android source tree.
- [ ] **CI hardening** — confirm `android.yml` is green on GitHub Actions after the YAML fix.
- [ ] **Stats / limits screens** (`ui/stats`, `ui/limit`) — verify all states render and wire to real data.

> **عربي:** تنظيف المستودع، التأكد أن الـ CI أخضر بعد الإصلاح، وإكمال شاشات الإحصائيات والحدود.

---

## 🔜 Remaining before 1.0 / المتبقي قبل الإطلاق

### 1. Internationalization (i18n) — **highest-effort item**
Today the app is bilingual (Arabic/English) via an **in-code `isArabic` boolean** spread across 30+ files. This works but is hard to scale.

- [ ] Migrate hardcoded UI strings into Android string resources (`values/`, `values-en/`, `values-ar/`).
- [ ] Replace `isArabic ? "..." : "..."` ternaries with `stringResource(...)`.
- [ ] Use per-app locale (`AppCompatDelegate.setApplicationLocales`) instead of a custom toggle.
- [ ] (Optional) Add further languages (Urdu, French, Turkish) once the resource layer exists.

> ⚠️ This is a large, cross-cutting refactor (~30 files). Recommend doing it on a dedicated branch, screen-by-screen, to avoid regressions.

### 2. Testing & quality
- [ ] Raise test coverage — currently **1 test file**. Add unit tests for `AuthViewModel`, repositories, compatibility scoring, and the phone-number message filter.
- [ ] Add Firestore Rules unit tests (`@firebase/rules-unit-testing`).
- [ ] Instrumented smoke test for the main navigation graph.

### 3. Performance & maintainability
- [ ] Split oversized files — `MainActivity.kt` is ~1,689 lines; extract screens into `ui/` packages.
- [ ] Add rate limiting to `onLikeCreated` / `onChatMessageCreated` Cloud Functions to prevent spam.

### 4. Security follow-ups
- [ ] **Rotate the leaked Gemini API key** (`AIzaSy…EWs`) found in git history — it must be revoked in Google Cloud Console; removing it from code does **not** remove it from history.
- [ ] Add API key restrictions (Android app + SHA-1) to the Firebase Android key in Google Cloud Console.
- [ ] Move production AI calls behind a backend proxy (key never ships in the client).

### 5. Release readiness
- [ ] Real release signing config (keystore in CI secrets, not committed).
- [ ] Play Store listing assets (screenshots, privacy policy, data-safety form).
- [ ] Crash reporting (Firebase Crashlytics) + basic analytics.

> **عربي:** أهم بند متبقٍّ هو **التعريب الكامل عبر موارد Android** (شغل كبير على 30+ ملف، يُفضَّل على فرع منفصل). يليه رفع تغطية الاختبارات، تقسيم `MainActivity` الضخم، rate limiting للـ Cloud Functions، **تجديد مفتاح Gemini المسرّب**, وتجهيز الإطلاق (توقيع، Crashlytics، أصول المتجر).

---

## 📌 Versioning
Versions follow [Semantic Versioning](https://semver.org/). See [CHANGELOG.md](CHANGELOG.md) for full history.

| Version | Date | Highlight |
|---------|------|-----------|
| 2.1.0 | 2026-06-01 | Force Stop recovery, branch protection, CI fix, security hardening |
| 2.0.0 | 2026-05-28 | Biometric lock, Adhan, App Check, Cloud Functions, Firestore Rules v2 |
| 1.5.0 | 2026-04-15 | Room chat cache, translation toggle, typed navigation |
| 1.2.0 | 2026-03-10 | Mutual-match detection, match detail screen |
| 1.0.0 | 2026-02-01 | Initial release |
