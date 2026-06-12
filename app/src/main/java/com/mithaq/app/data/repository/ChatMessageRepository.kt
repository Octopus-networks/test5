package com.mithaq.app.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.mithaq.app.domain.model.ChatMessage
import com.mithaq.app.util.prepareForUpload
import kotlinx.coroutines.tasks.await
import java.io.File

class ChatMessageRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val chatRepository: ChatRepository = ChatRepository(firestore, auth),
    private val blockRepository: BlockRepository = BlockRepository(firestore, auth)
) {
    suspend fun getMessages(chatId: String): List<ChatMessage> {
        val userId = auth.currentUser?.uid.orEmpty()
        if (!validateUserCanRead(chatId, userId)) return emptyList()
        // Bounded to the most recent page; chats can grow unbounded otherwise.
        return firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE.toLong())
            .get()
            .await()
            .documents
            .map { it.toChatMessage(chatId) }
            .filter { it.deletedAt == null && it.type in ATTACHMENT_VISIBLE_TYPES }
            .reversed()
    }

    /**
     * Realtime listener for the chat. Bounded to the most recent [PAGE_SIZE] messages so opening
     * a long conversation does not download (and keep live) its entire history. New messages still
     * arrive in realtime; older history is fetched on demand via [loadOlderMessages].
     * Emits in ascending (oldest-first) order to match the message list UI.
     */
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
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Could not listen to messages.")
                    return@addSnapshotListener
                }
                val messages = snapshot
                    ?.documents
                    .orEmpty()
                    .map { it.toChatMessage(chatId) }
                    .filter { it.deletedAt == null && it.type in ATTACHMENT_VISIBLE_TYPES }
                    .distinctBy { it.messageId }
                    .reversed()
                onMessages(messages)
            }
    }

    /**
     * Fetches one page of messages OLDER than [oldestLoadedMessageId] for scroll-up pagination.
     * Returns the batch in ascending (oldest-first) order plus whether more history may remain.
     */
    suspend fun loadOlderMessages(chatId: String, oldestLoadedMessageId: String): MessagePage {
        val user = auth.currentUser
        if (chatId.isBlank() || oldestLoadedMessageId.isBlank() || user?.isEmailVerified != true) {
            return MessagePage(emptyList(), hasMore = false)
        }
        return try {
            val messagesRef = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
            val cursor = messagesRef.document(oldestLoadedMessageId).get().await()
            if (!cursor.exists()) return MessagePage(emptyList(), hasMore = false)
            val snapshot = messagesRef
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .startAfter(cursor)
                .limit(PAGE_SIZE.toLong())
                .get()
                .await()
            val older = snapshot.documents
                .map { it.toChatMessage(chatId) }
                .filter { it.deletedAt == null && it.type in ATTACHMENT_VISIBLE_TYPES }
                .distinctBy { it.messageId }
                .reversed()
            // hasMore is based on the raw page size (before filtering) so a page made entirely of
            // hidden messages still lets the caller keep paging.
            MessagePage(messages = older, hasMore = snapshot.documents.size == PAGE_SIZE)
        } catch (e: Exception) {
            MessagePage(emptyList(), hasMore = false)
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

    suspend fun sendImageMessage(
        chatId: String,
        senderId: String,
        imageUri: Uri,
        mimeType: String?
    ): ChatMessageResult {
        if (!validateUserCanSend(chatId, senderId)) {
            return ChatMessageResult.Error("Messaging is unavailable for this conversation.")
        }
        return try {
            val messageRef = firestore.collection("chats").document(chatId)
                .collection("messages").document()
            val messageId = messageRef.id
            val storagePath = "chat_attachments/$chatId/$messageId"
            val contentType = "image/jpeg"
            val metadata = StorageMetadata.Builder().setContentType(contentType).build()
            val imageBytes = prepareForUpload(auth.app.applicationContext, imageUri)
            val snapshot = storage.reference.child(storagePath)
                .putBytes(imageBytes, metadata)
                .await()
            val sizeBytes = snapshot.totalByteCount

            messageRef.set(
                mapOf(
                    "messageId" to messageId,
                    "chatId" to chatId,
                    "senderId" to senderId,
                    "text" to "",
                    "type" to "image",
                    "status" to "sent",
                    "storagePath" to storagePath,
                    "mimeType" to contentType,
                    "sizeBytes" to sizeBytes,
                    "durationMs" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "editedAt" to null,
                    "deletedAt" to null
                )
            ).await()

            firestore.collection("chats").document(chatId).update(
                mapOf(
                    "lastMessagePreview" to "📷 Photo",
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            ChatMessageResult.Success(messageId)
        } catch (e: Exception) {
            ChatMessageResult.Error(e.localizedMessage ?: "Could not send image.")
        }
    }

    suspend fun sendVoiceMessage(
        chatId: String,
        senderId: String,
        audioFile: File,
        durationMs: Long
    ): ChatMessageResult {
        if (!validateUserCanSend(chatId, senderId)) {
            return ChatMessageResult.Error("Messaging is unavailable for this conversation.")
        }
        return try {
            val messageRef = firestore.collection("chats").document(chatId)
                .collection("messages").document()
            val messageId = messageRef.id
            val storagePath = "chat_attachments/$chatId/$messageId"
            val contentType = "audio/mp4"
            val metadata = StorageMetadata.Builder().setContentType(contentType).build()
            val snapshot = storage.reference.child(storagePath)
                .putFile(Uri.fromFile(audioFile), metadata).await()
            val sizeBytes = snapshot.totalByteCount

            messageRef.set(
                mapOf(
                    "messageId" to messageId,
                    "chatId" to chatId,
                    "senderId" to senderId,
                    "text" to "",
                    "type" to "voice",
                    "status" to "sent",
                    "storagePath" to storagePath,
                    "mimeType" to contentType,
                    "sizeBytes" to sizeBytes,
                    "durationMs" to durationMs,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "editedAt" to null,
                    "deletedAt" to null
                )
            ).await()

            firestore.collection("chats").document(chatId).update(
                mapOf(
                    "lastMessagePreview" to "🎤 Voice message",
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            audioFile.delete()
            ChatMessageResult.Success(messageId)
        } catch (e: Exception) {
            ChatMessageResult.Error(e.localizedMessage ?: "Could not send voice message.")
        }
    }

    /** Downloads attachment bytes for an authorised viewer. Storage rules are the boundary. */
    suspend fun loadAttachmentBytes(storagePath: String): ByteArray? {
        if (storagePath.isBlank()) return null
        return try {
            storage.reference.child(storagePath).getBytes(MAX_ATTACHMENT_BYTES).await()
        } catch (e: Exception) {
            null
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
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    suspend fun markIncomingMessagesRead(chatId: String, currentUserId: String) {
        if (chatId.isBlank() || currentUserId.isBlank()) return
        try {
            val messagesRef = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
            val snapshot = messagesRef
                .whereEqualTo("status", "sent")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()
            
            val toUpdate = snapshot.documents.filter { it.getString("senderId") != currentUserId }
            if (toUpdate.isEmpty()) return
            
            firestore.runBatch { batch ->
                for (doc in toUpdate) {
                    batch.update(doc.reference, mapOf(
                        "status" to "read",
                        "readAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ))
                }
            }.await()
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun isUserPremium(userId: String): Boolean {
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            doc.getBoolean("isPremium") ?: false
        } catch (e: Exception) {
            false
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
            storagePath = getString("storagePath").orEmpty(),
            mimeType = getString("mimeType").orEmpty(),
            sizeBytes = getLong("sizeBytes") ?: 0L,
            durationMs = getLong("durationMs") ?: 0L,
            createdAt = getTimestamp("createdAt")?.toDate(),
            updatedAt = getTimestamp("updatedAt")?.toDate(),
            editedAt = getTimestamp("editedAt")?.toDate(),
            deletedAt = getTimestamp("deletedAt")?.toDate(),
            readAt = getTimestamp("readAt")?.toDate()
        )
    }

    companion object {
        /** Page size for the realtime window and for each older-history page. */
        const val PAGE_SIZE = 30
        private val ATTACHMENT_VISIBLE_TYPES = setOf("text", "image", "voice")
        private const val MAX_ATTACHMENT_BYTES = 10L * 1024 * 1024
    }
}

/** A page of chat messages (ascending order) plus whether more history may remain. */
data class MessagePage(
    val messages: List<ChatMessage>,
    val hasMore: Boolean
)

sealed interface ChatMessageResult {
    data class Success(val messageId: String) : ChatMessageResult
    data class Error(val message: String) : ChatMessageResult
}
