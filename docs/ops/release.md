# Releasing an APK

The [`Release APK`](../../.github/workflows/release.yml) workflow builds the app and
attaches a downloadable APK to a GitHub Release.

## How to cut a release

**Option A — tag (recommended):**

```bash
git tag v2.1.0
git push origin v2.1.0
```

Pushing a `v*` tag triggers the workflow, which builds the APK and creates the
matching GitHub Release with auto-generated notes.

**Option B — manual:** Actions tab → **Release APK** → **Run workflow**, and enter the
tag (e.g. `v2.1.0`). The tag is created at the current `main` if it doesn't exist.

The published asset is named `mithaq-<tag>-debug.apk`.

## Current limitation: debug-signed, pre-release

The workflow currently attaches the **debug-signed** APK. It is installable for testing
but is **not** suitable for Play Store distribution, so releases are marked as
**pre-releases**.

## Upgrading to a signed release

To publish a production-signed APK:

1. Generate an upload keystore:
   ```bash
   keytool -genkey -v -keystore mithaq-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias mithaq
   ```
2. Add these **repository secrets** (Settings → Secrets and variables → Actions):
   - `KEYSTORE_BASE64` — `base64 -w0 mithaq-release.jks`
   - `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
3. Wire `signingConfigs` in `app/build.gradle.kts` to read those values, then switch the
   workflow's build step to `:app:assembleRelease` and attach
   `app/build/outputs/apk/release/app-release.apk`.
4. Drop `prerelease: true` once the signed APK is verified.

Do **not** commit the keystore or its passwords to the repository.
