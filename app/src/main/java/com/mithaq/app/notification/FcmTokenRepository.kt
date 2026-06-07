package com.mithaq.app.notification

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.security.MessageDigest

/**
 * Phase 13A — FCM token foundation.
 *
 * Canonical entry point for persisting a device's Firebase Cloud Messaging
 * registration token. Tokens are stored per-device in an owner-only subcollection:
 *
 *     users/{uid}/fcmTokens/{tokenId}
 *
 * where {tokenId} is a deterministic SHA-256 hash of the token. Hashing makes the
 * write idempotent — the same token always maps to the same document, so a refresh
 * updates in place instead of creating duplicate docs — and keeps the raw token out
 * of the document id/path.
 *
 * The legacy flat field users/{uid}.fcmToken is ALSO written so the existing push
 * delivery flow keeps working unchanged. Phase 13A is purely additive.
 *
 * Privacy / security:
 *  - fcmTokens docs are owner-only (see firestore.rules); never publicly readable.
 *  - The token is never written into publicProfiles.
 *  - The full token is never logged.
 */
class FcmTokenRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Registers / refreshes the FCM [token] for [uid]. Fire-and-forget so it is safe
     * to call from non-coroutine contexts such as
     * [com.google.firebase.messaging.FirebaseMessagingService.onNewToken].
     */
    fun registerToken(uid: String, token: String) {
        if (uid.isBlank() || token.isBlank()) return

        val userDoc = firestore.collection("users").document(uid)

        // New canonical per-device store (owner-only subcollection).
        val tokenDoc = mapOf(
            "token" to token,
            "platform" to "android",
            "updatedAt" to FieldValue.serverTimestamp()
        )
        userDoc.collection("fcmTokens")
            .document(tokenIdFor(token))
            .set(tokenDoc, SetOptions.merge())

        // Legacy flat mirror — preserved so the current push flow is not broken.
        userDoc.update("fcmToken", token)
    }

    companion object {
        /**
         * Deterministic, non-reversible document id for [token]. A SHA-256 hash keeps
         * the raw token out of the document path while staying stable across refreshes.
         */
        fun tokenIdFor(token: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
