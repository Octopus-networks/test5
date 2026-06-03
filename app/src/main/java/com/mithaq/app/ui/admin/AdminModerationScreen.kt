package com.mithaq.app.ui.admin

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mithaq.app.domain.model.ModerationStatus
import com.mithaq.app.domain.model.PhotoStatus
import com.mithaq.app.domain.model.Report
import com.mithaq.app.domain.model.ReportStatus
import com.mithaq.app.domain.model.UserModeration
import com.mithaq.app.domain.model.UserPhoto

/**
 * Phase 12 — admin-only moderation surface. Renders a safe "Not authorized" state for non-admins and
 * never loads admin data unless authorized (gate enforced in [AdminModerationViewModel] + Firestore
 * rules). Compose talks only to the ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminModerationScreen(
    isAdmin: Boolean,
    isArabic: Boolean,
    onBack: () -> Unit,
    viewModel: AdminModerationViewModel = viewModel(key = "admin_moderation")
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(isAdmin) { viewModel.start(isAdmin) }
    var tab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "الإشراف والإدارة" else "Admin & Moderation",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        if (!state.authorized) {
            NotAuthorizedState(isArabic = isArabic, modifier = Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = {
                    Text(if (isArabic) "البلاغات (${state.reports.size})" else "Reports (${state.reports.size})")
                })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = {
                    Text(if (isArabic) "مراجعة الصور (${state.pendingPhotos.size})" else "Photos (${state.pendingPhotos.size})")
                })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = {
                    Text(if (isArabic) "إشراف الأعضاء" else "User mod")
                })
            }

            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            when (tab) {
                0 -> ReportsTab(state.reports, isArabic, viewModel)
                1 -> PhotosTab(state.pendingPhotos, isArabic, viewModel)
                2 -> ModerationTab(state.moderationEntries, isArabic)
            }
        }
    }
}

@Composable
private fun NotAuthorizedState(isArabic: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isArabic) "غير مصرّح لك بالوصول" else "Not authorized",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isArabic) "هذه الصفحة مخصّصة للمشرفين فقط." else "This area is available to admins only.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ReportsTab(
    reports: List<Report>,
    isArabic: Boolean,
    viewModel: AdminModerationViewModel
) {
    if (reports.isEmpty()) {
        EmptyState(if (isArabic) "لا توجد بلاغات مفتوحة" else "No open reports")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(reports, key = { it.reportId }) { report ->
            var note by remember(report.reportId) { mutableStateOf(report.adminNote) }
            ModCard {
                Text(
                    text = (if (isArabic) "السبب: " else "Reason: ") + report.reason,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = (if (isArabic) "المُبلَّغ عنه: " else "Reported: ") + report.reportedId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (report.details.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = report.details, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (isArabic) "ملاحظة المشرف" else "Admin note") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { viewModel.reviewReport(report.reportId, ReportStatus.REVIEWED, note) },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (isArabic) "روجِعت" else "Reviewed", maxLines = 1) }
                    OutlinedButton(
                        onClick = { viewModel.reviewReport(report.reportId, ReportStatus.DISMISSED, note) },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (isArabic) "رُفِضت" else "Dismiss", maxLines = 1) }
                    Button(
                        onClick = { viewModel.reviewReport(report.reportId, ReportStatus.ACTION_TAKEN, note) },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (isArabic) "إجراء" else "Action", maxLines = 1) }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isArabic) "إجراء على العضو المُبلَّغ عنه:" else "Moderate reported user:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { viewModel.setUserModeration(report.reportedId, ModerationStatus.WARNED, note) },
                        enabled = report.reportedId.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text(if (isArabic) "تحذير" else "Warn", maxLines = 1) }
                    OutlinedButton(
                        onClick = { viewModel.setUserModeration(report.reportedId, ModerationStatus.SUSPENDED, note) },
                        enabled = report.reportedId.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text(if (isArabic) "إيقاف" else "Suspend", maxLines = 1) }
                    OutlinedButton(
                        onClick = { viewModel.setUserModeration(report.reportedId, ModerationStatus.BANNED, note) },
                        enabled = report.reportedId.isNotBlank(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) { Text(if (isArabic) "حظر" else "Ban", maxLines = 1) }
                }
            }
        }
    }
}

@Composable
private fun PhotosTab(
    photos: List<UserPhoto>,
    isArabic: Boolean,
    viewModel: AdminModerationViewModel
) {
    if (photos.isEmpty()) {
        EmptyState(if (isArabic) "لا توجد صور بانتظار المراجعة" else "No photos pending review")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(photos, key = { it.userId + "/" + it.photoId }) { photo ->
            var reason by remember(photo.userId + photo.photoId) { mutableStateOf("") }
            ModCard {
                Text(
                    text = (if (isArabic) "العضو: " else "User: ") + photo.userId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = (if (isArabic) "الصورة: " else "Photo: ") + photo.photoId + " (" + photo.type + ")",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(if (isArabic) "سبب الرفض (اختياري)" else "Rejection reason (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { viewModel.setPhotoStatus(photo.userId, photo.photoId, PhotoStatus.APPROVED) },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (isArabic) "قبول" else "Approve", maxLines = 1) }
                    OutlinedButton(
                        onClick = { viewModel.setPhotoStatus(photo.userId, photo.photoId, PhotoStatus.REJECTED, reason) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) { Text(if (isArabic) "رفض" else "Reject", maxLines = 1) }
                }
            }
        }
    }
}

@Composable
private fun ModerationTab(
    entries: List<UserModeration>,
    isArabic: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (isArabic)
                    "سجلّات الإشراف على الأعضاء. الحظر الكامل غير مُفعّل بعد (مهمة لاحقة)."
                else
                    "User moderation records. Full ban enforcement is not active yet (TODO).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (entries.isEmpty()) {
            item {
                Text(
                    text = if (isArabic) "لا توجد سجلات إشراف بعد" else "No moderation records yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        } else {
            items(entries, key = { it.userId }) { entry ->
                ModCard {
                    Text(
                        text = entry.userId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = (if (isArabic) "الحالة: " else "Status: ") + entry.status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (entry.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = entry.note, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
