package com.mithaq.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mithaq.app.domain.model.ModerationStatus
import com.mithaq.app.domain.model.PhotoStatus
import com.mithaq.app.domain.model.Report
import com.mithaq.app.domain.model.ReportStatus
import com.mithaq.app.domain.model.UserModeration
import com.mithaq.app.domain.model.UserPhoto
import kotlinx.coroutines.tasks.await

data class PendingVerification(val uid: String, val name: String)

/**
 * Phase 12 — admin-only moderation data access.
 *
 * Every method here is additionally protected by admin-only Firestore rules (`isAdmin()` reads the
 * server-controlled `users/{uid}.isAdmin` flag — clients cannot set it). The in-app admin check is a
 * UI gate; these rules are the real boundary. Reads return empty / writes return [AdminActionResult.Error]
 * for non-admins because the rules deny them.
 *
 * Collections moderated:
 *  - `reports/{reportId}`            — list open, mark reviewed/dismissed/action_taken, add admin note
 *  - `userPhotos/{uid}/photos/{id}`  — list pending_review (collection group), approve / reject
 *  - `userModeration/{uid}`          — admin-owned moderation status + note (foundation)
 */
class AdminModerationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    /** Defense-in-depth admin check from the server-controlled user doc. */
    suspend fun isCurrentUserAdmin(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            firestore.collection("users").document(uid).get().await().getBoolean("isAdmin") == true
        } catch (e: Exception) {
            false
        }
    }

    // ── Reports ────────────────────────────────────────────────────────────────
    suspend fun getOpenReports(limit: Long = 50): List<Report> {
        return try {
            firestore.collection("reports")
                .whereEqualTo("status", ReportStatus.OPEN.raw)
                .limit(limit)
                .get()
                .await()
                .documents
                .map { it.toReport() }
                .sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            Log.w("AdminModerationRepo", "getOpenReports failed", e)
            emptyList()
        }
    }

    suspend fun reviewReport(
        reportId: String,
        status: ReportStatus,
        adminNote: String
    ): AdminActionResult {
        if (reportId.isBlank()) return AdminActionResult.Error("Missing report id.")
        val adminUid = auth.currentUser?.uid.orEmpty()
        return try {
            firestore.collection("reports").document(reportId).update(
                mapOf(
                    "status" to status.raw,
                    "adminNote" to adminNote.trim().take(1000),
                    "reviewedBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            AdminActionResult.Success
        } catch (e: Exception) {
            AdminActionResult.Error(e.localizedMessage ?: "Could not update this report.")
        }
    }

    // ── Photo review ─────────────────────────────────────────────────────────────
    suspend fun getPendingPhotos(limit: Long = 50): List<UserPhoto> {
        return try {
            firestore.collectionGroup("photos")
                .whereEqualTo("status", PhotoStatus.PENDING_REVIEW.raw)
                .limit(limit)
                .get()
                .await()
                .documents
                .map { it.toUserPhoto() }
                .filter { it.userId.isNotBlank() && it.photoId.isNotBlank() }
                .sortedByDescending { it.updatedAt ?: it.createdAt }
        } catch (e: Exception) {
            // Usually a missing COLLECTION_GROUP index on photos.status — the thrown error carries a
            // one-click "create index" link. Logged so the live smoke test can diagnose it.
            Log.w("AdminModerationRepo", "getPendingPhotos failed (check Firestore collection-group index on photos.status)", e)
            emptyList()
        }
    }

    suspend fun setPhotoStatus(
        userId: String,
        photoId: String,
        status: PhotoStatus,
        rejectionReason: String = ""
    ): AdminActionResult {
        if (userId.isBlank() || photoId.isBlank()) return AdminActionResult.Error("Missing photo reference.")
        return try {
            val update = mutableMapOf<String, Any>(
                "status" to status.raw,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            // Only persist a rejection reason on rejection; clear it otherwise.
            update["rejectionReason"] = if (status == PhotoStatus.REJECTED) rejectionReason.trim().take(300) else ""
            firestore.collection("userPhotos")
                .document(userId)
                .collection("photos")
                .document(photoId)
                .update(update)
                .await()
            AdminActionResult.Success
        } catch (e: Exception) {
            AdminActionResult.Error(e.localizedMessage ?: "Could not update this photo.")
        }
    }

    // ── User moderation (foundation) ─────────────────────────────────────────────
    suspend fun getUserModerationEntries(limit: Long = 50): List<UserModeration> {
        return try {
            firestore.collection("userModeration")
                .limit(limit)
                .get()
                .await()
                .documents
                .map { it.toUserModeration() }
                .sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            Log.w("AdminModerationRepo", "getUserModerationEntries failed", e)
            emptyList()
        }
    }

    suspend fun setUserModeration(
        userId: String,
        status: ModerationStatus,
        note: String
    ): AdminActionResult {
        if (userId.isBlank()) return AdminActionResult.Error("Missing user id.")
        val adminUid = auth.currentUser?.uid.orEmpty()
        return try {
            firestore.collection("userModeration").document(userId).set(
                mapOf(
                    "userId" to userId,
                    "status" to status.raw,
                    "note" to note.trim().take(1000),
                    "updatedBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            AdminActionResult.Success
        } catch (e: Exception) {
            AdminActionResult.Error(e.localizedMessage ?: "Could not update user moderation.")
        }
    }

    // ── Verification review ──────────────────────────────────────────────────────
    suspend fun getPendingVerifications(limit: Long = 50): List<PendingVerification> {
        return try {
            firestore.collection("users")
                .whereEqualTo("verificationStatus", "PENDING")
                .limit(limit)
                .get()
                .await()
                .documents
                .map { doc -> PendingVerification(doc.id, doc.getString("name") ?: "Unknown") }
        } catch (e: Exception) {
            Log.w("AdminModerationRepo", "getPendingVerifications failed", e)
            emptyList()
        }
    }

    // ── Mappers ──────────────────────────────────────────────────────────────────
    private fun DocumentSnapshot.toReport(): Report {
        return Report(
            reportId = getString("reportId") ?: id,
            reporterId = getString("reporterId") ?: getString("reporterUserId").orEmpty(),
            reportedId = getString("reportedId") ?: getString("reportedUserId").orEmpty(),
            chatId = getString("chatId").orEmpty(),
            reason = getString("reason").orEmpty(),
            details = getString("details").orEmpty(),
            status = getString("status") ?: ReportStatus.OPEN.raw,
            adminNote = getString("adminNote").orEmpty(),
            reviewedBy = getString("reviewedBy").orEmpty(),
            createdAt = getTimestamp("createdAt")?.toDate() ?: getTimestamp("timestamp")?.toDate(),
            updatedAt = getTimestamp("updatedAt")?.toDate()
        )
    }

    private fun DocumentSnapshot.toUserPhoto(): UserPhoto {
        return UserPhoto(
            photoId = getString("photoId") ?: id,
            userId = getString("userId").orEmpty(),
            storagePath = getString("storagePath").orEmpty(),
            type = getString("type") ?: "extra",
            status = getString("status") ?: PhotoStatus.PENDING_REVIEW.raw,
            visibility = getString("visibility") ?: "blurred_by_default",
            createdAt = getTimestamp("createdAt")?.toDate(),
            updatedAt = getTimestamp("updatedAt")?.toDate()
        )
    }

    private fun DocumentSnapshot.toUserModeration(): UserModeration {
        return UserModeration(
            userId = getString("userId") ?: id,
            status = getString("status") ?: ModerationStatus.ACTIVE.raw,
            note = getString("note").orEmpty(),
            updatedBy = getString("updatedBy").orEmpty(),
            updatedAt = getTimestamp("updatedAt")?.toDate()
        )
    }
}

sealed interface AdminActionResult {
    data object Success : AdminActionResult
    data class Error(val message: String) : AdminActionResult
}
