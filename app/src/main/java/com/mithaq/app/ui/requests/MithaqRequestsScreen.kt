package com.mithaq.app.ui.requests

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
fun MithaqRequestsScreen(
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val tabs = if (isArabic) {
        listOf("طلبات الاهتمام", "طلبات الصور", "طلبات المحادثة", "طلبات الولي")
    } else {
        listOf("Interest requests", "Photo requests", "Chat requests", "Guardian requests")
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
                text = if (isArabic) "الطلبات" else "Requests",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isArabic) "كل طلب واضح ومفصول حتى يبقى التواصل محترمًا."
                else "Each request type is separated so contact stays clear and respectful.",
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
        MithaqEmptyState(
            title = if (isArabic) "لا توجد ${tabs[selectedTab]} الآن" else "No ${tabs[selectedTab].lowercase()} yet",
            message = if (isArabic) "عندما تصلك طلبات جديدة ستظهر هنا مع خطوات واضحة."
            else "New requests will appear here with clear next steps.",
            icon = Icons.Filled.Favorite,
            modifier = Modifier.padding(horizontal = 18.dp)
        )
    }
}
