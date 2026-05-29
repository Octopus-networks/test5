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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mithaq.app.domain.model.OnboardingAnswer
import com.mithaq.app.domain.model.OnboardingStep
import com.mithaq.app.domain.model.QuestionOption
import com.mithaq.app.domain.model.QuestionType
import com.mithaq.app.ui.onboarding.components.MithaqInputField
import com.mithaq.app.ui.onboarding.components.MithaqOptionCard
import com.mithaq.app.ui.onboarding.components.MithaqPrimaryButton
import com.mithaq.app.ui.onboarding.components.MithaqQuestionScaffold

@Composable
fun QuestionScreen(
    viewModel: OnboardingViewModel = remember { OnboardingViewModel() },
    onExitRequested: () -> Unit = {},
    onComplete: (Map<String, OnboardingAnswer>) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val step = state.currentStep
    var showExitConfirm by remember { mutableStateOf(false) }

    if (state.isComplete) {
        LaunchedEffect(Unit) {
            onComplete(state.answers)
        }
        Text("Completing profile setup...")
        return
    }

    fun handleBack() {
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
            title = { Text("Leave profile setup?") },
            text = { Text("Your answers are only saved in this session for now.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirm = false
                    onExitRequested()
                }) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text("Stay")
                }
            }
        )
    }

    if (step == null) {
        Text("No onboarding questions available.")
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
        canGoBack = true,
        canContinue = canContinue,
        showSkip = step.isOptional,
        onBack = ::handleBack,
        onContinue = viewModel::continueToNext,
        onSkip = viewModel::skipOptional
    ) {
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
            placeholder = "Type your answer"
        )

        QuestionType.NumberInput -> MithaqInputField(
            value = answer?.text.orEmpty(),
            onValueChange = { value ->
                if (value.all { it.isDigit() }) onTextChanged(value)
            },
            placeholder = "Enter a number",
            keyboardType = KeyboardType.Number
        )

        QuestionType.LongTextInput -> MithaqInputField(
            value = answer?.text.orEmpty(),
            onValueChange = onTextChanged,
            placeholder = "Write your answer",
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
            placeholder = "Search"
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
            text = answer?.text?.takeIf { it.isNotBlank() } ?: "Review your answers before continuing.",
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
            text = "Profile answers complete",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        steps.forEach { step ->
            val answer = answers[step.id]
            SummaryRow(step = step, answer = answer)
        }
        Spacer(modifier = Modifier.height(8.dp))
        MithaqPrimaryButton(
            text = "Finish",
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
        else -> "Not answered"
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
