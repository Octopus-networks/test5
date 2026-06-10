package com.mithaq.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.mithaq.app.model.UserProfile
import com.mithaq.app.model.Gender
import kotlinx.coroutines.tasks.await

class WaliRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getWardProfile(wardUid: String): UserProfile? {
        return try {
            val doc = firestore.collection("users").document(wardUid).get().await()
            if (doc.exists()) {
                val name = doc.getString("name") ?: ""
                val genderStr = doc.getString("gender") ?: "FEMALE"
                val gender = if (genderStr == "MALE") Gender.MALE else Gender.FEMALE
                val age = doc.getLong("age")?.toInt() ?: 25
                val city = doc.getString("city") ?: ""
                val country = doc.getString("country") ?: ""
                val imageUrl = doc.getString("imageUrl") ?: ""
                val photoApproved = doc.get("photoAccessApprovedUsers") as? List<String> ?: emptyList()
                val photoRequests = doc.get("photoAccessRequests") as? List<String> ?: emptyList()
                val verificationStatus = doc.getString("verificationStatus") ?: "NONE"
                
                UserProfile(
                    uid = wardUid,
                    name = name,
                    gender = gender,
                    age = age,
                    city = city,
                    country = country,
                    imageUrl = imageUrl,
                    photoAccessApprovedUsers = photoApproved,
                    photoAccessRequests = photoRequests,
                    verificationStatus = verificationStatus
                )
            } else {
                null
            }
        } catch (e: Exception) {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            null
        }
    }
}
