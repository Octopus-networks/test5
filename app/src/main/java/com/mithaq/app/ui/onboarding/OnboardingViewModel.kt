package com.mithaq.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mithaq.app.data.repository.OnboardingRepository
import com.mithaq.app.data.repository.OnboardingSaveResult
import com.mithaq.app.domain.model.OnboardingAnswer
import com.mithaq.app.domain.model.OnboardingState
import com.mithaq.app.domain.model.OnboardingStep
import com.mithaq.app.domain.model.OnboardingValidationRule
import com.mithaq.app.domain.model.QuestionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val repository: OnboardingRepository = OnboardingRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState(steps = MithaqOnboardingFlow.steps()))
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun loadCompletionStatus(userId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val completed = repository.loadCompletionStatus(userId)
            onResult(completed)
        }
    }

    suspend fun findMissingRequiredSteps(userId: String): List<String> {
        return repository.findMissingRequiredSteps(userId)
    }

    fun startCompletionAudit(userId: String, missingStepIds: List<String>) {
        val allSteps = MithaqOnboardingFlow.steps()
        val subset = allSteps.filter { 
            it.id in missingStepIds && it.type != QuestionType.SectionBreak && it.type != QuestionType.Summary 
        }
        _state.value = OnboardingState(
            steps = subset,
            currentStepIndex = 0,
            answers = emptyMap(),
            isLoading = false,
            validationMessage = null,
            isComplete = false,
            isAuditMode = true
        )
    }

    fun startSectionEdit(sectionSteps: List<OnboardingStep>, prefill: Map<String, OnboardingAnswer>) {
        val subset = sectionSteps.filter { it.isPersisted }
        _state.value = OnboardingState(
            steps = subset,
            currentStepIndex = 0,
            answers = prefill,
            isLoading = false,
            validationMessage = null,
            isComplete = false,
            isEditMode = true
        )
    }

    fun saveCompletionCache(userId: String, completed: Boolean) {
        repository.saveCompletionCache(userId, completed)
    }

    fun selectOption(optionId: String) {
        val current = _state.value
        val step = current.currentStep ?: return
        val existing = current.answers[step.id] ?: OnboardingAnswer(stepId = step.id)

        val selected = when (step.type) {
            QuestionType.MultiChoice -> {
                if (optionId in existing.selectedOptionIds) {
                    existing.selectedOptionIds - optionId
                } else {
                    existing.selectedOptionIds + optionId
                }
            }
            else -> listOf(optionId)
        }

        saveAnswer(existing.copy(selectedOptionIds = selected))
    }

    fun updateText(value: String) {
        val current = _state.value
        val step = current.currentStep ?: return
        val existing = current.answers[step.id] ?: OnboardingAnswer(stepId = step.id)
        saveAnswer(existing.copy(text = value, number = value.toIntOrNull()))
    }

    fun goBack() {
        val current = _state.value
        if (current.currentStepIndex > 0) {
            _state.value = current.copy(
                currentStepIndex = current.currentStepIndex - 1,
                validationMessage = null,
                isComplete = false
            )
        }
    }

    fun continueToNext(userId: String? = null) {
        val current = _state.value
        if (current.isLoading) return
        val step = current.currentStep ?: return
        val answer = current.answers[step.id]
        val validationMessage = validate(step, answer)
        if (validationMessage != null) {
            _state.value = current.copy(validationMessage = validationMessage)
            return
        }

        if (current.currentStepIndex >= current.steps.lastIndex) {
            saveOnboarding(userId.orEmpty())
        } else {
            _state.value = current.copy(
                currentStepIndex = current.currentStepIndex + 1,
                validationMessage = null,
                isComplete = false
            )
        }
    }

    fun skipOptional() {
        val current = _state.value
        if (current.isLoading) return
        val step = current.currentStep ?: return
        if (!step.isOptional) return
        if (current.currentStepIndex >= current.steps.lastIndex) {
            _state.value = current.copy(validationMessage = null, isComplete = true)
        } else {
            _state.value = current.copy(
                currentStepIndex = current.currentStepIndex + 1,
                validationMessage = null
            )
        }
    }

    private fun saveOnboarding(userId: String) {
        val current = _state.value
        _state.value = current.copy(
            isLoading = true,
            validationMessage = null,
            isComplete = false
        )
        viewModelScope.launch {
            val persistenceSteps = if (current.isEditMode) {
                MithaqOnboardingFlow.steps()
            } else {
                current.steps
            }
            when (val result = repository.saveOnboardingAnswers(userId, current.answers, persistenceSteps)) {
                is OnboardingSaveResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        validationMessage = null,
                        isComplete = true,
                        answeredQuestions = result.answeredQuestions,
                        profileCompletionPercent = result.profileCompletionPercent
                    )
                }
                is OnboardingSaveResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        validationMessage = result.message,
                        isComplete = false
                    )
                }
            }
        }
    }

    private fun saveAnswer(answer: OnboardingAnswer) {
        val current = _state.value
        _state.value = current.copy(
            answers = current.answers + (answer.stepId to answer),
            validationMessage = null,
            isComplete = false
        )
    }

    private fun validate(step: OnboardingStep, answer: OnboardingAnswer?): String? {
        if (step.isOptional && answer == null) return null

        for (rule in step.validationRules) {
            when (rule) {
                OnboardingValidationRule.Required -> {
                    val hasValue = when (step.type) {
                        QuestionType.TextInput,
                        QuestionType.LongTextInput,
                        QuestionType.NumberInput -> !answer?.text.isNullOrBlank()
                        QuestionType.SingleChoice,
                        QuestionType.MultiChoice,
                        QuestionType.YesNo,
                        QuestionType.SearchableList,
                        QuestionType.PrivacyMode -> answer?.selectedOptionIds?.isNotEmpty() == true
                        QuestionType.SectionBreak,
                        QuestionType.Summary -> true
                    }
                    if (!hasValue) return "Please answer this question to continue."
                }
                is OnboardingValidationRule.MinLength -> {
                    if ((answer?.text?.trim()?.length ?: 0) < rule.value) {
                        return "Please enter at least ${rule.value} characters."
                    }
                }
                is OnboardingValidationRule.MaxLength -> {
                    if ((answer?.text?.trim()?.length ?: 0) > rule.value) {
                        return "Please keep this under ${rule.value} characters."
                    }
                }
                is OnboardingValidationRule.NumberRange -> {
                    val number = answer?.number ?: answer?.text?.toIntOrNull()
                    if (number == null || number !in rule.min..rule.max) {
                        return "Please enter a number between ${rule.min} and ${rule.max}."
                    }
                }
                is OnboardingValidationRule.SelectionRange -> {
                    val count = answer?.selectedOptionIds?.size ?: 0
                    if (count !in rule.min..rule.max) {
                        return if (rule.min == rule.max) {
                            "Please choose ${rule.min} option."
                        } else {
                            "Please choose between ${rule.min} and ${rule.max} options."
                        }
                    }
                }
            }
        }
        return null
    }
}
