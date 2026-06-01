package com.mithaq.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mithaq.app.domain.model.PublicProfile
import kotlinx.coroutines.tasks.await

class PublicProfileRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val blockRepository: BlockRepository = BlockRepository(firestore, auth)
) {
    suspend fun createOrUpdatePublicProfileFromOnboarding(userId: String): PublicProfileWriteResult {
        if (userId.isBlank()) {
            return PublicProfileWriteResult.Error("Missing user id while preparing public profile.")
        }
        val user = auth.currentUser
        if (user?.uid != userId || !user.isEmailVerified) {
            return PublicProfileWriteResult.Error("Please verify your email before publishing discovery data.")
        }

        return try {
            val privateProfile = firestore.collection("profiles").document(userId).get().await()
            if (!privateProfile.exists()) {
                return PublicProfileWriteResult.Error("Profile setup was not found.")
            }

            val publicData = buildPublicProfileData(
                userId = userId,
                privateProfile = privateProfile,
                isEmailVerified = user.isEmailVerified
            )

            firestore.collection("publicProfiles")
                .document(userId)
                .set(publicData, SetOptions.merge())
                .await()

            PublicProfileWriteResult.Success
        } catch (e: Exception) {
            PublicProfileWriteResult.Error(
                e.localizedMessage ?: "Could not prepare public discovery profile."
            )
        }
    }

    suspend fun getDiscoverProfiles(limit: Long = 20): List<PublicProfile> {
        val currentUserId = auth.currentUser?.uid.orEmpty()
        val blockedByCurrentUser = blockRepository.getBlockedUserIds(currentUserId)
        val profiles = firestore.collection("publicProfiles")
            .whereEqualTo("isEmailVerified", true)
            .limit(limit)
            .get()
            .await()
            .documents
            .map { it.toPublicProfile() }

        val visibleProfiles = mutableListOf<PublicProfile>()
        for (profile in profiles) {
            if (profile.userId.isBlank() || profile.userId == currentUserId || profile.userId in blockedByCurrentUser) {
                continue
            }
            if (blockRepository.isBlockedBetweenUsers(currentUserId, profile.userId)) {
                continue
            }
            visibleProfiles += profile
        }

        return visibleProfiles.sortedWith(
            compareByDescending<PublicProfile> { it.lastActiveAt }
                .thenByDescending { it.updatedAt }
        )
    }

    suspend fun getPublicProfile(userId: String): PublicProfile? {
        if (userId.isBlank()) return null
        return try {
            val snapshot = firestore.collection("publicProfiles").document(userId).get().await()
            if (snapshot.exists()) snapshot.toPublicProfile() else null
        } catch (e: Exception) {
            null
        }
    }

    private fun buildPublicProfileData(
        userId: String,
        privateProfile: DocumentSnapshot,
        isEmailVerified: Boolean
    ): Map<String, Any?> {
        val basicInfo = privateProfile.getMap("basicInfo")
        val personalStatus = privateProfile.getMap("personalStatus")
        val marriageIntent = privateProfile.getMap("marriageIntent")

        val displayName = sanitizeDisplayName(basicInfo.string("name"))
        val accountType = basicInfo.string("accountType").humanize()
        val maritalStatus = personalStatus.string("maritalStatus").humanize()
        val timeline = marriageIntent.string("timeline").humanize()

        return mapOf(
            "userId" to userId,
            "displayName" to displayName,
            "age" to basicInfo.int("age"),
            "city" to basicInfo.string("city").trim(),
            "country" to basicInfo.string("country").humanize(),
            "accountType" to accountType,
            "maritalStatus" to maritalStatus,
            "marriageTimeline" to timeline,
            "prayerHabitPublicLabel" to "Not shared",
            "prayerRoutineShared" to false,
            "localTimeEnabled" to false,
            "hasGuardian" to false,
            "isEmailVerified" to isEmailVerified,
            "isIdentityVerified" to false,
            "photoPrivacyMode" to "blurred_by_default",
            "profileCompletionPercent" to (privateProfile.getLong("profileCompletionPercent")?.toInt() ?: 0),
            "lastActiveAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun DocumentSnapshot.toPublicProfile(): PublicProfile {
        return PublicProfile(
            userId = getString("userId").orEmpty(),
            displayName = getString("displayName").orEmpty(),
            age = getLong("age")?.toInt(),
            city = getString("city").orEmpty(),
            country = getString("country").orEmpty(),
            accountType = getString("accountType").orEmpty(),
            maritalStatus = getString("maritalStatus").orEmpty(),
            marriageTimeline = getString("marriageTimeline").orEmpty(),
            prayerHabitPublicLabel = getString("prayerHabitPublicLabel").orEmpty(),
            prayerRoutineShared = getBoolean("prayerRoutineShared") == true,
            localTimeEnabled = getBoolean("localTimeEnabled") == true,
            hasGuardian = getBoolean("hasGuardian") == true,
            isEmailVerified = getBoolean("isEmailVerified") == true,
            isIdentityVerified = getBoolean("isIdentityVerified") == true,
            photoPrivacyMode = getString("photoPrivacyMode") ?: "blurred_by_default",
            profileCompletionPercent = getLong("profileCompletionPercent")?.toInt() ?: 0,
            lastActiveAt = getTimestamp("lastActiveAt")?.toDate(),
            updatedAt = getTimestamp("updatedAt")?.toDate()
        )
    }

    private fun DocumentSnapshot.getMap(field: String): Map<*, *> {
        return get(field) as? Map<*, *> ?: emptyMap<Any, Any>()
    }

    private fun Map<*, *>.string(field: String): String {
        return this[field] as? String ?: ""
    }

    private fun Map<*, *>.int(field: String): Int? {
        val value = this[field]
        return when (value) {
            is Long -> value.toInt()
            is Int -> value
            is Double -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
    }

    private fun sanitizeDisplayName(rawName: String): String {
        return rawName
            .trim()
            .split(Regex("\\s+"))
            .firstOrNull()
            ?.take(30)
            .orEmpty()
    }

    private fun String.humanize(): String {
        return trim()
            .replace('-', '_')
            .split('_')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                val lower = word.lowercase()
                lower.replaceFirstChar { char -> char.titlecase() }
            }
    }
}

sealed interface PublicProfileWriteResult {
    data object Success : PublicProfileWriteResult
    data class Error(val message: String) : PublicProfileWriteResult
}
