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
    val polygamyAcceptance: Boolean? = null, // null means doesn't matter
    val country: String = "",
    val city: String = "",
    
    // New fields
    val maritalStatuses: Set<String> = emptySet(),
    val minHeight: Int = 140,
    val maxHeight: Int = 220,
    val haveChildren: Set<String> = emptySet(),
    val religiousValues: Set<String> = emptySet()
)


