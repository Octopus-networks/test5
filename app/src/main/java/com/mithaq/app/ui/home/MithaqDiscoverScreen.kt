package com.mithaq.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mithaq.app.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mithaq.app.domain.model.PublicProfile
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import com.mithaq.app.ui.components.MithaqEmptyState
import com.mithaq.app.ui.components.MithaqStateIllustration
import com.mithaq.app.ui.components.MithaqIllustrationType
import com.mithaq.app.ui.components.MithaqLoadingSkeleton
import com.mithaq.app.ui.components.SkeletonType
import com.mithaq.app.ui.requests.ChatRequestUiState
import com.mithaq.app.ui.requests.ChatRequestViewModel
import com.mithaq.app.ui.requests.InterestRequestUiState
import com.mithaq.app.ui.requests.InterestRequestViewModel
import com.mithaq.app.ui.requests.PhotoRequestUiState
import com.mithaq.app.ui.requests.PhotoRequestViewModel
import com.mithaq.app.ui.theme.AccentGold
import com.mithaq.app.ui.theme.AccentGoldDeep
import com.mithaq.app.ui.theme.BackgroundDark
import com.mithaq.app.ui.theme.ErrorRed
import com.mithaq.app.ui.theme.OutlineWarm
import com.mithaq.app.ui.theme.OutlineWarmVariant
import com.mithaq.app.ui.theme.PrimaryEmeraldDark
import com.mithaq.app.ui.theme.PrimaryEmeraldLight
import com.mithaq.app.ui.theme.SurfaceDark
import com.mithaq.app.ui.theme.SurfaceVariantDark
import com.mithaq.app.ui.theme.TextSecondaryDark
import com.mithaq.app.ui.theme.CardGlowEmerald
import com.mithaq.app.ui.theme.CardGlowGold
import com.mithaq.app.ui.theme.ShimmerHighlight
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.geometry.Offset

@Composable
private fun localizedString(isArabic: Boolean, englishResId: Int, arabicResId: Int): String =
    stringResource(id = if (isArabic) arabicResId else englishResId)

// ─────────────────────────────────────────────────────────────────────────────
// Discover Screen (root)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MithaqDiscoverScreen(
    currentUserId: String,
    isArabic: Boolean,
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = viewModel(key = "mithaq_discover_home_$currentUserId"),
    interestRequestViewModel: InterestRequestViewModel,
    photoRequestViewModel: PhotoRequestViewModel,
    chatRequestViewModel: ChatRequestViewModel,
    onOpenMessages: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val interestState by interestRequestViewModel.state.collectAsState()
    val photoState by photoRequestViewModel.state.collectAsState()
    val chatState by chatRequestViewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Text(
            text = localizedString(isArabic, R.string.discover_title, R.string.discover_title_ar),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AccentGold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = localizedString(isArabic, R.string.discover_subtitle, R.string.discover_subtitle_ar),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondaryDark
        )
        Spacer(modifier = Modifier.height(18.dp))
        PublicProfileFilterRow(
            isArabic = isArabic,
            selectedFilter = state.selectedFilter,
            onFilterSelected = viewModel::selectFilter
        )
        if (state.isNearMePlaceholder) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = localizedString(isArabic, R.string.discover_location_note, R.string.discover_location_note_ar),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryDark
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        InterestRequestMessage(state = interestState, isArabic = isArabic)
        PhotoRequestMessage(state = photoState, isArabic = isArabic)
        ChatRequestMessage(state = chatState, isArabic = isArabic)
        if (interestState.messageRes != null || interestState.errorMessageRes != null || interestState.errorMessage != null ||
            photoState.messageRes != null || photoState.errorMessageRes != null || photoState.errorMessage != null ||
            chatState.messageRes != null || chatState.errorMessageRes != null || chatState.errorMessage != null
        ) {
            Spacer(modifier = Modifier.height(12.dp))
        }
        DiscoverProfileContent(
            state = state,
            isArabic = isArabic,
            currentUserId = currentUserId,
            interestState = interestState,
            photoState = photoState,
            chatState = chatState,
            onSendInterest = { toUserId ->
                interestRequestViewModel.sendInterest(currentUserId, toUserId)
            },
            onCancelInterest = { toUserId ->
                interestRequestViewModel.cancelInterest(currentUserId, "${currentUserId}_$toUserId")
            },
            onRequestPhoto = { toUserId ->
                photoRequestViewModel.requestPhoto(currentUserId, toUserId)
            },
            onRequestChat = { toUserId ->
                chatRequestViewModel.requestChat(currentUserId, toUserId)
            },
            onOpenChat = { onOpenMessages() },
            onRetry = viewModel::loadProfiles
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Filter Chips
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PublicProfileFilterRow(
    isArabic: Boolean,
    selectedFilter: PublicProfileFilter,
    onFilterSelected: (PublicProfileFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PublicProfileFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label(isArabic)) },
                shape = RoundedCornerShape(16.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentGoldDeep.copy(alpha = 0.32f),
                    selectedLabelColor = AccentGold,
                    containerColor = SurfaceVariantDark.copy(alpha = 0.5f),
                    labelColor = TextSecondaryDark
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Status Toast Cards (Interest / Photo / Chat)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InterestRequestMessage(state: InterestRequestUiState, isArabic: Boolean) {
    val messageText = state.errorMessage ?: state.errorMessageRes?.let { localizedString(isArabic, it, state.errorMessageResAr ?: it) } ?: state.messageRes?.let { localizedString(isArabic, it, state.messageResAr ?: it) } ?: return
    val isError = state.errorMessage != null || state.errorMessageRes != null
    val borderColor = if (isError) ErrorRed.copy(alpha = 0.5f) else PrimaryEmeraldLight.copy(alpha = 0.4f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = messageText,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFF2EFEA)
        )
    }
}

@Composable
private fun PhotoRequestMessage(state: PhotoRequestUiState, isArabic: Boolean) {
    val messageText = state.errorMessage ?: state.errorMessageRes?.let { localizedString(isArabic, it, state.errorMessageResAr ?: it) } ?: state.messageRes?.let { localizedString(isArabic, it, state.messageResAr ?: it) } ?: return
    val isError = state.errorMessage != null || state.errorMessageRes != null
    val borderColor = if (isError) ErrorRed.copy(alpha = 0.5f) else PrimaryEmeraldLight.copy(alpha = 0.4f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = messageText,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFF2EFEA)
        )
    }
}

@Composable
private fun ChatRequestMessage(state: ChatRequestUiState, isArabic: Boolean) {
    val messageText = state.errorMessage ?: state.errorMessageRes?.let { localizedString(isArabic, it, state.errorMessageResAr ?: it) } ?: state.messageRes?.let { localizedString(isArabic, it, state.messageResAr ?: it) } ?: return
    val isError = state.errorMessage != null || state.errorMessageRes != null
    val borderColor = if (isError) ErrorRed.copy(alpha = 0.5f) else PrimaryEmeraldLight.copy(alpha = 0.4f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = messageText,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFF2EFEA)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Content Dispatcher (loading / error / empty / list)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DiscoverProfileContent(
    state: DiscoverUiState,
    isArabic: Boolean,
    currentUserId: String,
    interestState: InterestRequestUiState,
    photoState: PhotoRequestUiState,
    chatState: ChatRequestUiState,
    onSendInterest: (String) -> Unit,
    onCancelInterest: (String) -> Unit = {},
    onRequestPhoto: (String) -> Unit,
    onRequestChat: (String) -> Unit,
    onOpenChat: (String) -> Unit = {},
    onRetry: () -> Unit
) {
    when {
        state.isLoading -> {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(2) {
                    MithaqLoadingSkeleton(type = SkeletonType.PROFILE_CARD)
                }
            }
        }
        state.errorMessageRes != null || state.errorMessage != null -> {
            MithaqEmptyState(
                title = localizedString(isArabic, R.string.discover_load_error_title, R.string.discover_load_error_title_ar),
                message = state.errorMessageRes?.let { localizedString(isArabic, it, state.errorMessageResAr ?: it) } 
                          ?: state.errorMessage 
                          ?: localizedString(isArabic, R.string.discover_load_error_message, R.string.discover_load_error_message_ar),
                icon = Icons.Filled.Refresh,
                actionLabel = localizedString(isArabic, R.string.common_retry, R.string.common_retry_ar),
                onAction = onRetry
            )
        }
        state.isEmpty -> {
            MithaqEmptyState(
                title = localizedString(isArabic, R.string.discover_no_profiles_title, R.string.discover_no_profiles_title_ar),
                message = localizedString(isArabic, R.string.discover_no_profiles_message, R.string.discover_no_profiles_message_ar),
                icon = Icons.Filled.Search
            )
        }
        state.hasNoFilterResults -> {
            MithaqEmptyState(
                title = localizedString(isArabic, R.string.discover_no_filter_results_title, R.string.discover_no_filter_results_title_ar),
                message = localizedString(isArabic, R.string.discover_no_filter_results_message, R.string.discover_no_filter_results_message_ar),
                icon = Icons.Filled.Search
            )
        }
        else -> {
            state.visibleProfiles.forEach { profile ->
                MithaqPublicProfileCard(
                    profile = profile,
                    isArabic = isArabic,
                    currentUserId = currentUserId,
                    interestState = interestState,
                    photoState = photoState,
                    chatState = chatState,
                    onSendInterest = onSendInterest,
                    onCancelInterest = onCancelInterest,
                    onRequestPhoto = onRequestPhoto,
                    onRequestChat = onRequestChat,
                    onOpenChat = onOpenChat,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Profile Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MithaqPublicProfileCard(
    profile: PublicProfile,
    isArabic: Boolean,
    modifier: Modifier = Modifier,
    currentUserId: String = "",
    interestState: InterestRequestUiState = InterestRequestUiState(),
    photoState: PhotoRequestUiState = PhotoRequestUiState(),
    chatState: ChatRequestUiState = ChatRequestUiState(),
    onSendInterest: (String) -> Unit = {},
    onCancelInterest: (String) -> Unit = {},
    onRequestPhoto: (String) -> Unit = {},
    onRequestChat: (String) -> Unit = {},
    onOpenChat: (String) -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(CardGlowGold, CardGlowEmerald, CardGlowGold)
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
            // Phase 11: show the real photo only when this viewer holds an approved photo
            // request for the owner. Otherwise keep the privacy-aware placeholder.
            // SecurePhotoView re-checks access and Storage rules remain the real boundary.
            if (photoState.sentStatusByUserId[profile.userId] == "approved") {
                com.mithaq.app.ui.photo.SecurePhotoView(
                    ownerId = profile.userId,
                    photoPrivacyMode = profile.photoPrivacyMode,
                    isArabic = isArabic,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                PhotoPrivacyPlaceholder(
                    mode = profile.photoPrivacyMode,
                    isArabic = isArabic
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            // ── Name, Age & Heart Row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile.displayTitle(isArabic),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold
                        )
                        if (profile.isIdentityVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            com.mithaq.app.ui.components.VerifiedBadge()
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = AccentGold.copy(alpha = 0.6f),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = profile.locationLabel(isArabic),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark
                        )
                    }
                }
                // Heart = express interest, using the existing Send-interest workflow
                // (InterestRequestViewModel via onSendInterest). No separate like system.
                // Filled once an interest request is pending/accepted. A PENDING interest can
                // be taken back by tapping the heart again (cancel); accepted stays locked —
                // unwinding an accepted interest belongs to the requests page.
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
                        .clip(CircleShape)
                        .background(
                            if (heartActive) AccentGold.copy(alpha = 0.15f)
                            else SurfaceVariantDark.copy(alpha = 0.5f)
                        )
                ) {
                    Icon(
                        imageVector = if (heartActive) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = localizedString(isArabic, R.string.discover_send_interest, R.string.discover_send_interest_ar),
                        tint = when {
                            heartActive -> AccentGold
                            canHeart -> OutlineWarm
                            else -> OutlineWarm.copy(alpha = 0.38f)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Trust / Verified Signal Chips ──
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (profile.isIdentityVerified) {
                    ProfileSignalChip(
                        label = localizedString(isArabic, R.string.discover_identity_verified, R.string.discover_identity_verified_ar),
                        icon = Icons.Filled.Shield,
                        chipContainerColor = PrimaryEmeraldDark,
                        chipLabelColor = PrimaryEmeraldLight,
                        chipIconColor = PrimaryEmeraldLight
                    )
                } else if (profile.isEmailVerified) {
                    ProfileSignalChip(
                        label = localizedString(isArabic, R.string.discover_email_verified, R.string.discover_email_verified_ar),
                        icon = Icons.Filled.CheckCircle,
                        chipContainerColor = PrimaryEmeraldDark.copy(alpha = 0.6f),
                        chipLabelColor = PrimaryEmeraldLight,
                        chipIconColor = PrimaryEmeraldLight
                    )
                }
                if (profile.hasGuardian) {
                    ProfileSignalChip(
                        label = localizedString(isArabic, R.string.discover_guardian_added, R.string.discover_guardian_added_ar),
                        icon = Icons.Filled.Person,
                        chipContainerColor = AccentGoldDeep.copy(alpha = 0.22f),
                        chipLabelColor = AccentGold,
                        chipIconColor = AccentGold
                    )
                }
                if (profile.prayerRoutineShared) {
                    ProfileSignalChip(localizedString(isArabic, R.string.discover_prayer_shared, R.string.discover_prayer_shared_ar), Icons.Filled.Lock)
                }
                if (profile.localTimeEnabled) {
                    ProfileSignalChip(localizedString(isArabic, R.string.discover_local_time_shared, R.string.discover_local_time_shared_ar), Icons.Filled.Search)
                }
                if (profile.accountType.isNotBlank()) {
                    ProfileSignalChip(profile.accountType, Icons.Filled.Person)
                }
                if (profile.maritalStatus.isNotBlank()) {
                    ProfileSignalChip(profile.maritalStatus, Icons.Filled.Favorite)
                }
                if (profile.marriageTimeline.isNotBlank()) {
                    ProfileSignalChip(profile.marriageTimeline, Icons.Filled.Chat)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Subtle divider between info section and action buttons ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                AccentGold.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Action Buttons: Send Interest + Request Photo ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // --- Send Interest ---
                val requestStatus = interestState.sentStatusByUserId[profile.userId]
                val canSendInterest = currentUserId.isNotBlank() &&
                        profile.userId != currentUserId &&
                        profile.userId !in interestState.sendingToUserIds &&
                        (requestStatus == null || requestStatus == "cancelled")

                // State-aware button colors (visual only, logic is unchanged)
                val interestColors = when {
                    profile.userId in interestState.sendingToUserIds ->
                        AccentGold.copy(alpha = 0.6f) to BackgroundDark
                    requestStatus == "pending" ->
                        SurfaceVariantDark to AccentGold
                    requestStatus == "accepted" ->
                        PrimaryEmeraldDark to PrimaryEmeraldLight
                    requestStatus == "declined" ->
                        ErrorRed.copy(alpha = 0.15f) to ErrorRed
                    canSendInterest ->
                        AccentGold to BackgroundDark
                    else ->
                        SurfaceVariantDark to TextSecondaryDark.copy(alpha = 0.6f)
                }

                Button(
                    onClick = { onSendInterest(profile.userId) },
                    enabled = canSendInterest,
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(18.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = interestColors.first,
                        contentColor = interestColors.second,
                        disabledContainerColor = interestColors.first,
                        disabledContentColor = interestColors.second
                    )
                ) {
                    if (profile.userId in interestState.sendingToUserIds) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = interestColors.second
                        )
                    } else {
                        Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    val buttonText = when {
                        profile.userId in interestState.sendingToUserIds -> localizedString(isArabic, R.string.discover_sending, R.string.discover_sending_ar)
                        requestStatus == "pending" -> localizedString(isArabic, R.string.discover_sent, R.string.discover_sent_ar)
                        requestStatus == "accepted" -> localizedString(isArabic, R.string.discover_accepted, R.string.discover_accepted_ar)
                        requestStatus == "declined" -> localizedString(isArabic, R.string.discover_interest_declined, R.string.discover_interest_declined_ar)
                        else -> localizedString(isArabic, R.string.discover_send_interest, R.string.discover_send_interest_ar)
                    }
                    Text(buttonText)
                }

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

                // State-aware icon (decorative only — no logic impact)
                val photoIcon = when {
                    photoStatus == "approved" -> Icons.Filled.CheckCircle
                    !photoModeAllowsRequest -> Icons.Filled.Lock
                    !hasAcceptedInterest -> Icons.Filled.Lock
                    else -> Icons.Filled.Visibility
                }

                // State-aware border color
                val photoBorderColor = when {
                    photoStatus == "approved" -> PrimaryEmeraldLight.copy(alpha = 0.5f)
                    photoStatus == "declined" -> ErrorRed.copy(alpha = 0.4f)
                    !canRequestPhoto -> OutlineWarmVariant.copy(alpha = 0.3f)
                    else -> AccentGold.copy(alpha = 0.5f)
                }

                // State-aware content color
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
                        !photoModeAllowsRequest && normalizedPhotoMode == "matched_users_only" -> localizedString(isArabic, R.string.discover_matched_only, R.string.discover_matched_only_ar)
                        !photoModeAllowsRequest -> localizedString(isArabic, R.string.discover_photos_private, R.string.discover_photos_private_ar)
                        !hasAcceptedInterest -> localizedString(isArabic, R.string.discover_interest_first, R.string.discover_interest_first_ar)
                        else -> localizedString(isArabic, R.string.discover_request_photo, R.string.discover_request_photo_ar)
                    }
                    Text(photoButtonText)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- Request Chat ---
            val chatStatus = chatState.sentStatusByUserId[profile.userId]
            val hasAcceptedInterestForChat = profile.userId in interestState.acceptedWithUserIds
            val chatApproved = chatStatus == "approved"
            val canRequestChat = currentUserId.isNotBlank() &&
                    profile.userId != currentUserId &&
                    profile.userId !in chatState.requestingToUserIds &&
                    hasAcceptedInterestForChat &&
                    (chatStatus == null || chatStatus == "cancelled")

            // State-aware icon (decorative only)
            val chatIcon = when {
                !hasAcceptedInterestForChat && !chatApproved -> Icons.Filled.Lock
                else -> Icons.Filled.Chat
            }

            // State-aware border
            val chatBorderColor = when {
                chatApproved -> PrimaryEmeraldDark
                chatStatus == "pending" -> PrimaryEmeraldLight.copy(alpha = 0.5f)
                chatStatus == "declined" -> ErrorRed.copy(alpha = 0.4f)
                !(canRequestChat || chatApproved) -> OutlineWarmVariant.copy(alpha = 0.3f)
                else -> PrimaryEmeraldLight.copy(alpha = 0.5f)
            }

            // State-aware container and content colors
            val chatContainerColor = if (chatApproved) PrimaryEmeraldDark else Color.Transparent
            val chatContentColor = when {
                chatApproved -> Color.White
                profile.userId in chatState.requestingToUserIds -> PrimaryEmeraldLight.copy(alpha = 0.6f)
                chatStatus == "pending" -> PrimaryEmeraldLight
                chatStatus == "declined" -> ErrorRed
                !(canRequestChat || chatApproved) -> OutlineWarm.copy(alpha = 0.5f)
                else -> PrimaryEmeraldLight
            }

            OutlinedButton(
                onClick = {
                    if (chatApproved) onOpenChat(profile.userId) else onRequestChat(profile.userId)
                },
                enabled = canRequestChat || chatApproved,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, chatBorderColor),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = chatContainerColor,
                    contentColor = chatContentColor,
                    disabledContainerColor = chatContainerColor,
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
                    chatApproved -> localizedString(isArabic, R.string.discover_open_chat, R.string.discover_open_chat_ar)
                    chatStatus == "declined" -> localizedString(isArabic, R.string.discover_chat_declined, R.string.discover_chat_declined_ar)
                    !hasAcceptedInterestForChat -> localizedString(isArabic, R.string.discover_interest_first, R.string.discover_interest_first_ar)
                    else -> localizedString(isArabic, R.string.discover_request_chat, R.string.discover_request_chat_ar)
                }
                Text(chatButtonText)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Photo Privacy Placeholder
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PhotoPrivacyPlaceholder(
    mode: String,
    isArabic: Boolean
) {
    val normalizedMode = mode.ifBlank { "blurred_by_default" }
    val caption = when (normalizedMode) {
        "hidden" -> if (isArabic) "صور خاصة" else "PRIVATE PHOTOS"
        "approved_users_only" -> if (isArabic) "اطلب الإذن للعرض" else "REQUEST TO VIEW"
        "matched_users_only" -> if (isArabic) "تظهر بعد التطابق" else "MATCH TO VIEW"
        else -> if (isArabic) "صورة محمية" else "PHOTO PROTECTED"
    }

    // Subtle shimmer sweep animation for the photo placeholder
    val shimmerTransition = rememberInfiniteTransition(label = "photo_placeholder_shimmer")
    val shimmerOffset by shimmerTransition.animateFloat(
        initialValue = -400f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Restart
        ),
        label = "photo_shimmer_offset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(SurfaceDark, BackgroundDark),
                    radius = 600f
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(CardGlowGold, CardGlowEmerald, CardGlowGold)
                ),
                RoundedCornerShape(24.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Shimmer overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.Transparent, ShimmerHighlight, Color.Transparent),
                        start = Offset(shimmerOffset, 0f),
                        end = Offset(shimmerOffset + 250f, 250f)
                    )
                )
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MithaqStateIllustration(
                type = MithaqIllustrationType.SHIELD_LOCK,
                tint = AccentGold,
                modifier = Modifier.size(110.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = AccentGold.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentGold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Signal Chip (color-differentiated by trust level)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileSignalChip(
    label: String,
    icon: ImageVector,
    chipContainerColor: Color = SurfaceVariantDark,
    chipLabelColor: Color = TextSecondaryDark,
    chipIconColor: Color = TextSecondaryDark
) {
    AssistChip(
        onClick = {},
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, chipLabelColor.copy(alpha = 0.15f)),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = chipContainerColor,
            labelColor = chipLabelColor,
            leadingIconContentColor = chipIconColor
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Extension Helpers (logic unchanged)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PublicProfileFilter.label(isArabic: Boolean): String {
    return when (this) {
        PublicProfileFilter.Recommended -> localizedString(isArabic, R.string.discover_filter_recommended, R.string.discover_filter_recommended_ar)
        PublicProfileFilter.NearMe -> localizedString(isArabic, R.string.discover_filter_near_me, R.string.discover_filter_near_me_ar)
        PublicProfileFilter.Verified -> localizedString(isArabic, R.string.discover_filter_verified, R.string.discover_filter_verified_ar)
        PublicProfileFilter.WithGuardian -> localizedString(isArabic, R.string.discover_filter_with_guardian, R.string.discover_filter_with_guardian_ar)
        PublicProfileFilter.RecentlyActive -> localizedString(isArabic, R.string.discover_filter_recently_active, R.string.discover_filter_recently_active_ar)
        PublicProfileFilter.PrayerRoutineShared -> localizedString(isArabic, R.string.discover_filter_prayer_shared, R.string.discover_filter_prayer_shared_ar)
        PublicProfileFilter.NewMembers -> localizedString(isArabic, R.string.discover_new_members, R.string.discover_new_members_ar)
    }
}

@Composable
private fun PublicProfile.displayTitle(isArabic: Boolean): String {
    val name = displayName.ifBlank { localizedString(isArabic, R.string.discover_member_fallback, R.string.discover_member_fallback_ar) }
    val ageLabel = age?.toString()
    return if (ageLabel == null) name else "$name, $ageLabel"
}

@Composable
private fun PublicProfile.locationLabel(isArabic: Boolean): String {
    val parts = listOf(city, country).map { it.trim() }.filter { it.isNotBlank() }
    return parts.joinToString(", ").ifBlank {
        localizedString(isArabic, R.string.discover_location_not_shared, R.string.discover_location_not_shared_ar)
    }
}
