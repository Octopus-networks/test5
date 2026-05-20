package com.mithaq.app.model

/**
 * Data class representing a Chat Room session in the Mithaq app.
 * Supports chaperonage configuration which flags whether a Wali receives transcripts.
 */
data class ChatRoom(
    val roomId: String = "",
    val memberIds: List<String> = emptyList(),
    val isChaperoned: Boolean = false,
    val waliEmail: String? = null,
    val lastMessage: String? = null,
    val lastMessageTimestamp: Long = 0L
)
