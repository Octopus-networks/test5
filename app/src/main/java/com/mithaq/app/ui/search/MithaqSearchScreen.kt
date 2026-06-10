package com.mithaq.app.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mithaq.app.R
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Color
import com.mithaq.app.ui.components.MithaqEmptyState
import com.mithaq.app.ui.components.MithaqStateIllustration
import com.mithaq.app.ui.components.MithaqIllustrationType
import com.mithaq.app.ui.components.MithaqLoadingSkeleton
import com.mithaq.app.ui.components.SkeletonType
import com.mithaq.app.domain.model.PublicProfile
import com.mithaq.app.ui.home.DiscoverUiState
import com.mithaq.app.ui.home.DiscoverViewModel
import com.mithaq.app.ui.home.MithaqPublicProfileCard
import com.mithaq.app.ui.home.PublicProfileDetailScreen
import com.mithaq.app.ui.home.PublicProfileFilterRow
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
fun MithaqSearchScreen(
    currentUserId: String,
    isArabic: Boolean,
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = viewModel(key = "mithaq_public_search_$currentUserId"),
    interestRequestViewModel: InterestRequestViewModel,
    photoRequestViewModel: PhotoRequestViewModel,
    chatRequestViewModel: ChatRequestViewModel,
    onOpenMessages: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val interestState by interestRequestViewModel.state.collectAsState()
    val photoState by photoRequestViewModel.state.collectAsState()
    val chatState by chatRequestViewModel.state.collectAsState()
    var detailProfile by remember { mutableStateOf<PublicProfile?>(null) }

    if (detailProfile != null) {
        val viewerCountry = state.currentUserCountry ?: ""
        PublicProfileDetailScreen(
            profile = detailProfile!!,
            isArabic = isArabic,
            currentUserId = currentUserId,
            viewerCountry = viewerCountry,
            interestState = interestState,
            photoState = photoState,
            chatState = chatState,
            onSendInterest = { toUserId: String -> interestRequestViewModel.sendInterest(currentUserId, toUserId) },
            onCancelInterest = { toUserId: String -> interestRequestViewModel.cancelInterest(currentUserId, "${currentUserId}_$toUserId") },
            onRequestPhoto = { toUserId: String -> photoRequestViewModel.requestPhoto(currentUserId, toUserId) },
            onRequestChat = { toUserId: String -> chatRequestViewModel.requestChat(currentUserId, toUserId) },
            onBack = { detailProfile = null }
        )
        return
    }

    // The whole point of a Search tab: free-text matching on top of the chip filters.
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Text(
            text = localizedString(isArabic, R.string.search_title, R.string.search_title_ar),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = localizedString(isArabic, R.string.search_subtitle, R.string.search_subtitle_ar),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text(localizedString(isArabic, R.string.search_query_placeholder, R.string.search_query_placeholder_ar))
            },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
        PublicProfileFilterRow(
            isArabic = isArabic,
            selectedFilter = state.selectedFilter,
            onFilterSelected = viewModel::selectFilter
        )
        if (state.isNearMePlaceholder) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = localizedString(isArabic, R.string.search_location_note, R.string.search_location_note_ar),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        val query = searchQuery.trim()
        val profiles = if (query.isEmpty()) {
            state.visibleProfiles
        } else {
            state.visibleProfiles.filter {
                it.displayName.contains(query, ignoreCase = true) ||
                    it.city.contains(query, ignoreCase = true) ||
                    it.country.contains(query, ignoreCase = true)
            }
        }
        SearchResultsContent(
            state = state,
            profiles = profiles,
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
            onOpenDetail = { profile -> detailProfile = profile },
            onRetry = viewModel::loadProfiles
        )
    }
}

@Composable
private fun SearchResultsContent(
    state: DiscoverUiState,
    profiles: List<PublicProfile>,
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
    onOpenDetail: (PublicProfile) -> Unit = {},
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
        state.errorMessage != null -> {
            MithaqEmptyState(
                title = localizedString(isArabic, R.string.search_load_error_title, R.string.search_load_error_title_ar),
                message = localizedString(isArabic, R.string.search_load_error_message, R.string.search_load_error_message_ar),
                icon = Icons.Filled.Refresh,
                actionLabel = localizedString(isArabic, R.string.common_retry, R.string.common_retry_ar),
                onAction = onRetry
            )
        }
        state.isEmpty -> {
            MithaqEmptyState(
                title = localizedString(isArabic, R.string.search_no_profiles_title, R.string.search_no_profiles_title_ar),
                message = localizedString(isArabic, R.string.search_no_profiles_message, R.string.search_no_profiles_message_ar),
                icon = Icons.Filled.Search
            )
        }
        profiles.isEmpty() -> {
            // Covers both an empty chip-filter result and a text query with no hits.
            MithaqEmptyState(
                title = localizedString(isArabic, R.string.search_no_matching_title, R.string.search_no_matching_title_ar),
                message = localizedString(isArabic, R.string.search_no_matching_message, R.string.search_no_matching_message_ar),
                icon = Icons.Filled.Search
            )
        }
        else -> {
            profiles.forEach { profile ->
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
                    onOpenDetail = { onOpenDetail(profile) },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}
