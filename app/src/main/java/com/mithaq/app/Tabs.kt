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
import com.mithaq.app.ui.stats.MyStatsScreen
import com.mithaq.app.ui.splash.SplashScreen



@Composable
fun GridMatchCard(
    currentUser: UserProfile,
    profile: UserProfile,
    isCompatible: Boolean,
    isArabic: Boolean,
    onSelectMatch: (UserProfile) -> Unit,
    onFavoriteToggle: () -> Unit,
    onLikeToggle: () -> Unit,
    onShowBreakdown: () -> Unit,
    isFav: Boolean,
    isLiked: Boolean,
    modifier: Modifier = Modifier
) {
    val score = MatchScoreCalculator.calculateScore(currentUser, profile)
    val isAccessApproved = profile.photoAccessApprovedUsers.contains(currentUser.uid)
    val isBlurred = if (isCompatible) !isAccessApproved else true

    Card(
        modifier = modifier
            .height(200.dp)
            .padding(vertical = 4.dp)
            .let { if (!isCompatible) it.alpha(0.55f) else it },
        shape = RoundedCornerShape(16.dp),
        onClick = {
            if (isCompatible) {
                onSelectMatch(profile)
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            UserProfileImage(
                imageUrl = if (isCompatible) profile.imageUrl else "",
                gender = profile.gender,
                isBlurred = isBlurred,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            ),
                            startY = 100f
                        )
                    )
            )

            // Compatibility Score Badge top-left
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable {
                        if (isCompatible) {
                            onShowBreakdown()
                        }
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$score%",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            // Like / Favorite Buttons at the top-right
            if (isCompatible) {
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = if (isFav) Color(0xFFFFC107) else Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    IconButton(
                        onClick = onLikeToggle,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Like",
                            tint = if (isLiked) Color.Red else Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Profile info at the bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isCompatible) profile.name else (if (isArabic) "غير متوافق" else "Incompatible"),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isCompatible) {
                        if (profile.verificationStatus == "VERIFIED") {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        val isOnline = (System.currentTimeMillis() - profile.lastSeen) < 300000
                        val isActiveRecently = (System.currentTimeMillis() - profile.lastSeen) < 86400000
                        if (isOnline) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                            )
                        } else if (isActiveRecently) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF9800))
                            )
                        }
                    }
                }

                if (isCompatible) {
                    val sectLabel = profile.sect.getDisplayName(isArabic)
                    val ageSuffix = if (isArabic) "سنة" else "yrs"
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = AccentGold, modifier = Modifier.size(10.dp))
                        Text(
                            text = "${profile.seriousnessScore}%",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• $sectLabel",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = "${profile.city}, ${profile.country}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = if (isArabic) "البيانات مخفية" else "Details hidden",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

fun getMockUserProfile(uid: String): UserProfile {
    return when (uid) {
        "mock_user_2" -> UserProfile(
            uid = "mock_user_2",
            name = "Fatima / فاطمة",
            gender = Gender.FEMALE,
            age = 24,
            city = "Cairo",
            country = "Egypt",
            imageUrl = "avatar_sister_purple",
            sect = Sect.SUNNI,
            prayerFrequency = PrayerFrequency.ALWAYS,
            modestyPreference = ModestyPreference.HIJAB,
            relocationWillingness = RelocationWillingness.OPEN,
            verificationStatus = "VERIFIED"
        )
        "mock_user_3" -> UserProfile(
            uid = "mock_user_3",
            name = "Ahmad / أحمد",
            gender = Gender.MALE,
            age = 29,
            city = "Riyadh",
            country = "Saudi Arabia",
            imageUrl = "avatar_brother_green",
            sect = Sect.SUNNI,
            prayerFrequency = PrayerFrequency.ALWAYS,
            modestyPreference = ModestyPreference.DOES_NOT_MATTER,
            relocationWillingness = RelocationWillingness.YES,
            verificationStatus = "VERIFIED"
        )
        "mock_user_4" -> UserProfile(
            uid = "mock_user_4",
            name = "Sarah / سارة",
            gender = Gender.FEMALE,
            age = 27,
            city = "Dubai",
            country = "UAE",
            imageUrl = "avatar_sister_purple",
            sect = Sect.SUNNI,
            prayerFrequency = PrayerFrequency.USUALLY,
            modestyPreference = ModestyPreference.HIJAB,
            relocationWillingness = RelocationWillingness.OPEN,
            verificationStatus = "PENDING"
        )
        else -> UserProfile(
            uid = uid,
            name = if (uid.contains("mock")) "Mock User" else "Partner $uid",
            gender = Gender.FEMALE,
            age = 25,
            city = "Cairo",
            country = "Egypt"
        )
    }
}

fun getCameraImageUri(context: android.content.Context): android.net.Uri {
    val directory = java.io.File(context.cacheDir, "camera")
    if (!directory.exists()) {
        directory.mkdirs()
    }
    val file = java.io.File(directory, "camera_capture_${System.currentTimeMillis()}.jpg")
    return androidx.core.content.FileProvider.getUriForFile(
        context,
        "com.mithaq.app.provider",
        file
    )
}

fun getCameraVideoUri(context: android.content.Context): android.net.Uri {
    val directory = java.io.File(context.cacheDir, "camera")
    if (!directory.exists()) {
        directory.mkdirs()
    }
    val file = java.io.File(directory, "camera_capture_${System.currentTimeMillis()}.mp4")
    return androidx.core.content.FileProvider.getUriForFile(
        context,
        "com.mithaq.app.provider",
        file
    )
}

@Composable
fun rememberUserProfileResolver(
    searchViewModel: SearchViewModel,
    isMock: Boolean,
    currentUser: UserProfile
): (String) -> UserProfile {
    val searchResults by searchViewModel.searchResults.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    // Cache for profiles fetched asynchronously from Firestore (production mode)
    val profileCache = remember { mutableStateMapOf<String, UserProfile>() }

    return { uid ->
        // 1. Check local search results first (fast, synchronous)
        val found = searchResults.find { it.uid == uid } ?: profileCache[uid]
        if (found != null) {
            found
        } else if (isMock) {
            // 2. In mock mode use generated placeholder
            val mock = getMockUserProfile(uid)
            val oppositeGender = if (currentUser.gender == Gender.MALE) Gender.FEMALE else Gender.MALE
            if (mock.gender != oppositeGender) {
                mock.copy(
                    gender = oppositeGender,
                    name = if (oppositeGender == Gender.MALE) "Ahmad / أحمد" else "Fatima / فاطمة",
                    imageUrl = if (oppositeGender == Gender.MALE) "avatar_brother_green" else "avatar_sister_purple"
                )
            } else {
                mock
            }
        } else {
            // 3. In production mode, fetch profile from Firestore asynchronously
            //    Return a temporary placeholder while the real data loads
            if (!profileCache.containsKey(uid)) {
                // Mark as "loading" with placeholder to prevent duplicate fetches
                val oppositeGender = if (currentUser.gender == Gender.MALE) Gender.FEMALE else Gender.MALE
                profileCache[uid] = UserProfile(
                    uid = uid,
                    name = "...",
                    gender = oppositeGender
                )
                coroutineScope.launch {
                    try {
                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        val doc = db.collection("users").document(uid).get().await()
                        if (doc.exists()) {
                            val genderStr = doc.getString("gender") ?: "FEMALE"
                            val gender = if (genderStr.equals("MALE", ignoreCase = true)) Gender.MALE else Gender.FEMALE
                            val sectStr = doc.getString("sect") ?: "SUNNI"
                            val sect = try { com.mithaq.app.model.Sect.valueOf(sectStr.uppercase()) } catch (e: Exception) { com.mithaq.app.model.Sect.SUNNI }
                            val prayerStr = doc.getString("prayerFrequency") ?: "ALWAYS"
                            val prayer = try { com.mithaq.app.model.PrayerFrequency.valueOf(prayerStr.uppercase()) } catch (e: Exception) { com.mithaq.app.model.PrayerFrequency.ALWAYS }
                            val modestyStr = doc.getString("modestyPreference") ?: "HIJAB"
                            val modesty = try { com.mithaq.app.model.ModestyPreference.valueOf(modestyStr.uppercase()) } catch (e: Exception) { com.mithaq.app.model.ModestyPreference.HIJAB }
                            val relocationStr = doc.getString("relocationWillingness") ?: "OPEN"
                            val relocation = try { com.mithaq.app.model.RelocationWillingness.valueOf(relocationStr.uppercase()) } catch (e: Exception) { com.mithaq.app.model.RelocationWillingness.OPEN }
                            profileCache[uid] = UserProfile(
                                uid = uid,
                                name = doc.getString("name") ?: "",
                                gender = gender,
                                age = doc.getLong("age")?.toInt() ?: 18,
                                city = doc.getString("city") ?: "",
                                country = doc.getString("country") ?: "",
                                imageUrl = doc.getString("imageUrl") ?: "",
                                sect = sect,
                                prayerFrequency = prayer,
                                modestyPreference = modesty,
                                relocationWillingness = relocation,
                                verificationStatus = doc.getString("verificationStatus") ?: "NONE",
                                isPremium = doc.getBoolean("isPremium") ?: false,
                                lastSeen = doc.getLong("lastSeen") ?: 0L,
                                photoAccessApprovedUsers = doc.get("photoAccessApprovedUsers") as? List<String> ?: emptyList()
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            profileCache[uid] ?: getMockUserProfile(uid)
        }
    }
}

@Composable
fun LikesTabContent(
    currentUser: UserProfile,
    searchViewModel: SearchViewModel,
    likesRepository: com.mithaq.app.data.LikesRepository,
    isArabic: Boolean,
    onSelectMatch: (UserProfile) -> Unit,
    onNavigateToUpgrade: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val resolveProfile = rememberUserProfileResolver(searchViewModel, isMock = true, currentUser = currentUser)
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var activeSubTab by remember { mutableStateOf(0) } // 0: Who Liked Me, 1: My Likes, 2: Mutual Matches
    
    var whoLikedMeIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var myLikesIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var mutualIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    fun refresh() {
        isLoading = true
        coroutineScope.launch {
            try {
                whoLikedMeIds = likesRepository.getWhoLikedMe(currentUser.uid)
                myLikesIds = likesRepository.getLikesList(currentUser.uid)
                mutualIds = likesRepository.getMutualMatches(currentUser.uid)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
    
    LaunchedEffect(currentUser.uid) {
        refresh()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = if (isArabic) {
                listOf("من أعجب بي", "إعجاباتي", "تطابق متبادل")
            } else {
                listOf("Liked Me", "My Likes", "Mutual Matches")
            }
            tabs.forEachIndexed { index, title ->
                val isSelected = activeSubTab == index
                val tabBg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                val tabTextColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tabBg)
                        .clickable { activeSubTab = index }
                        .padding(vertical = 10.dp),
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
        
        if (isLoading) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val displayIds = when (activeSubTab) {
                0 -> whoLikedMeIds
                1 -> myLikesIds
                else -> mutualIds
            }
            
            if (displayIds.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = when (activeSubTab) {
                            0 -> if (isArabic) "لا توجد إعجابات واردة حالياً." else "No incoming likes yet."
                            1 -> if (isArabic) "لم تعجب بأي ملف شخصي بعد." else "No liked profiles yet."
                            else -> if (isArabic) "لا توجد تطابقات متبادلة حالياً." else "No mutual matches yet."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (activeSubTab == 0 && !currentUser.isPremium) {
                // Blurred grid for Free users on "Liked Me" tab
                val chunks = displayIds.chunked(2)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Paywall Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Likes count",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (isArabic) {
                                    "أعجب ${displayIds.size} من الأعضاء بملفك! قم بالترقية للباقة البلاتينية!"
                                } else {
                                    "${displayIds.size} people liked your profile! Upgrade to Platinum!"
                                },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    chunks.forEach { rowIds ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            rowIds.forEach { uid ->
                                val partner = resolveProfile(uid)
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(180.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        UserProfileImage(
                                            imageUrl = partner.imageUrl,
                                            gender = partner.gender,
                                            isBlurred = true,
                                            blurRadius = 35.dp,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Column(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(12.dp)
                                                .fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(80.dp)
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color.White.copy(alpha = 0.6f))
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width(120.dp)
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color.White.copy(alpha = 0.4f))
                                            )
                                        }
                                    }
                                }
                            }
                            if (rowIds.size == 1) {
                                Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onNavigateToUpgrade,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                    ) {
                        Text(
                            text = if (isArabic) "أظهر من أعجب بك" else "See who liked you",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    displayIds.forEach { uid ->
                        val partner = resolveProfile(uid)
                        val score = MatchScoreCalculator.calculateScore(currentUser, partner)
                        val isCompatible = searchViewModel.isCompatible(partner)
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .let { if (!isCompatible) it.alpha(0.55f) else it },
                            shape = RoundedCornerShape(20.dp),
                            onClick = {
                                if (isCompatible) {
                                    coroutineScope.launch {
                                        likesRepository.addProfileView(currentUser.uid, partner.uid)
                                    }
                                    onSelectMatch(partner)
                                }
                            }
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
                                    val isAccessApproved = partner.photoAccessApprovedUsers.contains(currentUser.uid)
                                    val isBlurred = if (isCompatible) !isAccessApproved else true
                                    
                                    Box(modifier = Modifier.size(52.dp)) {
                                        UserProfileImage(
                                            imageUrl = if (isCompatible) partner.imageUrl else "",
                                            gender = partner.gender,
                                            isBlurred = isBlurred,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = if (isCompatible) partner.name else (if (isArabic) "عضو غير متوافق" else "Incompatible Match"),
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCompatible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (isCompatible) {
                                                VerificationBadge(status = partner.verificationStatus)
                                                val isOnline = (System.currentTimeMillis() - partner.lastSeen) < 300000
                                                val isActiveRecently = (System.currentTimeMillis() - partner.lastSeen) < 86400000
                                                if (isOnline) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF4CAF50))
                                                    )
                                                } else if (isActiveRecently) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFFFF9800))
                                                    )
                                                }
                                            }
                                        }
                                        if (isCompatible) {
                                            val sectLabel = partner.sect.getDisplayName(isArabic)
                                            val ageSuffix = if (isArabic) "سنة" else "yrs"
                                            Text(text = "${partner.age} $ageSuffix • $sectLabel", style = MaterialTheme.typography.bodySmall)
                                            Text(text = "${partner.city}, ${partner.country}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        } else {
                                            Text(
                                                text = if (isArabic) "التفاصيل مخفية لعدم التوافق" else "Details hidden due to incompatibility",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isCompatible) {
                                        when (activeSubTab) {
                                            0 -> {
                                                IconButton(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            val isMutual = likesRepository.addLike(currentUser.uid, partner.uid)
                                                            if (isMutual) {
                                                                android.widget.Toast.makeText(
                                                                    context,
                                                                    if (isArabic) "لقد تم التطابق! ابدأ المحادثة الآن." else "Mutual Match! Chat unlocked.",
                                                                    android.widget.Toast.LENGTH_LONG
                                                                ).show()
                                                            } else {
                                                                android.widget.Toast.makeText(
                                                                    context,
                                                                    if (isArabic) "تم إرسال الإعجاب!" else "Like sent!",
                                                                    android.widget.Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                            refresh()
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Favorite,
                                                        contentDescription = "Like Back",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                            1 -> {
                                                Icon(
                                                    imageVector = Icons.Default.Favorite,
                                                    contentDescription = "Liked",
                                                    tint = Color.Red,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                            else -> {
                                                IconButton(
                                                    onClick = { onSelectMatch(partner) },
                                                    modifier = Modifier
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Chat,
                                                        contentDescription = "Chat",
                                                        tint = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    MatchScoreBadge(
                                        score = score,
                                        size = 40.dp,
                                        modifier = if (!isCompatible) Modifier.alpha(0.5f) else Modifier
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

@Composable
fun ViewsTabContent(
    currentUser: UserProfile,
    searchViewModel: SearchViewModel,
    likesRepository: com.mithaq.app.data.LikesRepository,
    isArabic: Boolean,
    onSelectMatch: (UserProfile) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val resolveProfile = rememberUserProfileResolver(searchViewModel, isMock = true, currentUser = currentUser)
    
    var activeSubTab by remember { mutableStateOf(0) } // 0: Viewed My Profile, 1: Profiles I Viewed
    
    var visitorIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var viewedByMeIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    fun refresh() {
        isLoading = true
        coroutineScope.launch {
            try {
                visitorIds = likesRepository.getProfileVisitors(currentUser.uid)
                viewedByMeIds = likesRepository.getProfilesIViewed(currentUser.uid)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
    
    LaunchedEffect(currentUser.uid) {
        refresh()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = if (isArabic) {
                listOf("من زار ملفي", "ملفات زرتها")
            } else {
                listOf("Viewed My Profile", "Profiles I Viewed")
            }
            tabs.forEachIndexed { index, title ->
                val isSelected = activeSubTab == index
                val tabBg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                val tabTextColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tabBg)
                        .clickable { activeSubTab = index }
                        .padding(vertical = 10.dp),
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
        
        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val displayIds = if (activeSubTab == 0) visitorIds else viewedByMeIds
            
            if (displayIds.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (activeSubTab == 0) {
                            if (isArabic) "لا توجد زيارات لملفك الشخصي حالياً." else "No profile visitors yet."
                        } else {
                            if (isArabic) "لم تقم بزيارة أي ملف شخصي بعد." else "No profiles viewed yet."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    displayIds.forEach { uid ->
                        val partner = resolveProfile(uid)
                        val score = MatchScoreCalculator.calculateScore(currentUser, partner)
                        val isCompatible = searchViewModel.isCompatible(partner)
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .let { if (!isCompatible) it.alpha(0.55f) else it },
                            shape = RoundedCornerShape(20.dp),
                            onClick = {
                                if (isCompatible) {
                                    coroutineScope.launch {
                                        likesRepository.addProfileView(currentUser.uid, partner.uid)
                                    }
                                    onSelectMatch(partner)
                                }
                            }
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
                                    val isAccessApproved = partner.photoAccessApprovedUsers.contains(currentUser.uid)
                                    val isBlurred = if (isCompatible) !isAccessApproved else true
                                    
                                    Box(modifier = Modifier.size(52.dp)) {
                                        UserProfileImage(
                                            imageUrl = if (isCompatible) partner.imageUrl else "",
                                            gender = partner.gender,
                                            isBlurred = isBlurred,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = if (isCompatible) partner.name else (if (isArabic) "عضو غير متوافق" else "Incompatible Match"),
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCompatible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (isCompatible) {
                                                VerificationBadge(status = partner.verificationStatus)
                                                val isOnline = (System.currentTimeMillis() - partner.lastSeen) < 300000
                                                val isActiveRecently = (System.currentTimeMillis() - partner.lastSeen) < 86400000
                                                if (isOnline) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF4CAF50))
                                                    )
                                                } else if (isActiveRecently) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFFFF9800))
                                                    )
                                                }
                                            }
                                        }
                                        if (isCompatible) {
                                            val sectLabel = partner.sect.getDisplayName(isArabic)
                                            val ageSuffix = if (isArabic) "سنة" else "yrs"
                                            Text(text = "${partner.age} $ageSuffix • $sectLabel", style = MaterialTheme.typography.bodySmall)
                                            Text(text = "${partner.city}, ${partner.country}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        } else {
                                            Text(
                                                text = if (isArabic) "التفاصيل مخفية لعدم التوافق" else "Details hidden due to incompatibility",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                                
                                MatchScoreBadge(
                                    score = score,
                                    size = 40.dp,
                                    modifier = if (!isCompatible) Modifier.alpha(0.5f) else Modifier
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesTabContent(
    currentUser: UserProfile,
    searchViewModel: SearchViewModel,
    likesRepository: com.mithaq.app.data.LikesRepository,
    isArabic: Boolean,
    onSelectMatch: (UserProfile) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val resolveProfile = rememberUserProfileResolver(searchViewModel, isMock = true, currentUser = currentUser)
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var activeSubTab by remember { mutableStateOf(0) } // 0: Who Favorited Me, 1: My Favorites, 2: Mutual Favorites
    
    var whoFavoritedMeIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var myFavoritesIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var mutualFavoritesIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    fun refresh() {
        isLoading = true
        coroutineScope.launch {
            try {
                whoFavoritedMeIds = likesRepository.getWhoFavoritedMe(currentUser.uid)
                myFavoritesIds = likesRepository.getFavorites(currentUser.uid)
                mutualFavoritesIds = likesRepository.getMutualFavorites(currentUser.uid)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
    
    LaunchedEffect(currentUser.uid) {
        refresh()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = if (isArabic) {
                listOf("من أضافني", "مفضلتي", "التفضيل المتبادل")
            } else {
                listOf("Added Me", "My Favorites", "Mutual Favorites")
            }
            tabs.forEachIndexed { index, title ->
                val isSelected = activeSubTab == index
                val tabBg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                val tabTextColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tabBg)
                        .clickable { activeSubTab = index }
                        .padding(vertical = 10.dp),
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
        
        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val displayIds = when (activeSubTab) {
                0 -> whoFavoritedMeIds
                1 -> myFavoritesIds
                else -> mutualFavoritesIds
            }
            
            if (displayIds.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = when (activeSubTab) {
                            0 -> if (isArabic) "لا أحد أضافك للمفضلة بعد." else "No one favorited you yet."
                            1 -> if (isArabic) "لم تضف أحداً للمفضلة بعد." else "No favorites added yet."
                            else -> if (isArabic) "لا توجد تفضيلات متبادلة بعد." else "No mutual favorites yet."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    displayIds.forEach { uid ->
                        val partner = resolveProfile(uid)
                        val score = MatchScoreCalculator.calculateScore(currentUser, partner)
                        val isCompatible = searchViewModel.isCompatible(partner)
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .let { if (!isCompatible) it.alpha(0.55f) else it },
                            shape = RoundedCornerShape(20.dp),
                            onClick = {
                                if (isCompatible) {
                                    coroutineScope.launch {
                                        likesRepository.addProfileView(currentUser.uid, partner.uid)
                                    }
                                    onSelectMatch(partner)
                                }
                            }
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
                                    val isAccessApproved = partner.photoAccessApprovedUsers.contains(currentUser.uid)
                                    val isBlurred = if (isCompatible) !isAccessApproved else true
                                    
                                    Box(modifier = Modifier.size(52.dp)) {
                                        UserProfileImage(
                                            imageUrl = if (isCompatible) partner.imageUrl else "",
                                            gender = partner.gender,
                                            isBlurred = isBlurred,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = if (isCompatible) partner.name else (if (isArabic) "عضو غير متوافق" else "Incompatible Match"),
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCompatible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (isCompatible) {
                                                VerificationBadge(status = partner.verificationStatus)
                                                val isOnline = (System.currentTimeMillis() - partner.lastSeen) < 300000
                                                val isActiveRecently = (System.currentTimeMillis() - partner.lastSeen) < 86400000
                                                if (isOnline) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF4CAF50))
                                                    )
                                                } else if (isActiveRecently) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFFFF9800))
                                                    )
                                                }
                                            }
                                        }
                                        if (isCompatible) {
                                            val sectLabel = partner.sect.getDisplayName(isArabic)
                                            val ageSuffix = if (isArabic) "سنة" else "yrs"
                                            Text(text = "${partner.age} $ageSuffix • $sectLabel", style = MaterialTheme.typography.bodySmall)
                                            Text(text = "${partner.city}, ${partner.country}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        } else {
                                            Text(
                                                text = if (isArabic) "التفاصيل مخفية لعدم التوافق" else "Details hidden due to incompatibility",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isCompatible) {
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    val added = likesRepository.toggleFavorite(currentUser.uid, partner.uid)
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        if (added) {
                                                            if (isArabic) "تمت الإضافة للمفضلة" else "Added to Favorites"
                                                        } else {
                                                            if (isArabic) "تمت الإزالة من المفضلة" else "Removed from Favorites"
                                                        },
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                    refresh()
                                                }
                                            }
                                        ) {
                                            val isFav = when (activeSubTab) {
                                                0 -> myFavoritesIds.contains(partner.uid) // check if I also favorited them
                                                else -> true
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Favorite",
                                                tint = if (isFav) Color(0xFFFFC107) else MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                    
                                    MatchScoreBadge(
                                        score = score,
                                        size = 40.dp,
                                        modifier = if (!isCompatible) Modifier.alpha(0.5f) else Modifier
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
