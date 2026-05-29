package com.mithaq.app.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mithaq.app.data.repository.BlockRepository
import com.mithaq.app.data.repository.ReportRepository
import com.mithaq.app.data.repository.SafetyWriteResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatSafetyUiState(
    val isBlocked: Boolean = false,
    val isCheckingBlock: Boolean = false,
    val isSubmittingReport: Boolean = false,
    val isBlocking: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null
)

class ChatSafetyViewModel(
    private val reportRepository: ReportRepository = ReportRepository(),
    private val blockRepository: BlockRepository = BlockRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(ChatSafetyUiState())
    val state: StateFlow<ChatSafetyUiState> = _state.asStateFlow()

    fun loadBlockState(currentUserId: String, otherUserId: String) {
        if (currentUserId.isBlank() || otherUserId.isBlank()) return
        _state.value = _state.value.copy(isCheckingBlock = true, errorMessage = null)
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    isCheckingBlock = false,
                    isBlocked = blockRepository.isBlockedBetweenUsers(currentUserId, otherUserId)
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isCheckingBlock = false,
                    errorMessage = e.localizedMessage ?: "Could not check conversation safety."
                )
            }
        }
    }

    fun reportUser(
        reporterUserId: String,
        reportedUserId: String,
        chatId: String,
        reason: String,
        details: String
    ) {
        if (_state.value.isSubmittingReport) return
        _state.value = _state.value.copy(isSubmittingReport = true, message = null, errorMessage = null)
        viewModelScope.launch {
            when (val result = reportRepository.reportUser(reporterUserId, reportedUserId, chatId, reason, details)) {
                is SafetyWriteResult.Success -> _state.value = _state.value.copy(
                    isSubmittingReport = false,
                    message = result.message
                )
                is SafetyWriteResult.Error -> _state.value = _state.value.copy(
                    isSubmittingReport = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun blockUser(blockerUserId: String, blockedUserId: String, chatId: String) {
        if (_state.value.isBlocking) return
        _state.value = _state.value.copy(isBlocking = true, message = null, errorMessage = null)
        viewModelScope.launch {
            when (val result = blockRepository.blockUser(blockerUserId, blockedUserId, chatId)) {
                is SafetyWriteResult.Success -> _state.value = _state.value.copy(
                    isBlocking = false,
                    isBlocked = true,
                    message = result.message
                )
                is SafetyWriteResult.Error -> _state.value = _state.value.copy(
                    isBlocking = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(message = null, errorMessage = null)
    }
}
