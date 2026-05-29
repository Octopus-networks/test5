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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mithaq.app.domain.model.InterestRequest
import com.mithaq.app.domain.model.PublicProfile
import com.mithaq.app.ui.components.MithaqEmptyState

@Composable
fun MithaqRequestsScreen(
    currentUserId: String,
    isArabic: Boolean,
    interestRequestViewModel: InterestRequestViewModel,
    modifier: Modifier = Modifier
) {
    val tabs = if (isArabic) {
        listOf("طلبات الاهتمام", "طلبات الصور", "طلبات المحادثة", "طلبات الولي")
    } else {
        listOf("Interest requests", "Photo requests", "Chat requests", "Guardian requests")
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    val interestState by interestRequestViewModel.state.collectAsState()

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
        if (selectedTab == 0) {
            InterestRequestsTab(
                currentUserId = currentUserId,
                isArabic = isArabic,
                state = interestState,
                onRetry = { interestRequestViewModel.loadForUser(currentUserId) },
                onRespond = { requestId, accepted ->
                    interestRequestViewModel.respondToInterest(currentUserId, requestId, accepted)
                }
            )
        } else {
            MithaqEmptyState(
                title = if (isArabic) "لا توجد ${tabs[selectedTab]} الآن" else "No ${tabs[selectedTab].lowercase()} yet",
                message = if (isArabic) "عندما تصلك طلبات جديدة ستظهر هنا مع خطوات واضحة."
                else "New requests will appear here with clear next steps.",
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
    onRespond: (String, Boolean) -> Unit
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
                title = if (isArabic) "تعذر تحميل طلبات الاهتمام" else "Could not load interest requests",
                message = state.errorMessage,
                icon = Icons.Filled.Refresh,
                actionLabel = if (isArabic) "إعادة المحاولة" else "Retry",
                onAction = onRetry,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }
        state.receivedPendingRequests.isEmpty() -> {
            MithaqEmptyState(
                title = if (isArabic) "لا توجد طلبات اهتمام الآن" else "No interest requests yet",
                message = if (isArabic) "عندما يرسل لك عضو اهتمامًا جادًا سيظهر هنا."
                else "When someone sends a serious interest request, it will appear here.",
                icon = Icons.Filled.Favorite,
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
                state.receivedPendingRequests.forEach { request ->
                    InterestRequestCard(
                        request = request,
                        senderProfile = state.senderPublicProfiles[request.fromUserId],
                        isArabic = isArabic,
                        isResponding = request.requestId in state.respondingRequestIds,
                        onAccept = { onRespond(request.requestId, true) },
                        onDecline = { onRespond(request.requestId, false) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InterestRequestCard(
    request: InterestRequest,
    senderProfile: PublicProfile?,
    isArabic: Boolean,
    isResponding: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val displayName = senderProfile?.displayName
        ?: request.fromDisplayName
        ?: ""
    val safeName = displayName.ifBlank { if (isArabic) "عضو ميثاق" else "Mithaq member" }
    val location = listOf(senderProfile?.city.orEmpty(), senderProfile?.country.orEmpty())
        .filter { it.isNotBlank() }
        .joinToString(", ")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isArabic) "طلب اهتمام جديد" else "New interest request",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = safeName,
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
            senderProfile?.let { profile ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listOf(profile.accountType, profile.maritalStatus)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onAccept,
                    enabled = !isResponding,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isArabic) "قبول" else "Accept")
                }
                OutlinedButton(
                    onClick = onDecline,
                    enabled = !isResponding,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isArabic) "رفض" else "Decline")
                }
            }
        }
    }
}
