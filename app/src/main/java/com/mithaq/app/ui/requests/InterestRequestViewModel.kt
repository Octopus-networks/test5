package com.mithaq.app.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mithaq.app.data.repository.InterestRequestRepository
import com.mithaq.app.data.repository.InterestRequestResult
import com.mithaq.app.data.repository.PublicProfileRepository
import com.mithaq.app.domain.model.InterestRequest
import com.mithaq.app.domain.model.PublicProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InterestRequestUiState(
    val isLoadingRequests: Boolean = false,
    val sendingToUserIds: Set<String> = emptySet(),
    val respondingRequestIds: Set<String> = emptySet(),
    val cancellingRequestIds: Set<String> = emptySet(),
    val sentPendingToUserIds: Set<String> = emptySet(),
    val sentStatusByUserId: Map<String, String> = emptyMap(),
    val sentRequests: List<InterestRequest> = emptyList(),
    val receivedPendingRequests: List<InterestRequest> = emptyList(),
    val receivedHistoryRequests: List<InterestRequest> = emptyList(),
    val publicProfilesByUserId: Map<String, PublicProfile> = emptyMap(),
    val message: String? = null,
    val errorMessage: String? = null
)

class InterestRequestViewModel(
    private val repository: InterestRequestRepository = InterestRequestRepository(),
    private val publicProfileRepository: PublicProfileRepository = PublicProfileRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(InterestRequestUiState())
    val state: StateFlow<InterestRequestUiState> = _state.asStateFlow()

    fun loadForUser(userId: String) {
        if (userId.isBlank()) return
        _state.value = _state.value.copy(isLoadingRequests = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val sent = repository.getSentInterestRequests(userId)
                val received = repository.getReceivedInterestRequests(userId)
                val receivedPending = received.filter { it.status == "pending" }
                val receivedHistory = received.filter { it.status != "pending" }
                val profileIds = (sent.map { it.toUserId } + received.map { it.fromUserId })
                    .filter { it.isNotBlank() }
                    .distinct()
                val summaries = profileIds
                    .mapNotNull { request ->
                        publicProfileRepository.getPublicProfile(request)?.let { profile ->
                            request to profile
                        }
                    }
                    .toMap()
                _state.value = _state.value.copy(
                    isLoadingRequests = false,
                    sentPendingToUserIds = sent
                        .filter { it.status == "pending" }
                        .map { it.toUserId }
                        .toSet(),
                    sentStatusByUserId = sent.associate { it.toUserId to it.status },
                    sentRequests = sent,
                    receivedPendingRequests = receivedPending,
                    receivedHistoryRequests = receivedHistory,
                    publicProfilesByUserId = summaries
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoadingRequests = false,
                    errorMessage = e.localizedMessage ?: "Could not load requests."
                )
            }
        }
    }

    fun sendInterest(fromUserId: String, toUserId: String) {
        if (fromUserId.isBlank() || toUserId.isBlank() || toUserId in _state.value.sendingToUserIds) return
        _state.value = _state.value.copy(
            sendingToUserIds = _state.value.sendingToUserIds + toUserId,
            message = null,
            errorMessage = null
        )
        viewModelScope.launch {
            when (val result = repository.sendInterest(fromUserId, toUserId)) {
                is InterestRequestResult.Success -> {
                    _state.value = _state.value.copy(
                        sendingToUserIds = _state.value.sendingToUserIds - toUserId,
                        sentPendingToUserIds = _state.value.sentPendingToUserIds + toUserId,
                        sentStatusByUserId = _state.value.sentStatusByUserId + (toUserId to "pending"),
                        message = "Interest request sent.",
                        errorMessage = null
                    )
                    loadForUser(fromUserId)
                }
                InterestRequestResult.AlreadyPending -> {
                    _state.value = _state.value.copy(
                        sendingToUserIds = _state.value.sendingToUserIds - toUserId,
                        sentPendingToUserIds = _state.value.sentPendingToUserIds + toUserId,
                        sentStatusByUserId = _state.value.sentStatusByUserId + (toUserId to "pending"),
                        message = "Interest request is already pending.",
                        errorMessage = null
                    )
                }
                is InterestRequestResult.Error -> {
                    _state.value = _state.value.copy(
                        sendingToUserIds = _state.value.sendingToUserIds - toUserId,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun respondToInterest(currentUserId: String, requestId: String, accepted: Boolean) {
        if (requestId.isBlank() || requestId in _state.value.respondingRequestIds) return
        _state.value = _state.value.copy(
            respondingRequestIds = _state.value.respondingRequestIds + requestId,
            message = null,
            errorMessage = null
        )
        viewModelScope.launch {
            when (val result = repository.respondToInterestRequest(requestId, accepted)) {
                is InterestRequestResult.Success -> {
                    _state.value = _state.value.copy(
                        respondingRequestIds = _state.value.respondingRequestIds - requestId,
                        message = if (accepted) "Interest accepted." else "Interest declined.",
                        errorMessage = null
                    )
                    loadForUser(currentUserId)
                }
                InterestRequestResult.AlreadyPending -> Unit
                is InterestRequestResult.Error -> {
                    _state.value = _state.value.copy(
                        respondingRequestIds = _state.value.respondingRequestIds - requestId,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun cancelInterest(currentUserId: String, requestId: String) {
        if (requestId.isBlank() || requestId in _state.value.cancellingRequestIds) return
        _state.value = _state.value.copy(
            cancellingRequestIds = _state.value.cancellingRequestIds + requestId,
            message = null,
            errorMessage = null
        )
        viewModelScope.launch {
            when (val result = repository.cancelInterestRequest(requestId)) {
                is InterestRequestResult.Success -> {
                    _state.value = _state.value.copy(
                        cancellingRequestIds = _state.value.cancellingRequestIds - requestId,
                        message = "Interest request cancelled.",
                        errorMessage = null
                    )
                    loadForUser(currentUserId)
                }
                InterestRequestResult.AlreadyPending -> Unit
                is InterestRequestResult.Error -> {
                    _state.value = _state.value.copy(
                        cancellingRequestIds = _state.value.cancellingRequestIds - requestId,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(message = null, errorMessage = null)
    }
}
