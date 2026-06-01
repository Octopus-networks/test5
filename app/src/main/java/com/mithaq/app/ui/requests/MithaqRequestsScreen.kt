package com.mithaq.app.ui.requests

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mithaq.app.R
import com.mithaq.app.domain.model.ChatRequest
import com.mithaq.app.domain.model.InterestRequest
import com.mithaq.app.domain.model.PhotoRequest
import com.mithaq.app.domain.model.PublicProfile
import com.mithaq.app.ui.components.MithaqEmptyState

@Composable
private fun localizedString(isArabic: Boolean, englishResId: Int, arabicResId: Int): String =
    stringResource(id = if (isArabic) arabicResId else englishResId)

@Composable
fun MithaqRequestsScreen(
    currentUserId: String,
    isArabic: Boolean,
    interestRequestViewModel: InterestRequestViewModel,
    photoRequestViewModel: PhotoRequestViewModel,
    chatRequestViewModel: ChatRequestViewModel,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        localizedString(isArabic, R.string.requests_tab_interest, R.string.requests_tab_interest_ar),
        localizedString(isArabic, R.string.requests_tab_photo, R.string.requests_tab_photo_ar),
        localizedString(isArabic, R.string.requests_tab_chat, R.string.requests_tab_chat_ar),
        localizedString(isArabic, R.string.requests_tab_guardian, R.string.requests_tab_guardian_ar)
    )
    var selectedTab by remember { mutableIntStateOf(0) }
    val interestState by interestRequestViewModel.state.collectAsState()
    val photoState by photoRequestViewModel.state.collectAsState()
    val chatState by chatRequestViewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 18.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
            Text(
                text = localizedString(isArabic, R.string.requests_title, R.string.requests_title_ar),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = localizedString(isArabic, R.string.requests_subtitle, R.string.requests_subtitle_ar),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 18.dp,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        when (selectedTab) {
            0 -> InterestRequestsTab(
                currentUserId = currentUserId,
                isArabic = isArabic,
                state = interestState,
                onRetry = { interestRequestViewModel.loadForUser(currentUserId) },
                onRespond = { requestId, accepted ->
                    interestRequestViewModel.respondToInterest(currentUserId, requestId, accepted)
                },
                onCancel = { requestId ->
                    interestRequestViewModel.cancelInterest(currentUserId, requestId)
                }
            )
            1 -> PhotoRequestsTab(
                currentUserId = currentUserId,
                isArabic = isArabic,
                state = photoState,
                onRetry = { photoRequestViewModel.loadForUser(currentUserId) },
                onRespond = { requestId, approved ->
                    photoRequestViewModel.respondToPhotoRequest(currentUserId, requestId, approved)
                },
                onCancel = { requestId ->
                    photoRequestViewModel.cancelPhotoRequest(currentUserId, requestId)
                }
            )
            2 -> ChatRequestsTab(
                currentUserId = currentUserId,
                isArabic = isArabic,
                state = chatState,
                onRetry = { chatRequestViewModel.loadForUser(currentUserId) },
                onRespond = { requestId, approved ->
                    chatRequestViewModel.respondToChatRequest(currentUserId, requestId, approved)
                },
                onCancel = { requestId ->
                    chatRequestViewModel.cancelChatRequest(currentUserId, requestId)
                }
            )
            else -> MithaqEmptyState(
                title = localizedString(isArabic, R.string.requests_empty_title, R.string.requests_empty_title_ar),
                message = localizedString(isArabic, R.string.requests_empty_message, R.string.requests_empty_message_ar),
                icon = Icons.Filled.Favorite,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }
    }
}

@Composable
private fun InterestRequestsTab(
    currentUserId: String,
    isArabic: Boolean,
    state: InterestRequestUiState,
    onRetry: () -> Unit,
    onRespond: (String, Boolean) -> Unit,
    onCancel: (String) -> Unit
) {
    when {
        state.isLoadingRequests -> {
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
                title = localizedString(isArabic, R.string.requests_interest_load_error, R.string.requests_interest_load_error_ar),
                message = state.errorMessage,
                icon = Icons.Filled.Refresh,
                actionLabel = localizedString(isArabic, R.string.common_retry, R.string.common_retry_ar),
                onAction = onRetry,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }
        else -> {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.message?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                InterestSectionTitle(localizedString(isArabic, R.string.requests_received_pending, R.string.requests_received_pending_ar))
                if (state.receivedPendingRequests.isEmpty()) {
                    MithaqEmptyState(
                        title = localizedString(isArabic, R.string.requests_no_received_interest, R.string.requests_no_received_interest_ar),
                        message = localizedString(isArabic, R.string.requests_no_received_interest_message, R.string.requests_no_received_interest_message_ar),
                        icon = Icons.Filled.Favorite
                    )
                } else {
                    state.receivedPendingRequests.forEach { request ->
                        InterestRequestCard(
                            request = request,
                            publicProfile = state.publicProfilesByUserId[request.fromUserId],
                            isArabic = isArabic,
                            isResponding = request.requestId in state.respondingRequestIds,
                            isBlocked = request.fromUserId in state.blockedUserIds,
                            onAccept = { onRespond(request.requestId, true) },
                            onDecline = { onRespond(request.requestId, false) }
                        )
                    }
                }

                InterestSectionTitle(localizedString(isArabic, R.string.requests_sent, R.string.requests_sent_ar))
                if (state.sentRequests.isEmpty()) {
                    MithaqEmptyState(
                        title = localizedString(isArabic, R.string.requests_no_sent_interest, R.string.requests_no_sent_interest_ar),
                        message = localizedString(isArabic, R.string.requests_no_sent_interest_message, R.string.requests_no_sent_interest_message_ar),
                        icon = Icons.Filled.Favorite
                    )
                } else {
                    state.sentRequests.forEach { request ->
                        SentInterestRequestCard(
                            request = request,
                            recipientProfile = state.publicProfilesByUserId[request.toUserId],
                            isArabic = isArabic,
                            isCancelling = request.requestId in state.cancellingRequestIds,
                            isBlocked = request.toUserId in state.blockedUserIds,
                            onCancel = { onCancel(request.requestId) }
                        )
                    }
                }

                InterestSectionTitle(localizedString(isArabic, R.string.requests_received_history, R.string.requests_received_history_ar))
                if (state.receivedHistoryRequests.isEmpty()) {
                    MithaqEmptyState(
                        title = localizedString(isArabic, R.string.requests_no_history, R.string.requests_no_history_ar),
                        message = localizedString(isArabic, R.string.requests_history_message, R.string.requests_history_message_ar),
                        icon = Icons.Filled.Favorite
                    )
                } else {
                    state.receivedHistoryRequests.forEach { request ->
                        InterestHistoryCard(
                            request = request,
                            publicProfile = state.publicProfilesByUserId[request.fromUserId],
                            isArabic = isArabic
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoRequestsTab(
    currentUserId: String,
    isArabic: Boolean,
    state: PhotoRequestUiState,
    onRetry: () -> Unit,
    onRespond: (String, Boolean) -> Unit,
    onCancel: (String) -> Unit
) {
    when {
        state.isLoadingRequests -> {
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
                title = localizedString(isArabic, R.string.requests_photo_load_error, R.string.requests_photo_load_error_ar),
                message = state.errorMessage,
                icon = Icons.Filled.Refresh,
                actionLabel = localizedString(isArabic, R.string.common_retry, R.string.common_retry_ar),
                onAction = onRetry,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }
        else -> {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.message?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                InterestSectionTitle(localizedString(isArabic, R.string.requests_photo_received_pending, R.string.requests_photo_received_pending_ar))
                if (state.receivedPendingRequests.isEmpty()) {
                    MithaqEmptyState(
                        title = localizedString(isArabic, R.string.requests_no_received_photo, R.string.requests_no_received_photo_ar),
                        message = localizedString(isArabic, R.string.requests_no_received_photo_message, R.string.requests_no_received_photo_message_ar),
                        icon = Icons.Filled.Favorite
                    )
                } else {
                    state.receivedPendingRequests.forEach { request ->
                        PhotoRequestCard(
                            request = request,
                            publicProfile = state.publicProfilesByUserId[request.fromUserId],
                            isArabic = isArabic,
                            isResponding = request.requestId in state.respondingRequestIds,
                            isBlocked = request.fromUserId in state.blockedUserIds,
                            onApprove = { onRespond(request.requestId, true) },
                            onDecline = { onRespond(request.requestId, false) }
                        )
                    }
                }

                InterestSectionTitle(localizedString(isArabic, R.string.requests_sent_photo, R.string.requests_sent_photo_ar))
                if (state.sentRequests.isEmpty()) {
                    MithaqEmptyState(
                        title = localizedString(isArabic, R.string.requests_no_sent_photo, R.string.requests_no_sent_photo_ar),
                        message = localizedString(isArabic, R.string.requests_no_sent_photo_message, R.string.requests_no_sent_photo_message_ar),
                        icon = Icons.Filled.Favorite
                    )
                } else {
                    state.sentRequests.forEach { request ->
                        SentPhotoRequestCard(
                            request = request,
                            recipientProfile = state.publicProfilesByUserId[request.toUserId],
                            isArabic = isArabic,
                            isCancelling = request.requestId in state.cancellingRequestIds,
                            isBlocked = request.toUserId in state.blockedUserIds,
                            onCancel = { onCancel(request.requestId) }
                        )
                    }
                }

                InterestSectionTitle(localizedString(isArabic, R.string.requests_photo_history, R.string.requests_photo_history_ar))
                if (state.receivedHistoryRequests.isEmpty()) {
                    MithaqEmptyState(
                        title = localizedString(isArabic, R.string.requests_no_photo_history, R.string.requests_no_photo_history_ar),
                        message = localizedString(isArabic, R.string.requests_photo_history_message, R.string.requests_photo_history_message_ar),
                        icon = Icons.Filled.Favorite
                    )
                } else {
                    state.receivedHistoryRequests.forEach { request ->
                        PhotoHistoryCard(
                            request = request,
                            publicProfile = state.publicProfilesByUserId[request.fromUserId],
                            isArabic = isArabic
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatRequestsTab(
    currentUserId: String,
    isArabic: Boolean,
    state: ChatRequestUiState,
    onRetry: () -> Unit,
    onRespond: (String, Boolean) -> Unit,
    onCancel: (String) -> Unit
) {
    when {
        state.isLoadingRequests -> {
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
                title = localizedString(isArabic, R.string.requests_chat_load_error, R.string.requests_chat_load_error_ar),
                message = state.errorMessage,
                icon = Icons.Filled.Refresh,
                actionLabel = localizedString(isArabic, R.string.common_retry, R.string.common_retry_ar),
                onAction = onRetry,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }
        else -> {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.message?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                InterestSectionTitle(localizedString(isArabic, R.string.requests_chat_received_pending, R.string.requests_chat_received_pending_ar))
                if (state.receivedPendingRequests.isEmpty()) {
                    MithaqEmptyState(
                        title = localizedString(isArabic, R.string.requests_no_received_chat, R.string.requests_no_received_chat_ar),
                        message = localizedString(isArabic, R.string.requests_no_received_chat_message, R.string.requests_no_received_chat_message_ar),
                        icon = Icons.Filled.Favorite
                    )
                } else {
                    state.receivedPendingRequests.forEach { request ->
                        ChatRequestCard(
                            request = request,
                            publicProfile = state.publicProfilesByUserId[request.fromUserId],
                            isArabic = isArabic,
                            isResponding = request.requestId in state.respondingRequestIds,
                            isBlocked = request.fromUserId in state.blockedUserIds,
                            onApprove = { onRespond(request.requestId, true) },
                            onDecline = { onRespond(request.requestId, false) }
                        )
                    }
                }

                InterestSectionTitle(localizedString(isArabic, R.string.requests_sent_chat, R.string.requests_sent_chat_ar))
                if (state.sentRequests.isEmpty()) {
                    MithaqEmptyState(
                        title = localizedString(isArabic, R.string.requests_no_sent_chat, R.string.requests_no_sent_chat_ar),
                        message = localizedString(isArabic, R.string.requests_no_sent_chat_message, R.string.requests_no_sent_chat_message_ar),
                        icon = Icons.Filled.Favorite
                    )
                } else {
                    state.sentRequests.forEach { request ->
                        SentChatRequestCard(
                            request = request,
                            recipientProfile = state.publicProfilesByUserId[request.toUserId],
                            isArabic = isArabic,
                            isCancelling = request.requestId in state.cancellingRequestIds,
                            isBlocked = request.toUserId in state.blockedUserIds,
                            onCancel = { onCancel(request.requestId) }
                        )
                    }
                }

                InterestSectionTitle(localizedString(isArabic, R.string.requests_chat_history, R.string.requests_chat_history_ar))
                if (state.receivedHistoryRequests.isEmpty()) {
                    MithaqEmptyState(
                        title = localizedString(isArabic, R.string.requests_no_chat_history, R.string.requests_no_chat_history_ar),
                        message = localizedString(isArabic, R.string.requests_chat_history_message, R.string.requests_chat_history_message_ar),
                        icon = Icons.Filled.Favorite
                    )
                } else {
                    state.receivedHistoryRequests.forEach { request ->
                        ChatHistoryCard(
                            request = request,
                            publicProfile = state.publicProfilesByUserId[request.fromUserId],
                            isArabic = isArabic
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InterestSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun InterestRequestCard(
    request: InterestRequest,
    publicProfile: PublicProfile?,
    isArabic: Boolean,
    isResponding: Boolean,
    isBlocked: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val displayName = publicProfile?.displayName ?: request.fromDisplayName
    val safeName = displayName.ifBlank { localizedString(isArabic, R.string.request_member_fallback, R.string.request_member_fallback_ar) }
    val location = publicProfile.locationLabel()

    InterestStatusCard(
        title = localizedString(isArabic, R.string.request_new_interest, R.string.request_new_interest_ar),
        name = safeName,
        location = location,
        status = request.status,
        isArabic = isArabic
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onAccept,
                enabled = !isResponding && !isBlocked,
                modifier = Modifier.weight(1f)
            ) {
                Text(localizedString(isArabic, R.string.request_accept, R.string.request_accept_ar))
            }
            OutlinedButton(
                onClick = onDecline,
                enabled = !isResponding && !isBlocked,
                modifier = Modifier.weight(1f)
            ) {
                Text(localizedString(isArabic, R.string.request_decline, R.string.request_decline_ar))
            }
        }
    }
}

@Composable
private fun SentInterestRequestCard(
    request: InterestRequest,
    recipientProfile: PublicProfile?,
    isArabic: Boolean,
    isCancelling: Boolean,
    isBlocked: Boolean,
    onCancel: () -> Unit
) {
    val displayName = recipientProfile?.displayName ?: request.toDisplayName
    InterestStatusCard(
        title = localizedString(isArabic, R.string.request_sent, R.string.request_sent_ar),
        name = displayName.ifBlank { localizedString(isArabic, R.string.request_member_fallback, R.string.request_member_fallback_ar) },
        location = recipientProfile.locationLabel(),
        status = request.status,
        isArabic = isArabic
    ) {
        if (request.status == "pending") {
            OutlinedButton(
                onClick = onCancel,
                enabled = !isCancelling && !isBlocked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(localizedString(isArabic, R.string.request_cancel, R.string.request_cancel_ar))
            }
        }
    }
}

@Composable
private fun InterestHistoryCard(
    request: InterestRequest,
    publicProfile: PublicProfile?,
    isArabic: Boolean
) {
    val displayName = publicProfile?.displayName ?: request.fromDisplayName
    InterestStatusCard(
        title = localizedString(isArabic, R.string.request_received, R.string.request_received_ar),
        name = displayName.ifBlank { localizedString(isArabic, R.string.request_member_fallback, R.string.request_member_fallback_ar) },
        location = publicProfile.locationLabel(),
        status = request.status,
        isArabic = isArabic
    )
}

@Composable
private fun PhotoRequestCard(
    request: PhotoRequest,
    publicProfile: PublicProfile?,
    isArabic: Boolean,
    isResponding: Boolean,
    isBlocked: Boolean,
    onApprove: () -> Unit,
    onDecline: () -> Unit
) {
    val displayName = publicProfile?.displayName.orEmpty()
    InterestStatusCard(
        title = localizedString(isArabic, R.string.request_photo_access, R.string.request_photo_access_ar),
        name = displayName.ifBlank { localizedString(isArabic, R.string.request_member_fallback, R.string.request_member_fallback_ar) },
        location = publicProfile.locationLabel(),
        status = request.status,
        isArabic = isArabic
    ) {
        Text(
            text = localizedString(isArabic, R.string.request_photo_approval_note, R.string.request_photo_approval_note_ar),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onApprove,
                enabled = !isResponding && !isBlocked,
                modifier = Modifier.weight(1f)
            ) {
                Text(localizedString(isArabic, R.string.request_approve, R.string.request_approve_ar))
            }
            OutlinedButton(
                onClick = onDecline,
                enabled = !isResponding && !isBlocked,
                modifier = Modifier.weight(1f)
            ) {
                Text(localizedString(isArabic, R.string.request_decline, R.string.request_decline_ar))
            }
        }
    }
}

@Composable
private fun SentPhotoRequestCard(
    request: PhotoRequest,
    recipientProfile: PublicProfile?,
    isArabic: Boolean,
    isCancelling: Boolean,
    isBlocked: Boolean,
    onCancel: () -> Unit
) {
    val displayName = recipientProfile?.displayName.orEmpty()
    InterestStatusCard(
        title = localizedString(isArabic, R.string.request_sent_photo, R.string.request_sent_photo_ar),
        name = displayName.ifBlank { localizedString(isArabic, R.string.request_member_fallback, R.string.request_member_fallback_ar) },
        location = recipientProfile.locationLabel(),
        status = request.status,
        isArabic = isArabic
    ) {
        if (request.status == "pending") {
            OutlinedButton(
                onClick = onCancel,
                enabled = !isCancelling && !isBlocked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(localizedString(isArabic, R.string.request_cancel_photo, R.string.request_cancel_photo_ar))
            }
        }
    }
}

@Composable
private fun PhotoHistoryCard(
    request: PhotoRequest,
    publicProfile: PublicProfile?,
    isArabic: Boolean
) {
    val displayName = publicProfile?.displayName.orEmpty()
    InterestStatusCard(
        title = localizedString(isArabic, R.string.request_received_photo, R.string.request_received_photo_ar),
        name = displayName.ifBlank { localizedString(isArabic, R.string.request_member_fallback, R.string.request_member_fallback_ar) },
        location = publicProfile.locationLabel(),
        status = request.status,
        isArabic = isArabic
    )
}

@Composable
private fun ChatRequestCard(
    request: ChatRequest,
    publicProfile: PublicProfile?,
    isArabic: Boolean,
    isResponding: Boolean,
    isBlocked: Boolean,
    onApprove: () -> Unit,
    onDecline: () -> Unit
) {
    val displayName = publicProfile?.displayName.orEmpty()
    InterestStatusCard(
        title = localizedString(isArabic, R.string.request_chat, R.string.request_chat_ar),
        name = displayName.ifBlank { localizedString(isArabic, R.string.request_member_fallback, R.string.request_member_fallback_ar) },
        location = publicProfile.locationLabel(),
        status = request.status,
        isArabic = isArabic
    ) {
        Text(
            text = localizedString(isArabic, R.string.request_chat_approval_note, R.string.request_chat_approval_note_ar),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onApprove,
                enabled = !isResponding && !isBlocked,
                modifier = Modifier.weight(1f)
            ) {
                Text(localizedString(isArabic, R.string.request_approve, R.string.request_approve_ar))
            }
            OutlinedButton(
                onClick = onDecline,
                enabled = !isResponding && !isBlocked,
                modifier = Modifier.weight(1f)
            ) {
                Text(localizedString(isArabic, R.string.request_decline, R.string.request_decline_ar))
            }
        }
    }
}

@Composable
private fun SentChatRequestCard(
    request: ChatRequest,
    recipientProfile: PublicProfile?,
    isArabic: Boolean,
    isCancelling: Boolean,
    isBlocked: Boolean,
    onCancel: () -> Unit
) {
    val displayName = recipientProfile?.displayName.orEmpty()
    InterestStatusCard(
        title = localizedString(isArabic, R.string.request_sent_chat, R.string.request_sent_chat_ar),
        name = displayName.ifBlank { localizedString(isArabic, R.string.request_member_fallback, R.string.request_member_fallback_ar) },
        location = recipientProfile.locationLabel(),
        status = request.status,
        isArabic = isArabic
    ) {
        if (request.status == "pending") {
            OutlinedButton(
                onClick = onCancel,
                enabled = !isCancelling && !isBlocked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(localizedString(isArabic, R.string.request_cancel_chat, R.string.request_cancel_chat_ar))
            }
        }
    }
}

@Composable
private fun ChatHistoryCard(
    request: ChatRequest,
    publicProfile: PublicProfile?,
    isArabic: Boolean
) {
    val displayName = publicProfile?.displayName.orEmpty()
    InterestStatusCard(
        title = localizedString(isArabic, R.string.request_received_chat, R.string.request_received_chat_ar),
        name = displayName.ifBlank { localizedString(isArabic, R.string.request_member_fallback, R.string.request_member_fallback_ar) },
        location = publicProfile.locationLabel(),
        status = request.status,
        isArabic = isArabic
    )
}

@Composable
private fun InterestStatusCard(
    title: String,
    name: String,
    location: String,
    status: String,
    isArabic: Boolean,
    actions: @Composable () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (location.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statusLabel(status, isArabic),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            actions()
        }
    }
}

private fun PublicProfile?.locationLabel(): String {
    if (this == null) return ""
    return listOf(city, country)
        .filter { it.isNotBlank() }
        .joinToString(", ")
}

@Composable
private fun statusLabel(status: String, isArabic: Boolean): String {
    return when (status) {
        "pending" -> localizedString(isArabic, R.string.request_status_pending, R.string.request_status_pending_ar)
        "accepted" -> localizedString(isArabic, R.string.request_status_accepted, R.string.request_status_accepted_ar)
        "approved" -> localizedString(isArabic, R.string.request_status_approved, R.string.request_status_approved_ar)
        "declined" -> localizedString(isArabic, R.string.request_status_declined, R.string.request_status_declined_ar)
        "cancelled" -> localizedString(isArabic, R.string.request_status_cancelled, R.string.request_status_cancelled_ar)
        else -> status
    }
}
