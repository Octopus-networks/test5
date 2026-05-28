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
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.mithaq.app.receiver.AdhanReceiver
import java.util.Calendar
import java.util.Date

object AdhanScheduler {
    private const val TAG = "AdhanScheduler"

    fun scheduleNextAdhan(context: Context, lat: Double, lng: Double) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms. Permission denied.")
                return
            }
        }

        val coordinates = Coordinates(lat, lng)
        val prefs = context.getSharedPreferences("mithaq_prefs", Context.MODE_PRIVATE)
        prefs.edit().putFloat("adhan_lat", lat.toFloat()).putFloat("adhan_lng", lng.toFloat()).apply()

        val today = DateComponents.from(Date())
        // Try Muslim World League calculation, since it's a good default
        val parameters = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
        
        val prayerTimes = PrayerTimes(coordinates, today, parameters)
        var nextPrayer = prayerTimes.nextPrayer()
        var nextPrayerTime = prayerTimes.timeForPrayer(nextPrayer)

        // If all prayers for today have passed, calculate for tomorrow
        if (nextPrayer == com.batoulapps.adhan.Prayer.NONE || nextPrayerTime == null) {
            val tomorrow = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            val tomorrowComponents = DateComponents.from(tomorrow.time)
            val tomorrowPrayerTimes = PrayerTimes(coordinates, tomorrowComponents, parameters)
            
            nextPrayer = com.batoulapps.adhan.Prayer.FAJR
            nextPrayerTime = tomorrowPrayerTimes.timeForPrayer(nextPrayer)
        }

        if (nextPrayerTime != null) {
            val intent = Intent(context, AdhanReceiver::class.java).apply {
                action = AdhanReceiver.ACTION_ADHAN_ALARM
                putExtra("PRAYER_NAME", nextPrayer.name)
                putExtra("LAT", lat)
                putExtra("LNG", lng)
            }
            
            // Use mutability flag properly for Android 12+
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Schedule exact alarm using setAlarmClock (bypasses Doze mode and guarantees precision)
            try {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(
                    nextPrayerTime.time,
                    pendingIntent // We use the same intent for the "show intent" to keep it simple
                )
                alarmManager.setAlarmClock(
                    alarmClockInfo,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled next Adhan: ${nextPrayer.name} at $nextPrayerTime")
            } catch (e: SecurityException) {
                Log.e(TAG, "Exact alarm permission missing", e)
            }
        }
    }
    
    fun cancelAdhan(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AdhanReceiver::class.java).apply {
            action = AdhanReceiver.ACTION_ADHAN_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled Adhan alarms")
    }
}
