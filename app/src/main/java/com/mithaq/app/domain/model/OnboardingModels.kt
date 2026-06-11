package com.mithaq.app.domain.model

import androidx.annotation.StringRes

/**
 * A localized string reference (English + Arabic resource pair).
 *
 * The app drives language through an in-app `isArabic` flag (see MithaqTheme), not the system
 * locale, so onboarding copy is stored as a resource pair and resolved at render time. The actual
 * visible text lives in res/values/strings.xml and res/values-ar/strings.xml — never hardcoded in
 * Kotlin.
 */
data class LocalizedStringRes(
    @StringRes val en: Int,
    @StringRes val ar: Int
) {
    @StringRes
    fun resolve(isArabic: Boolean): Int = if (isArabic) ar else en
}

enum class QuestionType {
    SingleChoice,
    MultiChoice,
    TextInput,
    NumberInput,
    LongTextInput,
    YesNo,
    SearchableList,
    PrivacyMode,
    SectionBreak, // info / break screen shown between sections; never collects an answer
    Summary
}

/**
 * Privacy classification for an onboarding answer. Drives how (and whether) a value may ever be
 * surfaced. Only the server (Cloud Function) mirrors anything to publicProfiles; Android never does.
 */
enum class OnboardingPrivacy {
    PUBLIC_SAFE,       // safe to show publicly as-is
    PUBLIC_CONTROLLED, // public but coarse / user-controlled (e.g. ranges, opt-in)
    PRIVATE,           // owner-only, never mirrored
    MATCH_ONLY         // used for matching only, not displayed on a profile
}

/**
 * Logical group the answer is written under inside `profiles/{userId}`. [firestoreKey] is the
 * nested map key; [NONE] marks flow-only steps (breaks / summary) that are never persisted.
 */
enum class OnboardingStorageGroup(val firestoreKey: String) {
    BASIC_INFO("basicInfo"),
    LOCATION("location"),
    RELIGIOUS_PRACTICE("religiousPractice"),
    MARRIAGE_INTENT("marriageIntent"),
    FAMILY("family"),
    EDUCATION_WORK("educationWork"),
    LIFESTYLE("lifestyle"),
    APPEARANCE("appearance"),
    PERSONALITY("personality"),
    PARTNER_PREFERENCES("partnerPreferences"),
    PRIVACY_TRUST("privacyTrust"),
    NONE("")
}

/**
 * Legacy fixed illustration set used by older screens. The structured flow uses [OnboardingStep.imageKey]
 * with a placeholder until real Stitch artwork is produced (see docs/design/onboarding-image-brief.md).
 */
enum class IllustrationKey {
    LANGUAGE,
    OATH,
    ACCOUNT_TYPE,
    BASIC_INFO,
    LOCATION,
    MARITAL_STATUS,
    EDUCATION,
    WORK,
    RELIGION,
    PRAYER,
    MARRIAGE_INTENT,
    PRIVACY,
    PHOTO_PRIVACY,
    GUARDIAN,
    DESCRIPTION,
    PROFILE_COMPLETE
}

data class OnboardingSection(
    val id: String,
    val title: LocalizedStringRes,
    val description: LocalizedStringRes? = null
)

data class QuestionOption(
    val id: String,
    val label: LocalizedStringRes,
    val helperText: LocalizedStringRes? = null
)

sealed interface OnboardingValidationRule {
    data object Required : OnboardingValidationRule
    data class MinLength(val value: Int) : OnboardingValidationRule
    data class MaxLength(val value: Int) : OnboardingValidationRule
    data class NumberRange(val min: Int, val max: Int) : OnboardingValidationRule
    data class SelectionRange(val min: Int, val max: Int) : OnboardingValidationRule
}

/**
 * A single onboarding step. Questions and break screens share this type so the index-based engine
 * (progress / back / continue from Phase 11.11) stays unchanged. Break screens use
 * [QuestionType.SectionBreak], collect no answer, and carry [OnboardingStorageGroup.NONE].
 *
 * @property id               stable questionId / breakId
 * @property imageKey         Stitch image key; rendered as a placeholder for now
 * @property fieldKey         Firestore field name within [storageGroup]; blank = not persisted
 * @property required         whether an answer is required (defaults to the inverse of [isOptional])
 * @property privacy          privacy classification of the answer
 * @property storageGroup     group the answer is written under inside profiles/{userId}
 * @property mirrorToPublic   whether this field MAY be mirrored to publicProfiles (by the server only)
 * @property contributesToMatch whether this field feeds the match score
 */
data class OnboardingStep(
    val id: String,
    val section: OnboardingSection,
    val type: QuestionType,
    val title: LocalizedStringRes,
    val imageKey: String,
    val helperText: LocalizedStringRes? = null,
    val illustration: IllustrationKey? = null,
    val options: List<QuestionOption> = emptyList(),
    val validationRules: List<OnboardingValidationRule> = emptyList(),
    val isOptional: Boolean = false,
    val required: Boolean = !isOptional,
    val privacy: OnboardingPrivacy = OnboardingPrivacy.PRIVATE,
    val storageGroup: OnboardingStorageGroup = OnboardingStorageGroup.NONE,
    val fieldKey: String = "",
    val mirrorToPublic: Boolean = false,
    val contributesToMatch: Boolean = false
) {
    val isBreak: Boolean get() = type == QuestionType.SectionBreak

    /** True when this step's answer should be written to profiles/{userId}. */
    val isPersisted: Boolean
        get() = !isBreak && type != QuestionType.Summary &&
            storageGroup != OnboardingStorageGroup.NONE && fieldKey.isNotBlank()
}

data class OnboardingAnswer(
    val stepId: String,
    val selectedOptionIds: List<String> = emptyList(),
    val text: String = "",
    val number: Int? = null
)

data class OnboardingProgress(
    val currentStep: Int,
    val totalSteps: Int
) {
    val fraction: Float
        get() = if (totalSteps <= 0) 0f else currentStep.toFloat() / totalSteps.toFloat()
}

data class OnboardingState(
    val steps: List<OnboardingStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val answers: Map<String, OnboardingAnswer> = emptyMap(),
    val validationMessage: String? = null,
    val isLoading: Boolean = false,
    val isComplete: Boolean = false,
    val answeredQuestions: Int = 0,
    val profileCompletionPercent: Int = 0,
    val isAuditMode: Boolean = false,
    val isEditMode: Boolean = false
) {
    val currentStep: OnboardingStep?
        get() = steps.getOrNull(currentStepIndex)

    val progress: OnboardingProgress
        get() = OnboardingProgress(
            currentStep = (currentStepIndex + 1).coerceAtMost(steps.size),
            totalSteps = steps.size
        )

    val canGoBack: Boolean
        get() = currentStepIndex > 0
}
