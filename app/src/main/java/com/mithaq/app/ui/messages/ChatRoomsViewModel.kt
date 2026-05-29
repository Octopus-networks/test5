package com.mithaq.app.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mithaq.app.data.repository.ChatRepository
import com.mithaq.app.domain.model.ChatRoom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatRoomsUiState(
    val isLoading: Boolean = false,
    val chatRooms: List<ChatRoom> = emptyList(),
    val errorMessage: String? = null,
    val selectedPlaceholderChatId: String? = null
)

class ChatRoomsViewModel(
    private val repository: ChatRepository = ChatRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(ChatRoomsUiState())
    val state: StateFlow<ChatRoomsUiState> = _state.asStateFlow()

    fun loadChatRooms(userId: String) {
        if (userId.isBlank()) return
        _state.value = _state.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    isLoading = false,
                    chatRooms = repository.getUserChatRooms(userId)
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Could not load chats."
                )
            }
        }
    }

    fun showPlaceholder(chatId: String) {
        _state.value = _state.value.copy(selectedPlaceholderChatId = chatId)
    }

    fun clearPlaceholder() {
        _state.value = _state.value.copy(selectedPlaceholderChatId = null)
    }
}
