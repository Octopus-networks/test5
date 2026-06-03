package com.mithaq.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mithaq.app.data.repository.AdminActionResult
import com.mithaq.app.data.repository.AdminModerationRepository
import com.mithaq.app.domain.model.ModerationStatus
import com.mithaq.app.domain.model.PhotoStatus
import com.mithaq.app.domain.model.Report
import com.mithaq.app.domain.model.ReportStatus
import com.mithaq.app.domain.model.UserModeration
import com.mithaq.app.domain.model.UserPhoto
import com.mithaq.app.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Safe admin check used to gate admin-only UI. Server rules remain the real boundary. */
fun isAdminUser(profile: UserProfile?): Boolean = profile?.isAdmin == true

data class AdminModerationUiState(
    val authorized: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val reports: List<Report> = emptyList(),
    val pendingPhotos: List<UserPhoto> = emptyList(),
    val moderationEntries: List<UserModeration> = emptyList(),
    val actionMessage: String? = null
)

/**
 * Phase 12 — drives [AdminModerationScreen]. Loads nothing and exposes no data unless the caller is
 * an admin (verified again server-side via [AdminModerationRepository.isCurrentUserAdmin]).
 */
class AdminModerationViewModel(
    private val repository: AdminModerationRepository = AdminModerationRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(AdminModerationUiState())
    val state: StateFlow<AdminModerationUiState> = _state.asStateFlow()

    /** Entry point. [isAdmin] is the UI gate (e.g. from currentUserProfile.isAdmin). */
    fun start(isAdmin: Boolean) {
        if (!isAdmin) {
            _state.value = AdminModerationUiState(authorized = false)
            return
        }
        _state.value = _state.value.copy(authorized = true)
        refresh()
    }

    fun refresh() {
        if (!_state.value.authorized) return
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            // Defense in depth: confirm admin from the server-controlled user doc before reading.
            if (!repository.isCurrentUserAdmin()) {
                _state.value = AdminModerationUiState(authorized = false)
                return@launch
            }
            val reports = repository.getOpenReports()
            val photos = repository.getPendingPhotos()
            val mods = repository.getUserModerationEntries()
            _state.value = _state.value.copy(
                isLoading = false,
                reports = reports,
                pendingPhotos = photos,
                moderationEntries = mods
            )
        }
    }

    fun reviewReport(reportId: String, status: ReportStatus, adminNote: String) {
        if (!_state.value.authorized) return
        viewModelScope.launch {
            applyResult(repository.reviewReport(reportId, status, adminNote))
        }
    }

    fun setPhotoStatus(
        userId: String,
        photoId: String,
        status: PhotoStatus,
        rejectionReason: String = ""
    ) {
        if (!_state.value.authorized) return
        viewModelScope.launch {
            applyResult(repository.setPhotoStatus(userId, photoId, status, rejectionReason))
        }
    }

    fun setUserModeration(userId: String, status: ModerationStatus, note: String) {
        if (!_state.value.authorized) return
        viewModelScope.launch {
            applyResult(repository.setUserModeration(userId, status, note))
        }
    }

    fun clearActionMessage() {
        _state.value = _state.value.copy(actionMessage = null)
    }

    private suspend fun applyResult(result: AdminActionResult) {
        when (result) {
            is AdminActionResult.Success -> {
                val reports = repository.getOpenReports()
                val photos = repository.getPendingPhotos()
                val mods = repository.getUserModerationEntries()
                _state.value = _state.value.copy(
                    reports = reports,
                    pendingPhotos = photos,
                    moderationEntries = mods,
                    actionMessage = "Done",
                    error = null
                )
            }
            is AdminActionResult.Error -> {
                _state.value = _state.value.copy(error = result.message)
            }
        }
    }
}
