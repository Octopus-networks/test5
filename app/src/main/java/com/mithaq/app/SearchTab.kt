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
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.material.icons.filled.TrendingUp
import com.mithaq.app.ui.stats.MyStatsScreen
import com.mithaq.app.ui.splash.SplashScreen



@Composable
fun SearchTabContent(
    currentUser: UserProfile,
    viewModel: SearchViewModel,
    isArabic: Boolean,
    onSelectMatch: (UserProfile) -> Unit,
    onNavigateToUpgrade: () -> Unit,
    onNavigateToScreen: (String) -> Unit = {},
    initialSubTab: Int = 0
) {
    val coroutineScope = rememberCoroutineScope()
    var activeSubTab by remember(initialSubTab) { mutableStateOf(initialSubTab) }
    var showFilters by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(false) }
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    var breakdownPartner by remember { mutableStateOf<UserProfile?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showAllMembersMode by remember { mutableStateOf(true) }
    val isDark = isSystemInDarkTheme()
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    val glassBgColor = if (isDark) GlassSurfaceDark else GlassSurfaceLight
    val glassBorderColor = if (isDark) GlassBorderDark else GlassBorderLight

    var selectedCategory by remember { mutableStateOf("Matches") }
    val categories = remember(isArabic) {
        listOf(
            "Matches" to (if (isArabic) "التوافق" else "Matches"),
            "Online" to (if (isArabic) "متصل الآن" else "Online"),
            "Popular" to (if (isArabic) "الأكثر شعبية" else "Popular"),
            "Newest" to (if (isArabic) "الأعضاء الجدد" else "Newest"),
            "Latest Photos" to (if (isArabic) "أحدث الصور" else "Latest Photos"),
            "In My Area" to (if (isArabic) "بالقرب مني" else "In My Area"),
            "Mutual Matches" to (if (isArabic) "توافق ممتاز" else "High Compatibility")
        )
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
            val tabs = if (isArabic) listOf("لوحة التحكم", "استكشاف الشركاء") else listOf("Dashboard", "Explore Partners")
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
                        color = tabTextColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activeSubTab == 0) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val guardianStatus = currentUser.guardianStatus ?: "None"
                val normalizedGuardianStatus = guardianStatus.uppercase()
                val waliBannerColor = when (normalizedGuardianStatus) {
                    "VERIFIED" -> SuccessGreen.copy(alpha = 0.15f)
                    "PENDING" -> Color(0xFFFF9800).copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                }
                val waliBannerBorder = when (normalizedGuardianStatus) {
                    "VERIFIED" -> SuccessGreen.copy(alpha = 0.4f)
                    "PENDING" -> Color(0xFFFF9800).copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                }
                val waliText = when (normalizedGuardianStatus) {
                    "VERIFIED" -> if (isArabic) "تحت الإشراف الشرعي الكامل لولي أمرك" else "Under full Islamic supervision of your Guardian"
                    "PENDING" -> if (isArabic) "دعوة ولي الأمر قيد الانتظار" else "Guardian invitation pending"
                    else -> if (isArabic) "اضغط لإشراك ولي أمرك لضمان الحشمة والجدية" else "Invite a guardian to oversee chats & ensure modesty"
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = waliBannerColor),
                    border = BorderStroke(1.dp, waliBannerBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (normalizedGuardianStatus == "VERIFIED") Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (normalizedGuardianStatus == "VERIFIED") SuccessGreen else if (normalizedGuardianStatus == "PENDING") Color(0xFFFF9800) else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = waliText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                val hasPhoto = !currentUser.imageUrl.contains("avatar_") && currentUser.imageUrl.isNotEmpty()
                val isVerified = currentUser.verificationStatus == "VERIFIED" || currentUser.verificationStatus == "PENDING"
                val hasQuiz = currentUser.questionnaireAnswers.isNotEmpty()
                val hasWali = !currentUser.guardianEmail.isNullOrEmpty()
                
                val completeness = (if (hasPhoto) 25 else 0) + 
                                     (if (isVerified) 25 else 0) + 
                                     (if (hasQuiz) 25 else 0) + 
                                     (if (hasWali) 25 else 0)
                
                val seriousnessScore = currentUser.seriousnessScore

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(glassBgColor)
                        .border(1.dp, glassBorderColor, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) "اكتمل ملفك الشخصي" else "Profile Completeness",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$completeness%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        LinearProgressIndicator(
                            progress = completeness.toFloat() / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                Text(
                                    text = if (isArabic) "مؤشر الجدية" else "Seriousness Score",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "$seriousnessScore%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (seriousnessScore > 70) SuccessGreen else if (seriousnessScore > 40) AccentAmber else ErrorRed
                            )
                        }

                        if (completeness < 100) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isArabic) "أكمل المهام التالية للحصول على أفضل التوافقات:" else "Complete these tasks for better matchmaking:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                CompletenessItem(done = hasPhoto, label = if (isArabic) "رفع صورة شخصية" else "Upload profile photo")
                                CompletenessItem(done = isVerified, label = if (isArabic) "توثيق الهوية والوجه" else "Submit identity verification")
                                CompletenessItem(done = hasQuiz, label = if (isArabic) "إكمال استبيان التوافق" else "Complete compatibility questionnaire")
                                CompletenessItem(done = hasWali, label = if (isArabic) "دعوة ولي الأمر (مشرف)" else "Invite your Wali (Guardian)")
                            }
                        }
                    }
                }
                
                // Questionnaire Banner (only if not done)
                if (!hasQuiz) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToScreen("questionnaire") },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isArabic) "⚡ أكمل استبيان التوافق!" else "⚡ Complete Your Compatibility Quiz!",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (isArabic) "رفع نسبة التوافق مع الشركاء بنسبة 3x أعلى" else "Get 3x better matches by completing the quiz",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                if (BetaFeatureGates.GEMINI_AI) {
                    // AI Khattaba Matchmaker Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToScreen("ai_matchmaker") },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            Color(0xFFE28B15).copy(alpha = 0.12f)
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) "🤖 استشر الخاطبة الإلكترونية الذكية" else "🤖 Consult the AI Khattaba Matchmaker",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (isArabic) "تحدث مع الخاطبة الذكية ودعها ترشح لك شركاء متوافقين معك فورا!" else "Chat with our AI bot to get personalized spouse recommendations!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val context3 = androidx.compose.ui.platform.LocalContext.current
                com.mithaq.app.ui.home.DailyPrayerTracker(
                    currentUser = currentUser,
                    isArabic = isArabic,
                    onPrayerToggled = { prayerName, isChecked ->
                        // Simulate sending a notification to matched users
                        if (isChecked) {
                            coroutineScope.launch {
                                android.widget.Toast.makeText(
                                    context3,
                                    if (isArabic) "تم إشعار أطراف التطابق بصلاة $prayerName!" else "Matches notified about $prayerName prayer!",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Mini Stats Row
                val context2 = androidx.compose.ui.platform.LocalContext.current
                val likesRepo2 = remember { com.mithaq.app.data.LikesRepository(context2) }
                var statsViews by remember { mutableStateOf(0) }
                var statsLikes by remember { mutableStateOf(0) }
                var statsMatches by remember { mutableStateOf(0) }
                LaunchedEffect(currentUser.uid) {
                    try {
                        statsViews = likesRepo2.getProfileVisitors(currentUser.uid).size
                        statsLikes = likesRepo2.getWhoLikedMe(currentUser.uid).size
                        statsMatches = likesRepo2.getMutualMatches(currentUser.uid).size
                    } catch (_: Exception) {}
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf(
                        Triple(Icons.Default.Visibility, if (isArabic) "مشاهدات" else "Views", statsViews),
                        Triple(Icons.Default.Favorite, if (isArabic) "إعجابات" else "Likes", statsLikes),
                        Triple(Icons.Default.CheckCircle, if (isArabic) "تطابقات" else "Matches", statsMatches)
                    ).forEach { (icon, label, count) ->
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Text(text = "$count", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Button(
                    onClick = { onNavigateToScreen("stats") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(if (isArabic) "عرض إحصائيات ملفي الشخصي" else "View My Detailed Stats", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                val topMatch = remember(searchResults) {
                    if (searchResults.isNotEmpty()) {
                        searchResults.maxByOrNull { MatchScoreCalculator.calculateScore(currentUser, it) }
                    } else null
                }

                if (topMatch != null) {
                    val score = MatchScoreCalculator.calculateScore(currentUser, topMatch)
                    Text(
                        text = if (isArabic) "ترشيح اليوم الأكثر توافقاً" else "Today's Top Compatibility Match",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(top = 8.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        onClick = { onSelectMatch(topMatch) }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                val isAccessApproved = topMatch.photoAccessApprovedUsers.contains(currentUser.uid)
                                Box(modifier = Modifier.size(64.dp)) {
                                    UserProfileImage(
                                        imageUrl = topMatch.imageUrl,
                                        gender = topMatch.gender,
                                        isBlurred = !isAccessApproved,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(text = topMatch.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        VerificationBadge(status = topMatch.verificationStatus)
                                        val isOnline = (System.currentTimeMillis() - topMatch.lastSeen) < 300000
                                        val isActiveRecently = (System.currentTimeMillis() - topMatch.lastSeen) < 86400000
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
                                    val sectLabel = topMatch.sect.getDisplayName(isArabic)
                                    val ageSuffix = if (isArabic) "سنة" else "yrs"
                                    val topMatchLocalTime = com.mithaq.app.util.CountryUtils.formatLocalTimeForCountry(
                                        topMatch.country,
                                        topMatch.timezone,
                                        isArabic
                                    )
                                    Text(text = "${topMatch.age} $ageSuffix • $sectLabel", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = if (isArabic) "${topMatch.city}، ${topMatch.country} • $topMatchLocalTime" else "${topMatch.city}, ${topMatch.country} • $topMatchLocalTime",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(60.dp)
                            ) {
                                val arcColor = MaterialTheme.colorScheme.primary
                                val trackColor = MaterialTheme.colorScheme.outlineVariant
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawArc(
                                        color = trackColor,
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = arcColor,
                                        startAngle = -90f,
                                        sweepAngle = (score.toFloat() / 100f) * 360f,
                                        useCenter = false,
                                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                                Text(
                                    text = "$score%",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { activeSubTab = 1 },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "استكشف كل التوافقات والبحث" else "Explore All Matches & Filter",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            // Question Card: Do you want to show all members?
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isArabic) "خيارات عرض الأعضاء:" else "Members Display Options:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val opt1Selected = showAllMembersMode
                        val opt1Bg = if (opt1Selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        val opt1TextColor = if (opt1Selected) Color.White else MaterialTheme.colorScheme.onSurface
                        val opt1Border = if (opt1Selected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(opt1Bg)
                                .border(1.dp, opt1Border, RoundedCornerShape(12.dp))
                                .clickable { showAllMembersMode = true }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "إظهار كل المسجلين" else "Show All Registered",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = opt1TextColor,
                                textAlign = TextAlign.Center
                            )
                        }

                        val opt2Selected = !showAllMembersMode
                        val opt2Bg = if (opt2Selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        val opt2TextColor = if (opt2Selected) Color.White else MaterialTheme.colorScheme.onSurface
                        val opt2Border = if (opt2Selected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(opt2Bg)
                                .border(1.dp, opt2Border, RoundedCornerShape(12.dp))
                                .clickable { showAllMembersMode = false }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "تصفية (المتوافقين فقط)" else "Filter (Compatible Only)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = opt2TextColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Search bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isArabic) "ابحث بالاسم أو المدينة..." else "Search by name or city...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Search
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Button(
                    onClick = {
                        keyboardController?.hide()
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(if (isArabic) "بحث" else "Search")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showFilters = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isArabic) "تصفية البحث" else "Filters")
                }

                IconButton(
                    onClick = { isGridView = !isGridView },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.List else Icons.Default.GridView,
                        contentDescription = "Toggle Grid/List",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat.first
                    val chipBgColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    val chipTextColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    val chipBorder = if (isSelected) BorderStroke(0.dp, Color.Transparent) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { selectedCategory = cat.first },
                        color = chipBgColor,
                        border = chipBorder,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = cat.second,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = chipTextColor
                        )
                    }
                }
            }

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
                val processedResults = remember(searchResults, selectedCategory, currentUser, searchQuery, showAllMembersMode) {
                    var list = searchResults
                    
                    // Enforce that males only see females, and females only see males
                    list = list.filter { it.gender != currentUser.gender }
                    
                    // Filter by search query first
                    if (searchQuery.isNotBlank()) {
                        list = list.filter {
                            it.name.contains(searchQuery, ignoreCase = true) ||
                            it.city.contains(searchQuery, ignoreCase = true) ||
                            it.country.contains(searchQuery, ignoreCase = true)
                        }
                    }
                    
                    // If not showAllMembersMode (i.e. Filter mode), filter out incompatible members from list completely
                    if (!showAllMembersMode) {
                        list = list.filter { viewModel.isCompatible(it) }
                    }
                    when (selectedCategory) {
                        "Matches" -> {
                            list = list.sortedByDescending { MatchScoreCalculator.calculateScore(currentUser, it) }
                        }
                        "Online" ->
                            // TODO: Replace with real Firestore presence (lastSeen field) when implemented.
                            // For now, show verified members as a reasonable proxy for activity.
                            list = list.filter { it.verificationStatus == "VERIFIED" || it.isPremium }
                        "Popular" -> {
                            list = list.filter { it.isPremium || it.verificationStatus == "VERIFIED" }
                        }
                        "Newest" -> {
                            list = list.reversed()
                        }
                        "Latest Photos" -> {
                            list = list.filter { it.imageUrl.isNotEmpty() && !it.imageUrl.contains("avatar_") }
                        }
                        "In My Area" -> {
                            list = list.filter { it.country.equals(currentUser.country, ignoreCase = true) }
                        }
                        "Mutual Matches" -> {
                            list = list.filter { MatchScoreCalculator.calculateScore(currentUser, it) >= 75 }
                        }
                    }
                    list
                }

                if (processedResults.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(text = if (isArabic) "لا توجد نتائج لهذه الفئة." else "No results found for this category.", modifier = Modifier.padding(16.dp))
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (isGridView) {
                            val chunkedResults = remember(processedResults) {
                                processedResults.chunked(2)
                            }
                            chunkedResults.forEach { rowProfiles ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowProfiles.forEach { profile ->
                                        val score = MatchScoreCalculator.calculateScore(currentUser, profile)
                                        val isCompatible = if (showAllMembersMode) true else viewModel.isCompatible(profile)
                                        
                                        val isAccessApproved = profile.photoAccessApprovedUsers.contains(currentUser.uid)
                                        val isBlurred = if (isCompatible) !isAccessApproved else true

                                        val context = androidx.compose.ui.platform.LocalContext.current
                                        val likesRepository = remember { com.mithaq.app.data.LikesRepository(context) }
                                        var isFav by remember { mutableStateOf(false) }
                                        var isLiked by remember { mutableStateOf(false) }
                                        
                                        LaunchedEffect(currentUser.uid, profile.uid) {
                                            isFav = likesRepository.getFavorites(currentUser.uid).contains(profile.uid)
                                            isLiked = likesRepository.getLikesList(currentUser.uid).contains(profile.uid)
                                        }

                                        GridMatchCard(
                                            currentUser = currentUser,
                                            profile = profile,
                                            isCompatible = isCompatible,
                                            isArabic = isArabic,
                                            onSelectMatch = onSelectMatch,
                                            onFavoriteToggle = {
                                                coroutineScope.launch {
                                                    val added = likesRepository.toggleFavorite(currentUser.uid, profile.uid)
                                                    isFav = added
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        if (added) {
                                                            if (isArabic) "تمت الإضافة للمفضلة" else "Added to Favorites"
                                                        } else {
                                                            if (isArabic) "تمت الإزالة من المفضلة" else "Removed from Favorites"
                                                        },
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            onLikeToggle = {
                                                coroutineScope.launch {
                                                    val liked = likesRepository.getLikesList(currentUser.uid).contains(profile.uid)
                                                    if (liked) { likesRepository.removeLike(currentUser.uid, profile.uid); isLiked = false } else {
                                                        val isMutual = likesRepository.addLike(currentUser.uid, profile.uid)
                                                        isLiked = true
                                                        if (isMutual) {
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                if (isArabic) "لقد تم التطابق! ابدأ المحادثة الآن." else "Mutual Match! Chat unlocked.",
                                                                android.widget.Toast.LENGTH_LONG
                                                            ).show()
                                                        } else {
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                if (isArabic) "تم تسجيل الإعجاب!" else "Liked!",
                                                                android.widget.Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                }
                                            },
                                            onShowBreakdown = {
                                                breakdownPartner = profile
                                            },
                                            isFav = isFav,
                                            isLiked = isLiked,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (rowProfiles.size < 2) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        } else {
                            processedResults.forEach { profile ->
                                val score = MatchScoreCalculator.calculateScore(currentUser, profile)
                                val isCompatible = if (showAllMembersMode) true else viewModel.isCompatible(profile)
                                
                                val isAccessApproved = profile.photoAccessApprovedUsers.contains(currentUser.uid)
                                val isBlurred = if (isCompatible) !isAccessApproved else true

                                val context = androidx.compose.ui.platform.LocalContext.current
                                val likesRepository = remember { com.mithaq.app.data.LikesRepository(context) }
                                var isFav by remember { mutableStateOf(false) }
                                var isLiked by remember { mutableStateOf(false) }
                                
                                LaunchedEffect(currentUser.uid, profile.uid) {
                                    isFav = likesRepository.getFavorites(currentUser.uid).contains(profile.uid)
                                    isLiked = likesRepository.getLikesList(currentUser.uid).contains(profile.uid)
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(280.dp)
                                        .padding(vertical = 8.dp)
                                        .let { if (!isCompatible) it.alpha(0.55f) else it },
                                    shape = RoundedCornerShape(8.dp),
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
                                            shape = RoundedCornerShape(8.dp)
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
                                                        startY = 200f
                                                    )
                                                )
                                        )
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp)
                                                .align(Alignment.TopStart),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                                                    .background(Color.Black.copy(alpha = 0.5f))
                                                    .clickable {
                                                        if (isCompatible) {
                                                            breakdownPartner = profile
                                                        }
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = "$score%",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                    Text(
                                                        text = if (isArabic) "توافق" else "Match",
                                                        color = Color.White.copy(alpha = 0.8f),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                            
                                            IconButton(
                                                onClick = {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        if (isArabic) "تم إرسال بلاغ للمشرفين لمراجعة الملف." else "Report submitted to moderators.",
                                                        android.widget.Toast.LENGTH_LONG
                                                    ).show()
                                                },
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.5f))
                                                    .size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = "Report",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                if (isCompatible) {
                                                    onSelectMatch(profile)
                                                }
                                            },
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.25f))
                                                .size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = "View Details",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp)
                                                .align(Alignment.BottomStart),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Bottom
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isCompatible) profile.name else (if (isArabic) "عضو غير متوافق" else "Incompatible Match"),
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 20.sp
                                                        )
                                                        if (isCompatible) {
                                                            VerificationBadge(status = profile.verificationStatus)
                                                        }
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    
                                                    if (isCompatible) {
                                                        val sectLabel = profile.sect.getDisplayName(isArabic)
                                                        val ageSuffix = if (isArabic) "سنة" else "yrs"
                                                        val localTime = com.mithaq.app.util.CountryUtils.formatLocalTimeForCountry(
                                                            profile.country,
                                                            profile.timezone,
                                                            isArabic
                                                        )
                                                        Text(
                                                            text = "${profile.age} $ageSuffix • $sectLabel • ${profile.gender.getDisplayName(isArabic)}",
                                                            color = Color.White.copy(alpha = 0.9f),
                                                            fontSize = 14.sp
                                                        )
                                                        Text(
                                                            text = "${profile.city}, ${profile.country} • $localTime",
                                                            color = Color.White.copy(alpha = 0.7f),
                                                            fontSize = 12.sp
                                                        )
                                                    } else {
                                                        Text(
                                                            text = if (isArabic) "البيانات مخفية لعدم التوافق" else "Details hidden due to incompatibility",
                                                            color = Color.White.copy(alpha = 0.7f),
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                }
                                                
                                                if (isCompatible) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        IconButton(
                                                            onClick = {
                                                                coroutineScope.launch {
                                                                    val added = likesRepository.toggleFavorite(currentUser.uid, profile.uid)
                                                                    isFav = added
                                                                    android.widget.Toast.makeText(
                                                                        context,
                                                                        if (added) {
                                                                            if (isArabic) "تمت الإضافة للمفضلة" else "Added to Favorites"
                                                                        } else {
                                                                            if (isArabic) "تمت الإزالة من المفضلة" else "Removed from Favorites"
                                                                        },
                                                                        android.widget.Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                            },
                                                            modifier = Modifier
                                                                .clip(CircleShape)
                                                                .background(Color.White.copy(alpha = 0.2f))
                                                                .size(40.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Star,
                                                                contentDescription = "Favorite",
                                                                tint = if (isFav) Color(0xFFFFC107) else Color.White,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                        
                                                        IconButton(
                                                            onClick = {
                                                                coroutineScope.launch {
                                                                    val liked = likesRepository.getLikesList(currentUser.uid).contains(profile.uid)
                                                                    if (liked) { likesRepository.removeLike(currentUser.uid, profile.uid); isLiked = false } else {
                                                                        val isMutual = likesRepository.addLike(currentUser.uid, profile.uid)
                                                                        isLiked = true
                                                                        if (isMutual) {
                                                                            android.widget.Toast.makeText(
                                                                                context,
                                                                                if (isArabic) "لقد تم التطابق! ابدأ المحادثة الآن." else "Mutual Match! Chat unlocked.",
                                                                                android.widget.Toast.LENGTH_LONG
                                                                            ).show()
                                                                        } else {
                                                                            android.widget.Toast.makeText(
                                                                                context,
                                                                                if (isArabic) "تم تسجيل الإعجاب!" else "Liked!",
                                                                                android.widget.Toast.LENGTH_SHORT
                                                                            ).show()
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            modifier = Modifier
                                                                .clip(CircleShape)
                                                                .background(Color.White.copy(alpha = 0.2f))
                                                                .size(40.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Favorite,
                                                                contentDescription = "Like",
                                                                tint = if (isLiked) Color.Red else Color.White,
                                                                modifier = Modifier.size(20.dp)
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
                }
            }
        }
    }

    if (showFilters) {
        SearchFilterBottomSheet(
            onDismissRequest = { showFilters = false },
            viewModel = viewModel,
            isPlatinum = currentUser.isPremium && (currentUser.subscriptionPlan.lowercase() == "platinum" || currentUser.subscriptionPlan.lowercase() == "gold"),
            onNavigateToUpgrade = onNavigateToUpgrade
        )
    }

    if (breakdownPartner != null) {
        CompatibilityBreakdownDialog(
            currentUser = currentUser,
            partner = breakdownPartner!!,
            isArabic = isArabic,
            onDismiss = { breakdownPartner = null }
        )
    }
}

@Composable
fun CompletenessItem(done: Boolean, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.Info,
            contentDescription = null,
            tint = if (done) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
