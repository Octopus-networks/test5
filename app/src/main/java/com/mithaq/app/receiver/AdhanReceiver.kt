package com.mithaq.app.receiver

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mithaq.app.MainActivity
import com.mithaq.app.R
import com.mithaq.app.util.AdhanScheduler
import java.util.Locale

class AdhanReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "adhan_channel"
        const val TAG = "AdhanReceiver"
        const val ACTION_ADHAN_ALARM = "com.mithaq.app.action.ADHAN_ALARM"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_LOCKED_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED
            )
        ) {
            Log.d(TAG, "System schedule event (${intent.action}). Rescheduling Adhan.")
            rescheduleSavedAdhan(context)
            return
        }

        if (intent.action != ACTION_ADHAN_ALARM) {
            Log.w(TAG, "Ignoring unsupported broadcast action: ${intent.action}")
            return
        }

        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Prayer"
        val lat = intent.getDoubleExtra("LAT", 0.0)
        val lng = intent.getDoubleExtra("LNG", 0.0)
        val calculationMethod = intent.getStringExtra("CALCULATION_METHOD")
        val soundPattern = intent.getStringExtra("SOUND_PATTERN") ?: "TAKBEER"

        Log.d(TAG, "Adhan triggered for: $prayerName")

        showNotification(context, prayerName, soundPattern)

        if (lat != 0.0 || lng != 0.0) {
            AdhanScheduler.scheduleNextAdhan(context, lat, lng, calculationMethod, soundPattern)
        }
    }

    private fun rescheduleSavedAdhan(context: Context) {
        val prefs = context.getSharedPreferences("mithaq_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("isAdhanEnabled", false)
        val lat = prefs.getFloat("adhan_lat", prefs.getFloat("adhanLocationLat", 0.0f)).toDouble()
        val lng = prefs.getFloat("adhan_lng", prefs.getFloat("adhanLocationLng", 0.0f)).toDouble()
        val calculationMethod = prefs.getString("adhan_calculation_method", "MUSLIM_WORLD_LEAGUE")
            ?: "MUSLIM_WORLD_LEAGUE"
        val soundPattern = prefs.getString("adhan_sound_pattern", "TAKBEER")
            ?: "TAKBEER"

        if (isEnabled && (lat != 0.0 || lng != 0.0)) {
            AdhanScheduler.scheduleNextAdhan(context, lat, lng, calculationMethod, soundPattern)
        }
    }

    private fun showNotification(context: Context, prayerName: String, soundPattern: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Cannot show Adhan notification. Notification permission denied.")
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val isSilent = soundPattern == "SILENT"
        val soundUri: Uri? = if (isSilent) {
            null
        } else {
            android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
        }
        val channelId = channelIdFor(soundPattern)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Adhan Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Prayer Times"
                if (isSilent || soundUri == null) {
                    setSound(null, null)
                } else {
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                    setSound(soundUri, audioAttributes)
                }
                enableLights(true)
                enableVibration(!isSilent)
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

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("حان وقت صلاة $arabicName")
            .setContentText("الله أكبر، حان الآن موعد الصلاة.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setSilent(isSilent)

        if (soundUri != null) {
            builder.setSound(soundUri)
        }

        notificationManager.notify(prayerName.hashCode(), builder.build())
    }

    private fun channelIdFor(soundPattern: String): String {
        return "${CHANNEL_ID}_${soundPattern.lowercase(Locale.ROOT)}"
    }
}
