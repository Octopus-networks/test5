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

data class ChatMessage(
    val senderId: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val translatedContent: String? = null
)

/**
 * ViewModel managing a chaperoned conversation.
 * Coordinates message delivery and securely duplicates logs for Wali transparency if enabled.
 */
class ChaperonedChatViewModel(
    private val roomId: String,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val context: android.content.Context? = null
) : ViewModel() {

    private val _chatRoom = MutableStateFlow<ChatRoom?>(null)
    val chatRoom: StateFlow<ChatRoom?> = _chatRoom.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private var messagesListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        fetchChatRoomDetails()
        listenToMessages()
    }

    private fun listenToMessages() {
        val isMock = try {
            firestore.app?.options?.apiKey == "mock-api-key-for-testing" || firestore.app?.options?.apiKey?.contains("mock") == true
        } catch (e: Exception) {
            true
        }
        if (isMock) {
            val list = mutableListOf<ChatMessage>()
            val prefs = context?.getSharedPreferences("mithaq_mock_chat", android.content.Context.MODE_PRIVATE)
            val jsonStr = prefs?.getString("messages_$roomId", null)
            if (jsonStr != null) {
                try {
                    val array = org.json.JSONArray(jsonStr)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val senderId = obj.getString("senderId")
                        val content = obj.getString("content")
                        val timestamp = obj.getLong("timestamp")
                        val translatedContent = if (obj.has("translatedContent") && !obj.isNull("translatedContent")) obj.getString("translatedContent") else null
                        list.add(ChatMessage(senderId, content, timestamp, translatedContent))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (list.isEmpty()) {
                list.addAll(listOf(
                    ChatMessage("system", "Wali monitoring active. This chat is chaperoned.", 1716200000000L),
                    ChatMessage("mock_other_user", "Assalamu Alaikum, I would like to inquire about your requirements.", 1716200010000L, "السلام عليكم، أود الاستفسار عن شروطك للموافقة."),
                    ChatMessage("mock_user", "Wa Alaikum Assalam, my guardian is aware. Here are my conditions.", 1716200020000L, "وعليكم السلام، ولي أمري على علم بكل التفاصيل. إليك شروطي.")
                ))
                saveMessagesMock(list)
            }
            _messages.value = list
            return
        }

        messagesListenerRegistration?.remove()
        messagesListenerRegistration = firestore.collection("chatRooms")
            .document(roomId)
            .collection("messages")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val senderId = doc.getString("senderId") ?: ""
                        val content = doc.getString("content") ?: ""
                        val timestamp = doc.getLong("timestamp") ?: 0L
                        val translation = doc.getString("translation")
                        ChatMessage(senderId, content, timestamp, translation)
                    }
                    _messages.value = list
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        messagesListenerRegistration?.remove()
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
        val currentUserId = auth.currentUser?.uid ?: "mock_user"
        if (messageText.trim().isEmpty()) return

        val isMock = try {
            firestore.app?.options?.apiKey == "mock-api-key-for-testing" || firestore.app?.options?.apiKey?.contains("mock") == true
        } catch (e: Exception) {
            true
        }
        if (isMock) {
            viewModelScope.launch {
                val list = _messages.value.toMutableList()
                val translationHelper = MockTranslationHelper()
                val targetLang = if (messageText.any { it in '\u0600'..'\u06FF' }) "en" else "ar"
                val translation = try { translationHelper.translateText(messageText, targetLang) } catch(e: Exception) { null }
                val newMsg = ChatMessage(currentUserId, messageText.trim(), System.currentTimeMillis(), translation)
                list.add(newMsg)
                _messages.value = list
                saveMessagesMock(list)
            }
            return
        }

        viewModelScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                
                // Try translation
                val translationHelper = MockTranslationHelper()
                val targetLang = if (messageText.any { it in '\u0600'..'\u06FF' }) "en" else "ar"
                val translation = try {
                    translationHelper.translateText(messageText, targetLang)
                } catch (e: Exception) {
                    null
                }

                val messagePayload = mutableMapOf<String, Any>(
                    "senderId" to currentUserId,
                    "content" to messageText.trim(),
                    "timestamp" to timestamp
                )
                if (translation != null) {
                    messagePayload["translation"] = translation
                }

                // Make sure room document exists first (upsert)
                val roomRef = firestore.collection("chatRooms").document(roomId)
                val roomSnap = roomRef.get().await()
                if (!roomSnap.exists()) {
                    roomRef.set(
                        mapOf(
                            "roomId" to roomId,
                            "isChaperoned" to false,
                            "memberIds" to roomId.split("_")
                        )
                    ).await()
                }

                // 1. Write message to normal chat history
                val msgRef = roomRef.collection("messages").document()
                msgRef.set(messagePayload).await()

                // 2. Update last message info on the room document
                roomRef.update(
                    mapOf(
                        "lastMessage" to messageText.trim(),
                        "lastMessageTimestamp" to timestamp
                    )
                ).await()

                // 3. Wali Mirroring: Duplicate message to waliLogs subcollection if chaperoned
                val currentRoom = _chatRoom.value
                if (currentRoom != null && currentRoom.isChaperoned && !currentRoom.waliEmail.isNullOrBlank()) {
                    val waliPayload = mutableMapOf<String, Any>(
                        "senderId" to currentUserId,
                        "content" to messageText.trim(),
                        "timestamp" to timestamp,
                        "reviewedByWali" to false
                    )
                    if (translation != null) {
                        waliPayload["translation"] = translation
                    }
                    roomRef.collection("waliLogs")
                        .document(msgRef.id) // Use same document ID to ensure unique mapping
                        .set(waliPayload)
                        .await()
                }

            } catch (e: Exception) {
                // Handle delivery errors
            }
        }
    }

    private fun saveMessagesMock(list: List<ChatMessage>) {
        val context = context ?: return
        val array = org.json.JSONArray()
        for (msg in list) {
            val obj = org.json.JSONObject()
            obj.put("senderId", msg.senderId)
            obj.put("content", msg.content)
            obj.put("timestamp", msg.timestamp)
            if (msg.translatedContent != null) {
                obj.put("translatedContent", msg.translatedContent)
            }
            array.put(obj)
        }
        context.getSharedPreferences("mithaq_mock_chat", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("messages_$roomId", array.toString())
            .apply()
        
        updateMockRoomMetadata(list.lastOrNull())
    }

    private fun updateMockRoomMetadata(lastMsg: ChatMessage?) {
        val context = context ?: return
        val lastMsgText = lastMsg?.content ?: ""
        val lastMsgTime = lastMsg?.timestamp ?: 0L
        
        val prefs = context.getSharedPreferences("mithaq_mock_chat", android.content.Context.MODE_PRIVATE)
        val roomsJsonStr = prefs.getString("mithaq_mock_rooms", null)
        val array = org.json.JSONArray()
        var updated = false
        
        if (roomsJsonStr != null) {
            try {
                val oldArray = org.json.JSONArray(roomsJsonStr)
                for (i in 0 until oldArray.length()) {
                    val obj = oldArray.getJSONObject(i)
                    val rId = obj.getString("roomId")
                    if (rId == roomId) {
                        obj.put("lastMessage", lastMsgText)
                        obj.put("lastMessageTimestamp", lastMsgTime)
                        updated = true
                    }
                    array.put(obj)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        if (!updated) {
            val obj = org.json.JSONObject()
            obj.put("roomId", roomId)
            val memberIdsArr = org.json.JSONArray()
            val ids = roomId.split("_")
            for (id in ids) {
                memberIdsArr.put(id)
            }
            obj.put("memberIds", memberIdsArr)
            obj.put("isChaperoned", true)
            obj.put("waliEmail", "guardian@mithaq.com")
            obj.put("lastMessage", lastMsgText)
            obj.put("lastMessageTimestamp", lastMsgTime)
            array.put(obj)
        }
        
        prefs.edit().putString("mithaq_mock_rooms", array.toString()).apply()
    }
}
