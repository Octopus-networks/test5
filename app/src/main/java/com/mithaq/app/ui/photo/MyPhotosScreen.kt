package com.mithaq.app.ui.photo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import com.mithaq.app.ui.components.MithaqEmptyState
import com.mithaq.app.ui.components.MithaqLoadingSkeleton
import com.mithaq.app.ui.components.SkeletonType
import com.mithaq.app.domain.model.PhotoType
import com.mithaq.app.domain.model.UserPhoto

/**
 * Phase 11 secure photo management entry (reached from the Profile Hub).
 * Uses the Android Photo Picker (no broad storage permission). Photos upload to
 * `user_photos/{userId}/{photoId}.jpg` with metadata under `userPhotos/{userId}/photos`.
 */
@Composable
fun MyPhotosScreen(
    currentUserId: String,
    isArabic: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyPhotosViewModel = viewModel(key = "mithaq_my_photos_$currentUserId")
) {
    val state by viewModel.state.collectAsState()

    val mainPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> viewModel.upload(PhotoType.MAIN, uri) }

    val extraPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> viewModel.upload(PhotoType.EXTRA, uri) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
            Text(
                text = if (isArabic) "صوري" else "My Photos",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isArabic) {
                "صورك تبقى خاصة. تُرفع للمراجعة وتظهر فقط حسب إعدادات الخصوصية أو بعد الموافقة."
            } else {
                "Your photos stay private. They are uploaded for review and shown only per your " +
                    "privacy settings or after an approved request."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = {
                mainPhotoLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            enabled = !state.isUploading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isArabic) "رفع الصورة الرئيسية" else "Upload main photo")
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            onClick = {
                extraPhotoLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            enabled = !state.isUploading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isArabic) "إضافة صورة أخرى" else "Add extra photo")
        }

        if (state.isUploading) {
            Spacer(modifier = Modifier.height(14.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        state.message?.let { msg ->
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = if (isArabic) "صوري المرفوعة" else "Uploaded photos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))

        when {
            state.isLoading -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(2) {
                    MithaqLoadingSkeleton(type = SkeletonType.REQUEST_ROW)
                }
            }
            state.photos.isEmpty() -> MithaqEmptyState(
                title = if (isArabic) "لا توجد صور بعد" else "No photos yet",
                message = if (isArabic) "صورك المرفوعة ستظهر هنا بعد المراجعة." else "Your uploaded photos will appear here after review.",
                icon = Icons.Filled.Favorite
            )
            else -> state.photos.forEach { photo ->
                MyPhotoRow(photo = photo, isArabic = isArabic)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun MyPhotoRow(photo: UserPhoto, isArabic: Boolean) {
    val softGold = Color(0xFFF2CA50)
    val softEmerald = Color(0xFF8BD6B6)
    val softRed = Color(0xFFE57373)

    val statusColor = when (photo.status) {
        "approved" -> softEmerald
        "rejected" -> softRed
        else -> softGold
    }

    val visibilityLabel = when (photo.visibility.lowercase()) {
        "hidden" -> if (isArabic) "خاصة" else "Private"
        "public" -> if (isArabic) "عامة" else "Public"
        else -> photo.visibility
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.16f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (photo.type == PhotoType.MAIN.raw) {
                    if (isArabic) "الصورة الرئيسية" else "Main photo"
                } else {
                    if (isArabic) "صورة إضافية" else "Extra photo"
                },
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = (if (isArabic) "الحالة: " else "Status: ") + statusLabel(photo.status, isArabic),
                style = MaterialTheme.typography.bodySmall,
                color = statusColor,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = (if (isArabic) "الخصوصية: " else "Visibility: ") + visibilityLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun statusLabel(status: String, isArabic: Boolean): String = when (status) {
    "approved" -> if (isArabic) "مقبولة" else "Approved"
    "rejected" -> if (isArabic) "مرفوضة" else "Rejected"
    else -> if (isArabic) "قيد المراجعة" else "Pending review"
}
