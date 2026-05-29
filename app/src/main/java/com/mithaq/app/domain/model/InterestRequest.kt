package com.mithaq.app.domain.model

import java.util.Date

data class InterestRequest(
    val requestId: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val fromDisplayName: String = "",
    val toDisplayName: String = "",
    val status: String = "pending",
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)
