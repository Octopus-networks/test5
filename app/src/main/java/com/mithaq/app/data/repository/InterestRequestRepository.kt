package com.mithaq.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mithaq.app.domain.model.InterestRequest
import kotlinx.coroutines.tasks.await

class InterestRequestRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val publicProfileRepository: PublicProfileRepository = PublicProfileRepository(firestore, auth)
) {
    suspend fun sendInterest(fromUserId: String, toUserId: String): InterestRequestResult {
        if (fromUserId.isBlank() || toUserId.isBlank()) {
            return InterestRequestResult.Error("Could not prepare this interest request.")
        }
        if (fromUserId == toUserId) {
            return InterestRequestResult.Error("You cannot send interest to yourself.")
        }
        val user = auth.currentUser
        if (user?.uid != fromUserId || !user.isEmailVerified) {
            return InterestRequestResult.Error("Please verify your email before sending interest.")
        }

        val requestId = requestId(fromUserId, toUserId)
        return try {
            val requestRef = firestore.collection("interestRequests").document(requestId)
            val existing = requestRef.get().await()
            if (existing.exists() && existing.getString("status") == "pending") {
                return InterestRequestResult.AlreadyPending
            }

            val fromPublic = publicProfileRepository.getPublicProfile(fromUserId)
            val toPublic = publicProfileRepository.getPublicProfile(toUserId)
            val requestData = mapOf(
                "requestId" to requestId,
                "fromUserId" to fromUserId,
                "toUserId" to toUserId,
                "fromDisplayName" to fromPublic?.displayName.orEmpty(),
                "toDisplayName" to toPublic?.displayName.orEmpty(),
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            requestRef.set(requestData, SetOptions.merge()).await()
            InterestRequestResult.Success(requestId)
        } catch (e: Exception) {
            InterestRequestResult.Error(
                e.localizedMessage ?: "Could not send interest request. Please try again."
            )
        }
    }

    suspend fun getSentInterestRequests(userId: String): List<InterestRequest> {
        if (!canReadForUser(userId)) return emptyList()
        return firestore.collection("interestRequests")
            .whereEqualTo("fromUserId", userId)
            .limit(100)
            .get()
            .await()
            .documents
            .map { it.toInterestRequest() }
            .sortedByDescending { it.updatedAt ?: it.createdAt }
    }

    suspend fun getReceivedInterestRequests(userId: String): List<InterestRequest> {
        if (!canReadForUser(userId)) return emptyList()
        return firestore.collection("interestRequests")
            .whereEqualTo("toUserId", userId)
            .limit(100)
            .get()
            .await()
            .documents
            .map { it.toInterestRequest() }
            .sortedByDescending { it.updatedAt ?: it.createdAt }
    }

    suspend fun cancelInterestRequest(requestId: String): InterestRequestResult {
        if (requestId.isBlank()) return InterestRequestResult.Error("Missing request id.")
        return try {
            val requestRef = firestore.collection("interestRequests").document(requestId)
            val snapshot = requestRef.get().await()
            val request = snapshot.toInterestRequest()
            val user = auth.currentUser
            if (user?.uid != request.fromUserId || !user.isEmailVerified) {
                return InterestRequestResult.Error("You can cancel only your own pending requests.")
            }
            if (request.status != "pending") {
                return InterestRequestResult.Error("This request is no longer pending.")
            }
            requestRef.update(
                mapOf(
                    "status" to "cancelled",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            InterestRequestResult.Success(requestId)
        } catch (e: Exception) {
            InterestRequestResult.Error(e.localizedMessage ?: "Could not cancel request.")
        }
    }

    suspend fun respondToInterestRequest(requestId: String, accepted: Boolean): InterestRequestResult {
        if (requestId.isBlank()) return InterestRequestResult.Error("Missing request id.")
        return try {
            val requestRef = firestore.collection("interestRequests").document(requestId)
            val snapshot = requestRef.get().await()
            val request = snapshot.toInterestRequest()
            val user = auth.currentUser
            if (user?.uid != request.toUserId || !user.isEmailVerified) {
                return InterestRequestResult.Error("You can respond only to requests sent to you.")
            }
            if (request.status != "pending") {
                return InterestRequestResult.Error("This request is no longer pending.")
            }
            requestRef.update(
                mapOf(
                    "status" to if (accepted) "accepted" else "declined",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            InterestRequestResult.Success(requestId)
        } catch (e: Exception) {
            InterestRequestResult.Error(e.localizedMessage ?: "Could not update request.")
        }
    }

    private fun canReadForUser(userId: String): Boolean {
        val user = auth.currentUser
        return user?.uid == userId && user.isEmailVerified
    }

    private fun requestId(fromUserId: String, toUserId: String): String {
        return "${fromUserId}_$toUserId"
    }

    private fun DocumentSnapshot.toInterestRequest(): InterestRequest {
        return InterestRequest(
            requestId = getString("requestId") ?: id,
            fromUserId = getString("fromUserId").orEmpty(),
            toUserId = getString("toUserId").orEmpty(),
            fromDisplayName = getString("fromDisplayName").orEmpty(),
            toDisplayName = getString("toDisplayName").orEmpty(),
            status = getString("status") ?: "pending",
            createdAt = getTimestamp("createdAt")?.toDate(),
            updatedAt = getTimestamp("updatedAt")?.toDate()
        )
    }
}

sealed interface InterestRequestResult {
    data class Success(val requestId: String) : InterestRequestResult
    data object AlreadyPending : InterestRequestResult
    data class Error(val message: String) : InterestRequestResult
}
