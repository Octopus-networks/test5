package com.mithaq.app.domain.model

import java.util.Date

data class UserReport(
    val reportId: String = "",
    val reporterUserId: String = "",
    val reportedUserId: String = "",
    val chatId: String = "",
    val reason: String = "",
    val details: String = "",
    val status: String = "open",
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)
