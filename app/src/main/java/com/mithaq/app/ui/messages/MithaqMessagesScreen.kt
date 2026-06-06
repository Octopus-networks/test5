package com.mithaq.app.ui.messages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mithaq.app.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mithaq.app.domain.model.ChatMessage
import com.mithaq.app.domain.model.ChatParticipantSummary
import com.mithaq.app.domain.model.ChatRoom
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import com.mithaq.app.ui.components.MithaqEmptyState
import com.mithaq.app.ui.components.MithaqStateIllustration
import com.mithaq.app.ui.components.MithaqIllustrationType
import com.mithaq.app.ui.components.MithaqLoadingSkeleton
import com.mithaq.app.ui.components.SkeletonType
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
private fun localizedString(isArabic: Boolean, englishResId: Int, arabicResId: Int): String =
    stringResource(id = if (isArabic) arabicResId else englishResId)

@Composable
fun MithaqMessagesScreen(
    currentUserId: String,
    isArabic: Boolean,
    modifier: Modifier = Modifier,
    viewModel: ChatRoomsViewModel = viewModel(key = "mithaq_chat_rooms_$currentUserId")
) {
    val tabs = listOf(
        localizedString(isArabic, R.string.chat_tab_requests, R.string.chat_tab_requests_ar),
        localizedString(isArabic, R.string.chat_tab_active_chats, R.string.chat_tab_active_chats_ar),
        localizedString(isArabic, R.string.chat_tab_archived, R.string.chat_tab_archived_ar)
    )
    var selectedTab by remember(currentUserId) { mutableIntStateOf(1) }
    val state by viewModel.state.collectAsState()
    val selectedRoom = state.chatRooms.firstOrNull { it.chatId == state.selectedChatId }

    LaunchedEffect(currentUserId) {
        viewModel.loadChatRooms(currentUserId)
    }

    if (selectedRoom != null) {
        ChatScreen(
            room = selectedRoom,
            currentUserId = currentUserId,
            isArabic = isArabic,
            onBack = {
                viewModel.closeChat()
                viewModel.loadChatRooms(currentUserId)
            },
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 18.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
            Text(
                text = localizedString(isArabic, R.string.messages_title, R.string.messages_title_ar),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = localizedString(isArabic, R.string.messages_subtitle, R.string.messages_subtitle_ar),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        TabRow(
            selectedTabIndex = selectedTab,
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
            1 -> ActiveChatsTab(
                currentUserId = currentUserId,
                isArabic = isArabic,
                state = state,
                onRetry = { viewModel.loadChatRooms(currentUserId) },
                onOpenChat = viewModel::openChat
            )
            else -> MithaqEmptyState(
                title = localizedString(isArabic, R.string.messages_empty_title, R.string.messages_empty_title_ar),
                message = localizedString(isArabic, R.string.messages_empty_message, R.string.messages_empty_message_ar),
                icon = Icons.Filled.Chat,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }
    }
}

private fun ChatMessage.stableMessageKey(): String {
    return messageId.ifBlank {
        listOf(
            chatId,
            senderId,
            createdAt?.time ?: updatedAt?.time ?: text.hashCode()
        ).joinToString("_")
    }
}

@Composable
private fun ActiveChatsTab(
    currentUserId: String,
    isArabic: Boolean,
    state: ChatRoomsUiState,
    onRetry: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    when {
        state.isLoading -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) {
                    MithaqLoadingSkeleton(type = SkeletonType.MESSAGE_ROW)
                }
            }
        }
        state.errorMessage != null -> {
            MithaqEmptyState(
                title = localizedString(isArabic, R.string.chat_load_error_title, R.string.chat_load_error_title_ar),
                message = state.errorMessage,
                icon = Icons.Filled.Refresh,
                actionLabel = localizedString(isArabic, R.string.common_retry, R.string.common_retry_ar),
                onAction = onRetry,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }
        state.chatRooms.isEmpty() -> {
            MithaqEmptyState(
                title = localizedString(isArabic, R.string.chat_no_active_title, R.string.chat_no_active_title_ar),
                message = localizedString(isArabic, R.string.chat_no_active_message, R.string.chat_no_active_message_ar),
                icon = Icons.Filled.Chat,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }
        else -> {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.chatRooms.forEach { room ->
                    ChatRoomCard(
                        room = room,
                        currentUserId = currentUserId,
                        isArabic = isArabic,
                        onOpenChat = { onOpenChat(room.chatId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatRoomCard(
    room: ChatRoom,
    currentUserId: String,
    isArabic: Boolean,
    onOpenChat: () -> Unit
) {
    val otherUserId = room.participantIds.firstOrNull { it != currentUserId }.orEmpty()
    val summary = room.participantPublicSummaries[otherUserId] ?: ChatParticipantSummary(userId = otherUserId)
    val location = listOf(summary.city, summary.country)
        .filter { it.isNotBlank() }
        .joinToString(", ")
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.displayTitle(isArabic),
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
                }
                Text(
                    text = room.status.replaceFirstChar { it.titlecase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = room.lastMessagePreview ?: localizedString(isArabic, R.string.chat_no_messages_yet, R.string.chat_no_messages_yet_ar),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onOpenChat,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    localizedString(isArabic, R.string.chat_open, R.string.chat_open_ar)
                )
            }
        }
    }
}

@Composable
private fun ChatScreen(
    room: ChatRoom,
    currentUserId: String,
    isArabic: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatMessageViewModel = viewModel(key = "mithaq_messages_${currentUserId}_${room.chatId}"),
    safetyViewModel: ChatSafetyViewModel = viewModel(key = "mithaq_chat_safety_${currentUserId}_${room.chatId}")
) {
    val state by viewModel.state.collectAsState()
    val safetyState by safetyViewModel.state.collectAsState()
    var draft by remember(room.chatId) { mutableStateOf("") }
    var showReportDialog by remember(room.chatId) { mutableStateOf(false) }
    var showBlockDialog by remember(room.chatId) { mutableStateOf(false) }
    var hasAutoScrolledInitial by remember(room.chatId) { mutableStateOf(false) }
    val messageListState = rememberLazyListState()
    val otherUserId = room.participantIds.firstOrNull { it != currentUserId }.orEmpty()
    val summary = room.participantPublicSummaries[otherUserId] ?: ChatParticipantSummary(userId = otherUserId)
    val canSend = room.status == "active" && !safetyState.isBlocked && !state.isSending && !safetyState.isCheckingBlock
    val isNearBottom by remember {
        derivedStateOf {
            val lastVisibleIndex = messageListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisibleIndex >= state.messages.lastIndex - 2
        }
    }

    LaunchedEffect(room.chatId, currentUserId, otherUserId) {
        viewModel.listenToMessages(room.chatId)
        safetyViewModel.loadBlockState(currentUserId, otherUserId)
    }
    DisposableEffect(room.chatId) {
        onDispose { viewModel.stopListening() }
    }
    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.messageId) {
        if (state.messages.isNotEmpty()) {
            val lastMessage = state.messages.last()
            val shouldScroll = !hasAutoScrolledInitial ||
                isNearBottom ||
                lastMessage.senderId == currentUserId
            if (shouldScroll) {
                messageListState.animateScrollToItem(state.messages.lastIndex)
                hasAutoScrolledInitial = true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(18.dp)
    ) {
        TextButton(onClick = onBack) {
            Text(localizedString(isArabic, R.string.common_back, R.string.common_back_ar))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = summary.displayTitle(isArabic),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = localizedString(isArabic, R.string.chat_screen_subtitle, R.string.chat_screen_subtitle_ar),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showReportDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Text(localizedString(isArabic, R.string.chat_report, R.string.chat_report_ar))
            }
            OutlinedButton(
                onClick = { showBlockDialog = true },
                modifier = Modifier.weight(1f),
                enabled = !safetyState.isBlocked && !safetyState.isBlocking
            ) {
                Text(localizedString(isArabic, R.string.chat_block, R.string.chat_block_ar))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        safetyState.message?.let { message ->
            InfoCard(text = message, isError = false)
            Spacer(modifier = Modifier.height(12.dp))
        }
        safetyState.errorMessage?.let { error ->
            InfoCard(text = error, isError = true)
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (safetyState.isBlocked) {
            InfoCard(
                text = localizedString(isArabic, R.string.chat_messaging_unavailable, R.string.chat_messaging_unavailable_ar),
                isError = false
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        state.errorMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            OutlinedButton(
                onClick = { viewModel.listenToMessages(room.chatId) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(localizedString(isArabic, R.string.common_retry, R.string.common_retry_ar))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repeat(4) {
                        MithaqLoadingSkeleton(type = SkeletonType.MESSAGE_ROW)
                    }
                }
            }
            state.messages.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    MithaqEmptyState(
                        title = localizedString(isArabic, R.string.chat_start_title, R.string.chat_start_title_ar),
                        message = localizedString(isArabic, R.string.chat_start_message, R.string.chat_start_message_ar),
                        icon = Icons.Filled.Chat
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    state = messageListState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = state.messages,
                        key = { message -> message.stableMessageKey() }
                    ) { message ->
                        ChatMessageBubble(
                            message = message,
                            isMine = message.senderId == currentUserId
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = draft,
                onValueChange = { if (it.length <= 1000) draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(localizedString(isArabic, R.string.chat_write_message, R.string.chat_write_message_ar)) },
                enabled = canSend,
                singleLine = false,
                maxLines = 4
            )
            Button(
                onClick = {
                    val text = draft
                    draft = ""
                    viewModel.sendTextMessage(room.chatId, currentUserId, text)
                },
                enabled = draft.isNotBlank() && canSend
            ) {
                Icon(Icons.Filled.Send, contentDescription = null)
            }
        }
        if (draft.length >= 850) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${draft.length}/1000",
                modifier = Modifier.align(Alignment.End),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showReportDialog) {
        ReportUserDialog(
            isArabic = isArabic,
            isSubmitting = safetyState.isSubmittingReport,
            onDismiss = {
                showReportDialog = false
                safetyViewModel.clearMessages()
            },
            onSubmit = { reason, details ->
                safetyViewModel.reportUser(currentUserId, otherUserId, room.chatId, reason, details)
                showReportDialog = false
            }
        )
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text(localizedString(isArabic, R.string.chat_block_dialog_title, R.string.chat_block_dialog_title_ar)) },
            text = { Text(localizedString(isArabic, R.string.chat_block_dialog_message, R.string.chat_block_dialog_message_ar)) },
            confirmButton = {
                Button(
                    onClick = {
                        safetyViewModel.blockUser(currentUserId, otherUserId, room.chatId)
                        showBlockDialog = false
                    },
                    enabled = !safetyState.isBlocking
                ) {
                    Text(localizedString(isArabic, R.string.chat_block, R.string.chat_block_ar))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) {
                    Text(localizedString(isArabic, R.string.common_cancel, R.string.common_cancel_ar))
                }
            }
        )
    }
}

@Composable
private fun InfoCard(
    text: String,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val softGold = Color(0xFFF2CA50)
    val softEmerald = Color(0xFF8BD6B6)
    val softRed = Color(0xFFE57373)

    val isSuccess = text.contains("نجاح", ignoreCase = true) || 
                    text.contains("success", ignoreCase = true) || 
                    text.contains("تم إرسال", ignoreCase = true) ||
                    text.contains("submitted", ignoreCase = true)

    val accentColor = when {
        isError -> softRed
        isSuccess -> softEmerald
        else -> softGold
    }

    val illustrationType = when {
        isError -> MithaqIllustrationType.ALERT_GEOMETRIC
        isSuccess -> MithaqIllustrationType.CHECK_GEOMETRIC
        else -> MithaqIllustrationType.ALERT_GEOMETRIC
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.24f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MithaqStateIllustration(
                type = illustrationType,
                tint = accentColor,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ReportUserDialog(
    isArabic: Boolean,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    val reasons = listOf(
        localizedString(isArabic, R.string.chat_report_reason_inappropriate, R.string.chat_report_reason_inappropriate_ar),
        localizedString(isArabic, R.string.chat_report_reason_harassment, R.string.chat_report_reason_harassment_ar),
        localizedString(isArabic, R.string.chat_report_reason_fake_profile, R.string.chat_report_reason_fake_profile_ar),
        localizedString(isArabic, R.string.chat_report_reason_privacy, R.string.chat_report_reason_privacy_ar),
        localizedString(isArabic, R.string.chat_report_reason_other, R.string.chat_report_reason_other_ar)
    )
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(localizedString(isArabic, R.string.chat_report_dialog_title, R.string.chat_report_dialog_title_ar)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = localizedString(isArabic, R.string.chat_report_dialog_message, R.string.chat_report_dialog_message_ar),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                reasons.forEach { reason ->
                    OutlinedButton(
                        onClick = { selectedReason = reason },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (selectedReason == reason) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                            }
                        )
                    ) {
                        Text(reason)
                    }
                }
                TextField(
                    value = details,
                    onValueChange = { details = it.take(500) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(localizedString(isArabic, R.string.chat_report_optional_details, R.string.chat_report_optional_details_ar)) },
                    minLines = 3,
                    maxLines = 5
                )
                Text(
                    text = "${details.length}/500",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedReason, details) },
                enabled = !isSubmitting && selectedReason.isNotBlank()
            ) {
                Text(localizedString(isArabic, R.string.chat_submit_report, R.string.chat_submit_report_ar))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(localizedString(isArabic, R.string.common_cancel, R.string.common_cancel_ar))
            }
        }
    )
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    isMine: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (isMine) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isMine) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            message.createdAt?.let { createdAt ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMine) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                    }
                )
            }
        }
    }
}

@Composable
private fun ConversationPlaceholderCard(
    isArabic: Boolean,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = localizedString(isArabic, R.string.chat_conversation_next_phase, R.string.chat_conversation_next_phase_ar),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onDismiss) {
                Text(localizedString(isArabic, R.string.common_ok, R.string.common_ok_ar))
            }
        }
    }
}

@Composable
private fun ChatParticipantSummary.displayTitle(isArabic: Boolean): String {
    val fallback = localizedString(isArabic, R.string.chat_member_fallback, R.string.chat_member_fallback_ar)
    val name = displayName.ifBlank { fallback }
    return age?.let { "$name, $it" } ?: name
}
