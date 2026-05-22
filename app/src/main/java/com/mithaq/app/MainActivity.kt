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
import androidx.compose.material.icons.filled.Settings
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
import com.mithaq.app.ui.admin.AdminConsoleScreen
import com.mithaq.app.ui.limit.PremiumStoreScreen
import com.mithaq.app.ui.match.QuestionnaireScreen
import com.mithaq.app.ui.match.CompatibilityBreakdownDialog
import com.mithaq.app.ui.chat.ChaperonedVoiceCallScreen
import com.mithaq.app.ui.chat.CallState
import com.mithaq.app.security.SecureScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning


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
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext

    // ViewModels initialized safely
    val authViewModel = remember { AuthViewModel(context = context) }
    val searchViewModel = remember { SearchViewModel(context = context) }
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
                    },
                    onNavigateToScreen = { currentScreen = it }
                )
            }
        }
        "admin" -> {
            androidx.activity.compose.BackHandler { currentScreen = "home" }
            AdminConsoleScreen(
                viewModel = authViewModel,
                isArabic = isArabic,
                onBack = { currentScreen = "home" }
            )
        }
        "premium_store" -> {
            androidx.activity.compose.BackHandler { currentScreen = "home" }
            PremiumStoreScreen(
                viewModel = authViewModel,
                isArabic = isArabic,
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
    onLogout: () -> Unit,
    onNavigateToScreen: (String) -> Unit
) {
    val strings = com.mithaq.app.ui.theme.LocalMithaqStrings.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(strings.searchTab, strings.chatTab, strings.waliTab, strings.profileTab)

    val profile = currentUserProfile ?: UserProfile(
        uid = currentUserId,
        name = "User",
        gender = Gender.MALE,
        age = 25,
        city = "Cairo",
        country = "Egypt"
    )

    var selectedChatUser by remember { mutableStateOf<UserProfile?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var isOfflineSimulated by remember { mutableStateOf(false) }

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
                        else -> Icons.Default.Settings
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
                            selectedChatUser = match
                            selectedTab = 1
                        }
                    )
                    1 -> ChatTabContent(
                        currentUser = profile,
                        targetUser = selectedChatUser,
                        onSelectTargetUser = { selectedChatUser = it }
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
                        },
                        isArabic = isArabic,
                        authViewModel = authViewModel,
                        onNavigateToScreen = onNavigateToScreen
                    )
                }
            }
        }
    }
}

@Composable
fun SearchTabContent(
    currentUser: UserProfile,
    viewModel: SearchViewModel,
    isArabic: Boolean,
    onSelectMatch: (UserProfile) -> Unit
) {
    var showFilters by remember { mutableStateOf(false) }
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    var breakdownPartner by remember { mutableStateOf<UserProfile?>(null) }

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
            Text(if (isArabic) "فتح شاشة تصفية البحث" else "Open Search Filters Sheet")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isArabic) "التوافقات المقترحة للزواج الشرعي" else "Islamic Match Compatibility Matches",
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
                Text(text = if (isArabic) "لا توجد نتائج مطابقة. يرجى تعديل خيارات البحث." else "No matches found. Try adjusting filters.", modifier = Modifier.padding(16.dp))
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                val isAccessApproved = profile.photoAccessApprovedUsers.contains(currentUser.uid)
                                Box(modifier = Modifier.size(52.dp)) {
                                    UserProfileImage(
                                        imageUrl = profile.imageUrl,
                                        gender = profile.gender,
                                        isBlurred = !isAccessApproved,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = profile.name, fontWeight = FontWeight.Bold)
                                        VerificationBadge(status = profile.verificationStatus)
                                    }
                                    val sectLabel = profile.sect.getDisplayName(isArabic)
                                    val ageSuffix = if (isArabic) "سنة" else "yrs"
                                    val modestyLabel = if (isArabic) "الحشمة: " else "Modesty: "
                                    Text(text = "${profile.age} $ageSuffix • $sectLabel", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "$modestyLabel${profile.modestyPreference.getDisplayName(isArabic)}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Box(
                                modifier = Modifier.clickable {
                                    breakdownPartner = profile
                                }
                            ) {
                                MatchScoreBadge(score = score, size = 50.dp)
                            }
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

    if (breakdownPartner != null) {
        CompatibilityBreakdownDialog(
            userAnswers = currentUser.questionnaireAnswers,
            partnerAnswers = breakdownPartner!!.questionnaireAnswers,
            partnerName = breakdownPartner!!.name,
            isArabic = isArabic,
            onDismiss = { breakdownPartner = null }
        )
    }
}

private fun getMockUserProfile(uid: String): UserProfile {
    return when (uid) {
        "mock_user_2" -> UserProfile(
            uid = "mock_user_2",
            name = "Fatima Al-Zahra",
            gender = Gender.FEMALE,
            age = 24,
            city = "Riyadh",
            country = "Saudi Arabia",
            imageUrl = "",
            sect = Sect.SUNNI,
            prayerFrequency = PrayerFrequency.ALWAYS,
            modestyPreference = ModestyPreference.HIJAB,
            relocationWillingness = RelocationWillingness.YES,
            polygamyAcceptance = false
        )
        "mock_user_3" -> UserProfile(
            uid = "mock_user_3",
            name = "Aisha Khan",
            gender = Gender.FEMALE,
            age = 27,
            city = "Dubai",
            country = "UAE",
            imageUrl = "",
            sect = Sect.SUNNI,
            prayerFrequency = PrayerFrequency.ALWAYS,
            modestyPreference = ModestyPreference.NIQAB,
            relocationWillingness = RelocationWillingness.OPEN,
            polygamyAcceptance = true
        )
        "mock_user_4" -> UserProfile(
            uid = "mock_user_4",
            name = "Yasmin Masri",
            gender = Gender.FEMALE,
            age = 22,
            city = "Cairo",
            country = "Egypt",
            imageUrl = "",
            sect = Sect.SUNNI,
            prayerFrequency = PrayerFrequency.USUALLY,
            modestyPreference = ModestyPreference.HIJAB,
            relocationWillingness = RelocationWillingness.NO,
            polygamyAcceptance = false
        )
        else -> UserProfile(
            uid = uid,
            name = if (uid == "mock_user_123" || uid == "mock_user") "Mock User" else "Partner $uid",
            gender = Gender.FEMALE,
            age = 25,
            city = "Cairo",
            country = "Egypt"
        )
    }
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

@Composable
fun ChatTabContent(
    currentUser: UserProfile,
    targetUser: UserProfile?,
    onSelectTargetUser: (UserProfile?) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val strings = com.mithaq.app.ui.theme.LocalMithaqStrings.current
    
    val isMock = try {
        com.google.firebase.firestore.FirebaseFirestore.getInstance().app?.options?.apiKey == "mock-api-key-for-testing" ||
        com.google.firebase.firestore.FirebaseFirestore.getInstance().app?.options?.apiKey?.contains("mock") == true
    } catch (e: Exception) {
        true
    }

    if (targetUser == null) {
        var activeRooms by remember { mutableStateOf<List<ChatRoom>>(emptyList()) }
        var roomPartners by remember { mutableStateOf<Map<String, UserProfile>>(emptyMap()) }
        var isLoadingRooms by remember { mutableStateOf(false) }

        LaunchedEffect(currentUser.uid) {
            isLoadingRooms = true
            if (isMock) {
                val prefs = context.getSharedPreferences("mithaq_mock_chat", android.content.Context.MODE_PRIVATE)
                val roomsJsonStr = prefs.getString("mithaq_mock_rooms", null)
                val roomsList = mutableListOf<ChatRoom>()
                if (roomsJsonStr != null) {
                    try {
                        val array = org.json.JSONArray(roomsJsonStr)
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val roomId = obj.getString("roomId")
                            val memberIdsArr = obj.getJSONArray("memberIds")
                            val memberIds = mutableListOf<String>()
                            for (j in 0 until memberIdsArr.length()) {
                                memberIds.add(memberIdsArr.getString(j))
                            }
                            val isChaperoned = obj.optBoolean("isChaperoned", false)
                            val waliEmail = obj.optString("waliEmail", null)
                            val lastMessage = obj.optString("lastMessage", null)
                            val lastMessageTimestamp = obj.optLong("lastMessageTimestamp", 0L)
                            roomsList.add(ChatRoom(roomId, memberIds, isChaperoned, waliEmail, lastMessage, lastMessageTimestamp))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                if (roomsList.isEmpty()) {
                    val defaultRoom = ChatRoom(
                        roomId = "${currentUser.uid}_mock_user_2",
                        memberIds = listOf(currentUser.uid, "mock_user_2"),
                        isChaperoned = true,
                        waliEmail = currentUser.guardianEmail ?: "guardian@mithaq.com",
                        lastMessage = "وعليكم السلام، ولي أمري على علم بكل التفاصيل. إليك شروطي.",
                        lastMessageTimestamp = 1716200020000L
                    )
                    roomsList.add(defaultRoom)
                    
                    val array = org.json.JSONArray()
                    val obj = org.json.JSONObject()
                    obj.put("roomId", defaultRoom.roomId)
                    val mArr = org.json.JSONArray()
                    mArr.put(currentUser.uid)
                    mArr.put("mock_user_2")
                    obj.put("memberIds", mArr)
                    obj.put("isChaperoned", defaultRoom.isChaperoned)
                    obj.put("waliEmail", defaultRoom.waliEmail)
                    obj.put("lastMessage", defaultRoom.lastMessage)
                    obj.put("lastMessageTimestamp", defaultRoom.lastMessageTimestamp)
                    array.put(obj)
                    
                    prefs.edit().putString("mithaq_mock_rooms", array.toString()).apply()
                }
                
                activeRooms = roomsList.sortedByDescending { it.lastMessageTimestamp }
                val partnersMap = mutableMapOf<String, UserProfile>()
                for (room in roomsList) {
                    val partnerId = room.memberIds.firstOrNull { it != currentUser.uid } ?: "mock_user_2"
                    partnersMap[room.roomId] = getMockUserProfile(partnerId)
                }
                roomPartners = partnersMap
                isLoadingRooms = false
            } else {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("chatRooms")
                    .whereArrayContains("memberIds", currentUser.uid)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val roomsList = snapshot.documents.mapNotNull { doc ->
                            try {
                                val roomId = doc.id
                                val memberIds = doc.get("memberIds") as? List<String> ?: emptyList()
                                val isChaperoned = doc.getBoolean("isChaperoned") ?: false
                                val waliEmail = doc.getString("waliEmail")
                                val lastMessage = doc.getString("lastMessage")
                                val lastMessageTimestamp = doc.getLong("lastMessageTimestamp") ?: 0L
                                ChatRoom(roomId, memberIds, isChaperoned, waliEmail, lastMessage, lastMessageTimestamp)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        
                        activeRooms = roomsList.sortedByDescending { it.lastMessageTimestamp }
                        val partnersMap = mutableMapOf<String, UserProfile>()
                        val partnerFetchCount = roomsList.size
                        if (partnerFetchCount == 0) {
                            isLoadingRooms = false
                        } else {
                            var fetched = 0
                            for (room in roomsList) {
                                val partnerId = room.memberIds.firstOrNull { it != currentUser.uid }
                                if (partnerId != null) {
                                    db.collection("users").document(partnerId).get()
                                        .addOnSuccessListener { userDoc ->
                                            if (userDoc.exists()) {
                                                val name = userDoc.getString("name") ?: ""
                                                val genderStr = userDoc.getString("gender") ?: "FEMALE"
                                                val gender = if (genderStr == "MALE") Gender.MALE else Gender.FEMALE
                                                val age = userDoc.getLong("age")?.toInt() ?: 25
                                                val city = userDoc.getString("city") ?: ""
                                                val country = userDoc.getString("country") ?: ""
                                                val imageUrl = userDoc.getString("imageUrl") ?: ""
                                                val sectStr = userDoc.getString("sect") ?: "SUNNI"
                                                val sect = try { Sect.valueOf(sectStr) } catch(e: Exception) { Sect.SUNNI }
                                                val prayerStr = userDoc.getString("prayerFrequency") ?: "ALWAYS"
                                                val prayer = try { PrayerFrequency.valueOf(prayerStr) } catch(e: Exception) { PrayerFrequency.ALWAYS }
                                                val modestyStr = userDoc.getString("modestyPreference") ?: "HIJAB"
                                                val modesty = try { ModestyPreference.valueOf(modestyStr) } catch(e: Exception) { ModestyPreference.HIJAB }
                                                val relocationStr = userDoc.getString("relocationWillingness") ?: "OPEN"
                                                val relocation = try { RelocationWillingness.valueOf(relocationStr) } catch(e: Exception) { RelocationWillingness.OPEN }
                                                val approved = userDoc.get("photoAccessApprovedUsers") as? List<String> ?: emptyList()
                                                val requests = userDoc.get("photoAccessRequests") as? List<String> ?: emptyList()

                                                partnersMap[room.roomId] = UserProfile(
                                                    uid = partnerId,
                                                    name = name,
                                                    gender = gender,
                                                    age = age,
                                                    city = city,
                                                    country = country,
                                                    imageUrl = imageUrl,
                                                    sect = sect,
                                                    prayerFrequency = prayer,
                                                    modestyPreference = modesty,
                                                    relocationWillingness = relocation,
                                                    photoAccessApprovedUsers = approved,
                                                    photoAccessRequests = requests
                                                )
                                            }
                                            fetched++
                                            if (fetched == partnerFetchCount) {
                                                roomPartners = partnersMap
                                                isLoadingRooms = false
                                            }
                                        }
                                        .addOnFailureListener {
                                            fetched++
                                            if (fetched == partnerFetchCount) {
                                                roomPartners = partnersMap
                                                isLoadingRooms = false
                                            }
                                        }
                                } else {
                                    fetched++
                                    if (fetched == partnerFetchCount) {
                                        roomPartners = partnersMap
                                        isLoadingRooms = false
                                    }
                                }
                            }
                        }
                    }
                    .addOnFailureListener {
                        isLoadingRooms = false
                    }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = if (strings.appName == "ميثاق") "المحادثات النشطة" else "Active Conversations",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (isLoadingRooms) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (activeRooms.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (strings.appName == "ميثاق") 
                            "لا توجد محادثات نشطة حالياً. ابحث عن شريك وبادر بمراسلته!" 
                        else 
                            "No active conversations yet. Find a partner in Search to start chatting!",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeRooms.forEach { room ->
                        val partner = roomPartners[room.roomId] ?: getMockUserProfile(room.memberIds.firstOrNull { it != currentUser.uid } ?: "")
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectTargetUser(partner) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(52.dp)) {
                                    UserProfileImage(
                                        imageUrl = partner.imageUrl,
                                        gender = partner.gender,
                                        isBlurred = !partner.photoAccessApprovedUsers.contains(currentUser.uid),
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = partner.name, fontWeight = FontWeight.Bold)
                                        VerificationBadge(status = partner.verificationStatus)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = room.lastMessage ?: "",
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (room.lastMessageTimestamp > 0L) {
                                    Text(
                                        text = formatDateTime(room.lastMessageTimestamp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        return
    }

    androidx.activity.compose.BackHandler {
        onSelectTargetUser(null)
    }

    val roomId = remember(currentUser.uid, targetUser.uid) {
        val first = minOf(currentUser.uid, targetUser.uid)
        val second = maxOf(currentUser.uid, targetUser.uid)
        "${first}_${second}"
    }

    val chatViewModel = remember(roomId) { ChaperonedChatViewModel(roomId = roomId, context = context) }
    val chatRoomState by chatViewModel.chatRoom.collectAsState()
    val messages by chatViewModel.messages.collectAsState()
    val warningState by chatViewModel.warningState.collectAsState()
    val callState by chatViewModel.callState.collectAsState()

    var messageText by remember { mutableStateOf("") }

    if (warningState != null) {
        AlertDialog(
            onDismissRequest = { chatViewModel.clearWarning() },
            title = { Text(if (strings.appName == "ميثاق") "تنبيه أمان" else "Security Alert") },
            text = { Text(warningState!!) },
            confirmButton = {
                TextButton(onClick = { chatViewModel.clearWarning() }) {
                    Text(if (strings.appName == "ميثاق") "موافق" else "OK")
                }
            }
        )
    }

    if (callState == CallState.ACTIVE) {
        ChaperonedVoiceCallScreen(
            partnerName = targetUser.name,
            waliName = chatRoomState?.waliEmail ?: (currentUser.guardianEmail ?: "Wali"),
            isWaliAccess = false,
            isArabic = strings.appName == "ميثاق",
            onEndCall = { chatViewModel.endCall() }
        )
    } else {
        SecureScreen {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Chat Header Row with Back Button
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onSelectTargetUser(null) }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back to conversation list")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(40.dp)) {
                        UserProfileImage(
                            imageUrl = targetUser.imageUrl,
                            gender = targetUser.gender,
                            isBlurred = !targetUser.photoAccessApprovedUsers.contains(currentUser.uid),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = targetUser.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            VerificationBadge(status = targetUser.verificationStatus)
                        }
                        Text(
                            text = if (targetUser.photoAccessApprovedUsers.contains(currentUser.uid)) 
                                (if (strings.appName == "ميثاق") "الصورة واضحة" else "Photo Visible")
                            else 
                                (if (strings.appName == "ميثاق") "الصورة مغطاة للحشمة" else "Photo Blurred"), 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { chatViewModel.requestCall() }) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Voice Call",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                val isChaperoned = chatRoomState?.isChaperoned ?: true
                val waliEmail = chatRoomState?.waliEmail ?: (currentUser.guardianEmail ?: "guardian@mithaq.com")

                ChaperonedChatBanner(
                    waliEmail = waliEmail,
                    isChaperoned = isChaperoned
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Wali request acceptance banner
                if (callState == CallState.REQUESTED) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (strings.appName == "ميثاق") "مكالمة صوتية مطلوبة (بانتظار الولي)" else "Voice Call Requested (Waiting for Wali)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = if (strings.appName == "ميثاق") "محاكاة: انقر للموافقة كولي أمر والبدء" else "Simulation: Click Accept as Wali to start the call",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Button(
                                onClick = { chatViewModel.acceptCall() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Text(if (strings.appName == "ميثاق") "قبول الولي" else "Wali Accept")
                            }
                        }
                    }
                }

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
    }

    if (callState == CallState.ENDED) {
        AlertDialog(
            onDismissRequest = { chatViewModel.resetCall() },
            title = { Text(if (strings.appName == "ميثاق") "انتهت المكالمة" else "Call Ended") },
            text = { Text(if (strings.appName == "ميثاق") "تم إنهاء المكالمة الصوتية المشرف عليها بنجاح." else "The chaperoned voice call has ended successfully.") },
            confirmButton = {
                TextButton(onClick = { chatViewModel.resetCall() }) {
                    Text(if (strings.appName == "ميثاق") "موافق" else "OK")
                }
            }
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModestyTabContent(
    currentUser: UserProfile,
    targetUser: UserProfile?,
    onRefreshProfile: () -> Unit,
    isArabic: Boolean,
    authViewModel: AuthViewModel,
    onNavigateToScreen: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val photoAccessManager = remember { com.mithaq.app.ui.photo.PhotoAccessManager() }
    var photoState by remember { mutableStateOf(com.mithaq.app.ui.photo.PhotoAccessState.NONE) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var isUploadingImage by remember { mutableStateOf(false) }

    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isUploadingImage = true
            coroutineScope.launch {
                val isMock = try {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    db.app?.options?.apiKey == "mock-api-key-for-testing" || db.app?.options?.apiKey?.contains("mock") == true
                } catch (e: Exception) {
                    true
                }

                val finalUrl = if (isMock) {
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val directory = java.io.File(context.filesDir, "profiles")
                        if (!directory.exists()) {
                            directory.mkdirs()
                        }
                        val localFile = java.io.File(directory, "${currentUser.uid}.jpg")
                        val outputStream = java.io.FileOutputStream(localFile)
                        inputStream?.use { input ->
                            outputStream.use { output ->
                                input.copyTo(output)
                            }
                        }
                        android.net.Uri.fromFile(localFile).toString()
                    } catch (e: Exception) {
                        uri.toString()
                    }
                } else {
                    try {
                        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
                        val profileImageRef = storageRef.child("profiles/${currentUser.uid}.jpg")
                        val inputStream = context.contentResolver.openInputStream(uri) ?: throw java.io.IOException("Unable to open input stream")
                        val bytes = inputStream.readBytes()
                        inputStream.close()
                        profileImageRef.putBytes(bytes).await()
                        profileImageRef.downloadUrl.await().toString()
                    } catch (e: Exception) {
                        try {
                            val inputStream = context.contentResolver.openInputStream(uri)
                            val directory = java.io.File(context.filesDir, "profiles")
                            if (!directory.exists()) {
                                directory.mkdirs()
                            }
                            val localFile = java.io.File(directory, "${currentUser.uid}.jpg")
                            val outputStream = java.io.FileOutputStream(localFile)
                            inputStream?.use { input ->
                                outputStream.use { output ->
                                    input.copyTo(output)
                                }
                            }
                            android.net.Uri.fromFile(localFile).toString()
                        } catch (ex: Exception) {
                            uri.toString()
                        }
                    }
                }

                if (isMock) {
                    context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
                        .edit()
                        .putString("imageUrl", finalUrl)
                        .apply()
                } else {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    db.collection("users")
                        .document(currentUser.uid)
                        .update("imageUrl", finalUrl)
                        .await()
                }
                isUploadingImage = false
                onRefreshProfile()
            }
        }
    }

    LaunchedEffect(currentUser.uid, targetUser?.uid) {
        if (targetUser != null) {
            photoState = photoAccessManager.checkPhotoAccessState(currentUser.uid, targetUser.uid)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (currentUser.gender == com.mithaq.app.model.Gender.MALE) "الملف الشخصي وإعدادات الحشمة" else "Profile & Modesty Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Own Profile Photo Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (currentUser.gender == com.mithaq.app.model.Gender.MALE) "الصورة الشخصية (أخ)" else "Profile Photo (Sister)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.size(96.dp)) {
                    UserProfileImage(
                        imageUrl = currentUser.imageUrl,
                        gender = currentUser.gender,
                        isBlurred = false, // Never blurred for oneself
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                var showImageEdit by remember { mutableStateOf(false) }
                if (!showImageEdit) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(onClick = { showImageEdit = true }) {
                            Text(if (currentUser.gender == com.mithaq.app.model.Gender.MALE) "تعديل الصورة الشخصية" else "Edit Profile Photo")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                galleryLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            if (isUploadingImage) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text(if (currentUser.gender == com.mithaq.app.model.Gender.MALE) "رفع صورة من الاستوديو" else "Upload from Gallery")
                            }
                        }
                    }
                } else {
                    var newImageUrl by remember { mutableStateOf(currentUser.imageUrl) }
                    
                    Text(
                        text = if (currentUser.gender == com.mithaq.app.model.Gender.MALE) "اختر رمزاً محتشماً:" else "Choose Modest Avatar:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val avatars = if (currentUser.gender == com.mithaq.app.model.Gender.MALE) BrotherhoodAvatars else SisterhoodAvatars
                        avatars.forEach { (avatarId, color) ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (newImageUrl == avatarId) 2.dp else 0.dp,
                                        color = if (newImageUrl == avatarId) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { newImageUrl = avatarId }
                            ) {
                                if (newImageUrl == avatarId) {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.align(Alignment.Center).size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = if (newImageUrl.startsWith("avatar_")) "" else newImageUrl,
                        onValueChange = { input ->
                            newImageUrl = input.ifBlank {
                                if (currentUser.gender == com.mithaq.app.model.Gender.MALE) "avatar_brother_green" else "avatar_sister_teal"
                            }
                        },
                        label = { Text(if (currentUser.gender == com.mithaq.app.model.Gender.MALE) "أو رابط صورة مخصصة" else "Or Custom Image URL") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(onClick = { showImageEdit = false }) {
                            Text(if (currentUser.gender == com.mithaq.app.model.Gender.MALE) "إلغاء" else "Cancel")
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    db.collection("users")
                                        .document(currentUser.uid)
                                        .update("imageUrl", newImageUrl)
                                        .addOnSuccessListener {
                                            showImageEdit = false
                                            onRefreshProfile()
                                        }
                                }
                            }
                        ) {
                            Text(if (currentUser.gender == com.mithaq.app.model.Gender.MALE) "حفظ" else "Save")
                        }
                    }
                }
            }
        }

        // ------------------ IDENTITY VERIFICATION CENTER ------------------
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isArabic) "مركز توثيق الهوية (الشارة الزرقاء)" else "Identity Verification Center (Blue Badge)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (currentUser.verificationStatus == "VERIFIED") {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "Verified Profile",
                            tint = Color.White,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2196F3))
                                .padding(2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                when (currentUser.verificationStatus) {
                    "VERIFIED" -> {
                        Text(
                            text = if (isArabic)
                                "حسابك موثق بالشارة الزرقاء. هويتك مؤكدة الآن!"
                            else
                                "Your account is verified. Your identity is confirmed!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4CAF50),
                            textAlign = TextAlign.Center
                        )
                    }
                    "PENDING" -> {
                        Text(
                            text = if (isArabic)
                                "طلب التحقق قيد المراجعة حالياً. سنقوم بتحديث حالتك قريباً."
                            else
                                "Your verification request is pending review. We will update your status shortly.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFF9800),
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {
                        Text(
                            text = if (isArabic)
                                "للحصول على الشارة الزرقاء وزيادة ثقة الأعضاء بك، يرجى رفع صورة الهوية وصورة شخصية واضحة."
                            else
                                "To get the Blue Badge and increase member trust, please upload your ID card and a clear selfie.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        var idCardUri by remember { mutableStateOf<android.net.Uri?>(null) }
                        var selfieUri by remember { mutableStateOf<android.net.Uri?>(null) }
                        var isSubmitting by remember { mutableStateOf(false) }
                        var statusMsg by remember { mutableStateOf<String?>(null) }

                        val idCardLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                            contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
                        ) { uri ->
                            if (uri != null) idCardUri = uri
                        }

                        val selfieLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                            contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
                        ) { uri ->
                            if (uri != null) selfieUri = uri
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    idCardLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (idCardUri != null) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text(
                                    text = if (idCardUri != null)
                                        (if (isArabic) "تم اختيار الهوية" else "ID Selected")
                                    else
                                        (if (isArabic) "اختر صورة الهوية" else "Select ID Card")
                                )
                            }

                            Button(
                                onClick = {
                                    selfieLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selfieUri != null) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text(
                                    text = if (selfieUri != null)
                                        (if (isArabic) "تم اختيار الصورة" else "Selfie Selected")
                                    else
                                        (if (isArabic) "التقط/اختر صورة" else "Select Selfie")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (statusMsg != null) {
                            Text(
                                text = statusMsg!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (statusMsg!!.contains("نجاح") || statusMsg!!.contains("success") || statusMsg!!.contains("قيد")) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Button(
                            onClick = {
                                if (idCardUri == null || selfieUri == null) {
                                    statusMsg = if (isArabic) "يرجى اختيار كل من صورة الهوية والصورة الشخصية." else "Please select both ID Card and Selfie."
                                    return@Button
                                }
                                isSubmitting = true
                                statusMsg = if (isArabic) "جاري معالجة التحقق ومطابقة الوجه..." else "Processing verification & face matching..."
                                authViewModel.submitVerification(idCardUri!!, selfieUri!!, context) { success, message ->
                                    isSubmitting = false
                                    statusMsg = message
                                    if (success) {
                                        onRefreshProfile()
                                    }
                                }
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text(if (isArabic) "تقديم طلب التحقق" else "Submit Verification")
                            }
                        }
                    }
                }

                // Instant Mock Admin Approval for developers/testers
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        authViewModel.mockAdminApproveVerification(context)
                        onRefreshProfile()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (isArabic) "تفعيل التوثيق الفوري (تجريبي)" else "Instant Mock Admin Approve (Demo)")
                }
            }
        }
        // ------------------------------------------------------------------

        Spacer(modifier = Modifier.height(16.dp))

        if (targetUser != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(
                    text = if (isArabic) "عرض صورة الشريك: ${targetUser.name}" else "Viewing Match's Photo: ${targetUser.name}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                VerificationBadge(status = targetUser.verificationStatus)
            }
            
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

        // ------------------ DEVELOPER OPTIONS & SETTINGS CARD ------------------
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Gear Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = if (isArabic) "خيارات المطور والإعدادات" else "Developer Settings & Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                val isMockDatabase = try {
                    val firestoreDb = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    firestoreDb.app?.options?.apiKey == "mock-api-key-for-testing" || firestoreDb.app?.options?.apiKey?.contains("mock") == true
                } catch (e: Exception) {
                    true
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isMockDatabase) Color(0xFFFF9800) else Color(0xFF4CAF50))
                    )
                    Text(
                        text = if (isMockDatabase) {
                            if (isArabic) "وضع الاتصال: وضع التجربة (قاعدة بيانات وهمية)" else "Connection: Demo Mode (Mock Database)"
                        } else {
                            if (isArabic) "وضع الاتصال: متصل بالسحابة (سيرفر حقيقي)" else "Connection: Connected to Cloud (Real Server)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isMockDatabase) Color(0xFFFF9800) else Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(16.dp))

                // Simulated Offline Switch Row
                val devPrefs = context.getSharedPreferences("mithaq_dev_options", android.content.Context.MODE_PRIVATE)
                var isOfflineSimulatedLocal by remember {
                    mutableStateOf(devPrefs.getBoolean("is_offline_simulated", false))
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "محاكاة وضع عدم الاتصال" else "Simulate Offline Mode",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isArabic) "تشغيل وضع عدم الاتصال بالشبكة لاختبار التخزين المحلي" else "Simulate network loss to test offline Room cache",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isOfflineSimulatedLocal,
                        onCheckedChange = { checked ->
                            isOfflineSimulatedLocal = checked
                            devPrefs.edit().putBoolean("is_offline_simulated", checked).apply()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(16.dp))

                // Mock Role Switcher Section
                Text(
                    text = if (isArabic) "تبديل دور المستخدم (للاختبار)" else "Switch Mock User Role (Testing)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Regular Member Chip
                    FilterChip(
                        selected = !currentUser.isWaliAccount && !currentUser.isAdmin,
                        onClick = {
                            authViewModel.updateMockRole(isWali = false, isAdmin = false, context = context)
                        },
                        label = { Text(if (isArabic) "يوزر عادي (عضو)" else "Regular Member") }
                    )
                    // Wali Chip
                    FilterChip(
                        selected = currentUser.isWaliAccount,
                        onClick = {
                            authViewModel.updateMockRole(isWali = true, isAdmin = false, context = context)
                        },
                        label = { Text(if (isArabic) "مشرف (ولي أمر)" else "Wali / Guardian") }
                    )
                    // Admin Chip
                    FilterChip(
                        selected = currentUser.isAdmin,
                        onClick = {
                            authViewModel.updateMockRole(isWali = false, isAdmin = true, context = context)
                        },
                        label = { Text(if (isArabic) "إدمن (مسؤول)" else "Admin") }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(20.dp))

                // Navigation Shortcuts Section
                Text(
                    text = if (isArabic) "روابط التنقل السريع" else "Navigation Shortcuts",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Premium Store Button
                Button(
                    onClick = { onNavigateToScreen("premium_store") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isArabic) "الذهاب إلى متجر الاشتراكات المميزة" else "Go to Premium Store")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Questionnaire Button
                Button(
                    onClick = { onNavigateToScreen("questionnaire") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isArabic) "الذهاب إلى استبيان التوافق" else "Go to Compatibility Questionnaire")
                }

                if (currentUser.isAdmin) {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Admin Console Button
                    Button(
                        onClick = { onNavigateToScreen("admin") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFC62828) // Admin Red
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isArabic) "الذهاب إلى لوحة تحكم الإدارة" else "Go to Admin Console")
                    }
                }
            }
        }
    }
}

@Composable
fun VerificationBadge(status: String?, modifier: Modifier = Modifier) {
    if (status == "VERIFIED") {
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.Done,
            contentDescription = "Verified Profile",
            tint = Color.White,
            modifier = modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Color(0xFF2196F3))
                .padding(2.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaliDashboardScreen(
    currentUserId: String,
    currentUserProfile: UserProfile?,
    authViewModel: AuthViewModel,
    isArabic: Boolean,
    onLanguageChange: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    val strings = com.mithaq.app.ui.theme.LocalMithaqStrings.current
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()

    var wardProfile by remember { mutableStateOf<UserProfile?>(null) }
    var activeRooms by remember { mutableStateOf<List<ChatRoom>>(emptyList()) }
    var roomPartners by remember { mutableStateOf<Map<String, UserProfile>>(emptyMap()) }
    var isLoadingWard by remember { mutableStateOf(false) }
    var selectedMonitoringChat by remember { mutableStateOf<UserProfile?>(null) }

    val wardUid = currentUserProfile?.wardUid ?: "mock_user_123"

    val isMock = try {
        com.google.firebase.firestore.FirebaseFirestore.getInstance().app?.options?.apiKey == "mock-api-key-for-testing" ||
        com.google.firebase.firestore.FirebaseFirestore.getInstance().app?.options?.apiKey?.contains("mock") == true
    } catch (e: Exception) {
        true
    }

    suspend fun approvePhotoAccess(wardUid: String, requestingUserId: String, context: android.content.Context, isMock: Boolean) {
        if (isMock) {
            val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
            val approvedStr = prefs.getString("photoAccessApprovedUsers_$wardUid", "[]") ?: "[]"
            val requestsStr = prefs.getString("photoAccessRequests_$wardUid", "[]") ?: "[]"
            val approvedArray = org.json.JSONArray(approvedStr)
            val requestsArray = org.json.JSONArray(requestsStr)
            
            val newRequestsArray = org.json.JSONArray()
            for (i in 0 until requestsArray.length()) {
                val id = requestsArray.getString(i)
                if (id != requestingUserId) {
                    newRequestsArray.put(id)
                }
            }
            
            var alreadyApproved = false
            for (i in 0 until approvedArray.length()) {
                if (approvedArray.getString(i) == requestingUserId) {
                    alreadyApproved = true
                }
            }
            if (!alreadyApproved) {
                approvedArray.put(requestingUserId)
            }
            
            prefs.edit()
                .putString("photoAccessApprovedUsers_$wardUid", approvedArray.toString())
                .putString("photoAccessRequests_$wardUid", newRequestsArray.toString())
                .apply()
        } else {
            com.mithaq.app.ui.photo.PhotoAccessManager().approvePhotoAccess(wardUid, requestingUserId)
        }
    }

    suspend fun revokePhotoAccess(wardUid: String, approvedUserId: String, context: android.content.Context, isMock: Boolean) {
        if (isMock) {
            val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
            val approvedStr = prefs.getString("photoAccessApprovedUsers_$wardUid", "[]") ?: "[]"
            val approvedArray = org.json.JSONArray(approvedStr)
            
            val newApprovedArray = org.json.JSONArray()
            for (i in 0 until approvedArray.length()) {
                val id = approvedArray.getString(i)
                if (id != approvedUserId) {
                    newApprovedArray.put(id)
                }
            }
            
            prefs.edit()
                .putString("photoAccessApprovedUsers_$wardUid", newApprovedArray.toString())
                .apply()
        } else {
            com.mithaq.app.ui.photo.PhotoAccessManager().revokePhotoAccess(wardUid, approvedUserId)
        }
    }

    fun loadWardData() {
        isLoadingWard = true
        coroutineScope.launch {
            if (isMock) {
                val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
                val approvedStr = prefs.getString("photoAccessApprovedUsers_$wardUid", "[]") ?: "[]"
                val requestsStr = prefs.getString("photoAccessRequests_$wardUid", "[]") ?: "[]"
                
                val approvedList = mutableListOf<String>()
                val requestsList = mutableListOf<String>()
                try {
                    val approvedArray = org.json.JSONArray(approvedStr)
                    for (i in 0 until approvedArray.length()) {
                        approvedList.add(approvedArray.getString(i))
                    }
                    val requestsArray = org.json.JSONArray(requestsStr)
                    for (i in 0 until requestsArray.length()) {
                        requestsList.add(requestsArray.getString(i))
                    }
                } catch(e: Exception) {}

                if (approvedList.isEmpty() && requestsList.isEmpty()) {
                    requestsList.add("mock_user_2")
                    prefs.edit()
                        .putString("photoAccessRequests_$wardUid", org.json.JSONArray(requestsList).toString())
                        .apply()
                }

                wardProfile = UserProfile(
                    uid = wardUid,
                    name = "عائشة أحمد / Aisha Ahmed",
                    gender = Gender.FEMALE,
                    age = 24,
                    city = "القاهرة / Cairo",
                    country = "مصر / Egypt",
                    imageUrl = "",
                    photoAccessApprovedUsers = approvedList,
                    photoAccessRequests = requestsList
                )
            } else {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                try {
                    val doc = db.collection("users").document(wardUid).get().await()
                    if (doc.exists()) {
                        val name = doc.getString("name") ?: ""
                        val genderStr = doc.getString("gender") ?: "FEMALE"
                        val gender = if (genderStr == "MALE") Gender.MALE else Gender.FEMALE
                        val age = doc.getLong("age")?.toInt() ?: 25
                        val city = doc.getString("city") ?: ""
                        val country = doc.getString("country") ?: ""
                        val imageUrl = doc.getString("imageUrl") ?: ""
                        val photoApproved = doc.get("photoAccessApprovedUsers") as? List<String> ?: emptyList()
                        val photoRequests = doc.get("photoAccessRequests") as? List<String> ?: emptyList()
                        val verificationStatus = doc.getString("verificationStatus") ?: "NONE"
                        wardProfile = UserProfile(
                            uid = wardUid,
                            name = name,
                            gender = gender,
                            age = age,
                            city = city,
                            country = country,
                            imageUrl = imageUrl,
                            photoAccessApprovedUsers = photoApproved,
                            photoAccessRequests = photoRequests,
                            verificationStatus = verificationStatus
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (isMock) {
                val prefs = context.getSharedPreferences("mithaq_mock_chat", android.content.Context.MODE_PRIVATE)
                val roomsJsonStr = prefs.getString("mithaq_mock_rooms", null)
                val roomsList = mutableListOf<ChatRoom>()
                if (roomsJsonStr != null) {
                    try {
                        val array = org.json.JSONArray(roomsJsonStr)
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val roomId = obj.getString("roomId")
                            val memberIdsArr = obj.getJSONArray("memberIds")
                            val memberIds = mutableListOf<String>()
                            for (j in 0 until memberIdsArr.length()) {
                                memberIds.add(memberIdsArr.getString(j))
                            }
                            val isChaperoned = obj.optBoolean("isChaperoned", false)
                            val waliEmail = obj.optString("waliEmail", null)
                            val lastMessage = obj.optString("lastMessage", null)
                            val lastMessageTimestamp = obj.optLong("lastMessageTimestamp", 0L)
                            roomsList.add(ChatRoom(roomId, memberIds, isChaperoned, waliEmail, lastMessage, lastMessageTimestamp))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val filteredRooms = roomsList.filter { it.memberIds.contains(wardUid) }
                activeRooms = filteredRooms.sortedByDescending { it.lastMessageTimestamp }
                
                val partnersMap = mutableMapOf<String, UserProfile>()
                for (room in filteredRooms) {
                    val partnerId = room.memberIds.firstOrNull { it != wardUid } ?: ""
                    partnersMap[room.roomId] = getMockUserProfile(partnerId)
                }
                roomPartners = partnersMap
                isLoadingWard = false
            } else {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("chatRooms")
                    .whereArrayContains("memberIds", wardUid)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val roomsList = snapshot.documents.mapNotNull { doc ->
                            try {
                                val roomId = doc.id
                                val memberIds = doc.get("memberIds") as? List<String> ?: emptyList()
                                val isChaperoned = doc.getBoolean("isChaperoned") ?: false
                                val waliEmail = doc.getString("waliEmail")
                                val lastMessage = doc.getString("lastMessage")
                                val lastMessageTimestamp = doc.getLong("lastMessageTimestamp") ?: 0L
                                ChatRoom(roomId, memberIds, isChaperoned, waliEmail, lastMessage, lastMessageTimestamp)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        activeRooms = roomsList.sortedByDescending { it.lastMessageTimestamp }
                        val partnersMap = mutableMapOf<String, UserProfile>()
                        val partnerFetchCount = roomsList.size
                        if (partnerFetchCount == 0) {
                            isLoadingWard = false
                        } else {
                            var fetched = 0
                            for (room in roomsList) {
                                val partnerId = room.memberIds.firstOrNull { it != wardUid }
                                if (partnerId != null) {
                                    db.collection("users").document(partnerId).get()
                                        .addOnSuccessListener { userDoc ->
                                            if (userDoc.exists()) {
                                                val name = userDoc.getString("name") ?: ""
                                                val genderStr = userDoc.getString("gender") ?: "MALE"
                                                val gender = if (genderStr == "MALE") Gender.MALE else Gender.FEMALE
                                                val age = userDoc.getLong("age")?.toInt() ?: 25
                                                val city = userDoc.getString("city") ?: ""
                                                val country = userDoc.getString("country") ?: ""
                                                val imageUrl = userDoc.getString("imageUrl") ?: ""
                                                val verificationStatus = userDoc.getString("verificationStatus") ?: "NONE"
                                                partnersMap[room.roomId] = UserProfile(
                                                    uid = partnerId,
                                                    name = name,
                                                    gender = gender,
                                                    age = age,
                                                    city = city,
                                                    country = country,
                                                    imageUrl = imageUrl,
                                                    verificationStatus = verificationStatus
                                                )
                                            }
                                            fetched++
                                            if (fetched == partnerFetchCount) {
                                                roomPartners = partnersMap
                                                isLoadingWard = false
                                            }
                                        }
                                        .addOnFailureListener {
                                            fetched++
                                            if (fetched == partnerFetchCount) {
                                                roomPartners = partnersMap
                                                isLoadingWard = false
                                            }
                                        }
                                } else {
                                    fetched++
                                    if (fetched == partnerFetchCount) {
                                        roomPartners = partnersMap
                                        isLoadingWard = false
                                    }
                                }
                            }
                        }
                    }
                    .addOnFailureListener {
                        isLoadingWard = false
                    }
            }
        }
    }

    LaunchedEffect(currentUserProfile?.uid) {
        loadWardData()
    }

    if (selectedMonitoringChat != null) {
        val partner = selectedMonitoringChat!!
        val roomId = remember(wardUid, partner.uid) {
            val first = minOf(wardUid, partner.uid)
            val second = maxOf(wardUid, partner.uid)
            "${first}_${second}"
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isArabic) "مراقبة: ${partner.name}" else "Monitoring: ${partner.name}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            VerificationBadge(status = partner.verificationStatus)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { selectedMonitoringChat = null }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                ReadOnlyChaperonedChat(
                    roomId = roomId,
                    context = context,
                    isArabic = isArabic
                )
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isArabic) "لوحة مراقبة ولي الأمر" else "Guardian Dashboard") },
                actions = {
                    TextButton(onClick = { onLanguageChange(!isArabic) }) {
                        Text(if (isArabic) "English" else "العربية", color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(onClick = onLogout) {
                        Text(if (isArabic) "خروج" else "Logout", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isArabic) "العضو الخاضع للإشراف (الابن/الابنة):" else "Monitored Member (Ward):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = wardProfile?.name ?: (if (isArabic) "جاري التحميل..." else "Loading Ward..."),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (wardProfile != null) {
                        Text(
                            text = "${wardProfile!!.age} ${if (isArabic) "عاماً" else "years"} • ${wardProfile!!.city}, ${wardProfile!!.country}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (isLoadingWard) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Text(
                    text = if (isArabic) "طلبات وأذونات الصور الشخصية" else "Photo Access & Approvals",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isArabic) "طلبات رؤية صورة ابنتك المعلقة:" else "Pending Requests to View Ward's Photo:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        val pendingRequests = wardProfile?.photoAccessRequests ?: emptyList()
                        if (pendingRequests.isEmpty()) {
                            Text(
                                text = if (isArabic) "لا توجد طلبات معلقة حالياً." else "No pending requests.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            pendingRequests.forEach { requesterId ->
                                val requesterProfile = getMockUserProfile(requesterId)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = requesterProfile.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${requesterProfile.age} yrs • ${requesterProfile.city}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                approvePhotoAccess(wardUid, requesterId, context, isMock)
                                                loadWardData()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                    ) {
                                        Text(if (isArabic) "الموافقة" else "Approve")
                                    }
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Text(
                            text = if (isArabic) "الأشخاص المصرح لهم بالرؤية:" else "Users Approved to View Ward's Photo:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        val approvedUsers = wardProfile?.photoAccessApprovedUsers ?: emptyList()
                        if (approvedUsers.isEmpty()) {
                            Text(
                                text = if (isArabic) "لم يتم منح إذن لأي شخص بعد." else "No approved users yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            approvedUsers.forEach { approvedId ->
                                val approvedProfile = getMockUserProfile(approvedId)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = approvedProfile.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${approvedProfile.age} yrs • ${approvedProfile.city}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                revokePhotoAccess(wardUid, approvedId, context, isMock)
                                                loadWardData()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text(if (isArabic) "إلغاء الإذن" else "Revoke")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isArabic) "مراقبة المحادثات النشطة" else "Monitor Active Conversations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (activeRooms.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isArabic) "لا توجد محادثات نشطة لابنتك حالياً." else "No active conversations for your ward.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        activeRooms.forEach { room ->
                            val partner = roomPartners[room.roomId] ?: getMockUserProfile(room.memberIds.firstOrNull { it != wardUid } ?: "")
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedMonitoringChat = partner },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(48.dp)) {
                                        UserProfileImage(
                                            imageUrl = partner.imageUrl,
                                            gender = partner.gender,
                                            isBlurred = false,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = partner.name, fontWeight = FontWeight.Bold)
                                            VerificationBadge(status = partner.verificationStatus)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = room.lastMessage ?: "",
                                            maxLines = 1,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (room.lastMessageTimestamp > 0L) {
                                        Text(
                                            text = formatDateTime(room.lastMessageTimestamp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
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
fun ReadOnlyChaperonedChat(
    roomId: String,
    context: android.content.Context,
    isArabic: Boolean
) {
    val chatViewModel = remember(roomId) { ChaperonedChatViewModel(roomId = roomId, context = context) }
    val messages by chatViewModel.messages.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Read Only",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = if (isArabic)
                        "وضع مراقبة ولي الأمر (قراءة فقط - المحادثة مشفرة ومسجلة)"
                    else
                        "Guardian Monitor Mode (Read-Only - Chat is secure & chaperoned)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            messages.forEach { msg ->
                val isMe = msg.senderId == "mock_user_123" || msg.senderId == "mock_user"
                ChatBubble(
                    messageText = msg.content,
                    isCurrentUser = isMe
                )
            }
        }
    }
}
