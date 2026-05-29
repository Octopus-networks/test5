package com.mithaq.app.ui.messages

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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mithaq.app.domain.model.ChatParticipantSummary
import com.mithaq.app.domain.model.ChatRoom
import com.mithaq.app.ui.components.MithaqEmptyState

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

    LaunchedEffect(currentUserId) {
        viewModel.loadChatRooms(currentUserId)
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
                onOpenPlaceholder = viewModel::showPlaceholder,
                onDismissPlaceholder = viewModel::clearPlaceholder
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
    onOpenPlaceholder: (String) -> Unit,
    onDismissPlaceholder: () -> Unit
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
                state.selectedPlaceholderChatId?.let {
                    ConversationPlaceholderCard(
                        isArabic = isArabic,
                        onDismiss = onDismissPlaceholder
                    )
                }
                state.chatRooms.forEach { room ->
                    ChatRoomCard(
                        room = room,
                        currentUserId = currentUserId,
                        isArabic = isArabic,
                        onOpenPlaceholder = { onOpenPlaceholder(room.chatId) }
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
    onOpenPlaceholder: () -> Unit
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
                onClick = onOpenPlaceholder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (isArabic) {
                        "عرض الحالة"
                    } else {
                        "View status"
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
