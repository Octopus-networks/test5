package com.mithaq.app.domain.model

import java.util.Date

object AdminRoleClaim {
    const val Admin = "admin"
    const val Moderator = "moderator"
}

object ReportStatus {
    const val Open = "open"
    const val Reviewed = "reviewed"
    const val Dismissed = "dismissed"
    const val ActionTaken = "action_taken"

    val AllowedValues = setOf(Open, Reviewed, Dismissed, ActionTaken)
}

object UserSafetyStatus {
    const val Normal = "normal"
    const val Warned = "warned"
    const val Restricted = "restricted"
    const val Suspended = "suspended"
    const val Banned = "banned"

    val AllowedValues = setOf(Normal, Warned, Restricted, Suspended, Banned)
}

data class ModerationUserReport(
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

data class ModerationPhotoReviewItem(
    val userId: String = "",
    val photoId: String = "",
    val storagePath: String = "",
    val type: String = PhotoType.Main,
    val status: String = PhotoStatus.PendingReview,
    val visibility: String = PhotoVisibility.ApprovedUsersOnly,
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    val reviewReason: String = "",
    val reviewedBy: String = "",
    val reviewedAt: Date? = null
)
