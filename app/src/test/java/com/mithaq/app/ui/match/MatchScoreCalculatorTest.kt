package com.mithaq.app.ui.match

import com.mithaq.app.model.Gender
import com.mithaq.app.model.PrayerFrequency
import com.mithaq.app.model.UserProfile
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

    private fun testProfile(
        uid: String,
        gender: Gender = Gender.MALE,
        age: Int = 29,
        prayerFrequency: PrayerFrequency = PrayerFrequency.ALWAYS
    ): UserProfile {
        return UserProfile(
            uid = uid,
            gender = gender,
            age = age,
            prayerFrequency = prayerFrequency,
            religiousValues = "religious",
            languagesSpoken = listOf("Arabic", "English"),
            interestsEntertainments = listOf("reading"),
            interestsSports = listOf("walking"),
            interestsFoods = listOf("halal")
        )
    }
}
