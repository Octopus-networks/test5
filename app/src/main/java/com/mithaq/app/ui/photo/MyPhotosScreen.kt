package com.mithaq.app.ui.photo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mithaq.app.domain.model.PhotoStatus
import com.mithaq.app.domain.model.PhotoType
import com.mithaq.app.domain.model.PhotoVisibility
import com.mithaq.app.domain.model.UserPhoto

private data class PhotoModeOption(
    val value: String,
    val englishLabel: String,
    val arabicLabel: String,
    val englishDescription: String,
    val arabicDescription: String
)

private val photoModeOptions = listOf(
    PhotoModeOption(PhotoVisibility.Hidden, "Hidden", "مخفية", "No one sees photos.", "لا يرى أحد الصور."),
    PhotoModeOption(PhotoVisibility.BlurredByDefault, "Blurred by default", "مموهة افتراضيًا", "Show only a safe blurred placeholder until approval.", "إظهار بديل مموه فقط حتى الموافقة."),
    PhotoModeOption(PhotoVisibility.ApprovedUsersOnly, "Approved users only", "للموافق عليهم فقط", "Visible only after approved photo request.", "تظهر بعد الموافقة على طلب الصورة فقط."),
    PhotoModeOption(PhotoVisibility.MatchedUsersOnly, "Matched users only", "للمتوافقين فقط", "Reserved for a future match policy.", "محجوزة لسياسة توافق لاحقة.")
)

@Composable
fun MyPhotosScreen(
    currentUserId: String,
    isArabic: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyPhotosViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var pendingType by remember { mutableStateOf(PhotoType.Main) }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.uploadProfilePhoto(currentUserId, uri, pendingType)
            }
        }
    )

    LaunchedEffect(currentUserId) {
        viewModel.load(currentUserId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = if (isArabic) "رجوع" else "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isArabic) "خصوصية الصور وصوري" else "Photo privacy / My photos",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isArabic) "ارفع الصور بدون كشف روابط خاصة في الملف العام." else "Upload photos without exposing private URLs in public profiles.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        PrivacyModeCard(
            isArabic = isArabic,
            selectedMode = state.currentPrivacyMode,
            onModeSelected = { viewModel.updatePhotoPrivacy(currentUserId, it) }
        )
        Spacer(modifier = Modifier.height(14.dp))
        UploadActionsCard(
            isArabic = isArabic,
            isUploading = state.isUploading,
            onPickMain = {
                pendingType = PhotoType.Main
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onPickExtra = {
                pendingType = PhotoType.Extra
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )
        Spacer(modifier = Modifier.height(14.dp))

        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(12.dp))
        }
        state.errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }
        state.successMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = if (isArabic) "الصور المرفوعة" else "Uploaded photos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (state.photos.isEmpty()) {
            Text(
                text = if (isArabic) "لا توجد صور بعد. اختر صورة رئيسية أو إضافية." else "No photos yet. Add a main or extra photo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            state.photos.forEach { photo ->
                UserPhotoRow(photo = photo, isArabic = isArabic)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isArabic) {
                "ملاحظة: المراجعة والتعتيم الحقيقي وقواعد التخزين النهائية TODO لاحقًا. هذه المرحلة تؤسس الرفع الآمن والمنع من كشف الروابط العامة."
            } else {
                "Note: review, real blurred previews, and production Storage access hardening remain TODOs. This phase establishes safe upload metadata and avoids public URL leaks."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PrivacyModeCard(
    isArabic: Boolean,
    selectedMode: String,
    onModeSelected: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text(
                    text = if (isArabic) "وضع خصوصية الصور" else "Current privacy mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            photoModeOptions.forEach { option ->
                val selected = selectedMode == option.value
                FilterChip(
                    selected = selected,
                    onClick = { onModeSelected(option.value) },
                    label = {
                        Column {
                            Text(if (isArabic) option.arabicLabel else option.englishLabel)
                            Text(
                                text = if (isArabic) option.arabicDescription else option.englishDescription,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun UploadActionsCard(
    isArabic: Boolean,
    isUploading: Boolean,
    onPickMain: () -> Unit,
    onPickExtra: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = if (isArabic) "إضافة الصور" else "Add photos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Button(onClick = onPickMain, enabled = !isUploading, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(if (isArabic) "إضافة صورة رئيسية" else "Add main photo")
            }
            OutlinedButton(onClick = onPickExtra, enabled = !isUploading, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(if (isArabic) "إضافة صورة إضافية" else "Add extra photo")
            }
        }
    }
}

@Composable
private fun UserPhotoRow(photo: UserPhoto, isArabic: Boolean) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (photo.type == PhotoType.Main) {
                        if (isArabic) "صورة رئيسية" else "Main photo"
                    } else {
                        if (isArabic) "صورة إضافية" else "Extra photo"
                    },
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isArabic) "لا يتم عرض رابط التخزين هنا" else "Storage URL is not shown here",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AssistChip(
                onClick = {},
                label = { Text(photo.status.toStatusLabel(isArabic)) },
                leadingIcon = { Icon(Icons.Filled.Visibility, contentDescription = null) }
            )
        }
    }
}

private fun String.toStatusLabel(isArabic: Boolean): String {
    return when (this) {
        PhotoStatus.Approved -> if (isArabic) "مقبولة" else "approved"
        PhotoStatus.Rejected -> if (isArabic) "مرفوضة" else "rejected"
        else -> if (isArabic) "بانتظار المراجعة" else "pending review"
    }
}
