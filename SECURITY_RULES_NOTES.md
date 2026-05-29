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
```

Review the existing custom rules before applying this globally because Mithaq already has guardian, wali, reports, notifications, chat, and photo-access rules.
