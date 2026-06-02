package com.mithaq.app.ui.photo

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mithaq.app.data.repository.PhotoRepository
import com.mithaq.app.data.repository.PhotoUploadResult
import com.mithaq.app.domain.model.PhotoType
import com.mithaq.app.domain.model.UserPhoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MyPhotosUiState(
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val photos: List<UserPhoto> = emptyList(),
    val message: String? = null
)

class MyPhotosViewModel(
    private val repository: PhotoRepository = PhotoRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(MyPhotosUiState())
    val state: StateFlow<MyPhotosUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = _state.value.copy(isLoading = true)
        viewModelScope.launch {
            val photos = repository.getMyPhotos()
            _state.value = _state.value.copy(isLoading = false, photos = photos)
        }
    }

    fun upload(type: PhotoType, uri: Uri?) {
        if (uri == null) return
        _state.value = _state.value.copy(isUploading = true, message = null)
        viewModelScope.launch {
            when (val result = repository.uploadPhoto(type, uri)) {
                is PhotoUploadResult.Success -> {
                    _state.value = _state.value.copy(
                        isUploading = false,
                        message = "Uploaded — pending review."
                    )
                    refresh()
                }
                is PhotoUploadResult.Error -> {
                    _state.value = _state.value.copy(isUploading = false, message = result.message)
                }
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
