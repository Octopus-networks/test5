package com.mithaq.app.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mithaq.app.R
import com.mithaq.app.model.UserProfile
import com.mithaq.app.util.PrayerRewardManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

class PrayerLoggedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_LOG_PRAYER) return
        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: return
        val notificationId = intent.getIntExtra("NOTIFICATION_ID", -1)
        val channelId = intent.getStringExtra("CHANNEL_ID") ?: return

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (currentUser == null) {
            if (notificationId != -1) {
                notificationManager.cancel(notificationId)
            }
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val uid = currentUser.uid
                val snapshot = firestore.collection("users").document(uid).get().await()
                if (snapshot.exists()) {
                    val profile = snapshot.toObject(UserProfile::class.java)
                    if (profile != null) {
                        val formattedPrayerName = prayerName.lowercase(Locale.ROOT).replaceFirstChar { 
                            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() 
                        }
                        val updatedProfile = PrayerRewardManager.onUserPrayerToggled(
                            profile = profile,
                            prayerName = formattedPrayerName,
                            isChecked = true
                        )
                        firestore.collection("users").document(uid).update(
                            mapOf(
                                "fajrPrayedToday" to updatedProfile.fajrPrayedToday,
                                "dhuhrPrayedToday" to updatedProfile.dhuhrPrayedToday,
                                "asrPrayedToday" to updatedProfile.asrPrayedToday,
                                "maghribPrayedToday" to updatedProfile.maghribPrayedToday,
                                "ishaPrayedToday" to updatedProfile.ishaPrayedToday,
                                "fajrWeeklyCount" to updatedProfile.fajrWeeklyCount,
                                "dhuhrWeeklyCount" to updatedProfile.dhuhrWeeklyCount,
                                "asrWeeklyCount" to updatedProfile.asrWeeklyCount,
                                "maghribWeeklyCount" to updatedProfile.maghribWeeklyCount,
                                "ishaWeeklyCount" to updatedProfile.ishaWeeklyCount,
                                "fajrMonthlyCount" to updatedProfile.fajrMonthlyCount,
                                "dhuhrMonthlyCount" to updatedProfile.dhuhrMonthlyCount,
                                "asrMonthlyCount" to updatedProfile.asrMonthlyCount,
                                "maghribMonthlyCount" to updatedProfile.maghribMonthlyCount,
                                "ishaMonthlyCount" to updatedProfile.ishaMonthlyCount,
                                "lastPrayerDate" to updatedProfile.lastPrayerDate,
                                "lastWeeklyResetDate" to updatedProfile.lastWeeklyResetDate,
                                "lastMonthlyResetDate" to updatedProfile.lastMonthlyResetDate
                            )
                        ).await()
                    }
                }
                
                // Update notification
                if (notificationId != -1) {
                    val prefs = context.getSharedPreferences("mithaq_prefs", Context.MODE_PRIVATE)
                    val lang = prefs.getString("app_language", Locale.getDefault().language)
                    val isArabic = lang?.startsWith("ar") == true
                    
                    val title = if (isArabic) "تقبل الله ✓" else "May Allah accept it ✓"
                    
                    val builder = NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(title)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setSilent(true)
                        
                    notificationManager.notify(notificationId, builder.build())
                }
            } catch (e: Exception) {
                Log.e("PrayerLoggedReceiver", "Failed to log prayer", e)
                if (notificationId != -1) {
                    notificationManager.cancel(notificationId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_LOG_PRAYER = "com.mithaq.app.action.LOG_PRAYER"
    }
}
