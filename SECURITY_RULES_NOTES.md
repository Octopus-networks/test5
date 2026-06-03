# Security Rules Notes

These notes document the recommended Firestore hardening for email verification. They are not applied yet because Phase 1 keeps rules enforcement unchanged until the app flow is tested.

## Recommended Helper

```js
function isVerifiedEmailUser() {
  return request.auth != null &&
    request.auth.token.email_verified == true;
}
```

## Recommended Direction

- Protected profile/app reads and writes should require `isVerifiedEmailUser()`.
- Raw user creation may remain possible for the signed-in owner if the app needs to create the initial user document immediately after registration.
- Home, onboarding, search, matches, requests, chat, guardian, prayer settings, photo upload, verification, and admin data should require verified email.
- `profiles/{userId}` should be treated as private profile data: owner-only reads/writes plus role-based admin/backend access.
- `publicProfiles/{userId}` should contain sanitized discovery data only and may be readable by verified users.
- `publicProfiles/{userId}` should be writable only by the owner during the current client-side mirror phase, or preferably by backend/admin logic in production.
- Raw prayer logs should remain private to the owner and privileged server/admin flows only.
- Guardian details such as phone, email, relationship notes, and permissions should remain private and must not be copied into public discovery documents.
- Chat data and message contents should remain private to room members, authorized wali access, and role-based admin moderation flows only.
- Private photo URLs should remain protected by the approval/photo-request system; public discovery should expose only privacy mode/status, not raw URLs.
- Sensitive profile data should be exposed only through visibility-aware summary fields.
- Admin access should remain role-based and should not rely on client-controlled fields.

## Public Discovery Mirror

Phase 4.5 introduces `publicProfiles/{userId}` as a sanitized mirror for Discover/Home/Search. The client currently creates this document after onboarding from approved public fields only:

- display name
- age
- city/country
- account type
- marital status
- marriage timeline
- verification badges/status placeholders
- guardian presence boolean
- photo privacy mode
- profile completion percent

It must not include sensitive raw data:

- income
- health or fertility details
- weight or hidden appearance fields
- exact prayer logs or raw prayer answers
- guardian contact details
- private photo URLs
- chat data
- hidden visibility settings

For stronger production security, move public profile mirroring to trusted backend logic such as Cloud Functions or an admin service. Client-side sanitization improves app behavior now, but server-side mirroring is the stronger boundary.

Phase 5 connects Home/Discover and Search to `publicProfiles` through `DiscoverViewModel -> PublicProfileRepository -> Firestore`. These screens must not read `profiles/{userId}` directly. Filtering is performed over the sanitized public fields only, and near-me filtering remains a placeholder until safe city/GPS matching is implemented.

Phase 6.1 introduces `interestRequests/{requestId}` for serious interest requests only. This flow must not create chats, photo requests, or expose private profile data. Request cards should use `publicProfiles` summaries only.

Phase 6.1.5 keeps interest requests as status updates instead of deletes. Senders may cancel only their own pending requests by setting `status = "cancelled"`. Receivers may accept or decline only pending requests where they are `toUserId`. Users may read only requests where they are `fromUserId` or `toUserId`.

Phase 6.2 introduces `photoRequests/{requestId}` as status-only access requests. This phase must not expose Firebase Storage URLs or private photo fields. A verified sender may create a request only for another user, preferably after an accepted interest exists between both users and the target public profile allows request-based photo access. Users may read only photo requests where they are `fromUserId` or `toUserId`. Receivers may approve or decline only received pending requests. Senders may cancel only their own pending requests. An approved photo request is only an access status; actual private photo rendering/storage access should be enforced separately in a later backend/storage-rules phase.

Phase 6.3 introduces `chatRequests/{requestId}` as status-only conversation requests. This phase must not create chat rooms, chat messages, or expose private data. A verified sender may create a request only for another user after accepted interest exists between both users. Users may read only chat requests where they are `fromUserId` or `toUserId`. Receivers may approve or decline only received pending requests. Senders may cancel only their own pending requests. An approved chat request is only an approval status; actual chat room creation should happen in a later phase after optional guardian/privacy checks.

Phase 7 creates `chats/{chatId}` after an approved chat request. The room stores participant ids and safe public summaries only; it must not contain private profile fields or messages. The client currently creates/reuses a deterministic room id for the two participants, but production should consider a Cloud Function so the server can verify the approved chat request, accepted interest, participant ids, and guardian/privacy requirements before creating the room. Messages subcollection rules should be added in a later phase before message sending is enabled.

Phase 7.5 hardens chat readiness before messages. Chat rooms should always have exactly two distinct participant ids, a deterministic id derived from those two ids, `status == "active"` at creation, `lastMessagePreview == null`, and `lastMessageAt == null`. Active chat lists should show rooms only when the current user is in `participantIds`, and UI should render only `participantPublicSummaries`. If client-side room creation remains temporarily enabled, rules should validate the shape of the document and block all message writes until the dedicated message phase.

Phase 8 introduces basic text messages under `chats/{chatId}/messages/{messageId}`. Only verified chat participants should read or write messages, the parent chat must be `active`, `senderId` must equal `request.auth.uid`, `chatId` must match the parent path, `type` must be `text`, and text must be non-empty with a maximum of 1000 characters. Attachments, image messages, voice messages, files, stickers, edits, deletes, and forwarding are not enabled in this phase. `lastMessagePreview` should be a safe truncated text preview only.

Phase 8.5 introduces basic chat safety foundations:

- `reports/{reportId}` stores user reports for moderation review. Verified users may create reports only where `reporterUserId == request.auth.uid`. Normal users should not read the global reports collection; admin/moderator review rules are needed before an admin console consumes this data.
- `blocks/{blockId}` stores user block records. Verified users may create, read, and delete only their own block records where `blockerUserId == request.auth.uid`. Block documents should contain only ids and optional chat context, not private profile data.
- Message sending should be denied when either participant has blocked the other. The client now checks this before sending, but production rules may need a helper structure or Cloud Function enforcement because cross-document block checks can become complex.
- Existing messages remain visible after a block, but new messages should be disabled for both participants until the block state changes.
- Reports and blocks do not expose private profiles, photo URLs, guardian data, or raw prayer data.

Phase 8.6 adds realtime Firestore listeners for active chat rooms and text messages. Listener access must still be protected by the same Firestore rules as normal reads: verified users may listen only to `chats/{chatId}` documents where their uid is in `participantIds`, and only to `chats/{chatId}/messages/{messageId}` under chats where they are participants. Realtime updates do not relax the text-only message limits, participant checks, block checks, or private-profile boundaries.

Phase 8.8 queues a `notifications/{notificationId}` document when a participant sends a text message. This document should be created only by the sender, only for the other participant in an existing chat room, and only with a `PENDING` status. The notification document should contain routing metadata such as `chatId`, `messageId`, sender uid, recipient uid, title, and a short safe preview. It must not contain private profile maps, photo URLs, guardian contact details, or full chat exports. For instant background delivery in production, prefer a Cloud Function that observes new chat messages or queued notifications and sends FCM data notifications through the Admin SDK.

Admin membership changes may temporarily fall back from Cloud Functions to direct Firestore updates for `isPremium`, `subscriptionPlan`, `premiumExpiry`, `isWaliAccount`, and `isAdmin`. Rules must allow only role-based admins to perform those writes, and production should prefer backend/admin writes with audit logs.

## Example Pattern

```js
match /users/{userId} {
  allow create: if request.auth != null && request.auth.uid == userId;

  allow read: if isVerifiedEmailUser();

  allow update: if isVerifiedEmailUser() &&
    request.auth.uid == userId;
}
```

Recommended profile split:

```js
match /profiles/{userId} {
  allow read, write: if isVerifiedEmailUser() &&
    request.auth.uid == userId;
}

match /publicProfiles/{userId} {
  allow read: if isVerifiedEmailUser();

  // Current client-side mirror phase only.
  // Prefer backend/admin writes in production.
  allow write: if isVerifiedEmailUser() &&
    request.auth.uid == userId;
}

match /interestRequests/{requestId} {
  allow create: if isVerifiedEmailUser() &&
    request.resource.data.fromUserId == request.auth.uid &&
    request.resource.data.toUserId is string &&
    request.resource.data.toUserId != request.auth.uid &&
    request.resource.data.status == "pending";

  allow read: if isVerifiedEmailUser() &&
    (resource.data.fromUserId == request.auth.uid ||
     resource.data.toUserId == request.auth.uid);

  allow update: if isVerifiedEmailUser() && (
    (
      resource.data.toUserId == request.auth.uid &&
      resource.data.status == "pending" &&
      request.resource.data.status in ["accepted", "declined"]
    ) ||
    (
      resource.data.fromUserId == request.auth.uid &&
      resource.data.status == "pending" &&
      request.resource.data.status == "cancelled"
    )
  );
}

match /photoRequests/{requestId} {
  allow create: if isVerifiedEmailUser() &&
    request.resource.data.fromUserId == request.auth.uid &&
    request.resource.data.toUserId is string &&
    request.resource.data.toUserId != request.auth.uid &&
    request.resource.data.status == "pending";

  allow read: if isVerifiedEmailUser() &&
    (resource.data.fromUserId == request.auth.uid ||
     resource.data.toUserId == request.auth.uid);

  allow update: if isVerifiedEmailUser() && (
    (
      resource.data.toUserId == request.auth.uid &&
      resource.data.status == "pending" &&
      request.resource.data.status in ["approved", "declined"]
    ) ||
    (
      resource.data.fromUserId == request.auth.uid &&
      resource.data.status == "pending" &&
      request.resource.data.status == "cancelled"
    )
  );
}

match /chatRequests/{requestId} {
  allow create: if isVerifiedEmailUser() &&
    request.resource.data.fromUserId == request.auth.uid &&
    request.resource.data.toUserId is string &&
    request.resource.data.toUserId != request.auth.uid &&
    request.resource.data.status == "pending" &&
    request.resource.data.requiresGuardianApproval == false &&
    request.resource.data.guardianApprovalStatus == "not_required";

  allow read: if isVerifiedEmailUser() &&
    (resource.data.fromUserId == request.auth.uid ||
     resource.data.toUserId == request.auth.uid);

  allow update: if isVerifiedEmailUser() && (
    (
      resource.data.toUserId == request.auth.uid &&
      resource.data.status == "pending" &&
      request.resource.data.status in ["approved", "declined"]
    ) ||
    (
      resource.data.fromUserId == request.auth.uid &&
      resource.data.status == "pending" &&
      request.resource.data.status == "cancelled"
    )
  );
}

match /chats/{chatId} {
  allow read: if isVerifiedEmailUser() &&
    request.auth.uid in resource.data.participantIds;

  // Current client-side phase only. Prefer Cloud Functions in production.
  allow create: if isVerifiedEmailUser() &&
    request.resource.data.participantIds is list &&
    request.resource.data.participantIds.size() == 2 &&
    request.auth.uid in request.resource.data.participantIds &&
    request.resource.data.status == "active" &&
    request.resource.data.guardianApprovalStatus in ["not_required", "approved"] &&
    request.resource.data.lastMessagePreview == null &&
    request.resource.data.lastMessageAt == null;

  allow update: if isVerifiedEmailUser() &&
    request.auth.uid in resource.data.participantIds &&
    resource.data.status == "active" &&
    request.resource.data.diff(resource.data).affectedKeys()
      .hasOnly(["lastMessagePreview", "lastMessageAt", "updatedAt"]);

  match /messages/{messageId} {
    allow read: if isVerifiedEmailUser() &&
      request.auth.uid in get(/databases/$(database)/documents/chats/$(chatId)).data.participantIds;

    allow create: if isVerifiedEmailUser() &&
      request.auth.uid in get(/databases/$(database)/documents/chats/$(chatId)).data.participantIds &&
      get(/databases/$(database)/documents/chats/$(chatId)).data.status == "active" &&
      request.resource.data.senderId == request.auth.uid &&
      request.resource.data.chatId == chatId &&
      request.resource.data.type == "text" &&
      request.resource.data.status == "sent" &&
      request.resource.data.text is string &&
      request.resource.data.text.size() > 0 &&
      request.resource.data.text.size() <= 1000 &&
      !("attachmentUrl" in request.resource.data);

    allow update, delete: if false;

    // Blocked or archived chats should prevent new messages, and attachments
    // should stay disabled until attachment moderation exists.
  }
}

match /reports/{reportId} {
  allow create: if isVerifiedEmailUser() &&
    request.resource.data.reporterUserId == request.auth.uid &&
    request.resource.data.reportedUserId is string &&
    request.resource.data.reportedUserId != request.auth.uid &&
    request.resource.data.reason is string &&
    request.resource.data.reason.size() > 0 &&
    request.resource.data.details is string &&
    request.resource.data.details.size() <= 500 &&
    request.resource.data.status == "open";

  // Normal users should not read all reports. Add role-based moderator/admin
  // access before building a moderation dashboard.
  allow read, update, delete: if false;
}

match /blocks/{blockId} {
  allow create: if isVerifiedEmailUser() &&
    request.resource.data.blockerUserId == request.auth.uid &&
    request.resource.data.blockedUserId is string &&
    request.resource.data.blockedUserId != request.auth.uid;

  allow read, delete: if isVerifiedEmailUser() &&
    resource.data.blockerUserId == request.auth.uid;

  allow update: if false;
}
```

Review the existing custom rules before applying this globally because Mithaq already has guardian, wali, reports, notifications, chat, and photo-access rules.

## Phase 12 — Admin & Moderation

Phase 12 adds admin-only moderation surfaces. Admin detection remains role-based via the
server-controlled `users/{uid}.isAdmin` field (clients cannot set it; only the `setUserRole` Cloud
Function or an existing admin can). See `ADMIN_MODERATION.md` for the full design and the recommended
migration to Firebase custom claims.

Rule changes (all admin-gated; no broad read/write, no `if true`):

- `reports/{reportId}` — added `allow update: if isAdmin()` so admins can set
  `status` (`reviewed` / `dismissed` / `action_taken`) and an `adminNote`. Normal users still cannot
  read or update reports; they may only create their own.
- `userPhotos/{uid}/photos/{photoId}` — added an **admin-only** update branch that may change **only**
  `status`, `rejectionReason`, `updatedAt` (validated with `diff().affectedKeys().hasOnly([...])`,
  `status in ["pending_review","approved","rejected"]`). Admins cannot alter the image bytes, owner,
  storage path, type, or visibility. Owners still upload as `pending_review` and can never
  self-approve. `rejectionReason` was added to the owner upload allow-list so a re-upload after a
  rejection still validates.
- `userModeration/{moderatedUserId}` — new collection, `allow read, write: if isAdmin()` only.
  Stores `status` (`active` / `warned` / `suspended` / `banned`) + `note` + `updatedBy`. Foundation
  only; full ban enforcement across sign-in/discovery/requests/chat remains a TODO and should likely
  move to a Cloud Function.

Indexing: the admin "pending photos" list uses a collection-group equality query on `photos.status`,
served by Firestore's automatic single-field (collection-group-scoped) index — no custom composite
index required.

Production hardening direction: move report/photo moderation writes behind Cloud Functions with
`adminAuditLogs`, and adopt custom claims so rules check `request.auth.token.admin` without a
per-evaluation `get()`.

## Phase 12.1 — Safer Firebase deploy workflow

`.github/workflows/firebase-deploy.yml` was hardened so a normal merge to `main` can no longer
auto-deploy Cloud Functions:

- **Automatic (push to `main`)** deploys **only** `firestore:rules,storage` (Firestore + Storage
  rules). **Functions are never deployed automatically**, and `--force` is not used on the automatic
  path.
- **Functions are manual-only** via `workflow_dispatch` with `deployTarget` (`rules` (default) /
  `storage` / `functions` / `all`). `--force` is applied only on manual, operator-initiated runs.
- No "deploy all" by default; no automatic functions deploy on PR merge.

Why: a functions deploy runs server-side code changes and (with `--force`) can replace/remove
functions, so it must be an explicit, reviewed action — not a side effect of any merge that touches
`functions/**`. Rules deploys are non-destructive and remain automatic after an approved merge.
Secrets handling is unchanged (`FIREBASE_SERVICE_ACCOUNT` used only in the auth step; never printed).
