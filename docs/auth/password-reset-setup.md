# Mithaq Password Reset Setup

Mithaq uses Firebase Authentication to send password reset emails. The app does not reveal whether an email address exists in Firebase.

## Firebase Console

1. Open Firebase Console.
2. Go to Authentication.
3. Open Templates.
4. Select Password reset.
5. Customize the subject and body with Mithaq branding.
6. Keep the wording respectful and privacy-first.

## Authorized Domain

Add the production domain in Firebase Authentication settings:

- `mithaq.app`

The reset email uses this continue URL:

- `https://mithaq.app/reset-password`

## Android App Link

The Android app declares an app link for:

- `https://mithaq.app/reset-password`

For Android App Links verification, host `assetlinks.json` at:

- `https://mithaq.app/.well-known/assetlinks.json`

Include the Android package:

- `com.mithaq.app`

Add the production SHA-256 certificate fingerprint.

## Current Reset Flow

1. User taps Forgot password.
2. User enters an email.
3. Firebase sends a password reset email.
4. User opens the email and resets the password on the Firebase hosted page/browser.
5. User returns to Mithaq and logs in with the new password.

The app intentionally does not implement a custom in-app `oobCode` password reset handler yet.

## Security Notes

- Do not show "email not found" or "account does not exist".
- Use generic success wording:
  - "If this email is registered, we will send a password reset link."
- Keep request cooldown enabled to reduce spam.
- Use friendly network and too-many-requests errors without exposing account existence.

