package com.mithaq.app.ui.stats

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mithaq.app.data.LikesRepository
import com.mithaq.app.model.UserProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyStatsScreen(
    currentUser: UserProfile,
    likesRepository: LikesRepository,
    isArabic: Boolean,
    onPrayerStatsUpdated: (UserProfile) -> Unit = {},
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var viewsCount by remember { mutableStateOf(0) }
    var likesReceived by remember { mutableStateOf(0) }
    var likesSent by remember { mutableStateOf(0) }
    var mutualMatches by remember { mutableStateOf(0) }
    var favoritesCount by remember { mutableStateOf(0) }
    var profilesViewed by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentUser.uid) {
        coroutineScope.launch {
            try {
                viewsCount = likesRepository.getProfileVisitors(currentUser.uid).size
                likesReceived = likesRepository.getWhoLikedMe(currentUser.uid).size
                likesSent = likesRepository.getLikesList(currentUser.uid).size
                mutualMatches = likesRepository.getMutualMatches(currentUser.uid).size
                favoritesCount = likesRepository.getFavorites(currentUser.uid).size
                profilesViewed = likesRepository.getProfilesIViewed(currentUser.uid).size
            } catch (_: Exception) {
            } finally {
                isLoading = false
            }
        }
    }

    // Profile completeness
    val hasPhoto = !currentUser.imageUrl.contains("avatar_") && currentUser.imageUrl.isNotEmpty()
    val isVerified = currentUser.verificationStatus == "VERIFIED" || currentUser.verificationStatus == "PENDING"
    val hasQuiz = currentUser.questionnaireAnswers.isNotEmpty()
    val hasWali = !currentUser.guardianEmail.isNullOrEmpty()
    val completeness = (if (hasPhoto) 25 else 0) +
            (if (isVerified) 25 else 0) +
            (if (hasQuiz) 25 else 0) +
            (if (hasWali) 25 else 0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isArabic) "إحصائياتي" else "My Stats") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Completeness Ring
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        AnimatedRing(
                            progress = completeness.toFloat() / 100f,
                            label = "$completeness%",
                            size = 80.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                        Column {
                            Text(
                                text = if (isArabic) "اكتمال الملف الشخصي" else "Profile Completeness",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isArabic)
                                    "الملف المكتمل يحصل على توافقات أفضل بنسبة 5x"
                                else
                                    "Complete profiles get 5x better matches",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = completeness.toFloat() / 100f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }

                // Main stats grid
                Text(
                    text = if (isArabic) "إحصائيات نشاطك" else "Your Activity Stats",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        icon = Icons.Default.Visibility,
                        label = if (isArabic) "مشاهدات ملفك" else "Profile Views",
                        count = viewsCount,
                        color = Color(0xFF7C4DFF),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Default.RemoveRedEye,
                        label = if (isArabic) "ملفات شاهدتها" else "Profiles I Viewed",
                        count = profilesViewed,
                        color = Color(0xFF00BCD4),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        icon = Icons.Default.FavoriteBorder,
                        label = if (isArabic) "إعجابات وصلتك" else "Likes Received",
                        count = likesReceived,
                        color = Color(0xFFE53935),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Default.Favorite,
                        label = if (isArabic) "أعجبت بهم" else "Likes Sent",
                        count = likesSent,
                        color = Color(0xFFFF7043),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        icon = Icons.Default.CheckCircle,
                        label = if (isArabic) "تطابقات متبادلة" else "Mutual Matches",
                        count = mutualMatches,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Default.Star,
                        label = if (isArabic) "في مفضلتي" else "My Favorites",
                        count = favoritesCount,
                        color = Color(0xFFFFC107),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                // --- PRAYER STATS SECTION ---
                Text(
                    text = if (isArabic) "إحصائيات الصلاة والتزامك" else "Your Prayer Commitment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // The interactive Daily Prayer Tracker is placed here
                com.mithaq.app.ui.home.DailyPrayerTracker(
                    currentUser = currentUser,
                    isArabic = isArabic,
                    onPrayerToggled = { prayerName, isChecked ->
                        onPrayerStatsUpdated(
                            com.mithaq.app.util.PrayerRewardManager.onUserPrayerToggled(
                                profile = currentUser,
                                prayerName = prayerName,
                                isChecked = isChecked
                            )
                        )
                    }
                )

                val todayCount = listOf(currentUser.fajrPrayedToday, currentUser.dhuhrPrayedToday, currentUser.asrPrayedToday, currentUser.maghribPrayedToday, currentUser.ishaPrayedToday).count { it }
                val weekCount = currentUser.fajrWeeklyCount + currentUser.dhuhrWeeklyCount + currentUser.asrWeeklyCount + currentUser.maghribWeeklyCount + currentUser.ishaWeeklyCount
                val monthCount = currentUser.fajrMonthlyCount + currentUser.dhuhrMonthlyCount + currentUser.asrMonthlyCount + currentUser.maghribMonthlyCount + currentUser.ishaMonthlyCount

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        icon = Icons.Default.Mosque,
                        label = if (isArabic) "صلوات اليوم" else "Today's Prayers",
                        count = todayCount,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Default.DateRange,
                        label = if (isArabic) "صلوات الأسبوع" else "Weekly Prayers",
                        count = weekCount,
                        color = Color(0xFF03A9F4),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Default.CalendarToday,
                        label = if (isArabic) "صلوات الشهر" else "Monthly Prayers",
                        count = monthCount,
                        color = Color(0xFF9C27B0),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                // Subscription badge
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentUser.isPremium)
                            Color(0xFFFFC107).copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (currentUser.isPremium) Icons.Default.Star else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (currentUser.isPremium) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = if (currentUser.isPremium)
                                    if (isArabic) "عضو مميز - ${currentUser.subscriptionPlan}" else "Premium Member - ${currentUser.subscriptionPlan}"
                                else
                                    if (isArabic) "عضو مجاني" else "Free Member",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (currentUser.isPremium)
                                    if (isArabic) "تمتع بجميع ميزات ميثاق بدون حدود" else "Enjoy all Mithaq features without limits"
                                else
                                    if (isArabic) "ترقَّ للحصول على توافقات أفضل" else "Upgrade for better matches & features",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Verification status
                val verificationColor = when (currentUser.verificationStatus) {
                    "VERIFIED" -> Color(0xFF4CAF50)
                    "PENDING" -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = verificationColor.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = when (currentUser.verificationStatus) {
                                "VERIFIED" -> Icons.Default.CheckCircle
                                "PENDING" -> Icons.Default.Info
                                else -> Icons.Default.Warning
                            },
                            contentDescription = null,
                            tint = verificationColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = when (currentUser.verificationStatus) {
                                    "VERIFIED" -> if (isArabic) "هويتك موثقة ✓" else "Identity Verified ✓"
                                    "PENDING" -> if (isArabic) "توثيق الهوية قيد المراجعة" else "Verification Pending"
                                    else -> if (isArabic) "الهوية غير موثقة" else "Identity Not Verified"
                                },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = verificationColor
                            )
                            Text(
                                text = when (currentUser.verificationStatus) {
                                    "VERIFIED" -> if (isArabic) "يثق بك الأعضاء الآخرون بشكل أكبر" else "Other members trust you more"
                                    "PENDING" -> if (isArabic) "سيتم المراجعة قريباً" else "Review in progress"
                                    else -> if (isArabic) "وثّق هويتك لزيادة الثقة" else "Verify your identity to build trust"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    // Animate count from 0 to actual value
    val animatedCount by animateIntAsState(
        targetValue = count,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "stat_count"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = "$animatedCount",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AnimatedRing(
    progress: Float,
    label: String,
    size: Dp,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "ring_progress"
    )
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = label,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            color = color
        )
    }
}
