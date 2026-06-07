package com.mithaq.app.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mithaq.app.data.repository.NotificationSettingsRepository
import com.mithaq.app.domain.model.NotificationSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationSettingsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val settings: NotificationSettings = NotificationSettings(),
    val baseline: NotificationSettings = NotificationSettings(),
    val errorMessage: String? = null,
    val justSaved: Boolean = false
) {
    val hasChanges: Boolean get() = settings != baseline
}

/**
 * Phase 13C — drives the notification settings screen. Loads the owner's preferences,
 * holds an editable copy, and persists on an explicit Save. Defaults to all-enabled
 * whenever preferences are missing or fail to load.
 */
class NotificationSettingsViewModel(
    private val currentUserId: String,
    private val repository: NotificationSettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationSettingsUiState())
    val state: StateFlow<NotificationSettingsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = _state.value.copy(isLoading = true, errorMessage = null, justSaved = false)
        viewModelScope.launch {
            val loaded = repository.load(currentUserId)
            _state.value = _state.value.copy(
                isLoading = false,
                settings = loaded,
                baseline = loaded
            )
        }
    }

    /** Applies an in-memory edit; not persisted until [save]. */
    fun update(transform: (NotificationSettings) -> NotificationSettings) {
        _state.value = _state.value.copy(
            settings = transform(_state.value.settings),
            justSaved = false,
            errorMessage = null
        )
    }

    fun save() {
        val current = _state.value
        if (current.isSaving || !current.hasChanges) return
        _state.value = current.copy(isSaving = true, errorMessage = null, justSaved = false)
        viewModelScope.launch {
            val result = repository.save(currentUserId, current.settings)
            _state.value = if (result.isSuccess) {
                _state.value.copy(
                    isSaving = false,
                    baseline = current.settings,
                    justSaved = true
                )
            } else {
                _state.value.copy(
                    isSaving = false,
                    errorMessage = result.exceptionOrNull()?.localizedMessage
                        ?: "Could not save your settings. Please try again."
                )
            }
        }
    }
}
