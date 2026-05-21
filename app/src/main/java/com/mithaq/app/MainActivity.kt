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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.material.icons.filled.ArrowBack

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
                                    Text(text = profile.name, fontWeight = FontWeight.Bold)
                                    Text(text = "${profile.age} yrs • ${profile.sect.displayName}", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Modesty: ${profile.modestyPreference.displayName}", style = MaterialTheme.typography.bodySmall)
                                }
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
                                    Text(text = partner.name, fontWeight = FontWeight.Bold)
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

    val roomId = remember(currentUser.uid, targetUser.uid) {
        val first = minOf(currentUser.uid, targetUser.uid)
        val second = maxOf(currentUser.uid, targetUser.uid)
        "${first}_${second}"
    }

    val chatViewModel = remember(roomId) { ChaperonedChatViewModel(roomId = roomId, context = context) }
    val chatRoomState by chatViewModel.chatRoom.collectAsState()
    val messages by chatViewModel.messages.collectAsState()

    var messageText by remember { mutableStateOf("") }

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
                Text(text = targetUser.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (targetUser.photoAccessApprovedUsers.contains(currentUser.uid)) 
                        (if (strings.appName == "ميثاق") "الصورة واضحة" else "Photo Visible")
                    else 
                        (if (strings.appName == "ميثاق") "الصورة مغطاة للحشمة" else "Photo Blurred"), 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

        Spacer(modifier = Modifier.height(16.dp))

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
