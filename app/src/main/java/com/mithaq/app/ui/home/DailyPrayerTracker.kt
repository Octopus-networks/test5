package com.mithaq.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mithaq.app.model.UserProfile
import com.mithaq.app.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DailyPrayerTracker(
    currentUser: UserProfile,
    isArabic: Boolean,
    onPrayerToggled: (String, Boolean) -> Unit
) {
    // Determine today's date string
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    
    // Derive checked states from independent daily prayer booleans
    val fajrChecked = currentUser.fajrPrayedToday
    val dhuhrChecked = currentUser.dhuhrPrayedToday
    val asrChecked = currentUser.asrPrayedToday
    val maghribChecked = currentUser.maghribPrayedToday
    val ishaChecked = currentUser.ishaPrayedToday

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mosque,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isArabic) "متتبع الصلوات الخمس" else "Daily Prayer Tracker",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isArabic) "حافظ على صلواتك وشاركها مع من تتواصل معهم." else "Track your prayers and notify your matches.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PrayerItem(name = if (isArabic) "الفجر" else "Fajr", isChecked = fajrChecked) { 
                    onPrayerToggled("Fajr", it) 
                }
                PrayerItem(name = if (isArabic) "الظهر" else "Dhuhr", isChecked = dhuhrChecked) { 
                    onPrayerToggled("Dhuhr", it) 
                }
                PrayerItem(name = if (isArabic) "العصر" else "Asr", isChecked = asrChecked) { 
                    onPrayerToggled("Asr", it) 
                }
                PrayerItem(name = if (isArabic) "المغرب" else "Maghrib", isChecked = maghribChecked) { 
                    onPrayerToggled("Maghrib", it) 
                }
                PrayerItem(name = if (isArabic) "العشاء" else "Isha", isChecked = ishaChecked) { 
                    onPrayerToggled("Isha", it) 
                }
            }

        }
    }
}

@Composable
fun PrayerItem(
    name: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = { onCheckedChange(!isChecked) },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                contentDescription = name,
                tint = if (isChecked) SuccessGreen else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(32.dp)
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
            color = if (isChecked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
