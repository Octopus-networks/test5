package com.mithaq.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.mithaq.app.domain.model.ChatMessage
import kotlinx.coroutines.tasks.await

class ChatMessageRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val chatRepository: ChatRepository = ChatRepository(firestore, auth)
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

    suspend fun sendTextMessage(chatId: String, senderId: String, text: String): ChatMessageResult {
        val cleanedText = text.trim()
        val validationError = validateOutgoingMessage(chatId, senderId, cleanedText)
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

    suspend fun validateUserCanSend(chatId: String, userId: String): Boolean {
        val user = auth.currentUser
        if (user?.uid != userId || !user.isEmailVerified) return false
        val room = chatRepository.getChatRoom(chatId) ?: return false
        return room.status == "active" && userId in room.participantIds
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
            return "You can send messages only in active approved chats."
        }
        return null
    }

    private fun String.safePreview(): String {
        return if (length <= 80) this else take(77).trimEnd() + "..."
    }

    private fun DocumentSnapshot.toChatMessage(parentChatId: String): ChatMessage {
        return ChatMessage(
            messageId = getString("messageId") ?: id,
            chatId = getString("chatId") ?: parentChatId,
            senderId = getString("senderId").orEmpty(),
            text = getString("text").orEmpty(),
            type = getString("type") ?: "text",
            status = getString("status") ?: "sent",
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
