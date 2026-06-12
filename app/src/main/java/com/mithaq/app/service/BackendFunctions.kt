package com.mithaq.app.service

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

object BackendFunctions {
    private val functions: FirebaseFunctions
        get() = FirebaseFunctions.getInstance("us-central1")

    suspend fun setVerificationStatus(targetUid: String, status: String, reason: String? = null) {
        val payload = mutableMapOf<String, Any>("targetUid" to targetUid, "status" to status)
        if (reason != null) {
            payload["reason"] = reason
        }
        functions
            .getHttpsCallable("setVerificationStatus")
            .call(payload)
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
     * Asks the server to create the chat request to [toUserId]. The callable checks the
     * caller's daily limit and creates the chatRequests document in one transaction
     * (Firestore rules deny client creates). Throws a
     * [com.google.firebase.functions.FirebaseFunctionsException] with code RESOURCE_EXHAUSTED
     * when a free user is over the daily cap (premium users are unlimited) and
     * FAILED_PRECONDITION when there is no accepted interest between the two members.
     */
    suspend fun recordChatInitiation(toUserId: String) {
        functions
            .getHttpsCallable("recordChatInitiation")
            .call(mapOf("toUserId" to toUserId))
            .await()
    }
}
