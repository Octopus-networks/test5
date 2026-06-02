# QA Phase 11.5 — Full End-to-End Test & Bug Fix Report

**App:** Mithaq (privacy-first Islamic marriage workflow)
**Repository:** Octopus-networks/test5
**QA branch:** `phase-11-5-qa-e2e` (off `main` @ `4ac5f17`)
**Date:** 2026-06-03

> ⚠️ **Honesty note on test method.** This QA was performed in a CLI environment **without a device/emulator or Firebase test credentials**, so the two-account flow could **not** be exercised interactively. Per the task's fallback instructions, this report is a **rigorous static code-path + security-rules + Cloud-Functions audit** of every step in the scenario, **two confirmed critical bugs were fixed**, and a **manual test checklist** is provided for on-device verification. No flow is claimed to have been "run" — each item below is marked **[STATIC ✅]** (verified correct by code review), **[BUG]** (defect found), or **[MANUAL]** (must be confirmed on a device).

---

## 1. Test environment

| Item | Value |
|---|---|
| Host | Windows, CLI (no emulator/device attached) |
| JDK | OpenJDK 21 (Android Studio JBR: `C:\Program Files\Android\Android Studio\jbr`) |
| Build cmd | `.\gradlew.bat --no-daemon --offline --console=plain :app:assembleDebug` |
| Main HEAD | `4ac5f17` (includes the Phase 11.12A structured-onboarding merge, PR #21) |
| Firebase | Rules/Functions reviewed from repo source; **not redeployed by this QA** |

> **Important context:** `main` now includes **Phase 11.12A structured onboarding** (merged just before this QA). That merge changed the shape of the `profiles/{uid}` write **without** updating Firestore rules or the public-profile mirror — which is the source of the two critical bugs below.

## 2. Test accounts used

Interactive run not performed, so **no live accounts were created**. For the manual checklist use two throwaway verified accounts; record only the **UIDs** (never passwords) when you run it:

| Role | Email (example) | UID (fill in) |
|---|---|---|
| Account A (man seeking wife) | `qa-a@example.com` | `__________` |
| Account B (woman seeking husband) | `qa-b@example.com` | `__________` |

---

## 3. Passed flows (static verification)

| Flow | Result | Evidence |
|---|---|---|
| Register → Email verification gating | [STATIC ✅] | `AuthViewModel` routes unverified sign-in to `AuthState.EmailVerificationRequired`; protected routes blocked in `MainActivity` |
| Forgot password | [STATIC ✅] | `AuthViewModel.sendPasswordResetEmail` with action settings |
| Login with unverified email | [STATIC ✅] | Sign-in reloads user; if `!isEmailVerified` → verify screen, no app access |
| Photo upload storage path | [STATIC ✅] | `PhotoRepository.storagePathFor` = `user_photos/{uid}/{photoId}.jpg` |
| Photo metadata path | [STATIC ✅] | `userPhotos/{uid}/photos/{photoId}`; keys: photoId,userId,storagePath,type,status,visibility,createdAt,updatedAt — **no URL** |
| Photo never exposes a URL | [STATIC ✅] | No `downloadUrl`/`getDownloadUrl` anywhere; viewers get **bytes** via `getBytes` gated by Storage rules |
| Photo display follows privacy | [STATIC ✅] | `resolveAccessLevel` = privacy mode × approved photo request; HIDDEN→locked, BLURRED→full only if approved, etc. |
| publicProfiles is server-owned | [STATIC ✅] | `PublicProfileRepository` write method is a deprecated **no-op**; rules deny all client writes to `publicProfiles` |
| publicProfiles has no photo URLs | [STATIC ✅] | `buildPublicProfile` allow-lists safe fields only; no photoUrl/privatePhotoUrl/downloadUrl/storagePath |
| Interest request create/accept | [STATIC ✅] | `InterestRequestRepository` + rules: create id=`from_to`, status pending; recipient sets accepted/declined |
| Photo request needs accepted interest | [STATIC ✅] | Rule `hasAcceptedInterest(...)` required on `photoRequests` create |
| Chat request needs accepted interest | [STATIC ✅] | Rule `hasAcceptedInterest(...)` required on `chatRequests` create |
| Chat room only after approval | [STATIC ✅] | `chats` create requires `approvedChatRequestFor(...)`; `ChatRepository` checks status `approved` |
| Messages are text-only | [STATIC ✅] | Rule enforces `type == "text"`, size 1..1000; repo filters `type=="text"` |
| Last-message preview updates | [STATIC ✅] | `ChatMessageRepository.sendTextMessage` updates `lastMessagePreview`/`lastMessageAt` |
| Old messages remain after block | [STATIC ✅] | Messages have `allow delete: if false`; nothing deletes history |
| Sending disabled after block | [STATIC ✅] | `validateUserCanSend` → `isBlockedBetweenUsers` returns false → send blocked client-side |
| Block / Report docs | [STATIC ✅] | `blocks/{blocker_blocked}` {blockerId,blockedId,timestamp}; `reports` with reporterId==me — match rules |

## 4. Failed flows (root-caused)

| Flow | Result | Root cause |
|---|---|---|
| **Complete onboarding (save)** | [BUG-1 🔴] | `profiles/{uid}` rule `hasOnly` allow-list omits the 11.12A groups → write **denied** for verified users |
| **Account A appears in Home/Search** | [BUG-1 🔴 → BUG-2 🟠] | No profile write ⇒ no mirror ⇒ no `publicProfiles`. Even after BUG-1, mirror read wrong paths ⇒ blank name/city/country/marital status |
| **Restart app after onboarding** | [BUG-1 🔴 (consequence)] | `onboardingCompleted` never persists ⇒ user re-enters onboarding every launch |
| **Blocked user hidden in Discover/Search** | [BUG-4 🟠] | `DiscoverViewModel` performs **no** block filtering; block enforced only in chat |

---

## 5. Bugs found

### 🔴 BUG-1 (Critical) — Onboarding save denied by Firestore rules
- **Where:** `firestore.rules` → `match /profiles/{userId}` create/update `keys().hasOnly([...])`.
- **Cause:** Phase 11.12A `OnboardingRepository.buildProfileGroups()` writes top-level groups `location, family, educationWork, lifestyle, appearance, personality, partnerPreferences, privacyTrust` (see `OnboardingStorageGroup`). The rule allow-list only had `basicInfo, personalStatus, religiousPractice, marriageIntent` (+ meta). Any extra key makes `hasOnly` false → **PERMISSION_DENIED**.
- **Impact:** Onboarding completion never saves for real (verified) users → blocks `publicProfiles` mirror → **breaks the entire downstream E2E flow** (discovery, interest, photo, chat) and traps users in the onboarding loop on restart.
- **Severity:** Critical / release-blocking.

### 🟠 BUG-2 (High) — Public mirror reads pre-11.12A field paths
- **Where:** `functions/index.js` → `buildPublicProfile`.
- **Cause:** Read `basicInfo.name`, `basicInfo.city`, `basicInfo.country`, `personalStatus.maritalStatus`; but 11.12A writes `basicInfo.displayName`, `location.city`, `location.country`, `marriageIntent.maritalStatus`.
- **Impact:** Once BUG-1 is fixed, discovery cards show **blank** display name / city / country / marital status.
- **Severity:** High (discovery unusable display).

### 🟡 BUG-3 (Low) — Client write to `notifications` is denied
- **Where:** `ChatMessageRepository.queueMessageNotification` → `notifications.add(...)`; rule `notifications` `create: if false`.
- **Impact:** The client insert always fails, but it is wrapped in try/catch and **does not fail message send**. New-chat-path notifications are simply not delivered (the existing `onChatMessageCreated` Function only triggers on the legacy `chatRooms/*` path).
- **Severity:** Low. **Out of scope** (notifications excluded this phase). Not fixed.

### 🟠 BUG-4 (Medium) — Blocked users still appear in Discover/Search
- **Where:** `DiscoverViewModel` / `PublicProfileRepository.getDiscoverProfiles` — no block filtering. (Discover and Search both use `DiscoverViewModel`.)
- **Impact:** A member you blocked still shows in discovery/search. Chat sending IS blocked, so the core safety boundary holds, but the preamble's "block enforcement works across discovery" is **inaccurate**.
- **Severity:** Medium. **Not fixed this phase** — see "Bugs not fixed" for rationale.

## 6. Bugs fixed (this phase)

| Bug | Fix | File | Weakens security? |
|---|---|---|---|
| BUG-1 | Added the 8 Phase-11.12A storage-group keys to the `profiles` write allow-list | `firestore.rules` | **No** — still owner-only + email-verified; data stays private to owner/admin; no other collection affected |
| BUG-2 | Read `basicInfo.displayName`, `location.city/country`, `marriageIntent.maritalStatus` (with legacy fallbacks); same output fields | `functions/index.js` | **No** — identical allow-listed output set; only public-safe fields; no new/private fields added |

Both are minimal and **require redeploy to take effect** (see TODOs). `node --check functions/index.js` → syntax OK.

## 7. Bugs not fixed (with rationale)

- **BUG-3 (notifications):** Out of scope ("Do not add notifications"); does not break the tested flow.
- **BUG-4 (discovery block filter):** Deferred deliberately. A *correct* filter must hide **both** directions (users I blocked **and** users who blocked me). Firestore rules only let a client read its **own** blocks (`blocks` read requires `blockerId == auth.uid`), so the "blocked-me" direction needs server denormalization or a Cloud Function — which exceeds "minimal fix / no new Cloud Functions". A one-directional client filter would give false confidence. Recommended as a dedicated follow-up. Core safety (cannot message a blocked user) already holds.

---

## 8. Steps to reproduce the key issues

**BUG-1 (onboarding save denied):**
1. Register + verify a new account on a build pointing at the deployed rules.
2. Complete the structured onboarding to the summary, tap Continue/Finish.
3. **Observed:** save fails (Firestore `PERMISSION_DENIED`); profile not persisted; on relaunch onboarding restarts.
4. Logcat filter: `Firestore` / `PERMISSION_DENIED`. Firestore console: `profiles/{uid}` absent or not updated.

**BUG-2 (blank discovery card):** After deploying the BUG-1 rule fix only (not BUG-2), complete onboarding on A, then view A from B's Home — name/city/country/marital status render empty.

**BUG-4 (block not filtered in discovery):** A blocks B in chat → A opens Discover/Search → B still appears.

---

## 9. Firestore / Storage observations (invariants)

| Invariant | Status | Notes |
|---|---|---|
| `profiles/{uid}` is private | ✅ | Read: owner(verified) or admin only |
| `publicProfiles/{uid}` safe fields only | ✅ | Allow-listed in `buildPublicProfile` |
| `publicProfiles` has no `photoUrl`/`privatePhotoUrl`/`downloadUrl`/`storagePath` | ✅ | Confirmed absent in mirror output and model |
| `userPhotos/{uid}/photos/{photoId}` metadata exists, owner-only write | ✅ | No URL stored; storagePath validated against `user_photos/{uid}/{photoId}.jpg` |
| Storage `user_photos/{uid}/{photoId}` gated | ✅ | Read = owner/admin/approved photo request; write = owner, <5MB, image/* |
| `photoRequests` status transitions | ✅ | pending → approved/declined (owner), cancelled (requester); needs accepted interest |
| `chatRequests` status transitions | ✅ | pending → approved/declined (recipient), cancelled (sender); `createdChatId` patch allowed |
| `chats/{chatId}` only after approved chat request | ✅ | `approvedChatRequestFor(...)` required at create |
| `chats/{chatId}/messages` text only | ✅ | `type=="text"`, 1..1000 chars, status `sent`, no delete |
| `blocks` / `reports` created correctly | ✅ | Shapes match rules; reports admin-read only |
| Onboarding write reaches `profiles` only (no client `publicProfiles` write) | ✅ (after BUG-1 fix) | Client never writes `publicProfiles` |

---

## 10. CI / build result

- **Command:** `.\gradlew.bat --no-daemon --offline --console=plain :app:assembleDebug` (JAVA_HOME = Android Studio JBR / JDK 21).
- **Result:** ✅ **BUILD SUCCESSFUL in 2m 30s** (41 tasks; up-to-date — Kotlin sources unchanged by this QA). APK assembled.
- **Note:** The two fixes touch `firestore.rules` and `functions/index.js` only — **neither is part of the Gradle/Android build**, so the APK build result reflects the merged `main` (Kotlin unchanged). `functions/index.js` passed `node --check`.

---

## 11. Remaining TODOs

1. **Deploy the fixes** (required for them to take effect):
   `firebase deploy --only firestore:rules,functions`
2. **Run the manual two-account checklist (§12) on a device/emulator** after deploy — interactive verification was not possible in this environment.
3. **BUG-4 follow-up:** discovery/search block filtering (needs server support for the bidirectional case).
4. **BUG-3 / notifications (out of scope):** new `chats/*` message path has no server notification trigger; the legacy `onChatMessageCreated` only watches `chatRooms/*`.
5. **Legacy cleanup (prior phase):** remove `OnboardingWizardScreen` (old ID/selfie verification) so no old verification flow can appear.
6. **Storage rules note:** `storage.rules` `/profiles/{imageId}` legacy path still allows owner reads of legacy media; confirm it's unused by the new photo flow before production.

---

## 12. Manual test checklist (run on device/emulator AFTER deploying §6 fixes)

> Capture for each ❌: a screenshot + Logcat (`adb logcat | grep -iE "firestore|permission|mithaq"`) + the relevant Firestore/Storage console path.

**Account A**
- [ ] Register, verify email, sign in.
- [ ] Complete onboarding to summary → Finish. **Expect:** success (no PERMISSION_DENIED). Firestore: `profiles/{A}` has `onboardingCompleted=true` + groups.
- [ ] Profile → My Photos → upload a photo. Firestore: `userPhotos/{A}/photos/main`. Storage: `user_photos/{A}/main.jpg`. **Expect:** no URL field in metadata.
- [ ] Firestore: `publicProfiles/{A}` exists with **non-empty** displayName/city/country (verifies BUG-2 fix) and **no** photo URL field.

**Account B**
- [ ] Register, verify, onboard.
- [ ] Home/Search → **Account A appears** with populated card.
- [ ] Send interest to A.

**Account A** — [ ] Requests → accept B's interest.
**Account B** — [ ] Request photo access to A.
**Account A** — [ ] Approve photo request. Firestore: `photoRequests/{B}_{A}.status == approved`.
**Account B**
- [ ] A's photo now displays per privacy (e.g. blurred→full). **Expect:** still no URL in `publicProfiles/{A}`.
- [ ] Request chat. Firestore: `chatRequests/{B}_{A}.status == pending`.
**Account A** — [ ] Approve chat. Firestore: `chatRequests` → `approved`, `chats/{sorted A_B}` created.

**Both accounts**
- [ ] Messages → chat room visible.
- [ ] Send messages both ways (realtime). Firestore: `chats/{id}/messages/*` `type=="text"`.
- [ ] Last-message preview updates in the list.
- [ ] Report user (A or B). Firestore: `reports/*` created.
- [ ] Block user. **Expect:** old messages still visible; **sending now fails** ("Messaging is unavailable…").
- [ ] (BUG-4) Check Discover/Search — blocked user **still appears** (known gap until follow-up).

**Cross-cutting**
- [ ] Forgot password sends reset email.
- [ ] Login with unverified email → routed to verify screen, no app access.
- [ ] Restart app after onboarding → goes to Home, **not** back into onboarding (verifies BUG-1 fix end-to-end).
- [ ] Empty states (no profiles / no requests / no chats) render friendly copy.
- [ ] Toggle Arabic ↔ English; onboarding + screens switch language and RTL.
- [ ] Airplane mode → friendly error messages (not crashes) on load/send.

---

## 13. Final verdict

**Phase 11.5 is NOT yet ready to close.** ❌ (pending deploy + on-device verification)

- The QA **found and fixed two critical, flow-breaking regressions** (BUG-1, BUG-2) introduced by the 11.12A merge; without them the entire two-account journey is dead at onboarding.
- The fixes are **code-complete and minimal** but **not yet deployed**, and the flow has **not been run interactively** in this environment.

**To close Phase 11.5:** (1) deploy `firestore:rules` + `functions`; (2) run the §12 checklist on a device and confirm all items; (3) decide on BUG-4 (discovery block filter) as accept-as-known-gap or schedule the follow-up. Once §12 passes on-device, Phase 11.5 can be closed. No new features were added; the photo/interest/chat/block **architecture was not changed** — only a rules allow-list extension and a mirror field-path correction.
