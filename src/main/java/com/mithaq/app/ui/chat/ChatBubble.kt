package com.mithaq.app.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * An upgraded Chat Bubble Composable representing Feature 4: In-Chat Instant Translation.
 * Renders a chat message with a dedicated 'Translate' toggle. Handles loading spinners
 * and supports toggling between the translated text and the original message.
 */
@Composable
fun ChatBubble(
    messageText: String,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier,
    translationHelper: TranslationHelper = remember { MockTranslationHelper() },
    targetLanguageCode: String = "en"
) {
    val coroutineScope = rememberCoroutineScope()
    
    // States for translation flow
    var translatedText by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }
    var showTranslated by remember { mutableStateOf(false) }

    val bubbleShape = if (isCurrentUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    val bubbleColor = if (isCurrentUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isCurrentUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val alignment = if (isCurrentUser) Alignment.End else Alignment.Start

    Column(
        horizontalAlignment = alignment,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            // Translate Trigger Icon (Left of bubble for incoming messages, right of bubble for outgoing messages)
            if (!isCurrentUser) {
                TranslateIconBtn(
                    isTranslating = isTranslating,
                    showTranslated = showTranslated,
                    onClick = {
                        if (translatedText != null) {
                            showTranslated = !showTranslated
                        } else {
                            isTranslating = true
                            coroutineScope.launch {
                                val result = translationHelper.translateText(messageText, targetLanguageCode)
                                translatedText = result
                                showTranslated = true
                                isTranslating = false
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Chat Bubble Main Card
            Surface(
                color = bubbleColor,
                shape = bubbleShape,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .animateContentSize()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Display either original or translated text
                    val displayText = if (showTranslated && translatedText != null) {
                        translatedText!!
                    } else {
                        messageText
                    }

                    Text(
                        text = displayText,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )

                    // Translated Indicator Badge inside bubble
                    AnimatedVisibility(visible = showTranslated && translatedText != null) {
                        Column {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Translated • Show original",
                                color = textColor.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    fontStyle = FontStyle.Italic
                                ),
                                modifier = Modifier.clickable {
                                    showTranslated = false
                                }
                            )
                        }
                    }
                }
            }

            if (isCurrentUser) {
                Spacer(modifier = Modifier.width(4.dp))
                TranslateIconBtn(
                    isTranslating = isTranslating,
                    showTranslated = showTranslated,
                    onClick = {
                        if (translatedText != null) {
                            showTranslated = !showTranslated
                        } else {
                            isTranslating = true
                            coroutineScope.launch {
                                val result = translationHelper.translateText(messageText, targetLanguageCode)
                                translatedText = result
                                showTranslated = true
                                isTranslating = false
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Clean reusable button representing translation trigger.
 */
@Composable
private fun TranslateIconBtn(
    isTranslating: Boolean,
    showTranslated: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(32.dp)
    ) {
        if (isTranslating) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh, // Standard translate / refresh transition icon
                    contentDescription = "Translate",
                    tint = if (showTranslated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
