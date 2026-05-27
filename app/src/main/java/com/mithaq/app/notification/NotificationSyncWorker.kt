package com.mithaq.app.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/**
 * NotificationSyncWorker
 *
 * Runs in the background every 15 minutes (Android's minimum for PeriodicWork).
 * Checks for:
 *  1. Queued notifications (likes, views, etc.) stored in SharedPreferences
 *  2. New chat messages whose timestamp is newer than the last-seen timestamp
 *
 * Fires a local system notification for each pending item found.
 * Works in both Mock and Production modes.
 */
class NotificationSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            checkQueuedNotifications()
            checkNewChatMessages()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    // ─── 1. Queued Notifications (likes / views / photo requests) ────────────

    private fun checkQueuedNotifications() {
        val authPrefs = context.getSharedPreferences("mithaq_mock_auth", Context.MODE_PRIVATE)
        val currentUid = authPrefs.getString("uid", null) ?: return

        val queuePrefs = context.getSharedPreferences("mithaq_notification_queue", Context.MODE_PRIVATE)
        val queueStr = queuePrefs.getString("queue_$currentUid", "[]") ?: "[]"
        val array = try { JSONArray(queueStr) } catch (e: Exception) { return }

        if (array.length() > 0) {
            for (i in 0 until array.length()) {
                try {
                    val notif = array.getJSONObject(i)
                    val title = notif.getString("title")
                    val body = notif.getString("body")
                    MithaqFirebaseMessagingService.showLocalNotification(context, title, body)
                } catch (_: Exception) {}
            }
            // Clear the queue after firing
            queuePrefs.edit().putString("queue_$currentUid", "[]").apply()
        }
    }

    // ─── 2. New Chat Messages ─────────────────────────────────────────────────

    private fun checkNewChatMessages() {
        val authPrefs = context.getSharedPreferences("mithaq_mock_auth", Context.MODE_PRIVATE)
        val currentUid = authPrefs.getString("uid", null) ?: return
        val currentName = authPrefs.getString("name", "عضو") ?: "عضو"

        val seenPrefs = context.getSharedPreferences("mithaq_badge_prefs", Context.MODE_PRIVATE)

        val isMock = com.mithaq.app.Config.isMock()
        if (isMock) {
            val chatPrefs = context.getSharedPreferences("mithaq_mock_chat", Context.MODE_PRIVATE)
            val roomsStr = chatPrefs.getString("mithaq_mock_rooms", "[]") ?: "[]"
            val roomsArr = try { JSONArray(roomsStr) } catch (e: Exception) { return }

            for (i in 0 until roomsArr.length()) {
                try {
                    val room = roomsArr.getJSONObject(i)
                    val roomId = room.getString("roomId")

                    // Only process rooms the current user belongs to
                    val memberIds = room.getJSONArray("memberIds")
                    var isMember = false
                    for (j in 0 until memberIds.length()) {
                        if (memberIds.getString(j) == currentUid) isMember = true
                    }
                    if (!isMember) continue

                    val lastMsgTs = room.optLong("lastMessageTimestamp", 0L)
                    val lastSeenTs = seenPrefs.getLong("chat_seen_$roomId", 0L)

                    if (lastMsgTs > lastSeenTs) {
                        // Find the latest message in this room
                        val messagesStr = chatPrefs.getString("messages_$roomId", "[]") ?: "[]"
                        val messagesArr = try { JSONArray(messagesStr) } catch (e: Exception) { continue }

                        // Find the most recent message NOT sent by the current user
                        var latestContent: String? = null
                        var latestSender: String? = null
                        var latestTs = 0L

                        for (k in 0 until messagesArr.length()) {
                            val msg = messagesArr.getJSONObject(k)
                            val ts = msg.optLong("timestamp", 0L)
                            val sender = msg.optString("senderId", "")
                            if (ts > lastSeenTs && ts > latestTs && sender != currentUid && sender != "system") {
                                latestTs = ts
                                latestContent = msg.optString("content", "")
                                latestSender = sender
                            }
                        }

                        if (latestContent != null && !latestContent.isNullOrBlank()) {
                            MithaqFirebaseMessagingService.showLocalNotification(
                                context,
                                "ميثاق - رسالة جديدة 💬",
                                latestContent
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        } else {
            // Production mode: handled by FCM push from server.
            // WorkManager here only covers the queued Firestore notification documents
            // as a fallback if FCM token delivery fails.
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("notifications")
                    .whereEqualTo("recipientUid", currentUid)
                    .whereEqualTo("status", "PENDING")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        for (doc in snapshot.documents) {
                            val title = doc.getString("title") ?: "ميثاق"
                            val body = doc.getString("body") ?: ""
                            MithaqFirebaseMessagingService.showLocalNotification(context, title, body)
                            doc.reference.update("status", "DELIVERED")
                        }
                    }
            } catch (_: Exception) {}
        }
    }

    companion object {
        private const val WORK_NAME = "mithaq_notification_sync"

        /**
         * Schedules (or re-schedules) the periodic background sync.
         * Call this after successful login and on app startup.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationSyncWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,  // Don't reschedule if already running
                request
            )
        }

        /**
         * Cancels the periodic sync — call this on logout.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
