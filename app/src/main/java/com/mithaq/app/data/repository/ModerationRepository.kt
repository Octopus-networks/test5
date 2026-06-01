package com.mithaq.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mithaq.app.domain.model.ModerationPhotoReviewItem
import com.mithaq.app.domain.model.ModerationUserReport
import com.mithaq.app.domain.model.PhotoStatus
import com.mithaq.app.domain.model.PhotoType
import com.mithaq.app.domain.model.PhotoVisibility
import com.mithaq.app.domain.model.ReportStatus
import kotlinx.coroutines.tasks.await

class ModerationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun isCurrentUserModerator(): Boolean {
        val user = auth.currentUser ?: return false
        if (!user.isEmailVerified) return false
        val token = user.getIdToken(false).await()
        return token.claims["admin"] == true || token.claims["moderator"] == true
    }

    suspend fun getOpenReports(): ModerationResult<List<ModerationUserReport>> {
        if (!isCurrentUserModerator()) return ModerationResult.Error("Admin or moderator access is required.")
        return try {
            val reports = firestore.collection("reports")
                .whereEqualTo("status", ReportStatus.Open)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .map { it.toModerationReport() }
            ModerationResult.Success(reports)
        } catch (e: Exception) {
            ModerationResult.Error(e.localizedMessage ?: "Could not load open reports.")
        }
    }

    suspend fun getReport(reportId: String): ModerationResult<ModerationUserReport> {
        if (!isCurrentUserModerator()) return ModerationResult.Error("Admin or moderator access is required.")
        if (reportId.isBlank()) return ModerationResult.Error("Missing report id.")
        return try {
            val snapshot = firestore.collection("reports").document(reportId).get().await()
            if (!snapshot.exists()) {
                ModerationResult.Error("Report not found.")
            } else {
                ModerationResult.Success(snapshot.toModerationReport())
            }
        } catch (e: Exception) {
            ModerationResult.Error(e.localizedMessage ?: "Could not load report.")
        }
    }

    suspend fun updateReportStatus(
        reportId: String,
        status: String,
        adminNote: String = ""
    ): ModerationResult<Unit> {
        if (!isCurrentUserModerator()) return ModerationResult.Error("Admin or moderator access is required.")
        val reviewerId = auth.currentUser?.uid.orEmpty()
        val normalizedStatus = status.takeIf { it in ReportStatus.AllowedValues }
            ?: return ModerationResult.Error("Unsupported report status.")
        if (reportId.isBlank()) return ModerationResult.Error("Missing report id.")
        return try {
            firestore.collection("reports").document(reportId).update(
                mapOf(
                    "status" to normalizedStatus,
                    "adminNote" to adminNote.trim().take(1000),
                    "reviewedBy" to reviewerId,
                    "reviewedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            ModerationResult.Success(Unit)
        } catch (e: Exception) {
            ModerationResult.Error(e.localizedMessage ?: "Could not update report.")
        }
    }

    suspend fun markReportReviewed(reportId: String): ModerationResult<Unit> {
        return updateReportStatus(reportId, ReportStatus.Reviewed)
    }

    suspend fun getPendingPhotosForReview(): ModerationResult<List<ModerationPhotoReviewItem>> {
        if (!isCurrentUserModerator()) return ModerationResult.Error("Admin or moderator access is required.")
        return try {
            val photos = firestore.collectionGroup("photos")
                .whereEqualTo("status", PhotoStatus.PendingReview)
                .get()
                .await()
                .documents
                .map { it.toModerationPhotoReviewItem() }
                .sortedByDescending { it.createdAt }
            ModerationResult.Success(photos)
        } catch (e: Exception) {
            ModerationResult.Error(e.localizedMessage ?: "Could not load pending photos.")
        }
    }

    suspend fun approvePhoto(userId: String, photoId: String): ModerationResult<Unit> {
        return updatePhotoReviewStatus(userId, photoId, PhotoStatus.Approved)
    }

    suspend fun rejectPhoto(userId: String, photoId: String, reason: String = ""): ModerationResult<Unit> {
        return updatePhotoReviewStatus(userId, photoId, PhotoStatus.Rejected, reason)
    }

    private suspend fun updatePhotoReviewStatus(
        userId: String,
        photoId: String,
        status: String,
        reason: String = ""
    ): ModerationResult<Unit> {
        if (!isCurrentUserModerator()) return ModerationResult.Error("Admin or moderator access is required.")
        if (userId.isBlank() || photoId.isBlank()) return ModerationResult.Error("Missing photo id.")
        if (status !in setOf(PhotoStatus.Approved, PhotoStatus.Rejected)) {
            return ModerationResult.Error("Unsupported photo status.")
        }
        return try {
            firestore.collection("userPhotos")
                .document(userId)
                .collection("photos")
                .document(photoId)
                .update(
                    mapOf(
                        "status" to status,
                        "reviewReason" to reason.trim().take(500),
                        "reviewedBy" to auth.currentUser?.uid.orEmpty(),
                        "reviewedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            ModerationResult.Success(Unit)
        } catch (e: Exception) {
            ModerationResult.Error(e.localizedMessage ?: "Could not update photo review.")
        }
    }

    private fun DocumentSnapshot.toModerationReport(): ModerationUserReport {
        return ModerationUserReport(
            reportId = getString("reportId") ?: id,
            reporterUserId = getString("reporterUserId") ?: getString("reporterId").orEmpty(),
            reportedUserId = getString("reportedUserId") ?: getString("reportedId").orEmpty(),
            chatId = getString("chatId").orEmpty(),
            reason = getString("reason").orEmpty(),
            details = getString("details").orEmpty(),
            status = getString("status") ?: ReportStatus.Open,
            createdAt = getTimestamp("createdAt")?.toDate() ?: getTimestamp("timestamp")?.toDate(),
            updatedAt = getTimestamp("updatedAt")?.toDate(),
            reviewedBy = getString("reviewedBy").orEmpty(),
            reviewedAt = getTimestamp("reviewedAt")?.toDate(),
            adminNote = getString("adminNote").orEmpty()
        )
    }

    private fun DocumentSnapshot.toModerationPhotoReviewItem(): ModerationPhotoReviewItem {
        val ownerId = getString("userId")
            ?: reference.parent.parent?.id
            ?: ""
        return ModerationPhotoReviewItem(
            userId = ownerId,
            photoId = getString("photoId") ?: id,
            storagePath = getString("storagePath").orEmpty(),
            type = getString("type") ?: PhotoType.Main,
            status = getString("status") ?: PhotoStatus.PendingReview,
            visibility = getString("visibility") ?: PhotoVisibility.ApprovedUsersOnly,
            createdAt = getTimestamp("createdAt")?.toDate(),
            updatedAt = getTimestamp("updatedAt")?.toDate(),
            reviewReason = getString("reviewReason").orEmpty(),
            reviewedBy = getString("reviewedBy").orEmpty(),
            reviewedAt = getTimestamp("reviewedAt")?.toDate()
        )
    }
}

sealed interface ModerationResult<out T> {
    data class Success<T>(val value: T) : ModerationResult<T>
    data class Error(val message: String) : ModerationResult<Nothing>
}
