package com.mithaq.app.ui.photo

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

enum class PhotoAccessState {
    NONE,       // Not requested (photo remains blurred)
    PENDING,    // Request sent, waiting for target user approval
    APPROVED    // Approved (photo unblurs)
}

/**
 * Manager class for Feature 2's Multi-Stage Modesty Photo Unlock.
 * Coordinates photo permission requests and approvals in Firestore.
 */
class PhotoAccessManager(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Sends a request to view a target user's unblurred photo.
     */
    suspend fun requestPhotoAccess(currentUserId: String, targetUserId: String): Boolean {
        return try {
            firestore.collection("users")
                .document(targetUserId)
                .update("photoAccessRequests", FieldValue.arrayUnion(currentUserId))
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Approves a request from another user to view the current user's unblurred photo.
     */
    suspend fun approvePhotoAccess(currentUserId: String, requestingUserId: String): Boolean {
        return try {
            val userRef = firestore.collection("users").document(currentUserId)
            
            // Perform atomic transaction: approve user and remove from pending requests list
            firestore.runTransaction { transaction ->
                transaction.update(userRef, "photoAccessApprovedUsers", FieldValue.arrayUnion(requestingUserId))
                transaction.update(userRef, "photoAccessRequests", FieldValue.arrayRemove(requestingUserId))
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Revokes another user's access to view the current user's unblurred photo.
     */
    suspend fun revokePhotoAccess(currentUserId: String, approvedUserId: String): Boolean {
        return try {
            firestore.collection("users")
                .document(currentUserId)
                .update("photoAccessApprovedUsers", FieldValue.arrayRemove(approvedUserId))
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Determines whether the current user is permitted to view the target user's unblurred photo.
     */
    suspend fun checkPhotoAccessState(currentUserId: String, targetUserId: String): PhotoAccessState {
        return try {
            val doc = firestore.collection("users")
                .document(targetUserId)
                .get()
                .await()

            if (!doc.exists()) return PhotoAccessState.NONE

            val approvedUsers = doc.get("photoAccessApprovedUsers") as? List<*> ?: emptyList<Any>()
            val pendingRequests = doc.get("photoAccessRequests") as? List<*> ?: emptyList<Any>()

            when {
                approvedUsers.contains(currentUserId) -> PhotoAccessState.APPROVED
                pendingRequests.contains(currentUserId) -> PhotoAccessState.PENDING
                else -> PhotoAccessState.NONE
            }
        } catch (e: Exception) {
            PhotoAccessState.NONE
        }
    }
}
