# Mithaq — Feature Status Matrix

> Honest status of each user-facing feature. Legend:
> **✅ Done** (works end-to-end) ·
> **🟡 Foundation** (data/UI exists, enforcement or backend incomplete) ·
> **🖼️ UI-only** (screen exists, not wired to a real backend) ·
> **📋 Planned**.
>
> Canonical messaging system = **`chats/*` (new)**. `chatRooms/*` is legacy and not built upon.

| Feature | Status | Notes / evidence |
|---|---|---|
| Email/password auth + email verification | ✅ Done | `AuthViewModel`, gated by `isVerifiedEmailUser()` in rules |
| Password reset / email verification deep links | ✅ Done | `docs/auth/`, manifest deep links |
| Profile onboarding (structured question engine) | ✅ Done | `ui/onboarding/OnboardingFlow.kt`; legacy wizard not yet removed |
| Discovery / search from `publicProfiles` mirror | ✅ Done | `PublicProfileRepository`, server-owned mirror |
| Discover opposite-gender filter | ✅ Done | PR #40 — Discover now matches Search (needs functions deploy + backfill after merge) |
| Interest → photo → chat request flow | ✅ Done | deterministic ids, gated transitions in repos + rules |
| Likes / mutual match | ✅ Done | mutual computed server-side (`onLikeCreated`) |
| Text chat (**canonical `chats/*` stack**) | ✅ Done | `ChatRepository` + `ChatMessageRepository`; **room creation is client-side — flagged for backend move before production** |
| Identity verification (ID + selfie + ML Kit) | ✅ Done | upload → `verification/{uid}/`, PENDING → wali/admin approval |
| Trust badges (verified / guardian) in discovery | ✅ Done | mirrored from `users/{uid}` via Cloud Functions |
| Guardian invitation + wali binding | ✅ Done | requires **verified** wali email |
| Photo privacy (request + approved viewers) | ✅ Done | Storage + Firestore rules; unauthorized viewers now render a preset avatar — **real photo bytes are never loaded/cached** (PR #42) |
| Logout/login state isolation | ✅ Done | per-user ViewModel keys; no cross-account data leak on a shared device (PR #41) |
| Admin console (users, premium, roles, verification) | ✅ Done | via callable Cloud Functions (admin-gated) |
| Admin moderation — reports & photo review | ✅ Done | `AdminModerationRepository`, 3-layer admin gating |
| Admin moderation — ban / suspend | 🟡 Foundation | **records state only; NOT enforced** anywhere (`Moderation.kt`, `AdminModerationScreen` TODO) |
| Chaperoned chat + wali logs | 🟡 Foundation (legacy) | built on **legacy `chatRooms/*`**; the canonical `chats/*` stack has no wali logging yet. **Out of scope for launch** — re-implement on `chats/*` later (see TECH_DEBT / ARCHITECTURE §3.1) |
| Premium / subscription (Gold, Platinum) | 🖼️ UI-only | **simulated checkout, no Google Play Billing**; production purchase returns an error; only admin can grant premium. Phase 17 decision: **defer** |
| Daily chat limit (free vs premium) | 📋 Planned | `ChatLimitManager` exists but **has zero call sites** — not enforced |
| AI matchmaker / Gemini assistance | 🖼️ UI-only / debug | release ships an empty Gemini key by design. Phase 17 decision: **hide** unless a backend proxy exists |
| Voice call | 🖼️ UI-only | scaffolding only. Phase 17 decision: **hide** in first release |
| Adhan scheduling / prayer tracking | ✅ Done | exact-alarm based |
| Biometric app lock | ✅ Done | optional (only when device has biometrics enrolled) |
| Bidirectional block filtering in discovery | 📋 Planned | block exists, but discovery does not yet filter blocked users both ways |

## Known "before production" gaps (cross-reference)

- Move chat-room creation + chat-limit/premium gate to Cloud Functions (client-trusted today).
- Real billing integration for premium (Phase 19).
- Enforce ban/suspension.
- Re-implement chaperoned/wali monitoring on the canonical `chats/*` stack; retire legacy stacks.
- Hide incomplete features (Voice Call / Billing / Gemini) for the first release (Phase 17).

✅ **Resolved in Phase 16:** real photo privacy (PR #42), logout state isolation (PR #41),
Discover gender filter (PR #40).

See [`TECH_DEBT.md`](./TECH_DEBT.md) for the engineering backlog behind the open gaps.
