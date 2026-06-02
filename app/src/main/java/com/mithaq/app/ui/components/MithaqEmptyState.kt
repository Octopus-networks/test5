package com.mithaq.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun MithaqEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    compact: Boolean = false
) {
    // Map standard icon types to beautiful line-art illustrations
    val illustrationType = when (icon) {
        Icons.Filled.Search -> MithaqIllustrationType.ARCH
        Icons.Filled.Chat -> MithaqIllustrationType.SPEECH_BUBBLES
        Icons.Filled.Favorite -> MithaqIllustrationType.CRESCENT_STAR
        Icons.Filled.Refresh -> MithaqIllustrationType.ALERT_GEOMETRIC
        else -> MithaqIllustrationType.ARCH
    }

    val isError = icon == Icons.Filled.Refresh
    val softGold = Color(0xFFF2CA50)
    val softRed = Color(0xFFE57373)
    // The card is always dark charcoal, so text must always be light for readable
    // contrast regardless of the active light/dark theme.
    val titleColor = Color(0xFFF2EFEA) // off-white
    val messageColor = Color(0xFFBBB3A6) // muted light gray

    val borderColor = if (isError) {
        softRed.copy(alpha = 0.32f)
    } else {
        softGold.copy(alpha = 0.28f)
    }

    val illustrationTint = if (isError) {
        softRed
    } else {
        softGold
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF131313).copy(alpha = 0.86f) // Premium dark charcoal surface
        ),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (compact) 20.dp else 24.dp,
                    vertical = if (compact) 18.dp else 32.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MithaqStateIllustration(
                type = illustrationType,
                tint = illustrationTint,
                modifier = Modifier.height(if (compact) 76.dp else 130.dp)
            )
            Spacer(modifier = Modifier.height(if (compact) 12.dp else 18.dp))
            Text(
                text = title,
                style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(if (compact) 6.dp else 10.dp))
            Text(
                text = message,
                style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                color = messageColor,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(22.dp))
                OutlinedButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, illustrationTint.copy(alpha = 0.72f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = illustrationTint
                    )
                ) {
                    Text(
                        text = actionLabel,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
