package com.mithaq.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.mithaq.app.domain.model.UserBlock
import kotlinx.coroutines.tasks.await

class BlockRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun blockUser(
        blockerUserId: String,
        blockedUserId: String,
        chatId: String = ""
    ): SafetyWriteResult {
        val user = auth.currentUser
        if (user?.uid != blockerUserId || !user.isEmailVerified) {
            return SafetyWriteResult.Error("Please verify your email before blocking.")
        }
        if (blockedUserId.isBlank() || blockerUserId == blockedUserId) {
            return SafetyWriteResult.Error("You cannot block this member.")
        }

        return try {
            val blockId = blockIdFor(blockerUserId, blockedUserId)
            val blockRef = firestore.collection("blocks").document(blockId)
            if (blockRef.get().await().exists()) {
                return SafetyWriteResult.Success("Member is already blocked.")
            }
            // Keep the current write shape compatible with the deployed rules.
            // TODO: Migrate to blockerUserId/blockedUserId + timestamps after rules are updated.
            blockRef.set(
                mapOf(
                    "blockerId" to blockerUserId,
                    "blockedId" to blockedUserId,
                    "timestamp" to FieldValue.serverTimestamp()
                )
            ).await()
            SafetyWriteResult.Success("Member blocked. Messaging is now unavailable for this conversation.")
        } catch (e: Exception) {
            SafetyWriteResult.Error(e.localizedMessage ?: "Could not block this member.")
        }
    }

    suspend fun unblockUser(blockerUserId: String, blockedUserId: String): SafetyWriteResult {
        val user = auth.currentUser
        if (user?.uid != blockerUserId || !user.isEmailVerified) {
            return SafetyWriteResult.Error("Please verify your email before changing block settings.")
        }
        if (blockedUserId.isBlank() || blockerUserId == blockedUserId) {
            return SafetyWriteResult.Error("Invalid member.")
        }

        return try {
            firestore.collection("blocks")
                .document(blockIdFor(blockerUserId, blockedUserId))
                .delete()
                .await()
            SafetyWriteResult.Success("Member unblocked.")
        } catch (e: Exception) {
            SafetyWriteResult.Error(e.localizedMessage ?: "Could not unblock this member.")
        }
    }

    suspend fun isBlockedBetweenUsers(userA: String, userB: String): Boolean {
        if (userA.isBlank() || userB.isBlank() || userA == userB) return true
        val first = firestore.collection("blocks")
            .document(blockIdFor(userA, userB))
            .get()
            .await()
            .exists()
        if (first) return true
        return firestore.collection("blocks")
            .document(blockIdFor(userB, userA))
            .get()
            .await()
            .exists()
    }

    suspend fun getBlockedUsers(userId: String): List<UserBlock> {
        val user = auth.currentUser
        if (user?.uid != userId || !user.isEmailVerified) return emptyList()
        return firestore.collection("blocks")
            .whereEqualTo("blockerId", userId)
            .get()
            .await()
            .documents
            .map { it.toUserBlock() }
    }

    private fun blockIdFor(blockerUserId: String, blockedUserId: String): String {
        return "${blockerUserId.trim()}_${blockedUserId.trim()}"
    }

    private fun DocumentSnapshot.toUserBlock(): UserBlock {
        return UserBlock(
            blockId = getString("blockId") ?: id,
            blockerUserId = getString("blockerUserId") ?: getString("blockerId").orEmpty(),
            blockedUserId = getString("blockedUserId") ?: getString("blockedId").orEmpty(),
            chatId = getString("chatId").orEmpty(),
            createdAt = getTimestamp("createdAt")?.toDate() ?: getTimestamp("timestamp")?.toDate(),
            updatedAt = getTimestamp("updatedAt")?.toDate() ?: getTimestamp("timestamp")?.toDate()
        )
    }
}
