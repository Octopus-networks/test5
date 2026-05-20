package com.mithaq.app.model

/**
 * Data class representing the search filter criteria selected by a user.
 */
data class FilterCriteria(
    val minAge: Int = 18,
    val maxAge: Int = 70,
    val sect: Sect? = null,
    val prayerFrequencies: Set<PrayerFrequency> = emptySet(),
    val modestyPreferences: Set<ModestyPreference> = emptySet(),
    val relocationWillingness: Set<RelocationWillingness> = emptySet(),
    val polygamyAcceptance: Boolean? = null // null means doesn't matter
)
