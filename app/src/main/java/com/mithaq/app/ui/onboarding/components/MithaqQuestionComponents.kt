package com.mithaq.app.ui.onboarding.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mithaq.app.R
import com.mithaq.app.domain.model.IllustrationKey
import com.mithaq.app.domain.model.OnboardingProgress
import com.mithaq.app.domain.model.QuestionOption

@Composable
fun MithaqQuestionScaffold(
    progress: OnboardingProgress,
    sectionTitle: String,
    title: String,
    helperText: String,
    validationMessage: String?,
    canGoBack: Boolean,
    canContinue: Boolean,
    showSkip: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            MithaqSectionCard(title = sectionTitle)
            Spacer(modifier = Modifier.height(14.dp))
            MithaqProgressBar(progress = progress)
            Spacer(modifier = Modifier.height(28.dp))
            MithaqQuestionTitle(title = title, helperText = helperText)
            Spacer(modifier = Modifier.height(24.dp))
            content()
        }

        Column {
            MithaqValidationBox(message = validationMessage)
            Spacer(modifier = Modifier.height(16.dp))
            MithaqBottomNavigationButtons(
                canGoBack = canGoBack,
                canContinue = canContinue,
                showSkip = showSkip,
                onBack = onBack,
                onContinue = onContinue,
                onSkip = onSkip
            )
        }
    }
}

@Composable
fun MithaqIllustrationHeader(
    illustration: IllustrationKey,
    modifier: Modifier = Modifier
) {
    val drawableRes = when (illustration) {
        IllustrationKey.LANGUAGE -> R.drawable.illustration_language
        IllustrationKey.OATH -> R.drawable.illustration_oath
        IllustrationKey.ACCOUNT_TYPE -> R.drawable.illustration_account_type
        IllustrationKey.BASIC_INFO -> R.drawable.illustration_basic_info
        IllustrationKey.LOCATION -> R.drawable.illustration_location
        IllustrationKey.MARITAL_STATUS -> R.drawable.illustration_marital_status
        IllustrationKey.EDUCATION -> R.drawable.illustration_education
        IllustrationKey.WORK -> R.drawable.illustration_work
        IllustrationKey.RELIGION -> R.drawable.illustration_religion
        IllustrationKey.PRAYER -> R.drawable.illustration_prayer
        IllustrationKey.MARRIAGE_INTENT -> R.drawable.illustration_marriage_intent
        IllustrationKey.PRIVACY -> R.drawable.illustration_privacy
        IllustrationKey.PHOTO_PRIVACY -> R.drawable.illustration_photo_privacy
        IllustrationKey.GUARDIAN -> R.drawable.illustration_guardian
        IllustrationKey.DESCRIPTION -> R.drawable.illustration_description
        IllustrationKey.PROFILE_COMPLETE -> R.drawable.illustration_profile_complete
    }
    val descriptionRes = when (illustration) {
        IllustrationKey.LANGUAGE -> R.string.illustration_language_desc
        IllustrationKey.OATH -> R.string.illustration_oath_desc
        IllustrationKey.ACCOUNT_TYPE -> R.string.illustration_account_type_desc
        IllustrationKey.BASIC_INFO -> R.string.illustration_basic_info_desc
        IllustrationKey.LOCATION -> R.string.illustration_location_desc
        IllustrationKey.MARITAL_STATUS -> R.string.illustration_marital_status_desc
        IllustrationKey.EDUCATION -> R.string.illustration_education_desc
        IllustrationKey.WORK -> R.string.illustration_work_desc
        IllustrationKey.RELIGION -> R.string.illustration_religion_desc
        IllustrationKey.PRAYER -> R.string.illustration_prayer_desc
        IllustrationKey.MARRIAGE_INTENT -> R.string.illustration_marriage_intent_desc
        IllustrationKey.PRIVACY -> R.string.illustration_privacy_desc
        IllustrationKey.PHOTO_PRIVACY -> R.string.illustration_photo_privacy_desc
        IllustrationKey.GUARDIAN -> R.string.illustration_guardian_desc
        IllustrationKey.DESCRIPTION -> R.string.illustration_description_desc
        IllustrationKey.PROFILE_COMPLETE -> R.string.illustration_profile_complete_desc
    }

    Image(
        painter = painterResource(id = drawableRes),
        contentDescription = stringResource(id = descriptionRes),
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp)
    )
}

@Composable
fun MithaqProgressBar(
    progress: OnboardingProgress,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = progress.fraction,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${progress.currentStep} / ${progress.totalSteps}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
fun MithaqQuestionTitle(
    title: String,
    helperText: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (helperText.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = helperText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MithaqOptionCard(
    option: QuestionOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: Add small option icons after the question engine visuals are fully tested.
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val background = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (option.helperText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = option.helperText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                    .background(
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MithaqInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun MithaqValidationBox(
    message: String?,
    modifier: Modifier = Modifier
) {
    if (message == null) return

    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Start,
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(14.dp)
    )
}

@Composable
fun MithaqBottomNavigationButtons(
    canGoBack: Boolean,
    canContinue: Boolean,
    showSkip: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showSkip) {
            TextButton(
                onClick = onSkip,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Skip")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MithaqSecondaryButton(
                text = "Back",
                onClick = onBack,
                enabled = canGoBack,
                modifier = Modifier.weight(1f)
            )
            MithaqPrimaryButton(
                text = "Continue",
                onClick = onContinue,
                enabled = canContinue,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MithaqPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text = text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MithaqSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(text = text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MithaqSectionCard(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}
