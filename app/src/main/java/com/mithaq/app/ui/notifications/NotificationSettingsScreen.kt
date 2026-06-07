package com.mithaq.app.ui.notifications

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mithaq.app.data.repository.NotificationSettingsRepository
import com.mithaq.app.domain.model.NotificationSettings

/**
 * Phase 13C — notification settings screen reached from Profile Hub → Notifications.
 * A master toggle plus one toggle per Phase 13B notification category. Saved explicitly
 * to users/{uid}/notificationSettings/preferences. Shows loading, saving, and error
 * states and never displays tokens or technical data.
 */
@Composable
fun NotificationSettingsScreen(
    currentUserId: String,
    isArabic: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: NotificationSettingsViewModel = viewModel(
        key = "mithaq_notification_settings_$currentUserId",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NotificationSettingsViewModel(
                    currentUserId = currentUserId,
                    repository = NotificationSettingsRepository(context.applicationContext)
                ) as T
            }
        }
    )
    val state by viewModel.state.collectAsState()
    val settings = state.settings

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
                text = if (isArabic) "الإشعارات" else "Notifications",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isArabic) {
                "تحكّم في الإشعارات التي تصلك من ميثاق."
            } else {
                "Choose which alerts Mithaq sends you."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                // Master switch.
                SettingToggleCard(
                    title = if (isArabic) "تفعيل الإشعارات" else "Enable notifications",
                    subtitle = if (isArabic) {
                        "عند الإيقاف، لن تصلك أي إشعارات."
                    } else {
                        "When off, you won't receive any notifications."
                    },
                    checked = settings.notificationsEnabled,
                    enabled = !state.isSaving,
                    onCheckedChange = { value ->
                        viewModel.update { it.copy(notificationsEnabled = value) }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isArabic) "أنواع الإشعارات" else "Notification types",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                val categoriesEnabled = settings.notificationsEnabled && !state.isSaving

                SettingToggleCard(
                    title = if (isArabic) "طلبات الاهتمام" else "Interest requests",
                    subtitle = if (isArabic) {
                        "عندما يرسل لك أحدهم طلب اهتمام."
                    } else {
                        "When someone sends you an interest request."
                    },
                    checked = settings.interestRequestNotifications,
                    enabled = categoriesEnabled,
                    onCheckedChange = { value ->
                        viewModel.update { it.copy(interestRequestNotifications = value) }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))

                SettingToggleCard(
                    title = if (isArabic) "طلبات الصور" else "Photo requests",
                    subtitle = if (isArabic) {
                        "عند طلب الوصول إلى صورك."
                    } else {
                        "When someone requests access to your photos."
                    },
                    checked = settings.photoRequestNotifications,
                    enabled = categoriesEnabled,
                    onCheckedChange = { value ->
                        viewModel.update { it.copy(photoRequestNotifications = value) }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))

                SettingToggleCard(
                    title = if (isArabic) "طلبات المحادثة" else "Chat requests",
                    subtitle = if (isArabic) {
                        "عندما يطلب أحدهم بدء محادثة."
                    } else {
                        "When someone requests to start a chat."
                    },
                    checked = settings.chatRequestNotifications,
                    enabled = categoriesEnabled,
                    onCheckedChange = { value ->
                        viewModel.update { it.copy(chatRequestNotifications = value) }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))

                SettingToggleCard(
                    title = if (isArabic) "الرسائل" else "Messages",
                    subtitle = if (isArabic) {
                        "عند استلام رسالة جديدة."
                    } else {
                        "When you receive a new message."
                    },
                    checked = settings.messageNotifications,
                    enabled = categoriesEnabled,
                    onCheckedChange = { value ->
                        viewModel.update { it.copy(messageNotifications = value) }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))

                SettingToggleCard(
                    title = if (isArabic) "مراجعة الصور" else "Photo review updates",
                    subtitle = if (isArabic) {
                        "عند الموافقة على صورتك أو رفضها."
                    } else {
                        "When your photo is approved or rejected."
                    },
                    checked = settings.photoModerationNotifications,
                    enabled = categoriesEnabled,
                    onCheckedChange = { value ->
                        viewModel.update { it.copy(photoModerationNotifications = value) }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Notification sound is owned by the Android system per channel (device-local).
                // Open the OS notification settings so the user picks the sound there — no app
                // storage, no backend, no channel changes.
                SettingActionCard(
                    title = if (isArabic) "صوت الإشعار" else "Notification sound",
                    subtitle = if (isArabic) {
                        "اختر صوت الإشعار من إعدادات أندرويد."
                    } else {
                        "Choose the notification sound in Android settings."
                    },
                    onClick = { openSystemNotificationSettings(context) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                state.errorMessage?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (state.justSaved && !state.hasChanges) {
                    Text(
                        text = if (isArabic) "تم حفظ التغييرات." else "Your settings were saved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Button(
                    onClick = { viewModel.save() },
                    enabled = state.hasChanges && !state.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(text = if (isArabic) "حفظ" else "Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun SettingActionCard(
    title: String,
    subtitle: String,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Opens the Android system notification settings for this app so the user can choose the
 * notification sound natively (channel sound is system-owned on Android 8+). Device-local
 * only — nothing is stored by the app and no notification channels are modified here.
 */
private fun openSystemNotificationSettings(context: Context) {
    val primaryIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", context.packageName, null))
    }
    try {
        context.startActivity(primaryIntent)
    } catch (e: Exception) {
        // Fallback to the app details page if the notification settings screen is unavailable.
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null))
            )
        } catch (_: Exception) {
            // Nothing else we can safely do.
        }
    }
}
