package com.mithaq.app.ui.onboarding

import androidx.annotation.StringRes
import com.mithaq.app.R
import com.mithaq.app.domain.model.LocalizedStringRes
import com.mithaq.app.domain.model.OnboardingPrivacy
import com.mithaq.app.domain.model.OnboardingSection
import com.mithaq.app.domain.model.OnboardingStep
import com.mithaq.app.domain.model.OnboardingStorageGroup
import com.mithaq.app.domain.model.OnboardingValidationRule
import com.mithaq.app.domain.model.QuestionOption
import com.mithaq.app.domain.model.QuestionType

/**
 * Single source of truth for the structured Mithaq marriage-profile onboarding (Phase 11.12A).
 *
 * ~49 questions grouped into 7 sections, each preceded by a break/info screen, finishing with a
 * summary. All visible copy is referenced through string resources (English + Arabic pairs); no
 * onboarding text is hardcoded here. Every step carries an [OnboardingStep.imageKey] for future
 * Stitch artwork (rendered as a placeholder for now — see docs/design/onboarding-image-brief.md).
 *
 * Privacy/storage metadata only controls what Android writes to `profiles/{userId}`. Android never
 * writes `publicProfiles`; the server Cloud Function owns that mirror.
 */
object MithaqOnboardingFlow {

    private fun loc(@StringRes en: Int, @StringRes ar: Int) = LocalizedStringRes(en, ar)

    private fun opt(id: String, @StringRes en: Int, @StringRes ar: Int) =
        QuestionOption(id, LocalizedStringRes(en, ar))

    // ── Shared options ────────────────────────────────────────────────────────
    private val optYes = opt("yes", R.string.onb_yes, R.string.onb_yes_ar)
    private val optNo = opt("no", R.string.onb_no, R.string.onb_no_ar)
    private val optMaybe = opt("maybe", R.string.onb_maybe, R.string.onb_maybe_ar)
    private val optDiscuss = opt("discuss", R.string.onb_discuss, R.string.onb_discuss_ar)
    private val optPreferNot = opt("prefer_not", R.string.onb_pns, R.string.onb_pns_ar)

    // ── Sections ──────────────────────────────────────────────────────────────
    private val secIdentity = OnboardingSection("identity_location", loc(R.string.onb_sec_identity, R.string.onb_sec_identity_ar))
    private val secReligion = OnboardingSection("religion_values", loc(R.string.onb_sec_religion, R.string.onb_sec_religion_ar))
    private val secMarriage = OnboardingSection("marriage_family", loc(R.string.onb_sec_marriage, R.string.onb_sec_marriage_ar))
    private val secEducation = OnboardingSection("education_work_appearance", loc(R.string.onb_sec_edu, R.string.onb_sec_edu_ar))
    private val secPersonality = OnboardingSection("personality_lifestyle", loc(R.string.onb_sec_personality, R.string.onb_sec_personality_ar))
    private val secPartner = OnboardingSection("partner_preferences", loc(R.string.onb_sec_partner, R.string.onb_sec_partner_ar))
    private val secPrivacy = OnboardingSection("privacy_trust", loc(R.string.onb_sec_privacy, R.string.onb_sec_privacy_ar))

    private val required = listOf(OnboardingValidationRule.Required)

    /** Compact question builder. */
    private fun q(
        id: String,
        section: OnboardingSection,
        type: QuestionType,
        @StringRes titleEn: Int,
        @StringRes titleAr: Int,
        group: OnboardingStorageGroup,
        fieldKey: String,
        privacy: OnboardingPrivacy,
        @StringRes helperEn: Int? = null,
        @StringRes helperAr: Int? = null,
        options: List<QuestionOption> = emptyList(),
        rules: List<OnboardingValidationRule> = required,
        optional: Boolean = false,
        mirror: Boolean = false,
        match: Boolean = false
    ): OnboardingStep = OnboardingStep(
        id = id,
        section = section,
        type = type,
        title = loc(titleEn, titleAr),
        imageKey = "onb_$id",
        helperText = if (helperEn != null && helperAr != null) loc(helperEn, helperAr) else null,
        options = options,
        validationRules = if (optional) emptyList() else rules,
        isOptional = optional,
        privacy = privacy,
        storageGroup = group,
        fieldKey = fieldKey,
        mirrorToPublic = mirror,
        contributesToMatch = match
    )

    /** Break / info screen between sections — collects no answer. */
    private fun brk(
        id: String,
        section: OnboardingSection,
        @StringRes titleEn: Int,
        @StringRes titleAr: Int,
        @StringRes subEn: Int,
        @StringRes subAr: Int
    ): OnboardingStep = OnboardingStep(
        id = id,
        section = section,
        type = QuestionType.SectionBreak,
        title = loc(titleEn, titleAr),
        imageKey = "onb_$id",
        helperText = loc(subEn, subAr),
        isOptional = true,
        privacy = OnboardingPrivacy.PUBLIC_SAFE,
        storageGroup = OnboardingStorageGroup.NONE
    )

    fun steps(): List<OnboardingStep> = listOf(
        // ════════════════ 1. Identity & Location ════════════════
        brk("brk_identity", secIdentity, R.string.onb_brk_identity_t, R.string.onb_brk_identity_t_ar, R.string.onb_brk_identity_s, R.string.onb_brk_identity_s_ar),
        q("account_type", secIdentity, QuestionType.SingleChoice, R.string.onb_account_type_t, R.string.onb_account_type_t_ar,
            OnboardingStorageGroup.BASIC_INFO, "accountType", OnboardingPrivacy.PUBLIC_SAFE, mirror = true,
            options = listOf(
                opt("male", R.string.onb_account_type_male, R.string.onb_account_type_male_ar),
                opt("female", R.string.onb_account_type_female, R.string.onb_account_type_female_ar),
                opt("guardian", R.string.onb_account_type_guardian, R.string.onb_account_type_guardian_ar)
            )),
        q("first_name", secIdentity, QuestionType.TextInput, R.string.onb_first_name_t, R.string.onb_first_name_t_ar,
            OnboardingStorageGroup.BASIC_INFO, "firstName", OnboardingPrivacy.PRIVATE,
            helperEn = R.string.onb_first_name_h, helperAr = R.string.onb_first_name_h_ar,
            rules = listOf(OnboardingValidationRule.Required, OnboardingValidationRule.MinLength(2), OnboardingValidationRule.MaxLength(40))),
        q("display_name", secIdentity, QuestionType.TextInput, R.string.onb_display_name_t, R.string.onb_display_name_t_ar,
            OnboardingStorageGroup.BASIC_INFO, "displayName", OnboardingPrivacy.PUBLIC_SAFE, mirror = true,
            helperEn = R.string.onb_display_name_h, helperAr = R.string.onb_display_name_h_ar,
            rules = listOf(OnboardingValidationRule.Required, OnboardingValidationRule.MinLength(2), OnboardingValidationRule.MaxLength(30))),
        q("gender", secIdentity, QuestionType.SingleChoice, R.string.onb_gender_t, R.string.onb_gender_t_ar,
            OnboardingStorageGroup.BASIC_INFO, "gender", OnboardingPrivacy.PUBLIC_SAFE, mirror = true, match = true,
            options = listOf(
                opt("male", R.string.onb_gender_male, R.string.onb_gender_male_ar),
                opt("female", R.string.onb_gender_female, R.string.onb_gender_female_ar)
            )),
        q("age", secIdentity, QuestionType.NumberInput, R.string.onb_age_t, R.string.onb_age_t_ar,
            OnboardingStorageGroup.BASIC_INFO, "age", OnboardingPrivacy.PUBLIC_CONTROLLED, mirror = true, match = true,
            helperEn = R.string.onb_age_h, helperAr = R.string.onb_age_h_ar,
            rules = listOf(OnboardingValidationRule.Required, OnboardingValidationRule.NumberRange(18, 77))),
        q("nationality", secIdentity, QuestionType.SearchableList, R.string.onb_nationality_t, R.string.onb_nationality_t_ar,
            OnboardingStorageGroup.BASIC_INFO, "nationality", OnboardingPrivacy.PUBLIC_SAFE, mirror = true,
            options = countryOptions()),
        q("country", secIdentity, QuestionType.SearchableList, R.string.onb_country_t, R.string.onb_country_t_ar,
            OnboardingStorageGroup.LOCATION, "country", OnboardingPrivacy.PUBLIC_SAFE, mirror = true, match = true,
            options = countryOptions()),
        q("city", secIdentity, QuestionType.TextInput, R.string.onb_city_t, R.string.onb_city_t_ar,
            OnboardingStorageGroup.LOCATION, "city", OnboardingPrivacy.PUBLIC_SAFE, mirror = true,
            rules = listOf(OnboardingValidationRule.Required, OnboardingValidationRule.MinLength(2))),
        q("willing_to_relocate", secIdentity, QuestionType.SingleChoice, R.string.onb_relocate_t, R.string.onb_relocate_t_ar,
            OnboardingStorageGroup.LOCATION, "willingToRelocate", OnboardingPrivacy.PUBLIC_CONTROLLED, match = true,
            options = listOf(optYes, optNo, optMaybe)),

        // ════════════════ 2. Religion & Values ════════════════
        brk("brk_religion", secReligion, R.string.onb_brk_religion_t, R.string.onb_brk_religion_t_ar, R.string.onb_brk_religion_s, R.string.onb_brk_religion_s_ar),
        q("sect", secReligion, QuestionType.SingleChoice, R.string.onb_sect_t, R.string.onb_sect_t_ar,
            OnboardingStorageGroup.RELIGIOUS_PRACTICE, "sect", OnboardingPrivacy.PUBLIC_CONTROLLED, match = true,
            options = listOf(
                opt("sunni", R.string.onb_sect_sunni, R.string.onb_sect_sunni_ar),
                opt("just_muslim", R.string.onb_sect_just_muslim, R.string.onb_sect_just_muslim_ar),
                opt("other", R.string.onb_sect_other, R.string.onb_sect_other_ar),
                optPreferNot
            )),
        q("religious_commitment", secReligion, QuestionType.SingleChoice, R.string.onb_commitment_t, R.string.onb_commitment_t_ar,
            OnboardingStorageGroup.RELIGIOUS_PRACTICE, "commitment", OnboardingPrivacy.PUBLIC_CONTROLLED, match = true,
            options = listOf(
                opt("very", R.string.onb_commitment_very, R.string.onb_commitment_very_ar),
                opt("practicing", R.string.onb_commitment_practicing, R.string.onb_commitment_practicing_ar),
                opt("moderate", R.string.onb_commitment_moderate, R.string.onb_commitment_moderate_ar),
                opt("learning", R.string.onb_commitment_learning, R.string.onb_commitment_learning_ar)
            )),
        q("prayer_habit", secReligion, QuestionType.SingleChoice, R.string.onb_prayer_t, R.string.onb_prayer_t_ar,
            OnboardingStorageGroup.RELIGIOUS_PRACTICE, "prayerHabit", OnboardingPrivacy.PUBLIC_CONTROLLED, match = true,
            options = listOf(
                opt("five_daily", R.string.onb_prayer_five, R.string.onb_prayer_five_ar),
                opt("most", R.string.onb_prayer_most, R.string.onb_prayer_most_ar),
                opt("sometimes", R.string.onb_prayer_sometimes, R.string.onb_prayer_sometimes_ar),
                opt("working_on", R.string.onb_prayer_working, R.string.onb_prayer_working_ar)
            )),
        q("mosque_jumuah", secReligion, QuestionType.SingleChoice, R.string.onb_mosque_t, R.string.onb_mosque_t_ar,
            OnboardingStorageGroup.RELIGIOUS_PRACTICE, "mosqueHabit", OnboardingPrivacy.PUBLIC_CONTROLLED,
            options = listOf(
                opt("always", R.string.onb_mosque_always, R.string.onb_mosque_always_ar),
                opt("usually", R.string.onb_mosque_usually, R.string.onb_mosque_usually_ar),
                opt("sometimes", R.string.onb_mosque_sometimes, R.string.onb_mosque_sometimes_ar),
                opt("rarely", R.string.onb_mosque_rarely, R.string.onb_mosque_rarely_ar)
            )),
        q("quran_reading", secReligion, QuestionType.SingleChoice, R.string.onb_quran_t, R.string.onb_quran_t_ar,
            OnboardingStorageGroup.RELIGIOUS_PRACTICE, "quranReading", OnboardingPrivacy.PUBLIC_CONTROLLED,
            options = listOf(
                opt("daily", R.string.onb_quran_daily, R.string.onb_quran_daily_ar),
                opt("weekly", R.string.onb_quran_weekly, R.string.onb_quran_weekly_ar),
                opt("sometimes", R.string.onb_quran_sometimes, R.string.onb_quran_sometimes_ar),
                opt("learning", R.string.onb_quran_learning, R.string.onb_quran_learning_ar)
            )),
        q("halal_food", secReligion, QuestionType.SingleChoice, R.string.onb_halal_t, R.string.onb_halal_t_ar,
            OnboardingStorageGroup.RELIGIOUS_PRACTICE, "halalCommitment", OnboardingPrivacy.PUBLIC_CONTROLLED,
            options = listOf(
                opt("strict", R.string.onb_halal_strict, R.string.onb_halal_strict_ar),
                opt("mostly", R.string.onb_halal_mostly, R.string.onb_halal_mostly_ar),
                opt("flexible", R.string.onb_halal_flexible, R.string.onb_halal_flexible_ar)
            )),
        q("family_values", secReligion, QuestionType.SingleChoice, R.string.onb_family_values_t, R.string.onb_family_values_t_ar,
            OnboardingStorageGroup.RELIGIOUS_PRACTICE, "familyValues", OnboardingPrivacy.PUBLIC_CONTROLLED, match = true,
            options = listOf(
                opt("traditional", R.string.onb_family_values_traditional, R.string.onb_family_values_traditional_ar),
                opt("balanced", R.string.onb_family_values_balanced, R.string.onb_family_values_balanced_ar),
                opt("modern", R.string.onb_family_values_modern, R.string.onb_family_values_modern_ar)
            )),
        q("modesty", secReligion, QuestionType.SingleChoice, R.string.onb_modesty_t, R.string.onb_modesty_t_ar,
            OnboardingStorageGroup.RELIGIOUS_PRACTICE, "modesty", OnboardingPrivacy.PUBLIC_CONTROLLED, match = true,
            helperEn = R.string.onb_modesty_h, helperAr = R.string.onb_modesty_h_ar,
            options = listOf(
                opt("niqab", R.string.onb_modesty_niqab, R.string.onb_modesty_niqab_ar),
                opt("hijab", R.string.onb_modesty_hijab, R.string.onb_modesty_hijab_ar),
                opt("modest_dress", R.string.onb_modesty_modest, R.string.onb_modesty_modest_ar),
                opt("beard_sunnah", R.string.onb_modesty_beard, R.string.onb_modesty_beard_ar),
                optPreferNot
            )),
        q("wali_involvement", secReligion, QuestionType.SingleChoice, R.string.onb_wali_t, R.string.onb_wali_t_ar,
            OnboardingStorageGroup.RELIGIOUS_PRACTICE, "waliInvolvement", OnboardingPrivacy.PUBLIC_CONTROLLED, match = true,
            helperEn = R.string.onb_wali_h, helperAr = R.string.onb_wali_h_ar,
            options = listOf(
                opt("essential", R.string.onb_wali_essential, R.string.onb_wali_essential_ar),
                opt("welcome", R.string.onb_wali_welcome, R.string.onb_wali_welcome_ar),
                opt("later", R.string.onb_wali_later, R.string.onb_wali_later_ar)
            )),

        // ════════════════ 3. Marriage & Family ════════════════
        brk("brk_marriage", secMarriage, R.string.onb_brk_marriage_t, R.string.onb_brk_marriage_t_ar, R.string.onb_brk_marriage_s, R.string.onb_brk_marriage_s_ar),
        q("marital_status", secMarriage, QuestionType.SingleChoice, R.string.onb_marital_t, R.string.onb_marital_t_ar,
            OnboardingStorageGroup.MARRIAGE_INTENT, "maritalStatus", OnboardingPrivacy.PUBLIC_SAFE, mirror = true, match = true,
            options = listOf(
                opt("single", R.string.onb_marital_single, R.string.onb_marital_single_ar),
                opt("divorced", R.string.onb_marital_divorced, R.string.onb_marital_divorced_ar),
                opt("widowed", R.string.onb_marital_widowed, R.string.onb_marital_widowed_ar),
                optDiscuss
            )),
        q("has_children", secMarriage, QuestionType.YesNo, R.string.onb_has_children_t, R.string.onb_has_children_t_ar,
            OnboardingStorageGroup.FAMILY, "hasChildren", OnboardingPrivacy.PUBLIC_CONTROLLED,
            options = listOf(optYes, optNo)),
        q("number_of_children", secMarriage, QuestionType.NumberInput, R.string.onb_num_children_t, R.string.onb_num_children_t_ar,
            OnboardingStorageGroup.FAMILY, "numberOfChildren", OnboardingPrivacy.PRIVATE,
            helperEn = R.string.onb_num_children_h, helperAr = R.string.onb_num_children_h_ar),
        q("wants_children", secMarriage, QuestionType.SingleChoice, R.string.onb_wants_children_t, R.string.onb_wants_children_t_ar,
            OnboardingStorageGroup.FAMILY, "wantsChildren", OnboardingPrivacy.PUBLIC_CONTROLLED, match = true,
            options = listOf(optYes, optMaybe, optNo, optDiscuss)),
        q("marriage_timeline", secMarriage, QuestionType.SingleChoice, R.string.onb_timeline_t, R.string.onb_timeline_t_ar,
            OnboardingStorageGroup.MARRIAGE_INTENT, "timeline", OnboardingPrivacy.PUBLIC_CONTROLLED, match = true,
            options = listOf(
                opt("asap", R.string.onb_timeline_asap, R.string.onb_timeline_asap_ar),
                opt("one_two", R.string.onb_timeline_one_two, R.string.onb_timeline_one_two_ar),
                opt("no_rush", R.string.onb_timeline_no_rush, R.string.onb_timeline_no_rush_ar),
                optDiscuss
            )),
        q("polygamy_stance", secMarriage, QuestionType.SingleChoice, R.string.onb_polygamy_t, R.string.onb_polygamy_t_ar,
            OnboardingStorageGroup.MARRIAGE_INTENT, "polygamyStance", OnboardingPrivacy.PRIVATE, match = true,
            options = listOf(
                opt("open", R.string.onb_polygamy_open, R.string.onb_polygamy_open_ar),
                opt("not_open", R.string.onb_polygamy_not_open, R.string.onb_polygamy_not_open_ar),
                optDiscuss
            )),
        q("housing_after_marriage", secMarriage, QuestionType.SingleChoice, R.string.onb_housing_t, R.string.onb_housing_t_ar,
            OnboardingStorageGroup.MARRIAGE_INTENT, "housing", OnboardingPrivacy.PRIVATE,
            options = listOf(
                opt("own_home", R.string.onb_housing_own, R.string.onb_housing_own_ar),
                opt("family_home", R.string.onb_housing_family, R.string.onb_housing_family_ar),
                opt("flexible", R.string.onb_housing_flexible, R.string.onb_housing_flexible_ar),
                optDiscuss
            )),
        q("household_responsibility", secMarriage, QuestionType.MultiChoice, R.string.onb_household_t, R.string.onb_household_t_ar,
            OnboardingStorageGroup.MARRIAGE_INTENT, "householdExpectations", OnboardingPrivacy.PRIVATE,
            helperEn = R.string.onb_household_h, helperAr = R.string.onb_household_h_ar,
            rules = listOf(OnboardingValidationRule.SelectionRange(1, 4)),
            options = listOf(
                opt("provider", R.string.onb_household_provider, R.string.onb_household_provider_ar),
                opt("homemaker", R.string.onb_household_homemaker, R.string.onb_household_homemaker_ar),
                opt("shared", R.string.onb_household_shared, R.string.onb_household_shared_ar),
                opt("flexible", R.string.onb_household_flexible, R.string.onb_household_flexible_ar)
            )),
        q("mahr_shabka", secMarriage, QuestionType.TextInput, R.string.onb_mahr_t, R.string.onb_mahr_t_ar,
            OnboardingStorageGroup.MARRIAGE_INTENT, "mahrExpectations", OnboardingPrivacy.PRIVATE,
            helperEn = R.string.onb_mahr_h, helperAr = R.string.onb_mahr_h_ar),

        // ════════════════ 4. Education / Work / Appearance ════════════════
        brk("brk_education", secEducation, R.string.onb_brk_edu_t, R.string.onb_brk_edu_t_ar, R.string.onb_brk_edu_s, R.string.onb_brk_edu_s_ar),
        q("education_level", secEducation, QuestionType.SingleChoice, R.string.onb_education_t, R.string.onb_education_t_ar,
            OnboardingStorageGroup.EDUCATION_WORK, "educationLevel", OnboardingPrivacy.PUBLIC_SAFE, mirror = true, match = true,
            options = listOf(
                opt("high_school", R.string.onb_education_high, R.string.onb_education_high_ar),
                opt("diploma", R.string.onb_education_diploma, R.string.onb_education_diploma_ar),
                opt("bachelor", R.string.onb_education_bachelor, R.string.onb_education_bachelor_ar),
                opt("master", R.string.onb_education_master, R.string.onb_education_master_ar),
                opt("phd", R.string.onb_education_phd, R.string.onb_education_phd_ar)
            )),
        q("study_field", secEducation, QuestionType.TextInput, R.string.onb_study_field_t, R.string.onb_study_field_t_ar,
            OnboardingStorageGroup.EDUCATION_WORK, "studyField", OnboardingPrivacy.PUBLIC_CONTROLLED),
        q("occupation", secEducation, QuestionType.TextInput, R.string.onb_occupation_t, R.string.onb_occupation_t_ar,
            OnboardingStorageGroup.EDUCATION_WORK, "occupation", OnboardingPrivacy.PUBLIC_CONTROLLED, mirror = true,
            rules = listOf(OnboardingValidationRule.Required, OnboardingValidationRule.MinLength(2))),
        q("employment_status", secEducation, QuestionType.SingleChoice, R.string.onb_employment_t, R.string.onb_employment_t_ar,
            OnboardingStorageGroup.EDUCATION_WORK, "employmentStatus", OnboardingPrivacy.PUBLIC_CONTROLLED,
            options = listOf(
                opt("employed", R.string.onb_employment_employed, R.string.onb_employment_employed_ar),
                opt("self_employed", R.string.onb_employment_self, R.string.onb_employment_self_ar),
                opt("student", R.string.onb_employment_student, R.string.onb_employment_student_ar),
                opt("seeking", R.string.onb_employment_seeking, R.string.onb_employment_seeking_ar),
                opt("not_working", R.string.onb_employment_not, R.string.onb_employment_not_ar)
            )),
        q("income_range", secEducation, QuestionType.SingleChoice, R.string.onb_income_t, R.string.onb_income_t_ar,
            OnboardingStorageGroup.EDUCATION_WORK, "incomeRange", OnboardingPrivacy.PRIVATE,
            helperEn = R.string.onb_income_h, helperAr = R.string.onb_income_h_ar,
            options = listOf(
                opt("low", R.string.onb_income_low, R.string.onb_income_low_ar),
                opt("medium", R.string.onb_income_medium, R.string.onb_income_medium_ar),
                opt("high", R.string.onb_income_high, R.string.onb_income_high_ar),
                optPreferNot
            )),
        q("height", secEducation, QuestionType.NumberInput, R.string.onb_height_t, R.string.onb_height_t_ar,
            OnboardingStorageGroup.APPEARANCE, "heightCm", OnboardingPrivacy.PUBLIC_CONTROLLED,
            helperEn = R.string.onb_height_h, helperAr = R.string.onb_height_h_ar,
            rules = listOf(OnboardingValidationRule.Required, OnboardingValidationRule.NumberRange(120, 220))),
        q("weight", secEducation, QuestionType.NumberInput, R.string.onb_weight_t, R.string.onb_weight_t_ar,
            OnboardingStorageGroup.APPEARANCE, "weightKg", OnboardingPrivacy.PRIVATE,
            helperEn = R.string.onb_weight_h, helperAr = R.string.onb_weight_h_ar),
        q("body_type", secEducation, QuestionType.SingleChoice, R.string.onb_body_t, R.string.onb_body_t_ar,
            OnboardingStorageGroup.APPEARANCE, "bodyType", OnboardingPrivacy.PUBLIC_CONTROLLED,
            options = listOf(
                opt("slim", R.string.onb_body_slim, R.string.onb_body_slim_ar),
                opt("athletic", R.string.onb_body_athletic, R.string.onb_body_athletic_ar),
                opt("average", R.string.onb_body_average, R.string.onb_body_average_ar),
                opt("curvy", R.string.onb_body_curvy, R.string.onb_body_curvy_ar),
                opt("plus", R.string.onb_body_plus, R.string.onb_body_plus_ar)
            )),
        q("smoking", secEducation, QuestionType.SingleChoice, R.string.onb_smoking_t, R.string.onb_smoking_t_ar,
            OnboardingStorageGroup.LIFESTYLE, "smoking", OnboardingPrivacy.PUBLIC_CONTROLLED, match = true,
            options = listOf(
                opt("no", R.string.onb_smoking_no, R.string.onb_smoking_no_ar),
                opt("occasionally", R.string.onb_smoking_occ, R.string.onb_smoking_occ_ar),
                opt("yes", R.string.onb_smoking_yes, R.string.onb_smoking_yes_ar)
            )),
        q("living_situation", secEducation, QuestionType.SingleChoice, R.string.onb_living_t, R.string.onb_living_t_ar,
            OnboardingStorageGroup.LIFESTYLE, "livingSituation", OnboardingPrivacy.PRIVATE,
            options = listOf(
                opt("with_family", R.string.onb_living_family, R.string.onb_living_family_ar),
                opt("alone", R.string.onb_living_alone, R.string.onb_living_alone_ar),
                opt("shared", R.string.onb_living_shared, R.string.onb_living_shared_ar)
            )),

        // ════════════════ 5. Personality & Lifestyle ════════════════
        brk("brk_personality", secPersonality, R.string.onb_brk_personality_t, R.string.onb_brk_personality_t_ar, R.string.onb_brk_personality_s, R.string.onb_brk_personality_s_ar),
        q("ideal_day", secPersonality, QuestionType.LongTextInput, R.string.onb_ideal_day_t, R.string.onb_ideal_day_t_ar,
            OnboardingStorageGroup.PERSONALITY, "idealDay", OnboardingPrivacy.PUBLIC_CONTROLLED,
            helperEn = R.string.onb_ideal_day_h, helperAr = R.string.onb_ideal_day_h_ar),
        q("preferred_activity", secPersonality, QuestionType.SingleChoice, R.string.onb_activity_t, R.string.onb_activity_t_ar,
            OnboardingStorageGroup.PERSONALITY, "preferredActivity", OnboardingPrivacy.PUBLIC_CONTROLLED,
            options = listOf(
                opt("outdoors", R.string.onb_activity_outdoors, R.string.onb_activity_outdoors_ar),
                opt("home", R.string.onb_activity_home, R.string.onb_activity_home_ar),
                opt("social", R.string.onb_activity_social, R.string.onb_activity_social_ar),
                opt("learning", R.string.onb_activity_learning, R.string.onb_activity_learning_ar),
                opt("worship", R.string.onb_activity_worship, R.string.onb_activity_worship_ar)
            )),
        q("hobbies", secPersonality, QuestionType.MultiChoice, R.string.onb_hobbies_t, R.string.onb_hobbies_t_ar,
            OnboardingStorageGroup.PERSONALITY, "hobbies", OnboardingPrivacy.PUBLIC_CONTROLLED,
            helperEn = R.string.onb_hobbies_h, helperAr = R.string.onb_hobbies_h_ar,
            rules = listOf(OnboardingValidationRule.SelectionRange(1, 5)),
            options = listOf(
                opt("reading", R.string.onb_hobbies_reading, R.string.onb_hobbies_reading_ar),
                opt("sports", R.string.onb_hobbies_sports, R.string.onb_hobbies_sports_ar),
                opt("cooking", R.string.onb_hobbies_cooking, R.string.onb_hobbies_cooking_ar),
                opt("travel", R.string.onb_hobbies_travel, R.string.onb_hobbies_travel_ar),
                opt("tech", R.string.onb_hobbies_tech, R.string.onb_hobbies_tech_ar),
                opt("volunteering", R.string.onb_hobbies_volunteering, R.string.onb_hobbies_volunteering_ar),
                opt("art", R.string.onb_hobbies_art, R.string.onb_hobbies_art_ar)
            )),
        q("languages_spoken", secPersonality, QuestionType.MultiChoice, R.string.onb_languages_t, R.string.onb_languages_t_ar,
            OnboardingStorageGroup.PERSONALITY, "languagesSpoken", OnboardingPrivacy.PUBLIC_SAFE, mirror = true,
            rules = listOf(OnboardingValidationRule.SelectionRange(1, 6)),
            options = listOf(
                opt("arabic", R.string.onb_languages_arabic, R.string.onb_languages_arabic_ar),
                opt("english", R.string.onb_languages_english, R.string.onb_languages_english_ar),
                opt("french", R.string.onb_languages_french, R.string.onb_languages_french_ar),
                opt("turkish", R.string.onb_languages_turkish, R.string.onb_languages_turkish_ar),
                opt("urdu", R.string.onb_languages_urdu, R.string.onb_languages_urdu_ar),
                opt("other", R.string.onb_languages_other, R.string.onb_languages_other_ar)
            )),
        q("english_level", secPersonality, QuestionType.SingleChoice, R.string.onb_english_t, R.string.onb_english_t_ar,
            OnboardingStorageGroup.PERSONALITY, "englishLevel", OnboardingPrivacy.PUBLIC_CONTROLLED,
            options = listOf(
                opt("fluent", R.string.onb_english_fluent, R.string.onb_english_fluent_ar),
                opt("good", R.string.onb_english_good, R.string.onb_english_good_ar),
                opt("basic", R.string.onb_english_basic, R.string.onb_english_basic_ar),
                opt("none", R.string.onb_english_none, R.string.onb_english_none_ar)
            )),

        // ════════════════ 6. Partner Preferences ════════════════
        brk("brk_partner", secPartner, R.string.onb_brk_partner_t, R.string.onb_brk_partner_t_ar, R.string.onb_brk_partner_s, R.string.onb_brk_partner_s_ar),
        q("partner_age_range", secPartner, QuestionType.SingleChoice, R.string.onb_partner_age_t, R.string.onb_partner_age_t_ar,
            OnboardingStorageGroup.PARTNER_PREFERENCES, "partnerAgeRange", OnboardingPrivacy.MATCH_ONLY, match = true,
            options = listOf(
                opt("18_25", R.string.onb_partner_age_18_25, R.string.onb_partner_age_18_25_ar),
                opt("25_30", R.string.onb_partner_age_25_30, R.string.onb_partner_age_25_30_ar),
                opt("30_40", R.string.onb_partner_age_30_40, R.string.onb_partner_age_30_40_ar),
                opt("40_plus", R.string.onb_partner_age_40_plus, R.string.onb_partner_age_40_plus_ar),
                opt("flexible", R.string.onb_partner_age_flexible, R.string.onb_partner_age_flexible_ar)
            )),
        q("partner_location", secPartner, QuestionType.TextInput, R.string.onb_partner_location_t, R.string.onb_partner_location_t_ar,
            OnboardingStorageGroup.PARTNER_PREFERENCES, "partnerLocation", OnboardingPrivacy.MATCH_ONLY, match = true,
            helperEn = R.string.onb_partner_location_h, helperAr = R.string.onb_partner_location_h_ar),
        q("partner_qualities", secPartner, QuestionType.MultiChoice, R.string.onb_partner_qualities_t, R.string.onb_partner_qualities_t_ar,
            OnboardingStorageGroup.PARTNER_PREFERENCES, "partnerQualities", OnboardingPrivacy.MATCH_ONLY, match = true,
            helperEn = R.string.onb_partner_qualities_h, helperAr = R.string.onb_partner_qualities_h_ar,
            rules = listOf(OnboardingValidationRule.SelectionRange(1, 4)),
            options = listOf(
                opt("religious", R.string.onb_pq_religious, R.string.onb_pq_religious_ar),
                opt("kind", R.string.onb_pq_kind, R.string.onb_pq_kind_ar),
                opt("ambitious", R.string.onb_pq_ambitious, R.string.onb_pq_ambitious_ar),
                opt("family_oriented", R.string.onb_pq_family, R.string.onb_pq_family_ar),
                opt("educated", R.string.onb_pq_educated, R.string.onb_pq_educated_ar),
                opt("humorous", R.string.onb_pq_humorous, R.string.onb_pq_humorous_ar)
            )),
        q("partner_religious_practice", secPartner, QuestionType.SingleChoice, R.string.onb_partner_religious_t, R.string.onb_partner_religious_t_ar,
            OnboardingStorageGroup.PARTNER_PREFERENCES, "partnerReligiousPractice", OnboardingPrivacy.MATCH_ONLY, match = true,
            options = listOf(
                opt("very_practicing", R.string.onb_partner_rel_very, R.string.onb_partner_rel_very_ar),
                opt("practicing", R.string.onb_partner_rel_practicing, R.string.onb_partner_rel_practicing_ar),
                opt("moderate", R.string.onb_partner_rel_moderate, R.string.onb_partner_rel_moderate_ar),
                opt("open", R.string.onb_partner_rel_open, R.string.onb_partner_rel_open_ar)
            )),
        q("partner_relocation", secPartner, QuestionType.SingleChoice, R.string.onb_partner_reloc_t, R.string.onb_partner_reloc_t_ar,
            OnboardingStorageGroup.PARTNER_PREFERENCES, "partnerRelocation", OnboardingPrivacy.MATCH_ONLY, match = true,
            options = listOf(
                opt("relocate_to_me", R.string.onb_partner_reloc_to_me, R.string.onb_partner_reloc_to_me_ar),
                opt("i_relocate", R.string.onb_partner_reloc_i_can, R.string.onb_partner_reloc_i_can_ar),
                opt("either", R.string.onb_partner_reloc_either, R.string.onb_partner_reloc_either_ar),
                optDiscuss
            )),

        // ════════════════ 7. Privacy & Trust ════════════════
        brk("brk_privacy", secPrivacy, R.string.onb_brk_privacy_t, R.string.onb_brk_privacy_t_ar, R.string.onb_brk_privacy_s, R.string.onb_brk_privacy_s_ar),
        q("photo_privacy", secPrivacy, QuestionType.PrivacyMode, R.string.onb_photo_privacy_t, R.string.onb_photo_privacy_t_ar,
            OnboardingStorageGroup.PRIVACY_TRUST, "photoPrivacy", OnboardingPrivacy.PRIVATE,
            helperEn = R.string.onb_photo_privacy_h, helperAr = R.string.onb_photo_privacy_h_ar,
            options = listOf(
                opt("on_request", R.string.onb_photo_on_request, R.string.onb_photo_on_request_ar),
                opt("after_match", R.string.onb_photo_after_match, R.string.onb_photo_after_match_ar),
                opt("wali_only", R.string.onb_photo_wali_only, R.string.onb_photo_wali_only_ar),
                opt("blurred_public", R.string.onb_photo_blurred, R.string.onb_photo_blurred_ar)
            )),
        brk("brk_trust_info", secPrivacy, R.string.onb_brk_trust_t, R.string.onb_brk_trust_t_ar, R.string.onb_brk_trust_s, R.string.onb_brk_trust_s_ar),
        q("oath_commitment", secPrivacy, QuestionType.YesNo, R.string.onb_oath_t, R.string.onb_oath_t_ar,
            OnboardingStorageGroup.PRIVACY_TRUST, "oathAccepted", OnboardingPrivacy.PUBLIC_SAFE,
            helperEn = R.string.onb_oath_h, helperAr = R.string.onb_oath_h_ar,
            options = listOf(
                opt("agree", R.string.onb_oath_agree, R.string.onb_oath_agree_ar),
                opt("not_yet", R.string.onb_oath_not_yet, R.string.onb_oath_not_yet_ar)
            )),
        OnboardingStep(
            id = "summary",
            section = secPrivacy,
            type = QuestionType.Summary,
            title = loc(R.string.onb_summary_t, R.string.onb_summary_t_ar),
            imageKey = "onb_summary",
            helperText = loc(R.string.onb_summary_s, R.string.onb_summary_s_ar),
            privacy = OnboardingPrivacy.PUBLIC_SAFE,
            storageGroup = OnboardingStorageGroup.NONE
        )
    )

    /** A compact country list for nationality/country pickers (searchable). */
    private fun countryOptions(): List<QuestionOption> = listOf(
        opt("egypt", R.string.onb_country_egypt, R.string.onb_country_egypt_ar),
        opt("saudi_arabia", R.string.onb_country_saudi, R.string.onb_country_saudi_ar),
        opt("uae", R.string.onb_country_uae, R.string.onb_country_uae_ar),
        opt("kuwait", R.string.onb_country_kuwait, R.string.onb_country_kuwait_ar),
        opt("qatar", R.string.onb_country_qatar, R.string.onb_country_qatar_ar),
        opt("bahrain", R.string.onb_country_bahrain, R.string.onb_country_bahrain_ar),
        opt("oman", R.string.onb_country_oman, R.string.onb_country_oman_ar),
        opt("jordan", R.string.onb_country_jordan, R.string.onb_country_jordan_ar),
        opt("morocco", R.string.onb_country_morocco, R.string.onb_country_morocco_ar),
        opt("algeria", R.string.onb_country_algeria, R.string.onb_country_algeria_ar),
        opt("tunisia", R.string.onb_country_tunisia, R.string.onb_country_tunisia_ar),
        opt("turkey", R.string.onb_country_turkey, R.string.onb_country_turkey_ar),
        opt("uk", R.string.onb_country_uk, R.string.onb_country_uk_ar),
        opt("usa", R.string.onb_country_usa, R.string.onb_country_usa_ar),
        opt("canada", R.string.onb_country_canada, R.string.onb_country_canada_ar),
        opt("other", R.string.onb_country_other, R.string.onb_country_other_ar)
    )
}
