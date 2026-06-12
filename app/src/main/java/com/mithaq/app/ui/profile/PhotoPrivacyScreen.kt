package com.mithaq.app.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mithaq.app.data.repository.PublicProfileRepository
import com.mithaq.app.model.UserProfile
import com.mithaq.app.ui.photo.PhotoAccessManager
import kotlinx.coroutines.launch

/**
 * "Photo privacy" screen reached from the Profile hub.
 * Lets the owner manage who can see their (otherwise blurred) photos:
 *  - pending requests -> approve / reject
 *  - approved users   -> revoke
 *
 * All actions write to the owner's own users/{uid} document via [PhotoAccessManager],
 * which the existing Firestore owner-write rules already allow (no rules change needed).
 */
@Composable
fun PhotoPrivacyScreen(
    currentUser: UserProfile,
    isArabic: Boolean,
    onRefreshProfile: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { PhotoAccessManager(context) }
    val repo = remember { PublicProfileRepository() }

    val requests = currentUser.photoAccessRequests
    val approved = currentUser.photoAccessApprovedUsers

    var names by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var working by remember { mutableStateOf(false) }

    // Resolve UIDs -> display names from the public mirror.
    LaunchedEffect(requests, approved) {
        val ids = (requests + approved).distinct()
        val resolved = mutableMapOf<String, String>()
        ids.forEach { id ->
            val name = repo.getPublicProfile(id)?.displayName
            resolved[id] = if (!name.isNullOrBlank()) name else id.take(6)
        }
        names = resolved
    }

    fun displayName(uid: String): String = names[uid] ?: uid.take(6)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        TextButton(onClick = onBack) {
            Text(text = if (isArabic) "رجوع" else "Back")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isArabic) "خصوصية الصور" else "Photo privacy",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isArabic) "تحكّم في من يمكنه رؤية صورك." else "Control who can see your photos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ---------------- Pending requests ----------------
        SectionTitle(text = if (isArabic) "طلبات معلّقة" else "Pending requests")
        Spacer(modifier = Modifier.height(8.dp))
        if (requests.isEmpty()) {
            EmptyHint(text = if (isArabic) "لا توجد طلبات حالياً." else "No pending requests.")
        } else {
            requests.forEach { uid ->
                PersonRow(name = displayName(uid)) {
                    Button(
                        enabled = !working,
                        onClick = {
                            scope.launch {
                                working = true
                                manager.approvePhotoAccess(currentUser.uid, uid)
                                onRefreshProfile()
                                working = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isArabic) "موافقة" else "Approve")
                    }
                    Spacer(modifier = Modifier.height(0.dp))
                    OutlinedButton(
                        enabled = !working,
                        onClick = {
                            scope.launch {
                                working = true
                                manager.rejectPhotoAccess(currentUser.uid, uid)
                                onRefreshProfile()
                                working = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isArabic) "رفض" else "Reject")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ---------------- Approved users ----------------
        SectionTitle(text = if (isArabic) "مصرّح لهم برؤية صورك" else "Approved to see your photos")
        Spacer(modifier = Modifier.height(8.dp))
        if (approved.isEmpty()) {
            EmptyHint(text = if (isArabic) "لم توافق لأحد بعد." else "You haven't approved anyone yet.")
        } else {
            approved.forEach { uid ->
                PersonRow(name = displayName(uid)) {
                    OutlinedButton(
                        enabled = !working,
                        onClick = {
                            scope.launch {
                                working = true
                                val revoked = manager.revokePhotoAccess(currentUser.uid, uid)
                                if (revoked) {
                                    onRefreshProfile()
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        if (isArabic) {
                                            "تعذر سحب صلاحية الصور. حاول مرة أخرى."
                                        } else {
                                            "Could not revoke photo access. Please try again."
                                        },
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                                working = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(if (isArabic) "سحب" else "Revoke")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun PersonRow(name: String, actions: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            actions()
        }
    }
}
