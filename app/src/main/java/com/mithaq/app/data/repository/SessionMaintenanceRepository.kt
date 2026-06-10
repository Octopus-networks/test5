package com.mithaq.app.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import com.mithaq.app.model.UserProfile
import com.mithaq.app.data.local.UserDao
import com.mithaq.app.data.local.MithaqDatabase
import com.mithaq.app.data.local.toCached

class SessionMaintenanceRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val context: Context?,
    private val _currentUserProfile: MutableStateFlow<UserProfile?>,
    private val userDao: UserDao? = context?.let { MithaqDatabase.getDatabase(it).userDao() }
) {

    suspend fun updateOnlineStatus() {
        val current = _currentUserProfile.value ?: return
        val now = System.currentTimeMillis()
        val updated = current.copy(lastSeen = now)
        _currentUserProfile.value = updated
        userDao?.insertUser(updated.toCached())
        val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
            auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
        } catch (e: Exception) {
            true
        }
        if (isMock) {
            context?.getSharedPreferences("mithaq_mock_auth", Context.MODE_PRIVATE)?.edit()?.apply {
                putLong("lastSeen", now)
                apply()
            }
        } else {
            try {
                firestore.collection("users").document(current.uid).update("lastSeen", now).await()
            } catch (e: Exception) {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    suspend fun updateGpsLocation(
        latitude: Double,
        longitude: Double,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        try {
            val current = _currentUserProfile.value
            if (current == null) {
                onResult(false, "No active user profile.")
                return
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
                context?.getSharedPreferences("mithaq_mock_auth", Context.MODE_PRIVATE)?.edit()?.apply {
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
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            onResult(false, e.localizedMessage ?: "Failed to update GPS location.")
        }
    }

    suspend fun updatePrayerStats(profile: com.mithaq.app.model.UserProfile) {
        val current = _currentUserProfile.value ?: return
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
            context?.getSharedPreferences("mithaq_mock_auth", Context.MODE_PRIVATE)?.edit()?.apply {
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

    suspend fun updateFcmToken(token: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
            auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
        } catch (e: Exception) { true }

        if (!isMock) {
            try {
                com.mithaq.app.notification.FcmTokenRepository(firestore).registerToken(currentUserId, token)
                val currentProfile = _currentUserProfile.value
                if (currentProfile != null) {
                    _currentUserProfile.value = currentProfile.copy(fcmToken = token)
                }
            } catch (e: Exception) {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }
}
