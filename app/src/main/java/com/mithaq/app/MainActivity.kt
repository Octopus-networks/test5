package com.mithaq.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.mithaq.app.ui.theme.MithaqTheme

class MainActivity : ComponentActivity() {

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
            MithaqTheme(isArabic = isArabic) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MithaqAppNavigation(
                        isArabic = isArabic,
                        onLanguageChange = { isArabic = it }
                    )
                }
            }
        }
    }
}

@Composable
fun MithaqAppNavigation(
    isArabic: Boolean,
    onLanguageChange: (Boolean) -> Unit
) {
    var currentScreen by remember { mutableStateOf("login") }
    var currentUserId by remember { mutableStateOf("") }

    // ViewModels initialized safely
    val authViewModel = remember { AuthViewModel() }
    val searchViewModel = remember { SearchViewModel() }
    val guardianViewModel = remember { GuardianViewModel() }

    when (currentScreen) {
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
            HomeScreen(
                currentUserId = currentUserId,
                searchViewModel = searchViewModel,
                guardianViewModel = guardianViewModel,
                isArabic = isArabic,
                onLanguageChange = onLanguageChange,
                onLogout = {
                    authViewModel.signOut()
                    currentScreen = "login"
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUserId: String,
    searchViewModel: SearchViewModel,
    guardianViewModel: GuardianViewModel,
    isArabic: Boolean,
    onLanguageChange: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    val strings = com.mithaq.app.ui.theme.LocalMithaqStrings.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(strings.searchTab, strings.chatTab, strings.waliTab, "Modesty")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.appName, style = MaterialTheme.typography.titleLarge) },
                actions = {
                    TextButton(onClick = { onLanguageChange(!isArabic) }) {
                        Text(if (isArabic) "English" else "العربية", color = MaterialTheme.colorScheme.primary)
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
                        0 -> Icons.Default.Search
                        1 -> Icons.Default.Chat
                        2 -> Icons.Default.Person
                        else -> Icons.Default.Lock
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
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                0 -> SearchTabContent(searchViewModel)
                1 -> ChatTabContent()
                2 -> GuardianTabContent(guardianViewModel)
                3 -> ModestyTabContent()
            }
        }
    }
}

@Composable
fun SearchTabContent(viewModel: SearchViewModel) {
    var showFilters by remember { mutableStateOf(false) }
    val currentCriteria by viewModel.filterCriteria.collectAsState()
    
    // Hardcoded profiles to calculate compatibility score against
    val currentUser = UserProfile(
        sect = Sect.SUNNI,
        prayerFrequency = PrayerFrequency.ALWAYS,
        modestyPreference = ModestyPreference.HIJAB,
        relocationWillingness = RelocationWillingness.OPEN,
        age = 26
    )

    val mockProfiles = remember {
        listOf(
            UserProfile(name = "Fatima Al-Hassan", age = 24, sect = Sect.SUNNI, prayerFrequency = PrayerFrequency.ALWAYS, modestyPreference = ModestyPreference.HIJAB, relocationWillingness = RelocationWillingness.YES),
            UserProfile(name = "Aisha Mansoor", age = 23, sect = Sect.SUNNI, prayerFrequency = PrayerFrequency.USUALLY, modestyPreference = ModestyPreference.NIQAB, relocationWillingness = RelocationWillingness.NO),
            UserProfile(name = "Omar Farooq", age = 28, sect = Sect.SUNNI, prayerFrequency = PrayerFrequency.ALWAYS, modestyPreference = ModestyPreference.NONE, relocationWillingness = RelocationWillingness.OPEN),
            UserProfile(name = "Zainab Al-Majed", age = 25, sect = Sect.SHIA, prayerFrequency = PrayerFrequency.SOMETIMES, modestyPreference = ModestyPreference.HIJAB, relocationWillingness = RelocationWillingness.OPEN)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { showFilters = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open Search Filters Sheet")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Islamic Match Compatibility Matches",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        mockProfiles.forEach { profile ->
            val score = MatchScoreCalculator.calculateScore(currentUser, profile)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = profile.name, fontWeight = FontWeight.Bold)
                        Text(text = "${profile.age} yrs • ${profile.sect.displayName}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "Modesty: ${profile.modestyPreference.displayName}", style = MaterialTheme.typography.bodySmall)
                    }
                    MatchScoreBadge(score = score, size = 50.dp)
                }
            }
        }

        if (showFilters) {
            SearchFilterBottomSheet(
                onDismissRequest = { showFilters = false },
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun ChatTabContent() {
    val mockChatViewModel = remember { ChaperonedChatViewModel("demo_room_1") }
    val chatRoomState by mockChatViewModel.chatRoom.collectAsState()

    var mockMessageText by remember { mutableStateOf("") }
    val mockMessages = remember {
        mutableStateListOf(
            "Assalamu Alaikum, I would like to inquire about your requirements." to "السلام عليكم، أود الاستفسار عن شروطك للموافقة.",
            "Wa Alaikum Assalam, my guardian is aware. Here are my conditions." to "وعليكم السلام، ولي أمري على علم بكل التفاصيل. إليك شروطي."
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Wali monitoring banner
        ChaperonedChatBanner(
            waliEmail = "guardian@mithaq.com",
            isChaperoned = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Chat logs
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            mockMessages.forEach { (en, ar) ->
                ChatBubble(
                    messageText = en,
                    isCurrentUser = false
                )
            }
        }

        // Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = mockMessageText,
                onValueChange = { mockMessageText = it },
                placeholder = { Text("Write serious message...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (mockMessageText.isNotBlank()) {
                        mockMessages.add(mockMessageText to "ترجمة: $mockMessageText")
                        mockMessageText = ""
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Send")
            }
        }
    }
}

@Composable
fun GuardianTabContent(viewModel: GuardianViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    var guardianName by remember { mutableStateOf<String?>(null) }
    var guardianEmail by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState) {
        if (uiState is com.mithaq.app.ui.guardian.GuardianUiState.Success) {
            guardianName = "Ahmad Al-Wali"
            guardianEmail = "guardian@mithaq.com"
        }
    }

    val strings = com.mithaq.app.ui.theme.LocalMithaqStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = strings.waliIntegration,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = strings.waliDescription,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        if (!guardianName.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(if (strings.appName == "ميثاق") "اسم الولي: $guardianName" else "Guardian Name: $guardianName", fontWeight = FontWeight.Bold)
                    Text(if (strings.appName == "ميثاق") "بريد الولي: $guardianEmail" else "Guardian Email: $guardianEmail")
                    Text(if (strings.appName == "ميثاق") "الحالة: قيد التحقق" else "Status: Pending Verification", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Button(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (guardianName.isNullOrBlank()) strings.inviteWali else if (strings.appName == "ميثاق") "تعديل بيانات الولي" else "Update Wali Details")
        }

        if (showDialog) {
            InviteGuardianDialog(
                viewModel = viewModel,
                onDismissRequest = { showDialog = false },
                titleText = strings.inviteWali,
                subtitleText = if (strings.appName == "ميثاق") "في الزواج الإسلامي، إشراك ولي الأمر يضمن رحلة مباركة، شفافة، وآمنة." else "In Islamic matchmaking, involving a guardian ensures a blessed, transparent, and safe journey.",
                nameLabel = strings.nameLabel,
                emailLabel = strings.emailLabel,
                submitButtonText = if (strings.appName == "ميثاق") "إرسال الدعوة" else "Send Invitation",
                cancelButtonText = if (strings.appName == "ميثاق") "إلغاء" else "Cancel",
                successTitle = if (strings.appName == "ميثاق") "تم إرسال الدعوة" else "Invitation Sent",
                successSubtitle = if (strings.appName == "ميثاق") "تم إرسال دعوة إلى ولي أمرك. سنقوم بإشعارك بمجرد قبوله." else "An invitation has been sent to your Guardian. We will notify you once they accept.",
                closeButtonText = if (strings.appName == "ميثاق") "إغلاق" else "Close"
            )
        }
    }
}

@Composable
fun ModestyTabContent() {
    var photoState by remember { mutableStateOf(PhotoAccessState.NONE) }
    val requests = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Photo Modesty Control",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Demonstration Card as a viewer
        Text(
            text = "Demonstrating: Viewing Another Match's Photo",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.align(Alignment.Start)
        )
        
        PhotoAccessRequestCard(
            isOwnProfile = false,
            accessState = photoState,
            onRequestAccessClicked = {
                photoState = PhotoAccessState.PENDING
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Demonstration Card as profile owner
        Text(
            text = "Demonstrating: Managing Requests to View Your Photo",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.align(Alignment.Start)
        )
        
        var mockRequests by remember { mutableStateOf(listOf("Omar Farooq")) }

        PhotoAccessRequestCard(
            isOwnProfile = true,
            accessState = PhotoAccessState.NONE,
            onRequestAccessClicked = {},
            pendingRequests = mockRequests,
            onApproveClicked = { name ->
                mockRequests = mockRequests.filter { it != name }
                photoState = PhotoAccessState.APPROVED // Approve access for testing
            }
        )
    }
}
