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

        var newDaily = profile.dailyPrayerCount
        var newWeekly = profile.weeklyPrayerCount
        var newMonthly = profile.monthlyPrayerCount

        // 1. Check for resets
        val lastDateStr = if (profile.lastPrayerDate > 0) sdf.format(Date(profile.lastPrayerDate)) else ""
        val todayStr = sdf.format(Date(currentMillis))

        if (lastDateStr != todayStr) {
            // New day, reset daily
            newDaily = 0
            
            // Check for weekly reset (different week of year or different year)
            val lastCal = Calendar.getInstance().apply { timeInMillis = profile.lastWeeklyResetDate }
            if (profile.lastWeeklyResetDate == 0L || 
                cal.get(Calendar.WEEK_OF_YEAR) != lastCal.get(Calendar.WEEK_OF_YEAR) ||
                cal.get(Calendar.YEAR) != lastCal.get(Calendar.YEAR)) {
                newWeekly = 0
            }

            // Check for monthly reset
            val lastMonthCal = Calendar.getInstance().apply { timeInMillis = profile.lastMonthlyResetDate }
            if (profile.lastMonthlyResetDate == 0L ||
                cal.get(Calendar.MONTH) != lastMonthCal.get(Calendar.MONTH) ||
                cal.get(Calendar.YEAR) != lastMonthCal.get(Calendar.YEAR)) {
                newMonthly = 0
            }
        }

        // 2. Adjust counters based on toggle
        if (prayerName == "AdhanToggle") {
            // Only update the adhan toggle boolean, do not increment counters
            return profile.copy(isAdhanEnabled = isChecked)
        }
        
        if (isChecked) {
            newDaily++
            newWeekly++
            newMonthly++
        } else {
            if (newDaily > 0) newDaily--
            if (newWeekly > 0) newWeekly--
            if (newMonthly > 0) newMonthly--
        }

        // Clamp values to max caps
        newDaily = newDaily.coerceIn(0, 5)
        newWeekly = newWeekly.coerceIn(0, 35)
        newMonthly = newMonthly.coerceIn(0, 155) // Approx max

        // Determine reset dates to save
        val newLastWeeklyReset = if (newWeekly == 1 && isChecked) currentMillis else if (profile.lastWeeklyResetDate == 0L) currentMillis else profile.lastWeeklyResetDate
        val newLastMonthlyReset = if (newMonthly == 1 && isChecked) currentMillis else if (profile.lastMonthlyResetDate == 0L) currentMillis else profile.lastMonthlyResetDate

        return profile.copy(
            dailyPrayerCount = newDaily,
            weeklyPrayerCount = newWeekly,
            monthlyPrayerCount = newMonthly,
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
