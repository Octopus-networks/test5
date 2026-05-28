package com.mithaq.app.ui.limit

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manager class for Feature 5: Smart Daily Chat Limits.
 * Restricts free tier users to initiating 3 new chats per calendar day.
 */
class ChatLimitManager(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Data structure tracking daily limit states.
     */
    data class LimitState(
        val isPremium: Boolean,
        val canInitiate: Boolean,
        val remainingChats: Int
    )

    private fun getTodayDateString(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(Date())
    }

    /**
     * Checks if the user is allowed to initiate a new chat.
     */
    suspend fun checkChatLimit(userId: String): LimitState {
        return try {
            val userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            val isPremium = userDoc.getBoolean("isPremium") ?: false
            if (isPremium) {
                return LimitState(isPremium = true, canInitiate = true, remainingChats = Int.MAX_VALUE)
            }

            val limitsDoc = firestore.collection("users")
                .document(userId)
                .collection("chatLimits")
                .document(getTodayDateString())
                .get()
                .await()

            val initiatedCount = limitsDoc.getLong("count")?.toInt() ?: 0
            val remaining = (3 - initiatedCount).coerceAtLeast(0)

            LimitState(
                isPremium = false,
                canInitiate = remaining > 0,
                remainingChats = remaining
            )
        } catch (e: Exception) {
            // Fail closed so a rules/network problem cannot bypass free-tier limits.
            LimitState(isPremium = false, canInitiate = false, remainingChats = 0)
        }
    }

    /**
     * Increments the initiated chat counter for the user for the current day.
     */
    suspend fun recordNewChatInitiated(userId: String): Boolean {
        return try {
            val limitsRef = firestore.collection("users")
                .document(userId)
                .collection("chatLimits")
                .document(getTodayDateString())

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(limitsRef)
                val currentCount = snapshot.getLong("count")?.toInt() ?: 0
                transaction.set(limitsRef, mapOf("count" to currentCount + 1))
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
