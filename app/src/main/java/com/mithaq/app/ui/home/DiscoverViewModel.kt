package com.mithaq.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mithaq.app.data.repository.PublicProfileRepository
import com.mithaq.app.domain.model.PublicProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.mithaq.app.R
import com.google.firebase.auth.FirebaseAuth
import com.mithaq.app.data.repository.premiumWeightedInterleave

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
    val errorMessageRes: Int? = null,
    val errorMessageResAr: Int? = null,
    val errorMessage: String? = null,
    val allProfiles: List<PublicProfile> = emptyList(),
    val currentUserCountry: String? = null,
    val selectedFilter: PublicProfileFilter = PublicProfileFilter.Recommended
) {
    val visibleProfiles: List<PublicProfile>
        get() {
            val profiles = when (selectedFilter) {
                PublicProfileFilter.Recommended -> allProfiles.sortedByDescending { it.profileCompletionPercent }
                PublicProfileFilter.NearMe -> allProfiles.filter { 
                    !currentUserCountry.isNullOrBlank() && 
                    !it.country.isNullOrBlank() && 
                    it.country.equals(currentUserCountry, ignoreCase = true) 
                }
                PublicProfileFilter.Verified -> allProfiles.filter { it.isIdentityVerified }
                PublicProfileFilter.WithGuardian -> allProfiles.filter { it.hasGuardian }
                PublicProfileFilter.RecentlyActive -> allProfiles
                    .filter { it.lastActiveAt != null }
                    .sortedByDescending { it.lastActiveAt }
                PublicProfileFilter.PrayerRoutineShared -> allProfiles.filter { it.prayerRoutineShared }
                PublicProfileFilter.NewMembers -> allProfiles
                    .sortedWith(compareByDescending<PublicProfile> { it.memberSince }.thenByDescending { it.updatedAt })
            }
            return profiles.premiumWeightedInterleave()
        }

    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && errorMessageRes == null && allProfiles.isEmpty()

    val hasNoFilterResults: Boolean
        get() = !isLoading && errorMessage == null && errorMessageRes == null && allProfiles.isNotEmpty() && visibleProfiles.isEmpty()

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
        _state.value = _state.value.copy(isLoading = true, errorMessageRes = null, errorMessageResAr = null, errorMessage = null)
        viewModelScope.launch {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                val userCountry = repository.getCurrentUserCountry(currentUserId)
                val profiles = repository.getDiscoverProfiles()
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessageRes = null,
                    errorMessageResAr = null,
                    errorMessage = null,
                    allProfiles = profiles,
                    currentUserCountry = userCountry
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessageRes = R.string.msg_err_load_profiles,
                    errorMessageResAr = R.string.msg_err_load_profiles_ar,
                    errorMessage = e.localizedMessage
                )
            }
        }
    }

    fun selectFilter(filter: PublicProfileFilter) {
        _state.value = _state.value.copy(selectedFilter = filter)
    }
}
