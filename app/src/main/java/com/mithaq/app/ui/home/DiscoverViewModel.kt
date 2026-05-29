package com.mithaq.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mithaq.app.data.repository.PublicProfileRepository
import com.mithaq.app.domain.model.PublicProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PublicProfileFilter {
    Recommended,
    NearMe,
    Verified,
    WithGuardian,
    RecentlyActive,
    PrayerRoutineShared,
    NewMembers
}

data class DiscoverUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val allProfiles: List<PublicProfile> = emptyList(),
    val selectedFilter: PublicProfileFilter = PublicProfileFilter.Recommended
) {
    val visibleProfiles: List<PublicProfile>
        get() {
            val profiles = when (selectedFilter) {
                PublicProfileFilter.Recommended -> allProfiles.sortedByDescending { it.profileCompletionPercent }
                PublicProfileFilter.NearMe -> allProfiles
                PublicProfileFilter.Verified -> allProfiles.filter { it.isEmailVerified || it.isIdentityVerified }
                PublicProfileFilter.WithGuardian -> allProfiles.filter { it.hasGuardian }
                PublicProfileFilter.RecentlyActive -> allProfiles
                    .filter { it.lastActiveAt != null }
                    .sortedByDescending { it.lastActiveAt }
                PublicProfileFilter.PrayerRoutineShared -> allProfiles.filter { it.prayerRoutineShared }
                PublicProfileFilter.NewMembers -> allProfiles
                    .filter { it.updatedAt != null }
                    .sortedByDescending { it.updatedAt }
            }
            return profiles
        }

    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && allProfiles.isEmpty()

    val hasNoFilterResults: Boolean
        get() = !isLoading && errorMessage == null && allProfiles.isNotEmpty() && visibleProfiles.isEmpty()

    val isNearMePlaceholder: Boolean
        get() = selectedFilter == PublicProfileFilter.NearMe
}

class DiscoverViewModel(
    private val repository: PublicProfileRepository = PublicProfileRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(DiscoverUiState())
    val state: StateFlow<DiscoverUiState> = _state.asStateFlow()

    init {
        loadProfiles()
    }

    fun loadProfiles() {
        _state.value = _state.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val profiles = repository.getDiscoverProfiles()
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = null,
                    allProfiles = profiles
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Could not load public profiles right now."
                )
            }
        }
    }

    fun selectFilter(filter: PublicProfileFilter) {
        _state.value = _state.value.copy(selectedFilter = filter)
    }
}
