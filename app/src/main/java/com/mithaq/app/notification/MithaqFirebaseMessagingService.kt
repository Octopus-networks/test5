package com.mithaq.app.notification

import android.Manifest
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

            // Build an explicit, app-scoped intent. The target component and package are set
            // directly on the variable (not via apply) so static analysis reliably sees the
            // explicit target. Wrapped in an IMMUTABLE PendingIntent so it can never be
            // modified or redirected to another app.
            val intent = Intent()
            intent.setClassName(context, MainActivity::class.java.name)
            intent.setPackage(context.packageName)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // Use the channel for the user's selected sound (device-local). On Android 8+ the
            // channel owns the sound; pre-O we set it on the builder below.
            val channelId = NotificationSoundPreferences.ensureActiveChannel(context)
            val soundUri = NotificationSoundPreferences.soundUri(context, NotificationSoundPreferences.getSelected(context))

            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setTicker(title)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVibrate(longArrayOf(0, 500, 200, 500))

            // Pre-O the sound is set on the builder; on Android 8+ the channel owns it.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && soundUri != null) {
                notificationBuilder.setSound(soundUri)
            }

            notificationManager.notify(notificationIdCounter.incrementAndGet(), notificationBuilder.build())
        }

    }
}
