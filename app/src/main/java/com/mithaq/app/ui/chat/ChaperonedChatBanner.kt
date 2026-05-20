package com.mithaq.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A premium Material Design 3 Composable Banner displayed at the top of chaperoned chats.
 * Mentions clearly that the conversation is monitored/accessible by the Wali.
 */
@Composable
fun ChaperonedChatBanner(
    waliEmail: String?,
    modifier: Modifier = Modifier,
    isChaperoned: Boolean = true
) {
    if (!isChaperoned) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF00A86B).copy(alpha = 0.08f) // Very light emerald green accent
        ),
        border = CardDefaults.outlinedCardBorder(enabled = true).copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF00A86B).copy(alpha = 0.3f))
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF00A86B), // Clean emerald green secure icon
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Chaperoned Conversation",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00A86B)
                )
                Spacer(modifier = Modifier.height(2.dp))
                val description = if (!waliEmail.isNullOrBlank()) {
                    "A direct transcript of this chat is shared with the guardian ($waliEmail) to maintain transparency and security."
                } else {
                    "This chat is chaperoned. Transcripts are automatically shared with your designated guardian to ensure a blessed connection."
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
