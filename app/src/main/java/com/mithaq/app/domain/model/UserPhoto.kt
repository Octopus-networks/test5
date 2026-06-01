package com.mithaq.app.domain.model

import java.util.Date

data class UserPhoto(
    val photoId: String = "",
    val userId: String = "",
    val storagePath: String = "",
    val type: String = "main",
    val status: String = "pending_review",
    val visibility: String = "approved_users_only",
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)

data class VisibleUserPhoto(
    val photo: UserPhoto,
    val downloadUrl: String
)

object PhotoVisibility {
    const val Hidden = "hidden"
    const val BlurredByDefault = "blurred_by_default"
    const val ApprovedUsersOnly = "approved_users_only"
    const val MatchedUsersOnly = "matched_users_only"

    val AllowedValues = setOf(Hidden, BlurredByDefault, ApprovedUsersOnly, MatchedUsersOnly)
}

object PhotoStatus {
    const val PendingReview = "pending_review"
    const val Approved = "approved"
    const val Rejected = "rejected"
}

object PhotoType {
    const val Main = "main"
    const val Extra = "extra"
}
