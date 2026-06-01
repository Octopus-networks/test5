# Phase 12 Admin and Moderation Foundations

Scope: moderation foundation only. No billing, notifications, chat features, attachments, voice messages, onboarding questions, prayer changes, guardian changes, dating-style features, or new Cloud Functions were added.

## Admin role model

Preferred production model: Firebase custom claims.

- `admin == true`: full moderation/admin access.
- `moderator == true`: moderation access for reports and photo review.

The Firestore/Storage rules now document custom-claim helpers. A legacy `users/{uid}.isAdmin` fallback remains only for compatibility with existing rule assumptions and should not be the production source of truth.

No production code hardcodes admin emails.

## Reports moderation

Existing user submissions still write to:

```text
reports/{reportId}
```

Report schema foundation:

- `reportId`
- `reporterUserId`
- `reportedUserId`
- optional `chatId`
- `reason`
- `details`
- `status`: `open`, `reviewed`, `dismissed`, `action_taken`
- `createdAt`
- `updatedAt`
- optional `reviewedBy`
- optional `reviewedAt`
- optional `adminNote`

`ModerationRepository` provides:

- `getOpenReports()`
- `getReport(reportId)`
- `updateReportStatus(reportId, status, adminNote)`
- `markReportReviewed(reportId)`

Normal users still only submit reports and receive submission confirmation; they are not given read/list access to the reports collection.

## Photo review foundation

Photo metadata remains at:

```text
userPhotos/{userId}/photos/{photoId}
```

`ModerationRepository` provides:

- `getPendingPhotosForReview()`
- `approvePhoto(userId, photoId)`
- `rejectPhoto(userId, photoId, reason)`

Supported review statuses:

- `pending_review`
- `approved`
- `rejected`

Normal users cannot self-approve photos. Rejected photos remain as metadata but should not display publicly. Approved photo status is only one access gate; display still depends on privacy mode, approved photo requests, and Storage/backend access policy.

## User safety status foundation

Documented future statuses only:

- `normal`
- `warned`
- `restricted`
- `suspended`
- `banned`

Phase 12 does not automatically warn, restrict, suspend, or ban users.

## Admin UI foundation

Added hidden internal UI:

```text
AdminModerationScreen -> AdminModerationViewModel -> ModerationRepository -> Firestore
```

The screen is intentionally not wired into normal navigation. It checks `admin`/`moderator` custom claims before loading moderation queues.

Visible foundation data:

- open reports count
- pending photos count
- reports list
- pending photo list
- report review/dismiss/action buttons
- photo approve/reject buttons

It does not display private profiles, photo URLs, message exports, guardian details, or prayer data.

## Rules/docs

Updated but not deployed:

- `firestore.rules`
- `storage.rules`
- `SECURITY_RULES_NOTES.md`

Rules direction:

- verified users can create reports only for themselves as reporter.
- normal users cannot list/read reports.
- admins/moderators can read/update report status.
- users create photo metadata only as `pending_review`.
- users cannot set photo status `approved`.
- admins/moderators can approve/reject photo metadata.
- approved photo access still depends on privacy and photo-request access.
- use custom claims; do not rely on client-side checks only.

## Verification

Local lightweight verification only. Android Gradle build was intentionally skipped because Mithaq Android builds should run through GitHub Actions on this Hermes environment.

Checks run:

- XML parse for default/Arabic strings and manifest.
- Admin screen has no direct Firebase calls.
- Admin screen is not wired into normal navigation.
- No hardcoded admin emails in moderation code.
- No public-profile photo URL/storage path references.
- `python3 scripts/verify_security_rules.py`.
- `npm --prefix functions run lint`.
- `npm --prefix functions test`.
- `git diff --check`.

## Production TODOs

- Assign Firebase custom claims from trusted admin tooling; no hardcoded admin email lists.
- Add Firebase Emulator tests for moderation rules:
  - report create allowed only for reporter.
  - normal users cannot read/list reports.
  - moderator/admin can read/update reports.
  - owner cannot approve own photo.
  - moderator/admin can approve/reject photos.
- Add production internal route only after custom claims are deployed and verified.
- Add audit logs for report/photo moderation actions.
- Consider Cloud Functions for photo review transitions if stronger server-side auditability is required.
- Run Android CI on GitHub Actions after pushing the branch.
