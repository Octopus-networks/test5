package com.mithaq.app

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Done
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.mithaq.app.model.*
import com.mithaq.app.navigation.AuthGate
import com.mithaq.app.navigation.Routes
import com.mithaq.app.ui.auth.AuthState
import com.mithaq.app.ui.auth.AuthViewModel
import com.mithaq.app.ui.auth.EntryDecisionScreen
import com.mithaq.app.ui.auth.ForgotPasswordScreen
import com.mithaq.app.ui.auth.LoginScreen
import com.mithaq.app.ui.auth.RegisterScreen
import com.mithaq.app.ui.chat.ChaperonedChatBanner
import com.mithaq.app.ui.chat.ChatBubble
import com.mithaq.app.ui.chat.ChaperonedChatViewModel
import com.mithaq.app.ui.filter.SearchFilterBottomSheet
import com.mithaq.app.ui.filter.SearchViewModel
import com.mithaq.app.ui.guardian.GuardianViewModel
import com.mithaq.app.ui.guardian.InviteGuardianDialog
import com.mithaq.app.ui.match.MatchScoreBadge
import com.mithaq.app.ui.match.MatchScoreCalculator
import com.mithaq.app.ui.photo.PhotoAccessRequestCard
import com.mithaq.app.ui.photo.PhotoAccessState
import com.mithaq.app.ui.photo.UserProfileImage
import com.mithaq.app.ui.photo.BrotherhoodAvatars
import com.mithaq.app.ui.photo.SisterhoodAvatars
import com.mithaq.app.ui.theme.MithaqTheme
import com.mithaq.app.ui.theme.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Add
import com.mithaq.app.ui.admin.AdminConsoleScreen
import com.mithaq.app.ui.limit.PremiumStoreScreen
import com.mithaq.app.ui.match.QuestionnaireScreen
import com.mithaq.app.ui.match.CompatibilityBreakdownDialog
import com.mithaq.app.ui.onboarding.OnboardingWizardScreen
import com.mithaq.app.ui.chat.ChaperonedVoiceCallScreen
import com.mithaq.app.ui.chat.CallState
import com.mithaq.app.security.SecureScreen
import com.mithaq.app.security.BiometricAuthManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import com.mithaq.app.ui.stats.MyStatsScreen
import com.mithaq.app.ui.splash.SplashScreen
import com.mithaq.app.ui.auth.CompleteProfileScreen
import com.mithaq.app.ui.settings.AppSettingsScreen
import com.mithaq.app.ui.verification.VerifyEmailScreen


class MainActivity : FragmentActivity() {
    private var latestDeepLinkData by mutableStateOf<String?>(null)
    private var latestDeepLinkNonce by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        captureVerificationDeepLink(intent)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val manager = getSystemService(android.app.NotificationManager::class.java)
            if (manager != null) {
                com.mithaq.app.notification.MithaqFirebaseMessagingService.ensureMessageChannels(
                    manager,
                    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                )
            }
        }

        // Debug-only Firebase fallback for local demo builds.
        try {
            if (!BuildConfig.IS_PRODUCTION && FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(
                    this,
                    FirebaseOptions.Builder()
                        .setApplicationId("1:1234567890:android:1234567890")
                        .setApiKey("mock-api-key-for-testing")
                        .setProjectId("mithaq-app-testing")
                        .build()
                )
            }
        } catch (e: Exception) {
            // Fail-safe
        }
        installAppCheck()

        setContent {
            var isArabic by remember { mutableStateOf(false) }
            var isDarkMode by remember { mutableStateOf(false) }
            var isBiometricAuthenticated by remember { mutableStateOf(false) }
            val biometricManager = remember { BiometricAuthManager(this) }

            LaunchedEffect(Unit) {
                if (biometricManager.isBiometricAvailable()) {
                    biometricManager.showBiometricPrompt(
                        activity = this@MainActivity,
                        title = if (isArabic) "المصادقة البيومترية" else "Biometric Authentication",
                        subtitle = if (isArabic) "يرجى تأكيد هويتك للمتابعة" else "Please authenticate to continue",
                        onSuccess = { isBiometricAuthenticated = true },
                        onError = { if (!BuildConfig.IS_PRODUCTION) isBiometricAuthenticated = true }
                    )
                } else {
                    isBiometricAuthenticated = true
                }
            }

            MithaqTheme(isArabic = isArabic, darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isBiometricAuthenticated) {
                        MithaqAppNavigation(
                            isArabic = isArabic,
                            onLanguageChange = { isArabic = it },
                            isDarkMode = isDarkMode,
                            onDarkModeChange = { isDarkMode = it },
                            deepLinkData = latestDeepLinkData,
                            deepLinkNonce = latestDeepLinkNonce
                        )
                    } else {
                        // Show a loading or locked state
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(if (isArabic) "التطبيق مقفل" else "App Locked", style = MaterialTheme.typography.headlineMedium)
                                Button(onClick = {
                                    biometricManager.showBiometricPrompt(
                                        activity = this@MainActivity,
                                        title = if (isArabic) "المصادقة البيومترية" else "Biometric Authentication",
                                        subtitle = if (isArabic) "يرجى تأكيد هويتك للمتابعة" else "Please authenticate to continue",
                                        onSuccess = { isBiometricAuthenticated = true },
                                        onError = { }
                                    )
                                }, modifier = Modifier.padding(top = 16.dp)) {
                                    Text(if (isArabic) "إلغاء القفل" else "Unlock")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureVerificationDeepLink(intent)
    }

    private fun captureVerificationDeepLink(intent: Intent?) {
        val data = intent?.dataString
        if (data?.startsWith("https://mithaq.app/verify-email") == true ||
            data?.startsWith("https://mithaq.app/reset-password") == true
        ) {
            latestDeepLinkData = data
            latestDeepLinkNonce += 1
        }
    }

    private fun installAppCheck() {
        try {
            val providerFactory = if (BuildConfig.IS_PRODUCTION) {
                PlayIntegrityAppCheckProviderFactory.getInstance()
            } else {
                debugAppCheckProviderFactory() ?: PlayIntegrityAppCheckProviderFactory.getInstance()
            }
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(providerFactory)
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "App Check setup failed", e)
        }
    }

    private fun debugAppCheckProviderFactory(): AppCheckProviderFactory? {
        return try {
            val clazz = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
            clazz.getMethod("getInstance").invoke(null) as? AppCheckProviderFactory
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
private fun HomeDashboardContent(
    currentUser: UserProfile,
    isArabic: Boolean,
    likesCount: Int,
    chatCount: Int,
    viewsCount: Int,
    onOpenSearch: () -> Unit,
    onOpenChats: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val hasPhoto = currentUser.imageUrl.isNotBlank() && !currentUser.imageUrl.contains("avatar_")
    val hasGuardian = !currentUser.guardianEmail.isNullOrBlank()
    val hasQuestionnaire = currentUser.questionnaireAnswers.isNotEmpty()
    val isVerified = currentUser.verificationStatus == "VERIFIED" || currentUser.verificationStatus == "PENDING"
    val completion = listOf(hasPhoto, hasGuardian, hasQuestionnaire, isVerified).count { it } * 25
    val nextPrayer = remember(
        currentUser.country,
        currentUser.adhanLocationLat,
        currentUser.adhanLocationLng,
        currentUser.adhanCalculationMethod,
        isArabic
    ) { nextPrayerSummary(currentUser, isArabic) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isArabic) "أهلا ${currentUser.name.ifBlank { "بك" }}" else "Welcome, ${currentUser.name.ifBlank { "there" }}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isArabic) "اكتمال الملف" else "Profile Completion",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$completion%",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Button(onClick = onOpenProfile, shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isArabic) "الملف" else "Profile")
                    }
                }
                LinearProgressIndicator(
                    progress = completion / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            DashboardMetricCard(
                icon = Icons.Default.Favorite,
                label = if (isArabic) "إعجابات" else "Likes",
                value = likesCount.toString(),
                modifier = Modifier.weight(1f),
                onClick = onOpenAlerts
            )
            DashboardMetricCard(
                icon = Icons.Default.Chat,
                label = if (isArabic) "محادثات" else "Chats",
                value = chatCount.toString(),
                modifier = Modifier.weight(1f),
                onClick = onOpenChats
            )
            DashboardMetricCard(
                icon = Icons.Default.Visibility,
                label = if (isArabic) "زيارات" else "Views",
                value = viewsCount.toString(),
                modifier = Modifier.weight(1f),
                onClick = onOpenAlerts
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) "الصلاة القادمة" else "Next Prayer",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(nextPrayer, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isArabic) "الأذان" else "Adhan")
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            DashboardActionButton(
                icon = Icons.Default.Search,
                label = if (isArabic) "استكشف" else "Explore",
                modifier = Modifier.weight(1f),
                onClick = onOpenSearch
            )
            DashboardActionButton(
                icon = Icons.Default.Favorite,
                label = if (isArabic) "التنبيهات" else "Alerts",
                modifier = Modifier.weight(1f),
                onClick = onOpenAlerts
            )
        }
    }
}

@Composable
private fun DashboardMetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DashboardActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun AlertsHubContent(
    currentUser: UserProfile,
    searchViewModel: SearchViewModel,
    likesRepository: com.mithaq.app.data.LikesRepository,
    isArabic: Boolean,
    onSelectMatch: (UserProfile) -> Unit,
    onNavigateToUpgrade: () -> Unit
) {
    var selected by remember { mutableStateOf(0) }
    val labels = if (isArabic) listOf("الإعجابات", "الزيارات", "المفضلة") else listOf("Likes", "Views", "Favorites")

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            labels.forEachIndexed { index, label ->
                val selectedTab = selected == index
                TextButton(
                    onClick = { selected = index },
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (selectedTab) MaterialTheme.colorScheme.surface else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                ) {
                    Text(
                        text = label,
                        color = if (selectedTab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selectedTab) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        when (selected) {
            0 -> LikesTabContent(
                currentUser = currentUser,
                searchViewModel = searchViewModel,
                likesRepository = likesRepository,
                isArabic = isArabic,
                onSelectMatch = onSelectMatch,
                onNavigateToUpgrade = onNavigateToUpgrade
            )
            1 -> ViewsTabContent(
                currentUser = currentUser,
                searchViewModel = searchViewModel,
                likesRepository = likesRepository,
                isArabic = isArabic,
                onSelectMatch = onSelectMatch
            )
            else -> FavoritesTabContent(
                currentUser = currentUser,
                searchViewModel = searchViewModel,
                likesRepository = likesRepository,
                isArabic = isArabic,
                onSelectMatch = onSelectMatch
            )
        }
    }
}

@Composable
private fun AccountHubContent(
    currentUser: UserProfile,
    isArabic: Boolean,
    isDarkMode: Boolean,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSubscription: () -> Unit,
    onToggleTheme: () -> Unit,
    onLanguageChange: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            UserProfileImage(
                imageUrl = currentUser.imageUrl,
                gender = currentUser.gender,
                isBlurred = false,
                modifier = Modifier.size(64.dp),
                shape = CircleShape
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    currentUser.name.ifBlank { if (isArabic) "حسابي" else "My Account" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                val localTime = com.mithaq.app.util.CountryUtils.formatLocalTimeForCountry(currentUser.country, currentUser.timezone, isArabic)
                Text("${currentUser.city}, ${currentUser.country} • $localTime", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        AccountActionRow(Icons.Default.Person, if (isArabic) "الملف الشخصي" else "Profile", onOpenProfile)
        AccountActionRow(Icons.Default.Settings, if (isArabic) "إعدادات التطبيق" else "App Settings", onOpenSettings)
        AccountActionRow(Icons.Default.Star, if (isArabic) "الإحصائيات" else "Stats", onOpenStats)
        AccountActionRow(Icons.Default.LockOpen, if (isArabic) "الاشتراك" else "Subscription", onOpenSubscription)
        AccountActionRow(
            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
            if (isArabic) "تبديل المظهر" else "Toggle Theme",
            onToggleTheme
        )
        AccountActionRow(Icons.Default.Info, if (isArabic) "تغيير اللغة" else "Change Language", onLanguageChange)

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (isArabic) "تسجيل الخروج" else "Logout")
        }
    }
}

@Composable
private fun AccountActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp).alpha(0.55f))
        }
    }
}

private fun nextPrayerSummary(currentUser: UserProfile, isArabic: Boolean): String {
    return runCatching {
        val now = java.util.Date()
        val times = if (currentUser.adhanLocationLat != 0.0 || currentUser.adhanLocationLng != 0.0) {
            com.mithaq.app.util.PrayerManager.getDailyPrayerTimes(
                latitude = currentUser.adhanLocationLat,
                longitude = currentUser.adhanLocationLng,
                calculationMethod = currentUser.adhanCalculationMethod.ifBlank { "MUSLIM_WORLD_LEAGUE" }
            )
        } else {
            com.mithaq.app.util.PrayerManager.getDailyPrayerTimes(currentUser.country)
        }
        val prayers = listOf(
            (if (isArabic) "الفجر" else "Fajr") to times.fajr,
            (if (isArabic) "الظهر" else "Dhuhr") to times.dhuhr,
            (if (isArabic) "العصر" else "Asr") to times.asr,
            (if (isArabic) "المغرب" else "Maghrib") to times.maghrib,
            (if (isArabic) "العشاء" else "Isha") to times.isha
        )
        val next = prayers.firstOrNull { it.second.after(now) } ?: prayers.first()
        val formatter = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        "${next.first} ${formatter.format(next.second)}"
    }.getOrElse {
        if (isArabic) "غير متاح" else "Unavailable"
    }
}

@Composable
fun MithaqAppNavigation(
    isArabic: Boolean,
    onLanguageChange: (Boolean) -> Unit,
    isDarkMode: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {},
    deepLinkData: String? = null,
    deepLinkNonce: Int = 0
) {
    var currentScreen by remember { mutableStateOf(Routes.Splash) }
    var currentUserId by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity

    var showExitDialog by remember { mutableStateOf(false) }

    if (showExitDialog) {
        val strings = com.mithaq.app.ui.theme.LocalMithaqStrings.current
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(if (isArabic) "خروج" else "Exit") },
            text = { Text(if (isArabic) "هل تود الخروج من البرنامج؟" else "Are you sure you want to exit the app?") },
            confirmButton = {
                Button(
                    onClick = { activity?.finish() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isArabic) "نعم" else "Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(if (isArabic) "لا" else "No")
                }
            }
        )
    }

    androidx.activity.compose.BackHandler(enabled = currentScreen == Routes.Home || currentScreen == Routes.Login || currentScreen == Routes.Entry) {
        showExitDialog = true
    }

    var selectedMatchProfile by remember { mutableStateOf<UserProfile?>(null) }
    var initialTab by remember { mutableStateOf(0) }
    var initialChatUser by remember { mutableStateOf<UserProfile?>(null) }
    var premiumStoreInitialTab by remember { mutableStateOf(0) }

    var hasNotificationPermission by remember { mutableStateOf(true) }

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted -> 
        hasNotificationPermission = isGranted
    }

    var showExactAlarmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            hasNotificationPermission = permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasNotificationPermission) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Check for exact alarm permission on Android 12+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                showExactAlarmDialog = true
            }
        }
    }
    
    if (showExactAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showExactAlarmDialog = false },
            title = { Text(if (isArabic) "صلاحية الأذان" else "Adhan Permission") },
            text = { Text(if (isArabic) "لضمان عمل الأذان في وقته الدقيق، يرجى منح التطبيق صلاحية المنبهات المجدولة من الإعدادات." else "To ensure the Adhan plays exactly on time, please grant the app Alarms & Reminders permission in Settings.") },
            confirmButton = {
                Button(onClick = {
                    showExactAlarmDialog = false
                    try {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = android.net.Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }) {
                    Text(if (isArabic) "فتح الإعدادات" else "Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExactAlarmDialog = false }) {
                    Text(if (isArabic) "تخطي" else "Skip")
                }
            }
        )
    }

    // ViewModels initialized safely
    val coroutineScope = rememberCoroutineScope()
    val authViewModel = remember { AuthViewModel(context = context) }
    val searchViewModel = remember { SearchViewModel(context = context) }
    val guardianViewModel = remember { GuardianViewModel() }

    val currentUserProfile by authViewModel.currentUserProfile.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    var hasDismissedOnboarding by remember { mutableStateOf(false) }
    val publicRoutes = remember {
        setOf(Routes.Splash, Routes.Entry, Routes.Login, Routes.Register, Routes.VerifyEmail, Routes.ForgotPassword)
    }
    val launchIntentData = deepLinkData

    LaunchedEffect(launchIntentData, deepLinkNonce) {
        when {
            launchIntentData?.startsWith("https://mithaq.app/verify-email") == true -> {
                currentScreen = Routes.VerifyEmail
                authViewModel.reloadAndCheckEmailVerification { verified, _ ->
                    if (verified) {
                        val uid = (authViewModel.authState.value as? AuthState.Authenticated)?.userId
                        if (uid != null) {
                            currentUserId = uid
                            currentScreen = Routes.Home
                        }
                    }
                }
            }
            launchIntentData?.startsWith("https://mithaq.app/reset-password") == true -> {
                currentScreen = Routes.Login
            }
        }
    }

    LaunchedEffect(
        currentUserProfile?.uid,
        currentUserProfile?.isAdhanEnabled,
        currentUserProfile?.adhanLocationLat,
        currentUserProfile?.adhanLocationLng,
        currentUserProfile?.adhanCalculationMethod,
        currentUserProfile?.adhanSoundPattern
    ) {
        val profile = currentUserProfile ?: return@LaunchedEffect
        if (profile.isAdhanEnabled && (profile.adhanLocationLat != 0.0 || profile.adhanLocationLng != 0.0)) {
            com.mithaq.app.util.AdhanScheduler.scheduleNextAdhan(
                context = context,
                lat = profile.adhanLocationLat,
                lng = profile.adhanLocationLng,
                calculationMethod = profile.adhanCalculationMethod.ifBlank { "MUSLIM_WORLD_LEAGUE" },
                soundPattern = profile.adhanSoundPattern.ifBlank { "TAKBEER" }
            )
        }
    }

    LaunchedEffect(authState, currentScreen) {
        when (val state = authState) {
            is AuthState.Authenticated -> {
                currentUserId = state.userId
                if (currentScreen in setOf(Routes.Splash, Routes.Entry, Routes.Login, Routes.Register, Routes.VerifyEmail)) {
                    com.mithaq.app.notification.NotificationSyncWorker.schedule(context)
                    currentScreen = AuthGate.routeFor(state)
                }
            }
            is AuthState.EmailVerificationRequired -> {
                currentUserId = ""
                com.mithaq.app.notification.NotificationSyncWorker.cancel(context)
                if (currentScreen != Routes.VerifyEmail) {
                    currentScreen = Routes.VerifyEmail
                }
            }
            AuthState.Idle -> {
                if (currentScreen !in publicRoutes) {
                    currentUserId = ""
                    currentScreen = Routes.Entry
                }
            }
            else -> {
                if (!AuthGate.canAccessProtectedRoute(authState) && currentScreen !in publicRoutes) {
                    currentScreen = Routes.Entry
                }
            }
        }
    }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(
        lifecycleOwner,
        currentUserId,
        currentUserProfile?.isAdhanEnabled,
        currentUserProfile?.adhanLocationLat,
        currentUserProfile?.adhanLocationLng,
        currentUserProfile?.adhanCalculationMethod,
        currentUserProfile?.adhanSoundPattern
    ) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (currentUserId.isNotEmpty()) {
                    authViewModel.updateOnlineStatus()
                }
                val profile = currentUserProfile
                if (profile != null && profile.isAdhanEnabled && (profile.adhanLocationLat != 0.0 || profile.adhanLocationLng != 0.0)) {
                    com.mithaq.app.util.AdhanScheduler.scheduleNextAdhan(
                        context = context,
                        lat = profile.adhanLocationLat,
                        lng = profile.adhanLocationLng,
                        calculationMethod = profile.adhanCalculationMethod.ifBlank { "MUSLIM_WORLD_LEAGUE" },
                        soundPattern = profile.adhanSoundPattern.ifBlank { "TAKBEER" }
                    )
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(currentUserId, currentUserProfile?.gender) {
        if (currentUserId.isNotEmpty()) {
            authViewModel.fetchCurrentUserProfile(currentUserId)
            searchViewModel.fetchUsers()
            authViewModel.updateOnlineStatus()
        }
    }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            val isMock = com.mithaq.app.Config.isMock()
            if (isMock) {
                // In mock mode, poll the local SharedPreferences queue every 5 seconds
                coroutineScope.launch {
                    while (true) {
                        val queuePrefs = context.getSharedPreferences("mithaq_notification_queue", android.content.Context.MODE_PRIVATE)
                        val queueStr = queuePrefs.getString("queue_$currentUserId", "[]") ?: "[]"
                        val array = org.json.JSONArray(queueStr)
                        if (array.length() > 0) {
                            for (i in 0 until array.length()) {
                                val notif = array.getJSONObject(i)
                                com.mithaq.app.notification.MithaqFirebaseMessagingService.showLocalNotification(
                                    context,
                                    notif.getString("title"),
                                    notif.getString("body")
                                )
                            }
                            queuePrefs.edit().putString("queue_$currentUserId", "[]").apply()
                        }
                        kotlinx.coroutines.delay(5000)
                    }
                }
            } else {
                // In production mode, listen to /notifications in real-time via Firestore
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
                try {
                    listenerRegistration = db.collection("notifications")
                        .whereEqualTo("recipientUid", currentUserId)
                        .whereEqualTo("status", "PENDING")
                        .addSnapshotListener { snapshot, error ->
                            if (error != null || snapshot == null) return@addSnapshotListener
                            for (doc in snapshot.documentChanges) {
                                if (doc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                    val title = doc.document.getString("title") ?: "ميثاق"
                                    val body = doc.document.getString("body") ?: ""
                                    com.mithaq.app.notification.MithaqFirebaseMessagingService.showLocalNotification(
                                        context, title, body
                                    )
                                    // Mark as DELIVERED so it won't fire again
                                    doc.document.reference.update("status", "DELIVERED")
                                }
                            }
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // Wait until this effect is cancelled (user logs out or uid changes) then clean up
                try {
                    kotlinx.coroutines.awaitCancellation()
                } finally {
                    listenerRegistration?.remove()
                }
            }
        }
    }

    when (currentScreen) {
        Routes.Splash -> {
            SplashScreen(
                onComplete = {
                    when (val state = authState) {
                        is AuthState.Authenticated -> {
                            currentUserId = state.userId
                            com.mithaq.app.notification.NotificationSyncWorker.schedule(context)
                            currentScreen = Routes.Home
                        }
                        is AuthState.EmailVerificationRequired -> currentScreen = Routes.VerifyEmail
                        else -> currentScreen = Routes.Entry
                    }
                }
            )
        }
        Routes.Entry -> {
            EntryDecisionScreen(
                isArabic = isArabic,
                onLanguageChange = onLanguageChange,
                onHasAccount = { currentScreen = Routes.Login },
                onCreateAccount = { currentScreen = Routes.Register }
            )
        }
        Routes.Login -> {
            LoginScreen(
                onNavigateToRegister = { currentScreen = Routes.Register },
                onForgotPassword = { currentScreen = Routes.ForgotPassword },
                onLoginSuccess = { uid ->
                    currentUserId = uid
                    com.mithaq.app.notification.NotificationSyncWorker.schedule(context)
                    currentScreen = Routes.Home
                },
                viewModel = authViewModel,
                isArabic = isArabic,
                onLanguageChange = onLanguageChange
            )
        }
        Routes.ForgotPassword -> {
            ForgotPasswordScreen(
                viewModel = authViewModel,
                isArabic = isArabic,
                onBackToLogin = { currentScreen = Routes.Login }
            )
        }
        Routes.Register -> {
            RegisterScreen(
                onNavigateToLogin = { currentScreen = Routes.Login },
                onRegisterSuccess = { uid ->
                    currentUserId = uid
                    com.mithaq.app.notification.NotificationSyncWorker.schedule(context)
                    currentScreen = Routes.Home
                },
                viewModel = authViewModel,
                isArabic = isArabic,
                onLanguageChange = onLanguageChange
            )
        }
        Routes.VerifyEmail -> {
            val verifyState = authState as? AuthState.EmailVerificationRequired
            VerifyEmailScreen(
                email = verifyState?.email ?: authViewModel.currentUserEmail,
                viewModel = authViewModel,
                isArabic = isArabic,
                onVerified = { uid ->
                    currentUserId = uid
                    com.mithaq.app.notification.NotificationSyncWorker.schedule(context)
                    currentScreen = Routes.Home
                },
                onChangeEmail = {
                    currentUserId = ""
                    currentScreen = Routes.Login
                },
                onSignOut = {
                    currentUserId = ""
                    currentScreen = Routes.Entry
                }
            )
        }
        Routes.Home -> {
            if (currentUserProfile?.isWaliAccount == true) {
                WaliDashboardScreen(
                    currentUserId = currentUserId,
                    currentUserProfile = currentUserProfile,
                    authViewModel = authViewModel,
                    isArabic = isArabic,
                    onLanguageChange = onLanguageChange,
                    onLogout = {
                        authViewModel.signOut()
                        com.mithaq.app.notification.NotificationSyncWorker.cancel(context)
                        currentScreen = "login"
                    }
                )
            } else {
                val loadedProfile = currentUserProfile
                val isProfileIncomplete = loadedProfile != null && (
                        loadedProfile.name.isBlank() ||
                        loadedProfile.username.isBlank() ||
                        loadedProfile.city.isBlank() ||
                        loadedProfile.country.isBlank() ||
                        !loadedProfile.oathChecked ||
                        loadedProfile.age !in 18..77
                )

                if (loadedProfile == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isArabic) "جاري تجهيز حسابك..." else "Preparing your account...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (isProfileIncomplete) {
                    CompleteProfileScreen(
                        userId = currentUserId,
                        onCompleteSuccess = {
                            authViewModel.fetchCurrentUserProfile(currentUserId)
                        },
                        viewModel = authViewModel,
                        isArabic = isArabic,
                        onLanguageChange = onLanguageChange
                    )
                } else {
                    val shouldShowWizard = currentUserProfile != null &&
                            (currentUserProfile!!.questionnaireAnswers.isEmpty() || currentUserProfile!!.verificationStatus == "NONE") &&
                            !hasDismissedOnboarding

                    if (shouldShowWizard) {
                        OnboardingWizardScreen(
                            authViewModel = authViewModel,
                            guardianViewModel = guardianViewModel,
                            isArabic = isArabic,
                            onComplete = {
                                authViewModel.fetchCurrentUserProfile(currentUserId)
                                hasDismissedOnboarding = true
                            },
                            onSkip = {
                                hasDismissedOnboarding = true
                            }
                        )
                    } else {
                    HomeScreen(
                        currentUserId = currentUserId,
                        currentUserProfile = currentUserProfile,
                        authViewModel = authViewModel,
                        searchViewModel = searchViewModel,
                        guardianViewModel = guardianViewModel,
                        isArabic = isArabic,
                        isDarkMode = isDarkMode,
                        onDarkModeChange = onDarkModeChange,
                        initialTab = initialTab,
                        initialChatUser = initialChatUser,
                        onClearInitialChat = {
                            initialTab = 0
                            initialChatUser = null
                        },
                        onLanguageChange = onLanguageChange,
                        onLogout = {
                            authViewModel.signOut()
                            com.mithaq.app.notification.NotificationSyncWorker.cancel(context)
                            currentScreen = "login"
                        },
                        onNavigateToScreen = { screen ->
                            if (screen == "premium_store_platinum") {
                                premiumStoreInitialTab = 1
                                currentScreen = "premium_store"
                            } else {
                                premiumStoreInitialTab = 0
                                currentScreen = screen
                            }
                        },
                        onNavigateToDetail = { partner ->
                            selectedMatchProfile = partner
                            currentScreen = "match_detail"
                        }
                    )
                }
            }
        }
    }
        "match_detail" -> {
            val partner = selectedMatchProfile
            if (partner != null) {
                val profile = currentUserProfile ?: UserProfile(uid = currentUserId, name = "User")
                val likesRepository = remember { com.mithaq.app.data.LikesRepository(context) }
                androidx.activity.compose.BackHandler { currentScreen = "home" }
                com.mithaq.app.ui.match.MatchDetailScreen(
                    partner = partner,
                    currentUser = profile,
                    isArabic = isArabic,
                    likesRepository = likesRepository,
                    searchViewModel = searchViewModel,
                    onBack = { currentScreen = "home" },
                    onNavigateToChat = { clickedPartner ->
                        initialTab = 2 // Chat Tab
                        initialChatUser = clickedPartner
                        currentScreen = "home"
                    },
                    onNavigateToUpgrade = {
                        currentScreen = "premium_store"
                    }
                )
            } else {
                currentScreen = "home"
            }
        }
        "admin" -> {
            if (currentUserProfile?.isAdmin == true) {
                androidx.activity.compose.BackHandler { currentScreen = "home" }
                AdminConsoleScreen(
                    viewModel = authViewModel,
                    isArabic = isArabic,
                    onBack = { currentScreen = "home" }
                )
            } else {
                currentScreen = "home"
            }
        }
        "premium_store" -> {
            androidx.activity.compose.BackHandler { currentScreen = "home" }
            PremiumStoreScreen(
                viewModel = authViewModel,
                isArabic = isArabic,
                initialTab = premiumStoreInitialTab,
                onBack = { currentScreen = "home" }
            )
        }
        "questionnaire" -> {
            androidx.activity.compose.BackHandler { currentScreen = "home" }
            QuestionnaireScreen(
                currentAnswers = currentUserProfile?.questionnaireAnswers ?: emptyMap(),
                isArabic = isArabic,
                onSaveAnswers = { answers ->
                    authViewModel.saveQuestionnaireAnswers(answers)
                    currentScreen = "home"
                },
                onBack = { currentScreen = "home" }
            )
        }
        "profile_settings" -> {
            androidx.activity.compose.BackHandler { currentScreen = "home" }
            ProfileSettingsScreen(
                currentUser = currentUserProfile ?: UserProfile(uid = currentUserId, name = "User"),
                onRefreshProfile = { authViewModel.fetchCurrentUserProfile(currentUserId) },
                isArabic = isArabic,
                authViewModel = authViewModel,
                guardianViewModel = guardianViewModel,
                onNavigateToScreen = { currentScreen = it },
                onBack = { currentScreen = "home" }
            )
        }
        "stats" -> {
            val profile = currentUserProfile ?: UserProfile(uid = currentUserId, name = "User")
            val likesRepository = remember { com.mithaq.app.data.LikesRepository(context) }
            androidx.activity.compose.BackHandler { currentScreen = "home" }
            MyStatsScreen(
                currentUser = profile,
                likesRepository = likesRepository,
                isArabic = isArabic,
                onPrayerStatsUpdated = { updatedProfile ->
                    authViewModel.updatePrayerStats(updatedProfile)
                },
                onBack = { currentScreen = "home" }
            )
        }
        "ai_matchmaker" -> {
            val profile = currentUserProfile ?: UserProfile(uid = currentUserId, name = "User")
            androidx.activity.compose.BackHandler { currentScreen = "home" }
            AiMatchmakerScreen(
                currentUser = profile,
                searchViewModel = searchViewModel,
                isArabic = isArabic,
                onBack = { currentScreen = "home" },
                onSelectMatch = { partner ->
                    selectedMatchProfile = partner
                    currentScreen = "match_detail"
                }
            )
        }
        "app_settings" -> {
            androidx.activity.compose.BackHandler { currentScreen = "profile_settings" }
            AppSettingsScreen(
                currentUser = currentUserProfile ?: UserProfile(uid = currentUserId, name = "User"),
                authViewModel = authViewModel,
                isArabic = isArabic,
                onLanguageChange = onLanguageChange,
                isDarkMode = isDarkMode,
                onDarkModeChange = onDarkModeChange,
                onBack = { currentScreen = "profile_settings" },
                onLogout = {
                    authViewModel.signOut()
                    com.mithaq.app.notification.NotificationSyncWorker.cancel(context)
                    currentScreen = "login"
                },
                onDeleteAccount = {
                    authViewModel.deleteCurrentUserAccount(context) {
                        currentScreen = "login"
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUserId: String,
    currentUserProfile: UserProfile?,
    authViewModel: AuthViewModel,
    searchViewModel: SearchViewModel,
    guardianViewModel: GuardianViewModel,
    isArabic: Boolean,
    isDarkMode: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {},
    onLanguageChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onNavigateToScreen: (String) -> Unit,
    initialTab: Int = 0,
    initialChatUser: UserProfile? = null,
    onClearInitialChat: () -> Unit = {},
    onNavigateToDetail: (UserProfile) -> Unit = {}
) {
    val strings = com.mithaq.app.ui.theme.LocalMithaqStrings.current
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    val tabs = if (isArabic) {
        listOf("الرئيسية", "البحث", "المحادثات", "التنبيهات", "حسابي")
    } else {
        listOf("Home", "Search", "Chats", "Alerts", "Account")
    }

    val profile = currentUserProfile ?: UserProfile(
        uid = currentUserId,
        name = "User",
        gender = Gender.MALE,
        age = 25,
        city = "Cairo",
        country = "Egypt"
    )

    var selectedChatUser by remember(initialChatUser) { mutableStateOf(initialChatUser) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var isOfflineSimulated by remember { mutableStateOf(false) }

    val likesRepository = remember { com.mithaq.app.data.LikesRepository(context) }

    // ── Badge Counts for Bottom Navigation ─────────────────────────────────
    val coroutineScopeHome = rememberCoroutineScope()
    var likesBadgeCount by remember { mutableStateOf(0) }
    var chatBadgeCount by remember { mutableStateOf(0) }
    var viewsBadgeCount by remember { mutableStateOf(0) }

    // SharedPrefs key for "last seen" counts so badge resets when tab is opened
    val badgePrefs = remember { context.getSharedPreferences("mithaq_badge_prefs", android.content.Context.MODE_PRIVATE) }

    // Refresh badge counts every 10 seconds while on home screen
    LaunchedEffect(currentUserId) {
        if (currentUserId.isEmpty()) return@LaunchedEffect
        while (true) {
            coroutineScopeHome.launch {
                try {
                    // Likes badge: new admirers since last viewed
                    val totalLikers = likesRepository.getWhoLikedMe(currentUserId).size
                    val seenLikers = badgePrefs.getInt("seen_likers_$currentUserId", 0)
                    if (selectedTab != 3) likesBadgeCount = maxOf(0, totalLikers - seenLikers)

                    // Views badge: new visitors since last viewed
                    val totalViews = likesRepository.getProfileVisitors(currentUserId).size
                    val seenViews = badgePrefs.getInt("seen_views_$currentUserId", 0)
                    if (selectedTab != 3) viewsBadgeCount = maxOf(0, totalViews - seenViews)

                    // Chat badge: count rooms with unread messages
                    if (selectedTab != 2) {
                        val isMockMode = com.mithaq.app.Config.isMock()
                        if (isMockMode) {
                            val chatPrefs = context.getSharedPreferences("mithaq_mock_chat", android.content.Context.MODE_PRIVATE)
                            val roomsStr = chatPrefs.getString("mithaq_mock_rooms", "[]") ?: "[]"
                            val roomsArr = org.json.JSONArray(roomsStr)
                            var unreadRooms = 0
                            for (i in 0 until roomsArr.length()) {
                                val room = roomsArr.getJSONObject(i)
                                val roomId = room.getString("roomId")
                                val lastMsgTs = room.optLong("lastMessageTimestamp", 0L)
                                val lastSeenTs = badgePrefs.getLong("chat_seen_$roomId", 0L)
                                if (lastMsgTs > lastSeenTs) unreadRooms++
                            }
                            chatBadgeCount = unreadRooms
                        } else {
                            // Production: count rooms with newer lastMessageTimestamp
                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            try {
                                val snap = db.collection("chatRooms")
                                    .whereArrayContains("memberIds", currentUserId)
                                    .get()
                                    .await()
                                var unreadRooms = 0
                                for (doc in snap.documents) {
                                    val roomId = doc.id
                                    val lastMsgTs = doc.getLong("lastMessageTimestamp") ?: 0L
                                    val lastSeenTs = badgePrefs.getLong("chat_seen_$roomId", 0L)
                                    if (lastMsgTs > lastSeenTs) unreadRooms++
                                }
                                chatBadgeCount = unreadRooms
                            } catch (_: Exception) {}
                        }
                    }
                } catch (_: Exception) {}
            }
            kotlinx.coroutines.delay(10_000)
        }
    }

    // When a tab is selected, clear its badge and save "seen" count
    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            3 -> {
                likesBadgeCount = 0
                viewsBadgeCount = 0
                coroutineScopeHome.launch {
                    val totalLikers = likesRepository.getWhoLikedMe(currentUserId).size
                    val totalViews = likesRepository.getProfileVisitors(currentUserId).size
                    badgePrefs.edit()
                        .putInt("seen_likers_$currentUserId", totalLikers)
                        .putInt("seen_views_$currentUserId", totalViews)
                        .apply()
                }
            }
            2 -> {
                chatBadgeCount = 0
                val isMockMode = com.mithaq.app.Config.isMock()
                if (isMockMode) {
                    val chatPrefs = context.getSharedPreferences("mithaq_mock_chat", android.content.Context.MODE_PRIVATE)
                    val roomsStr = chatPrefs.getString("mithaq_mock_rooms", "[]") ?: "[]"
                    val roomsArr = org.json.JSONArray(roomsStr)
                    val nowTs = System.currentTimeMillis()
                    val editor = badgePrefs.edit()
                    for (i in 0 until roomsArr.length()) {
                        val roomId = roomsArr.getJSONObject(i).getString("roomId")
                        editor.putLong("chat_seen_$roomId", nowTs)
                    }
                    editor.apply()
                }
            }
        }
    }
    // ───────────────────────────────────────────────────────────────────────

    DisposableEffect(context) {
        val prefs = context.getSharedPreferences("mithaq_dev_options", android.content.Context.MODE_PRIVATE)
        isOfflineSimulated = prefs.getBoolean("is_offline_simulated", false)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == "is_offline_simulated") {
                isOfflineSimulated = p.getBoolean("is_offline_simulated", false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.appName, style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { onDarkModeChange(!isDarkMode) }) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkMode) "Light Mode" else "Dark Mode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(onClick = { onLanguageChange(!isArabic) }) {
                        Text(if (isArabic) "English" else "العربية", color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onNavigateToScreen("profile_settings") }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Profile & Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(onClick = onLogout) {
                        Text(if (isArabic) "خروج" else "Logout", color = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, label ->
                    val icon = when (index) {
                        0 -> Icons.Default.Home
                        1 -> Icons.Default.Search
                        2 -> Icons.Default.Chat
                        3 -> Icons.Default.Favorite
                        else -> Icons.Default.Person
                    }
                    val badgeCount = when (index) {
                        2 -> chatBadgeCount
                        3 -> likesBadgeCount + viewsBadgeCount
                        else -> 0
                    }
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(label) },
                        icon = {
                            if (badgeCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge {
                                            Text(
                                                text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                ) {
                                    Icon(imageVector = icon, contentDescription = label)
                                }
                            } else {
                                Icon(imageVector = icon, contentDescription = label)
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (isOfflineSimulated) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Offline Mode",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = if (isArabic) "أنت تعمل حالياً بدون اتصال بالإنترنت (وضع محاكاة)" else "Working Offline (Simulated Mode)",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                when (selectedTab) {
                    0 -> HomeDashboardContent(
                        currentUser = profile,
                        isArabic = isArabic,
                        likesCount = likesBadgeCount,
                        chatCount = chatBadgeCount,
                        viewsCount = viewsBadgeCount,
                        onOpenSearch = { selectedTab = 1 },
                        onOpenChats = { selectedTab = 2 },
                        onOpenAlerts = { selectedTab = 3 },
                        onOpenSettings = { onNavigateToScreen("app_settings") },
                        onOpenProfile = { onNavigateToScreen("profile_settings") }
                    )
                    1 -> SearchTabContent(
                        currentUser = profile,
                        viewModel = searchViewModel,
                        isArabic = isArabic,
                        onSelectMatch = { match ->
                            onNavigateToDetail(match)
                        },
                        onNavigateToUpgrade = { onNavigateToScreen("premium_store_platinum") },
                        onNavigateToScreen = onNavigateToScreen,
                        initialSubTab = 1
                    )
                    2 -> ChatTabContent(
                        currentUser = profile,
                        targetUser = selectedChatUser,
                        onSelectTargetUser = { selectedChatUser = it },
                        guardianViewModel = guardianViewModel,
                        onNavigateToUpgrade = { onNavigateToScreen("premium_store") }
                    )
                    3 -> AlertsHubContent(
                        currentUser = profile,
                        searchViewModel = searchViewModel,
                        likesRepository = likesRepository,
                        isArabic = isArabic,
                        onSelectMatch = { match -> onNavigateToDetail(match) },
                        onNavigateToUpgrade = { onNavigateToScreen("premium_store") }
                    )
                    4 -> AccountHubContent(
                        currentUser = profile,
                        isArabic = isArabic,
                        isDarkMode = isDarkMode,
                        onOpenProfile = { onNavigateToScreen("profile_settings") },
                        onOpenSettings = { onNavigateToScreen("app_settings") },
                        onOpenStats = { onNavigateToScreen("stats") },
                        onOpenSubscription = { onNavigateToScreen("premium_store") },
                        onToggleTheme = { onDarkModeChange(!isDarkMode) },
                        onLanguageChange = { onLanguageChange(!isArabic) },
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}
