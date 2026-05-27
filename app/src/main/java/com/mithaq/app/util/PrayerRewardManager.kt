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
     * Called when a user checks off a prayer.
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
