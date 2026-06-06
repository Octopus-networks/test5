# Mithaq — Roadmap (Launch Readiness)

The single source of truth for project planning. This roadmap is **phase-based** (Phase 0 →
Phase 21). The dated SemVer sections in `CHANGELOG.md` are a release narrative layered on top.

## Where we are now

The core app is **live and working**: the real marriage flow runs end to end
(Interest → Photo request → Chat request → Chat → Message → Report → Block → Admin
moderation), Firestore rules and Cloud Functions are deployed, and the live smoke test is
closed. We are **not building from scratch** — we are in:

> **Launch Readiness + Security Hardening + UI polish + Google Play preparation.**

**Canonical decisions**
- **Messaging system = `chats/*` (new).** `chatRooms/*` is **legacy** and is not built upon.
- Roles & ownership: see [`HANDOFF.md`](./HANDOFF.md).

**Current focus:** Phase 16 security hardening is **done** (PRs #40/#41/#42, awaiting merge +
deploy). Next up: hide incomplete features (Phase 17); App Check (13) and release keystore (14)
are manual.

---

## Phases

### Phase 0 — Repo organization & protection
Foundation before any large work.
- ✅ CI, debug-APK build, CodeQL, force-push protection, commit-message validation, PR workflow.
- ✅ Firebase auto-deploy made safe (functions are **not** auto-deployed).
- ⬜ Closeout docs (CHANGELOG / ROADMAP / HANDOFF) — **in progress (this PR)**.

### Phase 11–12 — Functional & security foundation
The real Sharia-compliant flow: interest, photo request, chat request, chat, report, block, admin.
- ✅ Phase 11 secure photos · 11.5 QA fixes · 12 admin moderation foundation · 12.1 safer
  deploy · 12.1B CodeQL fix · 12.2 live smoke test.
- ✅ PRs #28–#37 (pending-photo index, request/chat/block rules, refresh, admin photos, chat
  list fix, send-message crash fix, block pre-check, security hardening + functions deployed).
- ✅ Live smoke test closed (chat opens, messages send, report reaches admin, block works).
- **No blocking items remain here.**

### Phase 12.6 — Antigravity / UI polish
Improve Discover / Profile Card visuals only; no logic.
- ✅ Antigravity polished Discover/Profile cards · PR **#35** open (single file
  `MithaqDiscoverScreen.kt`; no Firebase/ViewModel/Repository/Rules), CI green.
- ⬜ **Decision: merge PR #35 now, or defer for a full Stitch theme?** (lean: merge if the look
  is acceptable; defer full Stitch assets — MCP cannot export/download assets).

### Phase 13 — App Check + release foundation  ·  ⬜ manual
- ⬜ Register the Android app in Firebase App Check; enable Play Integrity for release.
- ⬜ Add debug App Check tokens for test devices; verify callable functions still pass.
- ⚠️ Do not enable App Check enforcement suddenly without debug tokens, or local tests break.

### Phase 14 — Release keystore / signing  ·  ⬜ manual (critical)
- ⬜ Create upload/release keystore; store it + passwords securely.
- ⬜ Add the release signing config; build release APK/AAB; record SHA-1/SHA-256; add SHAs to Firebase.
- ⚠️ A lost/incorrect keystore makes Play Store updates a serious problem.

### Phase 15 — Auth / email / app links  ·  ⬜ manual
- ⬜ Decide first-release auth: Google-only vs Email/Password too (lean: **Google-only** for Beta).
- ⬜ Customize email-verification + password-reset templates; authorized domains; `assetlinks.json`;
  link `mithaq.app` if used.

### Phase 16 — Security hardening before Beta  ·  ✅ DONE
The most important phase after the current stability. All three priorities shipped:
- ✅ **Real photo privacy** — unauthorized viewers render a preset avatar; real photo bytes are
  never loaded or disk-cached (PR **#42**).
- ✅ **User state isolation** — per-user ViewModel keys; no cross-account leak on logout/login (PR **#41**).
- ✅ **Discover gender filter** — opposite-gender only, like Search (PR **#40**; needs functions
  deploy + `publicProfiles` backfill after merge).

### Phase 17 — Incomplete-features decision  ·  ⬜ next (code)
Prevent half-finished screens from shipping in the first release.
- ⬜ Voice Call → **hide** in the first release.
- ⬜ Premium Billing → **defer** if Beta is free.
- ⬜ Gemini AI → **hide** unless a backend proxy exists.

### Phase 18 — Notifications / FCM  ·  ⬜ after hardening
- ⬜ Interest / photo-request / chat-request / new-message notifications (report/admin later).
- ⬜ Test on a real device.

### Phase 19 — Premium / billing  ·  ⬜ after Beta (or before launch if monetization is required)
- ⬜ Google Play Billing · purchase-verification backend · `setUserPremium` from backend only ·
  restore purchases · entitlement checks.

### Phase 20 — Google Play readiness  ·  ⬜
- ⬜ Privacy Policy · Terms · Data Safety form · content rating · store listing · app icon ·
  screenshots · feature graphic · closed testing · release AAB.

### Phase 21 — Final data / Firebase cleanup  ·  ⬜ before Beta
- ⬜ Remove unneeded test users · clean stale collections (`likes` / `chatRooms` / `profile_views`
  if unused) · backfill `publicProfiles` if needed · review indexes · review rules.
- ⚠️ No full reset now — reset happens just before Beta, after test accounts are fixed.

---

## Priority order (short)

| # | Item | Status |
|---|------|--------|
| 1 | Decide PR #35 | ⏳ |
| 2 | Update docs / roadmap / handoff | ⏳ this PR |
| 3 | App Check setup (manual) | ⬜ |
| 4 | Release keystore (manual) | ⬜ |
| 5 | Real photo privacy | ✅ #42 |
| 6 | Logout state isolation | ✅ #41 |
| 7 | Discover gender filter | ✅ #40 |
| 8 | Hide voice / billing / Gemini if not ready | ⬜ next |
| 9 | Google Play readiness | ⬜ |
| 10 | Notifications | ⬜ |
| 11 | Billing | ⬜ |
| 12 | Full Stitch / Antigravity theme assets | ⬜ |

## Open PRs

See [`HANDOFF.md`](./HANDOFF.md) for the live PR table and status.
