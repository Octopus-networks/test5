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
    private enum class DiscoveryDirection {
        MaleSeekingWife,
        FemaleSeekingHusband,
        Guardian,
        Unknown
    }

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
        val currentDirection = loadCurrentUserDirection(currentUserId)
        return firestore.collection("publicProfiles")
            .whereEqualTo("isEmailVerified", true)
            .limit(limit * 5)
            .get()
            .await()
            .documents
            .map { it.toPublicProfile() }
            .filter { it.userId.isNotBlank() && it.userId != currentUserId }
            // Client-side (not whereEqualTo) so docs the mirror hasn't rebuilt yet —
            // which lack the field entirely — stay visible instead of vanishing.
            .filter { !it.isIncognito }
            .filter { it.isEligibleFor(currentDirection) }
            .sortedWith(
                compareByDescending<PublicProfile> { it.lastActiveAt }
                    .thenByDescending { it.updatedAt }
            )
            .take(limit.toInt())
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
            isIncognito = getBoolean("isIncognito") == true,
            photoPrivacyMode = getString("photoPrivacyMode") ?: "blurred_by_default",
            profileCompletionPercent = getLong("profileCompletionPercent")?.toInt() ?: 0,
            lastActiveAt = getTimestamp("lastActiveAt")?.toDate(),
            updatedAt = getTimestamp("updatedAt")?.toDate()
        )
    }

    private suspend fun loadCurrentUserDirection(userId: String?): DiscoveryDirection {
        if (userId.isNullOrBlank()) return DiscoveryDirection.Unknown
        getPublicProfile(userId)?.accountType.toDiscoveryDirection().takeIf {
            it != DiscoveryDirection.Unknown
        }?.let { return it }

        return try {
            val snapshot = firestore.collection("profiles").document(userId).get().await()
            val basicInfo = snapshot.get("basicInfo") as? Map<*, *> ?: emptyMap<Any, Any>()
            val accountType = basicInfo["accountType"] as? String
            val gender = basicInfo["gender"] as? String
            accountType.toDiscoveryDirection().takeIf {
                it != DiscoveryDirection.Unknown
            } ?: gender.toDiscoveryDirection()
        } catch (e: Exception) {
            DiscoveryDirection.Unknown
        }
    }

    private fun PublicProfile.isEligibleFor(currentDirection: DiscoveryDirection): Boolean {
        val candidateDirection = accountType.toDiscoveryDirection()
        return when (currentDirection) {
            DiscoveryDirection.MaleSeekingWife -> candidateDirection == DiscoveryDirection.FemaleSeekingHusband
            DiscoveryDirection.FemaleSeekingHusband -> candidateDirection == DiscoveryDirection.MaleSeekingWife
            DiscoveryDirection.Guardian,
            DiscoveryDirection.Unknown -> false
        }
    }

    private fun String?.toDiscoveryDirection(): DiscoveryDirection {
        val normalized = this
            ?.trim()
            ?.lowercase()
            ?.replace("_", " ")
            ?.replace("-", " ")
            ?: return DiscoveryDirection.Unknown

        return when {
            normalized == "male" ||
                normalized.contains("man seeking") ||
                normalized.contains("male seeking") ||
                normalized.contains("husband seeking") -> DiscoveryDirection.MaleSeekingWife
            normalized == "female" ||
                normalized.contains("woman seeking") ||
                normalized.contains("female seeking") ||
                normalized.contains("wife seeking") -> DiscoveryDirection.FemaleSeekingHusband
            normalized == "guardian" || normalized.contains("wali") -> DiscoveryDirection.Guardian
            else -> DiscoveryDirection.Unknown
        }
    }
}

sealed interface PublicProfileWriteResult {
    data object Success : PublicProfileWriteResult
    data class Error(val message: String) : PublicProfileWriteResult
}
