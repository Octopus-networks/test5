package com.mithaq.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mithaq.app.data.repository.ModerationRepository
import com.mithaq.app.data.repository.ModerationResult
import com.mithaq.app.domain.model.ModerationPhotoReviewItem
import com.mithaq.app.domain.model.ModerationUserReport
import com.mithaq.app.domain.model.ReportStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminModerationUiState(
    val isCheckingAccess: Boolean = true,
    val hasModeratorAccess: Boolean = false,
    val isLoading: Boolean = false,
    val openReports: List<ModerationUserReport> = emptyList(),
    val pendingPhotos: List<ModerationPhotoReviewItem> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val openReportsCount: Int get() = openReports.size
    val pendingPhotosCount: Int get() = pendingPhotos.size
}

class AdminModerationViewModel(
    private val repository: ModerationRepository = ModerationRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(AdminModerationUiState())
    val state: StateFlow<AdminModerationUiState> = _state.asStateFlow()

    fun load() {
        _state.value = _state.value.copy(
            isCheckingAccess = true,
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )
        viewModelScope.launch {
            val hasAccess = repository.isCurrentUserModerator()
            if (!hasAccess) {
                _state.value = AdminModerationUiState(
                    isCheckingAccess = false,
                    hasModeratorAccess = false,
                    errorMessage = "Admin or moderator access is required."
                )
                return@launch
            }
            _state.value = _state.value.copy(
                isCheckingAccess = false,
                hasModeratorAccess = true
            )
            refreshLists()
        }
    }

    fun markReportReviewed(reportId: String) {
        updateReport(reportId, ReportStatus.Reviewed, "")
    }

    fun dismissReport(reportId: String, adminNote: String = "") {
        updateReport(reportId, ReportStatus.Dismissed, adminNote)
    }

    fun markReportActionTaken(reportId: String, adminNote: String = "") {
        updateReport(reportId, ReportStatus.ActionTaken, adminNote)
    }

    fun approvePhoto(userId: String, photoId: String) {
        performAction { repository.approvePhoto(userId, photoId) }
    }

    fun rejectPhoto(userId: String, photoId: String, reason: String = "") {
        performAction { repository.rejectPhoto(userId, photoId, reason) }
    }

    private fun updateReport(reportId: String, status: String, adminNote: String) {
        performAction { repository.updateReportStatus(reportId, status, adminNote) }
    }

    private fun performAction(action: suspend () -> ModerationResult<Unit>) {
        _state.value = _state.value.copy(isLoading = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            when (val result = action()) {
                is ModerationResult.Success -> {
                    _state.value = _state.value.copy(successMessage = "Moderation action saved.")
                    refreshLists()
                }
                is ModerationResult.Error -> {
                    _state.value = _state.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    private suspend fun refreshLists() {
        val reportsResult = repository.getOpenReports()
        val photosResult = repository.getPendingPhotosForReview()
        val reports = when (reportsResult) {
            is ModerationResult.Success -> reportsResult.value
            is ModerationResult.Error -> emptyList()
        }
        val photos = when (photosResult) {
            is ModerationResult.Success -> photosResult.value
            is ModerationResult.Error -> emptyList()
        }
        val error = listOf(reportsResult, photosResult)
            .filterIsInstance<ModerationResult.Error>()
            .firstOrNull()
            ?.message
        _state.value = _state.value.copy(
            isCheckingAccess = false,
            hasModeratorAccess = true,
            isLoading = false,
            openReports = reports,
            pendingPhotos = photos,
            errorMessage = error
        )
    }
}
