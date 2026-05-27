import re

file_path = r'c:\New folder (2)\app\src\main\java\com\mithaq\app\ui\filter\SearchViewModel.kt'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Add stats for mock_user_2
old_fatima = """                            questionnaireAnswers = mapOf(
                                "q1" to "opt1", "q2" to "opt1", "q3" to "opt2", "q4" to "opt4", "q5" to "opt1",
                                "q6" to "opt1", "q7" to "opt2", "q8" to "opt1", "q9" to "opt2", "q10" to "opt1"
                            )"""
new_fatima = """                            questionnaireAnswers = mapOf(
                                "q1" to "opt1", "q2" to "opt1", "q3" to "opt2", "q4" to "opt4", "q5" to "opt1",
                                "q6" to "opt1", "q7" to "opt2", "q8" to "opt1", "q9" to "opt2", "q10" to "opt1"
                            ),
                            fajrPrayedToday = true, fajrWeeklyCount = 5, fajrMonthlyCount = 20,
                            dhuhrPrayedToday = true, dhuhrWeeklyCount = 6, dhuhrMonthlyCount = 25,
                            asrPrayedToday = true, asrWeeklyCount = 6, asrMonthlyCount = 22,
                            maghribPrayedToday = true, maghribWeeklyCount = 7, maghribMonthlyCount = 28,
                            ishaPrayedToday = true, ishaWeeklyCount = 7, ishaMonthlyCount = 28"""
content = content.replace(old_fatima, new_fatima)

# Add stats for mock_user_3
old_ahmad = """                            questionnaireAnswers = mapOf(
                                "q1" to "opt1", "q2" to "opt1", "q3" to "opt2", "q4" to "opt1", "q5" to "opt2",
                                "q6" to "opt2", "q7" to "opt1", "q8" to "opt2", "q9" to "opt1", "q10" to "opt2"
                            )"""
new_ahmad = """                            questionnaireAnswers = mapOf(
                                "q1" to "opt1", "q2" to "opt1", "q3" to "opt2", "q4" to "opt1", "q5" to "opt2",
                                "q6" to "opt2", "q7" to "opt1", "q8" to "opt2", "q9" to "opt1", "q10" to "opt2"
                            ),
                            fajrPrayedToday = false, fajrWeeklyCount = 1, fajrMonthlyCount = 5,
                            dhuhrPrayedToday = true, dhuhrWeeklyCount = 2, dhuhrMonthlyCount = 10,
                            asrPrayedToday = false, asrWeeklyCount = 2, asrMonthlyCount = 9,
                            maghribPrayedToday = true, maghribWeeklyCount = 3, maghribMonthlyCount = 12,
                            ishaPrayedToday = false, ishaWeeklyCount = 3, ishaMonthlyCount = 11"""
content = content.replace(old_ahmad, new_ahmad)

# Add stats for mock_user_4
old_sarah = """                            guardianName = "Omar / عمر",
                            guardianEmail = "omar@mithaq.com",
                            guardianStatus = "PENDING",
                            verificationStatus = "PENDING"
                        )"""
new_sarah = """                            guardianName = "Omar / عمر",
                            guardianEmail = "omar@mithaq.com",
                            guardianStatus = "PENDING",
                            verificationStatus = "PENDING",
                            fajrPrayedToday = true, fajrWeeklyCount = 4, fajrMonthlyCount = 15,
                            dhuhrPrayedToday = true, dhuhrWeeklyCount = 5, dhuhrMonthlyCount = 18,
                            asrPrayedToday = true, asrWeeklyCount = 5, asrMonthlyCount = 17,
                            maghribPrayedToday = true, maghribWeeklyCount = 6, maghribMonthlyCount = 21,
                            ishaPrayedToday = true, ishaWeeklyCount = 6, ishaMonthlyCount = 20
                        )"""
content = content.replace(old_sarah, new_sarah)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("SearchViewModel.kt updated.")
