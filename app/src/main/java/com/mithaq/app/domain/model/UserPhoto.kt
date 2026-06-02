package com.mithaq.app.domain.model

import java.util.Date

/**
 * Privacy mode for a single photo (mirrors the public `photoPrivacyMode`).
 */
enum class PhotoVisibility(val raw: String) {
    HIDDEN("hidden"),
    BLURRED_BY_DEFAULT("blurred_by_default"),
    APPROVED_USERS_ONLY("approved_users_only"),
    MATCHED_USERS_ONLY("matched_users_only");

    companion object {
        fun from(value: String?): PhotoVisibility =
            entries.firstOrNull { it.raw == value } ?: BLURRED_BY_DEFAULT
    }
}

enum class PhotoType(val raw: String) {
    MAIN("main"),
    EXTRA("extra");

    companion object {
        fun from(value: String?): PhotoType =
            entries.firstOrNull { it.raw == value } ?: EXTRA
    }
}

enum class PhotoStatus(val raw: String) {
    PENDING_REVIEW("pending_review"),
    APPROVED("approved"),
    REJECTED("rejected");

    companion object {
        fun from(value: String?): PhotoStatus =
            entries.firstOrNull { it.raw == value } ?: PENDING_REVIEW
    }
}

/**
 * How much of an owner's photo a viewer may see. Resolved from the owner's public
 * privacy mode plus any approved photo request. Storage rules remain the real boundary
 * for the actual image bytes.
 */
enum class PhotoAccessLevel { FULL, BLURRED, LOCKED }

/**
 * Phase 11 secure photo metadata. Stored at `userPhotos/{userId}/photos/{photoId}`.
 * Never contains a download URL — only the storage path, which is itself gated by
 * Storage security rules.
 */
data class UserPhoto(
    val photoId: String = "",
    val userId: String = "",
    val storagePath: String = "",
    val type: String = PhotoType.EXTRA.raw,
    val status: String = PhotoStatus.PENDING_REVIEW.raw,
    val visibility: String = PhotoVisibility.BLURRED_BY_DEFAULT.raw,
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)
