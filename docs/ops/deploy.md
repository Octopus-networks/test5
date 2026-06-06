# Firebase deployment from GitHub Actions

This repository includes `.github/workflows/firebase-deploy.yml` to deploy Firebase from GitHub Actions.

## What the workflow deploys

**Automatic (push to `main`):** deploys **only Firestore rules + Storage rules**
(`firestore:rules,storage`). **Functions are never deployed automatically**, and `--force` is not
used on automatic deploys.

**Manual (`workflow_dispatch` from the Actions tab)** uses the `deployTarget` input:

- `rules` (default) → `firestore:rules`
- `storage` → `storage`
- `functions` → `functions`
- `all` → `firestore:rules,storage,functions`

**Functions can only be deployed manually** via `workflow_dispatch`. Manual runs apply `--force`
(intentional, operator-initiated). The workflow still runs on push when Firebase-related files
change, but on push it only validates and deploys the rules + storage targets above.

## Required GitHub secret

Add this repository secret:

- Name: `FIREBASE_SERVICE_ACCOUNT`
- Value: the full JSON key for a Google Cloud service account that can deploy Firebase rules and functions for project `mithaq-matchmaking`.

Recommended permissions for the service account:

- Firebase Rules Admin
- Cloud Functions Developer
- Cloud Build Editor
- Service Account User
- Artifact Registry Writer
- Logs Writer

Depending on the Firebase/GCP project configuration, Google may require one or two adjacent roles for first-time Functions deployment. If the workflow fails with a permission error, grant only the missing permission shown in the log instead of using broad Owner permissions.

## How to add the secret

1. Open GitHub repository settings.
2. Go to **Secrets and variables** → **Actions**.
3. Click **New repository secret**.
4. Name it `FIREBASE_SERVICE_ACCOUNT`.
5. Paste the service account JSON.
6. Save.

Do not commit the service account JSON into the repository.

## Manual deploy

1. Open GitHub → **Actions**.
2. Select **Deploy Firebase**.
3. Click **Run workflow**.
4. Choose a `deployTarget` (default `rules`; choose `functions` or `all` to deploy Cloud Functions).
5. Leave `dry_run=false` to deploy, or set `dry_run=true` to validate only.

## Safety notes

- The workflow deploys automatically only from `main`, and **only Firestore + Storage rules** on
  that automatic path. **Functions are never auto-deployed** — they require a manual
  `workflow_dispatch` with `deployTarget = functions` (or `all`).
- Why functions are manual-only: a functions deploy with `--force` can replace/remove Cloud
  Functions and run server-side code changes, so it should be an explicit, reviewed action — not a
  side effect of any merge that happens to touch `functions/**`.
- `--force` is applied only on manual runs; automatic rules/storage deploys never use it.
- Pull requests validate through the normal Android workflow; Firebase deploy happens only after merge to `main`.
- Keep Firebase Console rules and repository rules in sync. Prefer deploying from GitHub so the deployed state is traceable to a commit.
