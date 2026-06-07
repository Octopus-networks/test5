package com.mithaq.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.mithaq.app.domain.model.ChatMessage
import kotlinx.coroutines.tasks.await

class ChatMessageRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val chatRepository: ChatRepository = ChatRepository(firestore, auth),
    private val blockRepository: BlockRepository = BlockRepository(firestore, auth)
) {
    suspend fun getMessages(chatId: String): List<ChatMessage> {
        val userId = auth.currentUser?.uid.orEmpty()
        if (!validateUserCanRead(chatId, userId)) return emptyList()
        return firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("createdAt")
            .get()
            .await()
            .documents
            .map { it.toChatMessage(chatId) }
            .filter { it.deletedAt == null && it.type == "text" }
    }

    fun listenToMessages(
        chatId: String,
        onMessages: (List<ChatMessage>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        val user = auth.currentUser
        if (chatId.isBlank() || user?.isEmailVerified != true) {
            onError("Please verify your email before opening messages.")
            return null
        }

        return firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Could not listen to messages.")
                    return@addSnapshotListener
                }
                val messages = snapshot
                    ?.documents
                    .orEmpty()
                    .map { it.toChatMessage(chatId) }
                    .filter { it.deletedAt == null && it.type == "text" }
                    .distinctBy { it.messageId }
                onMessages(messages)
            }
    }

    suspend fun sendTextMessage(chatId: String, senderId: String, text: String): ChatMessageResult {
        val cleanedText = text.trim()
        val validationError = try {
            validateOutgoingMessage(chatId, senderId, cleanedText)
        } catch (e: Exception) {
            e.localizedMessage ?: "Could not verify this conversation before sending."
        }
        if (validationError != null) return ChatMessageResult.Error(validationError)

        return try {
            val messagesRef = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
            val messageRef = messagesRef.document()
            val messageData = mapOf(
                "messageId" to messageRef.id,
                "chatId" to chatId,
                "senderId" to senderId,
                "text" to cleanedText,
                "type" to "text",
                "status" to "sent",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "editedAt" to null,
                "deletedAt" to null
            )
            messageRef.set(messageData).await()

            firestore.collection("chats").document(chatId).update(
                mapOf(
                    "lastMessagePreview" to cleanedText.safePreview(),
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            queueMessageNotification(chatId, senderId, messageRef.id, cleanedText)

            ChatMessageResult.Success(messageRef.id)
        } catch (e: Exception) {
            ChatMessageResult.Error(e.localizedMessage ?: "Could not send message.")
        }
    }

    suspend fun markMessageSent(chatId: String, messageId: String): ChatMessageResult {
        if (chatId.isBlank() || messageId.isBlank()) {
            return ChatMessageResult.Error("Missing message id.")
        }
        return try {
            firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .update(
                    mapOf(
                        "status" to "sent",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            ChatMessageResult.Success(messageId)
        } catch (e: Exception) {
            ChatMessageResult.Error(e.localizedMessage ?: "Could not update message status.")
        }
    }

    /**
     * Sets ([emoji] non-null) or clears ([emoji] null) the current user's emoji reaction on a
     * message. Uses a field-path update so only reactions.<uid> changes; Firestore rules ensure
     * a participant can only touch their own reaction, never the text or other users' reactions.
     */
    suspend fun setReaction(chatId: String, messageId: String, emoji: String?): ChatMessageResult {
        val user = auth.currentUser
        if (user?.uid == null || !user.isEmailVerified) {
            return ChatMessageResult.Error("Please verify your email before reacting.")
        }
        if (chatId.isBlank() || messageId.isBlank()) {
            return ChatMessageResult.Error("Missing message reference.")
        }
        return try {
            val messageRef = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
            val reactionValue: Any = if (emoji.isNullOrBlank()) FieldValue.delete() else emoji
            messageRef.update(
                mapOf(
                    "reactions.${user.uid}" to reactionValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            ChatMessageResult.Success(messageId)
        } catch (e: Exception) {
            ChatMessageResult.Error(e.localizedMessage ?: "Could not update reaction.")
        }
    }

    suspend fun validateUserCanSend(chatId: String, userId: String): Boolean {
        val user = auth.currentUser
        if (user?.uid != userId || !user.isEmailVerified) return false
        val room = chatRepository.getChatRoom(chatId) ?: return false
        if (room.status != "active" || userId !in room.participantIds) return false
        val otherUserId = room.participantIds.firstOrNull { it != userId } ?: return false
        return !blockRepository.isBlockedBetweenUsers(userId, otherUserId)
    }

    private suspend fun validateUserCanRead(chatId: String, userId: String): Boolean {
        val user = auth.currentUser
        if (user?.uid != userId || !user.isEmailVerified) return false
        return chatRepository.isUserParticipant(chatId, userId)
    }

    private suspend fun validateOutgoingMessage(
        chatId: String,
        senderId: String,
        text: String
    ): String? {
        if (text.isBlank()) return "Message cannot be empty."
        if (text.length > 1000) return "Message must be 1000 characters or less."
        if (!validateUserCanSend(chatId, senderId)) {
            return "Messaging is unavailable for this conversation."
        }
        return null
    }

    private fun String.safePreview(): String {
        return if (length <= 80) this else take(77).trimEnd() + "..."
    }

    private suspend fun queueMessageNotification(
        chatId: String,
        senderId: String,
        messageId: String,
        text: String
    ) {
        try {
            val room = chatRepository.getChatRoom(chatId) ?: return
            val recipientId = room.participantIds.firstOrNull { it != senderId } ?: return
            val senderName = room.participantPublicSummaries[senderId]
                ?.displayName
                ?.ifBlank { null }
                ?: "Mithaq member"
            firestore.collection("notifications").add(
                mapOf(
                    "senderUid" to senderId,
                    "recipientUid" to recipientId,
                    "title" to "Mithaq - New message",
                    "body" to "$senderName: ${text.safePreview()}",
                    "status" to "PENDING",
                    "type" to "chat_message",
                    "chatId" to chatId,
                    "messageId" to messageId,
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()
        } catch (e: Exception) {
            // Message delivery must not fail just because notification queuing failed.
            e.printStackTrace()
        }
    }

    private fun DocumentSnapshot.toChatMessage(parentChatId: String): ChatMessage {
        return ChatMessage(
            messageId = getString("messageId") ?: id,
            chatId = getString("chatId") ?: parentChatId,
            senderId = getString("senderId").orEmpty(),
            text = getString("text").orEmpty(),
            type = getString("type") ?: "text",
            status = getString("status") ?: "sent",
            reactions = (get("reactions") as? Map<String, String>) ?: emptyMap(),
            createdAt = getTimestamp("createdAt")?.toDate(),
            updatedAt = getTimestamp("updatedAt")?.toDate(),
            editedAt = getTimestamp("editedAt")?.toDate(),
            deletedAt = getTimestamp("deletedAt")?.toDate()
        )
    }
}

sealed interface ChatMessageResult {
    data class Success(val messageId: String) : ChatMessageResult
    data class Error(val message: String) : ChatMessageResult
}
