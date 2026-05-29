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
- Raw prayer logs should remain private to the owner and privileged server/admin flows only.
- Sensitive profile data should be exposed only through visibility-aware summary fields.
- Admin access should remain role-based and should not rely on client-controlled fields.

## Example Pattern

```js
match /users/{userId} {
  allow create: if request.auth != null && request.auth.uid == userId;

  allow read: if isVerifiedEmailUser();

  allow update: if isVerifiedEmailUser() &&
    request.auth.uid == userId;
}
```

Review the existing custom rules before applying this globally because Mithaq already has guardian, wali, reports, notifications, chat, and photo-access rules.
