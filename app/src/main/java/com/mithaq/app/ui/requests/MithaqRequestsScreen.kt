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
                },
                onCancel = { requestId ->
                    interestRequestViewModel.cancelInterest(currentUserId, requestId)
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
                title = if (isArabic) "تعذر تحميل طلبات الاهتمام" else "Could not load interest requests",
                message = state.errorMessage,
                icon = Icons.Filled.Refresh,
                actionLabel = if (isArabic) "إعادة المحاولة" else "Retry",
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

                InterestSectionTitle(if (isArabic) "طلبات واردة معلقة" else "Received pending")
                if (state.receivedPendingRequests.isEmpty()) {
                    MithaqEmptyState(
                        title = if (isArabic) "لا توجد طلبات اهتمام واردة" else "No received interest requests",
                        message = if (isArabic) "عندما يرسل لك عضو اهتمامًا جادًا سيظهر هنا."
                        else "When someone sends a serious interest request, it will appear here.",
                        icon = Icons.Filled.Favorite
                    )
                } else {
                    state.receivedPendingRequests.forEach { request ->
                        InterestRequestCard(
                            request = request,
                            publicProfile = state.publicProfilesByUserId[request.fromUserId],
                            isArabic = isArabic,
                            isResponding = request.requestId in state.respondingRequestIds,
                            onAccept = { onRespond(request.requestId, true) },
                            onDecline = { onRespond(request.requestId, false) }
                        )
                    }
                }

                InterestSectionTitle(if (isArabic) "طلبات أرسلتها" else "Sent requests")
                if (state.sentRequests.isEmpty()) {
                    MithaqEmptyState(
                        title = if (isArabic) "لا توجد طلبات مرسلة" else "No sent interest requests",
                        message = if (isArabic) "عند إرسال اهتمام سيظهر هنا مع حالته."
                        else "Sent interest requests will appear here with their status.",
                        icon = Icons.Filled.Favorite
                    )
                } else {
                    state.sentRequests.forEach { request ->
                        SentInterestRequestCard(
                            request = request,
                            recipientProfile = state.publicProfilesByUserId[request.toUserId],
                            isArabic = isArabic,
                            isCancelling = request.requestId in state.cancellingRequestIds,
                            onCancel = { onCancel(request.requestId) }
                        )
                    }
                }

                InterestSectionTitle(if (isArabic) "سجل الطلبات الواردة" else "Received history")
                if (state.receivedHistoryRequests.isEmpty()) {
                    MithaqEmptyState(
                        title = if (isArabic) "لا يوجد سجل طلبات" else "No request history",
                        message = if (isArabic) "الطلبات المقبولة أو المرفوضة ستظهر هنا."
                        else "Accepted or declined received requests will appear here.",
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
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val displayName = publicProfile?.displayName ?: request.fromDisplayName
    val safeName = displayName.ifBlank { if (isArabic) "عضو ميثاق" else "Mithaq member" }
    val location = publicProfile.locationLabel()

    InterestStatusCard(
        title = if (isArabic) "طلب اهتمام جديد" else "New interest request",
        name = safeName,
        location = location,
        status = request.status,
        isArabic = isArabic
    ) {
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

@Composable
private fun SentInterestRequestCard(
    request: InterestRequest,
    recipientProfile: PublicProfile?,
    isArabic: Boolean,
    isCancelling: Boolean,
    onCancel: () -> Unit
) {
    val displayName = recipientProfile?.displayName ?: request.toDisplayName
    InterestStatusCard(
        title = if (isArabic) "طلب مرسل" else "Sent request",
        name = displayName.ifBlank { if (isArabic) "عضو ميثاق" else "Mithaq member" },
        location = recipientProfile.locationLabel(),
        status = request.status,
        isArabic = isArabic
    ) {
        if (request.status == "pending") {
            OutlinedButton(
                onClick = onCancel,
                enabled = !isCancelling,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isArabic) "إلغاء الطلب" else "Cancel request")
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
        title = if (isArabic) "طلب وارد" else "Received request",
        name = displayName.ifBlank { if (isArabic) "عضو ميثاق" else "Mithaq member" },
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

private fun statusLabel(status: String, isArabic: Boolean): String {
    return when (status) {
        "pending" -> if (isArabic) "معلق" else "Pending"
        "accepted" -> if (isArabic) "مقبول" else "Accepted"
        "declined" -> if (isArabic) "مرفوض" else "Declined"
        "cancelled" -> if (isArabic) "ملغي" else "Cancelled"
        else -> status
    }
}
