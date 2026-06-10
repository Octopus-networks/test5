package com.mithaq.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.mithaq.app.data.local.MithaqDatabase
import com.mithaq.app.data.local.toCached
import com.mithaq.app.data.local.toDomain
import com.mithaq.app.model.Gender
import com.mithaq.app.model.Sect
import com.mithaq.app.model.PrayerFrequency
import com.mithaq.app.model.ModestyPreference
import com.mithaq.app.model.RelocationWillingness
import com.mithaq.app.model.UserProfile
import com.mithaq.app.service.BackendFunctions

class AdminViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val context: android.content.Context? = null,
    private val _currentUserProfile: MutableStateFlow<UserProfile?>
) : ViewModel() {

    private val db = context?.let { MithaqDatabase.getDatabase(it) }
    private val userDao = db?.userDao()

    val allUsersFlow: Flow<List<UserProfile>> = userDao?.getAllUsersFlow()?.map { list ->
        list.map { it.toDomain() }
    } ?: flowOf(emptyList())

    val pendingVerificationUsers: Flow<List<UserProfile>> = userDao?.getPendingUsersFlow()?.map { list ->
        list.map { it.toDomain() }
    } ?: flowOf(emptyList())

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
                    com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
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
                        val isIncognito = doc.getBoolean("isIncognito") ?: false
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
                            isIncognito = isIncognito,
                            timezone = timezone,
                            currentStreakDays = currentStreakDays
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                profiles.forEach { userDao?.insertUser(it.toCached()) }
            } catch (e: Exception) {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
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
            val premiumExpiry = if (isPremium) {
                System.currentTimeMillis() + 365L * 24L * 60L * 60L * 1000L
            } else {
                0L
            }
            try {
                if (!isMock) {
                    try {
                        BackendFunctions.setUserPremium(targetUid, isPremium, plan)
                    } catch (functionsError: Exception) {
                        firestore.collection("users").document(targetUid).update(
                            mapOf(
                                "isPremium" to isPremium,
                                "subscriptionPlan" to plan,
                                "premiumExpiry" to premiumExpiry
                            )
                        ).await()
                    }
                }
                val cachedUser = userDao?.getUser(targetUid)
                if (cachedUser != null) {
                    userDao.insertUser(
                        cachedUser.copy(
                            isPremium = isPremium,
                            subscriptionPlan = plan,
                            premiumExpiry = premiumExpiry
                        )
                    )
                }
                if (targetUid == _currentUserProfile.value?.uid) {
                    _currentUserProfile.value = _currentUserProfile.value?.copy(
                        isPremium = isPremium,
                        subscriptionPlan = plan,
                        premiumExpiry = premiumExpiry
                    )
                }
            } catch (e: Exception) {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
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
                    try {
                        BackendFunctions.setUserRole(targetUid, isWali, isAdmin)
                    } catch (functionsError: Exception) {
                        firestore.collection("users").document(targetUid).update(
                            mapOf(
                                "isWaliAccount" to isWali,
                                "isAdmin" to isAdmin
                            )
                        ).await()
                    }
                }
                val cachedUser = userDao?.getUser(targetUid)
                if (cachedUser != null) {
                    userDao.insertUser(cachedUser.copy(isWaliAccount = isWali, isAdmin = isAdmin))
                }
                if (targetUid == _currentUserProfile.value?.uid) {
                    _currentUserProfile.value = _currentUserProfile.value?.copy(isWaliAccount = isWali, isAdmin = isAdmin)
                }
            } catch (e: Exception) {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    fun adminDeleteUser(targetUid: String) {
        viewModelScope.launch {
            try {
                userDao?.deleteUser(targetUid)
            } catch (e: Exception) {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
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
                    com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        }
    }
}
