# Google Play Console — Launch Checklist (Mithaq)

App: **Mithaq** · package `com.mithaq.app` · current `versionName` 2.1.14 / `versionCode` 35.
This is the human (account-owner) checklist — most steps can only be done by you in the
Play Console with the Google account that owns the developer profile. Code/build steps that
are already DONE are marked ✅.

---

## 0. Build artifact (DONE on this machine)
- ✅ Signed release pipeline (`feat/signed-release-pipeline`, keystore OUTSIDE repo).
- ✅ Release **AAB** built at `app/build/outputs/bundle/release/app-release.aab` — Play requires
  an **App Bundle (.aab)**, NOT the APK. Upload the `.aab`.
- ⚠️ **Decide app-signing model when you first upload:**
  - **Recommended: Play App Signing** — you upload with the *upload key* (our current keystore),
    Google holds the final *app signing key*. If our keystore is ever lost, Google can reset the
    upload key, so you can still ship updates. Pick this.
  - If you opt OUT of Play App Signing, our `mithaq-keys` keystore IS the app signing key and
    losing it is unrecoverable. (Back it up regardless — see below.)
- 🔴 **Back up `C:\Users\ahmed\mithaq-keys\` now** (cloud + offline). Non-negotiable.

## 1. Developer account
- [ ] Create/verify the Google Play Developer account ($25 one-time), identity + (for orgs) D-U-N-S.
- [ ] Set up the **payments profile** (required before you can offer the paid subscriptions).

## 2. Create the app
- [ ] New app → name "Mithaq" (or localized), default language, **App** type, Free (with in-app
      purchases — the subscriptions are paid, the app download is free).
- [ ] Declare it as an app (not game).

## 3. Store listing
- [ ] Short description + full description (AR + EN — the app is bilingual; add both locales).
- [ ] App icon 512×512 (export from `ic_launcher`; note our launcher is currently a single-density
      PNG — generate a proper 512 hi-res icon).
- [ ] Feature graphic 1024×500.
- [ ] Phone screenshots (min 2; capture: Discover, Prayer Hub/Qibla, Profile, Messages).
- [ ] Category: **Lifestyle** or **Social** (Dating is a separate restricted category with extra
      policy — see §6; "Islamic matchmaking with guardian oversight" fits Lifestyle/Social better
      and avoids the Dating-category restrictions, but read §6).

## 4. App content / policy declarations (Play will block release until all are green)
- [ ] **Privacy Policy URL** — REQUIRED (we collect photos, location, identity documents). Host one
      on octopus-networks.com. Must cover: account data, photos, **precise location** (adhan/prayer
      times), **identity verification documents** (ID + selfie video), chat content, FCM.
- [ ] **Data safety form** — declare honestly:
  - Personal info (name, email, approximate + **precise location**), Photos, Messages,
    **Government ID / identity documents** (verification), App activity.
  - Encrypted in transit: yes. Data deletion: yes — we have **in-app account deletion** that runs
    the full server cleanup (deleteUserProfile) → link the in-app path + a web request route.
- [ ] **Content rating** questionnaire (IARC) — answer honestly (user-generated content + chat →
      likely Teen/Mature; social-interaction + user-photos flags).
- [ ] **Target audience** = adults (18+). The app already enforces age ≥ 18 in onboarding.
- [ ] Ads: declare **No ads** (none currently).
- [ ] News app: No.

## 5. Subscriptions (Monetization → Products → Subscriptions)
- [ ] Create the two product IDs the client already references:
  - `premium_gold`
  - `premium_platinum`
- [ ] Set base plans + prices per market. (Client billing wiring is gated behind
      `BetaFeatureGates.PREMIUM_BILLING` — flip it on AFTER products exist + a billing-integration
      task ships.)
- [ ] For server-side validation later: create a **service account** with Play Developer API access.

## 6. ⚠️ Dating/relationship policy
- [ ] Read Play's policy on dating apps. "Matchmaking for marriage" can attract the Dating-app
      requirements (no sexual content, anti-fraud, etc.). Our guardian/modesty model is compliant in
      spirit; just make sure the listing language is "marriage/matrimony", not casual dating.

## 7. Testing track
- [ ] Upload `app-release.aab` to **Internal testing** first; add your tester emails.
- [ ] Install via the opt-in link on a real device, verify: sign-up → onboarding → discover →
      interest → chat request → chat. (This also finally exercises the **App Check** path — see §8.)

## 8. 🔴 RE-ENABLE App Check before public production
- App Check enforcement was disabled (`functions/index.js`, `enforceAppCheck: false`) because
  Play Integrity can't mint tokens for sideloaded debug APKs (PR #126). Once the app is installed
  **from Play** (internal testing counts), Play Integrity works:
  1. Register the app's Play-signing SHA-256 in Firebase Console → App Check.
  2. Flip `enforceAppCheck: true` back on in `functions/index.js` and redeploy the 5 callables.
  3. Verify chat-request / delete / admin callables still succeed from the Play build.

## 9. Pre-launch
- [ ] Review Play **Pre-launch report** (automated device testing) for crashes/accessibility.
- [ ] Confirm `versionCode` is bumped for every upload (Play rejects duplicates).

---
### Quick re-build commands (owner machine)
```
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:bundleRelease     # -> app/build/outputs/bundle/release/app-release.aab
```
