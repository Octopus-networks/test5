package com.mithaq.app.util

import com.mithaq.app.model.ChatRoom
import com.mithaq.app.model.UserProfile
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PrayerRewardManager {

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Updates personal user prayer stats when a user checks/unchecks a prayer.
     * Handles daily, weekly, and monthly counters and resets.
     */
    fun onUserPrayerToggled(
        profile: UserProfile,
        prayerName: String,
        isChecked: Boolean
    ): UserProfile {
        val cal = Calendar.getInstance()
        val currentMillis = cal.timeInMillis

        var fD = profile.fajrPrayedToday
        var dD = profile.dhuhrPrayedToday
        var aD = profile.asrPrayedToday
        var mD = profile.maghribPrayedToday
        var iD = profile.ishaPrayedToday
        
        var fW = profile.fajrWeeklyCount
        var dW = profile.dhuhrWeeklyCount
        var aW = profile.asrWeeklyCount
        var mW = profile.maghribWeeklyCount
        var iW = profile.ishaWeeklyCount
        
        var fM = profile.fajrMonthlyCount
        var dM = profile.dhuhrMonthlyCount
        var aM = profile.asrMonthlyCount
        var mM = profile.maghribMonthlyCount
        var iM = profile.ishaMonthlyCount

        // 1. Check for resets
        val lastDateStr = if (profile.lastPrayerDate > 0) sdf.format(Date(profile.lastPrayerDate)) else ""
        val todayStr = sdf.format(Date(currentMillis))

        if (lastDateStr != todayStr) {
            // New day, reset daily
            fD = false
            dD = false
            aD = false
            mD = false
            iD = false
            
            // Check for weekly reset
            val lastCal = Calendar.getInstance().apply { timeInMillis = profile.lastWeeklyResetDate }
            if (profile.lastWeeklyResetDate == 0L || 
                cal.get(Calendar.WEEK_OF_YEAR) != lastCal.get(Calendar.WEEK_OF_YEAR) ||
                cal.get(Calendar.YEAR) != lastCal.get(Calendar.YEAR)) {
                fW = 0
                dW = 0
                aW = 0
                mW = 0
                iW = 0
            }

            // Check for monthly reset
            val lastMonthCal = Calendar.getInstance().apply { timeInMillis = profile.lastMonthlyResetDate }
            if (profile.lastMonthlyResetDate == 0L ||
                cal.get(Calendar.MONTH) != lastMonthCal.get(Calendar.MONTH) ||
                cal.get(Calendar.YEAR) != lastMonthCal.get(Calendar.YEAR)) {
                fM = 0
                dM = 0
                aM = 0
                mM = 0
                iM = 0
            }
        }

        // 2. Adjust counters based on toggle
        if (prayerName == "AdhanToggle") {
            // Only update the adhan toggle boolean, do not increment counters
            return profile.copy(isAdhanEnabled = isChecked)
        }
        
        when (prayerName.lowercase(Locale.ROOT)) {
            "fajr" -> { fD = isChecked; if(isChecked) {fW++; fM++} else { if(fW>0) fW--; if(fM>0) fM-- } }
            "dhuhr" -> { dD = isChecked; if(isChecked) {dW++; dM++} else { if(dW>0) dW--; if(dM>0) dM-- } }
            "asr" -> { aD = isChecked; if(isChecked) {aW++; aM++} else { if(aW>0) aW--; if(aM>0) aM-- } }
            "maghrib" -> { mD = isChecked; if(isChecked) {mW++; mM++} else { if(mW>0) mW--; if(mM>0) mM-- } }
            "isha" -> { iD = isChecked; if(isChecked) {iW++; iM++} else { if(iW>0) iW--; if(iM>0) iM-- } }
        }

        // Determine reset dates to save
        val newLastWeeklyReset = if (profile.lastWeeklyResetDate == 0L) currentMillis else profile.lastWeeklyResetDate
        val newLastMonthlyReset = if (profile.lastMonthlyResetDate == 0L) currentMillis else profile.lastMonthlyResetDate

        return profile.copy(
            fajrPrayedToday = fD, dhuhrPrayedToday = dD, asrPrayedToday = aD, maghribPrayedToday = mD, ishaPrayedToday = iD,
            fajrWeeklyCount = fW, dhuhrWeeklyCount = dW, asrWeeklyCount = aW, maghribWeeklyCount = mW, ishaWeeklyCount = iW,
            fajrMonthlyCount = fM, dhuhrMonthlyCount = dM, asrMonthlyCount = aM, maghribMonthlyCount = mM, ishaMonthlyCount = iM,
            lastPrayerDate = currentMillis,
            lastWeeklyResetDate = newLastWeeklyReset,
            lastMonthlyResetDate = newLastMonthlyReset
        )
    }

    /**
     * Called when a user checks off a prayer in a chat context.
     * Updates the chat room's daily prayers and streaks.
     * Returns true if a 10-day mutual streak is achieved.
     */
    fun onPrayerCompleted(
        chatRoom: ChatRoom,
        userId: String,
        prayerName: String
    ): ChatRoom {
        val todayStr = sdf.format(Date())
        
        // 1. Update Daily Prayers
        val currentDaily = chatRoom.dailyPrayers.toMutableMap()
        val userPrayers = currentDaily[userId]?.toMutableList() ?: mutableListOf()
        if (!userPrayers.contains(prayerName)) {
            userPrayers.add(prayerName)
        }
        currentDaily[userId] = userPrayers
        
        // 2. Check if all 5 prayers are completed by this user today
        val hasCompletedAllPrayers = userPrayers.size >= 5
        
        // 3. Update Streak if all prayers are completed
        val currentStreaks = chatRoom.prayerStreaks.toMutableMap()
        if (hasCompletedAllPrayers) {
            val currentStreak = currentStreaks[userId] ?: 0
            // Assuming this is checked only once per day per user for simplicity
            currentStreaks[userId] = currentStreak + 1
        }
        
        // 4. Check for mutual 10-day streak
        var rewardUnlocked = chatRoom.goldRewardUnlocked
        if (!rewardUnlocked) {
            val user1 = chatRoom.memberIds.getOrNull(0) ?: ""
            val user2 = chatRoom.memberIds.getOrNull(1) ?: ""
            val streak1 = currentStreaks[user1] ?: 0
            val streak2 = currentStreaks[user2] ?: 0
            
            if (streak1 >= 10 && streak2 >= 10) {
                rewardUnlocked = true
            }
        }

        return chatRoom.copy(
            dailyPrayers = currentDaily,
            prayerStreaks = currentStreaks,
            goldRewardUnlocked = rewardUnlocked
        )
    }

    /**
     * Call this when rewardUnlocked becomes true to grant the Gold membership.
     */
    fun grantGoldMembership(userProfile: UserProfile): UserProfile {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 10) // 10 days free
        return userProfile.copy(
            isPremium = true,
            subscriptionPlan = "Gold (Prayer Reward)",
            premiumExpiry = cal.timeInMillis
        )
    }
}
