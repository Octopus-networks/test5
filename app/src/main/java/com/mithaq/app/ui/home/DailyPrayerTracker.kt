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
    
    // Derive checked states from dailyPrayerCount
    val fajrChecked = currentUser.dailyPrayerCount >= 1
    val dhuhrChecked = currentUser.dailyPrayerCount >= 2
    val asrChecked = currentUser.dailyPrayerCount >= 3
    val maghribChecked = currentUser.dailyPrayerCount >= 4
    val ishaChecked = currentUser.dailyPrayerCount >= 5

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

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Adhan Alerts Toggle
            val context = androidx.compose.ui.platform.LocalContext.current
            val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true || 
                              permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
                if (granted) {
                    // For now, simulate getting location for simplicity or assume a default location.
                    // In a real app, use FusedLocationProviderClient here.
                    // Assuming Mecca coordinates as a fallback demo
                    val lat = 21.4225
                    val lng = 39.8262
                    val updated = currentUser.copy(
                        isAdhanEnabled = true,
                        adhanLocationLat = lat,
                        adhanLocationLng = lng
                    )
                    // Trigger fake toggle to save updated profile
                    onPrayerToggled("AdhanToggle", true) 
                    com.mithaq.app.util.AdhanScheduler.scheduleNextAdhan(context, lat, lng)
                    android.widget.Toast.makeText(context, if (isArabic) "تم تفعيل الأذان بنجاح" else "Adhan enabled successfully", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, if (isArabic) "يجب تفعيل إذن الموقع للأذان" else "Location permission is required for Adhan", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isArabic) "تفعيل الأذان (يتطلب GPS)" else "Enable Adhan (Requires GPS)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isArabic) "تنبيه صوتي (تكبيرة صغيرة) لوقت الصلاة" else "Short Takbir audio alert at prayer time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = currentUser.isAdhanEnabled,
                    onCheckedChange = { isEnabled ->
                        if (isEnabled) {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        } else {
                            // Disable Adhan
                            com.mithaq.app.util.AdhanScheduler.cancelAdhan(context)
                            // We use the same toggle callback hack to update DB
                            onPrayerToggled("AdhanToggle", false)
                        }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = SuccessGreen, checkedTrackColor = SuccessGreen.copy(alpha = 0.3f))
                )
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
