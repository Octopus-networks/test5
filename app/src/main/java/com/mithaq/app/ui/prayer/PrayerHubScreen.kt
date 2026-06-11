package com.mithaq.app.ui.prayer

import android.content.Context
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batoulapps.adhan.Prayer
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.mithaq.app.R
import com.mithaq.app.model.UserProfile
import com.mithaq.app.ui.components.MithaqEmptyState
import com.mithaq.app.util.AdhanScheduler
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerHubScreen(
    currentUser: UserProfile,
    isArabic: Boolean,
    onOpenPrayerSettings: () -> Unit,
    onOpenQibla: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var coordinates by remember { mutableStateOf<AdhanScheduler.AdhanCoordinates?>(null) }
    var prayerTimes by remember { mutableStateOf<PrayerTimes?>(null) }
    var nextPrayer by remember { mutableStateOf<Prayer?>(null) }
    var nextPrayerTime by remember { mutableStateOf<Date?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentUser) {
        val resolvedCoords = AdhanScheduler.resolveAdhanCoordinates(context, currentUser)
        if (resolvedCoords.lat != 0.0 || resolvedCoords.lng != 0.0) {
            coordinates = resolvedCoords
            val prefs = context.getSharedPreferences("mithaq_prefs", Context.MODE_PRIVATE)
            val method = prefs.getString("adhan_calculation_method", "MUSLIM_WORLD_LEAGUE") ?: "MUSLIM_WORLD_LEAGUE"
            val params = AdhanScheduler.calculationParametersFor(method)
            // Local calendar, not DateComponents.from(Date()) which is UTC-based and
            // returns the previous day's times between local midnight and the UTC offset.
            val today = AdhanScheduler.localDateComponents(Calendar.getInstance())
            val pt = PrayerTimes(
                com.batoulapps.adhan.Coordinates(resolvedCoords.lat, resolvedCoords.lng),
                today,
                params
            )
            prayerTimes = pt
            
            // Find next prayer
            val now = Date()
            val timesList = listOf(
                Prayer.FAJR to pt.fajr,
                Prayer.SUNRISE to pt.sunrise,
                Prayer.DHUHR to pt.dhuhr,
                Prayer.ASR to pt.asr,
                Prayer.MAGHRIB to pt.maghrib,
                Prayer.ISHA to pt.isha
            )
            var nPrayer = timesList.firstOrNull { it.second.after(now) }
            if (nPrayer == null) {
                // Next prayer is Fajr tomorrow
                val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                val tomPt = PrayerTimes(
                    com.batoulapps.adhan.Coordinates(resolvedCoords.lat, resolvedCoords.lng),
                    AdhanScheduler.localDateComponents(tomorrow),
                    params
                )
                nPrayer = Prayer.FAJR to tomPt.fajr
            }
            nextPrayer = nPrayer.first
            nextPrayerTime = nPrayer.second
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localizedString(isArabic, R.string.prayer_hub_title, R.string.prayer_hub_title_ar)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        if (coordinates == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                MithaqEmptyState(
                    title = localizedString(isArabic, R.string.prayer_hub_empty_state_title, R.string.prayer_hub_empty_state_title_ar),
                    message = localizedString(isArabic, R.string.prayer_hub_empty_state_message, R.string.prayer_hub_empty_state_message_ar),
                    icon = Icons.Default.Explore,
                    actionLabel = localizedString(isArabic, R.string.prayer_hub_setup_btn, R.string.prayer_hub_setup_btn_ar),
                    onAction = onOpenPrayerSettings
                )
            }
            return@Scaffold
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            
            // Header: Hijri Date (API 26+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val hijri = HijrahDate.now()
                val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", if (isArabic) Locale("ar") else Locale.getDefault())
                Text(
                    text = hijri.format(formatter),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // Hero Card
            nextPrayer?.let { np ->
                nextPrayerTime?.let { npt ->
                    PrayerHeroCard(
                        nextPrayer = np,
                        nextPrayerTime = npt,
                        isArabic = isArabic
                    )
                }
            }

            // Quick Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val prefs = context.getSharedPreferences("mithaq_prefs", Context.MODE_PRIVATE)
                // Stored as a STRING by the settings screen — getInt on it throws
                // ClassCastException and crashed the whole hub after saving settings.
                val preRemind = prefs.getString("adhan_pre_reminder_min", "0")?.toIntOrNull() ?: 0
                
                FilterChip(
                    selected = false,
                    onClick = onOpenPrayerSettings,
                    label = { Text(localizedString(isArabic, R.string.prayer_hub_settings, R.string.prayer_hub_settings_ar)) },
                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = false,
                    onClick = onOpenQibla,
                    label = { Text(localizedString(isArabic, R.string.prayer_hub_qibla, R.string.prayer_hub_qibla_ar)) },
                    leadingIcon = { Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = preRemind > 0,
                    onClick = onOpenPrayerSettings,
                    label = { 
                        val txt = if (preRemind > 0) "-$preRemind min" else if (isArabic) "لا تنبيه مسبق" else "No Pre-alert"
                        Text(txt) 
                    },
                    leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            // Timetable
            prayerTimes?.let { pt ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        PrayerTimetableRow(Prayer.FAJR, pt.fajr, nextPrayer == Prayer.FAJR, currentUser.fajrPrayedToday, isArabic)
                        PrayerTimetableRow(Prayer.SUNRISE, pt.sunrise, nextPrayer == Prayer.SUNRISE, false, isArabic) // Sunrise cannot be prayed
                        PrayerTimetableRow(Prayer.DHUHR, pt.dhuhr, nextPrayer == Prayer.DHUHR, currentUser.dhuhrPrayedToday, isArabic)
                        PrayerTimetableRow(Prayer.ASR, pt.asr, nextPrayer == Prayer.ASR, currentUser.asrPrayedToday, isArabic)
                        PrayerTimetableRow(Prayer.MAGHRIB, pt.maghrib, nextPrayer == Prayer.MAGHRIB, currentUser.maghribPrayedToday, isArabic)
                        PrayerTimetableRow(Prayer.ISHA, pt.isha, nextPrayer == Prayer.ISHA, currentUser.ishaPrayedToday, isArabic)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PrayerHeroCard(nextPrayer: Prayer, nextPrayerTime: Date, isArabic: Boolean) {
    var currentTime by remember { mutableStateOf(Date()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = Date()
        }
    }

    val diff = nextPrayerTime.time - currentTime.time
    val hours = if (diff > 0) (diff / (1000 * 60 * 60)) % 24 else 0
    val minutes = if (diff > 0) (diff / (1000 * 60)) % 60 else 0
    val seconds = if (diff > 0) (diff / 1000) % 60 else 0
    val countdown = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

    val gradientColors = when (nextPrayer) {
        Prayer.FAJR -> listOf(Color(0xFF4A148C), Color(0xFFC2185B)) // Dawn purples/rose
        Prayer.SUNRISE -> listOf(Color(0xFF283593), Color(0xFF0277BD))
        Prayer.DHUHR -> listOf(Color(0xFF0288D1), Color(0xFF4FC3F7)) // Bright sky blues
        Prayer.ASR -> listOf(Color(0xFFF57F17), Color(0xFFFFB300)) // Warm gold
        Prayer.MAGHRIB -> listOf(Color(0xFFE65100), Color(0xFFF4511E)) // Sunset orange/red
        Prayer.ISHA, Prayer.NONE -> listOf(Color(0xFF1A237E), Color(0xFF000000)) // Deep night blues
    }

    val icon = when (nextPrayer) {
        Prayer.FAJR -> Icons.Default.WbTwilight
        Prayer.SUNRISE -> Icons.Default.WbSunny
        Prayer.DHUHR -> Icons.Default.WbSunny
        Prayer.ASR -> Icons.Default.WbSunny
        Prayer.MAGHRIB -> Icons.Default.WbTwilight
        Prayer.ISHA, Prayer.NONE -> Icons.Default.NightsStay
    }

    val name = getPrayerName(nextPrayer, isArabic)
    val timeStr = SimpleDateFormat("hh:mm a", if (isArabic) Locale("ar") else Locale.getDefault()).format(nextPrayerTime)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.verticalGradient(gradientColors))
    ) {
        // Subtle mosque/crescent silhouette
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(0f, h)
                lineTo(0f, h * 0.7f)
                quadraticBezierTo(w * 0.2f, h * 0.6f, w * 0.5f, h * 0.8f)
                quadraticBezierTo(w * 0.8f, h * 0.65f, w, h * 0.75f)
                lineTo(w, h)
                close()
            }
            drawPath(path, color = Color.White.copy(alpha = 0.1f))
            drawCircle(color = Color.White.copy(alpha = 0.15f), radius = 120f, center = androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.4f))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = countdown,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 54.sp),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = timeStr,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun PrayerTimetableRow(
    prayer: Prayer,
    time: Date,
    isNext: Boolean,
    isPrayed: Boolean,
    isArabic: Boolean
) {
    val name = getPrayerName(prayer, isArabic)
    val timeStr = SimpleDateFormat("hh:mm a", if (isArabic) Locale("ar") else Locale.getDefault()).format(time)
    
    val containerColor = if (isNext) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isNext) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = when (prayer) {
            Prayer.FAJR -> Icons.Default.WbTwilight
            Prayer.SUNRISE -> Icons.Default.WbSunny
            Prayer.DHUHR -> Icons.Default.WbSunny
            Prayer.ASR -> Icons.Default.WbSunny
            Prayer.MAGHRIB -> Icons.Default.WbTwilight
            Prayer.ISHA, Prayer.NONE -> Icons.Default.NightsStay
        }
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
            color = contentColor,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = timeStr,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
            color = contentColor
        )
        if (prayer != Prayer.SUNRISE) {
            Spacer(modifier = Modifier.width(12.dp))
            if (isPrayed) {
                Icon(Icons.Default.Check, contentDescription = "Prayed", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
            } else {
                Spacer(modifier = Modifier.size(20.dp))
            }
        }
    }
}

private fun getPrayerName(prayer: Prayer, isArabic: Boolean): String {
    return when (prayer) {
        Prayer.FAJR -> if (isArabic) "الفجر" else "Fajr"
        Prayer.SUNRISE -> if (isArabic) "الشروق" else "Sunrise"
        Prayer.DHUHR -> if (isArabic) "الظهر" else "Dhuhr"
        Prayer.ASR -> if (isArabic) "العصر" else "Asr"
        Prayer.MAGHRIB -> if (isArabic) "المغرب" else "Maghrib"
        Prayer.ISHA -> if (isArabic) "العشاء" else "Isha"
        Prayer.NONE -> ""
    }
}

@Composable
private fun localizedString(isArabic: Boolean, englishResId: Int, arabicResId: Int): String {
    return stringResource(id = if (isArabic) arabicResId else englishResId)
}
