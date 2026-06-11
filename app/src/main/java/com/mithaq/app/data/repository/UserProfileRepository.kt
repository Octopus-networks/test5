package com.mithaq.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.mithaq.app.model.Gender
import com.mithaq.app.model.ModestyPreference
import com.mithaq.app.model.PrayerFrequency
import com.mithaq.app.model.RelocationWillingness
import com.mithaq.app.model.Sect
import com.mithaq.app.model.UserProfile
import com.mithaq.app.util.CountryUtils
import kotlinx.coroutines.tasks.await

/**
 * Repository for user-profile field updates that were previously done inline
 * inside Composables. Constructor-injected Firestore with a default — matches
 * the existing repository convention (no Hilt).
 */
class UserProfileRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun getProfileForResolver(uid: String): UserProfile? {
        val doc = firestore.collection("users").document(uid).get().await()
        if (!doc.exists()) return null

        val genderStr = doc.getString("gender") ?: "FEMALE"
        val gender = if (genderStr.equals("MALE", ignoreCase = true)) Gender.MALE else Gender.FEMALE
        val sectStr = doc.getString("sect") ?: "SUNNI"
        val sect = try {
            Sect.valueOf(sectStr.uppercase())
        } catch (e: Exception) {
            Sect.SUNNI
        }
        val prayerStr = doc.getString("prayerFrequency") ?: "ALWAYS"
        val prayer = try {
            PrayerFrequency.valueOf(prayerStr.uppercase())
        } catch (e: Exception) {
            PrayerFrequency.ALWAYS
        }
        val modestyStr = doc.getString("modestyPreference") ?: "HIJAB"
        val modesty = try {
            ModestyPreference.valueOf(modestyStr.uppercase())
        } catch (e: Exception) {
            ModestyPreference.HIJAB
        }
        val relocationStr = doc.getString("relocationWillingness") ?: "OPEN"
        val relocation = try {
            RelocationWillingness.valueOf(relocationStr.uppercase())
        } catch (e: Exception) {
            RelocationWillingness.OPEN
        }

        return UserProfile(
            uid = uid,
            name = doc.getString("name") ?: "",
            gender = gender,
            age = doc.getLong("age")?.toInt() ?: 18,
            city = doc.getString("city") ?: "",
            country = doc.getString("country") ?: "",
            timezone = CountryUtils.getTimezoneForProfile(
                doc.getString("country") ?: "",
                doc.getString("timezone")
            ),
            imageUrl = doc.getString("imageUrl") ?: "",
            sect = sect,
            prayerFrequency = prayer,
            modestyPreference = modesty,
            relocationWillingness = relocation,
            verificationStatus = doc.getString("verificationStatus") ?: "NONE",
            isPremium = doc.getBoolean("isPremium") ?: false,
            lastSeen = doc.getLong("lastSeen") ?: 0L,
            photoAccessApprovedUsers =
                doc.get("photoAccessApprovedUsers") as? List<String> ?: emptyList()
        )
    }

    /**
     * Updates the profile image URL on the canonical `users/{uid}` document.
     */
    suspend fun updateImageUrl(uid: String, imageUrl: String) {
        firestore.collection("users")
            .document(uid)
            .update("imageUrl", imageUrl)
            .await()
    }

    /**
     * Returns `true` when the Firestore instance is configured with a mock/test
     * API key (used by the Developer Settings panel).
     */
    fun isMockDatabase(): Boolean {
        return try {
            val apiKey = firestore.app?.options?.apiKey
            apiKey == "mock-api-key-for-testing" || apiKey?.contains("mock") == true
        } catch (e: Exception) {
            true
        }
    }
}
