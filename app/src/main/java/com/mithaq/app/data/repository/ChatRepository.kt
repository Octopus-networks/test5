package com.mithaq.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mithaq.app.domain.model.ChatParticipantSummary
import com.mithaq.app.domain.model.ChatRoom
import com.mithaq.app.domain.model.PublicProfile
import kotlinx.coroutines.tasks.await

class ChatRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val publicProfileRepository: PublicProfileRepository = PublicProfileRepository(firestore, auth)
) {
    suspend fun createChatRoomFromApprovedRequest(chatRequestId: String): ChatRoomResult {
        if (chatRequestId.isBlank()) return ChatRoomResult.Error("Missing chat request id.")
        val user = auth.currentUser
        if (user?.isEmailVerified != true) {
            return ChatRoomResult.Error("Please verify your email before creating a chat room.")
        }

        return try {
            val requestRef = firestore.collection("chatRequests").document(chatRequestId)
            val requestSnapshot = requestRef.get().await()
            if (!requestSnapshot.exists()) {
                return ChatRoomResult.Error("Chat request was not found.")
            }
            val fromUserId = requestSnapshot.getString("fromUserId").orEmpty()
            val toUserId = requestSnapshot.getString("toUserId").orEmpty()
            val participantIds = normalizedParticipantIds(fromUserId, toUserId)
                ?: return ChatRoomResult.Error("Chat room requires exactly two different members.")
            if (user.uid != fromUserId && user.uid != toUserId) {
                return ChatRoomResult.Error("You can create chats only for your approved requests.")
            }
            if (requestSnapshot.getString("status") != "approved") {
                return ChatRoomResult.Error("Chat request must be approved before creating a room.")
            }

            val existing = findExistingChatRoomBetweenUsers(fromUserId, toUserId)
            if (existing != null) {
                requestRef.set(
                    mapOf(
                        "createdChatId" to existing.chatId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
                return ChatRoomResult.Success(existing)
            }

            val chatId = chatIdFor(fromUserId, toUserId)
            val summaries = participantIds.associateWith { userId ->
                publicProfileRepository.getPublicProfile(userId).toSummaryMap(userId)
            }
            // TODO: Move chat creation to backend/Cloud Function before production.
            val roomData = mapOf(
                "chatId" to chatId,
                "participantIds" to participantIds,
                "participantPublicSummaries" to summaries,
                "createdFromChatRequestId" to chatRequestId,
                "createdFromInterestRequestId" to requestSnapshot.getString("relatedInterestRequestId").orEmpty(),
                "status" to "active",
                "guardianApprovalStatus" to "not_required",
                "lastMessagePreview" to null,
                "lastMessageAt" to null,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("chats").document(chatId).set(roomData, SetOptions.merge()).await()
            requestRef.set(
                mapOf(
                    "createdChatId" to chatId,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            val createdRoom = firestore.collection("chats").document(chatId).get().await().toChatRoom()
            ChatRoomResult.Success(createdRoom)
        } catch (e: Exception) {
            ChatRoomResult.Error(e.localizedMessage ?: "Could not create chat room.")
        }
    }

    suspend fun getUserChatRooms(userId: String): List<ChatRoom> {
        if (!canReadForUser(userId)) return emptyList()
        return firestore.collection("chats")
            .whereArrayContains("participantIds", userId)
            .get()
            .await()
            .documents
            .map { it.toChatRoom() }
            .filter { it.chatId.isNotBlank() && userId in it.participantIds && it.participantIds.toSet().size == 2 }
            .sortedByDescending { it.lastMessageAt ?: it.updatedAt ?: it.createdAt }
    }

    suspend fun getChatRoom(chatId: String): ChatRoom? {
        if (chatId.isBlank()) return null
        val snapshot = firestore.collection("chats").document(chatId).get().await()
        val room = if (snapshot.exists()) snapshot.toChatRoom() else null
        val userId = auth.currentUser?.uid
        return if (room != null && userId in room.participantIds && auth.currentUser?.isEmailVerified == true) {
            room
        } else {
            null
        }
    }

    suspend fun isUserParticipant(chatId: String, userId: String): Boolean {
        if (chatId.isBlank() || userId.isBlank()) return false
        val room = getChatRoom(chatId) ?: return false
        return userId in room.participantIds
    }

    suspend fun findExistingChatRoomBetweenUsers(userA: String, userB: String): ChatRoom? {
        val expectedParticipants = normalizedParticipantIds(userA, userB) ?: return null
        val expectedSet = expectedParticipants.toSet()
        val chatId = chatIdFor(userA, userB)
        val deterministic = firestore.collection("chats").document(chatId).get().await()
        if (deterministic.exists()) {
            return deterministic.toChatRoom().takeIf { it.participantIds.toSet() == expectedSet }
        }

        val legacyMatch = firestore.collection("chats")
            .whereArrayContains("participantIds", userA)
            .get()
            .await()
            .documents
            .map { it.toChatRoom() }
            .firstOrNull { it.participantIds.toSet() == expectedSet }
        return legacyMatch
    }

    private fun canReadForUser(userId: String): Boolean {
        val user = auth.currentUser
        return user?.uid == userId && user.isEmailVerified
    }

    private fun chatIdFor(userA: String, userB: String): String {
        return normalizedParticipantIds(userA, userB)?.joinToString("_").orEmpty()
    }

    private fun normalizedParticipantIds(userA: String, userB: String): List<String>? {
        val participants = listOf(userA.trim(), userB.trim())
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        return participants.takeIf { it.size == 2 }
    }

    private fun PublicProfile?.toSummaryMap(userId: String): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "displayName" to (this?.displayName ?: ""),
            "age" to this?.age,
            "city" to (this?.city ?: ""),
            "country" to (this?.country ?: ""),
            "isEmailVerified" to (this?.isEmailVerified == true),
            "isIdentityVerified" to (this?.isIdentityVerified == true),
            "hasGuardian" to (this?.hasGuardian == true),
            "photoPrivacyMode" to (this?.photoPrivacyMode ?: "blurred_by_default")
        )
    }

    private fun DocumentSnapshot.toChatRoom(): ChatRoom {
        val summaryMap = get("participantPublicSummaries") as? Map<*, *> ?: emptyMap<Any, Any>()
        return ChatRoom(
            chatId = getString("chatId") ?: id,
            participantIds = (get("participantIds") as? List<*>)
                ?.mapNotNull { it as? String }
                ?: emptyList(),
            participantPublicSummaries = summaryMap.mapNotNull { (key, value) ->
                val userId = key as? String ?: return@mapNotNull null
                val fields = value as? Map<*, *> ?: return@mapNotNull null
                userId to fields.toChatParticipantSummary(userId)
            }.toMap(),
            createdFromChatRequestId = getString("createdFromChatRequestId").orEmpty(),
            createdFromInterestRequestId = getString("createdFromInterestRequestId").orEmpty(),
            status = getString("status") ?: "active",
            guardianApprovalStatus = getString("guardianApprovalStatus") ?: "not_required",
            lastMessagePreview = getString("lastMessagePreview"),
            lastMessageAt = getTimestamp("lastMessageAt")?.toDate(),
            createdAt = getTimestamp("createdAt")?.toDate(),
            updatedAt = getTimestamp("updatedAt")?.toDate()
        )
    }

    private fun Map<*, *>.toChatParticipantSummary(userId: String): ChatParticipantSummary {
        return ChatParticipantSummary(
            userId = (this["userId"] as? String).orEmpty().ifBlank { userId },
            displayName = (this["displayName"] as? String).orEmpty(),
            age = when (val rawAge = this["age"]) {
                is Long -> rawAge.toInt()
                is Int -> rawAge
                is Double -> rawAge.toInt()
                else -> null
            },
            city = (this["city"] as? String).orEmpty(),
            country = (this["country"] as? String).orEmpty(),
            isEmailVerified = this["isEmailVerified"] == true,
            isIdentityVerified = this["isIdentityVerified"] == true,
            hasGuardian = this["hasGuardian"] == true,
            photoPrivacyMode = (this["photoPrivacyMode"] as? String) ?: "blurred_by_default"
        )
    }
}

sealed interface ChatRoomResult {
    data class Success(val chatRoom: ChatRoom) : ChatRoomResult
    data class Error(val message: String) : ChatRoomResult
}
