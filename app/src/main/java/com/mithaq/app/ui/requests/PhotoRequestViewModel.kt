package com.mithaq.app.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mithaq.app.data.repository.BlockRepository
import com.mithaq.app.data.repository.PhotoRequestRepository
import com.mithaq.app.data.repository.PhotoRequestResult
import com.mithaq.app.data.repository.PublicProfileRepository
import com.mithaq.app.domain.model.PhotoRequest
import com.mithaq.app.domain.model.PublicProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PhotoRequestUiState(
    val isLoadingRequests: Boolean = false,
    val requestingToUserIds: Set<String> = emptySet(),
    val respondingRequestIds: Set<String> = emptySet(),
    val cancellingRequestIds: Set<String> = emptySet(),
    val sentStatusByUserId: Map<String, String> = emptyMap(),
    val blockedUserIds: Set<String> = emptySet(),
    val sentRequests: List<PhotoRequest> = emptyList(),
    val receivedPendingRequests: List<PhotoRequest> = emptyList(),
    val receivedHistoryRequests: List<PhotoRequest> = emptyList(),
    val publicProfilesByUserId: Map<String, PublicProfile> = emptyMap(),
    val message: String? = null,
    val errorMessage: String? = null
)

class PhotoRequestViewModel(
    private val repository: PhotoRequestRepository = PhotoRequestRepository(),
    private val publicProfileRepository: PublicProfileRepository = PublicProfileRepository(),
    private val blockRepository: BlockRepository = BlockRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(PhotoRequestUiState())
    val state: StateFlow<PhotoRequestUiState> = _state.asStateFlow()

    fun loadForUser(userId: String) {
        if (userId.isBlank()) return
        _state.value = _state.value.copy(isLoadingRequests = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val sent = repository.getSentPhotoRequests(userId)
                val received = repository.getReceivedPhotoRequests(userId)
                val profileIds = (sent.map { it.toUserId } + received.map { it.fromUserId })
                    .filter { it.isNotBlank() }
                    .distinct()
                val blockedUserIds = mutableSetOf<String>()
                for (profileId in profileIds) {
                    if (blockRepository.isBlockedBetweenUsers(userId, profileId)) {
                        blockedUserIds += profileId
                    }
                }
                val profiles = mutableMapOf<String, PublicProfile>()
                for (profileId in profileIds) {
                    publicProfileRepository.getPublicProfile(profileId)?.let { profile ->
                        profiles[profileId] = profile
                    }
                }
                _state.value = _state.value.copy(
                    isLoadingRequests = false,
                    sentStatusByUserId = sent.associate { it.toUserId to it.status },
                    blockedUserIds = blockedUserIds,
                    sentRequests = sent,
                    receivedPendingRequests = received.filter { it.status == "pending" },
                    receivedHistoryRequests = received.filter { it.status != "pending" },
                    publicProfilesByUserId = profiles
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoadingRequests = false,
                    errorMessage = e.localizedMessage ?: "Could not load photo requests."
                )
            }
        }
    }

    fun requestPhoto(fromUserId: String, toUserId: String) {
        if (fromUserId.isBlank() || toUserId.isBlank() || toUserId in _state.value.requestingToUserIds) return
        _state.value = _state.value.copy(
            requestingToUserIds = _state.value.requestingToUserIds + toUserId,
            message = null,
            errorMessage = null
        )
        viewModelScope.launch {
            when (val result = repository.requestPhoto(fromUserId, toUserId)) {
                is PhotoRequestResult.Success -> {
                    _state.value = _state.value.copy(
                        requestingToUserIds = _state.value.requestingToUserIds - toUserId,
                        sentStatusByUserId = _state.value.sentStatusByUserId + (toUserId to "pending"),
                        message = "Photo request sent.",
                        errorMessage = null
                    )
                    loadForUser(fromUserId)
                }
                PhotoRequestResult.AlreadyPending -> {
                    _state.value = _state.value.copy(
                        requestingToUserIds = _state.value.requestingToUserIds - toUserId,
                        sentStatusByUserId = _state.value.sentStatusByUserId + (toUserId to "pending"),
                        message = "Photo request is already pending.",
                        errorMessage = null
                    )
                }
                is PhotoRequestResult.Error -> {
                    _state.value = _state.value.copy(
                        requestingToUserIds = _state.value.requestingToUserIds - toUserId,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun cancelPhotoRequest(currentUserId: String, requestId: String) {
        if (requestId.isBlank() || requestId in _state.value.cancellingRequestIds) return
        _state.value = _state.value.copy(
            cancellingRequestIds = _state.value.cancellingRequestIds + requestId,
            message = null,
            errorMessage = null
        )
        viewModelScope.launch {
            when (val result = repository.cancelPhotoRequest(requestId)) {
                is PhotoRequestResult.Success -> {
                    _state.value = _state.value.copy(
                        cancellingRequestIds = _state.value.cancellingRequestIds - requestId,
                        message = "Photo request cancelled.",
                        errorMessage = null
                    )
                    loadForUser(currentUserId)
                }
                PhotoRequestResult.AlreadyPending -> Unit
                is PhotoRequestResult.Error -> {
                    _state.value = _state.value.copy(
                        cancellingRequestIds = _state.value.cancellingRequestIds - requestId,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun respondToPhotoRequest(currentUserId: String, requestId: String, approved: Boolean) {
        if (requestId.isBlank() || requestId in _state.value.respondingRequestIds) return
        _state.value = _state.value.copy(
            respondingRequestIds = _state.value.respondingRequestIds + requestId,
            message = null,
            errorMessage = null
        )
        viewModelScope.launch {
            when (val result = repository.respondToPhotoRequest(requestId, approved)) {
                is PhotoRequestResult.Success -> {
                    _state.value = _state.value.copy(
                        respondingRequestIds = _state.value.respondingRequestIds - requestId,
                        message = if (approved) "Photo access approved." else "Photo request declined.",
                        errorMessage = null
                    )
                    loadForUser(currentUserId)
                }
                PhotoRequestResult.AlreadyPending -> Unit
                is PhotoRequestResult.Error -> {
                    _state.value = _state.value.copy(
                        respondingRequestIds = _state.value.respondingRequestIds - requestId,
                        errorMessage = result.message
                    )
                }
            }
        }
    }
}
