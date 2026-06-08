package com.mithaq.app.ui.match

import com.mithaq.app.model.Gender
import com.mithaq.app.model.PrayerFrequency
import com.mithaq.app.model.Sect
import com.mithaq.app.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchScoreCalculatorTest {
    @Test
    fun identicalCoreProfilesScoreHigh() {
        val user = testProfile(uid = "user-a", age = 28)
        val partner = testProfile(uid = "user-b", gender = Gender.FEMALE, age = 30)

        val score = MatchScoreCalculator.calculateScore(user, partner)

        assertTrue(score >= 90)
    }

    @Test
    fun scoreIsClampedToValidPercentageRange() {
        val score = MatchScoreCalculator.calculateScore(
            testProfile(uid = "user-a", age = 18),
            testProfile(uid = "user-b", gender = Gender.FEMALE, age = 77, prayerFrequency = PrayerFrequency.NEVER)
        )

        assertTrue(score in 0..100)
    }

    @Test
    fun prayerMismatchReducesScore() {
        val base = testProfile(uid = "user-a")
        val aligned = testProfile(uid = "user-b", gender = Gender.FEMALE)
        val mismatched = aligned.copy(prayerFrequency = PrayerFrequency.NEVER)

        assertTrue(
            MatchScoreCalculator.calculateScore(base, aligned) >
                MatchScoreCalculator.calculateScore(base, mismatched)
        )
    }

    @Test
    fun sameSectScoresHigherThanDifferentSect() {
        val base = testProfile(uid = "user-a", sect = Sect.SUNNI)
        val sameSect = testProfile(uid = "user-b", gender = Gender.FEMALE, sect = Sect.SUNNI)
        val otherSect = testProfile(uid = "user-c", gender = Gender.FEMALE, sect = Sect.SHIA)

        // Sect contributes 20 vs 5 with everything else held equal -> a 15-point gap.
        assertEquals(
            15,
            MatchScoreCalculator.calculateScore(base, sameSect) -
                MatchScoreCalculator.calculateScore(base, otherSect)
        )
    }

    @Test
    fun closerAgeScoresAtLeastAsHighAsFartherAge() {
        val base = testProfile(uid = "user-a", age = 30)
        val close = testProfile(uid = "user-b", gender = Gender.FEMALE, age = 32) // diff 2 -> 5 pts
        val far = testProfile(uid = "user-c", gender = Gender.FEMALE, age = 45)   // diff 15 -> 1 pt

        assertTrue(
            MatchScoreCalculator.calculateScore(base, close) >
                MatchScoreCalculator.calculateScore(base, far)
        )
    }

    @Test
    fun scoreNeverExceeds100ForMaximallyAlignedProfiles() {
        val a = testProfile(uid = "user-a", age = 30)
        val b = testProfile(uid = "user-b", gender = Gender.FEMALE, age = 30)
        assertTrue(MatchScoreCalculator.calculateScore(a, b) <= 100)
    }

    @Test
    fun completelyOppositeProfilesStayWithinRange() {
        val a = testProfile(uid = "user-a", age = 20, sect = Sect.SUNNI, prayerFrequency = PrayerFrequency.ALWAYS)
        val b = testProfile(
            uid = "user-b",
            gender = Gender.FEMALE,
            age = 60,
            sect = Sect.SHIA,
            prayerFrequency = PrayerFrequency.NEVER
        ).copy(religiousValues = "not_religious", languagesSpoken = emptyList())

        assertTrue(MatchScoreCalculator.calculateScore(a, b) in 0..100)
    }

    private fun testProfile(
        uid: String,
        gender: Gender = Gender.MALE,
        age: Int = 29,
        prayerFrequency: PrayerFrequency = PrayerFrequency.ALWAYS,
        sect: Sect = Sect.SUNNI
    ): UserProfile {
        return UserProfile(
            uid = uid,
            gender = gender,
            age = age,
            sect = sect,
            prayerFrequency = prayerFrequency,
            religiousValues = "religious",
            languagesSpoken = listOf("Arabic", "English"),
            interestsEntertainments = listOf("reading"),
            interestsSports = listOf("walking"),
            interestsFoods = listOf("halal")
        )
    }
}
