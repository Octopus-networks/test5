package com.mithaq.app.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mithaq.app.MainActivity
import com.mithaq.app.R
import com.mithaq.app.util.AdhanScheduler

class AdhanReceiver : BroadcastReceiver() {
    
    companion object {
        const val CHANNEL_ID = "adhan_channel"
        const val TAG = "AdhanReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed. Rescheduling Adhan.")
            val prefs = context.getSharedPreferences("mithaq_prefs", Context.MODE_PRIVATE)
            val lat = prefs.getFloat("adhan_lat", 0.0f).toDouble()
            val lng = prefs.getFloat("adhan_lng", 0.0f).toDouble()
            if (lat != 0.0 && lng != 0.0) {
                AdhanScheduler.scheduleNextAdhan(context, lat, lng)
            }
            return
        }

        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Prayer"
        val lat = intent.getDoubleExtra("LAT", 0.0)
        val lng = intent.getDoubleExtra("LNG", 0.0)
        
        Log.d(TAG, "Adhan triggered for: $prayerName")

        showNotification(context, prayerName)

        // Schedule next Adhan
        if (lat != 0.0 && lng != 0.0) {
            AdhanScheduler.scheduleNextAdhan(context, lat, lng)
        }
    }

    private fun showNotification(context: Context, prayerName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Place a short "Allahu Akbar" sound in res/raw/takbeer.mp3
        var soundUri: Uri = Uri.parse("android.resource://" + context.packageName + "/raw/takbeer")
        
        // If raw resource doesn't exist, this might fail, so we just try to use it.
        // Android will fallback to default if not found in most cases.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Adhan Notifications"
            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Prayer Times"
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .build()
                setSound(soundUri, audioAttributes)
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1002,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val arabicName = when (prayerName) {
            "FAJR" -> "الفجر"
            "DHUHR" -> "الظهر"
            "ASR" -> "العصر"
            "MAGHRIB" -> "المغرب"
            "ISHA" -> "العشاء"
            else -> prayerName
        }

        val title = "حان وقت صلاة $arabicName"
        val text = "الله أكبر، حان الآن موعد الصلاة."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with proper app icon
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(prayerName.hashCode(), builder.build())
    }
}
