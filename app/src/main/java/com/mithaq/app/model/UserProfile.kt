package com.mithaq.app.model

/**
 * Data class representing a User's Profile in the Mithaq app.
 * Contains critical demographic, religious, and lifestyle details necessary for Islamic matchmaking.
 */
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val gender: Gender = Gender.MALE,
    val age: Int = 18,
    val city: String = "",
    val country: String = "",
    val imageUrl: String = "",
    
    // Islamic & Lifestyle Parameters
    val sect: Sect = Sect.SUNNI,
    val prayerFrequency: PrayerFrequency = PrayerFrequency.ALWAYS,
    val modestyPreference: ModestyPreference = ModestyPreference.HIJAB,
    val relocationWillingness: RelocationWillingness = RelocationWillingness.OPEN,
    val polygamyAcceptance: Boolean = false,
    
    // Guardian (Wali) Fields
    val guardianName: String? = null,
    val guardianEmail: String? = null,
    val guardianStatus: String? = null, // "None", "Pending", "Verified"

    // Photo modesty locks
    val photoAccessApprovedUsers: List<String> = emptyList(),
    val photoAccessRequests: List<String> = emptyList(),

    // Premium Security & Trust Upgrades
    val isWaliAccount: Boolean = false,
    val wardUid: String? = null,
    val verificationStatus: String = "NONE", // "NONE", "PENDING", "VERIFIED"
    val voiceIntroUrl: String? = null,
    val fcmToken: String? = null
)

enum class Gender {
    MALE, FEMALE
}

enum class Sect(val displayName: String) {
    SUNNI("Sunni"),
    SHIA("Shia"),
    IBADI("Ibadi"),
    OTHER("Other")
}

enum class PrayerFrequency(val displayName: String) {
    ALWAYS("Always (5 times daily)"),
    USUALLY("Usually"),
    SOMETIMES("Sometimes"),
    NEVER("Never")
}

enum class ModestyPreference(val displayName: String) {
    NONE("No Hijab / Open"),
    HIJAB("Hijab"),
    NIQAB("Niqab"),
    DOES_NOT_MATTER("Doesn't Matter")
}

enum class RelocationWillingness(val displayName: String) {
    YES("Willing to relocate"),
    NO("Not willing to relocate"),
    OPEN("Open to discussion")
}
