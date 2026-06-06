# Mithaq — Technical Debt & Engineering Backlog

Consolidated backlog from the architecture and code reviews. Grouped by theme; each item
has a rough priority (🔴 high / 🟠 medium / 🟡 low).

## A. Architecture / organization

- 🔴 **Consolidate model packages.** Delete `model/` and make `domain/model/` the single
  source of truth. Reconcile the two `ChatRoom` classes (zero shared fields) into one;
  decompose the ~150-field `model/UserProfile` god class into focused sub-models
  (`PrayerStats`, `AdhanSettings`, `GuardianInfo`, `AppearanceProfile`, …).
- 🔴 **Finish the dual-stack migrations.** Pick the new `chats/*` chat stack and the new
  structured onboarding as canonical; retire `chatRooms/*` + `ChaperonedChatViewModel` and
  `OnboardingWizardScreen`. Re-implement wali/chaperone monitoring on `chats/*`. (See
  [ARCHITECTURE.md](./ARCHITECTURE.md) §3.)
- 🟠 **Introduce dependency injection (Hilt).** Today repositories are `new`-ed with
  default-arg constructors and sometimes constructed directly inside Composables
  (`remember { LikesRepository(context) }`). Add Hilt and constructor-inject repositories;
  provide a single Firebase wiring point.
- 🟠 **Move the 8 root files into feature packages** (`Tabs`, `SearchTab`, `ChatTab`,
  `ProfileSettings`, `WaliDashboard`, `AiMatchmakerScreen` → their `ui/<feature>`;
  `Config` → `core/config`; `data/LikesRepository` → `data/repository`).
- 🟠 **Pull Firestore out of the presentation layer.** `FirebaseFirestore.getInstance()`
  appears ~57 times across 27 files, including Composables and the Activity (e.g.
  `ProfileSettings.kt` performs direct Firestore writes). Route all of it through repositories.
- 🟡 **Standardize screen naming** on `*Screen` (retire `*Tab` / `*Dashboard` / `*Settings`).
- 🟡 **Split god classes** (`AuthViewModel` ~2.3k LOC, `MainActivity` ~1.7k LOC) by
  responsibility.

## B. Security / correctness (from code review)

> ✅ **Phase 16 resolved (shipped, pending merge):** real photo privacy (PR #42), cross-user
> state leak (PR #41), and the Discover gender filter (PR #40). Kept below (struck through) for
> history. The remaining items are still outstanding.

- ✅ ~~**Photo blur is UI-only.**~~ **Shipped (PR #42).** Unauthorized viewers now render a
  preset avatar in `UserProfileImage.kt`; the real photo URL is handed to Coil only when the
  viewer is authorized, so the bytes are never loaded or disk-cached. *Follow-up debt:* prefer
  per-request signed Storage URLs over a stored durable `downloadUrl` so the URL string itself
  isn't a standing leak.
- ✅ ~~**Cross-user state leak on sign-out.**~~ **Shipped (PR #41).** Screen ViewModels are now
  keyed by uid (`viewModel(key = "...-$uid")`) and `SearchViewModel` via
  `remember(uid) { ... }`, so a new user gets fresh state.
- 🔴 **Chat limit + premium are client-trusted.** `ChatLimitManager` has no call sites and
  reads `isPremium` from the client. Enforce the limit and premium check in a Cloud Function
  at chat-room creation.
- 🟠 **GeminiService** sends real candidate `uid`/`name` and unsanitized user free-text into
  prompts (PII + prompt-injection risk). Minimize PII, delimit/escape user text, validate
  returned `recommended_uids` against what was sent.
- 🟠 **Two chat subsystems with divergent gating.** `ChaperonedChatViewModel` writes
  `chatRooms/*` directly without the email-verified/participant checks the `chats/*` path
  has; failed sends are swallowed silently. (Subsumed by the migration item in A.)
- ✅ ~~**Discovery does not filter by gender** while search does.~~ **Shipped (PR #40).**
  `getDiscoverProfiles` now filters to the opposite gender (mirrored into `publicProfiles`).
  *Still outstanding:* **bidirectional block filtering** in discovery.
- 🟠 **Swallowed exceptions** across repositories hide `PERMISSION_DENIED` from the tightened
  rules as "no data". Distinguish and surface permission errors.
- 🟠 **Unbounded message queries** (`ChatMessageRepository`) lack `limit`; add
  `limitToLast(N)` + pagination.
- 🟠 **Room uses `fallbackToDestructiveMigration()`** with no migrations → cache wipe on every
  schema bump. Set `exportSchema = true` and add migrations.
- 🟡 Biometric `onAuthenticationFailed()` is routed to the same callback as terminal errors;
  separate them and never auto-grant on error (debug auto-grant aside).
- 🟡 `profile_views`/`favorites` write client `System.currentTimeMillis()` instead of
  `FieldValue.serverTimestamp()`.
- 🟡 Camera capture contract grants `READ` on the output URI unnecessarily (grant `WRITE` only).
- 🟡 Remove dead code: `ChatLimitManager` (if not wired), `createLiveChatRoom`,
  `recordNewChatInitiated`.

## C. Testing / CI

- 🟠 **Automated test coverage is one unit test** (`MatchScoreCalculatorTest`) for ~36k LOC.
  Add ViewModel tests and Firestore-rules tests (`@firebase/rules-unit-testing`).
- 🟡 CI builds debug only; add a release (`assembleRelease`) build to validate ProGuard/R8 and
  reflection paths (e.g. `debugAppCheckProviderFactory`).

## D. Documentation hygiene

- 🟡 Keep the docs/ tree and the feature-status matrix current as items above close.
- 🟡 Cut real git tags if SemVer is to be used (none exist today).
