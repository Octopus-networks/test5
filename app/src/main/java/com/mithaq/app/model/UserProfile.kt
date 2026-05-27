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
    val additionalImages: List<String> = emptyList(),
    
    // New Onboarding Questionnaire fields
    val username: String = "",
    val oathChecked: Boolean = false,
    val skinColor: String = "",
    val healthStatus: List<String> = emptyList(),
    val nationality: String = "",
    val educationLevel: String = "",
    val jobTitle: String = "",
    val incomeLevel: String = "",
    val fastingHabit: String = "",
    val weddingTimeline: String = "",
    val wifeWorking: String = "",
    val householdExpenses: String = "",
    val aymaView: String = "",
    val shabkaView: String = "",
    val gpsLocationEnabled: Boolean = false,
    val blurPictures: Boolean = true,
    
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
    val premiumExpiry: Long = 0L,
    val questionnaireAnswers: Map<String, String> = emptyMap(),

    // --- Muslima Extended Upgrades ---
    val profileCreator: String = "self", // "self", "parent", "brother_sister", "relative", "friend"
    val regionalCode: String = "MUS",    // "MUS", "SAC", "IDC", "AFR", "INT", "IRC", "MLS"
    
    // Appearance
    val height: Int = 170, // 140–220 cm
    val weight: Int = 70,  // 40–220 kg
    val bodyType: String = "average", // petite, slim, athletic, average, extra_pounds, full_figured, large
    val hairColor: String = "black",  // bald, black, blonde, brown, grey, red
    val eyeColor: String = "brown",   // black, blue, brown, green, grey, hazel
    val ethnicity: String = "arab_middle_eastern",
    val appearanceRating: String = "average",

    // Lifestyle
    val maritalStatus: String = "single", // single, separated, widowed, divorced
    val haveChildren: String = "no",      // no, yes_live_at_home, yes_sometimes, not_at_home
    val numberOfChildren: Int = 0,
    val wantMoreChildren: String = "not_sure", // yes, not_sure, no
    val livingSituation: String = "with_family", // alone, friends, family, kids, spouse
    val occupation: String = "employed",
    val employmentStatus: String = "full_time",
    val annualIncome: String = "no_say",
    val drinkStatus: String = "dont_drink",
    val smokeStatus: String = "dont_smoke",
    val eatingHabit: String = "halal_always", // halal_always, halal_when_possible, no_restrictions
    val relationshipLookingFor: String = "marriage", // marriage, friendship

    // Islamic & Cultural Values
    val religiousValues: String = "religious", // very_religious, religious, not_religious
    val attendReligiousService: String = "sometimes", // daily, jummah, sometimes, ramadan, never
    val readQuran: String = "occasionally", // daily, occasionally, ramadan, jummah, translated, never
    val wearHijab: String = "no", // yes, sometimes, no
    val wearNiqab: String = "no", // yes, sometimes, no
    val convertStatus: String = "born_muslim", // born_muslim, reverted, planning_to_revert
    val familyValue: String = "moderate", // conservative, moderate, liberal

    // Languages & Interests
    val languagesSpoken: List<String> = emptyList(),
    val interestsEntertainments: List<String> = emptyList(),
    val interestsSports: List<String> = emptyList(),
    val interestsFoods: List<String> = emptyList(),
    val interestsMusics: List<String> = emptyList(),

    // Personality Text Fields
    val aboutYourself: String = "",
    val partnerPreferences: String = "",
    val profileHeading: String = "",
    val idealPartner: String = "",
    val lastSeen: Long = 0L,
    
    // Phase 2: Islamic Identity & Trust
    val seriousnessScore: Int = 0,
    val istikharaCount: Int = 0,

    // Phase 3: Prayer Tracking
    val timezone: String = "Asia/Riyadh",
    val currentStreakDays: Int = 0
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
