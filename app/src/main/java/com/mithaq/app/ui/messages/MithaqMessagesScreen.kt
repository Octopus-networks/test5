package com.mithaq.app.ui.messages

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.mithaq.app.data.repository.ChatMessageRepository
import com.mithaq.app.data.repository.PhotoRepository
import com.mithaq.app.domain.model.PhotoAccessLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.mithaq.app.ui.components.MithaqEmptyState
import com.mithaq.app.ui.components.MithaqStateIllustration
import com.mithaq.app.ui.components.MithaqIllustrationType
import com.mithaq.app.ui.components.MithaqLoadingSkeleton
import com.mithaq.app.ui.components.SkeletonType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
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
    var showEmoji by remember(room.chatId) { mutableStateOf(false) }
    var showReportDialog by remember(room.chatId) { mutableStateOf(false) }
    var showBlockDialog by remember(room.chatId) { mutableStateOf(false) }
    var menuOpen by remember(room.chatId) { mutableStateOf(false) }
    var hasAutoScrolledInitial by remember(room.chatId) { mutableStateOf(false) }
    val messageListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.sendImageMessage(room.chatId, currentUserId, uri, context.contentResolver.getType(uri))
        }
    }
    val voiceRecorder = remember { ChatVoiceRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }
    var recordSeconds by remember { mutableIntStateOf(0) }
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && voiceRecorder.start()) {
            isRecording = true
        }
    }
    fun startRecording() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            if (voiceRecorder.start()) isRecording = true
        } else {
            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    fun sendRecording() {
        val result = voiceRecorder.stop()
        isRecording = false
        if (result != null) {
            viewModel.sendVoiceMessage(room.chatId, currentUserId, result.first, result.second)
        }
    }
    fun cancelRecording() {
        voiceRecorder.cancel()
        isRecording = false
    }
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordSeconds = 0
            while (isRecording) {
                kotlinx.coroutines.delay(1000L)
                recordSeconds += 1
            }
        }
    }
    DisposableEffect(Unit) { onDispose { voiceRecorder.cancel() } }
    val otherUserId = room.participantIds.firstOrNull { it != currentUserId }.orEmpty()
    val summary = room.participantPublicSummaries[otherUserId] ?: ChatParticipantSummary(userId = otherUserId)
    val canSend = room.status == "active" && !safetyState.isBlocked && !state.isSending && !safetyState.isCheckingBlock
    val isNearBottom by remember {
        derivedStateOf {
            val lastVisibleIndex = messageListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisibleIndex >= state.messages.lastIndex - 2
        }
    }
    // Trigger loading older history once the user scrolls to the very top of the list.
    val isAtTop by remember {
        derivedStateOf {
            messageListState.firstVisibleItemIndex == 0 &&
                messageListState.firstVisibleItemScrollOffset == 0
        }
    }

    LaunchedEffect(room.chatId, currentUserId, otherUserId) {
        viewModel.listenToMessages(room.chatId)
        safetyViewModel.loadBlockState(currentUserId, otherUserId)
    }
    LaunchedEffect(isAtTop, state.hasMoreOlder, state.isLoadingOlder, hasAutoScrolledInitial) {
        if (isAtTop && hasAutoScrolledInitial && state.hasMoreOlder &&
            !state.isLoadingOlder && state.messages.isNotEmpty()
        ) {
            viewModel.loadOlderMessages(room.chatId)
        }
    }
    DisposableEffect(room.chatId) {
        onDispose { viewModel.stopListening() }
    }
    // Keyed on the newest message id only (not list size) so prepending older history during
    // scroll-up pagination does not yank the view back to the bottom.
    LaunchedEffect(state.messages.lastOrNull()?.messageId) {
        if (state.messages.isNotEmpty()) {
            val lastMessage = state.messages.last()
            val isMine = lastMessage.senderId == currentUserId
            val shouldScroll = !hasAutoScrolledInitial || isNearBottom || isMine
            if (shouldScroll) {
                // Snap instantly on first open AND whenever I send (so my message is always
                // visible without manual scrolling); animate for incoming messages.
                if (!hasAutoScrolledInitial || isMine) {
                    messageListState.scrollToItem(state.messages.lastIndex)
                } else {
                    messageListState.animateScrollToItem(state.messages.lastIndex)
                }
                hasAutoScrolledInitial = true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        // Compact top bar: back, name, and an overflow menu holding Report / Block.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = localizedString(isArabic, R.string.common_back, R.string.common_back_ar)
                )
            }
            ChatPartnerAvatar(
                otherUserId = otherUserId,
                displayName = summary.displayName,
                photoPrivacyMode = summary.photoPrivacyMode
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = summary.displayTitle(isArabic),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(localizedString(isArabic, R.string.chat_report, R.string.chat_report_ar)) },
                        onClick = {
                            menuOpen = false
                            showReportDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(localizedString(isArabic, R.string.chat_block, R.string.chat_block_ar)) },
                        enabled = !safetyState.isBlocked && !safetyState.isBlocking,
                        onClick = {
                            menuOpen = false
                            showBlockDialog = true
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = localizedString(isArabic, R.string.chat_screen_subtitle, R.string.chat_screen_subtitle_ar),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = messageListState,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.isLoadingOlder) {
                            item(key = "older_messages_loading") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                        itemsIndexed(
                            items = state.messages,
                            key = { _, message -> message.stableMessageKey() }
                        ) { index, message ->
                            val previousAt = if (index > 0) state.messages[index - 1].createdAt else null
                            if (shouldShowDaySeparator(previousAt, message.createdAt)) {
                                ChatDaySeparator(date = message.createdAt)
                            }
                            ChatMessageBubble(
                                message = message,
                                isMine = message.senderId == currentUserId,
                                currentUserId = currentUserId,
                                onReact = { emoji -> viewModel.setReaction(room.chatId, message.messageId, emoji) }
                            )
                        }
                    }
                    // "Jump to latest" button: appears only when scrolled up from the bottom.
                    if (!isNearBottom) {
                        SmallFloatingActionButton(
                            onClick = {
                                coroutineScope.launch {
                                    messageListState.animateScrollToItem(state.messages.lastIndex)
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 4.dp, bottom = 4.dp),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                contentDescription = if (isArabic) "آخر رسالة" else "Scroll to latest"
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        if (isRecording) {
            VoiceRecordingBar(
                seconds = recordSeconds,
                canSend = canSend,
                onCancel = { cancelRecording() },
                onSend = { sendRecording() }
            )
        } else {
        if (showEmoji) {
            ChatEmojiPicker(
                enabled = canSend,
                onEmoji = { emoji -> if (draft.length + emoji.length <= 1000) draft += emoji }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                enabled = canSend
            ) {
                Text(text = "📷", style = MaterialTheme.typography.titleLarge)
            }
            IconButton(
                onClick = { showEmoji = !showEmoji },
                enabled = canSend
            ) {
                Text(text = "😊", style = MaterialTheme.typography.titleLarge)
            }
            TextField(
                value = draft,
                onValueChange = { if (it.length <= 1000) draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(localizedString(isArabic, R.string.chat_write_message, R.string.chat_write_message_ar)) },
                enabled = canSend,
                singleLine = false,
                maxLines = 4
            )
            if (draft.isNotBlank()) {
                Button(
                    onClick = {
                        val text = draft
                        draft = ""
                        showEmoji = false
                        viewModel.sendTextMessage(room.chatId, currentUserId, text)
                    },
                    enabled = canSend
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null)
                }
            } else {
                IconButton(
                    onClick = { startRecording() },
                    enabled = canSend
                ) {
                    Text(text = "🎤", style = MaterialTheme.typography.titleLarge)
                }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    currentUserId: String,
    onReact: (String?) -> Unit
) {
    var showReactionBar by remember(message.messageId) { mutableStateOf(false) }
    val myReaction = message.reactions[currentUserId]
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
            if (showReactionBar) {
                ReactionBar(
                    selected = myReaction,
                    onPick = { emoji ->
                        showReactionBar = false
                        onReact(if (emoji == myReaction) null else emoji)
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Column(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .background(
                        color = if (isMine) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = if (isMine) {
                            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
                        } else {
                            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
                        }
                    )
                    .combinedClickable(
                        onClick = { if (showReactionBar) showReactionBar = false },
                        onLongClick = { showReactionBar = !showReactionBar }
                    )
                    .padding(12.dp)
            ) {
                if (message.type == "image") {
                    ChatImageAttachment(message = message)
                    if (message.text.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                } else if (message.type == "voice") {
                    ChatVoiceAttachment(message = message, isMine = isMine)
                    if (message.text.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isMine) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
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
            if (message.reactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                ReactionChip(reactions = message.reactions)
            }
        }
    }
}

/** WhatsApp-style quick-reaction bar shown when a message bubble is long-pressed. */
@Composable
private fun ReactionBar(selected: String?, onPick: (String) -> Unit) {
    val quick = listOf("❤️", "👍", "😊", "🤲", "🌹", "🙏")
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            quick.forEach { emoji ->
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (emoji == selected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable { onPick(emoji) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/** Compact chip under a bubble showing its reactions, grouped by emoji with counts. */
@Composable
private fun ReactionChip(reactions: Map<String, String>) {
    val grouped = reactions.values.groupingBy { it }.eachCount()
    if (grouped.isEmpty()) return
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            grouped.forEach { (emoji, count) ->
                Text(
                    text = if (count > 1) "$emoji $count" else emoji,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
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

/**
 * Curated emoji panel with a love / engagement / marriage focus, opened from the chat
 * input. Tapping an emoji appends it to the draft text (handled by the caller). UI only —
 * emoji are inserted as Unicode text into the existing text message.
 */
@Composable
private fun ChatEmojiPicker(enabled: Boolean, onEmoji: (String) -> Unit) {
    val emojis = listOf(
        "❤️", "💖", "💕", "💗", "🌹", "💐", "💍", "🤍",
        "🤵", "👰", "👫", "💑", "🏡", "🤲", "🕌", "🌙",
        "😊", "🥰", "😍", "🙏", "💌", "✨", "🌸", "👪"
    )
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            emojis.chunked(8).forEach { rowEmojis ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowEmojis.forEach { emoji ->
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = enabled) { onEmoji(emoji) }
                                .padding(6.dp)
                        )
                    }
                }
            }
        }
    }
}

/** A centered date chip shown between messages from different days. */
@Composable
private fun ChatDaySeparator(date: Date?) {
    if (date == null) return
    val label = SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(date)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

private val chatDayKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

/** True when [current] starts a new calendar day relative to [previous]. */
private fun shouldShowDaySeparator(previous: Date?, current: Date?): Boolean {
    if (current == null) return false
    if (previous == null) return true
    return chatDayKeyFormat.format(previous) != chatDayKeyFormat.format(current)
}

/** Renders an image attachment in a chat bubble; bytes are fetched via the access-gated Storage path. */
@Composable
private fun ChatImageAttachment(message: ChatMessage) {
    var bitmap by remember(message.messageId) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(message.messageId) { mutableStateOf(false) }
    LaunchedEffect(message.storagePath) {
        bitmap = null
        failed = false
        if (message.storagePath.isBlank()) {
            failed = true
            return@LaunchedEffect
        }
        val loaded = withContext(Dispatchers.IO) {
            try {
                ChatMessageRepository().loadAttachmentBytes(message.storagePath)?.let { bytes ->
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }
        if (loaded != null) bitmap = loaded else failed = true
    }
    Box(
        modifier = Modifier
            .widthIn(max = 240.dp)
            .heightIn(min = 120.dp, max = 320.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        val current = bitmap
        when {
            current != null -> Image(
                bitmap = current,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth()
            )
            failed -> Text(text = "🖼️", style = MaterialTheme.typography.headlineMedium)
            else -> CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
        }
    }
}

/** Composer bar shown while recording a voice note: cancel, live timer, send. */
@Composable
private fun VoiceRecordingBar(
    seconds: Int,
    canSend: Boolean,
    onCancel: () -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCancel) {
            Text(text = "✕", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
        }
        Text(
            text = "● " + formatDuration((seconds * 1000).toLong()),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
        Button(onClick = onSend, enabled = canSend) {
            Icon(Icons.Filled.Send, contentDescription = null)
        }
    }
}

/** Voice-note bubble with play/stop + duration; bytes fetched via the access-gated Storage path. */
@Composable
private fun ChatVoiceAttachment(message: ChatMessage, isMine: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPlaying by remember(message.messageId) { mutableStateOf(false) }
    var loading by remember(message.messageId) { mutableStateOf(false) }
    val playerHolder = remember { mutableStateOf<MediaPlayer?>(null) }

    fun stopPlayback() {
        playerHolder.value?.let { mp ->
            try { if (mp.isPlaying) mp.stop() } catch (_: Exception) {}
            try { mp.release() } catch (_: Exception) {}
        }
        playerHolder.value = null
        isPlaying = false
    }

    DisposableEffect(message.messageId) { onDispose { stopPlayback() } }

    fun togglePlay() {
        if (isPlaying) {
            stopPlayback()
            return
        }
        loading = true
        scope.launch {
            val file = withContext(Dispatchers.IO) {
                try {
                    val bytes = ChatMessageRepository().loadAttachmentBytes(message.storagePath)
                        ?: return@withContext null
                    val f = File(context.cacheDir, "play_${message.messageId}.m4a")
                    f.writeBytes(bytes)
                    f
                } catch (e: Exception) {
                    null
                }
            }
            loading = false
            if (file == null) return@launch
            try {
                val mp = MediaPlayer()
                mp.setDataSource(file.absolutePath)
                mp.setOnCompletionListener {
                    stopPlayback()
                    file.delete()
                }
                mp.prepare()
                mp.start()
                playerHolder.value = mp
                isPlaying = true
            } catch (e: Exception) {
                isPlaying = false
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = { togglePlay() }) {
            when {
                loading -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                isPlaying -> Text(text = "⏸", style = MaterialTheme.typography.titleLarge)
                else -> Text(text = "▶️", style = MaterialTheme.typography.titleLarge)
            }
        }
        Text(
            text = "🎤 " + formatDuration(message.durationMs),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).toInt()
    val minutes = totalSec / 60
    val secs = totalSec % 60
    return "%d:%02d".format(minutes, secs)
}

/**
 * Circular avatar for the chat partner. Shows the partner's REAL account photo only when the
 * existing photo-access model grants FULL access (own photo or an approved photo request) —
 * Storage security rules are the real boundary, so an unauthorised viewer just gets the initial
 * fallback. No rules are weakened and no photo bytes are exposed without access.
 */
@Composable
private fun ChatPartnerAvatar(
    otherUserId: String,
    displayName: String,
    photoPrivacyMode: String,
    sizeDp: Int = 40
) {
    var avatar by remember(otherUserId) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(otherUserId, photoPrivacyMode) {
        avatar = null
        if (otherUserId.isBlank()) return@LaunchedEffect
        avatar = withContext(Dispatchers.IO) {
            try {
                val repo = PhotoRepository()
                if (repo.resolveAccessLevel(otherUserId, photoPrivacyMode) == PhotoAccessLevel.FULL) {
                    repo.loadAccessiblePhotoBytes(otherUserId)?.let { bytes ->
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        val current = avatar
        if (current != null) {
            Image(
                bitmap = current,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = displayName.trim().take(1).uppercase().ifBlank { "?" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
