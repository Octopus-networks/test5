package com.mithaq.app.data.repository

import com.mithaq.app.domain.model.PublicProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression guards for [premiumWeightedInterleave] — the discovery exposure logic that
 * gives premium members a 2:1 weighting against free members (PREMIUM_WEIGHT = 2) without
 * dropping or duplicating anyone.
 */
class PremiumInterleaveTest {

    private fun profile(id: String, premium: Boolean) =
        PublicProfile(userId = id, isPremium = premium)

    @Test
    fun preservesEveryProfileExactlyOnce() {
        val input = listOf(
            profile("p1", true), profile("p2", true), profile("p3", true),
            profile("f1", false), profile("f2", false)
        )

        val result = input.premiumWeightedInterleave()

        assertEquals(input.size, result.size)
        assertEquals(input.map { it.userId }.toSet(), result.map { it.userId }.toSet())
    }

    @Test
    fun weightsPremiumTwoToOneAtTheFront() {
        val input = listOf(
            profile("p1", true), profile("p2", true), profile("p3", true), profile("p4", true),
            profile("f1", false), profile("f2", false)
        )

        val order = input.premiumWeightedInterleave().map { it.userId }

        // Pattern is two premium then one free, repeating.
        assertEquals(listOf("p1", "p2", "f1", "p3", "p4", "f2"), order)
    }

    @Test
    fun allFreeIsReturnedUnchanged() {
        val input = listOf(profile("f1", false), profile("f2", false))
        assertEquals(input, input.premiumWeightedInterleave())
    }

    @Test
    fun allPremiumIsReturnedUnchanged() {
        val input = listOf(profile("p1", true), profile("p2", true))
        assertEquals(input, input.premiumWeightedInterleave())
    }

    @Test
    fun emptyListIsSafe() {
        assertTrue(emptyList<PublicProfile>().premiumWeightedInterleave().isEmpty())
    }
}
