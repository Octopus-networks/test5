package com.mithaq.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Prayer
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.mithaq.app.MainActivity
import com.mithaq.app.receiver.AdhanReceiver
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AdhanScheduler {
    private const val TAG = "AdhanScheduler"
    private const val PREFS_NAME = "mithaq_prefs"
    private const val ALARM_REQUEST_CODE = 1001
    private const val SHOW_ALARM_REQUEST_CODE = 1002

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    fun scheduleNextAdhan(
        context: Context,
        lat: Double,
        lng: Double,
        calculationMethod: String? = null,
        soundPattern: String? = null
    ): Boolean {
        if (lat == 0.0 && lng == 0.0) {
            Log.w(TAG, "Cannot schedule Adhan without valid coordinates.")
            return false
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!canScheduleExactAlarms(context)) {
            Log.w(TAG, "Cannot schedule exact alarms. Permission denied.")
            return false
        }

        val coordinates = Coordinates(lat, lng)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val resolvedCalculationMethod = calculationMethod
            ?: prefs.getString("adhan_calculation_method", "MUSLIM_WORLD_LEAGUE")
            ?: "MUSLIM_WORLD_LEAGUE"
        val resolvedSoundPattern = soundPattern
            ?: prefs.getString("adhan_sound_pattern", "TAKBEER")
            ?: "TAKBEER"
        prefs.edit()
            .putBoolean("isAdhanEnabled", true)
            .putFloat("adhan_lat", lat.toFloat())
            .putFloat("adhan_lng", lng.toFloat())
            .putFloat("adhanLocationLat", lat.toFloat())
            .putFloat("adhanLocationLng", lng.toFloat())
            .putString("adhan_calculation_method", resolvedCalculationMethod)
            .putString("adhan_sound_pattern", resolvedSoundPattern)
            .putString("adhanCalculationMethod", resolvedCalculationMethod)
            .putString("adhanSoundPattern", resolvedSoundPattern)
            .apply()

        val today = DateComponents.from(Date())
        val parameters = calculationParametersFor(resolvedCalculationMethod)

        val prayerTimes = PrayerTimes(coordinates, today, parameters)
        var nextAdhan = nextPrayerTime(prayerTimes)

        // If all prayers for today have passed, calculate for tomorrow
        if (nextAdhan == null) {
            val tomorrow = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            val tomorrowComponents = DateComponents.from(tomorrow.time)
            val tomorrowPrayerTimes = PrayerTimes(coordinates, tomorrowComponents, parameters)
            nextAdhan = Prayer.FAJR to tomorrowPrayerTimes.timeForPrayer(Prayer.FAJR)
        }

        val nextPrayer = nextAdhan.first
        val nextPrayerTime = nextAdhan.second
        val intent = Intent(context, AdhanReceiver::class.java).apply {
            action = AdhanReceiver.ACTION_ADHAN_ALARM
            putExtra("PRAYER_NAME", nextPrayer.name)
            putExtra("LAT", lat)
            putExtra("LNG", lng)
            putExtra("CALCULATION_METHOD", resolvedCalculationMethod)
            putExtra("SOUND_PATTERN", resolvedSoundPattern)
        }

        // Use mutability flag properly for Android 12+
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            SHOW_ALARM_REQUEST_CODE,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            val alarmClockInfo = AlarmManager.AlarmClockInfo(
                nextPrayerTime.time,
                showPendingIntent
            )
            alarmManager.setAlarmClock(
                alarmClockInfo,
                pendingIntent
            )
            Log.d(TAG, "Scheduled next Adhan: ${nextPrayer.name} at $nextPrayerTime")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Exact alarm permission missing", e)
            false
        }
    }
    
    fun cancelAdhan(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AdhanReceiver::class.java).apply {
            action = AdhanReceiver.ACTION_ADHAN_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("isAdhanEnabled", false)
            .apply()
        Log.d(TAG, "Cancelled Adhan alarms")
    }

    private fun nextPrayerTime(prayerTimes: PrayerTimes): Pair<Prayer, Date>? {
        val now = Date()
        return listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)
            .mapNotNull { prayer ->
                prayerTimes.timeForPrayer(prayer)?.let { prayerTime -> prayer to prayerTime }
            }
            .firstOrNull { (_, prayerTime) -> prayerTime.after(now) }
    }

    private fun calculationParametersFor(method: String): CalculationParameters {
        val enumName = when (method.uppercase(Locale.ROOT)) {
            "ISNA" -> "NORTH_AMERICA"
            "GULF" -> "DUBAI"
            else -> method.uppercase(Locale.ROOT)
        }
        val calculationMethod = runCatching {
            CalculationMethod.valueOf(enumName)
        }.getOrDefault(CalculationMethod.MUSLIM_WORLD_LEAGUE)
        return calculationMethod.parameters
    }
}
