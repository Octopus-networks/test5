# Admin & Moderation — Phase 12 Foundation

Privacy-first admin/moderation foundations for Mithaq: report review, photo review, and user
moderation. **Foundations only** — no billing, notifications, chat features, or full ban enforcement
were added. Architecture is strictly **Compose Screen → ViewModel → Repository → Firestore**;
Compose never touches Firebase directly.

## 1. Admin custom claims setup

**Current state:** admin detection uses the **server-controlled Firestore field `users/{uid}.isAdmin`**,
not Firebase custom claims. This is already enforced everywhere:

- Firestore rules `isAdmin()` reads `users/{uid}.data.isAdmin == true`.
- Storage rules read the same field.
- Clients **cannot** set it: the `users` create rule forbids `isAdmin`, and `noPrivilegeEscalation()`
  blocks changing it on update. It is set only by the privileged `setUserRole` Cloud Function (Admin SDK)
  or by an existing admin.
- In-app gate: `isAdminUser(profile)` / `currentUserProfile.isAdmin` (UI only).

Custom claims are **not implemented**. Recommended upgrade (documentation only here):

```js
// One-time / admin Cloud Function using the Admin SDK
await admin.auth().setCustomUserClaims(uid, { admin: true });
// Client must refresh its token afterwards: user.getIdToken(true)
```

Then rules could check the token directly (no Firestore read):

```
function isAdmin() {
  return request.auth != null && request.auth.token.admin == true;
}
```

Custom claims are cheaper (no `get()` per rule evaluation) and harder to spoof. Migration path: set
claims for existing admins, switch `isAdmin()` to the token check, keep the `users.isAdmin` field in
sync for the in-app UI. **Until then, the `isAdmin` field is the source of truth and is already safe.**

## 2. Moderated collections

| Collection | Purpose | Who can read | Who can write |
|---|---|---|---|
| `reports/{reportId}` | User reports for review | Admins only | Reporter creates; **admins update** (status + note) |
| `userPhotos/{uid}/photos/{photoId}` | Photo review status | Owner / admin / approved viewer | Owner uploads (`pending_review`); **admins set** `status` + `rejectionReason` only |
| `userModeration/{uid}` | User moderation status + note | **Admins only** | **Admins only** |

Normal users **cannot** read the reports collection, cannot read others' moderation records, and
cannot approve/reject their own photos. No private profile data or photo URLs are exposed anywhere.

## 3. Report review flow

1. A verified user reports another (`ReportRepository`) → `reports/{reporterId}_… ` with `status: "open"`.
2. Admin opens **Admin moderation → Reports** (`AdminModerationScreen`).
3. `AdminModerationViewModel` → `AdminModerationRepository.getOpenReports()` lists `status == "open"`.
4. Admin reads details and chooses one of:
   - **Reviewed** → `status: "reviewed"`
   - **Dismiss** → `status: "dismissed"`
   - **Action** → `status: "action_taken"`
   plus an optional **admin note** (`reviewReport()` writes `status`, `adminNote`, `reviewedBy`, `updatedAt`).
5. Optionally moderate the reported user (Warn / Suspend / Ban) → writes `userModeration/{reportedId}`.

`ReportStatus = open | reviewed | dismissed | action_taken`.

## 4. Photo review flow

1. Owner uploads a photo (`PhotoRepository`) → `userPhotos/{uid}/photos/{photoId}` with
   `status: "pending_review"`. Owners can never self-approve (rules force `pending_review` on upload).
2. Admin opens **Admin moderation → Photos**.
3. `AdminModerationRepository.getPendingPhotos()` runs a **collection-group** query on `photos` where
   `status == "pending_review"`.
4. Admin **Approve** (`status: "approved"`) or **Reject** (`status: "rejected"` + optional
   `rejectionReason`). Rules restrict admin photo writes to **only** `status`, `rejectionReason`,
   `updatedAt` — admins cannot change the image, owner, path, type, or privacy.

`PhotoStatus = pending_review | approved | rejected`.

> **Index note:** the collection-group equality query on `photos.status` is served by Firestore's
> automatic single-field (collection-group-scoped) index — no custom index is required. If a future
> query adds an `orderBy`, add the composite index then.

## 5. User moderation (foundation)

- `ModerationStatus = active | warned | suspended | banned`, stored at `userModeration/{uid}`.
- Admins can set a status + note from a report, and view existing records in **Admin moderation → User mod**.
- **Enforcement is NOT wired** beyond recording the status (see TODOs). Banning/suspension does not yet
  block sign-in, discovery, requests, or chat.

## 6. Security notes

- Admin-only Firestore rules are the real boundary; the in-app `isAdmin` check only hides UI.
- `AdminModerationViewModel` re-checks admin server-side (`isCurrentUserAdmin()`) before loading data,
  and renders a safe **"Not authorized"** state for non-admins (loads nothing).
- The `admin_moderation` route is not visible to normal users (profile-hub entry shown only when
  `isAdmin`), and is safe even if reached accidentally.
- No rule was broadened to `if true`; no broad read/write was added. Photo admin writes are tightly
  scoped via `diff().affectedKeys().hasOnly([...])`.
- Reports/photos/moderation never expose private profiles, photo URLs, or guardian/chat data.

## 7. Remaining TODOs

- [ ] Migrate admin detection to **Firebase custom claims** (`admin: true`) and switch rules to the
      token check; keep `users.isAdmin` in sync for UI.
- [ ] **Enforce** moderation states: block `suspended`/`banned` users from sign-in / discovery /
      requests / chat (needs rules + likely a Cloud Function; currently recording-only).
- [ ] Move report/photo moderation writes behind **Cloud Functions** with audit logs
      (`adminAuditLogs`) for production hardening.
- [ ] Notify users of photo rejection / moderation outcomes (out of scope — notifications excluded).
- [ ] Add pagination + filters (e.g. show reviewed/dismissed history) to the admin lists.
- [ ] Optional: surface `userModeration.status` (e.g. a banner) in the existing `AdminConsoleScreen`
      member list.
