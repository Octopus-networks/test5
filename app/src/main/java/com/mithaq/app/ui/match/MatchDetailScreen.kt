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
import com.mithaq.app.ui.theme.GlassBorderDark
import com.mithaq.app.ui.theme.GlassBorderLight
import com.mithaq.app.ui.theme.GlassSurfaceDark
import com.mithaq.app.ui.theme.GlassSurfaceLight
import kotlinx.coroutines.launch
import androidx.compose.foundation.isSystemInDarkTheme

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    partner: UserProfile,
    currentUser: UserProfile,
    isArabic: Boolean,
    likesRepository: com.mithaq.app.data.LikesRepository,
    searchViewModel: com.mithaq.app.ui.filter.SearchViewModel,
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
    val context = androidx.compose.ui.platform.LocalContext.current

    // Register profile view automatically on open
    LaunchedEffect(partner.uid) {
        likesRepository.addProfileView(currentUser.uid, partner.uid)
        // Check if already favorite and already liked
        isFavorite = likesRepository.getFavorites(currentUser.uid).contains(partner.uid)
        isLiked = likesRepository.getLikesList(currentUser.uid).contains(partner.uid)
    }

    val isDark = isSystemInDarkTheme()
    val glassBgColor = if (isDark) GlassSurfaceDark else GlassSurfaceLight
    val glassBorderColor = if (isDark) GlassBorderDark else GlassBorderLight

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
                // Partner image or blurred placeholder
                val isAccessApproved = partner.photoAccessApprovedUsers.contains(currentUser.uid)
                val isBlurred = if (isCompatible) !isAccessApproved else true

                UserProfileImage(
                    imageUrl = if (isCompatible) partner.imageUrl else "",
                    gender = partner.gender,
                    isBlurred = isBlurred,
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                    modifier = Modifier.fillMaxSize()
                )

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
                        imageVector = Icons.Default.ArrowBack,
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
                            text = if (isCompatible) "${partner.name}, ${partner.age}" else (if (isArabic) "عضو غير متوافق" else "Incompatible Match"),
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
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isCompatible) "${partner.city}, ${partner.country}" else (if (isArabic) "الموقع مخفي لعدم التوافق" else "Location hidden"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Active Status Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isOnline = kotlin.math.abs(partner.uid.hashCode()) % 3 == 0
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) Color(0xFF4CAF50) else Color(0xFFFF5252))
                        )
                        Text(
                            text = if (isOnline) {
                                if (isArabic) "متصل الآن" else "Active now"
                            } else {
                                if (isArabic) "نشط منذ ٥ دقائق" else "Active 5m ago"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                // Inline quick interaction overlays on the bottom right of photo
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Chat",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
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
                            value = "${partner.city}, ${partner.country}",
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
                        InfoChip(icon = Icons.Default.Palette, label = if (isArabic) "لون الشعر" else "Hair color", value = if (isArabic) "أسود" else "Black")
                        InfoChip(icon = Icons.Default.Visibility, label = if (isArabic) "لون العين" else "Eye color", value = if (isArabic) "أسود" else "Black")
                        InfoChip(icon = Icons.Default.Straighten, label = if (isArabic) "الطول" else "Height", value = "159 cm (5'3\")")
                        InfoChip(icon = Icons.Default.MonitorWeight, label = if (isArabic) "الوزن" else "Weight", value = "65 kg (143 lb)")
                        InfoChip(icon = Icons.Default.Scale, label = if (isArabic) "بنية الجسم" else "Body style", value = if (isArabic) "رياضي" else "Athletic")
                        InfoChip(icon = Icons.Default.PersonOutline, label = if (isArabic) "العرق" else "Ethnicity", value = if (isArabic) "عربي" else "Arab")
                        InfoChip(icon = Icons.Default.Face, label = if (isArabic) "المظهر" else "Appearance", value = if (isArabic) "جذاب جداً" else "Very attractive")
                    }
                }

                // C. Bio/Text Cards
                TextSectionCard(
                    title = if (isArabic) "مقدمتي" else "My Bio",
                    content = if (isArabic) "روحي الجميلة وتطلعاتي نحو المستقبل" else "Lovely soul looking for a blessed journey."
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
                                    InfoChip(icon = Icons.Default.Flight, label = if (isArabic) "الانتقال" else "Relocate", value = if (isArabic) "مستعد للانتقال لبلد آخر" else "Willing to relocate")
                                    InfoChip(icon = Icons.Default.LocalBar, label = if (isArabic) "المشروبات" else "Drink", value = if (isArabic) "لا أشرب" else "Don't drink")
                                    InfoChip(icon = Icons.Default.SmokingRooms, label = if (isArabic) "التدخين" else "Smoke", value = if (isArabic) "لا أدخن" else "Don't smoke")
                                    InfoChip(icon = Icons.Default.Restaurant, label = if (isArabic) "الأكل الحلال" else "Eating habits", value = if (isArabic) "أكل حلال عندما أستطيع" else "Halal food")
                                    InfoChip(icon = Icons.Default.FavoriteBorder, label = if (isArabic) "الحالة الاجتماعية" else "Marital Status", value = if (isArabic) "أرمل" else "Widowed")
                                    InfoChip(icon = Icons.Default.ChildCare, label = if (isArabic) "الأطفال" else "Have children", value = if (isArabic) "لا يوجد أطفال" else "No children")
                                    InfoChip(icon = Icons.Default.Work, label = if (isArabic) "المهنة" else "Occupation", value = if (isArabic) "أخرى" else "Other")
                                    InfoChip(icon = Icons.Default.MonetizationOn, label = if (isArabic) "الدخل السنوي" else "Annual income", value = if (isArabic) "فضل عدم الإفصاح" else "Prefer not to say")
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
                                    InfoChip(icon = Icons.Default.Language, label = if (isArabic) "الجنسية" else "Nationality", value = if (isArabic) "سوري" else "Syrian")
                                    InfoChip(icon = Icons.Default.School, label = if (isArabic) "التعليم" else "Education", value = if (isArabic) "بكالوريوس" else "Bachelors Degree")
                                    InfoChip(icon = Icons.Default.Translate, label = if (isArabic) "اللغات" else "Languages spoken", value = "English, Arabic")
                                    InfoChip(icon = Icons.Default.Book, label = if (isArabic) "الديانة" else "Religion", value = partner.sect.getDisplayName(isArabic))
                                    InfoChip(icon = Icons.Default.Stars, label = if (isArabic) "الالتزام الديني" else "Religious values", value = if (isArabic) "ملتزم جداً" else "Very Religious")
                                    InfoChip(icon = Icons.Default.AccessTime, label = if (isArabic) "حضور الصلوات" else "Attend services", value = if (isArabic) "يومياً" else "Daily")
                                    InfoChip(icon = Icons.Default.MenuBook, label = if (isArabic) "قراءة القرآن" else "Read Qur'an", value = if (isArabic) "يومياً" else "Daily")
                                    InfoChip(icon = Icons.Default.Accessibility, label = if (isArabic) "منشئ الملف" else "Profile creator", value = if (isArabic) "نفسي" else "Self")
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
                        text = if (isArabic) {
                            "يجب أن يكون لطيفاً، مهتماً، محترماً وصادقاً ومقدراً للمسؤولية، وقبل كل شيء يخاف الله."
                        } else {
                            "He must be lovely, caring, respectful, honest and understanding, above all he must have the fear of Allah."
                        },
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
                                        try {
                                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                            db.collection("blocks").document("${currentUser.uid}_${partner.uid}").set(
                                                mapOf(
                                                    "blockerId" to currentUser.uid,
                                                    "blockedId" to partner.uid,
                                                    "timestamp" to System.currentTimeMillis()
                                                )
                                            )
                                        } catch (e: java.lang.Exception) {
                                            e.printStackTrace()
                                        }
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
                                        try {
                                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                            db.collection("reports").add(
                                                mapOf(
                                                    "reporterId" to currentUser.uid,
                                                    "reportedId" to partner.uid,
                                                    "reason" to "Spam / Inappropriate behavior",
                                                    "timestamp" to System.currentTimeMillis()
                                                )
                                            )
                                        } catch (e: java.lang.Exception) {
                                            e.printStackTrace()
                                        }
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
