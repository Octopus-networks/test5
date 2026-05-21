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
import kotlinx.coroutines.launch

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

    val currentUserProfile by authViewModel.currentUserProfile.collectAsState()

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            authViewModel.fetchCurrentUserProfile(currentUserId)
            searchViewModel.fetchUsers()
        }
    }

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
                currentUserProfile = currentUserProfile,
                authViewModel = authViewModel,
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
    currentUserProfile: UserProfile?,
    authViewModel: AuthViewModel,
    searchViewModel: SearchViewModel,
    guardianViewModel: GuardianViewModel,
    isArabic: Boolean,
    onLanguageChange: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    val strings = com.mithaq.app.ui.theme.LocalMithaqStrings.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(strings.searchTab, strings.chatTab, strings.waliTab, "Modesty")

    val profile = currentUserProfile ?: UserProfile(
        uid = currentUserId,
        name = "User",
        gender = Gender.MALE,
        age = 25,
        city = "Cairo",
        country = "Egypt"
    )

    var selectedChatUser by remember { mutableStateOf<UserProfile?>(null) }

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
                0 -> SearchTabContent(
                    currentUser = profile,
                    viewModel = searchViewModel,
                    onSelectMatch = { match ->
                        selectedChatUser = match
                        selectedTab = 1
                    }
                )
                1 -> ChatTabContent(
                    currentUser = profile,
                    targetUser = selectedChatUser
                )
                2 -> GuardianTabContent(
                    currentUser = profile,
                    viewModel = guardianViewModel,
                    onInviteSuccess = {
                        authViewModel.fetchCurrentUserProfile(currentUserId)
                    }
                )
                3 -> ModestyTabContent(
                    currentUser = profile,
                    targetUser = selectedChatUser,
                    onRefreshProfile = {
                        authViewModel.fetchCurrentUserProfile(currentUserId)
                    }
                )
            }
        }
    }
}

@Composable
fun SearchTabContent(
    currentUser: UserProfile,
    viewModel: SearchViewModel,
    onSelectMatch: (UserProfile) -> Unit
) {
    var showFilters by remember { mutableStateOf(false) }
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

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

        if (isLoading) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(text = error ?: "An error occurred", color = MaterialTheme.colorScheme.error)
            }
        } else if (searchResults.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(text = "No matches found. Try adjusting filters.", modifier = Modifier.padding(16.dp))
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                searchResults.forEach { profile ->
                    val score = MatchScoreCalculator.calculateScore(currentUser, profile)
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        onClick = { onSelectMatch(profile) }
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
fun ChatTabContent(
    currentUser: UserProfile,
    targetUser: UserProfile?
) {
    if (targetUser == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Select a profile from the Search tab to start chatting.", textAlign = TextAlign.Center)
        }
        return
    }

    val roomId = remember(currentUser.uid, targetUser.uid) {
        val first = minOf(currentUser.uid, targetUser.uid)
        val second = maxOf(currentUser.uid, targetUser.uid)
        "${first}_${second}"
    }

    val chatViewModel = remember(roomId) { ChaperonedChatViewModel(roomId) }
    val chatRoomState by chatViewModel.chatRoom.collectAsState()
    val messages by chatViewModel.messages.collectAsState()

    var messageText by remember { mutableStateOf("") }
    val strings = com.mithaq.app.ui.theme.LocalMithaqStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val isChaperoned = chatRoomState?.isChaperoned ?: true
        val waliEmail = chatRoomState?.waliEmail ?: (currentUser.guardianEmail ?: "guardian@mithaq.com")

        ChaperonedChatBanner(
            waliEmail = waliEmail,
            isChaperoned = isChaperoned
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            messages.forEach { msg ->
                val isMe = msg.senderId == currentUser.uid
                ChatBubble(
                    messageText = msg.content,
                    isCurrentUser = isMe
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text(if (strings.appName == "ميثاق") "اكتب رسالة جادة..." else "Write serious message...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (messageText.isNotBlank()) {
                        chatViewModel.sendChatMessage(messageText)
                        messageText = ""
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (strings.appName == "ميثاق") "إرسال" else "Send")
            }
        }
    }
}

@Composable
fun GuardianTabContent(
    currentUser: UserProfile,
    viewModel: GuardianViewModel,
    onInviteSuccess: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    val guardianName = currentUser.guardianName
    val guardianEmail = currentUser.guardianEmail
    val guardianStatus = currentUser.guardianStatus ?: "None"

    LaunchedEffect(uiState) {
        if (uiState is com.mithaq.app.ui.guardian.GuardianUiState.Success) {
            onInviteSuccess()
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(if (strings.appName == "ميثاق") "اسم الولي: $guardianName" else "Guardian Name: $guardianName", fontWeight = FontWeight.Bold)
                    Text(if (strings.appName == "ميثاق") "بريد الولي: $guardianEmail" else "Guardian Email: $guardianEmail")
                    Text(if (strings.appName == "ميثاق") "الحالة: $guardianStatus" else "Status: $guardianStatus", style = MaterialTheme.typography.labelSmall)
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
fun ModestyTabContent(
    currentUser: UserProfile,
    targetUser: UserProfile?,
    onRefreshProfile: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val photoAccessManager = remember { com.mithaq.app.ui.photo.PhotoAccessManager() }
    var photoState by remember { mutableStateOf(com.mithaq.app.ui.photo.PhotoAccessState.NONE) }

    LaunchedEffect(currentUser.uid, targetUser?.uid) {
        if (targetUser != null) {
            photoState = photoAccessManager.checkPhotoAccessState(currentUser.uid, targetUser.uid)
        }
    }

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

        if (targetUser != null) {
            Text(
                text = "Viewing Match's Photo: ${targetUser.name}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.Start)
            )
            
            PhotoAccessRequestCard(
                isOwnProfile = false,
                accessState = photoState,
                onRequestAccessClicked = {
                    coroutineScope.launch {
                        val success = photoAccessManager.requestPhotoAccess(currentUser.uid, targetUser.uid)
                        if (success) {
                            photoState = com.mithaq.app.ui.photo.PhotoAccessState.PENDING
                            onRefreshProfile()
                        }
                    }
                }
            )
        } else {
            Text(
                text = "Select a user on the Search tab to view their photo unlock options.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Pending Requests to View Your Photo",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.align(Alignment.Start)
        )

        val requests = currentUser.photoAccessRequests
        if (requests.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No pending requests.", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            PhotoAccessRequestCard(
                isOwnProfile = true,
                accessState = com.mithaq.app.ui.photo.PhotoAccessState.NONE,
                onRequestAccessClicked = {},
                pendingRequests = requests,
                onApproveClicked = { userId ->
                    coroutineScope.launch {
                        val success = photoAccessManager.approvePhotoAccess(currentUser.uid, userId)
                        if (success) {
                            onRefreshProfile()
                        }
                    }
                }
            )
        }
    }
}
