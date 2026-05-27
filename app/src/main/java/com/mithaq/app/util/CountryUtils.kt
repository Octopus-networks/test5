package com.mithaq.app.util

data class CountryTimezone(
    val nameEn: String,
    val nameAr: String,
    val timezone: String
)

object CountryUtils {
    // A comprehensive list of countries and their primary timezone
    // (for countries with multiple timezones, we select the capital's timezone)
    val countries = listOf(
        CountryTimezone("Afghanistan", "أفغانستان", "Asia/Kabul"),
        CountryTimezone("Albania", "ألبانيا", "Europe/Tirane"),
        CountryTimezone("Algeria", "الجزائر", "Africa/Algiers"),
        CountryTimezone("Andorra", "أندورا", "Europe/Andorra"),
        CountryTimezone("Angola", "أنغولا", "Africa/Luanda"),
        CountryTimezone("Argentina", "الأرجنتين", "America/Argentina/Buenos_Aires"),
        CountryTimezone("Armenia", "أرمينيا", "Asia/Yerevan"),
        CountryTimezone("Australia", "أستراليا", "Australia/Sydney"),
        CountryTimezone("Austria", "النمسا", "Europe/Vienna"),
        CountryTimezone("Azerbaijan", "أذربيجان", "Asia/Baku"),
        CountryTimezone("Bahrain", "البحرين", "Asia/Bahrain"),
        CountryTimezone("Bangladesh", "بنغلاديش", "Asia/Dhaka"),
        CountryTimezone("Belarus", "بيلاروسيا", "Europe/Minsk"),
        CountryTimezone("Belgium", "بلجيكا", "Europe/Brussels"),
        CountryTimezone("Bolivia", "بوليفيا", "America/La_Paz"),
        CountryTimezone("Bosnia and Herzegovina", "البوسنة والهرسك", "Europe/Sarajevo"),
        CountryTimezone("Brazil", "البرازيل", "America/Sao_Paulo"),
        CountryTimezone("Bulgaria", "بلغاريا", "Europe/Sofia"),
        CountryTimezone("Canada", "كندا", "America/Toronto"),
        CountryTimezone("Chile", "تشيلي", "America/Santiago"),
        CountryTimezone("China", "الصين", "Asia/Shanghai"),
        CountryTimezone("Colombia", "كولومبيا", "America/Bogota"),
        CountryTimezone("Croatia", "كرواتيا", "Europe/Zagreb"),
        CountryTimezone("Cyprus", "قبرص", "Asia/Nicosia"),
        CountryTimezone("Czech Republic", "جمهورية التشيك", "Europe/Prague"),
        CountryTimezone("Denmark", "الدنمارك", "Europe/Copenhagen"),
        CountryTimezone("Djibouti", "جيبوتي", "Africa/Djibouti"),
        CountryTimezone("Egypt", "مصر", "Africa/Cairo"),
        CountryTimezone("Eritrea", "إريتريا", "Africa/Asmara"),
        CountryTimezone("Ethiopia", "إثيوبيا", "Africa/Addis_Ababa"),
        CountryTimezone("Finland", "فنلندا", "Europe/Helsinki"),
        CountryTimezone("France", "فرنسا", "Europe/Paris"),
        CountryTimezone("Germany", "ألمانيا", "Europe/Berlin"),
        CountryTimezone("Greece", "اليونان", "Europe/Athens"),
        CountryTimezone("Hungary", "المجر", "Europe/Budapest"),
        CountryTimezone("Iceland", "آيسلندا", "Atlantic/Reykjavik"),
        CountryTimezone("India", "الهند", "Asia/Kolkata"),
        CountryTimezone("Indonesia", "إندونيسيا", "Asia/Jakarta"),
        CountryTimezone("Iran", "إيران", "Asia/Tehran"),
        CountryTimezone("Iraq", "العراق", "Asia/Baghdad"),
        CountryTimezone("Ireland", "أيرلندا", "Europe/Dublin"),
        CountryTimezone("Italy", "إيطاليا", "Europe/Rome"),
        CountryTimezone("Japan", "اليابان", "Asia/Tokyo"),
        CountryTimezone("Jordan", "الأردن", "Asia/Amman"),
        CountryTimezone("Kazakhstan", "كازاخستان", "Asia/Almaty"),
        CountryTimezone("Kenya", "كينيا", "Africa/Nairobi"),
        CountryTimezone("Kuwait", "الكويت", "Asia/Kuwait"),
        CountryTimezone("Lebanon", "لبنان", "Asia/Beirut"),
        CountryTimezone("Libya", "ليبيا", "Africa/Tripoli"),
        CountryTimezone("Malaysia", "ماليزيا", "Asia/Kuala_Lumpur"),
        CountryTimezone("Maldives", "المالديف", "Indian/Maldives"),
        CountryTimezone("Mali", "مالي", "Africa/Bamako"),
        CountryTimezone("Mauritania", "موريتانيا", "Africa/Nouakchott"),
        CountryTimezone("Mexico", "المكسيك", "America/Mexico_City"),
        CountryTimezone("Morocco", "المغرب", "Africa/Casablanca"),
        CountryTimezone("Netherlands", "هولندا", "Europe/Amsterdam"),
        CountryTimezone("New Zealand", "نيوزيلندا", "Pacific/Auckland"),
        CountryTimezone("Nigeria", "نيجيريا", "Africa/Lagos"),
        CountryTimezone("Norway", "النرويج", "Europe/Oslo"),
        CountryTimezone("Oman", "عمان", "Asia/Muscat"),
        CountryTimezone("Pakistan", "باكستان", "Asia/Karachi"),
        CountryTimezone("Palestine", "فلسطين", "Asia/Gaza"),
        CountryTimezone("Peru", "بيرو", "America/Lima"),
        CountryTimezone("Philippines", "الفلبين", "Asia/Manila"),
        CountryTimezone("Poland", "بولندا", "Europe/Warsaw"),
        CountryTimezone("Portugal", "البرتغال", "Europe/Lisbon"),
        CountryTimezone("Qatar", "قطر", "Asia/Qatar"),
        CountryTimezone("Romania", "رومانيا", "Europe/Bucharest"),
        CountryTimezone("Russia", "روسيا", "Europe/Moscow"),
        CountryTimezone("Saudi Arabia", "السعودية", "Asia/Riyadh"),
        CountryTimezone("Senegal", "السنغال", "Africa/Dakar"),
        CountryTimezone("Serbia", "صربيا", "Europe/Belgrade"),
        CountryTimezone("Singapore", "سنغافورة", "Asia/Singapore"),
        CountryTimezone("Somalia", "الصومال", "Africa/Mogadishu"),
        CountryTimezone("South Africa", "جنوب أفريقيا", "Africa/Johannesburg"),
        CountryTimezone("South Korea", "كوريا الجنوبية", "Asia/Seoul"),
        CountryTimezone("Spain", "إسبانيا", "Europe/Madrid"),
        CountryTimezone("Sri Lanka", "سريلانكا", "Asia/Colombo"),
        CountryTimezone("Sudan", "السودان", "Africa/Khartoum"),
        CountryTimezone("Sweden", "السويد", "Europe/Stockholm"),
        CountryTimezone("Switzerland", "سويسرا", "Europe/Zurich"),
        CountryTimezone("Syria", "سوريا", "Asia/Damascus"),
        CountryTimezone("Tajikistan", "طاجيكستان", "Asia/Dushanbe"),
        CountryTimezone("Tanzania", "تنزانيا", "Africa/Dar_es_Salaam"),
        CountryTimezone("Thailand", "تايلاند", "Asia/Bangkok"),
        CountryTimezone("Tunisia", "تونس", "Africa/Tunis"),
        CountryTimezone("Turkey", "تركيا", "Europe/Istanbul"),
        CountryTimezone("Turkmenistan", "تركمانستان", "Asia/Ashgabat"),
        CountryTimezone("Uganda", "أوغندا", "Africa/Kampala"),
        CountryTimezone("Ukraine", "أوكرانيا", "Europe/Kiev"),
        CountryTimezone("United Arab Emirates", "الإمارات العربية المتحدة", "Asia/Dubai"),
        CountryTimezone("United Kingdom", "المملكة المتحدة", "Europe/London"),
        CountryTimezone("United States", "الولايات المتحدة", "America/New_York"),
        CountryTimezone("Uzbekistan", "أوزبكستان", "Asia/Tashkent"),
        CountryTimezone("Venezuela", "فنزويلا", "America/Caracas"),
        CountryTimezone("Vietnam", "فيتنام", "Asia/Ho_Chi_Minh"),
        CountryTimezone("Yemen", "اليمن", "Asia/Aden"),
        CountryTimezone("Other", "أخرى", "UTC")
    )
    
    fun getDisplayName(countryTimezone: CountryTimezone, isArabic: Boolean): String {
        return if (isArabic) countryTimezone.nameAr else countryTimezone.nameEn
    }

    fun getTimezoneForCountry(countryName: String): String {
        return countries.find { it.nameEn == countryName || it.nameAr == countryName }?.timezone ?: "UTC"
    }

    fun formatLocalTime(timezoneId: String, isArabic: Boolean): String {
        return try {
            val tz = java.util.TimeZone.getTimeZone(timezoneId)
            val format = java.text.SimpleDateFormat("hh:mm a", if (isArabic) java.util.Locale("ar") else java.util.Locale.US)
            format.timeZone = tz
            format.format(java.util.Date())
        } catch (e: Exception) {
            "--:--"
        }
    }
}
