package com.mithaq.app.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mithaq.app.R
import com.mithaq.app.ui.photo.MyPhotosScreen

private data class ProfileHubItem(
    val titleResId: Int,
    val titleArabicResId: Int,
    val subtitleResId: Int,
    val subtitleArabicResId: Int,
    val icon: ImageVector
)

private val profileItems = listOf(
    ProfileHubItem(R.string.profile_hub_my_profile_title, R.string.profile_hub_my_profile_title_ar, R.string.profile_hub_my_profile_subtitle, R.string.profile_hub_my_profile_subtitle_ar, Icons.Filled.Person),
    ProfileHubItem(R.string.profile_hub_privacy_title, R.string.profile_hub_privacy_title_ar, R.string.profile_hub_privacy_subtitle, R.string.profile_hub_privacy_subtitle_ar, Icons.Filled.Lock),
    ProfileHubItem(R.string.profile_hub_photo_privacy_title, R.string.profile_hub_photo_privacy_title_ar, R.string.profile_hub_photo_privacy_subtitle, R.string.profile_hub_photo_privacy_subtitle_ar, Icons.Filled.Visibility),
    ProfileHubItem(R.string.profile_hub_guardian_title, R.string.profile_hub_guardian_title_ar, R.string.profile_hub_guardian_subtitle, R.string.profile_hub_guardian_subtitle_ar, Icons.Filled.CheckCircle),
    ProfileHubItem(R.string.profile_hub_prayer_title, R.string.profile_hub_prayer_title_ar, R.string.profile_hub_prayer_subtitle, R.string.profile_hub_prayer_subtitle_ar, Icons.Filled.Settings),
    ProfileHubItem(R.string.profile_hub_notifications_title, R.string.profile_hub_notifications_title_ar, R.string.profile_hub_notifications_subtitle, R.string.profile_hub_notifications_subtitle_ar, Icons.Filled.Info),
    ProfileHubItem(R.string.profile_hub_language_title, R.string.profile_hub_language_title_ar, R.string.profile_hub_language_subtitle, R.string.profile_hub_language_subtitle_ar, Icons.Filled.Settings),
    ProfileHubItem(R.string.profile_hub_security_title, R.string.profile_hub_security_title_ar, R.string.profile_hub_security_subtitle, R.string.profile_hub_security_subtitle_ar, Icons.Filled.Lock),
    ProfileHubItem(R.string.profile_hub_support_title, R.string.profile_hub_support_title_ar, R.string.profile_hub_support_subtitle, R.string.profile_hub_support_subtitle_ar, Icons.Filled.Info)
)

@Composable
private fun localizedString(isArabic: Boolean, englishResId: Int, arabicResId: Int): String =
    stringResource(id = if (isArabic) arabicResId else englishResId)

@Composable
fun MithaqProfileHubScreen(
    isArabic: Boolean,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var openItem by remember { mutableStateOf<ProfileHubItem?>(null) }
    var showMyPhotos by remember { mutableStateOf(false) }

    if (showMyPhotos) {
        MyPhotosScreen(
            isArabic = isArabic,
            onBack = { showMyPhotos = false },
            modifier = modifier
        )
        return
    }

    val selectedItem = openItem
    if (selectedItem != null) {
        ProfileSectionDetail(
            item = selectedItem,
            isArabic = isArabic,
            onBack = { openItem = null },
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Text(
            text = localizedString(isArabic, R.string.profile_hub_title, R.string.profile_hub_title_ar),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = localizedString(isArabic, R.string.profile_hub_subtitle, R.string.profile_hub_subtitle_ar),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))
        MyPhotosEntryCard(isArabic = isArabic, onClick = { showMyPhotos = true })
        Spacer(modifier = Modifier.height(10.dp))
        profileItems.forEach { item ->
            ProfileHubRow(item = item, isArabic = isArabic, onClick = { openItem = item })
            Spacer(modifier = Modifier.height(10.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = localizedString(isArabic, R.string.common_sign_out, R.string.common_sign_out_ar),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ProfileHubRow(
    item: ProfileHubItem,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = localizedString(isArabic, item.titleResId, item.titleArabicResId),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = localizedString(isArabic, item.subtitleResId, item.subtitleArabicResId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MyPhotosEntryCard(isArabic: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isArabic) "صوري" else "My Photos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (isArabic) {
                        "ارفع وأدِر صورك الخاصة بأمان"
                    } else {
                        "Upload and manage your private photos securely"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProfileSectionDetail(
    item: ProfileHubItem,
    isArabic: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        TextButton(onClick = onBack) {
            Text(text = if (isArabic) "‹ رجوع" else "‹ Back")
        }
        Spacer(modifier = Modifier.height(8.dp))
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = localizedString(isArabic, item.titleResId, item.titleArabicResId),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = localizedString(isArabic, item.subtitleResId, item.subtitleArabicResId),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isArabic) {
                "الإعدادات التفصيلية لهذا القسم قيد الربط وستتوفّر قريبًا."
            } else {
                "Detailed controls for this section are being connected and will be available soon."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
