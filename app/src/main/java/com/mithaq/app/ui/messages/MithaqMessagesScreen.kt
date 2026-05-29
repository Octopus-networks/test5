package com.mithaq.app.ui.messages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mithaq.app.ui.components.MithaqEmptyState

@Composable
fun MithaqMessagesScreen(
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val tabs = if (isArabic) {
        listOf("الطلبات", "المحادثات", "الأرشيف")
    } else {
        listOf("Requests", "Active chats", "Archived")
    }
    var selectedTab by remember { mutableIntStateOf(0) }

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
        MithaqEmptyState(
            title = if (isArabic) "لا توجد رسائل هنا" else "No messages here yet",
            message = if (isArabic) "عند قبول طلب محادثة، ستظهر المحادثة في هذا القسم."
            else "When a chat request is accepted, the conversation will appear in this section.",
            icon = Icons.Filled.Chat,
            modifier = Modifier.padding(horizontal = 18.dp)
        )
    }
}
