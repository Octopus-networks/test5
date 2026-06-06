# Mithaq — Architecture

> Status: **living document**. This file describes the architecture *as actually built*,
> including the in-progress migrations. Where reality differs from the ideal, that is
> called out explicitly rather than hidden.

## 1. High-level shape

Mithaq is a single-module Android app (`:app`) plus a Firebase Cloud Functions package
(`functions/`). There is no multi-module Gradle setup.

```
Android app (Kotlin, Jetpack Compose)
  └── talks to Firebase (Auth, Firestore, Storage, FCM) directly and via callable Functions
Firebase Cloud Functions (Node 20)
  └── privileged/admin operations + Firestore triggers (mirrors, notifications)
Firestore + Storage security rules
  └── the real authorization boundary (the client is not trusted)
```

The **security rules + Cloud Functions are the real trust boundary.** The Android client is
treated as untrusted: privileged fields (`isAdmin`, `isPremium`, `verificationStatus`,
`wardUid`, …) can only be changed by Cloud Functions running with the Admin SDK.

## 2. Intended layering (MVVM)

The newer code follows MVVM with a repository layer:

```
ui/<feature>/*Screen.kt        Composables (presentation)
ui/<feature>/*ViewModel.kt     State holders (StateFlow), call repositories only
data/repository/*Repository.kt Data sources, map Firestore <-> domain models
domain/model/*.kt              Plain data classes (the intended source of truth)
data/local/MithaqDatabase.kt   Room cache (offline)
```

> ⚠️ **The README badge says "MVVM + Clean Architecture". This is aspirational.** There is
> **no domain/use-case layer** (only data classes live in `domain/`), **no dependency-injection
> framework** (no Hilt/Koin — repositories are constructed with default-argument constructors
> and sometimes `new`-ed directly inside Composables), and **several screens/the Activity call
> Firestore directly**. The accurate description today is "MVVM, partially adopted." See
> [`docs/TECH_DEBT.md`](./TECH_DEBT.md) for the migration backlog.

## 3. ⚠️ Two parallel subsystems exist (read this before touching chat or onboarding)

The codebase is mid-migration between an older and a newer design. **Both versions are
present and compiled.** Knowing which one is canonical for production is essential.

### 3.1 Chat / messaging — TWO stacks

| | Legacy stack | New stack (canonical for production) |
|---|---|---|
| Firestore root | `chatRooms/{roomId}` + `messages` + `waliLogs` | `chats/{chatId}` + `messages` |
| Domain model | `model/ChatRoom.kt` (`roomId`, `memberIds`, `isChaperoned`, `waliEmail`, `dailyPrayers`, …) | `domain/model/ChatRoom.kt` (`chatId`, `participantIds`, `status`, `guardianApprovalStatus`, …) |
| ViewModel | `ui/chat/ChaperonedChatViewModel.kt` | `ui/messages/ChatRoomsViewModel.kt`, `ChatMessageViewModel.kt` |
| Repository | (writes Firestore directly) | `data/repository/ChatRepository.kt`, `ChatMessageRepository.kt` |
| Reached from | `WaliDashboard.kt` (wali read-only view) | `MithaqMainExperience` → `MithaqMessagesScreen` (the shell new users see) |

**The two `ChatRoom` classes share zero field names.** A "chat room" therefore means two
different shapes depending on the screen. The production messaging shell uses the **new
`chats/*` stack, which currently has NO wali logging / chaperone monitoring** — so the
README's "chaperoned chat with wali logs" feature is effectively orphaned for real users
(it lives only on the legacy `chatRooms/*` path).

**Action required (tracked in TECH_DEBT):** pick one stack. The recommended target is the
new `chats/*` stack; chaperone/wali logging needs to be re-implemented on top of it, and the
legacy `model/ChatRoom` + `ChaperonedChatViewModel` retired.

### 3.2 Onboarding — TWO flows

| | Legacy flow | New flow (canonical) |
|---|---|---|
| Entry | `ui/onboarding/OnboardingWizardScreen.kt` | `ui/onboarding/OnboardingFlow.kt` + `QuestionScreen.kt` (structured "question engine", Phase 11.12A) |
| Models | scattered profile fields | `domain/model/OnboardingModels.kt` |
| Routing | edge branch in `MainActivity.kt` | primary path for new users (`MainActivity` → `Routes.OnboardingQuestion`) |

`MainActivity.kt` carries `// TODO: Old onboarding will be replaced after the new question
engine is fully tested.` The legacy `OnboardingWizardScreen` is still imported by several
files and reachable in an edge branch; QA Phase 11.5 asked for its removal. Until removed,
**add new onboarding logic to the new flow only.**

## 4. Duplicate model packages

There are two model packages:

- `model/` — **legacy.** `UserProfile.kt` (a ~150-field "god" data class), `ChatRoom.kt`
  (legacy shape), `FilterCriteria.kt`.
- `domain/model/` — **newer, source of truth.** Small focused classes (`PublicProfile`,
  `ChatMessage`, `ChatRoom`, `InterestRequest`, `PhotoRequest`, `UserBlock`, `UserReport`,
  `Moderation`, `OnboardingModels`, …).

**Target:** consolidate everything into `domain/model/`, decompose the god `UserProfile` into
focused sub-models, reconcile the two `ChatRoom` shapes into one, and delete `model/`.
Tracked in [`docs/TECH_DEBT.md`](./TECH_DEBT.md).

## 5. Data ownership & key collections

| Collection | Owner | Notes |
|---|---|---|
| `users/{uid}` | user (limited) + Cloud Functions | privileged fields are server-only; rules block client privilege escalation |
| `profiles/{uid}` | user | private onboarding data (owner/admin read only) |
| `publicProfiles/{uid}` | **server only** | sanitized discovery mirror, written by `mirrorPublicProfile` / `mirrorPublicProfileOnUserChange` Cloud Functions; all client writes denied |
| `interestRequests` / `photoRequests` / `chatRequests` | participants | deterministic id `fromUid_toUid`; gated state transitions |
| `chats/{chatId}` + `messages` | participants | new messaging stack; created only after an approved chat request |
| `chatRooms/{roomId}` + `messages` + `waliLogs` | participants + wali | **legacy** messaging stack (read-only create on client) |
| `likes` | user | mutual match computed server-side (`onLikeCreated`), `isMutual` server-owned |
| `reports` / `blocks` | reporter/blocker | strict field allow-lists |
| `notifications` | **server only** | created by Cloud Functions; client may only flip status to DELIVERED/READ |
| `adminAuditLogs` / `userModeration` | admin (server) | admin-only |

## 6. Cloud Functions (`functions/index.js`)

Callable (App Check enforced):
- `setVerificationStatus` — admin or assigned wali sets a user's verification status.
- `setUserPremium` — admin sets premium tier (the only working premium-grant path).
- `setUserRole` — admin sets `isWaliAccount` / `isAdmin`.
- `deleteUserProfile` — admin deletes a user doc.

Firestore triggers:
- `onLikeCreated` — computes mutual matches, sends notifications.
- `onChatMessageCreated` — sends a (generic, privacy-preserving) new-message notification.
- `mirrorPublicProfile` — mirrors `profiles/{uid}` → `publicProfiles/{uid}` (sanitized).
- `mirrorPublicProfileOnUserChange` — re-mirrors when `verificationStatus`/`guardianStatus`
  on `users/{uid}` changes, so discovery badges stay current.

## 7. Security model (summary)

- Email verification (`isVerifiedEmailUser`) gates most user actions.
- Guardian/wali binding requires a **verified** email (`email_verified == true`) so a wali
  must prove inbox ownership before gaining access to a ward's data.
- App Check (Play Integrity in release) is enforced on callable functions.
- See [`docs/security/firestore-rules.md`](./security/firestore-rules.md) for the deployed
  rule set.

## 8. Build & CI

- Single Gradle module `:app`; Cloud Functions built/deployed separately.
- GitHub Actions: build debug APK + run unit tests on PRs; CodeQL; branch protection
  (no force-push to `main`); Firebase deploy workflow.
- See [`docs/ops/deploy.md`](./ops/deploy.md).
