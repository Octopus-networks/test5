package com.mithaq.app.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mithaq.app.ui.guardian.GuardianUiState

@Composable
fun GuardianScreen(
    currentUser: com.mithaq.app.model.UserProfile,
    viewModel: com.mithaq.app.ui.guardian.GuardianViewModel,
    isArabic: Boolean,
    onRefreshProfile: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var guardianName by remember(currentUser.uid) { mutableStateOf("") }
    var guardianEmail by remember(currentUser.uid) { mutableStateOf("") }
    val isLoading = uiState is GuardianUiState.Loading

    LaunchedEffect(uiState) {
        if (uiState is GuardianUiState.Success) {
            Toast.makeText(
                context,
                if (isArabic) "تم إرسال الدعوة" else "Invite sent",
                Toast.LENGTH_SHORT
            ).show()
            onRefreshProfile()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        TextButton(onClick = onBack) {
            Text(text = if (isArabic) "رجوع" else "Back")
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (isArabic) "وليّ الأمر" else "Guardian / Wali",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(18.dp))

        GuardianStatusCard(
            guardianName = currentUser.guardianName,
            guardianEmail = currentUser.guardianEmail,
            guardianStatus = currentUser.guardianStatus,
            isArabic = isArabic
        )

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isArabic) "إرسال دعوة لولي الأمر" else "Invite a guardian",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedTextField(
                    value = guardianName,
                    onValueChange = { guardianName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (isArabic) "اسم ولي الأمر" else "Guardian name") },
                    singleLine = true,
                    enabled = !isLoading
                )
                OutlinedTextField(
                    value = guardianEmail,
                    onValueChange = { guardianEmail = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (isArabic) "بريد ولي الأمر" else "Guardian email") },
                    singleLine = true,
                    enabled = !isLoading
                )
                if (uiState is GuardianUiState.Error) {
                    Text(
                        text = (uiState as GuardianUiState.Error).errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Button(
                    onClick = { viewModel.inviteGuardian(guardianName, guardianEmail) },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(text = if (isArabic) "إرسال الدعوة" else "Send invite")
                    }
                }
            }
        }
    }
}

@Composable
private fun GuardianStatusCard(
    guardianName: String?,
    guardianEmail: String?,
    guardianStatus: String?,
    isArabic: Boolean
) {
    val normalizedStatus = guardianStatus?.uppercase()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isArabic) "حالة ولي الأمر" else "Guardian status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            when (normalizedStatus) {
                "VERIFIED" -> {
                    GuardianBadge(
                        text = if (isArabic) "موثّق" else "Verified",
                        containerColor = Color(0xFF0F7A4D),
                        contentColor = Color.White
                    )
                    GuardianInfoRows(
                        guardianName = guardianName,
                        guardianEmail = guardianEmail,
                        isArabic = isArabic
                    )
                }
                "PENDING" -> {
                    GuardianBadge(
                        text = if (isArabic) "قيد الانتظار" else "Pending",
                        containerColor = Color(0xFFFFC857),
                        contentColor = Color(0xFF2A2110)
                    )
                    GuardianInfoRows(
                        guardianName = guardianName,
                        guardianEmail = guardianEmail,
                        isArabic = isArabic
                    )
                }
                else -> {
                    Text(
                        text = if (isArabic) {
                            "لم تتم إضافة وليّ أمر بعد"
                        } else {
                            "No guardian added yet"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun GuardianBadge(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = contentColor
    )
}

@Composable
private fun GuardianInfoRows(
    guardianName: String?,
    guardianEmail: String?,
    isArabic: Boolean
) {
    val fallback = if (isArabic) "غير محدد" else "Not provided"

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isArabic) "الاسم" else "Name",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = guardianName?.takeIf { it.isNotBlank() } ?: fallback,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isArabic) "البريد" else "Email",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = guardianEmail?.takeIf { it.isNotBlank() } ?: fallback,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
