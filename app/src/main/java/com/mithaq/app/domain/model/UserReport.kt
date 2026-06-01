package com.mithaq.app.domain.model

import java.util.Date

data class UserReport(
    val reportId: String = "",
    val reporterUserId: String = "",
    val reportedUserId: String = "",
    val chatId: String = "",
    val reason: String = "",
    val details: String = "",
    val status: String = ReportStatus.Open,
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    val reviewedBy: String = "",
    val reviewedAt: Date? = null,
    val adminNote: String = ""
)
