package com.mithaq.app.domain.model

import java.util.Date

data class PhotoRequest(
    val requestId: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val status: String = "pending",
    val relatedInterestRequestId: String = "",
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)
