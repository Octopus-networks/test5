package com.mithaq.app.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun loadMessages(chatId: String) {
        if (chatId.isBlank()) return
        _state.value = _state.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    isLoading = false,
                    messages = repository.getMessages(chatId)
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Could not load messages."
                )
            }
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
                    loadMessages(chatId)
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
}
