package com.mithaq.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ReportRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun reportUser(
        reporterUserId: String,
        reportedUserId: String,
        chatId: String,
        reason: String,
        details: String
    ): SafetyWriteResult {
        val user = auth.currentUser
        val cleanReason = reason.trim()
        val cleanDetails = details.trim().take(500)
        if (user?.uid != reporterUserId || !user.isEmailVerified) {
            return SafetyWriteResult.Error("Please verify your email before reporting.")
        }
        if (reportedUserId.isBlank() || reporterUserId == reportedUserId) {
            return SafetyWriteResult.Error("You cannot report this member.")
        }
        if (cleanReason.isBlank()) {
            return SafetyWriteResult.Error("Please choose a report reason.")
        }

        return try {
            val reportRef = firestore.collection("reports").document()
            reportRef.set(
                mapOf(
                    "reportId" to reportRef.id,
                    "reporterId" to reporterUserId,
                    "reportedId" to reportedUserId,
                    "reporterUserId" to reporterUserId,
                    "reportedUserId" to reportedUserId,
                    "chatId" to chatId,
                    "reason" to cleanReason,
                    "details" to cleanDetails,
                    "status" to "open",
                    "reviewedBy" to null,
                    "reviewedAt" to null,
                    "adminNote" to null,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            SafetyWriteResult.Success("Report submitted. Thank you for helping keep Mithaq respectful.")
        } catch (e: Exception) {
            SafetyWriteResult.Error(e.localizedMessage ?: "Could not submit report.")
        }
    }
}

sealed interface SafetyWriteResult {
    data class Success(val message: String) : SafetyWriteResult
    data class Error(val message: String) : SafetyWriteResult
}
