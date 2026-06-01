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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mithaq.app.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mithaq.app.domain.model.PublicProfile
import com.mithaq.app.ui.components.MithaqEmptyState
import com.mithaq.app.ui.requests.ChatRequestUiState
import com.mithaq.app.ui.requests.ChatRequestViewModel
import com.mithaq.app.ui.requests.InterestRequestUiState
import com.mithaq.app.ui.requests.InterestRequestViewModel
import com.mithaq.app.ui.requests.PhotoRequestUiState
import com.mithaq.app.ui.requests.PhotoRequestViewModel

@Composable
private fun localizedString(isArabic: Boolean, englishResId: Int, arabicResId: Int): String =
    stringResource(id = if (isArabic) arabicResId else englishResId)

@Composable
fun MithaqDiscoverScreen(
    currentUserId: String,
    isArabic: Boolean,
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = viewModel(key = "mithaq_discover_home"),
    interestRequestViewModel: InterestRequestViewModel,
    photoRequestViewModel: PhotoRequestViewModel,
    chatRequestViewModel: ChatRequestViewModel
) {
    val state by viewModel.state.collectAsState()
    val interestState by interestRequestViewModel.state.collectAsState()
    val photoState by photoRequestViewModel.state.collectAsState()
    val chatState by chatRequestViewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Text(
            text = localizedString(isArabic, R.string.discover_title, R.string.discover_title_ar),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = localizedString(isArabic, R.string.discover_subtitle, R.string.discover_subtitle_ar),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        InterestRequestMessage(state = interestState)
        PhotoRequestMessage(state = photoState)
        ChatRequestMessage(state = chatState)
        if (interestState.message != null ||
            interestState.errorMessage != null ||
            photoState.message != null ||
            photoState.errorMessage != null ||
            chatState.message != null ||
            chatState.errorMessage != null
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
            onRequestPhoto = { toUserId ->
                photoRequestViewModel.requestPhoto(currentUserId, toUserId)
            },
            onRequestChat = { toUserId ->
                chatRequestViewModel.requestChat(currentUserId, toUserId)
            },
            onRetry = viewModel::loadProfiles
        )
    }
}
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
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun InterestRequestMessage(state: InterestRequestUiState) {
    val message = state.errorMessage ?: state.message ?: return
    val isError = state.errorMessage != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            }
        )
    }
}

@Composable
private fun PhotoRequestMessage(state: PhotoRequestUiState) {
    val message = state.errorMessage ?: state.message ?: return
    val isError = state.errorMessage != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            }
        )
    }
}

@Composable
private fun ChatRequestMessage(state: ChatRequestUiState) {
    val message = state.errorMessage ?: state.message ?: return
    val isError = state.errorMessage != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            }
        )
    }
}

@Composable
private fun DiscoverProfileContent(
    state: DiscoverUiState,
    isArabic: Boolean,
    currentUserId: String,
    interestState: InterestRequestUiState,
    photoState: PhotoRequestUiState,
    chatState: ChatRequestUiState,
    onSendInterest: (String) -> Unit,
    onRequestPhoto: (String) -> Unit,
    onRequestChat: (String) -> Unit,
    onRetry: () -> Unit
) {
    when {
        state.isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        state.errorMessage != null -> {
            MithaqEmptyState(
                title = localizedString(isArabic, R.string.discover_load_error_title, R.string.discover_load_error_title_ar),
                message = localizedString(isArabic, R.string.discover_load_error_message, R.string.discover_load_error_message_ar),
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
                    onRequestPhoto = onRequestPhoto,
                    onRequestChat = onRequestChat,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

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
    onRequestPhoto: (String) -> Unit = {},
    onRequestChat: (String) -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.displayTitle(isArabic),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = profile.locationLabel(isArabic),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = {},
                    label = { Text("${profile.profileCompletionPercent}%") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (profile.isIdentityVerified) {
                    ProfileSignalChip(localizedString(isArabic, R.string.discover_identity_verified, R.string.discover_identity_verified_ar), Icons.Filled.CheckCircle)
                } else if (profile.isEmailVerified) {
                    ProfileSignalChip(localizedString(isArabic, R.string.discover_email_verified, R.string.discover_email_verified_ar), Icons.Filled.CheckCircle)
                }
                if (profile.hasGuardian) {
                    ProfileSignalChip(localizedString(isArabic, R.string.discover_guardian_added, R.string.discover_guardian_added_ar), Icons.Filled.Person)
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
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val requestStatus = interestState.sentStatusByUserId[profile.userId]
                val canSendInterest = currentUserId.isNotBlank() &&
                        profile.userId != currentUserId &&
                        profile.userId !in interestState.sendingToUserIds &&
                        (requestStatus == null || requestStatus == "cancelled")
                Button(
                    onClick = { onSendInterest(profile.userId) },
                    enabled = canSendInterest,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
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
                OutlinedButton(
                    onClick = { onRequestPhoto(profile.userId) },
                    enabled = canRequestPhoto,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
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
            val chatStatus = chatState.sentStatusByUserId[profile.userId]
            val hasAcceptedInterestForChat = profile.userId in interestState.acceptedWithUserIds
            val canRequestChat = currentUserId.isNotBlank() &&
                    profile.userId != currentUserId &&
                    profile.userId !in chatState.requestingToUserIds &&
                    hasAcceptedInterestForChat &&
                    (chatStatus == null || chatStatus == "cancelled")
            OutlinedButton(
                onClick = { onRequestChat(profile.userId) },
                enabled = canRequestChat,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                val chatButtonText = when {
                    profile.userId in chatState.requestingToUserIds -> localizedString(isArabic, R.string.discover_requesting, R.string.discover_requesting_ar)
                    chatStatus == "pending" -> localizedString(isArabic, R.string.discover_chat_requested, R.string.discover_chat_requested_ar)
                    chatStatus == "approved" -> localizedString(isArabic, R.string.discover_chat_approved, R.string.discover_chat_approved_ar)
                    chatStatus == "declined" -> localizedString(isArabic, R.string.discover_chat_declined, R.string.discover_chat_declined_ar)
                    !hasAcceptedInterestForChat -> localizedString(isArabic, R.string.discover_interest_first, R.string.discover_interest_first_ar)
                    else -> localizedString(isArabic, R.string.discover_request_chat, R.string.discover_request_chat_ar)
                }
                Text(chatButtonText)
            }
        }
    }
}

@Composable
private fun PhotoPrivacyPlaceholder(
    mode: String,
    isArabic: Boolean
) {
    val normalizedMode = mode.ifBlank { "blurred_by_default" }
    val label = when (normalizedMode) {
        "hidden" -> localizedString(isArabic, R.string.discover_photo_hidden, R.string.discover_photo_hidden_ar)
        "approved_users_only" -> localizedString(isArabic, R.string.discover_photo_by_approval, R.string.discover_photo_by_approval_ar)
        "matched_users_only" -> localizedString(isArabic, R.string.discover_photo_for_matches, R.string.discover_photo_for_matches_ar)
        else -> localizedString(isArabic, R.string.discover_photo_blurred, R.string.discover_photo_blurred_ar)
    }
    val icon = when (normalizedMode) {
        "approved_users_only" -> Icons.Filled.Visibility
        "matched_users_only" -> Icons.Filled.Favorite
        else -> Icons.Filled.Lock
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.22f)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.34f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(38.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ProfileSignalChip(
    label: String,
    icon: ImageVector
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
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.58f),
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            leadingIconContentColor = MaterialTheme.colorScheme.secondary
        )
    )
}

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
