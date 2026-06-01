package com.mithaq.app.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mithaq.app.domain.model.ModerationPhotoReviewItem
import com.mithaq.app.domain.model.ModerationUserReport

/**
 * Internal moderation foundation screen.
 *
 * This screen is intentionally not wired into normal app navigation. Production
 * routing should expose it only to trusted accounts with Firebase custom claims
 * (`admin == true` or `moderator == true`) and preferably behind an internal
 * build/route gate.
 */
@Composable
fun AdminModerationScreen(
    viewModel: AdminModerationViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Admin moderation",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Internal foundation only. Do not expose this route in normal navigation.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        when {
            state.isCheckingAccess -> CircularProgressIndicator()
            !state.hasModeratorAccess -> Text(
                text = state.errorMessage ?: "Admin or moderator access is required.",
                color = MaterialTheme.colorScheme.error
            )
            else -> {
                ModerationSummaryCard(
                    openReportsCount = state.openReportsCount,
                    pendingPhotosCount = state.pendingPhotosCount,
                    isLoading = state.isLoading
                )

                state.successMessage?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.primary)
                }
                state.errorMessage?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }

                Text(
                    text = "Open reports",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (state.openReports.isEmpty()) {
                    Text("No open reports.")
                } else {
                    state.openReports.forEach { report ->
                        ReportModerationCard(
                            report = report,
                            onReviewed = { viewModel.markReportReviewed(report.reportId) },
                            onDismiss = { viewModel.dismissReport(report.reportId) },
                            onActionTaken = { viewModel.markReportActionTaken(report.reportId) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Pending photo review",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (state.pendingPhotos.isEmpty()) {
                    Text("No pending photos.")
                } else {
                    state.pendingPhotos.forEach { photo ->
                        PhotoModerationCard(
                            photo = photo,
                            onApprove = { viewModel.approvePhoto(photo.userId, photo.photoId) },
                            onReject = { viewModel.rejectPhoto(photo.userId, photo.photoId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModerationSummaryCard(
    openReportsCount: Int,
    pendingPhotosCount: Int,
    isLoading: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Open reports: $openReportsCount")
            Text("Pending photos: $pendingPhotosCount")
            if (isLoading) Text("Refreshing moderation queues...")
        }
    }
}

@Composable
private fun ReportModerationCard(
    report: ModerationUserReport,
    onReviewed: () -> Unit,
    onDismiss: () -> Unit,
    onActionTaken: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Report ${report.reportId}", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Reported user: ${report.reportedUserId}", maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Reporter: ${report.reporterUserId}", maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (report.chatId.isNotBlank()) Text("Chat: ${report.chatId}", maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Reason: ${report.reason}")
            if (report.details.isNotBlank()) Text("Details: ${report.details}", maxLines = 3, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onReviewed) { Text("Reviewed") }
                OutlinedButton(onClick = onDismiss) { Text("Dismiss") }
                OutlinedButton(onClick = onActionTaken) { Text("Action") }
            }
        }
    }
}

@Composable
private fun PhotoModerationCard(
    photo: ModerationPhotoReviewItem,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Photo ${photo.photoId}", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Owner: ${photo.userId}", maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Type: ${photo.type} · Visibility: ${photo.visibility}")
            Text("Storage path hidden from normal users; moderators should review only through trusted internal tooling.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onApprove) { Text("Approve") }
                OutlinedButton(onClick = onReject) { Text("Reject") }
            }
        }
    }
}
