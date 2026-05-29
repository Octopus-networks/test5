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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mithaq.app.domain.model.ChatMessage
import com.mithaq.app.domain.model.ChatParticipantSummary
import com.mithaq.app.domain.model.ChatRoom
import com.mithaq.app.ui.components.MithaqEmptyState
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun MithaqMessagesScreen(
    currentUserId: String,
    isArabic: Boolean,
    modifier: Modifier = Modifier,
    viewModel: ChatRoomsViewModel = viewModel(key = "mithaq_chat_rooms")
) {
    val tabs = if (isArabic) {
        listOf("الطلبات", "المحادثات", "الأرشيف")
    } else {
        listOf("Requests", "Active chats", "Archived")
    }
    var selectedTab by remember { mutableIntStateOf(1) }
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
                text = if (isArabic) "الرسائل" else "Messages",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isArabic) "المحادثات تظهر بعد قبول طلب التواصل فقط."
                else "Chats appear only after a respectful request is accepted.",
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
                title = if (isArabic) "لا توجد رسائل هنا" else "No messages here yet",
                message = if (isArabic) "طلبات التواصل تدار من شاشة الطلبات، والرسائل الفعلية ستضاف لاحقا."
                else "Chat requests are managed in Requests, and real messaging will be added later.",
                icon = Icons.Filled.Chat,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }
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
                title = if (isArabic) "تعذر تحميل المحادثات" else "Could not load chats",
                message = state.errorMessage,
                icon = Icons.Filled.Refresh,
                actionLabel = if (isArabic) "إعادة المحاولة" else "Retry",
                onAction = onRetry,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }
        state.chatRooms.isEmpty() -> {
            MithaqEmptyState(
                title = if (isArabic) "لا توجد محادثات نشطة" else "No active chats",
                message = if (isArabic) "بعد قبول طلب التواصل ستظهر المحادثة هنا بدون كشف أي بيانات خاصة."
                else "After a chat request is approved, the conversation will appear here without exposing private data.",
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
                text = room.lastMessagePreview ?: if (isArabic) {
                    "لا توجد رسائل بعد"
                } else {
                    "No messages yet"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onOpenChat,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (isArabic) {
                        "فتح المحادثة"
                    } else {
                        "Open chat"
                    }
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
    viewModel: ChatMessageViewModel = viewModel(key = "mithaq_messages_${room.chatId}")
) {
    val state by viewModel.state.collectAsState()
    var draft by remember(room.chatId) { mutableStateOf("") }
    val otherUserId = room.participantIds.firstOrNull { it != currentUserId }.orEmpty()
    val summary = room.participantPublicSummaries[otherUserId] ?: ChatParticipantSummary(userId = otherUserId)

    LaunchedEffect(room.chatId) {
        viewModel.loadMessages(room.chatId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        TextButton(onClick = onBack) {
            Text(if (isArabic) "رجوع" else "Back")
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
            text = if (isArabic) "محادثة نصية محترمة داخل طلب تواصل مقبول فقط."
            else "Respectful text conversation inside an approved active chat only.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
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
            Spacer(modifier = Modifier.height(12.dp))
        }

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
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
                        title = if (isArabic) "ابدأ محادثة محترمة" else "Start a respectful conversation",
                        message = if (isArabic) "اكتب رسالة نصية واضحة ومهذبة."
                        else "Write a clear and considerate text message.",
                        icon = Icons.Filled.Chat
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.messages.forEach { message ->
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
                placeholder = { Text(if (isArabic) "اكتب رسالة" else "Write a message") },
                enabled = !state.isSending && room.status == "active",
                singleLine = false,
                maxLines = 4
            )
            Button(
                onClick = {
                    val text = draft
                    draft = ""
                    viewModel.sendTextMessage(room.chatId, currentUserId, text)
                },
                enabled = draft.isNotBlank() && !state.isSending && room.status == "active"
            ) {
                Icon(Icons.Filled.Send, contentDescription = null)
            }
        }
    }
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
                text = if (isArabic) "المحادثة ستكون متاحة في المرحلة القادمة."
                else "Conversation will be available in the next phase.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onDismiss) {
                Text(if (isArabic) "حسنًا" else "OK")
            }
        }
    }
}

private fun ChatParticipantSummary.displayTitle(isArabic: Boolean): String {
    val fallback = if (isArabic) "عضو ميثاق" else "Mithaq member"
    val name = displayName.ifBlank { fallback }
    return age?.let { "$name, $it" } ?: name
}
