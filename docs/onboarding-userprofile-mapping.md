# Onboarding → UserProfile mapping (Part 3 spec)

Goal: make the 49 onboarding answers (written to `profiles/{uid}` as nested groups) also land
in **`users/{uid}` (UserProfile)** so matching/search/own-profile actually use them.

- **Source**: `profiles/{uid}.<group>.<fieldKey>` (see `OnboardingFlow.steps()`).
- **Target**: `users/{uid}.<UserProfile field>` (see `model/UserProfile.kt`).
- Recommended mechanism: a **Cloud Function** `mirrorProfileToUser` on `profiles/{uid}` write
  (mirrors the existing `mirrorPublicProfile` pattern), so the mapping lives server-side.

Legend: ✅ direct · 🔁 value/enum conversion · ⚠️ needs option-id check · ❌ no UserProfile field (stays in `profiles/` only).

## A) Matching-critical (MUST reach users/{uid})
| Onboarding (profiles) | → UserProfile (users) | Conversion |
|---|---|---|
| basicInfo.gender (`male`/`female`) | `gender` | 🔁 → `Gender` enum (MALE/FEMALE) |
| basicInfo.age | `age` | ✅ Int |
| location.country | `country` | ✅ |
| location.city | `city` | ✅ |
| location.willingToRelocate | `relocationWillingness` | 🔁 → `RelocationWillingness` (YES/NO/OPEN) ⚠️ |
| religiousPractice.sect | `sect` | 🔁 → `Sect` (SUNNI/SHIA/IBADI/OTHER) ⚠️ |
| religiousPractice.prayerHabit | `prayerFrequency` | 🔁 → `PrayerFrequency` (ALWAYS/USUALLY/SOMETIMES/NEVER) ⚠️ |
| religiousPractice.commitment | `religiousValues` | 🔁 (very_religious/religious/not_religious) ⚠️ |
| religiousPractice.familyValues | `familyValue` | 🔁 (conservative/moderate/liberal) ⚠️ |
| religiousPractice.modesty | `modestyPreference` | 🔁 → `ModestyPreference` (NONE/HIJAB/NIQAB/DOES_NOT_MATTER) ⚠️ |
| marriageIntent.maritalStatus | `maritalStatus` | 🔁 (single/separated/widowed/divorced) ⚠️ |
| marriageIntent.polygamyStance | `polygamyAcceptance` | 🔁 → Boolean (accept→true) ⚠️ |
| family.numberOfChildren | `numberOfChildren` | ✅ Int |
| appearance.heightCm | `height` | ✅ Int |
| appearance.weightKg | `weight` | ✅ Int |
| personality.languagesSpoken | `languagesSpoken` | ✅ List<String> |
| personality.hobbies | `interestsEntertainments` | ✅ List<String> |

## B) Profile display / identity
| Onboarding | → UserProfile | Conversion |
|---|---|---|
| basicInfo.firstName | `name` | ✅ |
| basicInfo.displayName | `username` | ✅ |
| basicInfo.nationality | `nationality` | ✅ |
| basicInfo.accountType (`guardian`) | `isWaliAccount` | 🔁 → Boolean (guardian→true) ⚠️ (verify wali flow) |
| marriageIntent.timeline | `weddingTimeline` | ✅ String |
| family.wantsChildren | `wantMoreChildren` | 🔁 (yes/not_sure/no) ⚠️ |
| family.hasChildren (YesNo) | `haveChildren` | 🔁 (yes→`yes_live_at_home`? / no→`no`) ⚠️ |
| educationWork.educationLevel | `educationLevel` | ✅ String |
| educationWork.occupation | `occupation` | ✅ String |
| educationWork.employmentStatus | `employmentStatus` | ✅ String |
| educationWork.incomeRange | `incomeLevel` | 🔁 ⚠️ |
| appearance.bodyType | `bodyType` | 🔁 ⚠️ |
| lifestyle.smoking | `smokeStatus` | 🔁 (dont_smoke/...) ⚠️ |
| lifestyle.livingSituation | `livingSituation` | 🔁 ⚠️ |
| religiousPractice.quranReading | `readQuran` | 🔁 ⚠️ |
| religiousPractice.mosqueHabit | `attendReligiousService` | 🔁 ⚠️ |
| religiousPractice.halalCommitment | `eatingHabit` | 🔁 ⚠️ |
| personality.idealDay | `aboutYourself` | ✅ free text |
| privacyTrust.oathAccepted (YesNo) | `oathChecked` | 🔁 → Boolean (yes→true) |
| privacyTrust.photoPrivacy | `blurPictures` | 🔁 → Boolean ⚠️ |

## C) No UserProfile field → stay in profiles/{uid} only (not mirrored)
`religiousPractice.waliInvolvement`, `marriageIntent.housing`, `marriageIntent.householdExpectations`,
`marriageIntent.mahrExpectations`, `educationWork.studyField`, `personality.preferredActivity`,
`personality.englishLevel`, `partnerPreferences.partnerAgeRange`, `partnerPreferences.partnerLocation`,
`partnerPreferences.partnerQualities`, `partnerPreferences.partnerReligiousPractice`,
`partnerPreferences.partnerRelocation`.

> Partner-preference answers could be concatenated into UserProfile `partnerPreferences` / `idealPartner`
> free-text fields if you want them visible. Decision needed.

## Open items before coding
1. **Option-id normalization (⚠️ rows)**: confirm the onboarding option ids equal the UserProfile
   string/enum values (or add a per-option remap). Needs a read of each choice question's options.
2. **accountType=guardian** → does it really mean a Wali account here? (affects `isWaliAccount`).
3. **Partner-preference block (C)**: store as-is in profiles only, or also fold into `partnerPreferences`/`idealPartner`?
4. **Mechanism**: server-side Cloud Function (recommended) vs client write in `OnboardingRepository`.
