package com.mithaq.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mithaq.app.model.ChatRoom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel managing a chaperoned conversation.
 * Coordinates message delivery and securely duplicates logs for Wali transparency if enabled.
 */
class ChaperonedChatViewModel(
    private val roomId: String,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _chatRoom = MutableStateFlow<ChatRoom?>(null)
    val chatRoom: StateFlow<ChatRoom?> = _chatRoom.asStateFlow()

    init {
        fetchChatRoomDetails()
    }

    /**
     * Fetches metadata of the chat room (chaperone status, wali details).
     */
    private fun fetchChatRoomDetails() {
        viewModelScope.launch {
            try {
                val doc = firestore.collection("chatRooms")
                    .document(roomId)
                    .get()
                    .await()

                if (doc.exists()) {
                    val memberIds = doc.get("memberIds") as? List<String> ?: emptyList()
                    val isChaperoned = doc.getBoolean("isChaperoned") ?: false
                    val waliEmail = doc.getString("waliEmail")
                    val lastMessage = doc.getString("lastMessage")
                    val lastMessageTimestamp = doc.getLong("lastMessageTimestamp") ?: 0L

                    _chatRoom.value = ChatRoom(
                        roomId = roomId,
                        memberIds = memberIds,
                        isChaperoned = isChaperoned,
                        waliEmail = waliEmail,
                        lastMessage = lastMessage,
                        lastMessageTimestamp = lastMessageTimestamp
                    )
                }
            } catch (e: Exception) {
                // Fail silently in demo, log in production
            }
        }
    }

    /**
     * Sends a chat message. If chaperonage is enabled, mirrors logs to a dedicated
     * waliLogs subcollection for the guardian's review.
     */
    fun sendChatMessage(messageText: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (messageText.trim().isEmpty()) return

        viewModelScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                val messagePayload = mapOf(
                    "senderId" to currentUserId,
                    "content" to messageText.trim(),
                    "timestamp" to timestamp
                )

                // 1. Write message to normal chat history
                val msgRef = firestore.collection("chatRooms")
                    .document(roomId)
                    .collection("messages")
                    .document()
                
                msgRef.set(messagePayload).await()

                // 2. Update last message info on the room document
                firestore.collection("chatRooms")
                    .document(roomId)
                    .update(
                        mapOf(
                            "lastMessage" to messageText.trim(),
                            "lastMessageTimestamp" to timestamp
                        )
                    ).await()

                // 3. Wali Mirroring: Duplicate message to waliLogs subcollection if chaperoned
                val currentRoom = _chatRoom.value
                if (currentRoom != null && currentRoom.isChaperoned && !currentRoom.waliEmail.isNullOrBlank()) {
                    firestore.collection("chatRooms")
                        .document(roomId)
                        .collection("waliLogs")
                        .document(msgRef.id) // Use same document ID to ensure unique mapping
                        .set(
                            mapOf(
                                "senderId" to currentUserId,
                                "content" to messageText.trim(),
                                "timestamp" to timestamp,
                                "reviewedByWali" to false
                            )
                        ).await()
                }

            } catch (e: Exception) {
                // Handle delivery errors
            }
        }
    }
}
