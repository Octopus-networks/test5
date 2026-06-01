package com.mithaq.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mithaq.app.domain.model.PublicProfile
import kotlinx.coroutines.tasks.await

class PublicProfileRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val blockRepository: BlockRepository = BlockRepository(firestore, auth)
) {
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

    private fun com.google.firebase.firestore.DocumentSnapshot.toPublicProfile(): PublicProfile {
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
}
