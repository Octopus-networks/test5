package com.mithaq.app.ui.notifications

import android.media.Ringtone
import android.media.RingtoneManager
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.mithaq.app.notification.NotificationCategory
import com.mithaq.app.notification.NotificationSound
import com.mithaq.app.notification.NotificationSoundPreferences

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

    var selectedSound by remember { mutableStateOf(NotificationSoundPreferences.getSelected(context)) }
    var showSoundDialog by remember { mutableStateOf(false) }
    var soundDialogCategory by remember { mutableStateOf<NotificationCategory?>(null) }
    var categoryOverrides by remember {
        mutableStateOf(
            NotificationCategory.overridable.associateWith { NotificationSoundPreferences.getOverride(context, it) }
        )
    }
    val previewHolder = remember { mutableStateOf<Ringtone?>(null) }
    fun stopPreview() {
        previewHolder.value?.let { ringtone ->
            try { if (ringtone.isPlaying) ringtone.stop() } catch (_: Exception) {}
        }
        previewHolder.value = null
    }
    fun previewSound(sound: NotificationSound) {
        stopPreview()
        val uri = NotificationSoundPreferences.soundUri(context, sound) ?: return
        try {
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.play()
            previewHolder.value = ringtone
        } catch (_: Exception) {}
    }
    DisposableEffect(Unit) { onDispose { stopPreview() } }

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

                // In-app, device-local notification sound (bundled app sounds). On Android 8+
                // the choice is applied by recreating the messages channel with the selected
                // sound. No Firestore / Functions / backend.
                SettingActionCard(
                    title = if (isArabic) "صوت الإشعار" else "Notification sound",
                    subtitle = soundDisplayName(selectedSound, isArabic),
                    onClick = { showSoundDialog = true }
                )

                if (showSoundDialog) {
                    NotificationSoundPickerDialog(
                        isArabic = isArabic,
                        selected = selectedSound,
                        onSelect = { sound ->
                            selectedSound = sound
                            NotificationSoundPreferences.setSelected(context, sound)
                            previewSound(sound)
                        },
                        onDismiss = {
                            stopPreview()
                            showSoundDialog = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Per-type sound: each category may override the app default sound above.
                Text(
                    text = if (isArabic) "صوت لكل نوع" else "Sound per type",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                NotificationCategory.overridable.forEach { category ->
                    val override = categoryOverrides[category]
                    SettingActionCard(
                        title = categoryDisplayName(category, isArabic),
                        subtitle = if (override == null) {
                            (if (isArabic) "الافتراضي: " else "Default: ") + soundDisplayName(selectedSound, isArabic)
                        } else {
                            soundDisplayName(override, isArabic)
                        },
                        onClick = { soundDialogCategory = category }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                soundDialogCategory?.let { category ->
                    CategorySoundPickerDialog(
                        isArabic = isArabic,
                        selected = categoryOverrides[category],
                        onSelect = { sound ->
                            categoryOverrides = categoryOverrides + (category to sound)
                            NotificationSoundPreferences.setOverride(context, category, sound)
                            previewSound(sound ?: selectedSound)
                        },
                        onDismiss = {
                            stopPreview()
                            soundDialogCategory = null
                        }
                    )
                }

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

private fun soundDisplayName(sound: NotificationSound, isArabic: Boolean): String = when (sound) {
    NotificationSound.DEFAULT -> if (isArabic) "افتراضي" else "Default"
    NotificationSound.CHIME -> if (isArabic) "رنّة" else "Chime"
    NotificationSound.DING -> if (isArabic) "دينغ" else "Ding"
    NotificationSound.SOFT -> if (isArabic) "ناعم" else "Soft"
    NotificationSound.SILENT -> if (isArabic) "صامت" else "Silent"
}

private fun categoryDisplayName(category: NotificationCategory, isArabic: Boolean): String = when (category) {
    NotificationCategory.INTEREST -> if (isArabic) "طلبات الاهتمام" else "Interest requests"
    NotificationCategory.PHOTO_REQUEST -> if (isArabic) "طلبات الصور" else "Photo requests"
    NotificationCategory.CHAT_REQUEST -> if (isArabic) "طلبات المحادثة" else "Chat requests"
    NotificationCategory.MESSAGE -> if (isArabic) "الرسائل" else "Messages"
    NotificationCategory.PHOTO_MODERATION -> if (isArabic) "مراجعة الصور" else "Photo review"
    NotificationCategory.GENERAL -> if (isArabic) "عام" else "General"
}

/** Sound picker for a category: includes a "Follow default" option (maps to a null override). */
@Composable
private fun CategorySoundPickerDialog(
    isArabic: Boolean,
    selected: NotificationSound?,
    onSelect: (NotificationSound?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (isArabic) "صوت الإشعار" else "Notification sound") },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(null) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(selected = selected == null, onClick = { onSelect(null) })
                    Text(
                        text = if (isArabic) "اتبع الافتراضي" else "Follow default",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                NotificationSound.entries.forEach { sound ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(sound) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(selected = sound == selected, onClick = { onSelect(sound) })
                        Text(
                            text = soundDisplayName(sound, isArabic),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = if (isArabic) "تم" else "Done")
            }
        }
    )
}

@Composable
private fun NotificationSoundPickerDialog(
    isArabic: Boolean,
    selected: NotificationSound,
    onSelect: (NotificationSound) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (isArabic) "صوت الإشعار" else "Notification sound") },
        text = {
            Column {
                NotificationSound.entries.forEach { sound ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(sound) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = sound == selected,
                            onClick = { onSelect(sound) }
                        )
                        Text(
                            text = soundDisplayName(sound, isArabic),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = if (isArabic) "تم" else "Done")
            }
        }
    )
}
