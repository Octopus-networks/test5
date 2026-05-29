package com.mithaq.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ActionCodeSettings
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
import com.mithaq.app.service.BackendFunctions

sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    data class Authenticated(val userId: String) : AuthState
    data class EmailVerificationRequired(val userId: String, val email: String?) : AuthState
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
    private val verificationEmailCooldownMs = 60_000L
    private var lastVerificationEmailSentAtMs = 0L


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

    val currentUserEmail: String?
        get() = auth.currentUser?.email

    init {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            if (currentUser.isEmailVerified) {
                _authState.value = AuthState.Authenticated(currentUser.uid)
                fetchCurrentUserProfile(currentUser.uid)
            } else {
                _authState.value = AuthState.EmailVerificationRequired(currentUser.uid, currentUser.email)
            }
        }
        prepopulateMockUsersIfEmpty()
    }

    private fun prepopulateMockUsersIfEmpty() {
        if (com.mithaq.app.Config.IS_PRODUCTION) return
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
                                timezone = "Africa/Cairo",
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
                                timezone = "Asia/Riyadh",
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
                                timezone = "Asia/Dubai",
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
                                timezone = "Africa/Cairo",
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
                                timezone = "Africa/Cairo",
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

    private fun emailVerificationSettings(): ActionCodeSettings {
        return ActionCodeSettings.newBuilder()
            .setUrl("https://mithaq.app/verify-email")
            .setHandleCodeInApp(true)
            .setAndroidPackageName("com.mithaq.app", true, null)
            .build()
    }

    suspend fun sendVerificationEmailToCurrentUser() {
        val user = auth.currentUser ?: throw IllegalStateException("No signed-in user.")
        user.sendEmailVerification(emailVerificationSettings()).await()
        lastVerificationEmailSentAtMs = System.currentTimeMillis()
    }

    fun resendVerificationEmail(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                val elapsedMs = System.currentTimeMillis() - lastVerificationEmailSentAtMs
                if (elapsedMs in 0 until verificationEmailCooldownMs) {
                    val remainingSeconds = ((verificationEmailCooldownMs - elapsedMs) / 1000L).coerceAtLeast(1L)
                    onResult(false, "Please wait $remainingSeconds seconds before resending the email.")
                    return@launch
                }
                sendVerificationEmailToCurrentUser()
                onResult(true, "Verification email sent.")
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Could not resend verification email. Please check your connection and try again.")
            }
        }
    }

    fun reloadAndCheckEmailVerification(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                val user = auth.currentUser
                if (user == null) {
                    _authState.value = AuthState.Idle
                    onResult(false, "Please sign in again.")
                    return@launch
                }

                user.reload().await()
                val reloadedUser = auth.currentUser
                if (reloadedUser?.isEmailVerified == true) {
                    _authState.value = AuthState.Authenticated(reloadedUser.uid)
                    fetchCurrentUserProfile(reloadedUser.uid)
                    onResult(true, "Email verified.")
                } else {
                    _authState.value = AuthState.EmailVerificationRequired(user.uid, user.email)
                    onResult(false, "Your email is not verified yet. Please open the activation link first.")
                }
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Could not check email verification. Please check your connection and try again.")
            }
        }
    }

    fun purchasePremiumPlan(planId: String) {
        viewModelScope.launch {
            val current = _currentUserProfile.value ?: return@launch
            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }

            if (isMock) {
                val updated = current.copy(isPremium = true, subscriptionPlan = planId)
                _currentUserProfile.value = updated
                userDao?.insertUser(updated.toCached())
                context?.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)?.edit()?.apply {
                    putBoolean("isPremium", true)
                    putString("subscriptionPlan", planId)
                    apply()
                }
            } else {
                _authState.value = AuthState.Error(
                    "Premium purchases must be verified by the payment backend before membership is upgraded."
                )
            }
        }
    }

    fun adminUpdateVerification(targetUid: String, status: String) {
        viewModelScope.launch {
            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }

            if (isMock) {
                val cachedUser = userDao?.getUser(targetUid)
                if (cachedUser != null) {
                    userDao.insertUser(cachedUser.copy(verificationStatus = status))
                }
                if (targetUid == _currentUserProfile.value?.uid) {
                    _currentUserProfile.value = _currentUserProfile.value?.copy(verificationStatus = status)
                    context?.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)?.edit()?.apply {
                        putString("verificationStatus", status)
                        apply()
                    }
                }
            } else {
                try {
                    BackendFunctions.setVerificationStatus(targetUid, status)
                    val cachedUser = userDao?.getUser(targetUid)
                    if (cachedUser != null) {
                        userDao.insertUser(cachedUser.copy(verificationStatus = status))
                    }
                    if (targetUid == _currentUserProfile.value?.uid) {
                        _currentUserProfile.value = _currentUserProfile.value?.copy(verificationStatus = status)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
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
                        val lastSeen = doc.getLong("lastSeen") ?: 0L
                        val timezone = com.mithaq.app.util.CountryUtils.getTimezoneForProfile(
                            country,
                            doc.getString("timezone")
                        )
                        val currentStreakDays = doc.getLong("currentStreakDays")?.toInt() ?: 0

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
                            lastSeen = lastSeen,
                            timezone = timezone,
                            currentStreakDays = currentStreakDays
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
            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }
            try {
                if (!isMock) {
                    BackendFunctions.setUserPremium(targetUid, isPremium, plan)
                }
                val cachedUser = userDao?.getUser(targetUid)
                if (cachedUser != null) {
                    userDao.insertUser(cachedUser.copy(isPremium = isPremium, subscriptionPlan = plan))
                }
                if (targetUid == _currentUserProfile.value?.uid) {
                    _currentUserProfile.value = _currentUserProfile.value?.copy(isPremium = isPremium, subscriptionPlan = plan)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun adminUpdateUserRole(targetUid: String, isWali: Boolean, isAdmin: Boolean) {
        viewModelScope.launch {
            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }
            try {
                if (!isMock) {
                    BackendFunctions.setUserRole(targetUid, isWali, isAdmin)
                }
                val cachedUser = userDao?.getUser(targetUid)
                if (cachedUser != null) {
                    userDao.insertUser(cachedUser.copy(isWaliAccount = isWali, isAdmin = isAdmin))
                }
                if (targetUid == _currentUserProfile.value?.uid) {
                    _currentUserProfile.value = _currentUserProfile.value?.copy(isWaliAccount = isWali, isAdmin = isAdmin)
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
                    BackendFunctions.deleteUserProfile(targetUid)
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

    private fun calculateSeriousnessScore(profile: UserProfile): Int {
        var score = 0
        
        // 1. Profile Completion (up to 30 pts)
        var completionPoints = 0
        if (profile.aboutYourself.length > 20) completionPoints += 10
        if (profile.idealPartner.length > 20) completionPoints += 10
        if (profile.educationLevel.isNotEmpty()) completionPoints += 5
        if (profile.jobTitle.isNotEmpty()) completionPoints += 5
        score += completionPoints

        // 2. Identity Verification (30 pts)
        if (profile.verificationStatus == "VERIFIED") {
            score += 30
        } else if (profile.verificationStatus == "PENDING") {
            score += 15
        }

        // 3. Wali Link (20 pts)
        if (profile.guardianStatus == "VERIFIED") {
            score += 20
        } else if (profile.guardianStatus == "PENDING") {
            score += 10
        }

        // 4. Premium Status (10 pts)
        if (profile.isPremium) {
            score += 10
        }

        // 5. Activity (up to 10 pts)
        if (profile.lastSeen > 0) {
            val daysSinceLastSeen = (System.currentTimeMillis() - profile.lastSeen) / (1000 * 60 * 60 * 24)
            if (daysSinceLastSeen < 3) score += 10
            else if (daysSinceLastSeen < 7) score += 5
        }

        return score.coerceIn(0, 100)
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
                val cached = userDao?.getUser(uid)
                if (cached != null) {
                    _currentUserProfile.value = cached.toDomain()
                    context?.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)?.edit()?.apply {
                        putString("uid", cached.uid)
                        putString("name", cached.name)
                        putString("gender", cached.gender)
                        putInt("age", cached.age)
                        putString("city", cached.city)
                        putString("country", cached.country)
                        putString("imageUrl", cached.imageUrl)
                        putString("sect", cached.sect)
                        putString("prayerFrequency", cached.prayerFrequency)
                        putString("modestyPreference", cached.modestyPreference)
                        putString("relocationWillingness", cached.relocationWillingness)
                        putBoolean("isWaliAccount", cached.isWaliAccount)
                        putString("wardUid", cached.wardUid)
                        putString("verificationStatus", cached.verificationStatus)
                        putString("voiceIntroUrl", cached.voiceIntroUrl)
                        putString("fcmToken", cached.fcmToken)
                        putBoolean("isAdmin", cached.isAdmin)
                        putBoolean("isPremium", cached.isPremium)
                        putString("subscriptionPlan", cached.subscriptionPlan)
                        
                        val qAnswersObj = org.json.JSONObject()
                        cached.questionnaireAnswers.forEach { (k, v) -> qAnswersObj.put(k, v) }
                        putString("questionnaireAnswers", qAnswersObj.toString())
                        
                        val addImagesArr = org.json.JSONArray(cached.additionalImages)
                        putString("additionalImages", addImagesArr.toString())
                        
                        val approvedArr = org.json.JSONArray(cached.photoAccessApprovedUsers)
                        putString("photoAccessApprovedUsers", approvedArr.toString())
                        
                        val requestsArr = org.json.JSONArray(cached.photoAccessRequests)
                        putString("photoAccessRequests", requestsArr.toString())
                        
                        putString("aboutYourself", cached.aboutYourself)
                        putString("idealPartner", cached.idealPartner)
                        putString("username", cached.username)
                        putBoolean("oathChecked", cached.oathChecked)
                        putString("skinColor", cached.skinColor)
                        
                        val healthArr = org.json.JSONArray(cached.healthStatus)
                        putString("healthStatus", healthArr.toString())
                        
                        putString("nationality", cached.nationality)
                        putString("educationLevel", cached.educationLevel)
                        putString("jobTitle", cached.jobTitle)
                        putString("incomeLevel", cached.incomeLevel)
                        putString("fastingHabit", cached.fastingHabit)
                        putString("weddingTimeline", cached.weddingTimeline)
                        putString("wifeWorking", cached.wifeWorking)
                        putString("householdExpenses", cached.householdExpenses)
                        putString("aymaView", cached.aymaView)
                        putString("shabkaView", cached.shabkaView)
                        putBoolean("gpsLocationEnabled", cached.gpsLocationEnabled)
                        putBoolean("blurPictures", cached.blurPictures)
                        putInt("height", cached.height)
                        putInt("weight", cached.weight)
                        putString("bodyType", cached.bodyType)
                        putString("hairColor", cached.hairColor)
                        putString("eyeColor", cached.eyeColor)
                        putString("ethnicity", cached.ethnicity)
                        putString("maritalStatus", cached.maritalStatus)
                        putString("haveChildren", cached.haveChildren)
                        
                        val langsArr = org.json.JSONArray(cached.languagesSpoken)
                        putString("languagesSpoken", langsArr.toString())
                        
                        apply()
                    }
                    return@launch
                }
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
                    val isAdhanEnabled = prefs.getBoolean("isAdhanEnabled", false)
                    val adhanLocationLat = prefs.getFloat("adhanLocationLat", 0.0f).toDouble()
                    val adhanLocationLng = prefs.getFloat("adhanLocationLng", 0.0f).toDouble()
                    
                    val fajrPrayedToday = prefs.getBoolean("fajrPrayedToday", false)
                    val fajrWeeklyCount = prefs.getInt("fajrWeeklyCount", 0)
                    val fajrMonthlyCount = prefs.getInt("fajrMonthlyCount", 0)
                    val dhuhrPrayedToday = prefs.getBoolean("dhuhrPrayedToday", false)
                    val dhuhrWeeklyCount = prefs.getInt("dhuhrWeeklyCount", 0)
                    val dhuhrMonthlyCount = prefs.getInt("dhuhrMonthlyCount", 0)
                    val asrPrayedToday = prefs.getBoolean("asrPrayedToday", false)
                    val asrWeeklyCount = prefs.getInt("asrWeeklyCount", 0)
                    val asrMonthlyCount = prefs.getInt("asrMonthlyCount", 0)
                    val maghribPrayedToday = prefs.getBoolean("maghribPrayedToday", false)
                    val maghribWeeklyCount = prefs.getInt("maghribWeeklyCount", 0)
                    val maghribMonthlyCount = prefs.getInt("maghribMonthlyCount", 0)
                    val ishaPrayedToday = prefs.getBoolean("ishaPrayedToday", false)
                    val ishaWeeklyCount = prefs.getInt("ishaWeeklyCount", 0)
                    val ishaMonthlyCount = prefs.getInt("ishaMonthlyCount", 0)
                    
                    val lastPrayerDate = prefs.getLong("lastPrayerDate", 0L)
                    val lastWeeklyResetDate = prefs.getLong("lastWeeklyResetDate", 0L)
                    val lastMonthlyResetDate = prefs.getLong("lastMonthlyResetDate", 0L)
                    val questionnaireAnswersStr = prefs.getString("questionnaireAnswers", "{}") ?: "{}"
                    val lastSeen = prefs.getLong("lastSeen", 0L)
                    val timezone = com.mithaq.app.util.CountryUtils.getTimezoneForProfile(
                        country,
                        prefs.getString("timezone", null)
                    )
                    val currentStreakDays = prefs.getInt("currentStreakDays", 0)
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

                    val approvedStr = prefs.getString("photoAccessApprovedUsers", "[]") ?: "[]"
                    val approvedList = mutableListOf<String>()
                    try {
                        val arr = org.json.JSONArray(approvedStr)
                        for (i in 0 until arr.length()) { approvedList.add(arr.getString(i)) }
                    } catch(e: Exception) {}

                    val requestsStr = prefs.getString("photoAccessRequests", "[]") ?: "[]"
                    val requestsList = mutableListOf<String>()
                    try {
                        val arr = org.json.JSONArray(requestsStr)
                        for (i in 0 until arr.length()) { requestsList.add(arr.getString(i)) }
                    } catch(e: Exception) {}

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
                            isAdhanEnabled = isAdhanEnabled,
                            adhanLocationLat = adhanLocationLat,
                            adhanLocationLng = adhanLocationLng,
                            adhanCalculationMethod = "MUSLIM_WORLD_LEAGUE",
                            adhanSoundPattern = "TAKBEER",
                            fajrPrayedToday = fajrPrayedToday, fajrWeeklyCount = fajrWeeklyCount, fajrMonthlyCount = fajrMonthlyCount,
                            dhuhrPrayedToday = dhuhrPrayedToday, dhuhrWeeklyCount = dhuhrWeeklyCount, dhuhrMonthlyCount = dhuhrMonthlyCount,
                            asrPrayedToday = asrPrayedToday, asrWeeklyCount = asrWeeklyCount, asrMonthlyCount = asrMonthlyCount,
                            maghribPrayedToday = maghribPrayedToday, maghribWeeklyCount = maghribWeeklyCount, maghribMonthlyCount = maghribMonthlyCount,
                            ishaPrayedToday = ishaPrayedToday, ishaWeeklyCount = ishaWeeklyCount, ishaMonthlyCount = ishaMonthlyCount,
                            lastPrayerDate = lastPrayerDate,
                            lastWeeklyResetDate = lastWeeklyResetDate,
                            lastMonthlyResetDate = lastMonthlyResetDate,
                        questionnaireAnswers = questionnaireAnswers,
                        photoAccessApprovedUsers = approvedList,
                        photoAccessRequests = requestsList,
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
                        lastSeen = lastSeen,
                        timezone = timezone,
                        currentStreakDays = currentStreakDays
                    )
                    val finalProfile = profile.copy(seriousnessScore = calculateSeriousnessScore(profile))
                    _currentUserProfile.value = finalProfile
                    userDao?.insertUser(finalProfile.toCached())
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
                    val lastSeen = doc.getLong("lastSeen") ?: 0L
                    val timezone = com.mithaq.app.util.CountryUtils.getTimezoneForProfile(
                        country,
                        doc.getString("timezone")
                    )
                    val currentStreakDays = doc.getLong("currentStreakDays")?.toInt() ?: 0

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
                            isAdhanEnabled = isAdhanEnabled,
                            adhanLocationLat = adhanLocationLat,
                            adhanLocationLng = adhanLocationLng,
                            adhanCalculationMethod = "MUSLIM_WORLD_LEAGUE",
                            adhanSoundPattern = "TAKBEER",
                            fajrPrayedToday = fajrPrayedToday, fajrWeeklyCount = fajrWeeklyCount, fajrMonthlyCount = fajrMonthlyCount,
                            dhuhrPrayedToday = dhuhrPrayedToday, dhuhrWeeklyCount = dhuhrWeeklyCount, dhuhrMonthlyCount = dhuhrMonthlyCount,
                            asrPrayedToday = asrPrayedToday, asrWeeklyCount = asrWeeklyCount, asrMonthlyCount = asrMonthlyCount,
                            maghribPrayedToday = maghribPrayedToday, maghribWeeklyCount = maghribWeeklyCount, maghribMonthlyCount = maghribMonthlyCount,
                            ishaPrayedToday = ishaPrayedToday, ishaWeeklyCount = ishaWeeklyCount, ishaMonthlyCount = ishaMonthlyCount,
                            lastPrayerDate = lastPrayerDate,
                            lastWeeklyResetDate = lastWeeklyResetDate,
                            lastMonthlyResetDate = lastMonthlyResetDate,
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
                        languagesSpoken = languagesSpoken,
                        timezone = timezone,
                        currentStreakDays = currentStreakDays
                    )
                    val finalProfile = profile.copy(seriousnessScore = calculateSeriousnessScore(profile))
                    _currentUserProfile.value = finalProfile
                    userDao?.insertUser(finalProfile.toCached())
                    context?.getSharedPreferences("mithaq_prefs", android.content.Context.MODE_PRIVATE)?.edit()?.apply {
                        putString("uid", finalProfile.uid)
                        putString("name", finalProfile.name)
                        apply()
                    }

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
                    // Determine UID based on email for multi-identity testing
                    val emailPrefix = email.substringBefore("@").lowercase()
                    val uid = if (isWali) "mock_wali_$emailPrefix" else {
                        when (emailPrefix) {
                            "fatima" -> "mock_user_2"
                            "ahmad" -> "mock_user_3"
                            "sarah" -> "mock_user_4"
                            else -> "mock_user_$emailPrefix"
                        }
                    }
                    
                    context?.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)?.edit()?.apply {
                        putString("uid", uid)
                        putBoolean("isWaliAccount", isWali)
                        if (isWali) {
                            putString("name", "Guardian / ولي أمر ($emailPrefix)")
                            putString("wardUid", "mock_user_123")
                        } else {
                            putString("name", emailPrefix.replaceFirstChar { it.uppercase() })
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
                    user.reload().await()
                    val reloadedUser = auth.currentUser ?: user
                    if (reloadedUser.isEmailVerified) {
                        fetchCurrentUserProfile(reloadedUser.uid)
                        _authState.value = AuthState.Authenticated(reloadedUser.uid)
                    } else {
                        _authState.value = AuthState.EmailVerificationRequired(reloadedUser.uid, reloadedUser.email)
                    }
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
                val derivedTimezone = com.mithaq.app.util.CountryUtils.getTimezoneForProfile(
                    profile.country.trim(),
                    profile.timezone
                )
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
                        putString("timezone", derivedTimezone)
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
                        verificationStatus = "NONE",
                        timezone = derivedTimezone
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

                    val guardianName = profile.guardianName?.trim()?.takeIf { it.isNotBlank() }
                    val guardianEmail = profile.guardianEmail?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
                    val hasGuardianInvite = guardianName != null && guardianEmail != null

                    // 2. Save profile to Firestore users database
                    val userProfilePayload = mapOf(
                        "uid" to userId,
                        "name" to profile.name.trim(),
                        "gender" to profile.gender.name,
                        "age" to profile.age,
                        "city" to profile.city.trim(),
                        "country" to profile.country.trim(),
                        "timezone" to derivedTimezone,
                        "imageUrl" to finalImageUrl,
                        "sect" to profile.sect.name,
                        "prayerFrequency" to profile.prayerFrequency.name,
                        "modestyPreference" to profile.modestyPreference.name,
                        "relocationWillingness" to profile.relocationWillingness.name,
                        "polygamyAcceptance" to profile.polygamyAcceptance,
                        "guardianName" to guardianName,
                        "guardianEmail" to guardianEmail,
                        "guardianStatus" to if (hasGuardianInvite) "PENDING" else "NONE",
                        "isPremium" to false,
                        "subscriptionPlan" to "FREE",
                        "premiumExpiry" to 0L,
                        "isWaliAccount" to false,
                        "verificationStatus" to "NONE",
                        "voiceIntroUrl" to (finalVoiceUrl ?: ""),
                        "photoAccessApprovedUsers" to emptyList<String>(),
                        "photoAccessRequests" to emptyList<String>(),
                        
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

                    sendVerificationEmailToCurrentUser()
                    _authState.value = AuthState.EmailVerificationRequired(userId, authResult.user?.email)
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
                    user.reload().await()
                    val reloadedUser = auth.currentUser ?: user
                    if (!reloadedUser.isEmailVerified) {
                        _authState.value = AuthState.EmailVerificationRequired(reloadedUser.uid, reloadedUser.email)
                        return@launch
                    }

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
                            "subscriptionPlan" to "FREE",
                            "premiumExpiry" to 0L,
                            "isWaliAccount" to false,
                            "verificationStatus" to "NONE",
                            "photoAccessApprovedUsers" to emptyList<String>(),
                            "photoAccessRequests" to emptyList<String>(),
                            "profileComplete" to false  // Flag: must complete onboarding
                        )
                        firestore.collection("users")
                            .document(user.uid)
                            .set(userProfilePayload)
                            .await()
                        // Signal that profile completion (onboarding) is required
                        _authState.value = AuthState.Authenticated(user.uid)
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

    fun updateGoogleUserProfile(
        userId: String,
        name: String,
        username: String,
        age: Int,
        gender: Gender,
        country: String,
        city: String,
        oathChecked: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val trimmedCountry = country.trim()
                val trimmedCity = city.trim()
                val trimmedUsername = username.trim()
                val trimmedName = name.trim()
                val derivedTimezone = com.mithaq.app.util.CountryUtils.getTimezoneForCountry(trimmedCountry)
                db.collection("users").document(userId).set(
                    mapOf(
                        "name" to trimmedName,
                        "username" to trimmedUsername,
                        "age" to age,
                        "gender" to gender.name,
                        "country" to trimmedCountry,
                        "city" to trimmedCity,
                        "oathChecked" to oathChecked,
                        "timezone" to derivedTimezone,
                        "profileComplete" to true
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )

                val updated = (_currentUserProfile.value ?: UserProfile(uid = userId)).copy(
                    uid = userId,
                    name = trimmedName,
                    username = trimmedUsername,
                    age = age,
                    gender = gender,
                    country = trimmedCountry,
                    city = trimmedCity,
                    oathChecked = oathChecked,
                    timezone = derivedTimezone
                )
                _currentUserProfile.value = updated
                userDao?.insertUser(updated.toCached())

                fetchCurrentUserProfile(userId)
                _authState.value = AuthState.Authenticated(userId)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to update profile")
            }
        }
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
                try {
                    BackendFunctions.setVerificationStatus(userId, "VERIFIED")
                    _currentUserProfile.value = _currentUserProfile.value?.copy(verificationStatus = "VERIFIED")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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
                val derivedTimezone = com.mithaq.app.util.CountryUtils.getTimezoneForCountry(country.trim())
                val updated = current.copy(
                    name = name.trim(),
                    username = username.trim(),
                    age = age,
                    gender = gender,
                    country = country.trim(),
                    city = city.trim(),
                    oathChecked = oathChecked,
                    timezone = derivedTimezone
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
                    putString("timezone", derivedTimezone)
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
                                "timezone" to derivedTimezone,
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

    fun updateGpsLocation(
        latitude: Double,
        longitude: Double,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val current = _currentUserProfile.value
                if (current == null) {
                    onResult(false, "No active user profile.")
                    return@launch
                }

                val derivedTimezone = com.mithaq.app.util.CountryUtils.getTimezoneForProfile(
                    current.country,
                    current.timezone
                )
                val updated = current.copy(
                    gpsLocationEnabled = true,
                    adhanLocationLat = latitude,
                    adhanLocationLng = longitude,
                    timezone = derivedTimezone
                )
                _currentUserProfile.value = updated
                userDao?.insertUser(updated.toCached())

                val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                    auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
                } catch (e: Exception) {
                    true
                }

                if (isMock) {
                    context?.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)?.edit()?.apply {
                        putBoolean("gpsLocationEnabled", true)
                        putFloat("adhanLocationLat", latitude.toFloat())
                        putFloat("adhanLocationLng", longitude.toFloat())
                        putString("timezone", derivedTimezone)
                        apply()
                    }
                } else if (current.uid.isNotEmpty()) {
                    firestore.collection("users").document(current.uid).update(
                        mapOf(
                            "gpsLocationEnabled" to true,
                            "adhanLocationLat" to latitude,
                            "adhanLocationLng" to longitude,
                            "timezone" to derivedTimezone
                        )
                    ).await()
                }

                onResult(true, null)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.localizedMessage ?: "Failed to update GPS location.")
            }
        }
    }

    fun updatePrayerStats(profile: com.mithaq.app.model.UserProfile) {
        viewModelScope.launch {
            val current = _currentUserProfile.value ?: return@launch
            val updated = current.copy(
                isAdhanEnabled = profile.isAdhanEnabled,
                adhanLocationLat = profile.adhanLocationLat,
                adhanLocationLng = profile.adhanLocationLng,
                fajrPrayedToday = profile.fajrPrayedToday,
                dhuhrPrayedToday = profile.dhuhrPrayedToday,
                asrPrayedToday = profile.asrPrayedToday,
                maghribPrayedToday = profile.maghribPrayedToday,
                ishaPrayedToday = profile.ishaPrayedToday,
                fajrWeeklyCount = profile.fajrWeeklyCount,
                dhuhrWeeklyCount = profile.dhuhrWeeklyCount,
                asrWeeklyCount = profile.asrWeeklyCount,
                maghribWeeklyCount = profile.maghribWeeklyCount,
                ishaWeeklyCount = profile.ishaWeeklyCount,
                fajrMonthlyCount = profile.fajrMonthlyCount,
                dhuhrMonthlyCount = profile.dhuhrMonthlyCount,
                asrMonthlyCount = profile.asrMonthlyCount,
                maghribMonthlyCount = profile.maghribMonthlyCount,
                ishaMonthlyCount = profile.ishaMonthlyCount,
                lastPrayerDate = profile.lastPrayerDate,
                lastWeeklyResetDate = profile.lastWeeklyResetDate,
                lastMonthlyResetDate = profile.lastMonthlyResetDate,
                adhanCalculationMethod = profile.adhanCalculationMethod,
                adhanSoundPattern = profile.adhanSoundPattern
            )
            _currentUserProfile.value = updated
            userDao?.insertUser(updated.toCached())

            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) { true }

            if (isMock) {
                context?.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)?.edit()?.apply {
                    putBoolean("isAdhanEnabled", profile.isAdhanEnabled)
                    putFloat("adhanLocationLat", profile.adhanLocationLat.toFloat())
                    putFloat("adhanLocationLng", profile.adhanLocationLng.toFloat())
                    putBoolean("fajrPrayedToday", profile.fajrPrayedToday)
                    putBoolean("dhuhrPrayedToday", profile.dhuhrPrayedToday)
                    putBoolean("asrPrayedToday", profile.asrPrayedToday)
                    putBoolean("maghribPrayedToday", profile.maghribPrayedToday)
                    putBoolean("ishaPrayedToday", profile.ishaPrayedToday)
                    putInt("fajrWeeklyCount", profile.fajrWeeklyCount)
                    putInt("dhuhrWeeklyCount", profile.dhuhrWeeklyCount)
                    putInt("asrWeeklyCount", profile.asrWeeklyCount)
                    putInt("maghribWeeklyCount", profile.maghribWeeklyCount)
                    putInt("ishaWeeklyCount", profile.ishaWeeklyCount)
                    putInt("fajrMonthlyCount", profile.fajrMonthlyCount)
                    putInt("dhuhrMonthlyCount", profile.dhuhrMonthlyCount)
                    putInt("asrMonthlyCount", profile.asrMonthlyCount)
                    putInt("maghribMonthlyCount", profile.maghribMonthlyCount)
                    putInt("ishaMonthlyCount", profile.ishaMonthlyCount)
                    putLong("lastPrayerDate", profile.lastPrayerDate)
                    putLong("lastWeeklyResetDate", profile.lastWeeklyResetDate)
                    putLong("lastMonthlyResetDate", profile.lastMonthlyResetDate)
                    putString("adhanCalculationMethod", profile.adhanCalculationMethod)
                    putString("adhanSoundPattern", profile.adhanSoundPattern)
                    apply()
                }
            } else {
                try {
                    firestore.collection("users").document(current.uid).update(
                        mapOf(
                            "isAdhanEnabled" to profile.isAdhanEnabled,
                            "adhanLocationLat" to profile.adhanLocationLat,
                            "adhanLocationLng" to profile.adhanLocationLng,
                            "fajrPrayedToday" to profile.fajrPrayedToday,
                            "dhuhrPrayedToday" to profile.dhuhrPrayedToday,
                            "asrPrayedToday" to profile.asrPrayedToday,
                            "maghribPrayedToday" to profile.maghribPrayedToday,
                            "ishaPrayedToday" to profile.ishaPrayedToday,
                            "fajrWeeklyCount" to profile.fajrWeeklyCount,
                            "dhuhrWeeklyCount" to profile.dhuhrWeeklyCount,
                            "asrWeeklyCount" to profile.asrWeeklyCount,
                            "maghribWeeklyCount" to profile.maghribWeeklyCount,
                            "ishaWeeklyCount" to profile.ishaWeeklyCount,
                            "fajrMonthlyCount" to profile.fajrMonthlyCount,
                            "dhuhrMonthlyCount" to profile.dhuhrMonthlyCount,
                            "asrMonthlyCount" to profile.asrMonthlyCount,
                            "maghribMonthlyCount" to profile.maghribMonthlyCount,
                            "ishaMonthlyCount" to profile.ishaMonthlyCount,
                            "lastPrayerDate" to profile.lastPrayerDate,
                            "lastWeeklyResetDate" to profile.lastWeeklyResetDate,
                            "lastMonthlyResetDate" to profile.lastMonthlyResetDate,
                            "adhanCalculationMethod" to profile.adhanCalculationMethod,
                            "adhanSoundPattern" to profile.adhanSoundPattern
                        )
                    ).await()
                } catch (e: Exception) {}
            }
        }
    }
    fun updateFcmToken(token: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) { true }

            if (!isMock) {
                try {
                    firestore.collection("users").document(currentUserId)
                        .update("fcmToken", token)
                        .await()
                    val currentProfile = _currentUserProfile.value
                    if (currentProfile != null) {
                        _currentUserProfile.value = currentProfile.copy(fcmToken = token)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
