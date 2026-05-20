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

        // 1. Sect Compatibility (30 pts)
        if (profileA.sect == profileB.sect) {
            score += 30
        } else {
            // Different sects get partial points if either is open (represented by Sect.OTHER in this simple logic)
            // Or 10 points for minor theological proximity, otherwise 0.
            score += 10
        }

        // 2. Prayer Consistency (30 pts)
        val prayerDiff = abs(profileA.prayerFrequency.ordinal - profileB.prayerFrequency.ordinal)
        score += when (prayerDiff) {
            0 -> 30 // Identical prayer habits (e.g., both "Always" or both "Usually")
            1 -> 20 // Near identical (e.g. "Always" and "Usually")
            2 -> 10 // Discrepancy (e.g. "Usually" and "Sometimes")
            else -> 0 // Major gap (e.g. "Always" and "Never")
        }

        // 3. Modesty / Hijab Preferences (20 pts)
        // If modesty preference aligns or matches, add points.
        if (profileA.modestyPreference == profileB.modestyPreference) {
            score += 20
        } else if (profileA.modestyPreference == ModestyPreference.DOES_NOT_MATTER || 
                   profileB.modestyPreference == ModestyPreference.DOES_NOT_MATTER) {
            score += 15 // Partial points for flexible preferences
        } else {
            // E.g., one wears Niqab, other prefers Hijab
            score += 5
        }

        // 4. Relocation Agreement (10 pts)
        val relA = profileA.relocationWillingness
        val relB = profileB.relocationWillingness
        if (relA == relB) {
            score += 10
        } else if (relA == RelocationWillingness.OPEN || relB == RelocationWillingness.OPEN) {
            score += 8  // One of them is flexible
        } else {
            score += 2  // Contradicting relocation willingness (one wants to relocate, one doesn't)
        }

        // 5. Age Proximity (10 pts)
        val ageDiff = abs(profileA.age - profileB.age)
        score += when {
            ageDiff <= 3 -> 10
            ageDiff <= 6 -> 8
            ageDiff <= 10 -> 5
            else -> 2
        }

        // Bound final score between 0 and 100
        return score.coerceIn(0, 100)
    }
}
