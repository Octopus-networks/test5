package com.mithaq.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.mithaq.app.domain.model.PublicProfile
import kotlinx.coroutines.tasks.await

/**
 * Reads the server-owned public discovery mirror.
 *
 * Phase 11.8 (privacy closure): Android no longer writes `publicProfiles`. The mirror is
 * created/updated by the `mirrorPublicProfile` Cloud Function on `profiles/{userId}` writes,
 * and Firestore rules deny all client writes to `publicProfiles`. This repository is read-only.
 */
class PublicProfileRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    /**
     * Deprecated no-op. The public discovery mirror is owned by the Cloud Function; the client
     * only writes `profiles/{userId}`. Kept for source compatibility with existing callers.
     */
    @Deprecated(
        "publicProfiles are mirrored server-side by the mirrorPublicProfile Cloud Function. " +
            "Android must not write publicProfiles directly. This is now a no-op."
    )
    @Suppress("UNUSED_PARAMETER", "RedundantSuspendModifier")
    suspend fun createOrUpdatePublicProfileFromOnboarding(userId: String): PublicProfileWriteResult {
        // No client-side write. The server mirror handles publicProfiles.
        return PublicProfileWriteResult.Success
    }

    suspend fun getDiscoverProfiles(limit: Long = 20): List<PublicProfile> {
        val currentUserId = auth.currentUser?.uid
        // Islamic matchmaking: Discover must only surface opposite-gender profiles (Search already
        // does this). publicProfiles is the only readable source for other users, so the filter is
        // applied here on the mirrored `gender` field. Read the current user's own gender from their
        // own users/{uid} doc (own-doc reads are allowed by rules).
        val oppositeGender = currentUserId?.let { resolveOppositeGender(it) }
        return firestore.collection("publicProfiles")
            .whereEqualTo("isEmailVerified", true)
            .limit(limit)
            .get()
            .await()
            .documents
            .map { it.toPublicProfile() }
            .filter { it.userId.isNotBlank() && it.userId != currentUserId }
            // Keep only the opposite gender. Profiles whose gender is blank (not yet mirrored /
            // backfilled into publicProfiles) are kept so discovery doesn't go empty during rollout;
            // once the backfill runs, every mirror carries a gender and this fully enforces the rule.
            .filter { oppositeGender == null || it.gender.isBlank() || it.gender == oppositeGender }
            .sortedWith(
                compareByDescending<PublicProfile> { it.lastActiveAt }
                    .thenByDescending { it.updatedAt }
            )
    }

    /**
     * Returns the opposite gender ("MALE"/"FEMALE") of the current user, read from their own
     * users/{uid} document, or null when it cannot be determined (in which case discovery is not
     * gender-filtered rather than shown empty).
     */
    private suspend fun resolveOppositeGender(userId: String): String? {
        return try {
            val gender = firestore.collection("users").document(userId).get().await()
                .getString("gender")?.uppercase()
            when (gender) {
                "MALE" -> "FEMALE"
                "FEMALE" -> "MALE"
                else -> null
            }
        } catch (e: Exception) {
            null
        }
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

    private fun DocumentSnapshot.toPublicProfile(): PublicProfile {
        return PublicProfile(
            userId = getString("userId").orEmpty(),
            displayName = getString("displayName").orEmpty(),
            gender = getString("gender").orEmpty().uppercase(),
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
}

sealed interface PublicProfileWriteResult {
    data object Success : PublicProfileWriteResult
    data class Error(val message: String) : PublicProfileWriteResult
}
