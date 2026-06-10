package com.mithaq.app.ui.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mithaq.app.model.UserProfile
import com.mithaq.app.ui.auth.AuthViewModel
import com.mithaq.app.util.AdhanScheduler
import com.mithaq.app.util.PrayerManager
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    currentUser: UserProfile,
    authViewModel: AuthViewModel,
    isArabic: Boolean,
    onLanguageChange: (Boolean) -> Unit,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isArabic) "إعدادات التطبيق" else "App Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsHeaderCard(currentUser = currentUser, isArabic = isArabic)

            // General Settings
            Text(
                text = if (isArabic) "الإعدادات العامة" else "General Settings",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Language
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(if (isArabic) "اللغة (Language)" else "Language")
                        }
                        Switch(
                            checked = isArabic,
                            onCheckedChange = { onLanguageChange(it) }
                        )
                    }
                    HorizontalDivider()
                    // Dark Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(if (isArabic) "الوضع الليلي" else "Dark Mode")
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { onDarkModeChange(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isArabic) "فحص الموقع GPS" else "GPS Location Check",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            GpsCheckSection(
                currentUser = currentUser,
                authViewModel = authViewModel,
                isArabic = isArabic
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Adhan Settings
            Text(
                text = if (isArabic) "إعدادات الأذان والصلاة" else "Adhan & Prayer Settings",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            AdhanSettingsSectionFixed(
                currentUser = currentUser,
                authViewModel = authViewModel,
                isArabic = isArabic
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Account Management
            Text(
                text = if (isArabic) "إدارة الحساب" else "Account Management",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isArabic) "تسجيل الخروج" else "Logout")
                    }

                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isArabic) "حذف الحساب نهائياً" else "Delete Account Permanently", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = if (isArabic) "حذف الحساب نهائياً" else "Delete Account Permanently",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isArabic)
                        "هل أنت متأكد من رغبتك في حذف حسابك؟ سيؤدي ذلك إلى مسح جميع بيانات ملفك الشخصي ومحادثاتك بالكامل ولن تتمكن من استعادتها."
                    else "Are you sure you want to delete your account? This will permanently delete all your profile data and chats and cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteAccount()
                    }
                ) {
                    Text(
                        text = if (isArabic) "نعم، احذف حسابي" else "Yes, Delete Account",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(text = if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsHeaderCard(currentUser: UserProfile, isArabic: Boolean) {
    val guardianReady = currentUser.guardianStatus == "VERIFIED" || !currentUser.guardianEmail.isNullOrBlank()
    val adhanStatus = if (currentUser.isAdhanEnabled) {
        if (isArabic) "مفعل" else "On"
    } else {
        if (isArabic) "متوقف" else "Off"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = if (isArabic) "إدارة الحساب والتجربة" else "Account & Experience",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SettingsStatusPill(
                    label = if (isArabic) "الأذان" else "Adhan",
                    value = adhanStatus,
                    modifier = Modifier.weight(1f)
                )
                SettingsStatusPill(
                    label = if (isArabic) "الولي" else "Guardian",
                    value = if (guardianReady) {
                        if (isArabic) "جاهز" else "Ready"
                    } else {
                        if (isArabic) "غير مضاف" else "Missing"
                    },
                    modifier = Modifier.weight(1f)
                )
                SettingsStatusPill(
                    label = "GPS",
                    value = if (currentUser.gpsLocationEnabled) {
                        if (isArabic) "مفعل" else "Verified"
                    } else {
                        if (isArabic) "غير مفعل" else "Off"
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SettingsStatusPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun GpsCheckSection(
    currentUser: UserProfile,
    authViewModel: AuthViewModel,
    isArabic: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    var isChecking by remember { mutableStateOf(false) }
    var statusMessage by remember(currentUser.gpsLocationEnabled, currentUser.adhanLocationLat, currentUser.adhanLocationLng) {
        mutableStateOf<String?>(null)
    }
    val localTime = com.mithaq.app.util.CountryUtils.formatLocalTimeForCountry(
        currentUser.country,
        currentUser.timezone,
        isArabic
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(
                            text = if (isArabic) "تأكيد موقع العضو" else "Verify member location",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isArabic) "وقت بلدك الآن: $localTime" else "Your country time now: $localTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            if (currentUser.gpsLocationEnabled) {
                                if (isArabic) "GPS مفعل" else "GPS verified"
                            } else {
                                if (isArabic) "لم يتم الفحص" else "Not checked"
                            }
                        )
                    }
                )
            }

            if (currentUser.adhanLocationLat != 0.0 || currentUser.adhanLocationLng != 0.0) {
                Text(
                    text = "Lat ${"%.4f".format(currentUser.adhanLocationLat)}, Lng ${"%.4f".format(currentUser.adhanLocationLng)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            statusMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = {
                    if (!locationPermissionsState.allPermissionsGranted) {
                        locationPermissionsState.launchMultiplePermissionRequest()
                        statusMessage = if (isArabic) {
                            "اسمح للتطبيق باستخدام الموقع ثم اضغط فحص GPS مرة أخرى."
                        } else {
                            "Allow location permission, then tap Check GPS again."
                        }
                    } else {
                        isChecking = true
                        statusMessage = null
                        coroutineScope.launch {
                            val location = runCatching {
                                LocationServices.getFusedLocationProviderClient(context).lastLocation.await()
                            }.getOrNull()

                            if (location == null || (location.latitude == 0.0 && location.longitude == 0.0)) {
                                isChecking = false
                                val message = if (isArabic) {
                                    "لم يتم العثور على موقع حالي. افتح خدمة الموقع أو الخرائط ثم أعد المحاولة."
                                } else {
                                    "No current location was found. Enable location services or open Maps, then retry."
                                }
                                statusMessage = message
                                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                authViewModel.updateGpsLocation(location.latitude, location.longitude) { success, error ->
                                    isChecking = false
                                    val message = if (success) {
                                        if (isArabic) "تم تحديث GPS وحفظ موقعك بنجاح." else "GPS location saved successfully."
                                    } else {
                                        error ?: if (isArabic) "تعذر تحديث GPS." else "Could not update GPS."
                                    }
                                    statusMessage = message
                                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isChecking
            ) {
                if (isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isArabic) "فحص GPS الآن" else "Check GPS Now")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AdhanSettingsSectionFixed(
    currentUser: UserProfile,
    authViewModel: AuthViewModel,
    isArabic: Boolean
) {
    val coroutineScope = rememberCoroutineScope()
    // Optimistic state
    var isEnabled by remember { mutableStateOf(currentUser.isAdhanEnabled) }
    var calcMethod by remember { mutableStateOf(currentUser.adhanCalculationMethod.ifEmpty { "MUSLIM_WORLD_LEAGUE" }) }
    var soundPattern by remember { mutableStateOf(currentUser.adhanSoundPattern.ifEmpty { "TAKBEER" }) }
    var isSaving by remember { mutableStateOf(false) }

    var expandedCalc by remember { mutableStateOf(false) }
    var expandedSound by remember { mutableStateOf(false) }
    var expandedPreReminder by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val prefs = context.getSharedPreferences("mithaq_prefs", Context.MODE_PRIVATE)
    var preReminderMin by remember { mutableStateOf(prefs.getString("adhan_pre_reminder_min", "0") ?: "0") }
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    val playSoundPreview = { soundKey: String ->
        mediaPlayer?.release()
        // Simulate playing different sounds by playing default notification sound
        // In a real app, you would play actual raw resources based on soundKey.
        try {
            val defaultSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = MediaPlayer.create(context, defaultSoundUri)
            mediaPlayer?.start()
        } catch (e: Exception) {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    val calcOptions = mapOf(
        "MUSLIM_WORLD_LEAGUE" to if (isArabic) "رابطة العالم الإسلامي" else "Muslim World League",
        "EGYPTIAN" to if (isArabic) "الهيئة العامة المصرية للمساحة" else "Egyptian General Authority",
        "KARACHI" to if (isArabic) "جامعة العلوم الإسلامية بكراتشي" else "University of Islamic Sciences, Karachi",
        "UMM_AL_QURA" to if (isArabic) "جامعة أم القرى (مكة)" else "Umm al-Qura University, Makkah",
        "GULF" to if (isArabic) "منطقة الخليج" else "Gulf Region",
        "MOON_SIGHTING_COMMITTEE" to if (isArabic) "لجنة رؤية الهلال" else "Moon Sighting Committee",
        "ISNA" to if (isArabic) "الجمعية الإسلامية لأمريكا الشمالية" else "ISNA (North America)"
    )

    val soundOptions = mapOf(
        "TAKBEER" to if (isArabic) "تكبيرة صغيرة" else "Short Takbeer",
        "ADHAN_FULL" to if (isArabic) "أذان كامل" else "Full Adhan",
        "FULL_ADHAN_MECCA" to if (isArabic) "أذان الحرم المكي" else "Mecca Adhan",
        "FULL_ADHAN_MEDINA" to if (isArabic) "أذان الحرم المدني" else "Medina Adhan",
        "BIRD_CHIRP" to if (isArabic) "تغريد طيور" else "Bird Chirp",
        "SILENT" to if (isArabic) "صامت (إشعار فقط)" else "Silent (Notification only)"
    )

    val preReminderOptions = mapOf(
        "0" to if (isArabic) "بدون تذكير" else "Off",
        "5" to if (isArabic) "5 دقائق قبل الصلاة" else "5 minutes before",
        "10" to if (isArabic) "10 دقائق قبل الصلاة" else "10 minutes before",
        "15" to if (isArabic) "15 دقيقة قبل الصلاة" else "15 minutes before"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(if (isArabic) "تفعيل تنبيهات الأذان" else "Enable Adhan Alerts")
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { checked ->
                        isEnabled = checked
                        if (checked && !locationPermissionsState.allPermissionsGranted) {
                            locationPermissionsState.launchMultiplePermissionRequest()
                        }
                    }
                )
            }

            if (isEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (locationPermissionsState.allPermissionsGranted) {
                        if (isArabic) "سيتم استخدام موقعك الحالي لحساب أوقات الصلاة." else "Your current location will be used for prayer times."
                    } else {
                        if (isArabic) "لو لم تمنح صلاحية الموقع، سنستخدم بلدك كبديل تقريبي." else "If location is not granted, your country will be used as an approximate fallback."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!locationPermissionsState.allPermissionsGranted) {
                    TextButton(onClick = { locationPermissionsState.launchMultiplePermissionRequest() }) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "منح صلاحية الموقع" else "Grant location permission")
                    }
                }

                // Calculation Method Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedCalc,
                    onExpandedChange = { expandedCalc = !expandedCalc }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = calcOptions[calcMethod] ?: calcMethod,
                        onValueChange = { },
                        label = { Text(if (isArabic) "طريقة الحساب" else "Calculation Method") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCalc) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCalc,
                        onDismissRequest = { expandedCalc = false }
                    ) {
                        calcOptions.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    calcMethod = key
                                    expandedCalc = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sound Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedSound,
                    onExpandedChange = { expandedSound = !expandedSound }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = soundOptions[soundPattern] ?: soundPattern,
                        onValueChange = { },
                        label = { Text(if (isArabic) "صوت المؤذن / التنبيه" else "Adhan Sound / Alert") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSound) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSound,
                        onDismissRequest = { expandedSound = false }
                    ) {
                        soundOptions.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    soundPattern = key
                                    expandedSound = false
                                    if (key != "SILENT") playSoundPreview(key)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pre-Prayer Reminder Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedPreReminder,
                    onExpandedChange = { expandedPreReminder = !expandedPreReminder }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = preReminderOptions[preReminderMin] ?: preReminderOptions["0"]!!,
                        onValueChange = { },
                        label = { Text(if (isArabic) "تذكير قبل الصلاة" else "Pre-Prayer Reminder") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPreReminder) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPreReminder,
                        onDismissRequest = { expandedPreReminder = false }
                    ) {
                        preReminderOptions.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    preReminderMin = key
                                    expandedPreReminder = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    isSaving = true
                    prefs.edit().putString("adhan_pre_reminder_min", preReminderMin).apply()
                    coroutineScope.launch {
                        val success = AdhanScheduler.enableAndScheduleAdhan(
                            context = context,
                            currentUser = currentUser,
                            authViewModel = authViewModel,
                            isEnabled = isEnabled,
                            calcMethod = calcMethod,
                            soundPattern = soundPattern
                        )
                        if (!success && isEnabled && !AdhanScheduler.canScheduleExactAlarms(context)) {
                            isSaving = false
                            android.widget.Toast.makeText(
                                context,
                                if (isArabic) "فعّل صلاحية المنبهات الدقيقة ثم اضغط حفظ مرة أخرى." else "Allow exact alarms, then tap Save again.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                            return@launch
                        }
                        isSaving = false
                        android.widget.Toast.makeText(
                            context,
                            if (isArabic) "تم حفظ الإعدادات بنجاح" else "Settings saved successfully",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text(if (isArabic) "حفظ إعدادات الأذان" else "Save Adhan Settings")
                }
            }
        }
    }
}

