# Firebase deployment from GitHub Actions

This repository includes `.github/workflows/firebase-deploy.yml` to deploy Firebase from GitHub Actions.

## What the workflow deploys

The workflow can deploy these targets:

- `all` → `firestore:rules,storage,functions`
- `rules` → `firestore:rules,storage`
- `firestore` → `firestore:rules`
- `storage` → `storage`
- `functions` → `functions`

It runs automatically on pushes to `main` when Firebase-related files change, and it can also be started manually from the **Actions** tab.

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
4. Choose a target.
5. Leave `dry_run=false` to deploy, or set `dry_run=true` to validate only.

## Safety notes

- The workflow deploys automatically only from `main`.
- Pull requests validate through the normal Android workflow; Firebase deploy happens only after merge to `main`.
- Keep Firebase Console rules and repository rules in sync. Prefer deploying from GitHub so the deployed state is traceable to a commit.
