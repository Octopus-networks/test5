package com.mithaq.app.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mithaq.app.data.repository.PhotoRequestRepository
import com.mithaq.app.data.repository.PhotoRequestResult
import com.mithaq.app.data.repository.PublicProfileRepository
import com.mithaq.app.domain.model.PhotoRequest
import com.mithaq.app.domain.model.PublicProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

data class PhotoRequestUiState(
    val isLoadingRequests: Boolean = false,
    val requestingToUserIds: Set<String> = emptySet(),
    val respondingRequestIds: Set<String> = emptySet(),
    val cancellingRequestIds: Set<String> = emptySet(),
    val sentStatusByUserId: Map<String, String> = emptyMap(),
    val sentRequests: List<PhotoRequest> = emptyList(),
    val receivedPendingRequests: List<PhotoRequest> = emptyList(),
    val receivedHistoryRequests: List<PhotoRequest> = emptyList(),
    val publicProfilesByUserId: Map<String, PublicProfile> = emptyMap(),
    val messageRes: Int? = null,
    val messageResAr: Int? = null,
    val errorMessageRes: Int? = null,
    val errorMessageResAr: Int? = null,
    val errorMessage: String? = null
)

class PhotoRequestViewModel(
    private val repository: PhotoRequestRepository = PhotoRequestRepository(),
    private val publicProfileRepository: PublicProfileRepository = PublicProfileRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(PhotoRequestUiState())
    val state: StateFlow<PhotoRequestUiState> = _state.asStateFlow()

    fun loadForUser(userId: String) {
        if (userId.isBlank()) return
        _state.value = _state.value.copy(isLoadingRequests = true, errorMessage = null, errorMessageRes = null, errorMessageResAr = null)
        viewModelScope.launch {
            try {
                val sent = repository.getSentPhotoRequests(userId)
                val received = repository.getReceivedPhotoRequests(userId)
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
            messageRes = null,
            messageResAr = null,
            errorMessageRes = null,
            errorMessageResAr = null,
            errorMessage = null
        )
        viewModelScope.launch {
            when (val result = repository.requestPhoto(fromUserId, toUserId)) {
                is PhotoRequestResult.Success -> {
                    _state.value = _state.value.copy(
                        requestingToUserIds = _state.value.requestingToUserIds - toUserId,
                        sentStatusByUserId = _state.value.sentStatusByUserId + (toUserId to "pending"),
                        messageRes = com.mithaq.app.R.string.msg_photo_sent,
                        messageResAr = com.mithaq.app.R.string.msg_photo_sent_ar,
                        errorMessageRes = null,
                        errorMessageResAr = null,
                        errorMessage = null
                    )
                    loadForUser(fromUserId)
                }
                PhotoRequestResult.AlreadyPending -> {
                    _state.value = _state.value.copy(
                        requestingToUserIds = _state.value.requestingToUserIds - toUserId,
                        sentStatusByUserId = _state.value.sentStatusByUserId + (toUserId to "pending"),
                        messageRes = com.mithaq.app.R.string.msg_photo_pending,
                        messageResAr = com.mithaq.app.R.string.msg_photo_pending_ar,
                        errorMessageRes = null,
                        errorMessageResAr = null,
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
            messageRes = null,
            messageResAr = null,
            errorMessageRes = null,
            errorMessageResAr = null,
            errorMessage = null
        )
        viewModelScope.launch {
            when (val result = repository.cancelPhotoRequest(requestId)) {
                is PhotoRequestResult.Success -> {
                    _state.value = _state.value.copy(
                        cancellingRequestIds = _state.value.cancellingRequestIds - requestId,
                        messageRes = com.mithaq.app.R.string.msg_photo_cancelled,
                        messageResAr = com.mithaq.app.R.string.msg_photo_cancelled_ar,
                        errorMessageRes = null,
                        errorMessageResAr = null,
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
            messageRes = null,
            messageResAr = null,
            errorMessageRes = null,
            errorMessageResAr = null,
            errorMessage = null
        )
        viewModelScope.launch {
            when (val result = repository.respondToPhotoRequest(requestId, approved)) {
                is PhotoRequestResult.Success -> {
                    _state.value = _state.value.copy(
                        respondingRequestIds = _state.value.respondingRequestIds - requestId,
                        messageRes = if (approved) com.mithaq.app.R.string.msg_photo_approved else com.mithaq.app.R.string.msg_photo_declined,
                        messageResAr = if (approved) com.mithaq.app.R.string.msg_photo_approved_ar else com.mithaq.app.R.string.msg_photo_declined_ar,
                        errorMessageRes = null,
                        errorMessageResAr = null,
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

    fun clearMessages() {
        _state.value = _state.value.copy(messageRes = null, messageResAr = null, errorMessageRes = null, errorMessageResAr = null, errorMessage = null)
    }
}
