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
import com.mithaq.app.ui.chat.ChaperonedVoiceCallScreen
import com.mithaq.app.ui.chat.CallState
import com.mithaq.app.ui.chat.IceBreakerGenerator
import com.mithaq.app.ui.chat.SevenQuestionsChallengeScreen
import com.mithaq.app.security.SecureScreen
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
import androidx.compose.material.icons.filled.HelpCenter
import com.mithaq.app.ui.stats.MyStatsScreen
import com.mithaq.app.ui.splash.SplashScreen
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.style.TextDirection



fun formatDateTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

@Composable
fun ChatTabContent(
    currentUser: UserProfile,
    targetUser: UserProfile?,
    onSelectTargetUser: (UserProfile?) -> Unit,
    guardianViewModel: GuardianViewModel,
    onNavigateToUpgrade: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val strings = com.mithaq.app.ui.theme.LocalMithaqStrings.current
    val coroutineScope = rememberCoroutineScope()

    if (!BetaFeatureGates.LEGACY_CHATROOMS) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = if (strings.appName == "ميثاق") "المحادثات القديمة غير متاحة في البيتا" else "Legacy chat is paused for beta",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (strings.appName == "ميثاق") "استخدم تبويب الرسائل للمحادثات الآمنة بعد الموافقة." else "Use the Messages tab for approved secure chats.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    val photoAccessManager = remember { com.mithaq.app.ui.photo.PhotoAccessManager(context) }
    var partnerPhotoState by remember { mutableStateOf(PhotoAccessState.NONE) }
    var userPhotoStateForPartner by remember { mutableStateOf(PhotoAccessState.NONE) }

    var icebreakers by remember { mutableStateOf<List<String>>(emptyList()) }
    var loadingIcebreakers by remember { mutableStateOf(false) }

    LaunchedEffect(targetUser?.uid) {
        icebreakers = emptyList()
        loadingIcebreakers = false
    }

    LaunchedEffect(currentUser.uid, targetUser?.uid) {
        if (targetUser != null) {
            partnerPhotoState = photoAccessManager.checkPhotoAccessState(currentUser.uid, targetUser.uid)
            userPhotoStateForPartner = photoAccessManager.checkPhotoAccessState(targetUser.uid, currentUser.uid)
        }
    }
    
    val isMock = com.mithaq.app.Config.isMock()

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
                    val partnerProfile = getMockUserProfile(partnerId)
                    val oppositeGender = if (currentUser.gender == Gender.MALE) Gender.FEMALE else Gender.MALE
                    partnersMap[room.roomId] = if (partnerProfile.gender != oppositeGender) {
                        partnerProfile.copy(
                            gender = oppositeGender,
                            name = if (oppositeGender == Gender.MALE) "Ahmad / أحمد" else "Fatima / فاطمة",
                            imageUrl = if (oppositeGender == Gender.MALE) "avatar_brother_green" else "avatar_sister_purple"
                        )
                    } else {
                        partnerProfile
                    }
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
                                                val timezone = com.mithaq.app.util.CountryUtils.getTimezoneForProfile(
                                                    country,
                                                    userDoc.getString("timezone")
                                                )
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

                                                if (gender != currentUser.gender) {
                                                    partnersMap[room.roomId] = UserProfile(
                                                        uid = partnerId,
                                                        name = name,
                                                        gender = gender,
                                                        age = age,
                                                        city = city,
                                                        country = country,
                                                        timezone = timezone,
                                                        imageUrl = imageUrl,
                                                        sect = sect,
                                                        prayerFrequency = prayer,
                                                        modestyPreference = modesty,
                                                        relocationWillingness = relocation,
                                                        photoAccessApprovedUsers = approved,
                                                        photoAccessRequests = requests
                                                    )
                                                }
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

        var activeSubTab by remember { mutableStateOf(0) } // 0: Received, 1: Sent, 2: Favorites
        var favoritesIds by remember { mutableStateOf<List<String>>(emptyList()) }
        val likesRepository = remember { com.mithaq.app.data.LikesRepository(context) }

        LaunchedEffect(currentUser.uid) {
            try {
                favoritesIds = likesRepository.getFavorites(currentUser.uid)
            } catch(e: Exception) {}
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Row with Edit action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (strings.appName == "ميثاق") "الرسائل" else "Messages",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (strings.appName == "ميثاق") "تعديل" else "Edit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        android.widget.Toast.makeText(
                            context,
                            if (strings.appName == "ميثاق") "خيار التعديل قريباً" else "Edit option coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

            // Premium reminder banner
            if (BetaFeatureGates.PREMIUM_BILLING && !currentUser.isPremium) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Message promo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (strings.appName == "ميثاق") {
                                "لا تفوت فرصة التواصل! استمتع برسائل غير محدودة."
                            } else {
                                "Never Miss a Connection! Enjoy unlimited messaging."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Sub-tabs: Received, Sent, Favorites
            val subTabs = if (strings.appName == "ميثاق") {
                listOf("الواردة", "المرسلة", "المفضلة")
            } else {
                listOf("Received", "Sent", "Favorites")
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                subTabs.forEachIndexed { index, title ->
                    val isSelected = activeSubTab == index
                    val tabBg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    val tabTextColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(tabBg)
                            .clickable { activeSubTab = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = tabTextColor,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoadingRooms) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val displayedRooms = activeRooms.filter { room ->
                    val partnerId = room.memberIds.firstOrNull { it != currentUser.uid } ?: ""
                    when (activeSubTab) {
                        0 -> { // All received — show all conversations for now
                            // TODO: When storing initiatorUid on the chatRoom document, filter by that.
                            true
                        }
                        1 -> { // Sent — rooms where current user's uid comes first alphabetically
                            currentUser.uid < partnerId
                        }
                        else -> { // Favorites
                            favoritesIds.contains(partnerId)
                        }
                    }
                }

                if (displayedRooms.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (strings.appName == "ميثاق") 
                                "لا توجد محادثات في هذا القسم حالياً." 
                            else 
                                "No conversations in this section yet.",
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
                        displayedRooms.forEach { room ->
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
                                        val isPartnerPhotoApproved = partner.photoAccessApprovedUsers.contains(currentUser.uid)
                                        UserProfileImage(
                                            imageUrl = partner.imageUrl,
                                            gender = partner.gender,
                                            isBlurred = !isPartnerPhotoApproved,
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

                        // Unlock Your Messages Paywall Block inside lists for Free users
                        if (BetaFeatureGates.PREMIUM_BILLING && !currentUser.isPremium) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LockOpen,
                                        contentDescription = "Unlock Messages",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (strings.appName == "ميثاق") "افتح قفل رسائلك" else "Unlock Your Messages",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (strings.appName == "ميثاق") {
                                            "يمكن للأعضاء المتميزين إرسال واستقبال رسائل غير محدودة لجميع الأعضاء في ميثاق."
                                        } else {
                                            "Premium members can send and receive unlimited messages to all members on Mithaq."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = onNavigateToUpgrade,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = if (strings.appName == "ميثاق") "اشترك الآن" else "Upgrade Now",
                                            color = Color.White
                                        )
                                    }
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
    var showWaliDialog by remember { mutableStateOf(false) }
    var showChallenge by remember { mutableStateOf(false) }

    if (showChallenge) {
        SevenQuestionsChallengeScreen(
            partner = targetUser,
            isArabic = strings.appName == "ميثاق",
            onBack = { showChallenge = false }
        )
        return
    }

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

    if (BetaFeatureGates.VOICE_CALL && callState == CallState.ACTIVE) {
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
                        val isPartnerPhotoApproved = targetUser.photoAccessApprovedUsers.contains(currentUser.uid) || partnerPhotoState == PhotoAccessState.APPROVED
                        UserProfileImage(
                            imageUrl = targetUser.imageUrl,
                            gender = targetUser.gender,
                            isBlurred = !isPartnerPhotoApproved,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = targetUser.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            VerificationBadge(status = targetUser.verificationStatus)
                        }
                        
                        var currentTime by remember { mutableStateOf(com.mithaq.app.util.CountryUtils.formatLocalTimeForCountry(targetUser.country, targetUser.timezone, strings.appName == "ميثاق")) }
                        LaunchedEffect(Unit) {
                            while(true) {
                                kotlinx.coroutines.delay(60000)
                                currentTime = com.mithaq.app.util.CountryUtils.formatLocalTimeForCountry(targetUser.country, targetUser.timezone, strings.appName == "ميثاق")
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                            Text(
                                text = currentTime,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        val isPartnerPhotoApproved = targetUser.photoAccessApprovedUsers.contains(currentUser.uid) || partnerPhotoState == PhotoAccessState.APPROVED
                        Text(
                            text = if (isPartnerPhotoApproved) 
                                (if (strings.appName == "ميثاق") "الصورة واضحة" else "Photo Visible")
                            else 
                                (if (strings.appName == "ميثاق") "الصورة مغطاة للحشمة" else "Photo Blurred"), 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // In-Chat Wali invite shortcut button if room not chaperoned
                    val isChaperoned = chatRoomState?.isChaperoned ?: true
                    val waliEmail = chatRoomState?.waliEmail ?: (currentUser.guardianEmail ?: "guardian@mithaq.com")
                    
                    if (!isChaperoned || waliEmail == "guardian@mithaq.com") {
                        TextButton(
                            onClick = { showWaliDialog = true },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (strings.appName == "ميثاق") "أضف ولياً" else "Add Wali",
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (BetaFeatureGates.VOICE_CALL) {
                        IconButton(onClick = { chatViewModel.requestCall() }) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Voice Call",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    IconButton(onClick = { showChallenge = true }) {
                        Icon(
                            imageVector = Icons.Default.HelpCenter,
                            contentDescription = "7 Questions Challenge",
                            tint = MaterialTheme.colorScheme.secondary
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
                if (BetaFeatureGates.VOICE_CALL && callState == CallState.REQUESTED) {
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

                val listState = rememberLazyListState()
                LaunchedEffect(messages.size) {
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg ->
                        val isMe = msg.senderId == currentUser.uid
                        ChatBubble(
                            messageText = msg.content,
                            isCurrentUser = isMe
                        )
                    }

                    // Interactive Photo Access Bubble inside chat
                    val isPartnerPhotoApproved = targetUser.photoAccessApprovedUsers.contains(currentUser.uid) || partnerPhotoState == PhotoAccessState.APPROVED
                    if (!isPartnerPhotoApproved) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (strings.appName == "ميثاق") "صورة الشريك مغطاة للحشمة." else "Partner's photo is blurred for modesty.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (partnerPhotoState == PhotoAccessState.PENDING) {
                                        Text(
                                            text = if (strings.appName == "ميثاق") "طلب رؤية الصورة معلق بانتظار موافقة الشريك..." else "Photo access request is pending partner approval...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Center
                                        )
                                    } else {
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    val success = photoAccessManager.requestPhotoAccess(currentUser.uid, targetUser.uid)
                                                    if (success) {
                                                        partnerPhotoState = PhotoAccessState.PENDING
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(if (strings.appName == "ميثاق") "طلب رؤية الصورة" else "Request Photo Access")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Partner requesting user photo access bubble
                    val isUserPhotoApprovedForPartner = currentUser.photoAccessApprovedUsers.contains(targetUser.uid) || userPhotoStateForPartner == PhotoAccessState.APPROVED
                    val hasRequestedUserPhoto = currentUser.photoAccessRequests.contains(targetUser.uid) || userPhotoStateForPartner == PhotoAccessState.PENDING
                    
                    if (!isUserPhotoApprovedForPartner && hasRequestedUserPhoto) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (strings.appName == "ميثاق") "طلب الشريك رؤية صورتك الشخصية." else "Partner requested to view your profile photo.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    val success = photoAccessManager.approvePhotoAccess(currentUser.uid, targetUser.uid)
                                                    if (success) {
                                                        userPhotoStateForPartner = PhotoAccessState.APPROVED
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            if (strings.appName == "ميثاق") "تم السماح برؤية صورتك!" else "Photo access approved!",
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(if (strings.appName == "ميثاق") "موافقة" else "Approve")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    val success = photoAccessManager.revokePhotoAccess(currentUser.uid, targetUser.uid)
                                                    if (success) {
                                                        userPhotoStateForPartner = PhotoAccessState.NONE
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(if (strings.appName == "ميثاق") "رفض" else "Reject")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Icebreakers suggestions row
                val staticIcebreakers = remember(strings.appName) { IceBreakerGenerator.getRecommended(strings.appName == "ميثاق") }
                
                if (icebreakers.isEmpty() && !loadingIcebreakers) {
                    // Show static icebreakers as default or fallback
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        staticIcebreakers.forEach { msg ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .widthIn(max = 280.dp)
                                    .clickable {
                                        messageText = msg
                                    }
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp),
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (BetaFeatureGates.GEMINI_AI && targetUser != null && com.mithaq.app.Config.GEMINI_API_KEY.isNotEmpty() && com.mithaq.app.Config.GEMINI_API_KEY != "YOUR_GEMINI_API_KEY") {
                    if (icebreakers.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            icebreakers.forEach { msg ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .widthIn(max = 280.dp)
                                        .clickable {
                                            messageText = msg
                                        }
                                ) {
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(10.dp),
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    } else if (loadingIcebreakers) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (strings.appName == "ميثاق") "جاري توليد رسائل تعارف..." else "Generating icebreakers...",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    } else {
                        TextButton(
                            onClick = {
                                loadingIcebreakers = true
                                coroutineScope.launch {
                                    try {
                                        val service = com.mithaq.app.service.GeminiService(com.mithaq.app.Config.GEMINI_API_KEY)
                                        icebreakers = service.suggestOpeningMessages(currentUser, targetUser)
                                    } catch(e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        loadingIcebreakers = false
                                    }
                                }
                            },
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Text(
                                text = if (strings.appName == "ميثاق") "✨ اقتراح رسائل تعارف بالذكاء الاصطناعي" else "✨ Suggest Icebreakers with AI",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
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
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.ContentOrLtr)
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

    if (BetaFeatureGates.VOICE_CALL && callState == CallState.ENDED) {
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

    if (showWaliDialog) {
        InviteGuardianDialog(
            viewModel = guardianViewModel,
            onDismissRequest = { showWaliDialog = false },
            titleText = strings.inviteWali,
            subtitleText = if (strings.appName == "ميثاق") "في الزواج الإسلامي، إشراك ولي الأمر يضمن رحلة مباركة، شفافة، وآمنة." else "In Islamic matchmaking, involving a guardian ensures a blessed, transparent, and safe journey.",
            nameLabel = strings.nameLabel,
            emailLabel = strings.emailLabel,
            submitButtonText = if (strings.appName == "ميثاق") "إرسال الدعوة" else "Send Invitation",
            cancelButtonText = if (strings.appName == "ميثاق") "إلغاء" else "Cancel",
            successTitle = if (strings.appName == "ميثاق") "تم إرسال الدعوة" else "Invitation Sent",
            successSubtitle = if (strings.appName == "ميثاق") "تم إرسال دعوة إلى ولي أمرك. سنقوم بإشعارك بمجرد قبوله." else "An invitation has been sent to your Guardian. We will notify you once they accept.",
            closeButtonText = if (strings.appName == "ميثاق") "إغلاق" else "Close",
            isArabic = (strings.appName == "ميثاق")
        )
    }
}
