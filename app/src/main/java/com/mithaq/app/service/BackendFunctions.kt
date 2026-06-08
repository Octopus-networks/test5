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

    /**
     * Records a new chat initiation against the caller's server-side daily limit. Throws a
     * [com.google.firebase.functions.FirebaseFunctionsException] with code RESOURCE_EXHAUSTED
     * when a free user is over the daily cap; premium users are unlimited.
     */
    suspend fun recordChatInitiation() {
        functions
            .getHttpsCallable("recordChatInitiation")
            .call()
            .await()
    }
}
