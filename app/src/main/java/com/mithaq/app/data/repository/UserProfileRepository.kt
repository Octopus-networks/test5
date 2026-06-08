package com.mithaq.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository for user-profile field updates that were previously done inline
 * inside Composables. Constructor-injected Firestore with a default — matches
 * the existing repository convention (no Hilt).
 */
class UserProfileRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

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
