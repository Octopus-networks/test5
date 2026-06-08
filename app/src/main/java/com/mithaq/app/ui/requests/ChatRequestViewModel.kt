package com.mithaq.app.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mithaq.app.analytics.AppAnalytics
import com.mithaq.app.data.repository.ChatRequestRepository
import com.mithaq.app.data.repository.ChatRequestResult
import com.mithaq.app.data.repository.PublicProfileRepository
import com.mithaq.app.domain.model.ChatRequest
import com.mithaq.app.domain.model.PublicProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatRequestUiState(
    val isLoadingRequests: Boolean = false,
    val requestingToUserIds: Set<String> = emptySet(),
    val respondingRequestIds: Set<String> = emptySet(),
    val cancellingRequestIds: Set<String> = emptySet(),
    val sentStatusByUserId: Map<String, String> = emptyMap(),
    val sentRequests: List<ChatRequest> = emptyList(),
    val receivedPendingRequests: List<ChatRequest> = emptyList(),
    val receivedHistoryRequests: List<ChatRequest> = emptyList(),
    val publicProfilesByUserId: Map<String, PublicProfile> = emptyMap(),
    val message: String? = null,
    val errorMessage: String? = null
)

class ChatRequestViewModel(
    private val repository: ChatRequestRepository = ChatRequestRepository(),
    private val publicProfileRepository: PublicProfileRepository = PublicProfileRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(ChatRequestUiState())
    val state: StateFlow<ChatRequestUiState> = _state.asStateFlow()

    fun loadForUser(userId: String) {
        if (userId.isBlank()) return
        _state.value = _state.value.copy(isLoadingRequests = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val sent = repository.getSentChatRequests(userId)
                val received = repository.getReceivedChatRequests(userId)
                val profileIds = (sent.map { it.toUserId } + received.map { it.fromUserId })
                    .filter { it.isNotBlank() }
                    .distinct()
                val profiles = profileIds.mapNotNull { id ->
                    publicProfileRepository.getPublicProfile(id)?.let { id to it }
                }.toMap()
                _state.value = _state.value.copy(
                    isLoadingRequests = false,
                    sentStatusByUserId = sent.associate { it.toUserId to it.status },
                    sentRequests = sent,
                    receivedPendingRequests = received.filter { it.status == "pending" },
                    receivedHistoryRequests = received.filter { it.status != "pending" },
                    publicProfilesByUserId = profiles
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoadingRequests = false,
                    errorMessage = e.localizedMessage ?: "Could not load chat requests."
                )
            }
        }
    }

    fun requestChat(fromUserId: String, toUserId: String) {
        if (fromUserId.isBlank() || toUserId.isBlank() || toUserId in _state.value.requestingToUserIds) return
        _state.value = _state.value.copy(
            requestingToUserIds = _state.value.requestingToUserIds + toUserId,
            message = null,
            errorMessage = null
        )
        viewModelScope.launch {
            when (val result = repository.requestChat(fromUserId, toUserId)) {
                is ChatRequestResult.Success -> {
                    AppAnalytics.chatRequestSent()
                    _state.value = _state.value.copy(
                        requestingToUserIds = _state.value.requestingToUserIds - toUserId,
                        sentStatusByUserId = _state.value.sentStatusByUserId + (toUserId to "pending"),
                        message = "Chat request sent.",
                        errorMessage = null
                    )
                    loadForUser(fromUserId)
                }
                ChatRequestResult.AlreadyPending -> {
                    _state.value = _state.value.copy(
                        requestingToUserIds = _state.value.requestingToUserIds - toUserId,
                        sentStatusByUserId = _state.value.sentStatusByUserId + (toUserId to "pending"),
                        message = "Chat request is already pending.",
                        errorMessage = null
                    )
                }
                ChatRequestResult.LimitReached -> {
                    _state.value = _state.value.copy(
                        requestingToUserIds = _state.value.requestingToUserIds - toUserId,
                        errorMessage = "You've reached today's free chat limit (3). " +
                            "Upgrade to Premium for unlimited chats."
                    )
                }
                is ChatRequestResult.Error -> {
                    _state.value = _state.value.copy(
                        requestingToUserIds = _state.value.requestingToUserIds - toUserId,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun cancelChatRequest(currentUserId: String, requestId: String) {
        if (requestId.isBlank() || requestId in _state.value.cancellingRequestIds) return
        _state.value = _state.value.copy(
            cancellingRequestIds = _state.value.cancellingRequestIds + requestId,
            message = null,
            errorMessage = null
        )
        viewModelScope.launch {
            when (val result = repository.cancelChatRequest(requestId)) {
                is ChatRequestResult.Success -> {
                    _state.value = _state.value.copy(
                        cancellingRequestIds = _state.value.cancellingRequestIds - requestId,
                        message = "Chat request cancelled.",
                        errorMessage = null
                    )
                    loadForUser(currentUserId)
                }
                ChatRequestResult.AlreadyPending -> Unit
                ChatRequestResult.LimitReached -> Unit
                is ChatRequestResult.Error -> {
                    _state.value = _state.value.copy(
                        cancellingRequestIds = _state.value.cancellingRequestIds - requestId,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun respondToChatRequest(currentUserId: String, requestId: String, approved: Boolean) {
        if (requestId.isBlank() || requestId in _state.value.respondingRequestIds) return
        _state.value = _state.value.copy(
            respondingRequestIds = _state.value.respondingRequestIds + requestId,
            message = null,
            errorMessage = null
        )
        viewModelScope.launch {
            when (val result = repository.respondToChatRequest(requestId, approved)) {
                is ChatRequestResult.Success -> {
                    _state.value = _state.value.copy(
                        respondingRequestIds = _state.value.respondingRequestIds - requestId,
                        message = if (approved) "Chat request approved." else "Chat request declined.",
                        errorMessage = null
                    )
                    loadForUser(currentUserId)
                }
                ChatRequestResult.AlreadyPending -> Unit
                ChatRequestResult.LimitReached -> Unit
                is ChatRequestResult.Error -> {
                    _state.value = _state.value.copy(
                        respondingRequestIds = _state.value.respondingRequestIds - requestId,
                        errorMessage = result.message
                    )
                }
            }
        }
    }
}
