# Stitch Onboarding Image Brief — Phase 11.12A

This document specifies the illustrations to be produced with **Google Stitch** for the structured
Mithaq onboarding flow. **No final image assets exist yet** — the app currently renders an on-brand
placeholder (`MithaqOnboardingImagePlaceholder`) for every `imageKey` below. When artwork is ready,
each asset should be added as a drawable named after its `imageKey` (e.g. `onb_account_type.png`) and
wired into the illustration renderer.

Every onboarding **question** and **break/info screen** has exactly one `imageKey`. The `imageKey`
equals `onb_` + the step id and is defined in
`app/src/main/java/com/mithaq/app/ui/onboarding/OnboardingFlow.kt`.

## Global art direction

- **Style:** modest, premium Islamic line-art. Clean vector lines, generous negative space, soft
  rounded geometry. Subtle gold (primary) and emerald (trust) accents to match the Sacred Covenant
  theme; calm, warm, reassuring tone.
- **Mood:** dignified, serious, family-oriented, hopeful. This is a marriage covenant, not casual
  dating — avoid playful/flirty cues.
- **Composition:** single focal concept per image, centered, works at ~132dp tall and full card width.
  Must read clearly in both light and dark mode and when mirrored for RTL (Arabic).

## Safety notes (apply to ALL images)

- **No real people photos.** Illustration / line-art only. No photographic faces or bodies.
- **No identifiable faces.** If a human silhouette is needed, keep it abstract/faceless and modest.
- **Modest depiction only.** Any human form must be modestly dressed; no skin-revealing or suggestive
  poses. Respect both brothers and sisters.
- **No religiously inappropriate imagery.** No depiction of Allah, the Prophet ﷺ, or sacred figures.
  Mosques, crescents, prayer beads, Qur'an (closed book) used tastefully and respectfully.
- **No nudity, alcohol, music idolatry, or culturally insensitive symbols.**
- **Neutral & inclusive** across Arab/Muslim cultures; avoid flags or nation-specific stereotypes.
- **No text baked into the image** (all copy is localized separately in EN/AR string resources).

---

## 1. Identity & Location  (`section: identity_location`)

| imageKey | Question purpose | Suggested visual concept |
|---|---|---|
| `onb_brk_identity` | Section intro: build your profile | Welcoming doorway / open path with soft gold sunrise; sense of beginning a journey |
| `onb_account_type` | Choose account type (man/woman/guardian) | Three abstract role tokens (no faces) arranged respectfully |
| `onb_first_name` | Private real first name | A name tag / signature line, lock motif to signal privacy |
| `onb_display_name` | Public display name | A profile card outline with a friendly nameplate |
| `onb_gender` | Gender | Two abstract gender symbols in balanced, modest composition |
| `onb_age` | Age | Simple calendar / hourglass line-art |
| `onb_nationality` | Nationality | Abstract globe with gentle latitude lines (no flags) |
| `onb_country` | Country of residence | Map pin over stylised globe |
| `onb_city` | City | Modest skyline silhouette with a minaret |
| `onb_willing_to_relocate` | Willingness to relocate | Suitcase + directional path, open and optimistic |

## 2. Religion & Values  (`section: religion_values`)

| imageKey | Question purpose | Suggested visual concept |
|---|---|---|
| `onb_brk_religion` | Section intro: faith & values | Open (closed) Qur'an with soft light, prayer-bead accent |
| `onb_sect` | Religious orientation | Unified geometric Islamic star pattern |
| `onb_religious_commitment` | Level of religious commitment | Ascending gentle steps toward a crescent |
| `onb_prayer_habit` | Prayer habit | Prayer mat with mihrab arch, serene |
| `onb_mosque_jumuah` | Mosque / Jumu'ah attendance | Mosque facade with dome and minaret |
| `onb_quran_reading` | Qur'an reading | Closed Qur'an on a rehl (book stand) |
| `onb_halal_food` | Halal food commitment | Halal crescent mark over a simple plate |
| `onb_family_values` | Family values | Abstract family unit (faceless silhouettes) under a roof |
| `onb_modesty` | Modesty / dress | Tasteful modest-garment line-art (hijab/kufi motifs), no faces |
| `onb_wali_involvement` | Guardian (Wali) involvement | Protective hands around a small home / two parties with a chaperone bridge |

## 3. Marriage & Family  (`section: marriage_family`)

| imageKey | Question purpose | Suggested visual concept |
|---|---|---|
| `onb_brk_marriage` | Section intro: marriage & family | Two interlocking rings with a subtle covenant knot |
| `onb_marital_status` | Marital status | Status timeline / simple life-stage icons |
| `onb_has_children` | Has children | Small abstract family with a child figure (faceless) |
| `onb_number_of_children` | Number of children (optional) | Counting dots / stepping stones |
| `onb_wants_children` | Wants children | A nurturing sprout / growing plant |
| `onb_marriage_timeline` | Marriage timeline | Calendar with a highlighted near horizon |
| `onb_polygamy_stance` | Stance on polygamy (private) | Neutral balanced-scales motif, discreet/lock accent |
| `onb_housing_after_marriage` | Housing after marriage | A modest home outline with a key |
| `onb_household_responsibility` | Household responsibility expectations | Two hands sharing a household task, balanced |
| `onb_mahr_shabka` | Mahr / shabka (private, optional) | Discreet gift box / ring with a privacy lock |

## 4. Education / Work / Appearance  (`section: education_work_appearance`)

| imageKey | Question purpose | Suggested visual concept |
|---|---|---|
| `onb_brk_education` | Section intro: education, work & you | Graduation cap + briefcase, optimistic |
| `onb_education_level` | Education level | Stacked books / graduation cap |
| `onb_study_field` | Field of study (optional) | Open notebook with abstract diagrams |
| `onb_occupation` | Occupation | Neutral tools-of-trade cluster |
| `onb_employment_status` | Employment status | Briefcase with status badge |
| `onb_income_range` | Income range (private, optional) | Discreet wallet/coins with a privacy lock |
| `onb_height` | Height | Simple measuring ruler line-art |
| `onb_weight` | Weight (private, optional) | Minimal scale icon with a privacy lock |
| `onb_body_type` | Build (optional) | Abstract modest silhouette outlines, faceless |
| `onb_smoking` | Smoking | A "no smoking"-style neutral lifestyle icon |
| `onb_living_situation` | Current living situation | A modest home with occupancy dots |

## 5. Personality & Lifestyle  (`section: personality_lifestyle`)

| imageKey | Question purpose | Suggested visual concept |
|---|---|---|
| `onb_brk_personality` | Section intro: your personality | Warm abstract portrait frame (no face), soft rays |
| `onb_ideal_day` | Ideal day (optional free text) | Sun arc over a calm daily-rhythm scene |
| `onb_preferred_activity` | Preferred activity | Cluster of gentle activity icons (outdoors/home/learning) |
| `onb_hobbies` | Hobbies (multi-select) | A tidy grid of hobby line-icons |
| `onb_languages_spoken` | Languages spoken | Speech bubbles with abstract script marks |
| `onb_english_level` | English level | Speech bubble with an "ABC/أ ب ت" abstract |

## 6. Partner Preferences  (`section: partner_preferences`)

| imageKey | Question purpose | Suggested visual concept |
|---|---|---|
| `onb_brk_partner` | Section intro: what you're looking for | A compass / guiding star toward a heart-home |
| `onb_partner_age_range` | Preferred partner age range | Gentle range slider motif |
| `onb_partner_location` | Preferred partner location (optional) | Map pin with a search ring |
| `onb_partner_qualities` | Important partner qualities | Constellation of virtue icons (heart, book, family) |
| `onb_partner_religious_practice` | Preferred religious practice | Crescent over a gentle gradient of levels |
| `onb_partner_relocation` | Relocation expectations | Two pins linked by a soft path |

## 7. Privacy & Trust  (`section: privacy_trust`)

| imageKey | Question purpose | Suggested visual concept |
|---|---|---|
| `onb_brk_privacy` | Section intro: privacy & trust | Shield with a soft heart, reassuring |
| `onb_photo_privacy` | Photo sharing preference | A framed photo behind a privacy veil / shield |
| `onb_brk_trust_info` | Info: how Mithaq keeps you safe | Shield + chaperone bridge + review checkmark, no data leaking |
| `onb_oath_commitment` | Oath of serious, honest intentions | Open hand over a covenant scroll, dignified |
| `onb_summary` | Profile complete summary | Completed profile card with a gentle gold checkmark / blessing motif |

---

### Implementation note (for whoever wires the real assets)

1. Export each asset named exactly after its `imageKey` (e.g. `onb_account_type`).
2. Add to `res/drawable*` (provide density variants or a vector drawable).
3. Replace the placeholder call in `QuestionScreen` (`MithaqOnboardingImagePlaceholder`) with a real
   image renderer that resolves `imageKey` → drawable, keeping a placeholder fallback for any missing
   asset so the flow never breaks.
4. Keep content descriptions localized (EN/AR) for accessibility.
