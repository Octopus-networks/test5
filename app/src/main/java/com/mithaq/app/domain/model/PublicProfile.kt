package com.mithaq.app.domain.model

import java.util.Date

data class PublicProfile(
    val userId: String = "",
    val displayName: String = "",
    val age: Int? = null,
    val city: String = "",
    val country: String = "",
    val accountType: String = "",
    val maritalStatus: String = "",
    val marriageTimeline: String = "",
    val prayerHabitPublicLabel: String = "",
    val prayerRoutineShared: Boolean = false,
    val localTimeEnabled: Boolean = false,
    val hasGuardian: Boolean = false,
    val isEmailVerified: Boolean = false,
    val isIdentityVerified: Boolean = false,
    val isIncognito: Boolean = false,
    val photoPrivacyMode: String = "blurred_by_default",
    val profileCompletionPercent: Int = 0,
    val lastActiveAt: Date? = null,
    val updatedAt: Date? = null
)
