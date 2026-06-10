package com.mithaq.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctionsException
import com.mithaq.app.domain.model.ChatRequest
import com.mithaq.app.service.BackendFunctions
import kotlinx.coroutines.tasks.await

class ChatRequestRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val chatRepository: ChatRepository = ChatRepository(firestore, auth)
) {
    suspend fun requestChat(fromUserId: String, toUserId: String): ChatRequestResult {
        if (fromUserId.isBlank() || toUserId.isBlank()) {
            return ChatRequestResult.Error("Could not prepare this chat request.")
        }
        if (fromUserId == toUserId) {
            return ChatRequestResult.Error("You cannot request chat with yourself.")
        }
        val user = auth.currentUser
        if (user?.uid != fromUserId || !user.isEmailVerified) {
            return ChatRequestResult.Error("Please verify your email before requesting chat.")
        }

        return try {
            // Early, friendlier check; the callable revalidates this authoritatively.
            acceptedInterestRequestId(fromUserId, toUserId)
                ?: return ChatRequestResult.Error("Please send or receive accepted interest before requesting chat.")

            val requestId = requestId(fromUserId, toUserId)
            val requestRef = firestore.collection("chatRequests").document(requestId)
            val existing = requestRef.get().await()
            if (existing.exists() && existing.getString("status") == "pending") {
                return ChatRequestResult.AlreadyPending
            }

            // The recordChatInitiation callable checks the free-tier daily quota and CREATES
            // the chatRequests document in one transaction. Firestore rules deny client
            // creates, so the cap cannot be bypassed by writing directly, and a failed
            // create no longer consumes an attempt. No fail-open: if the backend is
            // unreachable the request fails visibly instead of silently skipping the cap.
            try {
                BackendFunctions.recordChatInitiation(toUserId)
            } catch (e: FirebaseFunctionsException) {
                return when (e.code) {
                    FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> ChatRequestResult.LimitReached
                    FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
                        ChatRequestResult.Error("Please send or receive accepted interest before requesting chat.")
                    else -> ChatRequestResult.Error(e.localizedMessage ?: "Could not send chat request.")
                }
            }
            ChatRequestResult.Success(requestId)
        } catch (e: Exception) {
            ChatRequestResult.Error(e.localizedMessage ?: "Could not send chat request.")
        }
    }

    suspend fun getSentChatRequests(userId: String): List<ChatRequest> {
        if (!canReadForUser(userId)) return emptyList()
        return firestore.collection("chatRequests")
            .whereEqualTo("fromUserId", userId)
            .limit(100)
            .get()
            .await()
            .documents
            .map { it.toChatRequest() }
            .sortedByDescending { it.updatedAt ?: it.createdAt }
    }

    suspend fun getReceivedChatRequests(userId: String): List<ChatRequest> {
        if (!canReadForUser(userId)) return emptyList()
        return firestore.collection("chatRequests")
            .whereEqualTo("toUserId", userId)
            .limit(100)
            .get()
            .await()
            .documents
            .map { it.toChatRequest() }
            .sortedByDescending { it.updatedAt ?: it.createdAt }
    }

    suspend fun cancelChatRequest(requestId: String): ChatRequestResult {
        if (requestId.isBlank()) return ChatRequestResult.Error("Missing request id.")
        return try {
            val requestRef = firestore.collection("chatRequests").document(requestId)
            val snapshot = requestRef.get().await()
            val request = snapshot.toChatRequest()
            val user = auth.currentUser
            if (user?.uid != request.fromUserId || !user.isEmailVerified) {
                return ChatRequestResult.Error("You can cancel only your own pending chat requests.")
            }
            if (request.status != "pending") {
                return ChatRequestResult.Error("This request is no longer pending.")
            }
            requestRef.update(
                mapOf(
                    "status" to "cancelled",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            ChatRequestResult.Success(requestId)
        } catch (e: Exception) {
            ChatRequestResult.Error(e.localizedMessage ?: "Could not cancel chat request.")
        }
    }

    suspend fun respondToChatRequest(requestId: String, approved: Boolean): ChatRequestResult {
        if (requestId.isBlank()) return ChatRequestResult.Error("Missing request id.")
        return try {
            val requestRef = firestore.collection("chatRequests").document(requestId)
            val snapshot = requestRef.get().await()
            val request = snapshot.toChatRequest()
            val user = auth.currentUser
            if (user?.uid != request.toUserId || !user.isEmailVerified) {
                return ChatRequestResult.Error("You can respond only to chat requests sent to you.")
            }
            if (approved && request.status == "approved") {
                return when (val roomResult = chatRepository.createChatRoomFromApprovedRequest(requestId)) {
                    is ChatRoomResult.Success -> ChatRequestResult.Success(
                        requestId = requestId,
                        createdChatId = roomResult.chatRoom.chatId
                    )
                    is ChatRoomResult.Error -> ChatRequestResult.Error(roomResult.message)
                }
            }
            if (request.status != "pending") {
                return ChatRequestResult.Error("This request is no longer pending.")
            }
            val newStatus = if (approved) "approved" else "declined"
            requestRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (!approved) {
                return ChatRequestResult.Success(requestId)
            }

            return when (val roomResult = chatRepository.createChatRoomFromApprovedRequest(requestId)) {
                is ChatRoomResult.Success -> ChatRequestResult.Success(
                    requestId = requestId,
                    createdChatId = roomResult.chatRoom.chatId
                )
                is ChatRoomResult.Error -> {
                    requestRef.update(
                        mapOf(
                            "status" to "pending",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    ChatRequestResult.Error(roomResult.message)
                }
            }
        } catch (e: Exception) {
            ChatRequestResult.Error(e.localizedMessage ?: "Could not update chat request.")
        }
    }

    suspend fun getChatRequestStatusBetweenUsers(fromUserId: String, toUserId: String): String? {
        if (!canReadForUser(fromUserId)) return null
        return try {
            firestore.collection("chatRequests")
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

    private fun requestId(fromUserId: String, toUserId: String): String {
        return "${fromUserId}_$toUserId"
    }

    private fun DocumentSnapshot.toChatRequest(): ChatRequest {
        return ChatRequest(
            requestId = getString("requestId") ?: id,
            fromUserId = getString("fromUserId").orEmpty(),
            toUserId = getString("toUserId").orEmpty(),
            fromUserIsPremium = getBoolean("fromUserIsPremium") ?: false,
            status = getString("status") ?: "pending",
            relatedInterestRequestId = getString("relatedInterestRequestId").orEmpty(),
            createdChatId = getString("createdChatId").orEmpty(),
            requiresGuardianApproval = getBoolean("requiresGuardianApproval") ?: false,
            guardianApprovalStatus = getString("guardianApprovalStatus") ?: "not_required",
            createdAt = getTimestamp("createdAt")?.toDate(),
            updatedAt = getTimestamp("updatedAt")?.toDate()
        )
    }
}

sealed interface ChatRequestResult {
    data class Success(
        val requestId: String,
        val createdChatId: String? = null
    ) : ChatRequestResult
    data object AlreadyPending : ChatRequestResult
    /** The free-tier daily chat-initiation limit has been reached (upgrade to premium). */
    data object LimitReached : ChatRequestResult
    data class Error(val message: String) : ChatRequestResult
}
