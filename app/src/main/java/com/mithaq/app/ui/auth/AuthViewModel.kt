package com.mithaq.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.mithaq.app.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.mithaq.app.data.local.MithaqDatabase
import com.mithaq.app.data.local.CachedUserProfile
import com.mithaq.app.data.local.toCached
import com.mithaq.app.data.local.toDomain

sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    data class Authenticated(val userId: String) : AuthState
    data class Error(val errorMessage: String) : AuthState
    /** New Google Sign-In users who haven't completed the onboarding questionnaire yet. */
    data class NeedsProfileCompletion(val userId: String) : AuthState
}

/**
 * ViewModel managing Authentication flows (Sign In, Sign Up, and Sign Out).
 * Saves user demographic & Islamic preferences to Firestore on registration.
 */
class AuthViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val context: android.content.Context? = null
) : ViewModel() {

    private val db = context?.let { MithaqDatabase.getDatabase(it) }
    private val userDao = db?.userDao()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    val allUsersFlow: Flow<List<UserProfile>> = userDao?.getAllUsersFlow()?.map { list ->
        list.map { it.toDomain() }
    } ?: flowOf(emptyList())

    val pendingVerificationUsers: Flow<List<UserProfile>> = userDao?.getPendingUsersFlow()?.map { list ->
        list.map { it.toDomain() }
    } ?: flowOf(emptyList())

    init {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _authState.value = AuthState.Authenticated(currentUser.uid)
            fetchCurrentUserProfile(currentUser.uid)
        }
        prepopulateMockUsersIfEmpty()
    }

    private fun prepopulateMockUsersIfEmpty() {
        viewModelScope.launch {
            try {
                if (userDao != null) {
                    val existing = userDao.getAllUsers()
                    if (existing.isEmpty()) {
                        val mockUsers = listOf(
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
                                photoAccessApprovedUsers = emptyList(),
                                photoAccessRequests = emptyList(),
                                isWaliAccount = false,
                                wardUid = null,
                                verificationStatus = "VERIFIED",
                                voiceIntroUrl = null,
                                fcmToken = null,
                                isAdmin = false,
                                isPremium = true,
                                subscriptionPlan = "GOLD",
                                questionnaireAnswers = mapOf(
                                    "q1" to "opt1", "q2" to "opt1", "q3" to "opt2", "q4" to "opt4", "q5" to "opt1",
                                    "q6" to "opt1", "q7" to "opt2", "q8" to "opt1", "q9" to "opt2", "q10" to "opt1"
                                )
                            ).toCached(),
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
                                guardianName = null,
                                guardianEmail = null,
                                guardianStatus = null,
                                photoAccessApprovedUsers = emptyList(),
                                photoAccessRequests = emptyList(),
                                isWaliAccount = false,
                                wardUid = null,
                                verificationStatus = "VERIFIED",
                                voiceIntroUrl = null,
                                fcmToken = null,
                                isAdmin = false,
                                isPremium = false,
                                subscriptionPlan = "FREE",
                                questionnaireAnswers = mapOf(
                                    "q1" to "opt1", "q2" to "opt1", "q3" to "opt2", "q4" to "opt1", "q5" to "opt2",
                                    "q6" to "opt2", "q7" to "opt1", "q8" to "opt2", "q9" to "opt1", "q10" to "opt2"
                                )
                            ).toCached(),
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
                                photoAccessApprovedUsers = emptyList(),
                                photoAccessRequests = emptyList(),
                                isWaliAccount = false,
                                wardUid = null,
                                verificationStatus = "PENDING",
                                voiceIntroUrl = null,
                                fcmToken = null,
                                isAdmin = false,
                                isPremium = false,
                                subscriptionPlan = "FREE",
                                questionnaireAnswers = emptyMap()
                            ).toCached(),
                            UserProfile(
                                uid = "mock_wali_001",
                                name = "Wali / ولي أمر",
                                gender = Gender.MALE,
                                age = 52,
                                city = "Cairo",
                                country = "Egypt",
                                imageUrl = "avatar_brother_green",
                                sect = Sect.SUNNI,
                                prayerFrequency = PrayerFrequency.ALWAYS,
                                modestyPreference = ModestyPreference.DOES_NOT_MATTER,
                                relocationWillingness = RelocationWillingness.NO,
                                polygamyAcceptance = false,
                                guardianName = null,
                                guardianEmail = null,
                                guardianStatus = null,
                                photoAccessApprovedUsers = emptyList(),
                                photoAccessRequests = emptyList(),
                                isWaliAccount = true,
                                wardUid = "mock_user_123",
                                verificationStatus = "VERIFIED",
                                voiceIntroUrl = null,
                                fcmToken = null,
                                isAdmin = false,
                                isPremium = false,
                                subscriptionPlan = "FREE",
                                questionnaireAnswers = emptyMap()
                            ).toCached(),
                            UserProfile(
                                uid = "mock_admin_001",
                                name = "Admin / مدير النظام",
                                gender = Gender.MALE,
                                age = 35,
                                city = "Cairo",
                                country = "Egypt",
                                imageUrl = "avatar_brother_green",
                                sect = Sect.SUNNI,
                                prayerFrequency = PrayerFrequency.ALWAYS,
                                modestyPreference = ModestyPreference.DOES_NOT_MATTER,
                                relocationWillingness = RelocationWillingness.OPEN,
                                polygamyAcceptance = false,
                                guardianName = null,
                                guardianEmail = null,
                                guardianStatus = null,
                                photoAccessApprovedUsers = emptyList(),
                                photoAccessRequests = emptyList(),
                                isWaliAccount = false,
                                wardUid = null,
                                verificationStatus = "VERIFIED",
                                voiceIntroUrl = null,
                                fcmToken = null,
                                isAdmin = true,
                                isPremium = true,
                                subscriptionPlan = "GOLD",
                                questionnaireAnswers = emptyMap()
                            ).toCached()
                        )
                        userDao.insertUsers(mockUsers)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun purchasePremiumPlan(planId: String) {
        viewModelScope.launch {
            val current = _currentUserProfile.value ?: return@launch
            val updated = current.copy(isPremium = true, subscriptionPlan = planId)
            _currentUserProfile.value = updated
            userDao?.insertUser(updated.toCached())

            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }

            if (isMock) {
                context?.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)?.edit()?.apply {
                    putBoolean("isPremium", true)
                    putString("subscriptionPlan", planId)
                    apply()
                }
            } else {
                try {
                    firestore.collection("users").document(current.uid).update(
                        mapOf(
                            "isPremium" to true,
                            "subscriptionPlan" to planId
                        )
                    ).await()
                } catch (e: Exception) {
                    // Ignored / offline sync later
                }
            }
        }
    }

    fun adminUpdateVerification(targetUid: String, status: String) {
        viewModelScope.launch {
            val cachedUser = userDao?.getUser(targetUid)
            if (cachedUser != null) {
                userDao.insertUser(cachedUser.copy(verificationStatus = status))
            }

            if (targetUid == _currentUserProfile.value?.uid) {
                _currentUserProfile.value = _currentUserProfile.value?.copy(verificationStatus = status)
            }

            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }

            if (isMock) {
                if (targetUid == _currentUserProfile.value?.uid) {
                    context?.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)?.edit()?.apply {
                        putString("verificationStatus", status)
                        apply()
                    }
                }
            } else {
                try {
                    firestore.collection("users").document(targetUid).update("verificationStatus", status).await()
                } catch (e: Exception) {
                    // Ignored
                }
            }
        }
    }

    fun adminFetchAllUsers() {
        viewModelScope.launch {
            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }
            if (isMock) return@launch

            try {
                val snapshot = firestore.collection("users").get().await()
                val profiles = snapshot.documents.mapNotNull { doc ->
                    try {
                        val uid = doc.id
                        val name = doc.getString("name") ?: ""
                        val genderStr = doc.getString("gender") ?: "FEMALE"
                        val gender = if (genderStr.equals("MALE", ignoreCase = true)) Gender.MALE else Gender.FEMALE
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
                        val additionalImages = doc.get("additionalImages") as? List<String> ?: emptyList()
                        val lastSeen = doc.getLong("lastSeen") ?: 0L

                        UserProfile(
                            uid = uid,
                            name = name,
                            gender = gender,
                            age = age,
                            city = city,
                            country = country,
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
                            questionnaireAnswers = questionnaireAnswers,
                            lastSeen = lastSeen
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                profiles.forEach { userDao?.insertUser(it.toCached()) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun adminUpdateUserPremium(targetUid: String, isPremium: Boolean, plan: String) {
        viewModelScope.launch {
            val cachedUser = userDao?.getUser(targetUid)
            if (cachedUser != null) {
                userDao.insertUser(cachedUser.copy(isPremium = isPremium, subscriptionPlan = plan))
            }
            if (targetUid == _currentUserProfile.value?.uid) {
                _currentUserProfile.value = _currentUserProfile.value?.copy(isPremium = isPremium, subscriptionPlan = plan)
            }
            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }
            if (!isMock) {
                try {
                    firestore.collection("users").document(targetUid)
                        .update("isPremium", isPremium, "subscriptionPlan", plan).await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun adminUpdateUserRole(targetUid: String, isWali: Boolean, isAdmin: Boolean) {
        viewModelScope.launch {
            val cachedUser = userDao?.getUser(targetUid)
            if (cachedUser != null) {
                userDao.insertUser(cachedUser.copy(isWaliAccount = isWali, isAdmin = isAdmin))
            }
            if (targetUid == _currentUserProfile.value?.uid) {
                _currentUserProfile.value = _currentUserProfile.value?.copy(isWaliAccount = isWali, isAdmin = isAdmin)
            }
            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }
            if (!isMock) {
                try {
                    firestore.collection("users").document(targetUid)
                        .update("isWaliAccount", isWali, "isAdmin", isAdmin).await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun adminDeleteUser(targetUid: String) {
        viewModelScope.launch {
            try {
                userDao?.deleteUser(targetUid)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }
            if (!isMock) {
                try {
                    firestore.collection("users").document(targetUid).delete().await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }


    fun saveQuestionnaireAnswers(answers: Map<String, String>) {
        viewModelScope.launch {
            val current = _currentUserProfile.value ?: return@launch
            val updated = current.copy(questionnaireAnswers = answers)
            _currentUserProfile.value = updated
            userDao?.insertUser(updated.toCached())

            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }

            if (isMock) {
                val jsonStr = org.json.JSONObject(answers).toString()
                context?.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)?.edit()?.apply {
                    putString("questionnaireAnswers", jsonStr)
                    apply()
                }
            } else {
                try {
                    firestore.collection("users").document(current.uid).update("questionnaireAnswers", answers).await()
                } catch (e: Exception) {
                    // Ignored
                }
            }
        }
    }

    fun fetchCurrentUserProfile(uid: String) {
        viewModelScope.launch {
            val isOfflineSimulated = context?.getSharedPreferences("mithaq_dev_options", android.content.Context.MODE_PRIVATE)
                ?.getBoolean("is_offline_simulated", false) ?: false

            if (isOfflineSimulated) {
                val cached = userDao?.getUser(uid)
                if (cached != null) {
                    _currentUserProfile.value = cached.toDomain()
                } else {
                    val fallback = UserProfile(
                        uid = uid,
                        name = "Mock Offline User",
                        gender = Gender.MALE,
                        age = 26,
                        city = "Cairo",
                        country = "Egypt",
                        imageUrl = "avatar_brother_green",
                        sect = Sect.SUNNI,
                        prayerFrequency = PrayerFrequency.ALWAYS,
                        modestyPreference = ModestyPreference.HIJAB,
                        relocationWillingness = RelocationWillingness.OPEN,
                        isWaliAccount = false,
                        verificationStatus = "VERIFIED",
                        isAdmin = false,
                        isPremium = false,
                        subscriptionPlan = "FREE",
                        questionnaireAnswers = emptyMap()
                    )
                    _currentUserProfile.value = fallback
                    userDao?.insertUser(fallback.toCached())
                }
                return@launch
            }

            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }

            if (isMock) {
                val prefs = context?.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
                val savedUid = prefs?.getString("uid", null)
                if (savedUid != null) {
                    val name = prefs.getString("name", "Mock User") ?: "Mock User"
                    val genderStr = prefs.getString("gender", "MALE") ?: "MALE"
                    val gender = if (genderStr == "FEMALE") Gender.FEMALE else Gender.MALE
                    val age = prefs.getInt("age", 26)
                    val city = prefs.getString("city", "Cairo") ?: "Cairo"
                    val country = prefs.getString("country", "Egypt") ?: "Egypt"
                    val imageUrl = prefs.getString("imageUrl", "") ?: ""
                    val sectStr = prefs.getString("sect", "SUNNI") ?: "SUNNI"
                    val sect = try { Sect.valueOf(sectStr) } catch(e: Exception) { Sect.SUNNI }
                    val prayerStr = prefs.getString("prayerFrequency", "ALWAYS") ?: "ALWAYS"
                    val prayer = try { PrayerFrequency.valueOf(prayerStr) } catch(e: Exception) { PrayerFrequency.ALWAYS }
                    val modestyStr = prefs.getString("modestyPreference", "HIJAB") ?: "HIJAB"
                    val modesty = try { ModestyPreference.valueOf(modestyStr) } catch(e: Exception) { ModestyPreference.HIJAB }
                    val relocationStr = prefs.getString("relocationWillingness", "OPEN") ?: "OPEN"
                    val relocation = try { RelocationWillingness.valueOf(relocationStr) } catch(e: Exception) { RelocationWillingness.OPEN }
                    
                    val isWaliAccount = prefs.getBoolean("isWaliAccount", false)
                    val wardUid = prefs.getString("wardUid", null)
                    val verificationStatus = prefs.getString("verificationStatus", "NONE") ?: "NONE"
                    val voiceIntroUrl = prefs.getString("voiceIntroUrl", null)
                    val fcmToken = prefs.getString("fcmToken", null)
                    val isAdmin = prefs.getBoolean("isAdmin", false)
                    val isPremium = prefs.getBoolean("isPremium", false)
                    val subscriptionPlan = prefs.getString("subscriptionPlan", "FREE") ?: "FREE"
                    val questionnaireAnswersStr = prefs.getString("questionnaireAnswers", "{}") ?: "{}"
                    val lastSeen = prefs.getLong("lastSeen", 0L)
                    val questionnaireAnswers = try {
                        val obj = org.json.JSONObject(questionnaireAnswersStr)
                        val map = mutableMapOf<String, String>()
                        val keys = obj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            map[key] = obj.getString(key)
                        }
                        map
                    } catch (e: Exception) {
                        emptyMap<String, String>()
                    }
                    val additionalImagesStr = prefs.getString("additionalImages", "[]") ?: "[]"
                    val additionalImages = try {
                        val arr = org.json.JSONArray(additionalImagesStr)
                        val list = mutableListOf<String>()
                        for (i in 0 until arr.length()) {
                            list.add(arr.getString(i))
                        }
                        list
                    } catch (e: Exception) {
                        emptyList<String>()
                    }

                    val username = prefs.getString("username", "") ?: ""
                    val oathChecked = prefs.getBoolean("oathChecked", false)
                    val skinColor = prefs.getString("skinColor", "") ?: ""
                    val healthStatusStr = prefs.getString("healthStatus", "[]") ?: "[]"
                    val healthStatus = try {
                        val arr = org.json.JSONArray(healthStatusStr)
                        val list = mutableListOf<String>()
                        for (i in 0 until arr.length()) { list.add(arr.getString(i)) }
                        list
                    } catch (e: Exception) { emptyList<String>() }
                    val nationality = prefs.getString("nationality", "") ?: ""
                    val educationLevel = prefs.getString("educationLevel", "") ?: ""
                    val jobTitle = prefs.getString("jobTitle", "") ?: ""
                    val incomeLevel = prefs.getString("incomeLevel", "") ?: ""
                    val fastingHabit = prefs.getString("fastingHabit", "") ?: ""
                    val weddingTimeline = prefs.getString("weddingTimeline", "") ?: ""
                    val wifeWorking = prefs.getString("wifeWorking", "") ?: ""
                    val householdExpenses = prefs.getString("householdExpenses", "") ?: ""
                    val aymaView = prefs.getString("aymaView", "") ?: ""
                    val shabkaView = prefs.getString("shabkaView", "") ?: ""
                    val gpsLocationEnabled = prefs.getBoolean("gpsLocationEnabled", false)
                    val blurPictures = prefs.getBoolean("blurPictures", true)
                    val height = prefs.getInt("height", 170)
                    val weight = prefs.getInt("weight", 70)
                    val bodyType = prefs.getString("bodyType", "average") ?: "average"
                    val hairColor = prefs.getString("hairColor", "black") ?: "black"
                    val eyeColor = prefs.getString("eyeColor", "brown") ?: "brown"
                    val ethnicity = prefs.getString("ethnicity", "arab_middle_eastern") ?: "arab_middle_eastern"
                    val maritalStatus = prefs.getString("maritalStatus", "single") ?: "single"
                    val haveChildren = prefs.getString("haveChildren", "no") ?: "no"
                    val languagesSpokenStr = prefs.getString("languagesSpoken", "[]") ?: "[]"
                    val languagesSpoken = try {
                        val arr = org.json.JSONArray(languagesSpokenStr)
                        val list = mutableListOf<String>()
                        for (i in 0 until arr.length()) { list.add(arr.getString(i)) }
                        list
                    } catch (e: Exception) { emptyList<String>() }

                    val profile = UserProfile(
                        uid = savedUid,
                        name = name,
                        gender = gender,
                        age = age,
                        city = city,
                        country = country,
                        imageUrl = imageUrl,
                        additionalImages = additionalImages,
                        sect = sect,
                        prayerFrequency = prayer,
                        modestyPreference = modesty,
                        relocationWillingness = relocation,
                        isWaliAccount = isWaliAccount,
                        wardUid = wardUid,
                        verificationStatus = verificationStatus,
                        voiceIntroUrl = voiceIntroUrl,
                        fcmToken = fcmToken,
                        isAdmin = isAdmin,
                        isPremium = isPremium,
                        subscriptionPlan = subscriptionPlan,
                        questionnaireAnswers = questionnaireAnswers,
                        aboutYourself = prefs.getString("aboutYourself", "") ?: "",
                        idealPartner = prefs.getString("idealPartner", "") ?: "",
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
                        languagesSpoken = languagesSpoken,
                        lastSeen = lastSeen
                    )
                    _currentUserProfile.value = profile
                    userDao?.insertUser(profile.toCached())
                } else {
                    val fallback = UserProfile(
                        uid = uid,
                        name = "Mock User",
                        gender = Gender.MALE,
                        age = 26,
                        city = "Cairo",
                        country = "Egypt",
                        imageUrl = "avatar_brother_green",
                        sect = Sect.SUNNI,
                        prayerFrequency = PrayerFrequency.ALWAYS,
                        modestyPreference = ModestyPreference.HIJAB,
                        relocationWillingness = RelocationWillingness.OPEN,
                        isWaliAccount = false,
                        verificationStatus = "NONE",
                        isAdmin = false,
                        isPremium = false,
                        subscriptionPlan = "FREE",
                        questionnaireAnswers = emptyMap()
                    )
                    _currentUserProfile.value = fallback
                    userDao?.insertUser(fallback.toCached())
                }
                return@launch
            }

            try {
                val doc = firestore.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    val name = doc.getString("name") ?: ""
                    val genderStr = doc.getString("gender") ?: "MALE"
                    val gender = if (genderStr.equals("FEMALE", ignoreCase = true)) Gender.FEMALE else Gender.MALE
                    val age = doc.getLong("age")?.toInt() ?: 25
                    val city = doc.getString("city") ?: ""
                    val country = doc.getString("country") ?: ""
                    val imageUrl = doc.getString("imageUrl") ?: ""
                    val sectStr = doc.getString("sect") ?: "SUNNI"
                    val sect = try { Sect.valueOf(sectStr.uppercase()) } catch(e: Exception) { Sect.SUNNI }
                    val prayerStr = doc.getString("prayerFrequency") ?: "ALWAYS"
                    val prayer = try { PrayerFrequency.valueOf(prayerStr.uppercase()) } catch(e: Exception) { PrayerFrequency.ALWAYS }
                    val modestyStr = doc.getString("modestyPreference") ?: "HIJAB"
                    val modesty = try { ModestyPreference.valueOf(modestyStr.uppercase()) } catch(e: Exception) { ModestyPreference.HIJAB }
                    val relocationStr = doc.getString("relocationWillingness") ?: "OPEN"
                    val relocation = try { RelocationWillingness.valueOf(relocationStr.uppercase()) } catch(e: Exception) { RelocationWillingness.OPEN }
                    
                    val guardianName = doc.getString("guardianName")
                    val guardianEmail = doc.getString("guardianEmail")
                    var guardianStatus = doc.getString("guardianStatus")
                    
                    val photoApproved = doc.get("photoAccessApprovedUsers") as? List<String> ?: emptyList()
                    val photoRequests = doc.get("photoAccessRequests") as? List<String> ?: emptyList()

                    var isWaliAccount = doc.getBoolean("isWaliAccount") ?: false
                    var wardUid = doc.getString("wardUid")

                    val authEmail = auth.currentUser?.email
                    if (authEmail != null && authEmail.isNotEmpty()) {
                        try {
                            val wardsQuery = firestore.collection("users")
                                .whereEqualTo("guardianEmail", authEmail.trim().lowercase())
                                .get()
                                .await()
                            if (!wardsQuery.isEmpty) {
                                val wardDoc = wardsQuery.documents.first()
                                val foundWardUid = wardDoc.id
                                if (!isWaliAccount || wardUid != foundWardUid) {
                                    isWaliAccount = true
                                    wardUid = foundWardUid
                                    firestore.collection("users").document(uid)
                                        .update("isWaliAccount", true, "wardUid", foundWardUid)
                                        .await()
                                }
                                val wardStatus = wardDoc.getString("guardianStatus")
                                if (wardStatus != "VERIFIED") {
                                    firestore.collection("users").document(foundWardUid)
                                        .update("guardianStatus", "VERIFIED")
                                        .await()
                                    guardianStatus = "VERIFIED"
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    val verificationStatus = doc.getString("verificationStatus") ?: "NONE"
                    val voiceIntroUrl = doc.getString("voiceIntroUrl")
                    val fcmToken = doc.getString("fcmToken")
                    val isAdmin = doc.getBoolean("isAdmin") ?: false
                    val isPremium = doc.getBoolean("isPremium") ?: false
                    val subscriptionPlan = doc.getString("subscriptionPlan") ?: "FREE"
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
                    val lastSeen = doc.getLong("lastSeen") ?: 0L

                    val profile = UserProfile(
                        uid = uid,
                        name = name,
                        gender = gender,
                        age = age,
                        city = city,
                        country = country,
                        imageUrl = imageUrl,
                        additionalImages = additionalImages,
                        sect = sect,
                        prayerFrequency = prayer,
                        modestyPreference = modesty,
                        relocationWillingness = relocation,
                        guardianName = guardianName,
                        guardianEmail = guardianEmail,
                        guardianStatus = guardianStatus,
                        photoAccessApprovedUsers = photoApproved,
                        photoAccessRequests = photoRequests,
                        isWaliAccount = isWaliAccount,
                        wardUid = wardUid,
                        verificationStatus = verificationStatus,
                        voiceIntroUrl = voiceIntroUrl,
                        fcmToken = fcmToken,
                        isAdmin = isAdmin,
                        isPremium = isPremium,
                        subscriptionPlan = subscriptionPlan,
                        questionnaireAnswers = questionnaireAnswers,
                        aboutYourself = doc.getString("aboutYourself") ?: "",
                        idealPartner = doc.getString("idealPartner") ?: "",
                        lastSeen = lastSeen,
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
                    _currentUserProfile.value = profile
                    userDao?.insertUser(profile.toCached())

                    try {
                        val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                        firestore.collection("users").document(uid).update("fcmToken", token)
                        _currentUserProfile.value = _currentUserProfile.value?.copy(fcmToken = token)
                    } catch(e: Exception) {
                        // ignored
                    }
                }
            } catch (e: Exception) {
                // Offline fallback from Room Cache
                val cached = userDao?.getUser(uid)
                if (cached != null) {
                    _currentUserProfile.value = cached.toDomain()
                }
            }
        }
    }

    /**
     * Authenticates user using Firebase Auth Email & Password.
     */
    fun signIn(email: String, emailPassed: String, isWali: Boolean = false) {
        if (email.isBlank() || emailPassed.isBlank()) {
            _authState.value = AuthState.Error("Please fill out all credentials.")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                    auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
                } catch (e: Exception) {
                    true
                }

                if (isMock) {
                    kotlinx.coroutines.delay(800)
                    val uid = if (isWali) "mock_wali_123" else "mock_user_123"
                    context?.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)?.edit()?.apply {
                        putString("uid", uid)
                        putBoolean("isWaliAccount", isWali)
                        if (isWali) {
                            putString("name", "Guardian / ولي أمر")
                            putString("wardUid", "mock_user_123")
                        } else {
                            putString("name", "Mock User")
                        }
                        apply()
                    }
                    _authState.value = AuthState.Authenticated(uid)
                    fetchCurrentUserProfile(uid)
                    return@launch
                }

                val result = auth.signInWithEmailAndPassword(email.trim(), emailPassed).await()
                val user = result.user
                if (user != null) {
                    fetchCurrentUserProfile(user.uid)
                    _authState.value = AuthState.Authenticated(user.uid)
                } else {
                    _authState.value = AuthState.Error("Failed to authenticate.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Invalid email or password.")
            }
        }
    }

    /**
     * Creates new user credentials on Firebase Auth and inserts profile preferences into Firestore.
     */
    fun signUp(
        email: String,
        passwordPass: String,
        profile: UserProfile,
        localImageUri: android.net.Uri? = null,
        localVoiceUri: android.net.Uri? = null,
        context: android.content.Context? = null
    ) {
        if (email.isBlank() || passwordPass.isBlank() || profile.name.isBlank()) {
            _authState.value = AuthState.Error("Core credentials cannot be blank.")
            return
        }
        // Server-side age validation (defense-in-depth — UI also validates)
        if (profile.age !in 18..77) {
            _authState.value = AuthState.Error("Age must be between 18 and 77 years.")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                    auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
                } catch (e: Exception) {
                    true
                }

                if (isMock) {
                    kotlinx.coroutines.delay(800)
                    val finalImageUrl = if (localImageUri != null && context != null) {
                        saveImageLocally(context, localImageUri, "mock_user_123")
                    } else {
                        profile.imageUrl
                    }
                    val finalVoiceUrl = if (localVoiceUri != null && context != null) {
                        saveVoiceLocally(context, localVoiceUri, "mock_user_123")
                    } else {
                        null
                    }
                    context?.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)?.edit()?.apply {
                        putString("uid", "mock_user_123")
                        putString("name", profile.name.trim())
                        putString("gender", profile.gender.name)
                        putInt("age", profile.age)
                        putString("city", profile.city.trim())
                        putString("country", profile.country.trim())
                        putString("imageUrl", finalImageUrl)
                        putString("sect", profile.sect.name)
                        putString("prayerFrequency", profile.prayerFrequency.name)
                        putString("modestyPreference", profile.modestyPreference.name)
                        putString("relocationWillingness", profile.relocationWillingness.name)
                        putString("voiceIntroUrl", finalVoiceUrl)
                        putString("verificationStatus", "NONE")
                        putBoolean("isWaliAccount", false)
                        
                        putString("username", profile.username)
                        putBoolean("oathChecked", profile.oathChecked)
                        putString("skinColor", profile.skinColor)
                        putString("healthStatus", org.json.JSONArray(profile.healthStatus).toString())
                        putString("nationality", profile.nationality)
                        putString("educationLevel", profile.educationLevel)
                        putString("jobTitle", profile.jobTitle)
                        putString("incomeLevel", profile.incomeLevel)
                        putString("fastingHabit", profile.fastingHabit)
                        putString("weddingTimeline", profile.weddingTimeline)
                        putString("wifeWorking", profile.wifeWorking)
                        putString("householdExpenses", profile.householdExpenses)
                        putString("aymaView", profile.aymaView)
                        putString("shabkaView", profile.shabkaView)
                        putBoolean("gpsLocationEnabled", profile.gpsLocationEnabled)
                        putBoolean("blurPictures", profile.blurPictures)
                        putInt("height", profile.height)
                        putInt("weight", profile.weight)
                        putString("bodyType", profile.bodyType)
                        putString("hairColor", profile.hairColor)
                        putString("eyeColor", profile.eyeColor)
                        putString("ethnicity", profile.ethnicity)
                        putString("maritalStatus", profile.maritalStatus)
                        putString("haveChildren", profile.haveChildren)
                        putString("languagesSpoken", org.json.JSONArray(profile.languagesSpoken).toString())
                        putString("aboutYourself", profile.aboutYourself)
                        putString("idealPartner", profile.idealPartner)

                        apply()
                    }
                    _currentUserProfile.value = profile.copy(
                        uid = "mock_user_123", 
                        imageUrl = finalImageUrl,
                        voiceIntroUrl = finalVoiceUrl,
                        verificationStatus = "NONE"
                    )
                    _authState.value = AuthState.Authenticated("mock_user_123")
                    return@launch
                }

                // 1. Create User in Firebase Auth
                val authResult = auth.createUserWithEmailAndPassword(email.trim(), passwordPass).await()
                val userId = authResult.user?.uid

                if (userId != null) {
                    // Upload Image to Storage (or fallback to local if upload fails)
                    val finalImageUrl = if (localImageUri != null && context != null) {
                        try {
                            uploadProfileImage(userId, localImageUri, context)
                        } catch (e: Exception) {
                            saveImageLocally(context, localImageUri, userId)
                        }
                    } else {
                        profile.imageUrl
                    }

                    // Upload Voice Intro to Storage
                    val finalVoiceUrl = if (localVoiceUri != null && context != null) {
                        try {
                            uploadVoiceIntro(userId, localVoiceUri, context)
                        } catch (e: Exception) {
                            saveVoiceLocally(context, localVoiceUri, userId)
                        }
                    } else {
                        null
                    }

                    // 2. Save profile to Firestore users database
                    val userProfilePayload = mapOf(
                        "uid" to userId,
                        "name" to profile.name.trim(),
                        "gender" to profile.gender.name,
                        "age" to profile.age,
                        "city" to profile.city.trim(),
                        "country" to profile.country.trim(),
                        "imageUrl" to finalImageUrl,
                        "sect" to profile.sect.name,
                        "prayerFrequency" to profile.prayerFrequency.name,
                        "modestyPreference" to profile.modestyPreference.name,
                        "relocationWillingness" to profile.relocationWillingness.name,
                        "polygamyAcceptance" to profile.polygamyAcceptance,
                        "guardianStatus" to "NONE",
                        "isPremium" to false,
                        "isWaliAccount" to false,
                        "verificationStatus" to "NONE",
                        "voiceIntroUrl" to (finalVoiceUrl ?: ""),
                        
                        "username" to profile.username,
                        "oathChecked" to profile.oathChecked,
                        "skinColor" to profile.skinColor,
                        "healthStatus" to profile.healthStatus,
                        "nationality" to profile.nationality,
                        "educationLevel" to profile.educationLevel,
                        "jobTitle" to profile.jobTitle,
                        "incomeLevel" to profile.incomeLevel,
                        "fastingHabit" to profile.fastingHabit,
                        "weddingTimeline" to profile.weddingTimeline,
                        "wifeWorking" to profile.wifeWorking,
                        "householdExpenses" to profile.householdExpenses,
                        "aymaView" to profile.aymaView,
                        "shabkaView" to profile.shabkaView,
                        "gpsLocationEnabled" to profile.gpsLocationEnabled,
                        "blurPictures" to profile.blurPictures,
                        
                        "height" to profile.height,
                        "weight" to profile.weight,
                        "bodyType" to profile.bodyType,
                        "hairColor" to profile.hairColor,
                        "eyeColor" to profile.eyeColor,
                        "ethnicity" to profile.ethnicity,
                        "maritalStatus" to profile.maritalStatus,
                        "haveChildren" to profile.haveChildren,
                        "languagesSpoken" to profile.languagesSpoken,
                        "aboutYourself" to profile.aboutYourself,
                        "idealPartner" to profile.idealPartner
                    )

                    firestore.collection("users")
                        .document(userId)
                        .set(userProfilePayload)
                        .await()

                    fetchCurrentUserProfile(userId)
                    _authState.value = AuthState.Authenticated(userId)
                } else {
                    _authState.value = AuthState.Error("Could not retrieve created user ID.")
                }

            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to sign up.")
            }
        }
    }

    private fun saveVoiceLocally(context: android.content.Context, voiceUri: android.net.Uri, userId: String): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(voiceUri) ?: return ""
            val directory = java.io.File(context.filesDir, "voices")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val localFile = java.io.File(directory, "$userId.mp4")
            val outputStream = java.io.FileOutputStream(localFile)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            android.net.Uri.fromFile(localFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private suspend fun uploadVoiceIntro(userId: String, voiceUri: android.net.Uri, context: android.content.Context): String {
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
        val voiceRef = storageRef.child("voices/$userId.mp4")
        val inputStream = context.contentResolver.openInputStream(voiceUri) ?: throw java.io.IOException("Unable to open input stream")
        val bytes = inputStream.readBytes()
        inputStream.close()
        voiceRef.putBytes(bytes).await()
        return voiceRef.downloadUrl.await().toString()
    }

    private suspend fun uploadProfileImage(userId: String, imageUri: android.net.Uri, context: android.content.Context): String {
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
        val profileImageRef = storageRef.child("profiles/$userId.jpg")
        val inputStream = context.contentResolver.openInputStream(imageUri) ?: throw java.io.IOException("Unable to open input stream")
        val bytes = inputStream.readBytes()
        inputStream.close()
        profileImageRef.putBytes(bytes).await()
        return profileImageRef.downloadUrl.await().toString()
    }

    private fun saveImageLocally(context: android.content.Context, imageUri: android.net.Uri, userId: String): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri) ?: return ""
            val directory = java.io.File(context.filesDir, "profiles")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val localFile = java.io.File(directory, "$userId.jpg")
            val outputStream = java.io.FileOutputStream(localFile)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            android.net.Uri.fromFile(localFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun signInWithGoogle(idToken: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val isMock = com.mithaq.app.Config.isMock()
                if (isMock) {
                    kotlinx.coroutines.delay(800)
                    _authState.value = AuthState.Authenticated("mock_user_google_123")
                    return@launch
                }

                val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user
                if (user != null) {
                    val doc = firestore.collection("users").document(user.uid).get().await()
                    if (!doc.exists()) {
                        // New Google user: create a minimal skeleton profile.
                        // The user MUST complete the onboarding questionnaire before accessing the app.
                        // Do NOT write hardcoded defaults (age, city, gender) — they must fill these in.
                        val name = user.displayName ?: user.email?.substringBefore("@") ?: "Google User"
                        val userProfilePayload = mapOf(
                            "uid" to user.uid,
                            "name" to name,
                            "imageUrl" to (user.photoUrl?.toString() ?: ""),
                            "isPremium" to false,
                            "verificationStatus" to "NONE",
                            "profileComplete" to false  // Flag: must complete onboarding
                        )
                        firestore.collection("users")
                            .document(user.uid)
                            .set(userProfilePayload)
                            .await()
                        // Signal that profile completion (onboarding) is required
                        _authState.value = AuthState.NeedsProfileCompletion(user.uid)
                        return@launch
                    }
                    fetchCurrentUserProfile(user.uid)
                    _authState.value = AuthState.Authenticated(user.uid)
                } else {
                    _authState.value = AuthState.Error("Failed to authenticate with Google.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to sign in with Google.")
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _currentUserProfile.value = null
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun verifySelfie(imageUri: android.net.Uri, context: android.content.Context, onSuccess: (Boolean) -> Unit) {
        try {
            val isVideo = context.contentResolver.getType(imageUri)?.startsWith("video") == true 
                || imageUri.path?.endsWith(".mp4") == true
                
            val image: InputImage = if (isVideo) {
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(context, imageUri)
                val bitmap = retriever.getFrameAtTime(1000000, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime 
                    ?: throw java.lang.Exception("Could not extract frame from video")
                retriever.release()
                InputImage.fromBitmap(bitmap, 0)
            } else {
                InputImage.fromFilePath(context, imageUri)
            }

            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build()
            val detector = FaceDetection.getClient(options)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    onSuccess(faces.isNotEmpty())
                }
                .addOnFailureListener {
                    onSuccess(false)
                }
        } catch (e: Exception) {
            onSuccess(false)
        }
    }

    fun submitVerification(idCardUri: android.net.Uri, selfieUri: android.net.Uri, context: android.content.Context, onResult: (Boolean, String) -> Unit) {
        val isArabic = java.util.Locale.getDefault().language == "ar"
        verifySelfie(selfieUri, context) { hasFace ->
            if (!hasFace) {
                onResult(false, if (isArabic) "لم يتم اكتشاف وجه في فيديو السيلفي. يرجى تسجيل فيديو سيلفي واضح ومقرب بوجهك." else "No face detected in selfie video. Please record a clear, close-up selfie video of your face.")
                return@verifySelfie
            }
            viewModelScope.launch {
                try {
                    val userId = auth.currentUser?.uid ?: _currentUserProfile.value?.uid
                    if (userId == null) {
                        onResult(false, "المستخدم غير مسجل.")
                        return@launch
                    }
                    val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                        auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
                    } catch (e: Exception) {
                        true
                    }

                    if (isMock) {
                        val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
                        prefs.edit().putString("verificationStatus", "PENDING").apply()
                        _currentUserProfile.value = _currentUserProfile.value?.copy(verificationStatus = "PENDING")
                        com.mithaq.app.notification.MithaqFirebaseMessagingService.showLocalNotification(
                            context,
                            if (isArabic) "ميثاق - تم إرسال طلب التوثيق" else "Mithaq - Verification Request Sent",
                            if (isArabic) 
                                "تم إرسال طلب التوثيق بالفيديو بنجاح إلى مشرفك (ولي أمرك) وإلى الإدمن للمراجعة."
                            else 
                                "Video verification request has been sent to your Wali & Admin for review."
                        )
                        onResult(true, if (isArabic) "تم تقديم طلب التوثيق بنجاح وإرساله إلى مشرفك (ولي أمرك) وإلى الإدمن للمراجعة." else "Verification request submitted successfully and sent to your supervisor (Wali) and the Admin for review.")
                        return@launch
                    }

                    val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
                    val idRef = storageRef.child("verification/$userId/id_card.jpg")
                    
                    val isVideo = context.contentResolver.getType(selfieUri)?.startsWith("video") == true 
                        || selfieUri.path?.endsWith(".mp4") == true
                    val selfieRef = if (isVideo) {
                        storageRef.child("verification/$userId/selfie_video.mp4")
                    } else {
                        storageRef.child("verification/$userId/selfie.jpg")
                    }

                    val idStream = context.contentResolver.openInputStream(idCardUri) ?: throw java.io.IOException("Cannot open ID card")
                    val selfieStream = context.contentResolver.openInputStream(selfieUri) ?: throw java.io.IOException("Cannot open Selfie")

                    idRef.putBytes(idStream.readBytes()).await()
                    
                    val metadata = com.google.firebase.storage.storageMetadata {
                        contentType = if (isVideo) "video/mp4" else "image/jpeg"
                    }
                    selfieRef.putBytes(selfieStream.readBytes(), metadata).await()
                    
                    idStream.close()
                    selfieStream.close()

                    firestore.collection("users").document(userId)
                        .update("verificationStatus", "PENDING").await()

                    _currentUserProfile.value = _currentUserProfile.value?.copy(verificationStatus = "PENDING")
                    com.mithaq.app.notification.MithaqFirebaseMessagingService.showLocalNotification(
                        context,
                        if (isArabic) "ميثاق - تم إرسال طلب التوثيق" else "Mithaq - Verification Request Sent",
                        if (isArabic) 
                            "تم إرسال طلب التوثيق بنجاح إلى مشرفك (ولي أمرك) وإلى الإدمن للمراجعة."
                        else 
                            "Verification request has been sent to your Wali & Admin for review."
                    )
                    onResult(true, if (isArabic) "تم تقديم طلب التوثيق بنجاح وإرساله إلى مشرفك (ولي أمرك) وإلى الإدمن للمراجعة." else "Verification request submitted successfully and sent to your supervisor (Wali) and the Admin for review.")
                } catch (e: Exception) {
                    onResult(false, "حدث خطأ أثناء رفع المستندات: ${e.localizedMessage}")
                }
            }
        }
    }

    fun mockAdminApproveVerification(context: android.content.Context) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: _currentUserProfile.value?.uid ?: return@launch
            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }
            if (isMock) {
                val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
                prefs.edit().putString("verificationStatus", "VERIFIED").apply()
                _currentUserProfile.value = _currentUserProfile.value?.copy(verificationStatus = "VERIFIED")
            } else {
                firestore.collection("users").document(userId)
                    .update("verificationStatus", "VERIFIED").await()
                _currentUserProfile.value = _currentUserProfile.value?.copy(verificationStatus = "VERIFIED")
            }
        }
    }

    fun updateMockRole(isWali: Boolean, isAdmin: Boolean, context: android.content.Context) {
        // SECURITY: Never allow role changes in production. This is strictly a dev/demo tool.
        if (com.mithaq.app.Config.IS_PRODUCTION) {
            android.util.Log.w("AuthViewModel", "updateMockRole called in production — blocked.")
            return
        }
        viewModelScope.launch {
            val current = _currentUserProfile.value ?: return@launch
            val updated = current.copy(isWaliAccount = isWali, isAdmin = isAdmin)
            _currentUserProfile.value = updated
            userDao?.insertUser(updated.toCached())
            val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
            prefs.edit().apply {
                putBoolean("isWaliAccount", isWali)
                putBoolean("isAdmin", isAdmin)
                apply()
            }
            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }
            if (!isMock && current.uid.isNotEmpty()) {
                try {
                    firestore.collection("users").document(current.uid)
                        .update("isWaliAccount", isWali, "isAdmin", isAdmin).await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun updateBio(aboutYourself: String, idealPartner: String, context: android.content.Context) {
        viewModelScope.launch {
            val current = _currentUserProfile.value ?: return@launch
            val updated = current.copy(aboutYourself = aboutYourself, idealPartner = idealPartner)
            _currentUserProfile.value = updated
            userDao?.insertUser(updated.toCached())
            val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("aboutYourself", aboutYourself)
                putString("idealPartner", idealPartner)
                apply()
            }
            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }
            if (!isMock && current.uid.isNotEmpty()) {
                try {
                    firestore.collection("users").document(current.uid)
                        .update("aboutYourself", aboutYourself, "idealPartner", idealPartner).await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun updateBasicInfo(name: String, context: android.content.Context) {
        viewModelScope.launch {
            val current = _currentUserProfile.value ?: return@launch
            val updated = current.copy(name = name)
            _currentUserProfile.value = updated
            userDao?.insertUser(updated.toCached())
            val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("name", name)
                apply()
            }
            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }
            if (!isMock && current.uid.isNotEmpty()) {
                try {
                    firestore.collection("users").document(current.uid)
                        .update("name", name).await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun updateGender(gender: Gender, context: android.content.Context) {
        viewModelScope.launch {
            val current = _currentUserProfile.value ?: return@launch
            val updated = current.copy(gender = gender)
            _currentUserProfile.value = updated
            userDao?.insertUser(updated.toCached())
            val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("gender", gender.name)
                apply()
            }
            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }
            if (!isMock && current.uid.isNotEmpty()) {
                try {
                    firestore.collection("users").document(current.uid)
                        .update("gender", gender.name).await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun completeCoreProfile(
        name: String,
        username: String,
        age: Int,
        gender: Gender,
        country: String,
        city: String,
        oathChecked: Boolean,
        context: android.content.Context,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val current = _currentUserProfile.value ?: UserProfile()
                val updated = current.copy(
                    name = name.trim(),
                    username = username.trim(),
                    age = age,
                    gender = gender,
                    country = country.trim(),
                    city = city.trim(),
                    oathChecked = oathChecked
                )
                _currentUserProfile.value = updated
                userDao?.insertUser(updated.toCached())

                val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putString("name", name.trim())
                    putString("username", username.trim())
                    putInt("age", age)
                    putString("gender", gender.name)
                    putString("country", country.trim())
                    putString("city", city.trim())
                    putBoolean("oathChecked", oathChecked)
                    apply()
                }

                val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                    auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
                } catch (e: Exception) {
                    true
                }

                if (!isMock && updated.uid.isNotEmpty()) {
                    firestore.collection("users").document(updated.uid)
                        .update(
                            mapOf(
                                "name" to name.trim(),
                                "username" to username.trim(),
                                "age" to age,
                                "gender" to gender.name,
                                "country" to country.trim(),
                                "city" to city.trim(),
                                "oathChecked" to oathChecked,
                                "profileComplete" to true
                            )
                        ).await()
                }
                onResult(true, null)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.localizedMessage ?: "Failed to save profile.")
            }
        }
    }

    fun updateAdditionalImages(images: List<String>, context: android.content.Context) {
        viewModelScope.launch {
            val current = _currentUserProfile.value ?: return@launch
            val updated = current.copy(additionalImages = images)
            _currentUserProfile.value = updated
            userDao?.insertUser(updated.toCached())
            val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
            val arr = org.json.JSONArray()
            images.forEach { arr.put(it) }
            prefs.edit().putString("additionalImages", arr.toString()).apply()
            
            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }
            if (!isMock && current.uid.isNotEmpty()) {
                try {
                    firestore.collection("users").document(current.uid)
                        .update("additionalImages", images).await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun deleteCurrentUserAccount(context: android.content.Context, onComplete: () -> Unit) {
        val user = auth.currentUser
        val uid = user?.uid ?: _currentUserProfile.value?.uid
        viewModelScope.launch {
            if (uid != null) {
                // 1. Delete from Firestore
                val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                    auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
                } catch (e: Exception) {
                    true
                }
                if (!isMock) {
                    try {
                        firestore.collection("users").document(uid).delete().await()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                // 2. Delete from local database (Room)
                userDao?.deleteUser(uid)
            }
            // 3. Clear Shared Preferences
            context.getSharedPreferences("mithaq_prefs", android.content.Context.MODE_PRIVATE).edit().clear().apply()
            context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE).edit().clear().apply()
            
            // 4. Delete Firebase Auth User
            if (user != null) {
                try {
                    user.delete().await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 5. Sign Out & Reset State
            signOut()
            onComplete()
        }
    }

    fun updateOnlineStatus() {
        val current = _currentUserProfile.value ?: return
        val now = System.currentTimeMillis()
        val updated = current.copy(lastSeen = now)
        _currentUserProfile.value = updated
        viewModelScope.launch {
            userDao?.insertUser(updated.toCached())
            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }
            if (isMock) {
                context?.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)?.edit()?.apply {
                    putLong("lastSeen", now)
                    apply()
                }
            } else {
                try {
                    firestore.collection("users").document(current.uid).update("lastSeen", now).await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
