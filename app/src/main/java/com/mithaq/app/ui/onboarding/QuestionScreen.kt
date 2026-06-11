package com.mithaq.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.mithaq.app.domain.model.OnboardingPrivacy
import com.mithaq.app.domain.model.OnboardingStep
import com.mithaq.app.domain.model.QuestionOption
import com.mithaq.app.domain.model.QuestionType
import com.mithaq.app.ui.onboarding.components.MithaqInputField
import com.mithaq.app.ui.onboarding.components.MithaqOnboardingImagePlaceholder
import com.mithaq.app.ui.onboarding.components.MithaqOptionCard
import com.mithaq.app.ui.onboarding.components.MithaqQuestionScaffold

@Composable
fun QuestionScreen(
    viewModel: OnboardingViewModel = remember { OnboardingViewModel() },
    userId: String = "",
    isArabic: Boolean = false,
    onExitRequested: () -> Unit = {},
    onComplete: (answeredQuestions: Int, profileCompletionPercent: Int) -> Unit = { _, _ -> }
) {
    val state by viewModel.state.collectAsState()
    val step = state.currentStep

    if (state.isComplete) {
        LaunchedEffect(state.answeredQuestions, state.profileCompletionPercent) {
            onComplete(state.answeredQuestions, state.profileCompletionPercent)
        }
        Text(stringResource(id = if (isArabic) R.string.onboarding_completing_ar else R.string.onboarding_completing))
        return
    }

    fun handleBack() {
        if (state.isLoading) return
        if (state.canGoBack) {
            viewModel.goBack()
        }
    }

    BackHandler {
        handleBack()
    }


    if (step == null) {
        Text(stringResource(id = if (isArabic) R.string.onboarding_no_questions_ar else R.string.onboarding_no_questions))
        return
    }

    val answer = state.answers[step.id]
    val canContinue = step.isOptional || answerHasValue(step, answer)
    val showSkip = false

    MithaqQuestionScaffold(
        progress = state.progress,
        sectionTitle = stringResource(id = step.section.title.resolve(isArabic)),
        title = stringResource(id = step.title.resolve(isArabic)),
        helperText = step.helperText?.let { stringResource(id = it.resolve(isArabic)) }.orEmpty(),
        validationMessage = state.validationMessage,
        canGoBack = !state.isLoading,
        canContinue = canContinue && !state.isLoading,
        showSkip = showSkip,
        isArabic = isArabic,
        onBack = ::handleBack,
        onContinue = { viewModel.continueToNext(userId) },
        onSkip = viewModel::skipOptional
    ) {
        if (state.isAuditMode) {
            androidx.compose.material3.Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = if (isArabic) R.string.profile_completion_audit_title_ar else R.string.profile_completion_audit_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = if (isArabic) R.string.profile_completion_audit_subtitle_ar else R.string.profile_completion_audit_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(id = if (isArabic) R.string.onboarding_saving_ar else R.string.onboarding_saving),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        MithaqOnboardingImagePlaceholder(imageKey = step.imageKey, isArabic = isArabic)
        Spacer(modifier = Modifier.height(18.dp))

        when {
            step.isBreak -> Unit // title + subtitle + image already shown; no input
            step.type == QuestionType.Summary -> SummaryContent(
                steps = state.steps,
                answers = state.answers,
                isArabic = isArabic
            )
            else -> {
                QuestionContent(
                    step = step,
                    answer = answer,
                    isArabic = isArabic,
                    onOptionSelected = viewModel::selectOption,
                    onTextChanged = viewModel::updateText
                )
                PrivacyNote(step = step, isArabic = isArabic)
            }
        }
    }
}

@Composable
private fun QuestionContent(
    step: OnboardingStep,
    answer: OnboardingAnswer?,
    isArabic: Boolean,
    onOptionSelected: (String) -> Unit,
    onTextChanged: (String) -> Unit
) {
    when (step.type) {
        QuestionType.SingleChoice,
        QuestionType.MultiChoice,
        QuestionType.YesNo,
        QuestionType.PrivacyMode -> OptionList(
            options = step.options,
            isArabic = isArabic,
            selectedIds = answer?.selectedOptionIds.orEmpty(),
            onOptionSelected = onOptionSelected
        )

        QuestionType.TextInput -> MithaqInputField(
            value = answer?.text.orEmpty(),
            onValueChange = onTextChanged,
            placeholder = stringResource(id = if (isArabic) R.string.onboarding_type_answer_ar else R.string.onboarding_type_answer)
        )

        QuestionType.NumberInput -> MithaqInputField(
            value = answer?.text.orEmpty(),
            onValueChange = { value ->
                if (value.all { it.isDigit() }) onTextChanged(value)
            },
            placeholder = stringResource(id = if (isArabic) R.string.onboarding_enter_number_ar else R.string.onboarding_enter_number),
            keyboardType = KeyboardType.Number
        )

        QuestionType.LongTextInput -> MithaqInputField(
            value = answer?.text.orEmpty(),
            onValueChange = onTextChanged,
            placeholder = stringResource(id = if (isArabic) R.string.onboarding_write_answer_ar else R.string.onboarding_write_answer),
            singleLine = false,
            minLines = 5
        )

        QuestionType.SearchableList -> SearchableOptionList(
            options = step.options,
            isArabic = isArabic,
            selectedIds = answer?.selectedOptionIds.orEmpty(),
            onOptionSelected = onOptionSelected
        )

        QuestionType.SectionBreak,
        QuestionType.Summary -> Unit
    }
}

@Composable
private fun OptionList(
    options: List<QuestionOption>,
    isArabic: Boolean,
    selectedIds: List<String>,
    onOptionSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { option ->
            MithaqOptionCard(
                label = stringResource(id = option.label.resolve(isArabic)),
                helperText = option.helperText?.let { stringResource(id = it.resolve(isArabic)) },
                selected = option.id in selectedIds,
                onClick = { onOptionSelected(option.id) }
            )
        }
    }
}

@Composable
private fun SearchableOptionList(
    options: List<QuestionOption>,
    isArabic: Boolean,
    selectedIds: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    // Resolve labels once so search matches the language the user actually sees.
    val resolved = options.map { it to stringResource(id = it.label.resolve(isArabic)) }
    val filtered = if (query.isBlank()) {
        resolved
    } else {
        resolved.filter { (_, label) -> label.contains(query, ignoreCase = true) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MithaqInputField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(id = if (isArabic) R.string.onboarding_search_ar else R.string.onboarding_search)
        )
        filtered.forEach { (option, label) ->
            MithaqOptionCard(
                label = label,
                helperText = option.helperText?.let { stringResource(id = it.resolve(isArabic)) },
                selected = option.id in selectedIds,
                onClick = { onOptionSelected(option.id) }
            )
        }
    }
}

@Composable
private fun PrivacyNote(step: OnboardingStep, isArabic: Boolean) {
    val noteRes = when {
        step.privacy == OnboardingPrivacy.PRIVATE ->
            if (isArabic) R.string.onb_note_private_ar else R.string.onb_note_private
        step.privacy == OnboardingPrivacy.MATCH_ONLY ->
            if (isArabic) R.string.onb_note_match_ar else R.string.onb_note_match
        step.isOptional ->
            if (isArabic) R.string.onb_note_optional_ar else R.string.onb_note_optional
        else -> return
    }
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(id = noteRes),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SummaryContent(
    steps: List<OnboardingStep>,
    answers: Map<String, OnboardingAnswer>,
    isArabic: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        steps.filter { it.isPersisted }.forEach { step ->
            val value = summaryValue(step, answers[step.id], isArabic)
            if (value.isNotBlank()) {
                SummaryRow(
                    label = stringResource(id = step.title.resolve(isArabic)),
                    value = value
                )
            }
        }
    }
}

@Composable
private fun summaryValue(
    step: OnboardingStep,
    answer: OnboardingAnswer?,
    isArabic: Boolean
): String {
    if (answer == null) return ""
    val labels = answer.selectedOptionIds
        .mapNotNull { id -> step.options.firstOrNull { it.id == id } }
        .map { stringResource(id = it.label.resolve(isArabic)) }
    return when {
        labels.isNotEmpty() -> labels.joinToString(", ")
        answer.text.isNotBlank() -> answer.text
        answer.number != null -> answer.number.toString()
        else -> ""
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
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
    if (step.isBreak || step.type == QuestionType.Summary) return true
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
        QuestionType.SectionBreak,
        QuestionType.Summary -> true
    }
}
