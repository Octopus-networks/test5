package com.mithaq.app.util

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

data class DailyPrayerTimes(
    val fajr: Date,
    val sunrise: Date,
    val dhuhr: Date,
    val asr: Date,
    val maghrib: Date,
    val isha: Date
)

object PrayerManager {

    // Predefined approximate coordinates for major capitals to be used as a fallback.
    // In a production app, we would use the user's actual GPS location.
    private val capitalCoordinates = mapOf(
        "Saudi Arabia" to Coordinates(24.7136, 46.6753), // Riyadh
        "السعودية" to Coordinates(24.7136, 46.6753), // Riyadh
        "Egypt" to Coordinates(30.0444, 31.2357), // Cairo
        "مصر" to Coordinates(30.0444, 31.2357), // Cairo
        "UAE" to Coordinates(24.4539, 54.3773), // Abu Dhabi
        "United Arab Emirates" to Coordinates(24.4539, 54.3773), // Abu Dhabi
        "الإمارات" to Coordinates(24.4539, 54.3773), // Abu Dhabi
        "Jordan" to Coordinates(31.9454, 35.9284), // Amman
        "الأردن" to Coordinates(31.9454, 35.9284), // Amman
        "Syria" to Coordinates(33.5138, 36.2765), // Damascus
        "سوريا" to Coordinates(33.5138, 36.2765), // Damascus
        "Yemen" to Coordinates(15.3694, 44.1910), // Sana'a
        "اليمن" to Coordinates(15.3694, 44.1910), // Sana'a
        "Morocco" to Coordinates(34.0209, -6.8416), // Rabat
        "المغرب" to Coordinates(34.0209, -6.8416), // Rabat
        "United States" to Coordinates(38.9072, -77.0369), // Washington D.C.
        "United Kingdom" to Coordinates(51.5074, -0.1278), // London
        // Default to Mecca
        "Other" to Coordinates(21.4225, 39.8262)
    )

    fun getCoordinatesForCountry(countryName: String): Coordinates {
        return capitalCoordinates[countryName] ?: Coordinates(21.4225, 39.8262) // Mecca fallback
    }

    fun getDailyPrayerTimes(countryName: String, date: Date = Date()): DailyPrayerTimes {
        val coordinates = getCoordinatesForCountry(countryName)
        val cal = Calendar.getInstance()
        cal.time = date
        val dateComponents = DateComponents.from(date)
        
        // Use standard Muslim World League method for calculation
        val parameters = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
        parameters.madhab = Madhab.SHAFI

        val prayerTimes = PrayerTimes(coordinates, dateComponents, parameters)
        
        return DailyPrayerTimes(
            fajr = prayerTimes.fajr,
            sunrise = prayerTimes.sunrise,
            dhuhr = prayerTimes.dhuhr,
            asr = prayerTimes.asr,
            maghrib = prayerTimes.maghrib,
            isha = prayerTimes.isha
        )
    }

    fun isPrayerTimePassed(prayerTime: Date): Boolean {
        return Date().after(prayerTime)
    }
}
