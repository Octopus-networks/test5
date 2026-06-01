# Phase 11.5 Full App Testing and Bug Fixing

Scope: testing and bug fixing only. No notifications, billing, new onboarding questions, chat attachments, voice messages, Cloud Functions, guardian changes, prayer changes, or admin panel work were added.

## Tested flows

### Auth
- Reviewed register/email-verification/login/unverified-login/forgot-password/restart/sign-out code paths.
- Fixed in-app email verification links so the app applies Firebase `oobCode` before reloading the user.
- Fixed password reset deep-link handling by stopping the app from intercepting `/reset-password`; Firebase hosted reset flow remains responsible for password reset completion.

### Onboarding
- Reviewed `profiles/{userId}` save and completion-cache behavior.
- Fixed onboarding save to include the safe `publicSettings` fields consumed by the server-side `publicProfiles` mirror.
- Confirmed Android still writes private `profiles/{userId}` only; `publicProfiles` remains server-owned.

### Discover/Search
- Reviewed Home/Search loading through `PublicProfileRepository` and `publicProfiles`.
- Fixed repository fallback so the known app `currentUserId` is used even when Firebase `auth.currentUser` is absent in non-production/mock flows.
- Confirmed current user exclusion and block filtering remain in the public profile repository.

### Interest requests
- Reviewed duplicate request IDs, pending/accepted/declined/cancel handling, and blocked-user UI gating.
- No code changes beyond shorter blocked button labels.

### Photo requests
- Reviewed request/approve/decline/cancel handling and photo URL privacy assumptions.
- Confirmed no private photo URLs are written to `publicProfiles` by Android.

### Photo upload
- Reviewed `userPhotos/{userId}/photos/{photoId}` metadata and `user_photos/{userId}/{photoId}.jpg` upload path.
- Fixed viewer privacy-mode reads to use `publicProfiles/{ownerUserId}.photoPrivacyMode` instead of private `profiles/{ownerUserId}` for non-owner viewer flows.

### Chat request / Messages
- Reviewed chat request approve/decline/cancel and chat-room creation/reuse paths.
- Reviewed active chat/listener/send-text/last-message-preview paths.
- Fixed a rules-assumption mismatch by removing client-side writes to `notifications`, which rules deny and which are outside this phase.

### Report/block
- Reviewed report/block repository and UI gating.
- Confirmed old-message visibility is preserved by read/listener paths and blocked sending is disabled through `ChatMessageRepository.validateUserCanSend`.

### Localization/UI
- Parsed default and Arabic XML resources.
- Fixed missing Arabic resources for blocked request strings.
- Added short blocked button labels to avoid overflowing compact Discover/Search action buttons.

### Rules/docs
- Reviewed `firestore.rules` and `storage.rules` assumptions only; rules were not deployed.

## Bugs found and fixed

1. **Email verification deep links did not apply Firebase action codes**
   - Root cause: app captured `/verify-email` but only called `reload()`.
   - Fix: parse `oobCode` and call `FirebaseAuth.applyActionCode(oobCode)` before reload.

2. **Password reset links could be intercepted by the app without a reset-confirm UI**
   - Root cause: `/reset-password` intent filter routed to Login only.
   - Fix: remove app interception for reset links so Firebase hosted reset flow can complete.

3. **Discover/Search could be empty in mock/non-production auth mode**
   - Root cause: repository ignored the known app `currentUserId` and relied only on `FirebaseAuth.currentUser`.
   - Fix: pass and use `viewerUserId` fallback in `PublicProfileRepository`.

4. **New onboarding profiles did not provide mirror-safe `publicSettings`**
   - Root cause: onboarding saved private profile sections but omitted the safe settings consumed by Cloud Functions mirror.
   - Fix: save `hasGuardian`, `prayerRoutineShared`, `prayerHabitPublicLabel`, and `photoPrivacyMode` under `publicSettings`.

5. **Non-owner photo privacy reads tried private `profiles/{owner}`**
   - Root cause: `PhotoRepository.getPhotoPrivacyMode` read private profile docs for viewers.
   - Fix: owner reads private profile; non-owner viewer flows read public mirror only.

6. **Messages attempted denied client notification writes**
   - Root cause: `ChatMessageRepository` queued `notifications` from Android, but rules deny notification creates.
   - Fix: removed the client notification write attempt.

7. **Arabic blocked-request strings were missing and blocked labels were too long for buttons**
   - Root cause: Arabic resource parity gap and sentence-length button label.
   - Fix: add Arabic resources and use short `Blocked` / `محظور` labels for compact buttons.

## Rules assumptions documented for follow-up

These were reviewed but not changed/deployed in Phase 11.5:

- `userPhotos/{owner}/photos/{photoId}` metadata reads are owner/admin-only today. If approved viewers must load real approved photos directly from the client, rules need a narrowly scoped approved-viewer metadata read path.
- Storage approved-viewer reads should ideally validate the matching photo metadata (`status == approved`) for the specific `photoId`, or production should move to a backend/signed-access flow.
- Reciprocal block enforcement is strongest in app logic today; production rules/backend validation should enforce blocks for request/message writes because a blocked user may not be able to read the other user's block document.

## Verification

Local lightweight checks only; no local Gradle build was run because Mithaq Android builds should run in GitHub Actions.

Commands/checks:
- XML parse for default and Arabic `strings.xml`.
- Android resource-key parity check.
- `python3 scripts/verify_security_rules.py`.
- `npm --prefix functions run lint`.
- `npm --prefix functions test`.
- `git diff --check`.

GitHub Actions should be run on the Phase 11.5 branch when repository credentials are available.

## Remaining TODOs

- Run Android CI on GitHub Actions.
- If the product decision is to display approved photos directly from the client, update and emulator-test Firestore/Storage rules for approved-viewer photo metadata and specific approved photo files.
- Consider backend/signed-access photo delivery before production instead of broad direct Storage reads.
- Add rules/backend block enforcement for request/message writes before production hardening.
