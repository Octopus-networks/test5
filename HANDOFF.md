# Mithaq — Handoff

Living snapshot of **where the project stands**, **who does what**, and **what is in flight**.
Read this first when picking the project back up.

## Current state (summary)

The core app is **live and working**. The Sharia-compliant flow runs end to end:

> Interest → Photo request → Chat request → Chat → Message → Report → Block → Admin moderation

- Firestore **rules** and Cloud **Functions** are deployed; the live smoke test is closed.
- **Phase 16 security hardening is complete** (photo privacy, logout state isolation, Discover
  gender filter) — shipped in PRs #40/#41/#42, **awaiting merge + deploy**.
- We are in **Launch Readiness** (see [`ROADMAP.md`](./ROADMAP.md)), not feature-building.

## Canonical decisions

- **Messaging system = `chats/*` (new).** `chatRooms/*` is **legacy** and is **not** built
  upon. Chaperoned/wali logging currently lives only on the legacy stack and is **out of scope
  for launch**; it will be re-implemented on `chats/*` later (tracked in `docs/TECH_DEBT.md`).
- **No new refactors right now**: no Hilt, no model consolidation, no moving the 8 root files,
  no app-code refactor, no Firebase deploy outside the documented launch steps.

## Roles

| Who | Role | Owns |
|---|---|---|
| **ChatGPT** | Planning | Roadmap, phase decisions, prioritization |
| **Codex** | Main implementation | Feature PRs, the bulk of app-code changes |
| **Android Studio** | Live testing | On-device runs, logcat, build, keystore, Firebase console |
| **Claude** | QA / fix support only | Reviews, small contained fixes, docs; **no large refactors** |

**Coordination rule:** any PR that touches `firestore.rules`, `functions/`, or the data layer
gets a QA review before merge. Pure-visual PRs (e.g. Antigravity UI) merge after CI is green.

## Open PRs

| PR | Title | Type | CI | Action |
|---|---|---|---|---|
| #35 | Antigravity Discover UI polish | UI-only | 🟢 | Decision: merge or defer for full theme |
| #38 | Docs: architecture, roadmap, feature-status, cleanup | docs | 🟢 | **this PR** |
| #39 | Move LikesRepository → data/repository | refactor | 🟢 | **Deferred / optional — not a launch blocker; do not merge now** |
| #40 | Discover opposite-gender filter | security | 🟢 | Merge → then `firebase deploy --only functions` + run `scripts/backfill_public_profiles.js` |
| #41 | Per-user ViewModel isolation (logout/login) | security | 🟢 | Merge |
| #42 | Real photo privacy (avatar for unauthorized) | security | 🟢 | Merge |

Suggested merge order to minimize conflicts: **#38 → #42 → #40 → #41 → #35** (and #39 stays
parked). #39 and #41 both touch `MainActivity` on different lines; whichever merges second may
need a trivial rebase — CI will catch any conflict.

## Pending manual steps (not code)

- **After merging #40:** deploy functions, then run the `publicProfiles` backfill.
- **Phase 13 — App Check:** register the app, enable Play Integrity for release, add debug
  tokens *before* enforcing (or local tests break).
- **Phase 14 — Release keystore:** create + securely store the upload keystore; add SHA-1/-256
  to Firebase. (Losing it breaks Play updates.)

## Next up

Phase 17 — hide incomplete features for the first release (Voice Call: hide · Billing: defer ·
Gemini: hide unless a backend proxy exists). See [`ROADMAP.md`](./ROADMAP.md) and
[`docs/FEATURE_STATUS.md`](./docs/FEATURE_STATUS.md).
