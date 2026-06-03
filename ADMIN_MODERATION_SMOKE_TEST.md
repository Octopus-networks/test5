# Admin Moderation — Phase 12.2 Smoke Test

**App:** Mithaq · **Repo:** Octopus-networks/test5 · **Branch:** `phase-12-2-admin-smoke-test` (off `main` @ `4f68210`) · **Date:** 2026-06-03

> ⚠️ **Honesty note on method.** This was run in a CLI environment with **no device/emulator and no live admin/normal Firebase accounts (or their ID tokens)**. I therefore **could not execute the live UI flows or authenticated live rule probes**, and I do not claim to have. Per the task's fallback, this report contains: (a) a **static verification** of the merged-on-`main` code + rules (marked **[STATIC ✅]**), and (b) an exact **manual on-device checklist** (marked **[MANUAL]**) with the screenshots/logs to send back. Nothing was deployed; no code or rules were changed (none needed).

---

## 1. Test environment
| Item | Value |
|---|---|
| Code under test | `main` @ `4f68210` (Phase 12 + 12.1 + 12.1B merged) |
| Build/CI on main | ✅ green (Build Debug APK + CodeQL all languages) |
| Firestore rules | deployed (rules/storage auto-deploy on merge; functions manual-only) |
| Live tests | **not runnable here** — device + 2 verified accounts (1 admin) required |

## 2. Test accounts (fill in when you run it; never record passwords)
| Role | How to provision | Email (example) | UID |
|---|---|---|---|
| **Admin** | set `users/{uid}.isAdmin = true` (via existing Admin Console "Make Admin", `setUserRole` Cloud Function, or Firebase console) | `qa-admin@example.com` | `____` |
| **Normal** | a regular verified account, `isAdmin` absent/false | `qa-user@example.com` | `____` |

---

## 3. Static verification (what I could confirm from merged code/rules)

| # | Requirement | Result | Evidence (on `main`) |
|---|---|---|---|
| 1a | Admin sees "Admin moderation" entry | [STATIC ✅] | `MithaqProfileHubScreen`: entry rendered only `if (isAdmin)`; `isAdmin` threaded from `currentUserProfile?.isAdmin == true` |
| 1b | Normal user does NOT see entry | [STATIC ✅] | same `if (isAdmin)` gate — hidden when false |
| 1c | Direct route access by normal user → "Not authorized" | [STATIC ✅] | `admin_moderation` route renders `AdminModerationScreen(isAdmin=…)`; VM `start(false)` ⇒ `authorized=false` ⇒ `NotAuthorizedState`, loads no data |
| 2a–d | Admin lists open reports / views details / sets reviewed·dismissed·action_taken / admin note | [STATIC ✅] | `AdminModerationRepository.getOpenReports()` (`status==open`); `reviewReport(status, adminNote)` writes status+adminNote+reviewedBy; Reports tab UI implements all three actions + note field |
| 2e | Normal user cannot read all reports | [STATIC ✅] | `reports` rule: `read: if isAdmin()` only; VM loads nothing unless admin (re-checked server-side) |
| 3a–b | Normal user upload → `pending_review` | [STATIC ✅] | `PhotoRepository.uploadPhoto` writes `status="pending_review"`; rule forces `pending_review` on owner upload |
| 3c | Admin sees pending photos | [STATIC ✅] | `getPendingPhotos()` = collection-group query `photos where status==pending_review`; `userPhotos` read rule allows `isAdmin()` |
| 3d–e | Admin approve / reject (+reason) | [STATIC ✅] | `setPhotoStatus(APPROVED/REJECTED, rejectionReason)`; rule admin branch allows changing **only** `status`,`rejectionReason`,`updatedAt` |
| 3f | Normal user cannot self-approve | [STATIC ✅] | owner upload rule forces `pending_review`; owner visibility-only update can't touch `status`; only `isAdmin()` branch may set approved/rejected |
| 4a–b | Admin views/sets user moderation | [STATIC ✅] | User-mod tab lists `userModeration`; report card Warn/Suspend/Ban → `setUserModeration(status,note)` |
| 4c | Normal user cannot access `userModeration` | [STATIC ✅] | rule: `read, write: if isAdmin()` only |
| 5 | Rules: reports admin-read; userPhotos status admin-write; userModeration admin-only; no broad allow | [STATIC ✅] | `reports read/update: isAdmin()`; userPhotos admin branch scoped via `diff().hasOnly([...])`; `userModeration read,write: isAdmin()`; catch-all `allow read,write: if false`; **no `if true` anywhere** |

**Defense in depth confirmed:** UI gate (`isAdmin`) → ViewModel gate (`authorized` + server-side `isCurrentUserAdmin()`) → Firestore rules (`isAdmin()`). A non-admin is blocked at all three layers.

## 4. Tests failed / Bugs
- **Tests failed (static):** none.
- **Bugs found:** none. **Bugs fixed:** none. **Bugs not fixed:** none.
- No code or rule change was required for Phase 12.2.

## 5. Firestore observations (from `firestore.rules` on main)
- `reports/{id}` — `create`: reporter only; `read, delete, update`: **`isAdmin()` only**. Normal users cannot read the collection. ✅
- `userPhotos/{uid}/photos/{id}` — owner uploads as `pending_review` (cannot self-approve); **admin-only** branch may change only `status` + `rejectionReason` (+`updatedAt`). ✅
- `userModeration/{uid}` — **`read, write: if isAdmin()`** only. ✅
- Catch-all `match /{document=**} { allow read, write: if false; }`; **no `allow read/write: if true`**. ✅
- ⚠️ Watch item (not a bug): the admin "pending photos" list uses a **collection-group** query on `photos.status`. If the list is unexpectedly empty/errors on first use, Firestore may need the collection-group single-field index enabled (it usually auto-creates; the console will prompt with a link).

---

## 6. [MANUAL] On-device checklist (please run; send results)
> Capture for each ❌: screenshot + `adb logcat | grep -iE "firestore|permission|mithaq"` + the relevant Firestore console path. Use the same build as `main`.

**Setup**
- [ ] Make `qa-admin` an admin (`users/{uid}.isAdmin=true`); keep `qa-user` normal. Record both UIDs.

**1. Admin access**
- [ ] As **admin** → Profile tab → "Admin moderation" entry **is visible** → opens the screen (Reports/Photos/User-mod tabs).
- [ ] As **normal** → Profile tab → entry is **NOT visible**.
- [ ] As **normal**, force-open the `admin_moderation` route → shows **"Not authorized"**, **no data** loads. (Confirm Firestore shows no `reports`/`userModeration` reads succeeded.)

**2. Reports**
- [ ] As normal, Report another user (existing Report flow) → Firestore: `reports/*` `status:"open"`.
- [ ] As admin → Reports tab lists it; details show reporter/reported/reason.
- [ ] Tap **Reviewed** / **Dismiss** / **Action** + type an **admin note** → Firestore: `status` updates + `adminNote`/`reviewedBy` set.
- [ ] As normal, attempt to read `reports` (e.g. via app/devtools) → **denied**.

**3. Photo review**
- [ ] As normal → Profile → My Photos → upload a photo → Firestore `userPhotos/{uid}/photos/{id}` `status:"pending_review"`.
- [ ] As admin → Photos tab → the pending photo appears.
- [ ] **Approve** → status `approved`. Upload another → **Reject** with a reason → status `rejected`, `rejectionReason` set.
- [ ] As normal, attempt to set your own photo `status:"approved"` → **denied** by rules.

**4. User moderation**
- [ ] As admin → on a report, tap **Warn/Suspend/Ban** → Firestore `userModeration/{reportedUid}` created with status+note; appears in the User-mod tab.
- [ ] As normal, attempt to read `userModeration/*` → **denied**.

**Send back:** screenshots of (admin entry visible / hidden for normal / Not-authorized), the Reports + Photos tabs, and Firestore console showing `reports.status`, `userPhotos…status`, `userModeration` docs — plus any `PERMISSION_DENIED` logcat lines from the normal-user denial attempts.

---

## 7. Is Phase 12.2 ready to close?
**Static verification: PASS** — admin gating, normal-user blocking (UI + VM + rules), report review, photo review, and user-moderation foundations are all correctly implemented and the rules are admin-only with no broad access. main is green.

**Verdict: NOT closeable by me alone** 🟡 — the live UI/rule behaviour was **not** executed in this environment. Phase 12.2 can be **closed once the §6 on-device checklist passes** (especially 1c Not-authorized, 2e/3f/4c denials, and the approve/reject flow). If any item fails, send the captured logs/screenshots and I'll do a minimal fix + PR.
