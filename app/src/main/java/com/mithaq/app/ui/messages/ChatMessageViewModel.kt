package com.mithaq.app.ui.messages

import android.net.Uri
import java.io.File
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
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

    fun loadMessages(chatId: String) {
        if (chatId.isBlank()) return
        listenToMessages(chatId)
    }

    fun listenToMessages(chatId: String) {
        if (chatId.isBlank() || listeningChatId == chatId && messagesRegistration != null) return
        stopListening()
        listeningChatId = chatId
        _state.value = _state.value.copy(isLoading = true, errorMessage = null)
        messagesRegistration = repository.listenToMessages(
            chatId = chatId,
            onMessages = { messages ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    messages = messages.distinctBy { it.messageId },
                    errorMessage = null
                )
            },
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
    }

    override fun onCleared() {
        stopListening()
        super.onCleared()
    }
}
