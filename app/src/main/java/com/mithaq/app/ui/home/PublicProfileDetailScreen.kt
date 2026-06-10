package com.mithaq.app.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mithaq.app.R
import com.mithaq.app.domain.model.PublicProfile
import com.mithaq.app.ui.requests.ChatRequestUiState
import com.mithaq.app.ui.requests.InterestRequestUiState
import com.mithaq.app.ui.requests.PhotoRequestUiState
import com.mithaq.app.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
private fun localizedString(isArabic: Boolean, englishResId: Int, arabicResId: Int): String =
    stringResource(id = if (isArabic) arabicResId else englishResId)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileDetailScreen(
    profile: PublicProfile,
    isArabic: Boolean,
    currentUserId: String,
    viewerCountry: String, // from PublicProfileRepository.getCurrentUserCountry
    interestState: InterestRequestUiState,
    photoState: PhotoRequestUiState,
    chatState: ChatRequestUiState,
    onSendInterest: (String) -> Unit,
    onCancelInterest: (String) -> Unit,
    onRequestPhoto: (String) -> Unit,
    onRequestChat: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = localizedString(isArabic, R.string.common_back, R.string.common_back_ar),
                            tint = AccentGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        bottomBar = {
            DetailBottomBar(
                profile = profile,
                isArabic = isArabic,
                currentUserId = currentUserId,
                interestState = interestState,
                photoState = photoState,
                chatState = chatState,
                onSendInterest = onSendInterest,
                onCancelInterest = onCancelInterest,
                onRequestPhoto = onRequestPhoto,
                onRequestChat = onRequestChat
            )
        },
        containerColor = BackgroundDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Header: Avatar + Info
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar Placeholder
                val photoStatus = photoState.sentStatusByUserId[profile.userId]
                if (photoStatus == "approved") {
                    com.mithaq.app.ui.photo.SecurePhotoView(
                        ownerId = profile.userId,
                        photoPrivacyMode = profile.photoPrivacyMode,
                        isArabic = isArabic,
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .border(2.dp, AccentGold, CircleShape)
                    )
                } else {
                    PhotoPrivacyPlaceholder(
                        mode = profile.photoPrivacyMode,
                        isArabic = isArabic,
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name & Badges
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.displayTitle(isArabic),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold
                    )
                    if (profile.isIdentityVerified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        com.mithaq.app.ui.components.VerifiedBadge()
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                // Location & Age
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = AccentGold.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = profile.locationLabel(isArabic),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondaryDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Timezone Row
            val timeZoneId = getTimeZoneForCountry(profile.country)
            if (profile.country.isNotBlank() && timeZoneId != null) {
                LocalTimeRow(timeZoneId = timeZoneId, isArabic = isArabic)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Compatibility Hints
            if (viewerCountry.isNotBlank() && viewerCountry.equals(profile.country, ignoreCase = true)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryEmeraldDark.copy(alpha = 0.2f))
                        .padding(12.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = PrimaryEmeraldLight, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = localizedString(isArabic, R.string.profile_detail_same_country, R.string.profile_detail_same_country_ar),
                        color = PrimaryEmeraldLight,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Info Section
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    InfoRow(
                        label = localizedString(isArabic, R.string.profile_detail_label_marital_status, R.string.profile_detail_label_marital_status_ar),
                        value = profile.maritalStatus.ifBlank { "-" },
                        icon = Icons.Filled.Favorite
                    )
                    Divider(color = SurfaceVariantDark, modifier = Modifier.padding(vertical = 12.dp))
                    InfoRow(
                        label = localizedString(isArabic, R.string.profile_detail_label_marriage_timeline, R.string.profile_detail_label_marriage_timeline_ar),
                        value = profile.marriageTimeline.ifBlank { "-" },
                        icon = Icons.Filled.DateRange
                    )
                    Divider(color = SurfaceVariantDark, modifier = Modifier.padding(vertical = 12.dp))
                    InfoRow(
                        label = localizedString(isArabic, R.string.profile_detail_label_account_type, R.string.profile_detail_label_account_type_ar),
                        value = profile.accountType.ifBlank { "-" },
                        icon = Icons.Filled.Person
                    )
                    Divider(color = SurfaceVariantDark, modifier = Modifier.padding(vertical = 12.dp))
                    InfoRow(
                        label = localizedString(isArabic, R.string.profile_detail_label_guardian, R.string.profile_detail_label_guardian_ar),
                        value = if (profile.hasGuardian) "✓" else "-",
                        icon = Icons.Filled.Shield
                    )
                    
                    if (profile.memberSince != null) {
                        Divider(color = SurfaceVariantDark, modifier = Modifier.padding(vertical = 12.dp))
                        val format = SimpleDateFormat("MMMM yyyy", if (isArabic) Locale("ar") else Locale.ENGLISH)
                        InfoRow(
                            label = localizedString(isArabic, R.string.profile_detail_label_member_since, R.string.profile_detail_label_member_since_ar),
                            value = format.format(profile.memberSince),
                            icon = Icons.Filled.Event
                        )
                    }

                    Divider(color = SurfaceVariantDark, modifier = Modifier.padding(vertical = 12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = OutlineWarm, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = localizedString(isArabic, R.string.profile_detail_label_completion, R.string.profile_detail_label_completion_ar),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryDark
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                progress = { profile.profileCompletionPercent / 100f },
                                modifier = Modifier.size(20.dp),
                                color = AccentGold,
                                trackColor = SurfaceVariantDark,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${profile.profileCompletionPercent}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = OutlineWarm, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryDark
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LocalTimeRow(timeZoneId: String, isArabic: Boolean) {
    var currentTime by remember { mutableStateOf("") }
    
    LaunchedEffect(timeZoneId, isArabic) {
        val sdf = SimpleDateFormat("h:mm a", if (isArabic) Locale("ar") else Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone(timeZoneId)
        while (true) {
            currentTime = sdf.format(Date())
            delay(60_000)
        }
    }

    // Update initially before delay
    DisposableEffect(timeZoneId, isArabic) {
        val sdf = SimpleDateFormat("h:mm a", if (isArabic) Locale("ar") else Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone(timeZoneId)
        currentTime = sdf.format(Date())
        onDispose { }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceVariantDark.copy(alpha = 0.3f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Schedule, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = localizedString(isArabic, R.string.profile_detail_their_time, R.string.profile_detail_their_time_ar),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryDark
            )
        }
        Text(
            text = currentTime,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun getTimeZoneForCountry(country: String): String? {
    val map = mapOf(
        "egypt" to "Africa/Cairo",
        "saudi arabia" to "Asia/Riyadh",
        "uae" to "Asia/Dubai",
        "kuwait" to "Asia/Kuwait",
        "qatar" to "Asia/Qatar",
        "bahrain" to "Asia/Bahrain",
        "oman" to "Asia/Muscat",
        "jordan" to "Asia/Amman",
        "palestine" to "Asia/Gaza",
        "lebanon" to "Asia/Beirut",
        "syria" to "Asia/Damascus",
        "iraq" to "Asia/Baghdad",
        "yemen" to "Asia/Aden",
        "libya" to "Africa/Tripoli",
        "tunisia" to "Africa/Tunis",
        "algeria" to "Africa/Algiers",
        "morocco" to "Africa/Casablanca",
        "sudan" to "Africa/Khartoum",
        "turkey" to "Europe/Istanbul",
        "uk" to "Europe/London",
        "united kingdom" to "Europe/London",
        "usa" to "America/New_York",
        "united states" to "America/New_York",
        "canada" to "America/Toronto",
        "germany" to "Europe/Berlin",
        "france" to "Europe/Paris",
        "indonesia" to "Asia/Jakarta",
        "malaysia" to "Asia/Kuala_Lumpur",
        "pakistan" to "Asia/Karachi",
        "india" to "Asia/Kolkata",
        "bangladesh" to "Asia/Dhaka",
        "nigeria" to "Africa/Lagos",
        "somalia" to "Africa/Mogadishu"
    )
    return map[country.trim().lowercase(Locale.ROOT)]
}

@Composable
private fun DetailBottomBar(
    profile: PublicProfile,
    isArabic: Boolean,
    currentUserId: String,
    interestState: InterestRequestUiState,
    photoState: PhotoRequestUiState,
    chatState: ChatRequestUiState,
    onSendInterest: (String) -> Unit,
    onCancelInterest: (String) -> Unit,
    onRequestPhoto: (String) -> Unit,
    onRequestChat: (String) -> Unit
) {
    Surface(
        color = SurfaceDark,
        tonalElevation = 8.dp,
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Heart Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val heartStatus = interestState.sentStatusByUserId[profile.userId]
                val heartSending = profile.userId in interestState.sendingToUserIds
                val heartActive = heartStatus == "pending" || heartStatus == "accepted" || heartSending
                val canHeart = currentUserId.isNotBlank() &&
                        profile.userId != currentUserId &&
                        !heartSending &&
                        (heartStatus == null || heartStatus == "cancelled" || heartStatus == "pending")

                IconButton(
                    onClick = {
                        if (heartStatus == "pending") onCancelInterest(profile.userId)
                        else onSendInterest(profile.userId)
                    },
                    enabled = canHeart,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (heartActive) AccentGold.copy(alpha = 0.15f)
                            else SurfaceVariantDark.copy(alpha = 0.5f)
                        )
                ) {
                    if (heartSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = AccentGold
                        )
                    } else {
                        Icon(
                            imageVector = if (heartActive) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = localizedString(isArabic, R.string.discover_send_interest, R.string.discover_send_interest_ar),
                            tint = when {
                                heartActive -> AccentGold
                                canHeart -> OutlineWarm
                                else -> OutlineWarm.copy(alpha = 0.38f)
                            },
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // --- Request Photo ---
                val photoStatus = photoState.sentStatusByUserId[profile.userId]
                val hasAcceptedInterest = profile.userId in interestState.acceptedWithUserIds
                val normalizedPhotoMode = profile.photoPrivacyMode.ifBlank { "blurred_by_default" }
                val photoModeAllowsRequest = normalizedPhotoMode == "blurred_by_default" ||
                        normalizedPhotoMode == "approved_users_only"
                val canRequestPhoto = currentUserId.isNotBlank() &&
                        profile.userId != currentUserId &&
                        profile.userId !in photoState.requestingToUserIds &&
                        hasAcceptedInterest &&
                        photoModeAllowsRequest &&
                        (photoStatus == null || photoStatus == "cancelled")

                val photoIcon = when {
                    photoStatus == "approved" -> Icons.Filled.CheckCircle
                    !photoModeAllowsRequest -> Icons.Filled.Lock
                    !hasAcceptedInterest -> Icons.Filled.Lock
                    else -> Icons.Filled.Visibility
                }

                val photoBorderColor = when {
                    photoStatus == "approved" -> PrimaryEmeraldLight.copy(alpha = 0.5f)
                    photoStatus == "declined" -> ErrorRed.copy(alpha = 0.4f)
                    !canRequestPhoto -> OutlineWarmVariant.copy(alpha = 0.3f)
                    else -> AccentGold.copy(alpha = 0.5f)
                }

                val photoContentColor = when {
                    profile.userId in photoState.requestingToUserIds -> AccentGold.copy(alpha = 0.6f)
                    photoStatus == "approved" -> PrimaryEmeraldLight
                    photoStatus == "declined" -> ErrorRed
                    !canRequestPhoto -> OutlineWarm.copy(alpha = 0.5f)
                    else -> AccentGold
                }

                OutlinedButton(
                    onClick = { onRequestPhoto(profile.userId) },
                    enabled = canRequestPhoto,
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, photoBorderColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = photoContentColor,
                        disabledContentColor = photoContentColor
                    )
                ) {
                    if (profile.userId in photoState.requestingToUserIds) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = photoContentColor
                        )
                    } else {
                        Icon(photoIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    val photoButtonText = when {
                        profile.userId in photoState.requestingToUserIds -> localizedString(isArabic, R.string.discover_requesting, R.string.discover_requesting_ar)
                        photoStatus == "pending" -> localizedString(isArabic, R.string.discover_photo_requested, R.string.discover_photo_requested_ar)
                        photoStatus == "approved" -> localizedString(isArabic, R.string.discover_photo_approved, R.string.discover_photo_approved_ar)
                        photoStatus == "declined" -> localizedString(isArabic, R.string.discover_photo_declined, R.string.discover_photo_declined_ar)
                        !hasAcceptedInterest -> localizedString(isArabic, R.string.discover_interest_first, R.string.discover_interest_first_ar)
                        !photoModeAllowsRequest -> localizedString(isArabic, R.string.discover_photos_private, R.string.discover_photos_private_ar)
                        else -> localizedString(isArabic, R.string.discover_request_photo, R.string.discover_request_photo_ar)
                    }
                    Text(photoButtonText)
                }

                // --- Request Chat ---
                val chatStatus = chatState.sentStatusByUserId[profile.userId]
                val canRequestChat = currentUserId.isNotBlank() &&
                        profile.userId != currentUserId &&
                        profile.userId !in chatState.requestingToUserIds &&
                        hasAcceptedInterest &&
                        (chatStatus == null || chatStatus == "cancelled")

                val chatIcon = when {
                    chatStatus == "approved" -> Icons.Filled.Chat
                    !hasAcceptedInterest -> Icons.Filled.Lock
                    else -> Icons.Filled.Chat
                }

                val chatBorderColor = when {
                    chatStatus == "approved" -> PrimaryEmeraldLight.copy(alpha = 0.5f)
                    chatStatus == "declined" -> ErrorRed.copy(alpha = 0.4f)
                    !canRequestChat -> OutlineWarmVariant.copy(alpha = 0.3f)
                    else -> AccentGold.copy(alpha = 0.5f)
                }

                val chatContentColor = when {
                    profile.userId in chatState.requestingToUserIds -> AccentGold.copy(alpha = 0.6f)
                    chatStatus == "approved" -> PrimaryEmeraldLight
                    chatStatus == "declined" -> ErrorRed
                    !canRequestChat -> OutlineWarm.copy(alpha = 0.5f)
                    else -> AccentGold
                }

                OutlinedButton(
                    onClick = { onRequestChat(profile.userId) },
                    enabled = canRequestChat && chatStatus != "approved",
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, chatBorderColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = chatContentColor,
                        disabledContentColor = chatContentColor
                    )
                ) {
                    if (profile.userId in chatState.requestingToUserIds) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = chatContentColor
                        )
                    } else {
                        Icon(chatIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    val chatButtonText = when {
                        profile.userId in chatState.requestingToUserIds -> localizedString(isArabic, R.string.discover_requesting, R.string.discover_requesting_ar)
                        chatStatus == "pending" -> localizedString(isArabic, R.string.discover_chat_requested, R.string.discover_chat_requested_ar)
                        chatStatus == "approved" -> localizedString(isArabic, R.string.discover_chat_approved, R.string.discover_chat_approved_ar)
                        chatStatus == "declined" -> localizedString(isArabic, R.string.discover_chat_declined, R.string.discover_chat_declined_ar)
                        !hasAcceptedInterest -> localizedString(isArabic, R.string.discover_interest_first, R.string.discover_interest_first_ar)
                        else -> localizedString(isArabic, R.string.discover_request_chat, R.string.discover_request_chat_ar)
                    }
                    Text(chatButtonText)
                }
            }
        }
    }
}
