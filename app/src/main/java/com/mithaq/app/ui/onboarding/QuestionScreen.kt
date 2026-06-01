package com.mithaq.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mithaq.app.R
import com.mithaq.app.domain.model.OnboardingAnswer
import com.mithaq.app.domain.model.OnboardingStep
import com.mithaq.app.domain.model.QuestionOption
import com.mithaq.app.domain.model.QuestionType
import com.mithaq.app.ui.onboarding.components.MithaqInputField
import com.mithaq.app.ui.onboarding.components.MithaqIllustrationHeader
import com.mithaq.app.ui.onboarding.components.MithaqOptionCard
import com.mithaq.app.ui.onboarding.components.MithaqPrimaryButton
import com.mithaq.app.ui.onboarding.components.MithaqQuestionScaffold

@Composable
fun QuestionScreen(
    viewModel: OnboardingViewModel = remember { OnboardingViewModel() },
    userId: String = "",
    onExitRequested: () -> Unit = {},
    onComplete: (answeredQuestions: Int, profileCompletionPercent: Int) -> Unit = { _, _ -> }
) {
    val state by viewModel.state.collectAsState()
    val step = state.currentStep
    var showExitConfirm by remember { mutableStateOf(false) }

    if (state.isComplete) {
        LaunchedEffect(state.answeredQuestions, state.profileCompletionPercent) {
            onComplete(state.answeredQuestions, state.profileCompletionPercent)
        }
        Text(stringResource(id = R.string.onboarding_completing))
        return
    }

    fun handleBack() {
        if (state.isLoading) return
        if (state.canGoBack) {
            viewModel.goBack()
        } else {
            showExitConfirm = true
        }
    }

    BackHandler {
        handleBack()
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text(stringResource(id = R.string.onboarding_leave_title)) },
            text = { Text(stringResource(id = R.string.onboarding_leave_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirm = false
                    onExitRequested()
                }) {
                    Text(stringResource(id = R.string.onboarding_leave))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text(stringResource(id = R.string.onboarding_stay))
                }
            }
        )
    }

    if (step == null) {
        Text(stringResource(id = R.string.onboarding_no_questions))
        return
    }

    val answer = state.answers[step.id]
    val canContinue = step.isOptional || answerHasValue(step, answer)

    MithaqQuestionScaffold(
        progress = state.progress,
        sectionTitle = step.section.title,
        title = step.title,
        helperText = step.helperText,
        validationMessage = state.validationMessage,
        canGoBack = !state.isLoading,
        canContinue = canContinue && !state.isLoading,
        showSkip = step.isOptional,
        onBack = ::handleBack,
        onContinue = { viewModel.continueToNext(userId) },
        onSkip = viewModel::skipOptional
    ) {
        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(id = R.string.onboarding_saving),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
        step.illustration?.let { illustration ->
            MithaqIllustrationHeader(illustration = illustration)
            Spacer(modifier = Modifier.height(18.dp))
        }
        QuestionContent(
            step = step,
            answer = answer,
            onOptionSelected = viewModel::selectOption,
            onTextChanged = viewModel::updateText
        )
    }
}

@Composable
private fun QuestionContent(
    step: OnboardingStep,
    answer: OnboardingAnswer?,
    onOptionSelected: (String) -> Unit,
    onTextChanged: (String) -> Unit
) {
    when (step.type) {
        QuestionType.SingleChoice,
        QuestionType.MultiChoice,
        QuestionType.PrivacyMode -> OptionList(
            options = step.options,
            selectedIds = answer?.selectedOptionIds.orEmpty(),
            onOptionSelected = onOptionSelected
        )

        QuestionType.YesNo -> OptionList(
            options = if (step.options.isEmpty()) {
                listOf(QuestionOption("yes", "Yes"), QuestionOption("no", "No"))
            } else {
                step.options
            },
            selectedIds = answer?.selectedOptionIds.orEmpty(),
            onOptionSelected = onOptionSelected
        )

        QuestionType.TextInput -> MithaqInputField(
            value = answer?.text.orEmpty(),
            onValueChange = onTextChanged,
            placeholder = stringResource(id = R.string.onboarding_type_answer)
        )

        QuestionType.NumberInput -> MithaqInputField(
            value = answer?.text.orEmpty(),
            onValueChange = { value ->
                if (value.all { it.isDigit() }) onTextChanged(value)
            },
            placeholder = stringResource(id = R.string.onboarding_enter_number),
            keyboardType = KeyboardType.Number
        )

        QuestionType.LongTextInput -> MithaqInputField(
            value = answer?.text.orEmpty(),
            onValueChange = onTextChanged,
            placeholder = stringResource(id = R.string.onboarding_write_answer),
            singleLine = false,
            minLines = 5
        )

        QuestionType.SearchableList -> SearchableOptionList(
            options = step.options,
            selectedIds = answer?.selectedOptionIds.orEmpty(),
            onOptionSelected = onOptionSelected
        )

        QuestionType.Summary -> SummaryQuestion(answer = answer)
    }
}

@Composable
private fun OptionList(
    options: List<QuestionOption>,
    selectedIds: List<String>,
    onOptionSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { option ->
            MithaqOptionCard(
                option = option,
                selected = option.id in selectedIds,
                onClick = { onOptionSelected(option.id) }
            )
        }
    }
}

@Composable
private fun SearchableOptionList(
    options: List<QuestionOption>,
    selectedIds: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, options) {
        if (query.isBlank()) {
            options
        } else {
            options.filter { it.label.contains(query, ignoreCase = true) }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MithaqInputField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(id = R.string.onboarding_search)
        )
        filtered.forEach { option ->
            MithaqOptionCard(
                option = option,
                selected = option.id in selectedIds,
                onClick = { onOptionSelected(option.id) }
            )
        }
    }
}

@Composable
private fun SummaryQuestion(answer: OnboardingAnswer?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Text(
            text = answer?.text?.takeIf { it.isNotBlank() } ?: stringResource(id = R.string.onboarding_review_answers),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun OnboardingSummary(
    answers: Map<String, OnboardingAnswer>,
    steps: List<OnboardingStep>,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.onboarding_answers_complete),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        steps.forEach { step ->
            val answer = answers[step.id]
            SummaryRow(step = step, answer = answer)
        }
        Spacer(modifier = Modifier.height(8.dp))
        MithaqPrimaryButton(
            text = stringResource(id = R.string.onboarding_finish),
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SummaryRow(
    step: OnboardingStep,
    answer: OnboardingAnswer?
) {
    val selectedLabels = answer?.selectedOptionIds.orEmpty()
        .mapNotNull { id -> step.options.firstOrNull { it.id == id }?.label }
    val value = when {
        selectedLabels.isNotEmpty() -> selectedLabels.joinToString()
        !answer?.text.isNullOrBlank() -> answer?.text.orEmpty()
        else -> stringResource(id = R.string.onboarding_review_answers)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun answerHasValue(step: OnboardingStep, answer: OnboardingAnswer?): Boolean {
    if (step.type == QuestionType.Summary) return true
    if (answer == null) return false
    return when (step.type) {
        QuestionType.TextInput,
        QuestionType.NumberInput,
        QuestionType.LongTextInput -> answer.text.isNotBlank()
        QuestionType.SingleChoice,
        QuestionType.MultiChoice,
        QuestionType.YesNo,
        QuestionType.SearchableList,
        QuestionType.PrivacyMode -> answer.selectedOptionIds.isNotEmpty()
        QuestionType.Summary -> true
    }
}
