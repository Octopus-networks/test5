package com.mithaq.app.domain.model

enum class QuestionType {
    SingleChoice,
    MultiChoice,
    TextInput,
    NumberInput,
    LongTextInput,
    YesNo,
    SearchableList,
    PrivacyMode,
    Summary
}

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
    val title: String,
    val description: String = ""
)

data class QuestionOption(
    val id: String,
    val label: String,
    val helperText: String = ""
)

sealed interface OnboardingValidationRule {
    data object Required : OnboardingValidationRule
    data class MinLength(val value: Int) : OnboardingValidationRule
    data class MaxLength(val value: Int) : OnboardingValidationRule
    data class NumberRange(val min: Int, val max: Int) : OnboardingValidationRule
    data class SelectionRange(val min: Int, val max: Int) : OnboardingValidationRule
}

data class OnboardingStep(
    val id: String,
    val section: OnboardingSection,
    val type: QuestionType,
    val title: String,
    val helperText: String = "",
    val illustration: IllustrationKey? = null,
    val options: List<QuestionOption> = emptyList(),
    val validationRules: List<OnboardingValidationRule> = emptyList(),
    val isOptional: Boolean = false
)

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
    val profileCompletionPercent: Int = 0
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
