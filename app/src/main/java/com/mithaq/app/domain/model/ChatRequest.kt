package com.mithaq.app.domain.model

import java.util.Date

data class ChatRequest(
    val requestId: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val status: String = "pending",
    val relatedInterestRequestId: String = "",
    val requiresGuardianApproval: Boolean = false,
    val guardianApprovalStatus: String = "not_required",
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)
