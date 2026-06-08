# Mithaq — Feature Status Matrix

> Honest status of each user-facing feature. Legend:
> **✅ Done** (works end-to-end) ·
> **🟡 Foundation** (data/UI exists, enforcement or backend incomplete) ·
> **🖼️ UI-only** (screen exists, not wired to a real backend) ·
> **📋 Planned**.
>
> Canonical messaging system = **`chats/*` (new)**. `chatRooms/*` is legacy and not built upon.

## Auth & onboarding
| Feature | Status | Notes / evidence |
|---|---|---|
| Email/password auth + email verification | ✅ Done | `AuthViewModel`, gated by `isVerifiedEmailUser()` in rules |
| Password reset / email verification deep links | ✅ Done | `docs/auth/`, manifest deep links |
| Profile onboarding (structured question engine) | ✅ Done | `ui/onboarding/OnboardingFlow.kt`; legacy wizard not yet removed |

## Discovery, matching & requests
| Feature | Status | Notes / evidence |
|---|---|---|
| Discovery / search from `publicProfiles` mirror | ✅ Done | `PublicProfileRepository`, server-owned mirror |
| Discover opposite-gender filter | ✅ Done | merged via `phase-16a` (Phase 16) |
| Interest → photo → chat request flow | ✅ Done | deterministic ids, gated transitions in repos + rules |
| Likes / mutual match | ✅ Done | mutual computed server-side (`onLikeCreated`) |
| Compatibility scoring | ✅ Done | `ui/match/MatchScoreCalculator.kt` |

## Chat
| Feature | Status | Notes / evidence |
|---|---|---|
| Text chat (**canonical `chats/*` stack**) | ✅ Done | `ChatRepository` + `ChatMessageRepository`; **room creation is client-side — flagged for backend move before production** |
| Chat image attachments | ✅ Done | `chat_attachments/{chatId}/{messageId}` storage rule + notify function (deployed) |
| Chat voice notes | ✅ Done | record + playback (Android-only; no backend change) |
| Message reactions (emoji) | ✅ Done | WhatsApp-style reactions; rules deployed |
| Chaperoned chat + wali logs | 🟡 Foundation (legacy) | built on **legacy `chatRooms/*`**; the canonical `chats/*` stack has no wali logging yet. **Out of scope for launch** — re-implement on `chats/*` later (see TECH_DEBT / ARCHITECTURE §3.1) |

## Trust, safety & verification
| Feature | Status | Notes / evidence |
|---|---|---|
| Identity verification (ID + selfie + ML Kit) | ✅ Done | upload → `verification/{uid}/`, PENDING → wali/admin approval |
| Trust badges (verified / guardian) in discovery | ✅ Done | mirrored from `users/{uid}` via Cloud Functions |
| Photo privacy (request + approved viewers) | ✅ Done | unauthorized viewers render a preset avatar; **real photo bytes are never loaded/cached** (merged via `phase-16c`) |
| Logout/login state isolation | ✅ Done | per-user ViewModel keys; no cross-account leak on a shared device (merged via `phase-16b`) |
| Admin console (users, premium, roles, verification) | ✅ Done | via callable Cloud Functions (admin-gated) |
| Admin moderation — reports & photo review | ✅ Done | `AdminModerationRepository`, 3-layer admin gating |
| Admin moderation — ban / suspend | 🟡 Foundation | **records state only; NOT enforced** anywhere (`Moderation.kt`, `AdminModerationScreen` TODO) |
| Bidirectional block filtering in discovery | 📋 Planned | block exists, but discovery does not yet filter blocked users both ways |

## Profile hub & account settings (`ui/profile`)
All 10 hub cards are wired (PR #60). The hub raises `onOpenXxx` callbacks → `MithaqMainExperience` → `MainActivity` routes.

| Feature | Status | Notes / evidence |
|---|---|---|
| My photos management | ✅ Done | `MyPhotosScreen` |
| Edit profile (info, bio, modesty, guardian) | ✅ Done | `ProfileSettingsScreen` (re-enabled); AI bio button gated behind `BetaFeatureGates.GEMINI_AI` |
| Privacy — per-field public visibility | ✅ Done | hide age / location / marital status / marriage timeline; stored at `profiles/{uid}.privacyTrust`, honored by `buildPublicProfile` (functions deployed) |
| Photo privacy management (approve / reject / revoke) | ✅ Done | `PhotoPrivacyScreen` + `PhotoAccessManager` (owner-write, no rules change) |
| Prayer settings (Adhan) | ✅ Done | `PrayerSettingsScreen` reuses `AdhanSettingsSectionFixed` |
| Notification settings | ✅ Done | Phase 13C; global + per-type toggles |
| Language / theme | ✅ Done | `AppSettingsScreen` (Arabic ⇄ English + dark mode) |
| Security — biometric app lock | 🟡 Foundation | setting persists + verified via a biometric prompt, **but lock-on-launch is NOT enforced yet** (`SecuritySettingsScreen`) |
| Support & help (FAQ + email contact) | ✅ Done | `SupportScreen` |
| Guardian (Wali) invite / status | ✅ Done | `GuardianScreen` + `GuardianViewModel` (user-side invite; **not** the gated wali-account dashboard) |

## Notifications
| Feature | Status | Notes / evidence |
|---|---|---|
| Push notifications (FCM) | ✅ Done | Phase 13A/B/C; `users/{uid}/fcmTokens`, Cloud Function triggers (interest / photo / chat request, message, photo moderation) |
| Per-type notification sounds | ✅ Done | bundled `res/raw` chimes; global default + per-category override |
| Notification preferences gating | ✅ Done | `users/{uid}/notificationSettings/preferences` honored in functions |

## Islamic & lifestyle
| Feature | Status | Notes / evidence |
|---|---|---|
| Adhan scheduling / prayer tracking | ✅ Done | exact-alarm based |

## Deferred / incomplete (Phase 17 decisions)
| Feature | Status | Notes / evidence |
|---|---|---|
| Premium / subscription (Gold, Platinum) | 🖼️ UI-only | **simulated checkout, no Google Play Billing**; production purchase returns an error; only admin can grant premium. Decision: **defer** |
| Daily chat limit (free vs premium) | 📋 Planned | `ChatLimitManager` exists but **has zero call sites** — not enforced |
| AI matchmaker / Gemini assistance | 🖼️ UI-only / debug | release ships an empty Gemini key by design. Decision: **hide** unless a backend proxy exists |
| Voice call | 🖼️ UI-only | scaffolding only. Decision: **hide** in first release |

## Known "before production" gaps (cross-reference)

- Move chat-room creation + chat-limit/premium gate to Cloud Functions (client-trusted today).
- Real billing integration for premium (Phase 19).
- Enforce ban/suspension.
- Enforce the biometric app lock on app launch/resume (the setting exists; enforcement does not).
- Re-implement chaperoned/wali monitoring on the canonical `chats/*` stack; retire legacy stacks.
- Hide incomplete features (Voice Call / Billing / Gemini) for the first release (Phase 17).

✅ **Profile hub complete (PR #60):** all 10 account-settings cards wired; per-field Privacy shipped full-stack (Android + `mirrorPublicProfile` deployed); Support & Security screens built with coding agents (Antigravity / Codex).
✅ **Resolved in Phase 16:** real photo privacy, logout state isolation, Discover gender filter (merged via `phase-16a/b/c`; the duplicate `fix/*` PRs #40/#41/#42 are superseded and can be closed).

See [`TECH_DEBT.md`](./TECH_DEBT.md) for the engineering backlog behind the open gaps.
