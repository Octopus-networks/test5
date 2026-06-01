package com.mithaq.app.ui.photo

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mithaq.app.data.repository.PhotoOperationResult
import com.mithaq.app.data.repository.PhotoRepository
import com.mithaq.app.domain.model.PhotoType
import com.mithaq.app.domain.model.PhotoVisibility
import com.mithaq.app.domain.model.UserPhoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MyPhotosUiState(
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val currentPrivacyMode: String = PhotoVisibility.ApprovedUsersOnly,
    val photos: List<UserPhoto> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class MyPhotosViewModel(
    application: Application,
    private val repository: PhotoRepository = PhotoRepository()
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(MyPhotosUiState())
    val state: StateFlow<MyPhotosUiState> = _state.asStateFlow()

    fun load(userId: String) {
        if (userId.isBlank()) return
        _state.value = _state.value.copy(isLoading = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            try {
                val privacy = repository.getPhotoPrivacyMode(userId)
                val photos = repository.getMyPhotos(userId)
                _state.value = _state.value.copy(
                    isLoading = false,
                    currentPrivacyMode = privacy,
                    photos = photos
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Could not load photos."
                )
            }
        }
    }

    fun uploadProfilePhoto(userId: String, uri: Uri, type: String) {
        if (userId.isBlank()) return
        _state.value = _state.value.copy(isUploading = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            when (val result = repository.uploadProfilePhoto(userId, uri, type, getApplication())) {
                is PhotoOperationResult.Success -> {
                    _state.value = _state.value.copy(
                        isUploading = false,
                        successMessage = if (type == PhotoType.Main) "Main photo uploaded for review." else "Extra photo uploaded for review."
                    )
                    load(userId)
                }
                is PhotoOperationResult.Error -> {
                    _state.value = _state.value.copy(isUploading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun updatePhotoPrivacy(userId: String, visibility: String) {
        if (userId.isBlank()) return
        _state.value = _state.value.copy(isLoading = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            when (val result = repository.updatePhotoPrivacy(userId, visibility)) {
                is PhotoOperationResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        currentPrivacyMode = result.value,
                        successMessage = "Photo privacy updated."
                    )
                    load(userId)
                }
                is PhotoOperationResult.Error -> {
                    _state.value = _state.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }
}
