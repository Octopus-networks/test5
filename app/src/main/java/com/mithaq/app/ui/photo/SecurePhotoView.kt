package com.mithaq.app.ui.photo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mithaq.app.data.repository.PhotoRepository
import com.mithaq.app.domain.model.PhotoAccessLevel
import com.mithaq.app.domain.model.PhotoVisibility

/**
 * Phase 11 secure photo display foundation.
 *
 * Resolves access from the owner's public privacy mode + approved photo request, then:
 *  - FULL   -> renders the real photo (bytes fetched at runtime, never a stored URL).
 *  - else   -> renders a safe locked/blurred placeholder and never fetches the image.
 *
 * Storage security rules remain the real boundary for the bytes.
 */
@Composable
fun SecurePhotoView(
    ownerId: String,
    photoPrivacyMode: String,
    isArabic: Boolean,
    modifier: Modifier = Modifier,
    repository: PhotoRepository = remember { PhotoRepository() }
) {
    var accessLevel by remember(ownerId, photoPrivacyMode) { mutableStateOf<PhotoAccessLevel?>(null) }
    var imageBytes by remember(ownerId, photoPrivacyMode) { mutableStateOf<ByteArray?>(null) }

    LaunchedEffect(ownerId, photoPrivacyMode) {
        accessLevel = null
        imageBytes = null
        val level = repository.resolveAccessLevel(ownerId, photoPrivacyMode)
        accessLevel = level
        // Only fetch real bytes when fully authorised. Locked/blurred never fetch.
        if (level == PhotoAccessLevel.FULL) {
            imageBytes = repository.loadAccessiblePhotoBytes(ownerId)
        }
    }

    val bitmap = remember(imageBytes) {
        imageBytes?.let { bytes ->
            try {
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                null
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (accessLevel == PhotoAccessLevel.FULL && bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            SecurePhotoPlaceholder(mode = photoPrivacyMode, isArabic = isArabic)
        }
    }
}

@Composable
private fun SecurePhotoPlaceholder(mode: String, isArabic: Boolean) {
    val visibility = PhotoVisibility.from(mode)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (visibility == PhotoVisibility.BLURRED_BY_DEFAULT) Icons.Filled.Visibility else Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = placeholderLabel(visibility, isArabic),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun placeholderLabel(visibility: PhotoVisibility, isArabic: Boolean): String = when (visibility) {
    PhotoVisibility.HIDDEN ->
        if (isArabic) "هذا العضو يبقي صوره خاصة" else "This member keeps photos private"
    PhotoVisibility.MATCHED_USERS_ONLY ->
        if (isArabic) "الصور تظهر بعد التطابق فقط" else "Photos are visible to matches only"
    PhotoVisibility.APPROVED_USERS_ONLY ->
        if (isArabic) "الصورة مقفلة — اطلب الإذن للعرض" else "Photo locked — request access to view"
    PhotoVisibility.BLURRED_BY_DEFAULT ->
        if (isArabic) "الصورة محجوبة حتى الموافقة" else "Photo blurred until access is approved"
}
