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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

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
    val messageRes: Int? = null,
    val messageResAr: Int? = null,
    val errorMessageRes: Int? = null,
    val errorMessageResAr: Int? = null,
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
        _state.value = _state.value.copy(isLoadingRequests = true, errorMessage = null, errorMessageRes = null, errorMessageResAr = null)
        viewModelScope.launch {
            try {
                val sent = repository.getSentChatRequests(userId)
                val received = repository.getReceivedChatRequests(userId)
                val profileIds = (sent.map { it.toUserId } + received.map { it.fromUserId })
                    .filter { it.isNotBlank() }
                    .distinct()
                val profiles = coroutineScope {
                    profileIds.map { id ->
                        async {
                            try {
                                publicProfileRepository.getPublicProfile(id)?.let { id to it }
                            } catch (e: Exception) { null }
                        }
                    }.awaitAll().filterNotNull().toMap()
                }
                _state.value = _state.value.copy(
                    isLoadingRequests = false,
                    sentStatusByUserId = sent.associate { it.toUserId to it.status },
                    sentRequests = sent,
                    receivedPendingRequests = received
                        .filter { it.status == "pending" }
                        .sortedWith(
                            compareByDescending<ChatRequest> { it.fromUserIsPremium }
                                .thenByDescending { it.updatedAt ?: it.createdAt }
                        ),
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
            messageRes = null,
            messageResAr = null,
            errorMessageRes = null,
            errorMessageResAr = null,
            errorMessage = null
        )
        viewModelScope.launch {
            when (val result = repository.requestChat(fromUserId, toUserId)) {
                is ChatRequestResult.Success -> {
                    AppAnalytics.chatRequestSent()
                    _state.value = _state.value.copy(
                        requestingToUserIds = _state.value.requestingToUserIds - toUserId,
                        sentStatusByUserId = _state.value.sentStatusByUserId + (toUserId to "pending"),
                        messageRes = com.mithaq.app.R.string.msg_chat_sent,
                        messageResAr = com.mithaq.app.R.string.msg_chat_sent_ar,
                        errorMessageRes = null,
                        errorMessageResAr = null,
                        errorMessage = null
                    )
                    loadForUser(fromUserId)
                }
                ChatRequestResult.AlreadyPending -> {
                    _state.value = _state.value.copy(
                        requestingToUserIds = _state.value.requestingToUserIds - toUserId,
                        sentStatusByUserId = _state.value.sentStatusByUserId + (toUserId to "pending"),
                        messageRes = com.mithaq.app.R.string.msg_chat_pending,
                        messageResAr = com.mithaq.app.R.string.msg_chat_pending_ar,
                        errorMessageRes = null,
                        errorMessageResAr = null,
                        errorMessage = null
                    )
                }
                ChatRequestResult.LimitReached -> {
                    _state.value = _state.value.copy(
                        requestingToUserIds = _state.value.requestingToUserIds - toUserId,
                        errorMessageRes = com.mithaq.app.R.string.msg_chat_limit,
                        errorMessageResAr = com.mithaq.app.R.string.msg_chat_limit_ar
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
            messageRes = null,
            messageResAr = null,
            errorMessageRes = null,
            errorMessageResAr = null,
            errorMessage = null
        )
        viewModelScope.launch {
            when (val result = repository.cancelChatRequest(requestId)) {
                is ChatRequestResult.Success -> {
                    _state.value = _state.value.copy(
                        cancellingRequestIds = _state.value.cancellingRequestIds - requestId,
                        messageRes = com.mithaq.app.R.string.msg_chat_cancelled,
                        messageResAr = com.mithaq.app.R.string.msg_chat_cancelled_ar,
                        errorMessageRes = null,
                        errorMessageResAr = null,
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
            messageRes = null,
            messageResAr = null,
            errorMessageRes = null,
            errorMessageResAr = null,
            errorMessage = null
        )
        viewModelScope.launch {
            when (val result = repository.respondToChatRequest(requestId, approved)) {
                is ChatRequestResult.Success -> {
                    _state.value = _state.value.copy(
                        respondingRequestIds = _state.value.respondingRequestIds - requestId,
                        messageRes = if (approved) com.mithaq.app.R.string.msg_chat_approved else com.mithaq.app.R.string.msg_chat_declined,
                        messageResAr = if (approved) com.mithaq.app.R.string.msg_chat_approved_ar else com.mithaq.app.R.string.msg_chat_declined_ar,
                        errorMessageRes = null,
                        errorMessageResAr = null,
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

    fun clearMessages() {
        _state.value = _state.value.copy(messageRes = null, messageResAr = null, errorMessageRes = null, errorMessageResAr = null, errorMessage = null)
    }
}
