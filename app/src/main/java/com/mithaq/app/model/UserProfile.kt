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
    val fcmToken: String? = null,
    val isAdmin: Boolean = false,
    val isPremium: Boolean = false,
    val subscriptionPlan: String = "FREE",
    val questionnaireAnswers: Map<String, String> = emptyMap()
)

enum class Gender(val displayNameEn: String, val displayNameAr: String) {
    MALE("Male", "ذكر"),
    FEMALE("Female", "أنثى");
    fun getDisplayName(isArabic: Boolean): String = if (isArabic) displayNameAr else displayNameEn
}

enum class Sect(val displayNameEn: String, val displayNameAr: String) {
    SUNNI("Sunni", "سني"),
    SHIA("Shia", "شيعي"),
    IBADI("Ibadi", "إباضي"),
    OTHER("Other", "أخرى");

    val displayName: String get() = displayNameEn
    fun getDisplayName(isArabic: Boolean): String = if (isArabic) displayNameAr else displayNameEn
}

enum class PrayerFrequency(val displayNameEn: String, val displayNameAr: String) {
    ALWAYS("Always (5 times daily)", "دائماً (٥ فروض يومياً)"),
    USUALLY("Usually", "غالباً"),
    SOMETIMES("Sometimes", "أحياناً"),
    NEVER("Never", "أبداً");

    val displayName: String get() = displayNameEn
    fun getDisplayName(isArabic: Boolean): String = if (isArabic) displayNameAr else displayNameEn
}

enum class ModestyPreference(val displayNameEn: String, val displayNameAr: String) {
    NONE("No Hijab / Open", "بدون حجاب / غير مقيد"),
    HIJAB("Hijab", "حجاب"),
    NIQAB("Niqab", "نقاب"),
    DOES_NOT_MATTER("Doesn't Matter", "لا يهم");

    val displayName: String get() = displayNameEn
    fun getDisplayName(isArabic: Boolean): String = if (isArabic) displayNameAr else displayNameEn
}

enum class RelocationWillingness(val displayNameEn: String, val displayNameAr: String) {
    YES("Willing to relocate", "مستعد للانتقال"),
    NO("Not willing to relocate", "غير مستعد للانتقال"),
    OPEN("Open to discussion", "قابل للنقاش");

    val displayName: String get() = displayNameEn
    fun getDisplayName(isArabic: Boolean): String = if (isArabic) displayNameAr else displayNameEn
}
