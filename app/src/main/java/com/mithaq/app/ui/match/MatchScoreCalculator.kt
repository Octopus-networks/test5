package com.mithaq.app.ui.match

import com.mithaq.app.model.ModestyPreference
import com.mithaq.app.model.PrayerFrequency
import com.mithaq.app.model.RelocationWillingness
import com.mithaq.app.model.UserProfile
import kotlin.math.abs

/**
 * Utility class for Feature 2: Smart Compatibility Match Score.
 * Computes compatibility percentage (0 to 100) between two user profiles.
 */
object MatchScoreCalculator {

    /**
     * Calculates compatibility score based on crucial Islamic and lifestyle values.
     * Weights:
     * - Sect Compatibility: 30%
     * - Prayer Consistency: 30%
     * - Modesty/Lifestyle Values: 20%
     * - Relocation Agreement: 10%
     * - Age compatibility: 10%
     */
    fun calculateScore(profileA: UserProfile, profileB: UserProfile): Int {
        var score = 0

        // ================= HIGH IMPORTANCE (50 PTS TOTAL) =================
        // 1. Sect Compatibility (20 pts)
        if (profileA.sect == profileB.sect) {
            score += 20
        } else {
            score += 5
        }

        // 2. Prayer Consistency (15 pts)
        val prayerDiff = abs(profileA.prayerFrequency.ordinal - profileB.prayerFrequency.ordinal)
        score += when (prayerDiff) {
            0 -> 15
            1 -> 10
            2 -> 5
            else -> 0
        }

        // 3. Religious Values alignment (15 pts)
        if (profileA.religiousValues == profileB.religiousValues) {
            score += 15
        } else if (profileA.religiousValues == "religious" && profileB.religiousValues == "very_religious") {
            score += 10
        } else if (profileA.religiousValues == "religious" && profileB.religiousValues == "not_religious") {
            score += 5
        } else {
            score += 2
        }

        // ================= MEDIUM IMPORTANCE (35 PTS TOTAL) =================
        // 4. Common Languages Spoken (10 pts)
        val commonLangs = profileA.languagesSpoken.intersect(profileB.languagesSpoken.toSet())
        if (commonLangs.isNotEmpty() || (profileA.languagesSpoken.isEmpty() && profileB.languagesSpoken.isEmpty())) {
            score += 10
        } else {
            score += 3
        }

        // 5. Relocation & Polygamy (10 pts)
        val relA = profileA.relocationWillingness
        val relB = profileB.relocationWillingness
        val relScore = if (relA == relB) 5 else if (relA == RelocationWillingness.OPEN || relB == RelocationWillingness.OPEN) 4 else 1
        val polyScore = if (profileA.polygamyAcceptance == profileB.polygamyAcceptance) 5 else 1
        score += (relScore + polyScore)

        // 6. Family Values alignment (8 pts)
        if (profileA.familyValue == profileB.familyValue) {
            score += 8
        } else if (profileA.familyValue == "moderate" || profileB.familyValue == "moderate") {
            score += 5
        } else {
            score += 1
        }

        // 7. Marital Status (7 pts)
        if (profileA.maritalStatus == profileB.maritalStatus) {
            score += 7
        } else {
            score += 3
        }

        // ================= LOW IMPORTANCE (15 PTS TOTAL) =================
        // 8. Shared Interests (5 pts)
        val sharedEntertainments = profileA.interestsEntertainments.intersect(profileB.interestsEntertainments.toSet())
        val sharedSports = profileA.interestsSports.intersect(profileB.interestsSports.toSet())
        val sharedFoods = profileA.interestsFoods.intersect(profileB.interestsFoods.toSet())
        val overlapCount = sharedEntertainments.size + sharedSports.size + sharedFoods.size
        score += when {
            overlapCount >= 3 -> 5
            overlapCount >= 1 -> 3
            else -> 1
        }

        // 9. Age Proximity (5 pts)
        val ageDiff = abs(profileA.age - profileB.age)
        score += when {
            ageDiff <= 3 -> 5
            ageDiff <= 6 -> 4
            ageDiff <= 10 -> 2
            else -> 1
        }

        // 10. Height & Weight compatibility (5 pts)
        val heightDiff = abs(profileA.height - profileB.height)
        score += when {
            heightDiff <= 15 -> 5
            heightDiff <= 25 -> 3
            else -> 1
        }

        return score.coerceIn(0, 100)
    }
}
