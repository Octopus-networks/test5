package com.mithaq.app.ui.messages

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.mithaq.app.data.repository.ChatRepository
import com.mithaq.app.domain.model.ChatRoom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ChatRoomsUiState(
    val isLoading: Boolean = false,
    val chatRooms: List<ChatRoom> = emptyList(),
    val errorMessage: String? = null,
    val selectedChatId: String? = null
)

class ChatRoomsViewModel(
    private val repository: ChatRepository = ChatRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(ChatRoomsUiState())
    val state: StateFlow<ChatRoomsUiState> = _state.asStateFlow()
    private var roomsRegistration: ListenerRegistration? = null
    private var listeningUserId: String? = null

    fun loadChatRooms(userId: String) {
        if (userId.isBlank()) return
        listenToChatRooms(userId)
    }

    fun listenToChatRooms(userId: String) {
        if (userId.isBlank() || listeningUserId == userId && roomsRegistration != null) return
        stopListening()
        listeningUserId = userId
        _state.value = _state.value.copy(isLoading = true, errorMessage = null)
        roomsRegistration = repository.listenToUserChatRooms(
            userId = userId,
            onRooms = { rooms ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    chatRooms = rooms.distinctBy { it.chatId },
                    errorMessage = null
                )
            },
            onError = { message ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = message
                )
            }
        )
    }

    fun openChat(chatId: String) {
        _state.value = _state.value.copy(selectedChatId = chatId)
    }

    fun closeChat() {
        _state.value = _state.value.copy(selectedChatId = null)
    }

    fun stopListening() {
        roomsRegistration?.remove()
        roomsRegistration = null
        listeningUserId = null
    }

    override fun onCleared() {
        stopListening()
        super.onCleared()
    }
}
