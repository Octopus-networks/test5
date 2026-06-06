# Mithaq Email Verification Setup

Mithaq blocks protected app access until the Firebase Auth user's email is verified.

## Firebase Console

1. Open Firebase Console.
2. Enable Authentication > Sign-in method > Email/Password.
3. Open Authentication > Templates > Email address verification.
4. Customize the template so the action button says `Activate Account`.
5. Add `mithaq.app` under Authentication > Settings > Authorized domains.
6. Add the Android app SHA-1 and SHA-256 fingerprints for debug and release builds.

## Android App Link

The app handles this continue URL:

```text
https://mithaq.app/verify-email
```

The Android manifest includes an app link intent filter for this URL. To complete verification on production devices, host this file:

```text
https://mithaq.app/.well-known/assetlinks.json
```

Use the real package name and signing certificate fingerprint:

```json
[
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "com.mithaq.app",
      "sha256_cert_fingerprints": [
        "REPLACE_WITH_RELEASE_SHA256"
      ]
    }
  }
]
```

## Runtime Flow

1. Register creates a Firebase Auth user.
2. The app sends `sendEmailVerification` with an Android continue URL.
3. The user is routed to `verify_email`.
4. Protected screens remain blocked until `currentUser.reload()` reports `emailVerified == true`.
5. The user can resend the activation email, change email, or sign out.

## Notes

- Do not allow onboarding, home, chat, search, profile, guardian, photo upload, prayer settings, or admin screens for unverified email users.
- Google sign-in is also checked. If Firebase reports the email is not verified, the same gate is shown.
- Firestore rules are already deployed and enforce verified-email gating. See [`../security/firestore-rules.md`](../security/firestore-rules.md).
