package com.mithaq.app.domain.model

import java.util.Date

data class ChatRoom(
    val chatId: String = "",
    val participantIds: List<String> = emptyList(),
    val participantPublicSummaries: Map<String, ChatParticipantSummary> = emptyMap(),
    val createdFromChatRequestId: String = "",
    val createdFromInterestRequestId: String = "",
    val status: String = "active",
    val guardianApprovalStatus: String = "not_required",
    val lastMessagePreview: String? = null,
    val lastMessageAt: Date? = null,
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)

data class ChatParticipantSummary(
    val userId: String = "",
    val displayName: String = "",
    val age: Int? = null,
    val city: String = "",
    val country: String = "",
    val isEmailVerified: Boolean = false,
    val isIdentityVerified: Boolean = false,
    val hasGuardian: Boolean = false,
    val photoPrivacyMode: String = "blurred_by_default"
)
