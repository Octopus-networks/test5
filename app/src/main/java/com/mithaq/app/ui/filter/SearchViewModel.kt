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
import com.mithaq.app.data.local.MithaqDatabase
import com.mithaq.app.data.local.CachedUserProfile
import com.mithaq.app.data.local.toCached
import com.mithaq.app.data.local.toDomain

/**
 * ViewModel managing search parameters and applying complex Islamic filters to user results.
 */
class SearchViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val context: android.content.Context? = null
) : ViewModel() {

    private val db = context?.let { MithaqDatabase.getDatabase(it) }
    private val userDao = db?.userDao()

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
     * Fetches all matching users from Firestore or Room DB cache.
     */
    fun fetchUsers() {
        val isOfflineSimulated = context?.getSharedPreferences("mithaq_dev_options", android.content.Context.MODE_PRIVATE)
            ?.getBoolean("is_offline_simulated", false) ?: false

        if (isOfflineSimulated) {
            _isLoading.value = true
            _errorMessage.value = null
            viewModelScope.launch {
                val currentUid = auth.currentUser?.uid ?: "mock_user_123"
                val localCached = userDao?.getAllUsers()?.map { it.toDomain() } ?: emptyList()
                val currentUserProfile = userDao?.getUser(currentUid)?.toDomain()
                val userGender = currentUserProfile?.gender ?: Gender.MALE
                val oppositeGender = if (userGender == Gender.MALE) Gender.FEMALE else Gender.MALE
                
                val filteredCached = localCached.filter { it.uid != currentUid && it.gender == oppositeGender }
                if (filteredCached.isNotEmpty()) {
                    allUsersCache = filteredCached
                } else {
                    val defaultMock = listOf(
                        UserProfile(
                            uid = "mock_user_2",
                            name = "Fatima / فاطمة",
                            gender = Gender.FEMALE,
                            age = 24,
                            city = "Cairo",
                            country = "Egypt",
                            imageUrl = "avatar_sister_purple",
                            sect = Sect.SUNNI,
                            prayerFrequency = PrayerFrequency.ALWAYS,
                            modestyPreference = ModestyPreference.HIJAB,
                            relocationWillingness = RelocationWillingness.OPEN,
                            polygamyAcceptance = false,
                            guardianName = "Mahmoud / محمود",
                            guardianEmail = "mahmoud@mithaq.com",
                            guardianStatus = "VERIFIED",
                            verificationStatus = "VERIFIED",
                            isPremium = true,
                            subscriptionPlan = "GOLD",
                            questionnaireAnswers = mapOf(
                                "q1" to "opt1", "q2" to "opt1", "q3" to "opt2", "q4" to "opt4", "q5" to "opt1",
                                "q6" to "opt1", "q7" to "opt2", "q8" to "opt1", "q9" to "opt2", "q10" to "opt1"
                            )
                        ),
                        UserProfile(
                            uid = "mock_user_3",
                            name = "Ahmad / أحمد",
                            gender = Gender.MALE,
                            age = 29,
                            city = "Riyadh",
                            country = "Saudi Arabia",
                            imageUrl = "avatar_brother_green",
                            sect = Sect.SUNNI,
                            prayerFrequency = PrayerFrequency.ALWAYS,
                            modestyPreference = ModestyPreference.DOES_NOT_MATTER,
                            relocationWillingness = RelocationWillingness.YES,
                            polygamyAcceptance = true,
                            verificationStatus = "VERIFIED",
                            isPremium = false,
                            subscriptionPlan = "FREE",
                            questionnaireAnswers = mapOf(
                                "q1" to "opt1", "q2" to "opt1", "q3" to "opt2", "q4" to "opt1", "q5" to "opt2",
                                "q6" to "opt2", "q7" to "opt1", "q8" to "opt2", "q9" to "opt1", "q10" to "opt2"
                            )
                        )
                    )
                    defaultMock.forEach { userDao?.insertUser(it.toCached()) }
                    allUsersCache = defaultMock.filter { it.uid != currentUid && it.gender == oppositeGender }
                }
                applyLocalFilters()
                _isLoading.value = false
            }
            return
        }

        val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
            firestore.app?.options?.apiKey == "mock-api-key-for-testing" || firestore.app?.options?.apiKey?.contains("mock") == true
        } catch (e: Exception) {
            true
        }

        if (isMock) {
            _isLoading.value = true
            _errorMessage.value = null
            viewModelScope.launch {
                kotlinx.coroutines.delay(500)
                val currentUid = auth.currentUser?.uid ?: "mock_user_123"
                val localCached = userDao?.getAllUsers()?.map { it.toDomain() } ?: emptyList()
                val currentUserProfile = userDao?.getUser(currentUid)?.toDomain()
                val oppositeGender = if (currentUserProfile?.gender == Gender.MALE) Gender.FEMALE else Gender.MALE
                
                val filteredCached = localCached.filter { it.uid != currentUid && it.gender == oppositeGender }
                if (filteredCached.isNotEmpty()) {
                    allUsersCache = filteredCached
                } else {
                    val defaultMock = listOf(
                        UserProfile(
                            uid = "mock_user_2",
                            name = "Fatima / فاطمة",
                            gender = Gender.FEMALE,
                            age = 24,
                            city = "Cairo",
                            country = "Egypt",
                            imageUrl = "avatar_sister_purple",
                            sect = Sect.SUNNI,
                            prayerFrequency = PrayerFrequency.ALWAYS,
                            modestyPreference = ModestyPreference.HIJAB,
                            relocationWillingness = RelocationWillingness.OPEN,
                            polygamyAcceptance = false,
                            guardianName = "Mahmoud / محمود",
                            guardianEmail = "mahmoud@mithaq.com",
                            guardianStatus = "VERIFIED",
                            verificationStatus = "VERIFIED",
                            isPremium = true,
                            subscriptionPlan = "GOLD",
                            questionnaireAnswers = mapOf(
                                "q1" to "opt1", "q2" to "opt1", "q3" to "opt2", "q4" to "opt4", "q5" to "opt1",
                                "q6" to "opt1", "q7" to "opt2", "q8" to "opt1", "q9" to "opt2", "q10" to "opt1"
                            )
                        ),
                        UserProfile(
                            uid = "mock_user_3",
                            name = "Ahmad / أحمد",
                            gender = Gender.MALE,
                            age = 29,
                            city = "Riyadh",
                            country = "Saudi Arabia",
                            imageUrl = "avatar_brother_green",
                            sect = Sect.SUNNI,
                            prayerFrequency = PrayerFrequency.ALWAYS,
                            modestyPreference = ModestyPreference.DOES_NOT_MATTER,
                            relocationWillingness = RelocationWillingness.YES,
                            polygamyAcceptance = true,
                            verificationStatus = "VERIFIED",
                            isPremium = false,
                            subscriptionPlan = "FREE",
                            questionnaireAnswers = mapOf(
                                "q1" to "opt1", "q2" to "opt1", "q3" to "opt2", "q4" to "opt1", "q5" to "opt2",
                                "q6" to "opt2", "q7" to "opt1", "q8" to "opt2", "q9" to "opt1", "q10" to "opt2"
                            )
                        ),
                        UserProfile(
                            uid = "mock_user_4",
                            name = "Sarah / سارة",
                            gender = Gender.FEMALE,
                            age = 27,
                            city = "Dubai",
                            country = "UAE",
                            imageUrl = "avatar_sister_purple",
                            sect = Sect.SUNNI,
                            prayerFrequency = PrayerFrequency.USUALLY,
                            modestyPreference = ModestyPreference.HIJAB,
                            relocationWillingness = RelocationWillingness.OPEN,
                            polygamyAcceptance = false,
                            guardianName = "Omar / عمر",
                            guardianEmail = "omar@mithaq.com",
                            guardianStatus = "PENDING",
                            verificationStatus = "PENDING"
                        )
                    )
                    defaultMock.forEach { userDao?.insertUser(it.toCached()) }
                    allUsersCache = defaultMock.filter { it.uid != currentUid && it.gender == oppositeGender }
                }
                applyLocalFilters()
                _isLoading.value = false
            }
            return
        }

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
                        val sect = try { Sect.valueOf(sectStr.uppercase()) } catch (e: Exception) { Sect.SUNNI }
                        
                        val prayerStr = doc.getString("prayerFrequency") ?: "ALWAYS"
                        val prayer = try { PrayerFrequency.valueOf(prayerStr.uppercase()) } catch (e: Exception) { PrayerFrequency.ALWAYS }
                        
                        val modestyStr = doc.getString("modestyPreference") ?: "HIJAB"
                        val modesty = try { ModestyPreference.valueOf(modestyStr.uppercase()) } catch (e: Exception) { ModestyPreference.HIJAB }
                        
                        val relocationStr = doc.getString("relocationWillingness") ?: "OPEN"
                        val relocation = try { RelocationWillingness.valueOf(relocationStr.uppercase()) } catch (e: Exception) { RelocationWillingness.OPEN }
                        
                        val polygamy = doc.getBoolean("polygamyAcceptance") ?: false
                        val guardianName = doc.getString("guardianName")
                        val guardianEmail = doc.getString("guardianEmail")
                        val guardianStatus = doc.getString("guardianStatus")
                        val isWaliAccount = doc.getBoolean("isWaliAccount") ?: false
                        val wardUid = doc.getString("wardUid")
                        val verificationStatus = doc.getString("verificationStatus") ?: "NONE"
                        val voiceIntroUrl = doc.getString("voiceIntroUrl")
                        val isAdmin = doc.getBoolean("isAdmin") ?: false
                        val isPremium = doc.getBoolean("isPremium") ?: false
                        val subscriptionPlan = doc.getString("subscriptionPlan") ?: "FREE"
                        val questionnaireAnswers = doc.get("questionnaireAnswers") as? Map<String, String> ?: emptyMap()

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
                            polygamyAcceptance = polygamy,
                            guardianName = guardianName,
                            guardianEmail = guardianEmail,
                            guardianStatus = guardianStatus,
                            isWaliAccount = isWaliAccount,
                            wardUid = wardUid,
                            verificationStatus = verificationStatus,
                            voiceIntroUrl = voiceIntroUrl,
                            isAdmin = isAdmin,
                            isPremium = isPremium,
                            subscriptionPlan = subscriptionPlan,
                            questionnaireAnswers = questionnaireAnswers
                        )
                    } catch (e: Exception) {
                        null // Skip malformed profile documents
                    }
                }

                // Write fetched profiles to local Room database
                profiles.forEach { userDao?.insertUser(it.toCached()) }

                allUsersCache = profiles
                applyLocalFilters()
                _isLoading.value = false
            } catch (e: Exception) {
                // If network fails, load matching cached profiles from Room database
                val localCached = userDao?.getAllUsers()?.map { it.toDomain() } ?: emptyList()
                val currentUid = currentUser.uid
                val currentUserProfile = userDao?.getUser(currentUid)?.toDomain()
                val userGender = currentUserProfile?.gender ?: Gender.MALE
                val oppositeGender = if (userGender == Gender.MALE) Gender.FEMALE else Gender.MALE
                
                val filteredCached = localCached.filter { it.uid != currentUid && it.gender == oppositeGender }
                if (filteredCached.isNotEmpty()) {
                    allUsersCache = filteredCached
                    applyLocalFilters()
                    _isLoading.value = false
                } else {
                    _errorMessage.value = e.localizedMessage ?: "Failed to load profiles."
                    _isLoading.value = false
                }
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
