package com.mithaq.app.model

/**
 * Data class representing a Chat Room session in the Mithaq app.
 * Supports chaperonage configuration which flags whether a Wali receives transcripts.
 */
data class ChatRoom(
    val roomId: String = "",
    val memberIds: List<String> = emptyList(),
    val isChaperoned: Boolean = false,
    val waliEmail: String? = null,
    val lastMessage: String? = null,
    val lastMessageTimestamp: Long = 0L,
    
    // Prayer Tracking & Mutual Rewards (Phase 3)
    val dailyPrayers: Map<String, List<String>> = emptyMap(), // map of uid -> list of completed prayers today (e.g. ["fajr", "dhuhr"])
    val prayerStreaks: Map<String, Int> = emptyMap(), // map of uid -> consecutive days completed 5 prayers
    val goldRewardUnlocked: Boolean = false
)
