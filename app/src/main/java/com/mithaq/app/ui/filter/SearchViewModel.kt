package com.mithaq.app.ui.filter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mithaq.app.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel managing search parameters and applying complex Islamic filters to user results.
 */
class SearchViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _filterCriteria = MutableStateFlow(FilterCriteria())
    val filterCriteria: StateFlow<FilterCriteria> = _filterCriteria.asStateFlow()

    private val _searchResults = MutableStateFlow<List<UserProfile>>(emptyList())
    val searchResults: StateFlow<List<UserProfile>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Full list of profiles downloaded to filter locally (to support complex multi-field inequalities not possible in Firestore indexing)
    private var allUsersCache = listOf<UserProfile>()

    init {
        fetchUsers()
    }

    /**
     * Fetches all matching users from Firestore (excluding current user and opposite gender).
     */
    fun fetchUsers() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _errorMessage.value = "User not authenticated."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                // First get current user's profile to know their gender and filter for the opposite gender
                val currentUserDoc = firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()
                
                val userGender = currentUserDoc.getString("gender") ?: "MALE"
                val oppositeGender = if (userGender == "MALE") "FEMALE" else "MALE"

                val snapshot = firestore.collection("users")
                    .whereEqualTo("gender", oppositeGender)
                    .get()
                    .await()

                val profiles = snapshot.documents.mapNotNull { doc ->
                    try {
                        val uid = doc.id
                        val name = doc.getString("name") ?: ""
                        val genderStr = doc.getString("gender") ?: "FEMALE"
                        val gender = if (genderStr == "MALE") Gender.MALE else Gender.FEMALE
                        val age = doc.getLong("age")?.toInt() ?: 18
                        val city = doc.getString("city") ?: ""
                        val country = doc.getString("country") ?: ""
                        val imageUrl = doc.getString("imageUrl") ?: ""
                        
                        val sectStr = doc.getString("sect") ?: "SUNNI"
                        val sect = Sect.valueOf(sectStr)
                        
                        val prayerStr = doc.getString("prayerFrequency") ?: "ALWAYS"
                        val prayer = PrayerFrequency.valueOf(prayerStr)
                        
                        val modestyStr = doc.getString("modestyPreference") ?: "HIJAB"
                        val modesty = ModestyPreference.valueOf(modestyStr)
                        
                        val relocationStr = doc.getString("relocationWillingness") ?: "OPEN"
                        val relocation = RelocationWillingness.valueOf(relocationStr)
                        
                        val polygamy = doc.getBoolean("polygamyAcceptance") ?: false

                        UserProfile(
                            uid = uid,
                            name = name,
                            gender = gender,
                            age = age,
                            city = city,
                            country = country,
                            imageUrl = imageUrl,
                            sect = sect,
                            prayerFrequency = prayer,
                            modestyPreference = modesty,
                            relocationWillingness = relocation,
                            polygamyAcceptance = polygamy
                        )
                    } catch (e: Exception) {
                        null // Skip malformed profile documents
                    }
                }

                allUsersCache = profiles
                applyLocalFilters()
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to load profiles."
                _isLoading.value = false
            }
        }
    }

    /**
     * Updates current filter selections and triggers re-filtering.
     */
    fun updateFilters(newCriteria: FilterCriteria) {
        _filterCriteria.value = newCriteria
        applyLocalFilters()
    }

    /**
     * Filters the cached list of user profiles locally.
     */
    private fun applyLocalFilters() {
        val criteria = _filterCriteria.value
        val filtered = allUsersCache.filter { user ->
            // 1. Age Range Check
            if (user.age !in criteria.minAge..criteria.maxAge) return@filter false
            
            // 2. Sect Check
            if (criteria.sect != null && user.sect != criteria.sect) return@filter false
            
            // 3. Prayer Frequency Check
            if (criteria.prayerFrequencies.isNotEmpty() && user.prayerFrequency !in criteria.prayerFrequencies) return@filter false
            
            // 4. Modesty Preference (Hijab/Niqab) Check
            if (criteria.modestyPreferences.isNotEmpty() && user.modestyPreference !in criteria.modestyPreferences) return@filter false
            
            // 5. Relocation Willingness Check
            if (criteria.relocationWillingness.isNotEmpty() && user.relocationWillingness !in criteria.relocationWillingness) return@filter false
            
            // 6. Polygamy Acceptance Check
            if (criteria.polygamyAcceptance != null && user.polygamyAcceptance != criteria.polygamyAcceptance) return@filter false
            
            true
        }
        _searchResults.value = filtered
    }

    fun resetFilters() {
        _filterCriteria.value = FilterCriteria()
        applyLocalFilters()
    }
}
