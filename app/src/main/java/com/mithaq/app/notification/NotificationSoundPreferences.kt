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
 * A single app-wide default sound (GENERAL) plus an optional per-category override, applied to
 * the app's locally-posted notifications (foreground FCM + the WorkManager fallback). On
 * Android 8+ a channel's sound is immutable, so every category has its own channel keyed by its
 * effective sound; switching a sound recreates that channel. Stored only on this device — no
 * Firestore, Cloud Functions, or backend.
 *
 * Notifications shown by the system while the app is killed follow the FCM payload's channel,
 * not this selection (that would require a server change, intentionally out of scope).
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

/**
 * Notification categories mirror the Phase 13B server notification types. GENERAL is the app-wide
 * default; the [overridable] categories may each pick their own sound or follow the default.
 */
enum class NotificationCategory(val key: String, val channelName: String) {
    GENERAL("general", "Mithaq"),
    INTEREST("interest", "Interest requests"),
    PHOTO_REQUEST("photo_request", "Photo requests"),
    CHAT_REQUEST("chat_request", "Chat requests"),
    MESSAGE("message", "Messages"),
    PHOTO_MODERATION("photo_review", "Photo review");

    companion object {
        fun fromType(type: String?): NotificationCategory = when (type) {
            "interest_request" -> INTEREST
            "photo_request" -> PHOTO_REQUEST
            "chat_request" -> CHAT_REQUEST
            "chat_message" -> MESSAGE
            "photo_approved", "photo_rejected" -> PHOTO_MODERATION
            else -> GENERAL
        }

        val overridable: List<NotificationCategory> =
            listOf(INTEREST, PHOTO_REQUEST, CHAT_REQUEST, MESSAGE, PHOTO_MODERATION)
    }
}

object NotificationSoundPreferences {
    private const val PREFS = "mithaq_notification_sound"
    private const val KEY_DEFAULT = "selected_sound"
    private const val CHANNEL_PREFIX = "mithaq_"

    // Retired channel ids from earlier versions, removed so System settings stays clean.
    private val LEGACY_CHANNEL_IDS = listOf(
        "mithaq_messages_channel_v2",
        "mithaq_alerts_channel_v4",
        "mithaq_messages_snd_default",
        "mithaq_messages_snd_chime",
        "mithaq_messages_snd_ding",
        "mithaq_messages_snd_soft",
        "mithaq_messages_snd_silent"
    )

    // ── App-wide default (GENERAL) ──────────────────────────────────────────────
    fun getSelected(context: Context): NotificationSound {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return NotificationSound.fromKey(prefs.getString(KEY_DEFAULT, NotificationSound.DEFAULT.key))
    }

    fun setSelected(context: Context, sound: NotificationSound) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_DEFAULT, sound.key)
            .apply()
        ensureAllChannels(context)
    }

    // ── Per-category override ────────────────────────────────────────────────────
    private fun overrideKey(category: NotificationCategory) = "sound_${category.key}"

    /** The override for [category], or null when it follows the app default. GENERAL never overrides. */
    fun getOverride(context: Context, category: NotificationCategory): NotificationSound? {
        if (category == NotificationCategory.GENERAL) return null
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(overrideKey(category), null)
        return raw?.let { NotificationSound.fromKey(it) }
    }

    /** Set ([sound] non-null) or clear ([sound] == null → follow default) the override for [category]. */
    fun setOverride(context: Context, category: NotificationCategory, sound: NotificationSound?) {
        if (category == NotificationCategory.GENERAL) {
            if (sound != null) setSelected(context, sound)
            return
        }
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (sound == null) editor.remove(overrideKey(category)) else editor.putString(overrideKey(category), sound.key)
        editor.apply()
        ensureAllChannels(context)
    }

    /** The effective sound for [category]: its override, else the app default. */
    fun effectiveSound(context: Context, category: NotificationCategory): NotificationSound =
        getOverride(context, category) ?: getSelected(context)

    fun soundUri(context: Context, sound: NotificationSound): Uri? = when (sound) {
        NotificationSound.SILENT -> null
        NotificationSound.DEFAULT -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        else -> sound.rawResId?.let { Uri.parse("android.resource://${context.packageName}/$it") }
    }

    private fun channelId(category: NotificationCategory, sound: NotificationSound): String =
        "$CHANNEL_PREFIX${category.key}_${sound.key}"

    /** Ensures the channel for [category] (with its effective sound) exists and returns its id. */
    fun ensureChannel(context: Context, category: NotificationCategory): String {
        val sound = effectiveSound(context, category)
        val activeId = channelId(category, sound)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return activeId
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return activeId

        // A channel's sound is immutable, so remove this category's channels for other sounds.
        for (other in NotificationSound.entries) {
            val id = channelId(category, other)
            if (id != activeId) nm.deleteNotificationChannel(id)
        }

        val channel = NotificationChannel(activeId, category.channelName, NotificationManager.IMPORTANCE_HIGH).apply {
            enableLights(true)
            enableVibration(true)
            setShowBadge(true)
            setLockscreenVisibility(Notification.VISIBILITY_PUBLIC)
            vibrationPattern = longArrayOf(0, 500, 200, 500)
            val uri = soundUri(context, sound)
            if (uri == null) {
                setSound(null, null)
            } else {
                setSound(
                    uri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
        }
        nm.createNotificationChannel(channel)
        return activeId
    }

    /** Ensures every category channel and removes retired legacy channels. Call on startup + changes. */
    fun ensureAllChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        for (legacy in LEGACY_CHANNEL_IDS) nm.deleteNotificationChannel(legacy)
        for (category in NotificationCategory.entries) ensureChannel(context, category)
    }
}
