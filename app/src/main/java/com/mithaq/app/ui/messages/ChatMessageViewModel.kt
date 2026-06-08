package com.mithaq.app.ui.messages

import android.net.Uri
import java.io.File
import java.util.Date
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.mithaq.app.analytics.AppAnalytics
import com.mithaq.app.data.repository.ChatMessageRepository
import com.mithaq.app.data.repository.ChatMessageResult
import com.mithaq.app.domain.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessageUiState(
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val isLoadingOlder: Boolean = false,
    val hasMoreOlder: Boolean = true,
    val messages: List<ChatMessage> = emptyList(),
    val errorMessage: String? = null
)

class ChatMessageViewModel(
    private val repository: ChatMessageRepository = ChatMessageRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(ChatMessageUiState())
    val state: StateFlow<ChatMessageUiState> = _state.asStateFlow()
    private var messagesRegistration: ListenerRegistration? = null
    private var listeningChatId: String? = null

    // All messages currently loaded for the open chat: the realtime newest window plus any older
    // pages fetched on scroll-up, keyed by id. Mutated only on the main thread (listener callbacks
    // and viewModelScope default dispatcher), so no extra synchronization is needed.
    private val loadedMessages = LinkedHashMap<String, ChatMessage>()
    private var initialWindowApplied = false

    fun loadMessages(chatId: String) {
        if (chatId.isBlank()) return
        listenToMessages(chatId)
    }

    fun listenToMessages(chatId: String) {
        if (chatId.isBlank() || listeningChatId == chatId && messagesRegistration != null) return
        AppAnalytics.chatOpened()
        stopListening()
        listeningChatId = chatId
        loadedMessages.clear()
        initialWindowApplied = false
        _state.value = _state.value.copy(
            isLoading = true,
            isLoadingOlder = false,
            hasMoreOlder = true,
            messages = emptyList(),
            errorMessage = null
        )
        messagesRegistration = repository.listenToMessages(
            chatId = chatId,
            onMessages = { window -> onLiveWindow(window) },
            onError = { message ->
                messagesRegistration?.remove()
                messagesRegistration = null
                listeningChatId = null
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = message
                )
            }
        )
    }

    /**
     * Merges a realtime window emission. Within the window's time range the listener is
     * authoritative, so a previously-loaded message in that range that is no longer present
     * (e.g. deleted) is dropped; older paged-in messages are retained.
     */
    private fun onLiveWindow(window: List<ChatMessage>) {
        val windowIds = window.mapTo(HashSet()) { it.messageId }
        val windowOldest = window.firstOrNull()?.createdAt
        if (windowOldest != null) {
            val iterator = loadedMessages.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val createdAt = entry.value.createdAt
                if (createdAt != null && createdAt >= windowOldest && entry.key !in windowIds) {
                    iterator.remove()
                }
            }
        }
        window.forEach { loadedMessages[it.messageId] = it }

        if (!initialWindowApplied) {
            initialWindowApplied = true
            // A first window smaller than a full page means there is no older history to fetch.
            _state.value = _state.value.copy(hasMoreOlder = window.size >= ChatMessageRepository.PAGE_SIZE)
        }
        emitMessages()
    }

    private fun emitMessages() {
        _state.value = _state.value.copy(
            isLoading = false,
            messages = orderedMessages(),
            errorMessage = null
        )
    }

    private fun orderedMessages(): List<ChatMessage> =
        loadedMessages.values.sortedWith(compareBy(nullsLast<Date>()) { it.createdAt })

    /** Fetches the next page of older messages and prepends them. Triggered when the user scrolls up. */
    fun loadOlderMessages(chatId: String) {
        val current = _state.value
        if (current.isLoadingOlder || !current.hasMoreOlder) return
        val oldest = current.messages.firstOrNull() ?: return
        _state.value = current.copy(isLoadingOlder = true)
        viewModelScope.launch {
            val page = repository.loadOlderMessages(chatId, oldest.messageId)
            page.messages.forEach { loadedMessages.putIfAbsent(it.messageId, it) }
            _state.value = _state.value.copy(
                isLoadingOlder = false,
                hasMoreOlder = page.hasMore,
                messages = orderedMessages()
            )
        }
    }

    fun sendTextMessage(chatId: String, senderId: String, text: String) {
        if (_state.value.isSending) return
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        _state.value = _state.value.copy(isSending = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = repository.sendTextMessage(chatId, senderId, trimmed)) {
                is ChatMessageResult.Success -> {
                    _state.value = _state.value.copy(isSending = false)
                }
                is ChatMessageResult.Error -> {
                    _state.value = _state.value.copy(
                        isSending = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun sendImageMessage(chatId: String, senderId: String, imageUri: Uri, mimeType: String?) {
        if (_state.value.isSending) return
        _state.value = _state.value.copy(isSending = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.sendImageMessage(chatId, senderId, imageUri, mimeType)
            _state.value = when (result) {
                is ChatMessageResult.Success -> _state.value.copy(isSending = false)
                is ChatMessageResult.Error -> _state.value.copy(isSending = false, errorMessage = result.message)
            }
        }
    }

    fun sendVoiceMessage(chatId: String, senderId: String, audioFile: File, durationMs: Long) {
        if (_state.value.isSending) return
        _state.value = _state.value.copy(isSending = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.sendVoiceMessage(chatId, senderId, audioFile, durationMs)
            _state.value = when (result) {
                is ChatMessageResult.Success -> _state.value.copy(isSending = false)
                is ChatMessageResult.Error -> _state.value.copy(isSending = false, errorMessage = result.message)
            }
        }
    }

    fun setReaction(chatId: String, messageId: String, emoji: String?) {
        if (chatId.isBlank() || messageId.isBlank()) return
        viewModelScope.launch {
            val result = repository.setReaction(chatId, messageId, emoji)
            if (result is ChatMessageResult.Error) {
                _state.value = _state.value.copy(errorMessage = result.message)
            }
        }
    }

    fun stopListening() {
        messagesRegistration?.remove()
        messagesRegistration = null
        listeningChatId = null
        loadedMessages.clear()
        initialWindowApplied = false
    }

    override fun onCleared() {
        stopListening()
        super.onCleared()
    }
}
