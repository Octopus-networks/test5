package com.mithaq.app.service

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

object BackendFunctions {
    private val functions: FirebaseFunctions
        get() = FirebaseFunctions.getInstance("us-central1")

    suspend fun setVerificationStatus(targetUid: String, status: String) {
        functions
            .getHttpsCallable("setVerificationStatus")
            .call(mapOf("targetUid" to targetUid, "status" to status))
            .await()
    }

    suspend fun setUserPremium(targetUid: String, isPremium: Boolean, plan: String) {
        functions
            .getHttpsCallable("setUserPremium")
            .call(mapOf("targetUid" to targetUid, "isPremium" to isPremium, "plan" to plan))
            .await()
    }

    suspend fun setUserRole(targetUid: String, isWali: Boolean, isAdmin: Boolean) {
        functions
            .getHttpsCallable("setUserRole")
            .call(mapOf("targetUid" to targetUid, "isWali" to isWali, "isAdmin" to isAdmin))
            .await()
    }

    suspend fun deleteUserProfile(targetUid: String) {
        functions
            .getHttpsCallable("deleteUserProfile")
            .call(mapOf("targetUid" to targetUid))
            .await()
    }
}
