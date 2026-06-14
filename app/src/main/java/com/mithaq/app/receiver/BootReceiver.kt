package com.mithaq.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.os.UserManagerCompat
import com.mithaq.app.notification.NotificationSyncWorker
import com.mithaq.app.util.AdhanScheduler

/**
 * Restores Adhan scheduling after boot, app updates, and local clock changes.
 *
 * Handles:
 *  - BOOT_COMPLETED / LOCKED_BOOT_COMPLETED
 *  - MY_PACKAGE_REPLACED
 *  - TIME_SET / TIMEZONE_CHANGED
 *
 * Adhan alarms are restored for every supported action. Notification sync is restarted only
 * after boot. Credential-protected preferences are never read before the user unlocks.
 *
 * Force Stop prevents manifest receivers from running until the user launches the app again.
 * A device reboot does not bypass that Android stopped-package behavior.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        private val BOOT_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED
        )
        private val ADHAN_RESCHEDULE_ACTIONS = BOOT_ACTIONS + setOf(
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in ADHAN_RESCHEDULE_ACTIONS) return

        if (action in BOOT_ACTIONS) {
            Log.i(TAG, "Device booted (action=$action). Restoring background services...")
        } else {
            Log.i(TAG, "System schedule event (action=$action). Restoring Adhan schedule...")
        }

        if (!UserManagerCompat.isUserUnlocked(context)) {
            Log.i(TAG, "User storage is locked; deferring credential-protected restore until unlock.")
            return
        }

        // mithaq_prefs is credential-protected and is safe to access only after the guard above.
        val prefs = context.getSharedPreferences("mithaq_prefs", Context.MODE_PRIVATE)

        // 1. Reschedule Adhan alarms for every supported system action.
        val isAdhanEnabled = prefs.getBoolean("isAdhanEnabled", false)
        val lat = prefs.getFloat(
            "adhan_lat",
            prefs.getFloat("adhanLocationLat", 0.0f)
        ).toDouble()
        val lng = prefs.getFloat(
            "adhan_lng",
            prefs.getFloat("adhanLocationLng", 0.0f)
        ).toDouble()
        val calculationMethod = prefs.getString(
            "adhan_calculation_method",
            "MUSLIM_WORLD_LEAGUE"
        )
        val soundPattern = prefs.getString("adhan_sound_pattern", "TAKBEER")

        if (isAdhanEnabled && (lat != 0.0 || lng != 0.0)) {
            Log.i(TAG, "Rescheduling Adhan for ($lat, $lng)...")
            AdhanScheduler.scheduleNextAdhan(
                context,
                lat,
                lng,
                calculationMethod,
                soundPattern
            )
        } else {
            Log.d(TAG, "Adhan disabled or no saved location - skipping reschedule.")
        }

        if (action !in BOOT_ACTIONS) return

        // 2. Restart WorkManager notification sync only after boot.
        val authPrefs = context.getSharedPreferences("mithaq_mock_auth", Context.MODE_PRIVATE)
        val savedUid = authPrefs.getString("uid", null)
            ?: prefs.getString("last_logged_in_uid", null)

        if (savedUid != null) {
            Log.i(TAG, "Restarting NotificationSyncWorker for uid=$savedUid")
            NotificationSyncWorker.schedule(context)
        } else {
            Log.d(TAG, "No logged-in user found - skipping WorkManager reschedule.")
        }
    }
}
