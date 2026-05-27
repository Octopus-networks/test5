import re

file_path = r'c:\New folder (2)\app\src\main\java\com\mithaq\app\ui\auth\AuthViewModel.kt'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix 1: Reading detailed stats from SharedPreferences in fetchCurrentUserProfile
old_reading = """                    val isAdhanEnabled = prefs.getBoolean("isAdhanEnabled", false)
                    val adhanLocationLat = prefs.getFloat("adhanLocationLat", 0.0f).toDouble()
                    val adhanLocationLng = prefs.getFloat("adhanLocationLng", 0.0f).toDouble()
                    val dailyPrayerCount = prefs.getInt("dailyPrayerCount", 0)
                    val weeklyPrayerCount = prefs.getInt("weeklyPrayerCount", 0)
                    val monthlyPrayerCount = prefs.getInt("monthlyPrayerCount", 0)
                    val lastPrayerDate = prefs.getLong("lastPrayerDate", 0L)
                    val lastWeeklyResetDate = prefs.getLong("lastWeeklyResetDate", 0L)
                    val lastMonthlyResetDate = prefs.getLong("lastMonthlyResetDate", 0L)"""

new_reading = """                    val isAdhanEnabled = prefs.getBoolean("isAdhanEnabled", false)
                    val adhanLocationLat = prefs.getFloat("adhanLocationLat", 0.0f).toDouble()
                    val adhanLocationLng = prefs.getFloat("adhanLocationLng", 0.0f).toDouble()
                    
                    val fajrPrayedToday = prefs.getBoolean("fajrPrayedToday", false)
                    val fajrWeeklyCount = prefs.getInt("fajrWeeklyCount", 0)
                    val fajrMonthlyCount = prefs.getInt("fajrMonthlyCount", 0)
                    val dhuhrPrayedToday = prefs.getBoolean("dhuhrPrayedToday", false)
                    val dhuhrWeeklyCount = prefs.getInt("dhuhrWeeklyCount", 0)
                    val dhuhrMonthlyCount = prefs.getInt("dhuhrMonthlyCount", 0)
                    val asrPrayedToday = prefs.getBoolean("asrPrayedToday", false)
                    val asrWeeklyCount = prefs.getInt("asrWeeklyCount", 0)
                    val asrMonthlyCount = prefs.getInt("asrMonthlyCount", 0)
                    val maghribPrayedToday = prefs.getBoolean("maghribPrayedToday", false)
                    val maghribWeeklyCount = prefs.getInt("maghribWeeklyCount", 0)
                    val maghribMonthlyCount = prefs.getInt("maghribMonthlyCount", 0)
                    val ishaPrayedToday = prefs.getBoolean("ishaPrayedToday", false)
                    val ishaWeeklyCount = prefs.getInt("ishaWeeklyCount", 0)
                    val ishaMonthlyCount = prefs.getInt("ishaMonthlyCount", 0)
                    
                    val lastPrayerDate = prefs.getLong("lastPrayerDate", 0L)
                    val lastWeeklyResetDate = prefs.getLong("lastWeeklyResetDate", 0L)
                    val lastMonthlyResetDate = prefs.getLong("lastMonthlyResetDate", 0L)"""

content = content.replace(old_reading, new_reading)

# Fix 2: Setting the parsed variables to the UserProfile constructor
old_constructor = """                            isAdhanEnabled = false,
                            adhanLocationLat = 0.0,
                            adhanLocationLng = 0.0,
                            adhanCalculationMethod = "MUSLIM_WORLD_LEAGUE",
                            adhanSoundPattern = "TAKBEER",
                            fajrPrayedToday = false, fajrWeeklyCount = 0, fajrMonthlyCount = 0,
                            dhuhrPrayedToday = false, dhuhrWeeklyCount = 0, dhuhrMonthlyCount = 0,
                            asrPrayedToday = false, asrWeeklyCount = 0, asrMonthlyCount = 0,
                            maghribPrayedToday = false, maghribWeeklyCount = 0, maghribMonthlyCount = 0,
                            ishaPrayedToday = false, ishaWeeklyCount = 0, ishaMonthlyCount = 0,
                            lastPrayerDate = 0L,
                            lastWeeklyResetDate = 0L,
                            lastMonthlyResetDate = 0L,"""

new_constructor = """                            isAdhanEnabled = isAdhanEnabled,
                            adhanLocationLat = adhanLocationLat,
                            adhanLocationLng = adhanLocationLng,
                            adhanCalculationMethod = "MUSLIM_WORLD_LEAGUE",
                            adhanSoundPattern = "TAKBEER",
                            fajrPrayedToday = fajrPrayedToday, fajrWeeklyCount = fajrWeeklyCount, fajrMonthlyCount = fajrMonthlyCount,
                            dhuhrPrayedToday = dhuhrPrayedToday, dhuhrWeeklyCount = dhuhrWeeklyCount, dhuhrMonthlyCount = dhuhrMonthlyCount,
                            asrPrayedToday = asrPrayedToday, asrWeeklyCount = asrWeeklyCount, asrMonthlyCount = asrMonthlyCount,
                            maghribPrayedToday = maghribPrayedToday, maghribWeeklyCount = maghribWeeklyCount, maghribMonthlyCount = maghribMonthlyCount,
                            ishaPrayedToday = ishaPrayedToday, ishaWeeklyCount = ishaWeeklyCount, ishaMonthlyCount = ishaMonthlyCount,
                            lastPrayerDate = lastPrayerDate,
                            lastWeeklyResetDate = lastWeeklyResetDate,
                            lastMonthlyResetDate = lastMonthlyResetDate,"""

content = content.replace(old_constructor, new_constructor)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("AuthViewModel.kt updated.")
