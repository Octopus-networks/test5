package com.mithaq.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.mithaq.app.domain.model.PhotoRequest
import kotlinx.coroutines.tasks.await

class PhotoRequestRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val publicProfileRepository: PublicProfileRepository = PublicProfileRepository(firestore, auth)
) {
    suspend fun requestPhoto(fromUserId: String, toUserId: String): PhotoRequestResult {
        if (fromUserId.isBlank() || toUserId.isBlank()) {
            return PhotoRequestResult.Error("Could not prepare this photo request.")
        }
        if (fromUserId == toUserId) {
            return PhotoRequestResult.Error("You cannot request your own photo.")
        }
        val user = auth.currentUser
        if (user?.uid != fromUserId || !user.isEmailVerified) {
            return PhotoRequestResult.Error("Please verify your email before requesting photo access.")
        }

        return try {
            val targetProfile = publicProfileRepository.getPublicProfile(toUserId)
                ?: return PhotoRequestResult.Error("This member is not available for photo requests.")
            if (!allowsRequestBasedPhotoAccess(targetProfile.photoPrivacyMode)) {
                return PhotoRequestResult.Error("This member controls who can view their photos.")
            }

            val acceptedInterestId = acceptedInterestRequestId(fromUserId, toUserId)
                ?: return PhotoRequestResult.Error("Please send or receive accepted interest before requesting photo access.")

            val requestId = requestId(fromUserId, toUserId)
            val requestRef = firestore.collection("photoRequests").document(requestId)
            val existing = requestRef.get().await()
            if (existing.exists() && existing.getString("status") == "pending") {
                return PhotoRequestResult.AlreadyPending
            }

            val requestData = mapOf(
                "requestId" to requestId,
                "fromUserId" to fromUserId,
                "toUserId" to toUserId,
                "status" to "pending",
                "relatedInterestRequestId" to acceptedInterestId,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            requestRef.set(requestData, SetOptions.merge()).await()
            PhotoRequestResult.Success(requestId)
        } catch (e: Exception) {
            PhotoRequestResult.Error(e.localizedMessage ?: "Could not send photo request.")
        }
    }

    suspend fun getSentPhotoRequests(userId: String): List<PhotoRequest> {
        if (!canReadForUser(userId)) return emptyList()
        return firestore.collection("photoRequests")
            .whereEqualTo("fromUserId", userId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()
            .documents
            .map { it.toPhotoRequest() }
    }

    suspend fun getReceivedPhotoRequests(userId: String): List<PhotoRequest> {
        if (!canReadForUser(userId)) return emptyList()
        return firestore.collection("photoRequests")
            .whereEqualTo("toUserId", userId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()
            .documents
            .map { it.toPhotoRequest() }
    }

    suspend fun cancelPhotoRequest(requestId: String): PhotoRequestResult {
        if (requestId.isBlank()) return PhotoRequestResult.Error("Missing request id.")
        return try {
            val requestRef = firestore.collection("photoRequests").document(requestId)
            val snapshot = requestRef.get().await()
            val request = snapshot.toPhotoRequest()
            val user = auth.currentUser
            if (user?.uid != request.fromUserId || !user.isEmailVerified) {
                return PhotoRequestResult.Error("You can cancel only your own pending photo requests.")
            }
            if (request.status != "pending") {
                return PhotoRequestResult.Error("This request is no longer pending.")
            }
            requestRef.update(
                mapOf(
                    "status" to "cancelled",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            PhotoRequestResult.Success(requestId)
        } catch (e: Exception) {
            PhotoRequestResult.Error(e.localizedMessage ?: "Could not cancel photo request.")
        }
    }

    suspend fun respondToPhotoRequest(requestId: String, approved: Boolean): PhotoRequestResult {
        if (requestId.isBlank()) return PhotoRequestResult.Error("Missing request id.")
        return try {
            val requestRef = firestore.collection("photoRequests").document(requestId)
            val snapshot = requestRef.get().await()
            val request = snapshot.toPhotoRequest()
            val user = auth.currentUser
            if (user?.uid != request.toUserId || !user.isEmailVerified) {
                return PhotoRequestResult.Error("You can respond only to photo requests sent to you.")
            }
            if (request.status != "pending") {
                return PhotoRequestResult.Error("This request is no longer pending.")
            }
            requestRef.update(
                mapOf(
                    "status" to if (approved) "approved" else "declined",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            PhotoRequestResult.Success(requestId)
        } catch (e: Exception) {
            PhotoRequestResult.Error(e.localizedMessage ?: "Could not update photo request.")
        }
    }

    suspend fun getPhotoRequestStatusBetweenUsers(fromUserId: String, toUserId: String): String? {
        if (!canReadForUser(fromUserId)) return null
        return try {
            firestore.collection("photoRequests")
                .document(requestId(fromUserId, toUserId))
                .get()
                .await()
                .getString("status")
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun acceptedInterestRequestId(fromUserId: String, toUserId: String): String? {
        val sentId = "${fromUserId}_$toUserId"
        val receivedId = "${toUserId}_$fromUserId"
        val sent = firestore.collection("interestRequests").document(sentId).get().await()
        if (sent.exists() && sent.getString("status") == "accepted") return sentId
        val received = firestore.collection("interestRequests").document(receivedId).get().await()
        if (received.exists() && received.getString("status") == "accepted") return receivedId
        return null
    }

    private fun canReadForUser(userId: String): Boolean {
        val user = auth.currentUser
        return user?.uid == userId && user.isEmailVerified
    }

    private fun allowsRequestBasedPhotoAccess(mode: String): Boolean {
        return when (mode.ifBlank { "blurred_by_default" }) {
            "blurred_by_default",
            "approved_users_only" -> true
            else -> false
        }
    }

    private fun requestId(fromUserId: String, toUserId: String): String {
        return "${fromUserId}_$toUserId"
    }

    private fun DocumentSnapshot.toPhotoRequest(): PhotoRequest {
        return PhotoRequest(
            requestId = getString("requestId") ?: id,
            fromUserId = getString("fromUserId").orEmpty(),
            toUserId = getString("toUserId").orEmpty(),
            status = getString("status") ?: "pending",
            relatedInterestRequestId = getString("relatedInterestRequestId").orEmpty(),
            createdAt = getTimestamp("createdAt")?.toDate(),
            updatedAt = getTimestamp("updatedAt")?.toDate()
        )
    }
}

sealed interface PhotoRequestResult {
    data class Success(val requestId: String) : PhotoRequestResult
    data object AlreadyPending : PhotoRequestResult
    data class Error(val message: String) : PhotoRequestResult
}
