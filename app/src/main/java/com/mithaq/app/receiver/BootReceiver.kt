package com.mithaq.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mithaq.app.notification.NotificationSyncWorker
import com.mithaq.app.util.AdhanScheduler

/**
 * BootReceiver — يستيقظ تلقائياً عند إعادة تشغيل الجهاز.
 *
 * يتعامل مع:
 *  - BOOT_COMPLETED      → الإطلاق العادي بعد إعادة التشغيل
 *  - LOCKED_BOOT_COMPLETED → الإطلاق المبكر (Android 7+) قبل رفع قفل الشاشة
 *
 * يُعيد جدولة:
 *  1. منبهات الأذان (AdhanScheduler)
 *  2. مزامنة الإشعارات (WorkManager / NotificationSyncWorker)
 *
 * ملاحظة: Force Stop يمنع هذا الـ Receiver من العمل حتى يفتح المستخدم التطبيق
 * مجدداً — وهو قيد مقصود من Android لحماية المستخدم. إعادة التشغيل لا تُعاني
 * من هذا القيد لأن حالة Force Stop تُمسح تلقائياً عند إعادة الإقلاع.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) return

        Log.i(TAG, "Device booted (action=$action). Restoring background services...")

        val prefs = context.getSharedPreferences("mithaq_prefs", Context.MODE_PRIVATE)

        // ── 1. Reschedule Adhan alarms ────────────────────────────────────────
        val isAdhanEnabled = prefs.getBoolean("isAdhanEnabled", false)
        val lat = prefs.getFloat("adhan_lat",
            prefs.getFloat("adhanLocationLat", 0.0f)
        ).toDouble()
        val lng = prefs.getFloat("adhan_lng",
            prefs.getFloat("adhanLocationLng", 0.0f)
        ).toDouble()
        val calculationMethod = prefs.getString(
            "adhan_calculation_method", "MUSLIM_WORLD_LEAGUE"
        )
        val soundPattern = prefs.getString("adhan_sound_pattern", "TAKBEER")

        if (isAdhanEnabled && (lat != 0.0 || lng != 0.0)) {
            Log.i(TAG, "Rescheduling Adhan for ($lat, $lng)...")
            AdhanScheduler.scheduleNextAdhan(context, lat, lng, calculationMethod, soundPattern)
        } else {
            Log.d(TAG, "Adhan disabled or no saved location — skipping reschedule.")
        }

        // ── 2. Restart WorkManager notification sync ─────────────────────────
        // Only schedule if a user was previously logged in
        val authPrefs = context.getSharedPreferences("mithaq_mock_auth", Context.MODE_PRIVATE)
        val savedUid = authPrefs.getString("uid", null)
            ?: context.getSharedPreferences("mithaq_prefs", Context.MODE_PRIVATE)
                .getString("last_logged_in_uid", null)

        if (savedUid != null) {
            Log.i(TAG, "Restarting NotificationSyncWorker for uid=$savedUid")
            NotificationSyncWorker.schedule(context)
        } else {
            Log.d(TAG, "No logged-in user found — skipping WorkManager reschedule.")
        }
    }
}
