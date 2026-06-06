# Mithaq — Feature Status Matrix

> Honest status of each user-facing feature. Legend:
> **✅ Done** (works end-to-end) ·
> **🟡 Foundation** (data/UI exists, enforcement or backend incomplete) ·
> **🖼️ UI-only** (screen exists, not wired to a real backend) ·
> **📋 Planned**.

| Feature | Status | Notes / evidence |
|---|---|---|
| Email/password auth + email verification | ✅ Done | `AuthViewModel`, gated by `isVerifiedEmailUser()` in rules |
| Password reset / email verification deep links | ✅ Done | `docs/auth/email-flows.md`, manifest deep links |
| Profile onboarding (structured question engine) | ✅ Done | `ui/onboarding/OnboardingFlow.kt` (Phase 11.12A); legacy wizard not yet removed |
| Discovery / search from `publicProfiles` mirror | ✅ Done | `PublicProfileRepository`, server-owned mirror |
| Interest → photo → chat request flow | ✅ Done | deterministic ids, gated transitions in repos + rules |
| Likes / mutual match | ✅ Done | mutual computed server-side (`onLikeCreated`) |
| Text chat (new `chats/*` stack) | ✅ Done | `ChatRepository` + `ChatMessageRepository`; **room creation is client-side — flagged for backend move before production** |
| Identity verification (ID + selfie + ML Kit) | ✅ Done | upload → `verification/{uid}/`, PENDING → wali/admin approval |
| Trust badges (verified / guardian) in discovery | ✅ Done | mirrored from `users/{uid}` via Cloud Functions |
| Guardian invitation + wali binding | ✅ Done | requires **verified** wali email |
| Photo privacy requests + approved-viewer list | ✅ Done | Storage + Firestore rules; **note: legacy `imageUrl` blur is UI-only — see TECH_DEBT** |
| Admin console (users, premium, roles, verification) | ✅ Done | via callable Cloud Functions (admin-gated) |
| Admin moderation — reports & photo review | ✅ Done | `AdminModerationRepository`, 3-layer admin gating |
| Admin moderation — ban / suspend | 🟡 Foundation | **records state only; NOT enforced** anywhere (`Moderation.kt`, `AdminModerationScreen` TODO) |
| Chaperoned chat + wali logs | 🟡 Foundation | built on **legacy `chatRooms/*`** stack; the production messaging shell uses `chats/*` which has no wali logging → effectively orphaned for real users (see ARCHITECTURE §3.1) |
| Premium / subscription (Gold, Platinum) | 🖼️ UI-only | **simulated checkout, no Google Play Billing**; production purchase returns an error; only admin can grant premium |
| Daily chat limit (free vs premium) | 📋 Planned | `ChatLimitManager` exists but **has zero call sites** — not enforced |
| AI matchmaker / Gemini assistance | ✅ Done (debug) | release ships an empty Gemini key by design; intended to move behind a backend proxy |
| Adhan scheduling / prayer tracking | ✅ Done | exact-alarm based |
| Biometric app lock | ✅ Done | optional (only when device has biometrics enrolled) |
| Bidirectional block filtering in discovery | 📋 Planned | block exists, but discovery does not yet filter blocked users both ways |

## Known "before production" gaps (cross-reference)

- Move chat-room creation + chat-limit/premium gate to Cloud Functions (client-trusted today).
- Real billing integration for premium.
- Enforce ban/suspension.
- Finish the chat + onboarding migrations; retire legacy stacks.
- Fix UI-only photo blur (full-res bytes are still fetched/cached).

See [`TECH_DEBT.md`](./TECH_DEBT.md) for the engineering backlog behind these.
