package com.mithaq.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mithaq.app.MainActivity

class MithaqFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val prefs = getSharedPreferences("mithaq_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()

        // Persist the refreshed token if a user is signed in. FcmTokenRepository writes the
        // owner-only users/{uid}/fcmTokens/{tokenId} subcollection doc AND mirrors the legacy
        // users/{uid}.fcmToken field so the existing push flow keeps working. (Phase 13A)
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FcmTokenRepository().registerToken(uid, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "ميثاق - Mithaq"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "رسالة جديدة"

        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        showLocalNotification(this, title, body)
    }

    companion object {
        private val notificationIdCounter = java.util.concurrent.atomic.AtomicInteger((System.currentTimeMillis() % 100000).toInt())

        fun showLocalNotification(context: Context, title: String, body: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val channelId = MESSAGE_CHANNEL_ID
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

            ensureMessageChannels(notificationManager, soundUri)

            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setTicker(title)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setSound(soundUri)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .setDefaults(NotificationCompat.DEFAULT_ALL)

            notificationManager.notify(notificationIdCounter.incrementAndGet(), notificationBuilder.build())
        }

        fun ensureMessageChannels(notificationManager: NotificationManager, soundUri: android.net.Uri? = null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

            val attributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val urgentChannel = NotificationChannel(
                MESSAGE_CHANNEL_ID,
                "Mithaq Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts for messages and interactions"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC)
                if (soundUri != null) {
                    setSound(soundUri, attributes)
                }
            }

            val defaultChannel = NotificationChannel(
                "mithaq_alerts_channel_v4",
                "Mithaq Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Default alerts for Mithaq"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                if (soundUri != null) {
                    setSound(soundUri, attributes)
                }
            }

            notificationManager.createNotificationChannel(urgentChannel)
            notificationManager.createNotificationChannel(defaultChannel)
        }

        const val MESSAGE_CHANNEL_ID = "mithaq_messages_channel_v2"
    }
}
