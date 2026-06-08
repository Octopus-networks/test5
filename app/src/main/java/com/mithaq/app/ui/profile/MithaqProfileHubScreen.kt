package com.mithaq.app.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import com.mithaq.app.ui.components.MithaqEmptyState
import com.mithaq.app.ui.notifications.NotificationSettingsScreen
import com.mithaq.app.ui.photo.MyPhotosScreen

private data class ProfileHubItem(
    val titleResId: Int,
    val titleArabicResId: Int,
    val subtitleResId: Int,
    val subtitleArabicResId: Int,
    val icon: ImageVector
)

private val comingSoonProfileItems = listOf(
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
    currentUserId: String,
    isArabic: Boolean,
    onSignOut: () -> Unit,
    isAdmin: Boolean = false,
    onOpenAdminModeration: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    onOpenProfileSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var openItem by remember(currentUserId) { mutableStateOf<ProfileHubItem?>(null) }
    var showMyPhotos by remember(currentUserId) { mutableStateOf(false) }
    var showNotificationSettings by remember(currentUserId) { mutableStateOf(false) }
    var showSecurity by remember(currentUserId) { mutableStateOf(false) }
    var showSupport by remember(currentUserId) { mutableStateOf(false) }

    BackHandler(enabled = showMyPhotos || showNotificationSettings || showSecurity || showSupport || openItem != null) {
        when {
            showMyPhotos -> showMyPhotos = false
            showNotificationSettings -> showNotificationSettings = false
            showSecurity -> showSecurity = false
            showSupport -> showSupport = false
            else -> openItem = null
        }
    }

    if (showMyPhotos) {
        MyPhotosScreen(
            currentUserId = currentUserId,
            isArabic = isArabic,
            onBack = { showMyPhotos = false },
            modifier = modifier
        )
        return
    }

    if (showNotificationSettings) {
        NotificationSettingsScreen(
            currentUserId = currentUserId,
            isArabic = isArabic,
            onBack = { showNotificationSettings = false },
            modifier = modifier
        )
        return
    }

    if (showSecurity) {
        SecuritySettingsScreen(
            currentUserId = currentUserId,
            isArabic = isArabic,
            onBack = { showSecurity = false },
            modifier = modifier
        )
        return
    }

    if (showSupport) {
        SupportScreen(
            isArabic = isArabic,
            onBack = { showSupport = false },
            modifier = modifier
        )
        return
    }

    val selectedItem = openItem
    if (selectedItem != null) {
        ProfileComingSoonScreen(
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
        comingSoonProfileItems.forEach { item ->
            when (item.titleResId) {
                // Phase 1: My Profile opens the full ProfileSettings editor.
                R.string.profile_hub_my_profile_title -> ProfileHubRow(
                    item = item,
                    isArabic = isArabic,
                    showComingSoon = false,
                    onClick = onOpenProfileSettings
                )
                // Phase 13C: Notifications is a real screen (no longer "Coming Soon").
                R.string.profile_hub_notifications_title -> ProfileHubRow(
                    item = item,
                    isArabic = isArabic,
                    showComingSoon = false,
                    onClick = { showNotificationSettings = true }
                )
                // Phase 0 wiring: Language opens the existing App Settings screen.
                R.string.profile_hub_language_title -> ProfileHubRow(
                    item = item,
                    isArabic = isArabic,
                    showComingSoon = false,
                    onClick = onOpenAppSettings
                )
                // Phase 0 wiring: Security opens the inline Security settings screen.
                R.string.profile_hub_security_title -> ProfileHubRow(
                    item = item,
                    isArabic = isArabic,
                    showComingSoon = false,
                    onClick = { showSecurity = true }
                )
                // Phase 0 wiring: Support opens the inline Support screen.
                R.string.profile_hub_support_title -> ProfileHubRow(
                    item = item,
                    isArabic = isArabic,
                    showComingSoon = false,
                    onClick = { showSupport = true }
                )
                else -> ProfileHubRow(item = item, isArabic = isArabic, onClick = { openItem = item })
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
        // Admin-only entry. Hidden entirely for normal users; the destination screen also
        // re-checks admin status and renders a safe "Not authorized" state otherwise.
        if (isAdmin) {
            AdminModerationEntryCard(isArabic = isArabic, onClick = onOpenAdminModeration)
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
    onClick: () -> Unit,
    showComingSoon: Boolean = true
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
            ProfileHubIcon(icon = item.icon)
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
                if (showComingSoon) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = localizedString(
                            isArabic,
                            R.string.profile_hub_coming_soon_badge,
                            R.string.profile_hub_coming_soon_badge_ar
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
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
            ProfileHubIcon(icon = Icons.Filled.Image)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = localizedString(
                        isArabic,
                        R.string.profile_hub_my_photos_title,
                        R.string.profile_hub_my_photos_title_ar
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = localizedString(
                        isArabic,
                        R.string.profile_hub_my_photos_subtitle,
                        R.string.profile_hub_my_photos_subtitle_ar
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AdminModerationEntryCard(isArabic: Boolean, onClick: () -> Unit) {
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
            ProfileHubIcon(icon = Icons.Filled.Lock)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = localizedString(
                        isArabic,
                        R.string.profile_hub_admin_title,
                        R.string.profile_hub_admin_title_ar
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = localizedString(
                        isArabic,
                        R.string.profile_hub_admin_subtitle,
                        R.string.profile_hub_admin_subtitle_ar
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProfileHubIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun ProfileComingSoonScreen(
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
            Text(text = localizedString(isArabic, R.string.common_back, R.string.common_back_ar))
        }
        Spacer(modifier = Modifier.height(8.dp))
        ProfileHubIcon(icon = item.icon)
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
        Spacer(modifier = Modifier.height(18.dp))
        MithaqEmptyState(
            title = localizedString(
                isArabic,
                R.string.profile_hub_coming_soon_title,
                R.string.profile_hub_coming_soon_title_ar
            ),
            message = localizedString(
                isArabic,
                R.string.profile_hub_coming_soon_message,
                R.string.profile_hub_coming_soon_message_ar
            ),
            icon = Icons.Filled.Info,
            actionLabel = localizedString(isArabic, R.string.common_back, R.string.common_back_ar),
            onAction = onBack
        )
    }
}
