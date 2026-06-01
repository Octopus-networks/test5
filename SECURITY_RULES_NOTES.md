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
- `publicProfiles/{userId}` is server-owned in Phase 10. Android clients must not create, update, or delete public mirror documents directly; Cloud Functions/Admin SDK mirrors `profiles/{userId}` through a strict allowlist.
- Raw prayer logs should remain private to the owner and privileged server/admin flows only.
- Guardian details such as phone, email, relationship notes, and permissions should remain private and must not be copied into public discovery documents.
- Chat data and message contents should remain private to room members, authorized wali access, and role-based admin moderation flows only.
- Private photo URLs should remain protected by the approval/photo-request system; public discovery should expose only privacy mode/status, not raw URLs.
- Phase 11 stores uploaded profile photos in Storage under `user_photos/{userId}/{photoId}.jpg` and metadata under `userPhotos/{userId}/photos/{photoId}`. `publicProfiles` must never contain Storage paths or download URLs.
- Sensitive profile data should be exposed only through visibility-aware summary fields.
- Admin access should remain role-based and should not rely on client-controlled fields.

## Public Discovery Mirror

Phase 4.5 introduced `publicProfiles/{userId}` as a sanitized mirror for Discover/Home/Search. Phase 10 moves this mirror out of Android and into Cloud Functions. The client now saves onboarding data only to `profiles/{userId}`; `mirrorPublicProfile` observes that private profile document and writes the public mirror with a strict allowlist:

- user id
- display name
- age
- city/country
- account type
- marital status
- marriage timeline
- public prayer label/share flag only
- local time sharing flag
- guardian presence boolean only
- email/identity verification booleans
- photo privacy mode
- profile completion percent
- last active/update timestamps

It must not include sensitive raw data:

- income
- health or fertility details
- weight or hidden appearance fields
- exact prayer logs or raw prayer answers
- guardian contact details
- private photo URLs
- chat data
- hidden visibility settings

Cloud Functions deletes `publicProfiles/{userId}` when `profiles/{userId}` is deleted. That is the least risky default because deleted private profiles disappear from discovery instead of leaving stale public data behind.

Existing users can be backfilled with `npm run backfill:publicProfiles --prefix functions`. The script uses the same sanitizer helper as the live trigger and is safe to rerun.

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

Phase 11 adds the secure photo upload/display foundation:

- Photo metadata lives at `userPhotos/{userId}/photos/{photoId}` with `photoId`, `userId`, `storagePath`, `type`, `status`, `visibility`, `createdAt`, and `updatedAt` only.
- Photo bytes live at `user_photos/{userId}/{photoId}.jpg` in Firebase Storage.
- Users may upload only to their own `user_photos/{userId}/...` path and read their own photos.
- Approved viewers may read a private photo only when an approved `photoRequests/{viewerUid_ownerUid}` document exists. Production may move this to Cloud Functions/signed access for stronger validation and auditability.
- Discovery/Home/Search must not store or read photo URLs from `publicProfiles`; approved viewing is resolved through `PhotoRepository` after checking photo request/visibility policy.
- `hidden` shows a locked placeholder, `blurred_by_default` shows a symbolic blurred placeholder until real blurred previews exist, `approved_users_only` requires an approved photo request, and `matched_users_only` remains a future match-policy placeholder.
- TODOs before production: ML Kit/manual photo review, blurred preview generation, stronger Storage emulator tests/rules, admin review workflow, and optional Cloud Function access validation.

Phase 8.6 adds realtime Firestore listeners for active chat rooms and text messages. Listener access must still be protected by the same Firestore rules as normal reads: verified users may listen only to `chats/{chatId}` documents where their uid is in `participantIds`, and only to `chats/{chatId}/messages/{messageId}` under chats where they are participants. Realtime updates do not relax the text-only message limits, participant checks, block checks, or private-profile boundaries.

Phase 8.8 queues a `notifications/{notificationId}` document when a participant sends a text message. This document should be created only by the sender, only for the other participant in an existing chat room, and only with a `PENDING` status. The notification document should contain routing metadata such as `chatId`, `messageId`, sender uid, recipient uid, title, and a short safe preview. It must not contain private profile maps, photo URLs, guardian contact details, or full chat exports. For instant background delivery in production, prefer a Cloud Function that observes new chat messages or queued notifications and sends FCM data notifications through the Admin SDK.

Phase 8.9 enforces block state across discovery, requests, and chat in app logic:

- Discover/Home/Search must read only `publicProfiles` and then client-filter users blocked by the current user. If reciprocal block reads are permitted by rules, users who blocked the current user should also be excluded; if not safely readable, the app must fail closed for sends and avoid reading private profiles.
- Interest/photo/chat request creation must check `BlockRepository.isBlockedBetweenUsers(fromUserId, toUserId)` before writing new pending requests. Existing request history can remain visible to participants, but response/cancel actions should be disabled or rejected while either side is blocked.
- Chat rooms and old messages remain readable to participants after a block, but new message sends must be disabled with `Messaging is unavailable for this conversation.` No chat rooms or messages are deleted by block enforcement.
- `BlockRepository.getBlockedUserIds(userId)` is the central source for current-user block filtering. `BlockRepository.isBlockedBetweenUsers(userA, userB)` is the central app-logic gate for actions between two users.
- Current deployed block documents use `blockerId`, `blockedId`, and `timestamp` with deterministic id `{blockerId}_{blockedId}`. A future rules/data migration can add `blockerUserId`, `blockedUserId`, `createdAt`, `updatedAt`, and optional chat context once Firestore rules are updated.
- Production security should eventually enforce request/message block checks in Firestore rules or Cloud Functions; client checks improve UX and reduce accidental writes but are not the only security boundary.

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

  // Phase 10: server/Admin SDK writes only via Cloud Functions.
  allow create, update, delete: if false;
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
