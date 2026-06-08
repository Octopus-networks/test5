package com.mithaq.app.ui.match

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mithaq.app.model.*
import com.mithaq.app.ui.photo.UserProfileImage
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.mithaq.app.ui.theme.GlassBorderDark
import com.mithaq.app.ui.theme.GlassBorderLight
import com.mithaq.app.ui.theme.GlassSurfaceDark
import com.mithaq.app.ui.theme.GlassSurfaceLight
import com.mithaq.app.ui.match.IstikharaGuideDialog
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.isSystemInDarkTheme

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    partner: UserProfile,
    currentUser: UserProfile,
    isArabic: Boolean,
    likesRepository: com.mithaq.app.data.LikesRepository,
    searchViewModel: com.mithaq.app.ui.filter.SearchViewModel,
    matchDetailViewModel: com.mithaq.app.ui.match.MatchDetailViewModel,
    onBack: () -> Unit,
    onNavigateToChat: (UserProfile) -> Unit,
    onNavigateToUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val isCompatible = searchViewModel.isCompatible(partner)
    val isPremium = currentUser.isPremium // Unlocks special premium checks
    val isVerified = partner.verificationStatus == "VERIFIED"

    var isFavorite by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(false) }
    var showMoreDetails by remember { mutableStateOf(false) }
    var showIstikharaGuide by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Register profile view automatically on open
    LaunchedEffect(partner.uid) {
        likesRepository.addProfileView(currentUser.uid, partner.uid)
        // Check if already favorite and already liked
        isFavorite = likesRepository.getFavorites(currentUser.uid).contains(partner.uid)
        isLiked = likesRepository.getLikesList(currentUser.uid).contains(partner.uid)
    }

    val isDark = true
    val glassBgColor = GlassSurfaceDark
    val glassBorderColor = GlassBorderDark

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 80.dp) // Avoid overlap with bottom floating action bar
        ) {
            // 1. TOP PHOTO CARD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                // Partner image pager or blurred placeholder
                val allImages = remember(partner.imageUrl, partner.additionalImages) {
                    listOfNotNull(partner.imageUrl.takeIf { it.isNotEmpty() }) + partner.additionalImages
                }
                val pagerState = rememberPagerState(pageCount = { allImages.size.coerceAtLeast(1) })

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val imgUrl = allImages.getOrNull(page) ?: ""
                    val isAccessApproved = partner.photoAccessApprovedUsers.contains(currentUser.uid)
                    val isBlurred = if (isCompatible) !isAccessApproved else true

                    UserProfileImage(
                        imageUrl = if (isCompatible) imgUrl else "",
                        gender = partner.gender,
                        isBlurred = isBlurred,
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (allImages.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(allImages.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f))
                            )
                        }
                    }
                }

                // Dark gradient overlay at the bottom of image
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                startY = 150f
                            )
                        )
                )

                // Circular Back Button overlay (Top-Left)
                Box(
                    modifier = Modifier
                        .padding(top = 40.dp, start = 16.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { onBack() }
                        .align(Alignment.TopStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Circular Options Button overlay (Top-Right)
                Box(
                    modifier = Modifier
                        .padding(top = 40.dp, end = 16.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White
                    )
                }

                // Header Info overlay (Bottom details)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isCompatible) "${partner.name}، ${partner.age} (${partner.gender.getDisplayName(isArabic)})" else (if (isArabic) "عضو غير متوافق" else "Incompatible Match"),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (isVerified && isCompatible) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Member",
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        if (isCompatible && partner.subscriptionPlan == "Gold (Prayer Reward)") {
                            Surface(
                                color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Prayer Badge",
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isArabic) "مواظب على الصلاة" else "Prays Regularly",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = com.mithaq.app.ui.theme.AccentGold, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (isArabic) "مؤشر الجدية: ${partner.seriousnessScore}%" else "Seriousness: ${partner.seriousnessScore}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val partnerLocalTime = com.mithaq.app.util.CountryUtils.formatLocalTimeForCountry(partner.country, partner.timezone, isArabic)
                    Text(
                        text = if (isCompatible) "${partner.city}, ${partner.country} • $partnerLocalTime" else (if (isArabic) "الموقع مخفي لعدم التوافق" else "Location hidden"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Activity Status Indicator
                    // TODO: Replace with real Firestore lastSeen field for accurate presence.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFA726)) // Amber — indicates recent but unknown exact status
                        )
                        Text(
                            text = if (isArabic) "نشط مؤخراً" else "Recently active",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Local Time Indicator
                    if (isCompatible) {
                        var currentTime by remember { mutableStateOf(com.mithaq.app.util.CountryUtils.formatLocalTimeForCountry(partner.country, partner.timezone, isArabic)) }
                        LaunchedEffect(Unit) {
                            while(true) {
                                kotlinx.coroutines.delay(60000)
                                currentTime = com.mithaq.app.util.CountryUtils.formatLocalTimeForCountry(partner.country, partner.timezone, isArabic)
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                            Text(
                                text = if (isArabic) "الوقت عنده الآن: $currentTime" else "Local time: $currentTime",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                    // Inline quick interaction overlays on the bottom right of photo
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Istikhara Button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary)
                            .clickable {
                                showIstikharaGuide = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Istikhara",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Quick Like
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable {
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
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Like",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Quick Chat
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                            .clickable {
                                if (isCompatible) {
                                    onNavigateToChat(partner)
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        if (isArabic) "غير متوافق! عدّل فلاتر البحث." else "Incompatible match!",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Chat",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // --- Prayer Commitment Indicator ---
            val prayerCommitmentColor = when {
                (partner.fajrWeeklyCount + partner.dhuhrWeeklyCount + partner.asrWeeklyCount + partner.maghribWeeklyCount + partner.ishaWeeklyCount) >= 25 -> Color(0xFF4CAF50) // Green - High commitment
                (partner.fajrWeeklyCount + partner.dhuhrWeeklyCount + partner.asrWeeklyCount + partner.maghribWeeklyCount + partner.ishaWeeklyCount) >= 10 -> Color(0xFFFF9800) // Orange - Medium
                else -> Color(0xFF9E9E9E) // Gray - Low or unknown
            }
            
            val prayerCommitmentText = when {
                (partner.fajrWeeklyCount + partner.dhuhrWeeklyCount + partner.asrWeeklyCount + partner.maghribWeeklyCount + partner.ishaWeeklyCount) >= 25 -> if (isArabic) "التزام عالٍ بالصلاة" else "High Prayer Commitment"
                (partner.fajrWeeklyCount + partner.dhuhrWeeklyCount + partner.asrWeeklyCount + partner.maghribWeeklyCount + partner.ishaWeeklyCount) >= 10 -> if (isArabic) "التزام متوسط بالصلاة" else "Medium Prayer Commitment"
                else -> if (isArabic) "غير محدد" else "Prayer Commitment Unknown"
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = prayerCommitmentColor.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(prayerCommitmentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mosque,
                            contentDescription = "Prayer",
                            tint = prayerCommitmentColor
                        )
                    }
                    Column {
                        Text(
                            text = prayerCommitmentText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = prayerCommitmentColor
                        )
                        val partnerMonthCount = partner.fajrMonthlyCount + partner.dhuhrMonthlyCount + partner.asrMonthlyCount + partner.maghribMonthlyCount + partner.ishaMonthlyCount
                        Text(
                            text = if (isArabic) "صلى ${partnerMonthCount} صلاة هذا الشهر" else "Prayed ${partnerMonthCount} times this month",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Detailed breakdown
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val prayers = listOf(
                                (if(isArabic) "الفجر" else "Fajr") to partner.fajrMonthlyCount,
                                (if(isArabic) "الظهر" else "Dhuhr") to partner.dhuhrMonthlyCount,
                                (if(isArabic) "العصر" else "Asr") to partner.asrMonthlyCount,
                                (if(isArabic) "المغرب" else "Maghrib") to partner.maghribMonthlyCount,
                                (if(isArabic) "العشاء" else "Isha") to partner.ishaMonthlyCount
                            )
                            prayers.forEach { (name, count) ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "$name: $count",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. PREMIUM TOP PROMO (For free users)
            if (!isPremium) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = "Upgrade Today",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Column {
                                Text(
                                    text = if (isArabic) "ترقية الحساب الآن للتواصل!" else "Upgrade now to connect!",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (isArabic) "تواصل مع ${partner.name} اليوم وغير حياتك!" else "Connect with ${partner.name} today!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = onNavigateToUpgrade,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Upgrade",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // 3. SECTIONS
            if (!isCompatible) {
                // Warning section for incompatible users
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Incompatible",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isArabic) "بيانات وتفاصيل العضو مخفية لعدم التوافق" else "Details hidden due to incompatibility",
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isArabic) "يرجى تعديل فلاتر البحث الخاصة بك لتتوافق مع هذا العضو." else "Please adjust your search criteria to unlock this profile.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // A. Overview Section
                InfoSectionCard(
                    title = if (isArabic) "نظرة عامة" else "Overview",
                    badgeText = if (isArabic) "تطابق" else "Matches",
                    badgeValue = "2"
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoChip(
                            icon = Icons.Default.Cake,
                            label = if (isArabic) "العمر" else "Age",
                            value = "${partner.age}",
                            verified = isVerified
                        )
                        InfoChip(
                            icon = Icons.Default.Person,
                            label = if (isArabic) "الجنس" else "Gender",
                            value = partner.gender.getDisplayName(isArabic),
                            verified = isVerified
                        )
                        InfoChip(
                            icon = Icons.Default.Home,
                            label = if (isArabic) "يعيش في" else "Lives in",
                            value = "${partner.city}, ${partner.country} • ${com.mithaq.app.util.CountryUtils.formatLocalTimeForCountry(partner.country, partner.timezone, isArabic)}",
                            verified = isVerified
                        )
                    }
                }

                // B. Appearance Section
                InfoSectionCard(
                    title = if (isArabic) "المظهر الخارجي" else "Appearance"
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val formatHeight = "${partner.height} cm"
                        val formatWeight = "${partner.weight} kg"
                        InfoChip(icon = Icons.Default.Palette, label = if (isArabic) "لون الشعر" else "Hair color", value = partner.hairColor.replaceFirstChar { it.uppercase() })
                        InfoChip(icon = Icons.Default.Visibility, label = if (isArabic) "لون العين" else "Eye color", value = partner.eyeColor.replaceFirstChar { it.uppercase() })
                        InfoChip(icon = Icons.Default.Straighten, label = if (isArabic) "الطول" else "Height", value = formatHeight)
                        InfoChip(icon = Icons.Default.MonitorWeight, label = if (isArabic) "الوزن" else "Weight", value = formatWeight)
                        InfoChip(icon = Icons.Default.Scale, label = if (isArabic) "بنية الجسم" else "Body style", value = partner.bodyType.replaceFirstChar { it.uppercase() }.replace("_", " "))
                        InfoChip(icon = Icons.Default.PersonOutline, label = if (isArabic) "العرق" else "Ethnicity", value = partner.ethnicity.replaceFirstChar { it.uppercase() }.replace("_", " "))
                        InfoChip(icon = Icons.Default.Face, label = if (isArabic) "المظهر" else "Appearance", value = partner.appearanceRating.replaceFirstChar { it.uppercase() }.replace("_", " "))
                    }
                }

                // C. Bio/Text Cards
                TextSectionCard(
                    title = if (isArabic) "مقدمتي" else "My Bio",
                    content = partner.aboutYourself.ifEmpty { if (isArabic) "لا توجد تفاصيل" else "No details provided." }
                )

                // D. Show More/Less expandable block
                Column(modifier = Modifier.fillMaxWidth()) {
                    AnimatedVisibility(
                        visible = showMoreDetails,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Lifestyle Section
                            InfoSectionCard(
                                title = if (isArabic) "أسلوب الحياة" else "Lifestyle"
                            ) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    InfoChip(icon = Icons.Default.Flight, label = if (isArabic) "الانتقال" else "Relocate", value = partner.relocationWillingness.getDisplayName(isArabic))
                                    InfoChip(icon = Icons.Default.LocalBar, label = if (isArabic) "المشروبات" else "Drink", value = partner.drinkStatus.replaceFirstChar { it.uppercase() }.replace("_", " "))
                                    InfoChip(icon = Icons.Default.SmokingRooms, label = if (isArabic) "التدخين" else "Smoke", value = partner.smokeStatus.replaceFirstChar { it.uppercase() }.replace("_", " "))
                                    InfoChip(icon = Icons.Default.Restaurant, label = if (isArabic) "الأكل الحلال" else "Eating habits", value = partner.eatingHabit.replaceFirstChar { it.uppercase() }.replace("_", " "))
                                    InfoChip(icon = Icons.Default.FavoriteBorder, label = if (isArabic) "الحالة الاجتماعية" else "Marital Status", value = partner.maritalStatus.replaceFirstChar { it.uppercase() }.replace("_", " "))
                                    InfoChip(icon = Icons.Default.ChildCare, label = if (isArabic) "الأطفال" else "Have children", value = partner.haveChildren.replaceFirstChar { it.uppercase() }.replace("_", " "))
                                    InfoChip(icon = Icons.Default.Work, label = if (isArabic) "المهنة" else "Occupation", value = partner.occupation.replaceFirstChar { it.uppercase() }.replace("_", " "))
                                    InfoChip(icon = Icons.Default.MonetizationOn, label = if (isArabic) "الدخل السنوي" else "Annual income", value = partner.annualIncome.replaceFirstChar { it.uppercase() }.replace("_", " "))
                                }
                            }

                            // Background / Cultural Values
                            InfoSectionCard(
                                title = if (isArabic) "القيم والخلفية الثقافية" else "Background / Cultural Values"
                            ) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    InfoChip(icon = Icons.Default.Language, label = if (isArabic) "الجنسية" else "Nationality", value = partner.nationality.ifEmpty { if (isArabic) "غير محدد" else "Not specified" })
                                    InfoChip(icon = Icons.Default.School, label = if (isArabic) "التعليم" else "Education", value = partner.educationLevel.ifEmpty { if (isArabic) "غير محدد" else "Not specified" })
                                    val languagesStr = if (partner.languagesSpoken.isNotEmpty()) partner.languagesSpoken.joinToString(", ") else if (isArabic) "العربية" else "Arabic"
                                    InfoChip(icon = Icons.Default.Translate, label = if (isArabic) "اللغات" else "Languages spoken", value = languagesStr)
                                    InfoChip(icon = Icons.Default.Book, label = if (isArabic) "الديانة" else "Religion", value = partner.sect.getDisplayName(isArabic))
                                    InfoChip(icon = Icons.Default.Stars, label = if (isArabic) "الالتزام الديني" else "Religious values", value = partner.religiousValues.replaceFirstChar { it.uppercase() }.replace("_", " "))
                                    InfoChip(icon = Icons.Default.AccessTime, label = if (isArabic) "حضور الصلوات" else "Attend services", value = partner.attendReligiousService.replaceFirstChar { it.uppercase() }.replace("_", " "))
                                    InfoChip(icon = Icons.AutoMirrored.Filled.MenuBook, label = if (isArabic) "قراءة القرآن" else "Read Qur'an", value = partner.readQuran.replaceFirstChar { it.uppercase() }.replace("_", " "))
                                    InfoChip(icon = Icons.Default.Accessibility, label = if (isArabic) "منشئ الملف" else "Profile creator", value = partner.profileCreator.replaceFirstChar { it.uppercase() }.replace("_", " "))
                                }
                            }
                        }
                    }

                    // Show More/Less Button
                    TextButton(
                        onClick = { showMoreDetails = !showMoreDetails },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = if (showMoreDetails) {
                                if (isArabic) "عرض أقل ^" else "Show less ^"
                            } else {
                                if (isArabic) "عرض المزيد v" else "Show more v"
                            },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // E. Partner Preferences "I'm Looking For" Section
                InfoSectionCard(
                    title = if (isArabic) "شريكي المفضل" else "I'M LOOKING FOR",
                    badgeText = if (isArabic) "تطابق" else "Matches",
                    badgeValue = "1"
                ) {
                    Text(
                        text = partner.partnerPreferences.ifEmpty { if (isArabic) "لا توجد تفاصيل" else "No details provided." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoChip(
                            icon = Icons.Default.Person,
                            label = if (isArabic) "الجنس المطلق" else "Gender",
                            value = if (partner.gender == Gender.MALE) {
                                if (isArabic) "أنثى" else "Female"
                            } else {
                                if (isArabic) "ذكر" else "Male"
                            },
                            verified = true
                        )
                        InfoChip(
                            icon = Icons.Default.Cake,
                            label = if (isArabic) "العمر المفضل" else "Age",
                            value = "25 - 45"
                        )
                    }
                }

                // F. In-Card Premium Banner Card (For non-premium users)
                if (!isPremium) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Unlock Messages",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isArabic) "إرسال واستقبال الرسائل بلا حدود" else "Send and receive messages",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isArabic) {
                                    "يمكن للأعضاء المتميزين إرسال رسائل غير محدودة لجميع الأعضاء في ميثاق."
                                } else {
                                    "Premium members can send and receive unlimited messages to all members on Mithaq."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onNavigateToUpgrade,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(if (isArabic) "اشترك الآن" else "Upgrade Now", color = Color.White)
                            }
                        }
                    }
                }

                // G. Block / Report Cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Block Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                coroutineScope.launch {
                                    val isMock = com.mithaq.app.Config.isMock()
                                    if (isMock) {
                                        val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
                                        val blockedStr = prefs.getString("blocked_users_${currentUser.uid}", "[]") ?: "[]"
                                        val array = org.json.JSONArray(blockedStr)
                                        array.put(partner.uid)
                                        prefs.edit().putString("blocked_users_${currentUser.uid}", array.toString()).apply()
                                    } else {
                                        matchDetailViewModel.blockUser(currentUser.uid, partner.uid)
                                    }
                                    android.widget.Toast.makeText(
                                        context,
                                        if (isArabic) "تم حظر العضو بنجاح" else "User blocked successfully",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                    onBack()
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "Block",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isArabic) "حظر العضو" else "Block",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isArabic) "لن يتمكن هذا العضو من مراسلتك بعد الآن." else "This user won't be able to write to you anymore.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Report Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                coroutineScope.launch {
                                    val isMock = com.mithaq.app.Config.isMock()
                                    if (isMock) {
                                        val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
                                        val reportsStr = prefs.getString("user_reports", "[]") ?: "[]"
                                        val array = org.json.JSONArray(reportsStr)
                                        val reportObj = org.json.JSONObject().apply {
                                            put("reporterId", currentUser.uid)
                                            put("reportedId", partner.uid)
                                            put("timestamp", System.currentTimeMillis())
                                        }
                                        array.put(reportObj)
                                        prefs.edit().putString("user_reports", array.toString()).apply()
                                    } else {
                                        matchDetailViewModel.reportUser(currentUser.uid, partner.uid)
                                    }
                                    android.widget.Toast.makeText(
                                        context,
                                        if (isArabic) "تم إرسال التقرير للإدارة للمراجعة" else "Report submitted to admin for review",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                    onBack()
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReportProblem,
                                contentDescription = "Report",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isArabic) "إبلاغ" else "Report",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isArabic) "الإبلاغ عن العضو بسبب سلوك غير لائق." else "Report user for inappropriate behavior.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Member ID in footer
                Text(
                    text = if (isArabic) "رقم العضوية: ${(kotlin.math.abs(partner.uid.hashCode()) % 9000000) + 1000000}" else "Member id: ${(kotlin.math.abs(partner.uid.hashCode()) % 9000000) + 1000000}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            }
        }

        // H. Floating Bottom Sticky Action Bar (Like, Favorite & Chat icons in pill)
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .wrapContentWidth()
                .height(60.dp),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Heart Like icon
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            val isMutual = likesRepository.addLike(currentUser.uid, partner.uid)
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
                                    if (isArabic) "تم إرسال الإعجاب!" else "Like sent!",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isArabic) "إعجاب" else "Like",
                        tint = if (isLiked) Color(0xFFE53935) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Divider line
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.5f)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                )

                // Star Favorite icon
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            val added = likesRepository.toggleFavorite(currentUser.uid, partner.uid)
                            isFavorite = added
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
                    }
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (isArabic) "مفضلة" else "Favorite",
                        tint = if (isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Divider line
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.5f)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                )

                // Chat bubble icon
                IconButton(
                    onClick = {
                        if (isCompatible) {
                            onNavigateToChat(partner)
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                if (isArabic) "غير متوافق! عدّل فلاتر البحث." else "Incompatible match!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = if (isArabic) "محادثة" else "Chat",
                        tint = if (isCompatible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }

    if (showIstikharaGuide) {
        IstikharaGuideDialog(
            isArabic = isArabic,
            onDismiss = { showIstikharaGuide = false }
        )
    }
}

@Composable
private fun InfoSectionCard(
    title: String,
    badgeText: String? = null,
    badgeValue: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (badgeText != null && badgeValue != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$badgeValue $badgeText",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun TextSectionCard(
    title: String,
    content: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    label: String,
    value: String,
    verified: Boolean = false
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "$label: $value",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (verified) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Verified",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
