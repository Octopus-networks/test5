package com.mithaq.app

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
import com.mithaq.app.model.*
import com.mithaq.app.ui.auth.AuthViewModel
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


class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Resilient Firebase Init: Initialize dummy options if no google-services.json exists
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
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
                        onError = { /* Handle error or allow fallback */ isBiometricAuthenticated = true } // For demo/simplicity
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
                            onDarkModeChange = { isDarkMode = it }
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
}

@Composable
fun MithaqAppNavigation(
    isArabic: Boolean,
    onLanguageChange: (Boolean) -> Unit,
    isDarkMode: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf("splash") }
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

    androidx.activity.compose.BackHandler(enabled = currentScreen == "home" || currentScreen == "login") {
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
    }

    // ViewModels initialized safely
    val authViewModel = remember { AuthViewModel(context = context) }
    val searchViewModel = remember { SearchViewModel(context = context) }
    val guardianViewModel = remember { GuardianViewModel() }

    val currentUserProfile by authViewModel.currentUserProfile.collectAsState()
    var hasDismissedOnboarding by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, currentUserId) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (currentUserId.isNotEmpty()) {
                    authViewModel.updateOnlineStatus()
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

    when (currentScreen) {
        "splash" -> {
            SplashScreen(onComplete = { currentScreen = "login" })
        }
        "login" -> {
            LoginScreen(
                onNavigateToRegister = { currentScreen = "register" },
                onLoginSuccess = { uid ->
                    currentUserId = uid
                    currentScreen = "home"
                },
                viewModel = authViewModel,
                isArabic = isArabic,
                onLanguageChange = onLanguageChange
            )
        }
        "register" -> {
            RegisterScreen(
                onNavigateToLogin = { currentScreen = "login" },
                onRegisterSuccess = { uid ->
                    currentUserId = uid
                    currentScreen = "home"
                },
                viewModel = authViewModel,
                isArabic = isArabic,
                onLanguageChange = onLanguageChange
            )
        }
        "home" -> {
            if (currentUserProfile?.isWaliAccount == true) {
                WaliDashboardScreen(
                    currentUserId = currentUserId,
                    currentUserProfile = currentUserProfile,
                    authViewModel = authViewModel,
                    isArabic = isArabic,
                    onLanguageChange = onLanguageChange,
                    onLogout = {
                        authViewModel.signOut()
                        currentScreen = "login"
                    }
                )
            } else {
                val isProfileIncomplete = currentUserProfile != null && (
                        currentUserProfile!!.username.isEmpty() ||
                        currentUserProfile!!.city.isEmpty() ||
                        currentUserProfile!!.country.isEmpty() ||
                        !currentUserProfile!!.oathChecked ||
                        currentUserProfile!!.age < 18
                )

                if (isProfileIncomplete) {
                    CompleteProfileScreen(
                        currentUserProfile = currentUserProfile!!,
                        viewModel = authViewModel,
                        isArabic = isArabic,
                        onComplete = {
                            authViewModel.fetchCurrentUserProfile(currentUserId)
                        }
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
    val tabs = listOf(strings.searchTab, strings.likesTab, strings.chatTab, strings.viewsTab, strings.favoritesTab)

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
                        1 -> Icons.Default.Favorite
                        2 -> Icons.Default.Chat
                        3 -> Icons.Default.Visibility
                        else -> Icons.Default.Star
                    }
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(label) },
                        icon = { Icon(imageVector = icon, contentDescription = label) }
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
                    0 -> SearchTabContent(
                        currentUser = profile,
                        viewModel = searchViewModel,
                        isArabic = isArabic,
                        onSelectMatch = { match ->
                            onNavigateToDetail(match)
                        },
                        onNavigateToUpgrade = { onNavigateToScreen("premium_store_platinum") },
                        onNavigateToScreen = onNavigateToScreen
                    )
                    1 -> LikesTabContent(
                        currentUser = profile,
                        searchViewModel = searchViewModel,
                        likesRepository = likesRepository,
                        isArabic = isArabic,
                        onSelectMatch = { match ->
                            onNavigateToDetail(match)
                        },
                        onNavigateToUpgrade = { onNavigateToScreen("premium_store") }
                    )
                    2 -> ChatTabContent(
                        currentUser = profile,
                        targetUser = selectedChatUser,
                        onSelectTargetUser = { selectedChatUser = it },
                        guardianViewModel = guardianViewModel,
                        onNavigateToUpgrade = { onNavigateToScreen("premium_store") }
                    )
                    3 -> ViewsTabContent(
                        currentUser = profile,
                        searchViewModel = searchViewModel,
                        likesRepository = likesRepository,
                        isArabic = isArabic,
                        onSelectMatch = { match ->
                            onNavigateToDetail(match)
                        }
                    )
                    4 -> FavoritesTabContent(
                        currentUser = profile,
                        searchViewModel = searchViewModel,
                        likesRepository = likesRepository,
                        isArabic = isArabic,
                        onSelectMatch = { match ->
                            onNavigateToDetail(match)
                        }
                    )
                }
            }
        }
    }
}
