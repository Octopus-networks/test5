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
import com.mithaq.app.ui.onboarding.OnboardingWizardScreen
import com.mithaq.app.ui.chat.ChaperonedVoiceCallScreen
import com.mithaq.app.ui.chat.CallState
import com.mithaq.app.ui.chat.ChatMessage
import com.mithaq.app.security.SecureScreen
import com.mithaq.app.security.SafetyUtils
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

    val isMock = com.mithaq.app.Config.isMock()
    val wardUid = currentUserProfile?.wardUid ?: if (isMock) "mock_user_123" else ""

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
        if (wardUid.isBlank()) {
            isLoadingWard = false
            wardProfile = null
            return
        }
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = wardProfile?.name ?: (if (isArabic) "جاري التحميل..." else "Loading Ward..."),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        VerificationBadge(status = wardProfile?.verificationStatus)
                    }
                    if (wardProfile != null) {
                        val localTime = com.mithaq.app.util.CountryUtils.formatLocalTimeForCountry(
                            wardProfile!!.country,
                            wardProfile!!.timezone,
                            isArabic
                        )
                        Text(
                            text = "${wardProfile!!.age} ${if (isArabic) "عاماً" else "years"} • ${wardProfile!!.city}, ${wardProfile!!.country} • $localTime",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (wardProfile != null && wardProfile!!.verificationStatus == "PENDING") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isArabic) "طلب توثيق الهوية والوجه معلق:" else "Pending identity verification request:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isArabic) 
                                "قام العضو الخاضع لإشرافك برفع صورة الهوية وصورة سيلفي لتوثيق حسابه. يرجى مراجعة وتأكيد الهوية."
                            else 
                                "Your ward has uploaded their ID and selfie for identity verification. Please review and verify their identity.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    authViewModel.adminUpdateVerification(wardUid, "VERIFIED")
                                    loadWardData()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isArabic) "قبول التوثيق" else "Approve Verification")
                            }
                            Button(
                                onClick = {
                                    authViewModel.adminUpdateVerification(wardUid, "NONE")
                                    loadWardData()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isArabic) "رفض" else "Reject")
                            }
                        }
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
                // Safety Alerts Section
                Text(
                    text = if (isArabic) "تنبيهات الأمان (تبادل معلومات اتصال)" else "Safety Alerts (Contact Info Detected)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                
                var alertsFound by remember { mutableStateOf(false) }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        activeRooms.forEach { room ->
                            val partner = roomPartners[room.roomId] ?: getMockUserProfile(room.memberIds.firstOrNull { it != wardUid } ?: "")
                            // In a real app, we'd fetch all messages. For demo, we check the last message.
                            if (room.lastMessage != null && SafetyUtils.containsContactInfo(room.lastMessage)) {
                                alertsFound = true
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isArabic) "تنبيه في محادثة ${partner.name}" else "Alert in chat with ${partner.name}",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = room.lastMessage,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { selectedMonitoringChat = partner }) {
                                        Icon(Icons.Default.Visibility, contentDescription = "View", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        
                        if (!alertsFound) {
                            Text(
                                text = if (isArabic) "لا توجد تنبيهات أمان حالياً." else "No safety alerts at this time.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                                            text = "${requesterProfile.age} ${if (isArabic) "عاماً" else "yrs"} • ${requesterProfile.city}",
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

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

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
                                            text = "${approvedProfile.age} ${if (isArabic) "عاماً" else "yrs"} • ${approvedProfile.city}",
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
