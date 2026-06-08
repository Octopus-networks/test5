package com.mithaq.app.domain.model

import java.util.Date

data class ChatMessage(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val text: String = "",
    val type: String = "text",
    val status: String = "sent",
    val reactions: Map<String, String> = emptyMap(),
    val storagePath: String = "",
    val mimeType: String = "",
    val sizeBytes: Long = 0L,
    val durationMs: Long = 0L,
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    val editedAt: Date? = null,
    val deletedAt: Date? = null
)
