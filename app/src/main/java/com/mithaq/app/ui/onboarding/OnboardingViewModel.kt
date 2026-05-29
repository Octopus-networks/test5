package com.mithaq.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mithaq.app.data.repository.OnboardingRepository
import com.mithaq.app.data.repository.OnboardingSaveResult
import com.mithaq.app.domain.model.IllustrationKey
import com.mithaq.app.domain.model.OnboardingAnswer
import com.mithaq.app.domain.model.OnboardingSection
import com.mithaq.app.domain.model.OnboardingState
import com.mithaq.app.domain.model.OnboardingStep
import com.mithaq.app.domain.model.OnboardingValidationRule
import com.mithaq.app.domain.model.QuestionOption
import com.mithaq.app.domain.model.QuestionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val repository: OnboardingRepository = OnboardingRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState(steps = sampleSteps()))
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun loadCompletionStatus(userId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val completed = repository.loadCompletionStatus(userId)
            onResult(completed)
        }
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
            when (val result = repository.saveOnboardingAnswers(userId, current.answers, current.steps)) {
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

    private fun sampleSteps(): List<OnboardingStep> {
        val basics = OnboardingSection("basics", "Basic info")
        val personal = OnboardingSection("personal", "Personal status")
        val faith = OnboardingSection("faith", "Religious practice")
        val intent = OnboardingSection("intent", "Marriage intent")

        return listOf(
            OnboardingStep(
                id = "account_type",
                section = basics,
                type = QuestionType.SingleChoice,
                title = "What type of account are you creating?",
                illustration = IllustrationKey.ACCOUNT_TYPE,
                options = listOf(
                    QuestionOption("male", "Male seeking wife"),
                    QuestionOption("female", "Female seeking husband"),
                    QuestionOption("guardian", "Guardian / Wali")
                ),
                validationRules = listOf(OnboardingValidationRule.Required)
            ),
            OnboardingStep(
                id = "name",
                section = basics,
                type = QuestionType.TextInput,
                title = "What is your name?",
                helperText = "Use the name you want serious matches to see.",
                illustration = IllustrationKey.BASIC_INFO,
                validationRules = listOf(
                    OnboardingValidationRule.Required,
                    OnboardingValidationRule.MinLength(3),
                    OnboardingValidationRule.MaxLength(30)
                )
            ),
            OnboardingStep(
                id = "age",
                section = basics,
                type = QuestionType.NumberInput,
                title = "How old are you?",
                illustration = IllustrationKey.BASIC_INFO,
                validationRules = listOf(
                    OnboardingValidationRule.Required,
                    OnboardingValidationRule.NumberRange(18, 77)
                )
            ),
            OnboardingStep(
                id = "country",
                section = basics,
                type = QuestionType.SearchableList,
                title = "Which country do you live in?",
                illustration = IllustrationKey.LOCATION,
                options = listOf(
                    QuestionOption("egypt", "Egypt"),
                    QuestionOption("saudi_arabia", "Saudi Arabia"),
                    QuestionOption("uae", "United Arab Emirates"),
                    QuestionOption("kuwait", "Kuwait"),
                    QuestionOption("qatar", "Qatar"),
                    QuestionOption("usa", "United States")
                ),
                validationRules = listOf(OnboardingValidationRule.Required)
            ),
            OnboardingStep(
                id = "city",
                section = basics,
                type = QuestionType.TextInput,
                title = "Which city are you in?",
                illustration = IllustrationKey.LOCATION,
                validationRules = listOf(
                    OnboardingValidationRule.Required,
                    OnboardingValidationRule.MinLength(2)
                )
            ),
            OnboardingStep(
                id = "marital_status",
                section = personal,
                type = QuestionType.SingleChoice,
                title = "What is your marital status?",
                illustration = IllustrationKey.MARITAL_STATUS,
                options = listOf(
                    QuestionOption("single", "Single"),
                    QuestionOption("divorced_no_children", "Divorced without children"),
                    QuestionOption("divorced_with_children", "Divorced with children"),
                    QuestionOption("widowed", "Widowed"),
                    QuestionOption("discuss_later", "Prefer to discuss later")
                ),
                validationRules = listOf(OnboardingValidationRule.Required)
            ),
            OnboardingStep(
                id = "prayer_habit",
                section = faith,
                type = QuestionType.PrivacyMode,
                title = "How would you describe your prayer habit?",
                helperText = "This is private by default and can be hidden.",
                illustration = IllustrationKey.RELIGION,
                options = listOf(
                    QuestionOption("always", "Always"),
                    QuestionOption("daily", "Daily"),
                    QuestionOption("sometimes", "Sometimes"),
                    QuestionOption("prefer_not", "Prefer not to say")
                ),
                validationRules = listOf(OnboardingValidationRule.Required)
            ),
            OnboardingStep(
                id = "marriage_timeline",
                section = intent,
                type = QuestionType.SingleChoice,
                title = "When do you plan to get married?",
                illustration = IllustrationKey.MARRIAGE_INTENT,
                options = listOf(
                    QuestionOption("soon", "As soon as possible"),
                    QuestionOption("one_two_years", "In 1 to 2 years"),
                    QuestionOption("not_hurry", "I am not in a hurry"),
                    QuestionOption("discuss_later", "Prefer to discuss later")
                ),
                validationRules = listOf(OnboardingValidationRule.Required)
            )
        )
    }
}
