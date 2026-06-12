package com.mithaq.app.ui.filter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
                            ),
                            fajrPrayedToday = true, fajrWeeklyCount = 5, fajrMonthlyCount = 20,
                            dhuhrPrayedToday = true, dhuhrWeeklyCount = 6, dhuhrMonthlyCount = 25,
                            asrPrayedToday = true, asrWeeklyCount = 6, asrMonthlyCount = 22,
                            maghribPrayedToday = true, maghribWeeklyCount = 7, maghribMonthlyCount = 28,
                            ishaPrayedToday = true, ishaWeeklyCount = 7, ishaMonthlyCount = 28
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
                            ),
                            fajrPrayedToday = false, fajrWeeklyCount = 1, fajrMonthlyCount = 5,
                            dhuhrPrayedToday = true, dhuhrWeeklyCount = 2, dhuhrMonthlyCount = 10,
                            asrPrayedToday = false, asrWeeklyCount = 2, asrMonthlyCount = 9,
                            maghribPrayedToday = true, maghribWeeklyCount = 3, maghribMonthlyCount = 12,
                            ishaPrayedToday = false, ishaWeeklyCount = 3, ishaMonthlyCount = 11
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

        val isMock = com.mithaq.app.Config.isMock()

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
                            ),
                            fajrPrayedToday = true, fajrWeeklyCount = 5, fajrMonthlyCount = 20,
                            dhuhrPrayedToday = true, dhuhrWeeklyCount = 6, dhuhrMonthlyCount = 25,
                            asrPrayedToday = true, asrWeeklyCount = 6, asrMonthlyCount = 22,
                            maghribPrayedToday = true, maghribWeeklyCount = 7, maghribMonthlyCount = 28,
                            ishaPrayedToday = true, ishaWeeklyCount = 7, ishaMonthlyCount = 28
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
                            ),
                            fajrPrayedToday = false, fajrWeeklyCount = 1, fajrMonthlyCount = 5,
                            dhuhrPrayedToday = true, dhuhrWeeklyCount = 2, dhuhrMonthlyCount = 10,
                            asrPrayedToday = false, asrWeeklyCount = 2, asrMonthlyCount = 9,
                            maghribPrayedToday = true, maghribWeeklyCount = 3, maghribMonthlyCount = 12,
                            ishaPrayedToday = false, ishaWeeklyCount = 3, ishaMonthlyCount = 11
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
                            verificationStatus = "PENDING",
                            fajrPrayedToday = true, fajrWeeklyCount = 4, fajrMonthlyCount = 15,
                            dhuhrPrayedToday = true, dhuhrWeeklyCount = 5, dhuhrMonthlyCount = 18,
                            asrPrayedToday = true, asrWeeklyCount = 5, asrMonthlyCount = 17,
                            maghribPrayedToday = true, maghribWeeklyCount = 6, maghribMonthlyCount = 21,
                            ishaPrayedToday = true, ishaWeeklyCount = 6, ishaMonthlyCount = 20
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
                
                val userGender = (currentUserDoc.getString("gender") ?: "MALE").uppercase()
                val oppositeGender = if (userGender == "MALE") "FEMALE" else "MALE"

                val snapshot = firestore.collection("users")
                    .whereEqualTo("gender", oppositeGender)
                    .orderBy("lastSeen", Query.Direction.DESCENDING)
                    .limit(100)
                    .get()
                    .await()

                val profiles = snapshot.documents.mapNotNull { doc ->
                    try {
                        val uid = doc.id
                        val name = doc.getString("name") ?: ""
                        val genderStr = doc.getString("gender") ?: "FEMALE"
                        val gender = if (genderStr.uppercase() == "MALE") Gender.MALE else Gender.FEMALE
                        val isIncognito = doc.getBoolean("isIncognito") ?: false
                        if (isIncognito) return@mapNotNull null
                        
                        val age = doc.getLong("age")?.toInt() ?: 18
                        val city = doc.getString("city") ?: ""
                        val country = doc.getString("country") ?: ""
                        val timezone = com.mithaq.app.util.CountryUtils.getTimezoneForProfile(
                            country,
                            doc.getString("timezone")
                        )
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
                        val isAdhanEnabled = doc.getBoolean("isAdhanEnabled") ?: false
                        val adhanLocationLat = doc.getDouble("adhanLocationLat") ?: 0.0
                        val adhanLocationLng = doc.getDouble("adhanLocationLng") ?: 0.0
                        val adhanCalculationMethod = doc.getString("adhanCalculationMethod") ?: "MUSLIM_WORLD_LEAGUE"
                        val adhanSoundPattern = doc.getString("adhanSoundPattern") ?: "TAKBEER"
                        val fajrPrayedToday = doc.getBoolean("fajrPrayedToday") ?: false
                        val fajrWeeklyCount = doc.getLong("fajrWeeklyCount")?.toInt() ?: 0
                        val fajrMonthlyCount = doc.getLong("fajrMonthlyCount")?.toInt() ?: 0
                        val dhuhrPrayedToday = doc.getBoolean("dhuhrPrayedToday") ?: false
                        val dhuhrWeeklyCount = doc.getLong("dhuhrWeeklyCount")?.toInt() ?: 0
                        val dhuhrMonthlyCount = doc.getLong("dhuhrMonthlyCount")?.toInt() ?: 0
                        val asrPrayedToday = doc.getBoolean("asrPrayedToday") ?: false
                        val asrWeeklyCount = doc.getLong("asrWeeklyCount")?.toInt() ?: 0
                        val asrMonthlyCount = doc.getLong("asrMonthlyCount")?.toInt() ?: 0
                        val maghribPrayedToday = doc.getBoolean("maghribPrayedToday") ?: false
                        val maghribWeeklyCount = doc.getLong("maghribWeeklyCount")?.toInt() ?: 0
                        val maghribMonthlyCount = doc.getLong("maghribMonthlyCount")?.toInt() ?: 0
                        val ishaPrayedToday = doc.getBoolean("ishaPrayedToday") ?: false
                        val ishaWeeklyCount = doc.getLong("ishaWeeklyCount")?.toInt() ?: 0
                        val ishaMonthlyCount = doc.getLong("ishaMonthlyCount")?.toInt() ?: 0
                        val lastPrayerDate = doc.getLong("lastPrayerDate") ?: 0L
                        val lastWeeklyResetDate = doc.getLong("lastWeeklyResetDate") ?: 0L
                        val lastMonthlyResetDate = doc.getLong("lastMonthlyResetDate") ?: 0L
                        val questionnaireAnswers = doc.get("questionnaireAnswers") as? Map<String, String> ?: emptyMap()
                        val additionalImages = doc.get("additionalImages") as? List<String> ?: emptyList()

                        val username = doc.getString("username") ?: ""
                        val oathChecked = doc.getBoolean("oathChecked") ?: false
                        val skinColor = doc.getString("skinColor") ?: ""
                        val healthStatus = doc.get("healthStatus") as? List<String> ?: emptyList()
                        val nationality = doc.getString("nationality") ?: ""
                        val educationLevel = doc.getString("educationLevel") ?: ""
                        val jobTitle = doc.getString("jobTitle") ?: ""
                        val incomeLevel = doc.getString("incomeLevel") ?: ""
                        val fastingHabit = doc.getString("fastingHabit") ?: ""
                        val weddingTimeline = doc.getString("weddingTimeline") ?: ""
                        val wifeWorking = doc.getString("wifeWorking") ?: ""
                        val householdExpenses = doc.getString("householdExpenses") ?: ""
                        val aymaView = doc.getString("aymaView") ?: ""
                        val shabkaView = doc.getString("shabkaView") ?: ""
                        val gpsLocationEnabled = doc.getBoolean("gpsLocationEnabled") ?: false
                        val blurPictures = doc.getBoolean("blurPictures") ?: true
                        val height = doc.getLong("height")?.toInt() ?: 170
                        val weight = doc.getLong("weight")?.toInt() ?: 70
                        val bodyType = doc.getString("bodyType") ?: "average"
                        val hairColor = doc.getString("hairColor") ?: "black"
                        val eyeColor = doc.getString("eyeColor") ?: "brown"
                        val ethnicity = doc.getString("ethnicity") ?: "arab_middle_eastern"
                        val maritalStatus = doc.getString("maritalStatus") ?: "single"
                        val haveChildren = doc.getString("haveChildren") ?: "no"
                        val languagesSpoken = doc.get("languagesSpoken") as? List<String> ?: emptyList()

                        UserProfile(
                            uid = uid,
                            name = name,
                            gender = gender,
                            age = age,
                            city = city,
                            country = country,
                            timezone = timezone,
                            imageUrl = imageUrl,
                            additionalImages = additionalImages,
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
                            isAdhanEnabled = isAdhanEnabled,
                            adhanLocationLat = adhanLocationLat,
                            adhanLocationLng = adhanLocationLng,
                            adhanCalculationMethod = adhanCalculationMethod,
                            adhanSoundPattern = adhanSoundPattern,
                            fajrPrayedToday = fajrPrayedToday, fajrWeeklyCount = fajrWeeklyCount, fajrMonthlyCount = fajrMonthlyCount,
                            dhuhrPrayedToday = dhuhrPrayedToday, dhuhrWeeklyCount = dhuhrWeeklyCount, dhuhrMonthlyCount = dhuhrMonthlyCount,
                            asrPrayedToday = asrPrayedToday, asrWeeklyCount = asrWeeklyCount, asrMonthlyCount = asrMonthlyCount,
                            maghribPrayedToday = maghribPrayedToday, maghribWeeklyCount = maghribWeeklyCount, maghribMonthlyCount = maghribMonthlyCount,
                            ishaPrayedToday = ishaPrayedToday, ishaWeeklyCount = ishaWeeklyCount, ishaMonthlyCount = ishaMonthlyCount,
                            lastPrayerDate = lastPrayerDate,
                            lastWeeklyResetDate = lastWeeklyResetDate,
                            lastMonthlyResetDate = lastMonthlyResetDate,
                            questionnaireAnswers = questionnaireAnswers,
                            username = username,
                            oathChecked = oathChecked,
                            skinColor = skinColor,
                            healthStatus = healthStatus,
                            nationality = nationality,
                            educationLevel = educationLevel,
                            jobTitle = jobTitle,
                            incomeLevel = incomeLevel,
                            fastingHabit = fastingHabit,
                            weddingTimeline = weddingTimeline,
                            wifeWorking = wifeWorking,
                            householdExpenses = householdExpenses,
                            aymaView = aymaView,
                            shabkaView = shabkaView,
                            gpsLocationEnabled = gpsLocationEnabled,
                            blurPictures = blurPictures,
                            height = height,
                            weight = weight,
                            bodyType = bodyType,
                            hairColor = hairColor,
                            eyeColor = eyeColor,
                            ethnicity = ethnicity,
                            maritalStatus = maritalStatus,
                            haveChildren = haveChildren,
                            languagesSpoken = languagesSpoken
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
    fun isCompatible(user: UserProfile, criteria: FilterCriteria = _filterCriteria.value): Boolean {
        // 1. Age Range Check
        if (user.age !in criteria.minAge..criteria.maxAge) return false
        
        // 2. Sect Check
        if (criteria.sect != null && user.sect != criteria.sect) return false
        
        // 3. Prayer Frequency Check
        if (criteria.prayerFrequencies.isNotEmpty() && user.prayerFrequency !in criteria.prayerFrequencies) return false
        
        // 4. Modesty Preference (Hijab/Niqab) Check
        if (criteria.modestyPreferences.isNotEmpty() && user.modestyPreference !in criteria.modestyPreferences) return false
        
        // 5. Relocation Willingness Check
        if (criteria.relocationWillingness.isNotEmpty() && user.relocationWillingness !in criteria.relocationWillingness) return false
        
        // 6. Polygamy Acceptance Check
        if (criteria.polygamyAcceptance != null && user.polygamyAcceptance != criteria.polygamyAcceptance) return false

        // 7. Country Check
        if (criteria.country.isNotBlank() && !user.country.contains(criteria.country, ignoreCase = true)) return false

        // 8. City Check
        if (criteria.city.isNotBlank() && !user.city.contains(criteria.city, ignoreCase = true)) return false
        
        // 9. Height Check
        if (user.height !in criteria.minHeight..criteria.maxHeight) return false
        
        // 10. Marital Status Check
        if (criteria.maritalStatuses.isNotEmpty() && user.maritalStatus !in criteria.maritalStatuses) return false
        
        // 11. Have Children Check
        if (criteria.haveChildren.isNotEmpty() && user.haveChildren !in criteria.haveChildren) return false
        
        // 12. Religious Values Check
        if (criteria.religiousValues.isNotEmpty() && user.religiousValues !in criteria.religiousValues) return false

        // 13. Distance Check
        if (criteria.maxDistanceKm != null && criteria.currentLat != null && criteria.currentLng != null) {
            val dist = calculateDistanceKm(criteria.currentLat, criteria.currentLng, user.adhanLocationLat, user.adhanLocationLng)
            if (dist > criteria.maxDistanceKm) return false
        }

        return true
    }


    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Radius of the earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }

    private fun applyLocalFilters() {
        val filtered = allUsersCache.filter { isCompatible(it) }

        // Profile Boost: apply a bounded re-rank so premium members float up
        // while preserving the original relevance/ordering.
        _searchResults.value = filtered.mapIndexed { index, user ->
            val boost = if (user.isPremium) {
                when (user.subscriptionPlan.uppercase()) {
                    "PLATINUM" -> 15
                    "GOLD" -> 8
                    else -> 5
                }
            } else 0
            
            // Lower score ranks higher. We subtract the boost from their original index.
            Triple(user, index - boost, index)
        }.sortedWith(
            compareBy<Triple<UserProfile, Int, Int>> { it.second }.thenBy { it.third }
        ).map { it.first }
    }

    fun resetFilters() {
        _filterCriteria.value = FilterCriteria()
        applyLocalFilters()
    }
}
