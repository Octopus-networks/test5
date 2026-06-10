package com.mithaq.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class PrivacySettingsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    data class PrivacySettingsData(
        val isPremium: Boolean = false,
        val isIncognito: Boolean = false,
        val showAge: Boolean = true,
        val showLocation: Boolean = true,
        val showMaritalStatus: Boolean = true,
        val showMarriageTimeline: Boolean = true
    )

    suspend fun getPrivacySettings(uid: String): PrivacySettingsData {
        val userSnap = firestore.collection("users").document(uid).get().await()
        val isPremium = userSnap.getBoolean("isPremium") ?: false
        val isIncognito = userSnap.getBoolean("isIncognito") ?: false

        val snap = firestore.collection("profiles").document(uid).get().await()
        @Suppress("UNCHECKED_CAST")
        val privacy = snap.get("privacyTrust") as? Map<String, Any?> ?: emptyMap()
        val showAge = (privacy["showAge"] as? Boolean) ?: true
        val showLocation = (privacy["showLocation"] as? Boolean) ?: true
        val showMaritalStatus = (privacy["showMaritalStatus"] as? Boolean) ?: true
        val showMarriageTimeline = (privacy["showMarriageTimeline"] as? Boolean) ?: true

        return PrivacySettingsData(
            isPremium = isPremium,
            isIncognito = isIncognito,
            showAge = showAge,
            showLocation = showLocation,
            showMaritalStatus = showMaritalStatus,
            showMarriageTimeline = showMarriageTimeline
        )
    }

    suspend fun updatePrivacySettings(
        uid: String, 
        isIncognito: Boolean,
        showAge: Boolean,
        showLocation: Boolean,
        showMaritalStatus: Boolean,
        showMarriageTimeline: Boolean
    ) {
        val privacy = mapOf(
            "showAge" to showAge,
            "showLocation" to showLocation,
            "showMaritalStatus" to showMaritalStatus,
            "showMarriageTimeline" to showMarriageTimeline
        )
        firestore.collection("profiles")
            .document(uid)
            .set(mapOf("privacyTrust" to privacy), SetOptions.merge())
            .await()
        
        firestore.collection("users")
            .document(uid)
            .update("isIncognito", isIncognito)
            .await()
    }
}
