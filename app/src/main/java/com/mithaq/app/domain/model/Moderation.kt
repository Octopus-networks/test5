package com.mithaq.app.domain.model

import java.util.Date

/**
 * Phase 12 — Admin & moderation foundations.
 *
 * These models back the admin-only moderation surfaces (reports review, photo review, user
 * moderation). They are read/written exclusively through [com.mithaq.app.data.repository.AdminModerationRepository]
 * and are protected by admin-only Firestore rules — the rules are the real boundary; the in-app
 * admin check only gates the UI.
 */

/** Lifecycle of a user report. Stored in `reports/{reportId}.status`. */
enum class ReportStatus(val raw: String) {
    OPEN("open"),
    REVIEWED("reviewed"),
    DISMISSED("dismissed"),
    ACTION_TAKEN("action_taken");

    companion object {
        fun from(value: String?): ReportStatus =
            entries.firstOrNull { it.raw == value } ?: OPEN
    }
}

/** Moderation state for a user. Foundation only — full enforcement is a documented TODO. */
enum class ModerationStatus(val raw: String) {
    ACTIVE("active"),
    WARNED("warned"),
    SUSPENDED("suspended"),
    BANNED("banned");

    companion object {
        fun from(value: String?): ModerationStatus =
            entries.firstOrNull { it.raw == value } ?: ACTIVE
    }
}

/**
 * A user report, read by admins for moderation. Mirrors the document written by
 * `ReportRepository` (which uses both `reporterId`/`reportedId` and legacy `*UserId` aliases).
 */
data class Report(
    val reportId: String = "",
    val reporterId: String = "",
    val reportedId: String = "",
    val chatId: String = "",
    val reason: String = "",
    val details: String = "",
    val status: String = ReportStatus.OPEN.raw,
    val adminNote: String = "",
    val reviewedBy: String = "",
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)

/** Admin-owned moderation record for a user. Stored at `userModeration/{userId}`. */
data class UserModeration(
    val userId: String = "",
    val status: String = ModerationStatus.ACTIVE.raw,
    val note: String = "",
    val updatedBy: String = "",
    val updatedAt: Date? = null
)
