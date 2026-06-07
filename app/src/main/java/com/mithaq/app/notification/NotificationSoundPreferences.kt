package com.mithaq.app.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import com.mithaq.app.R

/**
 * In-app notification sound selection (device-local).
 *
 * Lets the user pick the sound used for the app's locally-posted notifications (foreground
 * FCM messages + the WorkManager fallback). On Android 8+ a channel's sound is immutable, so
 * each sound gets its own channel id and switching sounds recreates the channel. The choice
 * is stored only on this device — no Firestore, Cloud Functions, or backend involvement.
 *
 * Note: notifications delivered by the system while the app is killed follow the channel in
 * the FCM payload, not this selection (that would require a server change, intentionally out
 * of scope for this device-local feature).
 */
enum class NotificationSound(val key: String, val rawResId: Int?) {
    DEFAULT("default", null),
    CHIME("chime", R.raw.notif_chime),
    DING("ding", R.raw.notif_ding),
    SOFT("soft", R.raw.notif_soft),
    SILENT("silent", null);

    companion object {
        fun fromKey(key: String?): NotificationSound = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

object NotificationSoundPreferences {
    private const val PREFS = "mithaq_notification_sound"
    private const val KEY_SELECTED = "selected_sound"
    private const val CHANNEL_PREFIX = "mithaq_messages_snd_"
    private const val CHANNEL_NAME = "Mithaq Messages"

    // Older fixed channels are retired so System settings shows a single messages channel.
    private val LEGACY_CHANNEL_IDS = listOf("mithaq_messages_channel_v2", "mithaq_alerts_channel_v4")

    fun getSelected(context: Context): NotificationSound {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return NotificationSound.fromKey(prefs.getString(KEY_SELECTED, NotificationSound.DEFAULT.key))
    }

    fun setSelected(context: Context, sound: NotificationSound) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_SELECTED, sound.key)
            .apply()
        // Recreate the channel so the new sound takes effect on Android 8+.
        ensureActiveChannel(context)
    }

    /** Resolves the playable Uri for a sound (null == silent). */
    fun soundUri(context: Context, sound: NotificationSound): Uri? = when (sound) {
        NotificationSound.SILENT -> null
        NotificationSound.DEFAULT -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        else -> sound.rawResId?.let { Uri.parse("android.resource://${context.packageName}/$it") }
    }

    fun channelId(sound: NotificationSound): String = CHANNEL_PREFIX + sound.key

    /**
     * Ensures the channel for the currently-selected sound exists with that sound, and removes
     * stale per-sound channels plus the legacy channels so the system list stays clean. Returns
     * the active channel id. Safe to call repeatedly; a no-op below Android O.
     */
    fun ensureActiveChannel(context: Context): String {
        val selected = getSelected(context)
        val activeId = channelId(selected)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return activeId
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return activeId

        // Clean up other per-sound channels and the retired legacy channels.
        for (other in NotificationSound.entries) {
            if (other != selected) nm.deleteNotificationChannel(channelId(other))
        }
        for (legacy in LEGACY_CHANNEL_IDS) nm.deleteNotificationChannel(legacy)

        val channel = NotificationChannel(activeId, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Message and interaction alerts"
            enableLights(true)
            enableVibration(true)
            setShowBadge(true)
            setLockscreenVisibility(Notification.VISIBILITY_PUBLIC)
            vibrationPattern = longArrayOf(0, 500, 200, 500)
            val uri = soundUri(context, selected)
            if (uri == null) {
                setSound(null, null)
            } else {
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(uri, attributes)
            }
        }
        nm.createNotificationChannel(channel)
        return activeId
    }
}
