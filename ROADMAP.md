# Mithaq — Roadmap

> One place to reconcile the project's planning. Historically, three independent numbering
> schemes were used and never mapped to each other:
> 1. **SemVer** in `CHANGELOG.md` (v1.0 → v2.0)
> 2. **Fine-grained "Phase N.M"** labels in `docs/security/firestore-rules.md`, QA reports,
>    and inline code comments (e.g. "Phase 11.12A", "Phase 12.2E")
> 3. **Coarse "Phase 1/2/3"** labels inside some model/code comments
>
> This file is now the **single source of truth**. The "Phase N.M" labels are the canonical
> ones; SemVer versions are coarse release snapshots layered on top.

## Where we are now

**Current focus:** Phase 12.2x and post-12 **security hardening** (latest merged work:
verified-email guardian/wali binding + tightened report/favorite/view rules; block-get
rules fix).

**Definition-of-done status:** the two most recent QA gates are still formally open —
QA Phase 11.5 is "code-complete pending on-device verification" and Phase 12.2 is
"not closeable by automated review alone." Both need an on-device pass.

## Phase timeline (reconstructed, canonical)

| Phase | Theme | Status |
|---|---|---|
| 1 | Email-verification gating (auth) | ✅ Done |
| 2–3 | Islamic identity/trust fields + prayer tracking | ✅ Done |
| 4.5 | `publicProfiles` sanitized discovery mirror (server-owned) | ✅ Done |
| 5 | Discover/Search read from the mirror | ✅ Done |
| 6.1 / 6.1.5 | Interest requests | ✅ Done |
| 6.2 | Photo-access requests | ✅ Done |
| 6.3 | Chat requests | ✅ Done |
| 7 / 7.5 | Chat-room creation after approval (`chats/*`) | ✅ Done (client-side — see risks) |
| 8 / 8.5 / 8.6 / 8.8 | Text messages, blocks/reports, realtime listeners, notification queue | ✅ Done |
| 11 / 11.8 | Secure photo metadata + privacy closure | ✅ Done |
| 11.5 | Full end-to-end QA | 🟡 Code-complete, on-device gate open |
| 11.11 / 11.12A | Structured question-engine onboarding | ✅ Done (legacy flow not yet removed) |
| 12 / 12.1 / 12.2 / 12.2E | Admin & moderation foundation, safer deploy, photo moderation | 🟡 Foundation done, enforcement open |
| 12.x+ (security hardening) | Verified-email wali binding, rule tightening, block-get fix | ✅ Merged |

## Planned next (open items, from code TODOs and QA reports)

These are the explicit "before production" items not yet closed. See
[`docs/FEATURE_STATUS.md`](./docs/FEATURE_STATUS.md) and [`docs/TECH_DEBT.md`](./docs/TECH_DEBT.md)
for detail.

1. **Enforce ban/suspension** across sign-in, discovery, and chat (currently recording-only).
2. **Move chat-room creation and the chat-limit/premium gate to Cloud Functions** (currently
   client-side and client-trusted).
3. **Real billing** for premium (the in-app store is a simulated checkout today).
4. **Migrate admin detection to Firebase custom claims** (currently a Firestore `isAdmin` field).
5. **Bidirectional block filtering** in discovery.
6. **Retire the legacy onboarding + legacy `chatRooms/*` chat stack** (finish the migration —
   see [`docs/ARCHITECTURE.md`](./docs/ARCHITECTURE.md) §3).
7. **Re-implement chaperoned/wali monitoring** on the new `chats/*` stack.

## Coherence note

The backend phasing (4.5 → 8.8) is genuinely sequential and well-disciplined. The main
sources of confusion this roadmap resolves are (a) the three numbering schemes above and
(b) an architectural migration frozen mid-flight (two chat stacks, two onboarding flows).
Closing items 6–7 above completes that migration.
