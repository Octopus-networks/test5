package com.mithaq.app.data.repository

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mithaq.app.Config
import com.mithaq.app.domain.model.NotificationSettings
import kotlinx.coroutines.tasks.await

/**
 * Phase 13C — reads/writes the owner-only notification preferences document at
 * users/{uid}/notificationSettings/preferences.
 *
 * Safe-by-default: any missing document, missing field, or read error resolves to
 * the all-enabled defaults so a user is never accidentally muted. In mock/demo
 * builds the preferences live in SharedPreferences instead of Firestore.
 *
 * No FCM tokens or technical data are touched here.
 */
class NotificationSettingsRepository(
    private val context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun load(uid: String): NotificationSettings {
        if (uid.isBlank()) return NotificationSettings()
        if (Config.isMock()) return loadFromPrefs()
        return try {
            val snapshot = firestore.collection("users").document(uid)
                .collection(COLLECTION).document(DOCUMENT)
                .get().await()
            if (!snapshot.exists()) {
                NotificationSettings()
            } else {
                NotificationSettings(
                    notificationsEnabled = snapshot.getBoolean("notificationsEnabled") ?: true,
                    interestRequestNotifications = snapshot.getBoolean("interestRequestNotifications") ?: true,
                    photoRequestNotifications = snapshot.getBoolean("photoRequestNotifications") ?: true,
                    chatRequestNotifications = snapshot.getBoolean("chatRequestNotifications") ?: true,
                    messageNotifications = snapshot.getBoolean("messageNotifications") ?: true,
                    photoModerationNotifications = snapshot.getBoolean("photoModerationNotifications") ?: true
                )
            }
        } catch (e: Exception) {
            // Safe fallback: treat notifications as enabled rather than silently muting.
            NotificationSettings()
        }
    }

    suspend fun save(uid: String, settings: NotificationSettings): Result<Unit> {
        if (uid.isBlank()) return Result.failure(IllegalStateException("Missing user."))
        if (Config.isMock()) {
            saveToPrefs(settings)
            return Result.success(Unit)
        }
        return try {
            firestore.collection("users").document(uid)
                .collection(COLLECTION).document(DOCUMENT)
                .set(settings.toMap(), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun NotificationSettings.toMap(): Map<String, Any> = mapOf(
        "notificationsEnabled" to notificationsEnabled,
        "interestRequestNotifications" to interestRequestNotifications,
        "photoRequestNotifications" to photoRequestNotifications,
        "chatRequestNotifications" to chatRequestNotifications,
        "messageNotifications" to messageNotifications,
        "photoModerationNotifications" to photoModerationNotifications,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun loadFromPrefs(): NotificationSettings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return NotificationSettings(
            notificationsEnabled = prefs.getBoolean("notificationsEnabled", true),
            interestRequestNotifications = prefs.getBoolean("interestRequestNotifications", true),
            photoRequestNotifications = prefs.getBoolean("photoRequestNotifications", true),
            chatRequestNotifications = prefs.getBoolean("chatRequestNotifications", true),
            messageNotifications = prefs.getBoolean("messageNotifications", true),
            photoModerationNotifications = prefs.getBoolean("photoModerationNotifications", true)
        )
    }

    private fun saveToPrefs(settings: NotificationSettings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            putBoolean("notificationsEnabled", settings.notificationsEnabled)
            putBoolean("interestRequestNotifications", settings.interestRequestNotifications)
            putBoolean("photoRequestNotifications", settings.photoRequestNotifications)
            putBoolean("chatRequestNotifications", settings.chatRequestNotifications)
            putBoolean("messageNotifications", settings.messageNotifications)
            putBoolean("photoModerationNotifications", settings.photoModerationNotifications)
            apply()
        }
    }

    private companion object {
        const val COLLECTION = "notificationSettings"
        const val DOCUMENT = "preferences"
        const val PREFS = "mithaq_notification_settings"
    }
}
