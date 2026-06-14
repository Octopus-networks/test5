package com.mithaq.app.ui.photo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A dual-purpose UI card for Feature 2: Multi-Stage Modesty Photo Unlock.
 * - For viewers: Renders Lock status and "Request Access" controls.
 * - For profile owners: Renders incoming requests with Approve/Decline actions.
 */
@Composable
fun PhotoAccessRequestCard(
    isArabic: Boolean,
    isOwnProfile: Boolean,
    accessState: PhotoAccessState,
    onRequestAccessClicked: () -> Unit,
    modifier: Modifier = Modifier,
    pendingRequests: List<String> = emptyList(),
    onApproveClicked: (userId: String) -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isOwnProfile) {
                // Profile Owner View: List requests
                if (pendingRequests.isNotEmpty()) {
                    Text(
                        text = if (isArabic) "طلبات الوصول للصور" else "Photo Access Requests",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    pendingRequests.forEach { requesterName ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) "يطلب $requesterName الوصول للصور" else "$requesterName requests photo access",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Button(
                                onClick = { onApproveClicked(requesterName) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00A86B) // Emerald Green Approve
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = "Approve",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isArabic) "موافقة" else "Approve", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                } else {
                    // Default state
                    Text(
                        text = if (isArabic) "الصور مموهة افتراضيًا" else "Photos Blurred by Default",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (isArabic) "يتم تمويه صورك للحفاظ على الحشمة. سيطلب الأشخاص الجادون الإذن لرؤيتها." else "Your photos are blurred to preserve modesty. Serious matches will request permission to view them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                // Viewer View: Check access level
                when (accessState) {
                    PhotoAccessState.NONE -> {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isArabic) "صورة مموهة للحشمة" else "Photo Blurred for Modesty",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isArabic) "اسأل بجدية لرؤية صورة الملف الشخصي." else "Inquire seriously to view this profile photo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRequestAccessClicked,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isArabic) "طلب رؤية الصورة" else "Request Photo Access")
                        }
                    }

                    PhotoAccessState.PENDING -> {
                        Icon(
                            imageVector = Icons.Default.MailOutline,
                            contentDescription = "Pending",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isArabic) "طلب الوصول معلق" else "Access Request Pending",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB300)
                        )
                        Text(
                            text = if (isArabic) "سنقوم بإبلاغك بمجرد قبولهم لطلبك." else "We will notify you once they accept your request.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    PhotoAccessState.APPROVED -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "Unlocked",
                                tint = Color(0xFF00A86B),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isArabic) "صورة مفتوحة" else "Photo Unlocked",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00A86B)
                            )
                        }
                    }
                }
            }
        }
    }
}
