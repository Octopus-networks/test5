# Public Profiles Mirror

## Why `publicProfiles` exists

Mithaq keeps private onboarding/profile data in `profiles/{userId}` and exposes only a small discovery-safe projection in `publicProfiles/{userId}`. Home, Discover, Search, request cards, and chat summaries should use `publicProfiles` whenever they need another member's public summary.

This split prevents discovery/search code from reading private profile answers or accidentally exposing sensitive fields.

## Phase 10 authority boundary

Android must not create, update, or delete `publicProfiles/{userId}` directly. The Android client saves onboarding answers to `profiles/{userId}` only.

Firebase Cloud Functions owns the mirror:

- Trigger: `profiles/{userId}` create/update/delete.
- Create/update: build a sanitized public object with `buildPublicProfileFromPrivateProfile(userId, profileData)` and write `publicProfiles/{userId}` with Admin SDK.
- Delete: delete `publicProfiles/{userId}`. This is the least risky behavior because deleted private profiles disappear from discovery instead of leaving stale public data.

## Allowed public fields

Only these fields may be written to `publicProfiles`:

- `userId`
- `displayName`
- `age`
- `city`
- `country`
- `accountType`
- `maritalStatus`
- `marriageTimeline`
- `prayerHabitPublicLabel`
- `prayerRoutineShared`
- `localTimeEnabled`
- `hasGuardian`
- `isEmailVerified`
- `isIdentityVerified`
- `photoPrivacyMode`
- `profileCompletionPercent`
- `lastActiveAt`
- `updatedAt`

Missing fields use safe defaults. The mirror function must not crash when optional onboarding sections are absent.

## Forbidden fields

Never mirror these into `publicProfiles`:

- income
- health details
- fertility details
- weight
- exact prayer logs or raw prayer answers
- guardian name, phone, email, relationship notes, or permissions
- private photo URLs or storage paths
- chat data or message data
- reports
- blocks
- hidden visibility settings
- raw religious sect if marked private
- unknown fields not explicitly allowlisted

## Backfill existing users

Run the one-time backfill after deploying the function code, or whenever a safe mirror rebuild is needed:

```bash
npm run backfill:publicProfiles --prefix functions
```

The script:

- reads all `profiles` documents
- uses the same sanitizer helper as the live trigger
- writes `publicProfiles/{userId}` with the strict allowlist
- is safe to rerun
- logs the number of processed profiles

It uses Firebase Admin SDK default credentials. Do not commit credentials or hardcode production secrets.

## Firestore rules direction

Recommended rules:

- `profiles/{userId}`: owner-only read/write plus admin/backend access where needed.
- `publicProfiles/{userId}`: readable by verified users; direct client create/update/delete denied.
- Cloud Functions/Admin SDK writes bypass Firestore Rules and must keep the strict sanitizer as the privacy boundary.

No rule should use broad access such as `allow read, write: if true`.

## Production notes

- Deploy Cloud Functions before relying on server-side mirroring for new onboarding saves.
- Backfill existing users after deployment.
- Keep Home/Search reads on `publicProfiles`; do not regress to reading private `profiles` or `users` documents.
- If new public fields are needed later, update the sanitizer, docs, tests, and rules notes together.
- Treat `publicProfiles` as public-to-verified-members data: never store anything that would be harmful if discovered by another verified member.
