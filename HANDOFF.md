# Mithaq — Handoff

Living snapshot of **where the project stands**, **who does what**, and **what is in flight**.
Read this first when picking the project back up.

## Current state (summary)

The latest released version is **2.1.0** (`v2.1.0`, versionCode 21, PR #93). `main` is ahead of
that release through **PR #96**, so the Crashlytics cleanup, completed Premium Highlight, and
2x Exposure interleave are merged but belong to the next release unless `v2.1.0` is rebuilt.

The core Sharia-compliant flow works end to end:

> Mandatory onboarding → Discover / Search → Interest → Photo request → Chat request → Chat →
> Message → Report / Block → Admin moderation

- Onboarding is one mandatory **58-step** engine loaded from
  `assets/onboarding/onboarding_steps.json`. A consistency gate checks the JSON against the
  static fallback, and `mirrorProfileToUser` maps completed onboarding answers into
  `users/{uid}` for matching/search.
- Premium entitlements are implemented: **Incognito**, bounded **Boost** re-ranking,
  **Read receipts**, **Profile Highlight** (gold border), and deterministic **2x Exposure** in
  Discover. **Google Play Billing remains OFF**; premium is currently admin/server granted.
- The PR #84-#92 security sweep is enforced in code/rules/functions: photo-access revocation
  removes Storage access, Incognito profiles are excluded from Discover, blocks constrain
  protected interactions, admin deletion removes Auth/data/media, app lock honors the per-user
  setting, SUSPENDED/BANNED accounts are disabled and write-blocked, and chat requests plus the
  free daily cap are server-authoritative.
- Cloud Functions use the v2 modular API on **Node 22**, with
  `firebase-admin ^13.10.0` and `firebase-functions ^7.2.5`.
- Unhandled Android exceptions previously sent to `printStackTrace()` now report through
  Firebase Crashlytics.

## Canonical decisions

- **Messaging system = `chats/*` (new).** `chatRooms/*` is **legacy** and is **not** built
  upon. Chaperoned/wali logging currently lives only on the legacy stack and will be
  re-implemented on `chats/*` later (tracked in `docs/TECH_DEBT.md`).
- **Onboarding = the JSON-driven question engine.** The legacy onboarding wizard was removed
  in PR #75. Update the JSON and its validated fallback together.
- **Premium perks are live; billing is not.** Do not imply that the store performs a real
  purchase until Google Play Billing and server-side purchase verification ship.
- **Functions stay modular on Node 22.** Keep `firebase-admin` inside the compatible
  `firebase-functions` peer range.
- **No broad refactors during release work**: no Hilt migration, model consolidation, or
  unrelated movement of the large root-level screens.

## Roles

| Who | Role | Owns |
|---|---|---|
| **ChatGPT** | Planning | Roadmap, phase decisions, prioritization |
| **Codex** | Main implementation | Feature PRs, the bulk of app-code changes |
| **Android Studio** | Live testing | On-device runs, logcat, build, keystore, Firebase console |
| **Claude** | QA / fix support only | Reviews, small contained fixes, docs; **no large refactors** |

**Coordination rule:** any PR that touches `firestore.rules`, `storage.rules`, `functions/`, or
the data layer gets a QA review before merge. Pure-visual PRs merge after CI is green.

## Latest merged work

| PRs | Area | Current result |
|---|---|---|
| #74 | Likes | Heart action now toggles like / unlike correctly |
| #75-#77 | Onboarding + matching data | Single mandatory engine; onboarding answers mirrored server-side into `users/{uid}` |
| #76 | Adhan | Location requested on app open; prayer scheduling uses the user's location |
| #78-#81 | Premium + trust | Incognito, Boost, premium read receipts, and reusable Verified badge |
| #82-#83 | Data-driven onboarding | All 58 steps moved to JSON behind a static consistency gate |
| #84-#92 | Backend + security sweep | Node 22 modular Functions; revocation, blocks, deletion, app lock, moderation enforcement, and server-created limited chat requests |
| #93 | Release | Version 2.1.0 / versionCode 21 tagged as `v2.1.0` |
| #94-#96 | Post-release `main` | Crashlytics reporting, completed gold Premium Highlight, deterministic 2x Exposure |

## Pending operational steps

- Cut the next release from current `main` if PRs #94-#96 should ship; `v2.1.0` itself points
  to PR #93.
- Keep `BetaFeatureGates.PREMIUM_BILLING = false` until Google Play Billing and backend receipt
  verification are complete.
- Confirm each Firebase environment has the latest Functions, Firestore rules, and Storage
  rules deployed; repository state cannot prove deployment state.
- Complete release-signing / Play delivery checks and run the two-account on-device smoke test.

## Next up

Production billing and purchase verification, release validation, and eventual migration of
wali/chaperone monitoring from legacy `chatRooms/*` to canonical `chats/*`. See
[`ROADMAP.md`](./ROADMAP.md), [`docs/FEATURE_STATUS.md`](./docs/FEATURE_STATUS.md), and
[`docs/TECH_DEBT.md`](./docs/TECH_DEBT.md).
