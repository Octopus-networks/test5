package com.mithaq.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.mithaq.app.security.BiometricAuthManager

@Composable
fun SecuritySettingsScreen(
    currentUserId: String,
    isArabic: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("mithaq_security_prefs", android.content.Context.MODE_PRIVATE)
    }
    val appLockKey = remember(currentUserId) { "app_lock_enabled_$currentUserId" }
    var appLockEnabled by remember(currentUserId, prefs) {
        mutableStateOf(prefs.getBoolean(appLockKey, false))
    }
    var unavailableMessageVisible by remember(currentUserId) { mutableStateOf(false) }

    fun persistAppLock(enabled: Boolean) {
        prefs.edit().putBoolean(appLockKey, enabled).apply()
        appLockEnabled = enabled
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        TextButton(onClick = onBack) {
            Text(text = if (isArabic) "رجوع" else "Back")
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (isArabic) "الأمان" else "Security",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(18.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isArabic) "قفل التطبيق بالبصمة" else "Biometric app lock",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = appLockEnabled,
                        onCheckedChange = { requestedEnabled ->
                            if (!requestedEnabled) {
                                unavailableMessageVisible = false
                                persistAppLock(false)
                                return@Switch
                            }

                            val activity = context as? FragmentActivity
                            val biometricManager = BiometricAuthManager(context)
                            if (activity == null || !biometricManager.isBiometricAvailable()) {
                                persistAppLock(false)
                                unavailableMessageVisible = true
                                return@Switch
                            }

                            unavailableMessageVisible = false
                            biometricManager.showBiometricPrompt(
                                activity = activity,
                                title = if (isArabic) "تفعيل قفل التطبيق" else "Enable app lock",
                                subtitle = if (isArabic) {
                                    "استخدم المصادقة البيومترية لتأمين ميثاق"
                                } else {
                                    "Use biometric authentication to secure Mithaq"
                                },
                                onSuccess = {
                                    unavailableMessageVisible = false
                                    persistAppLock(true)
                                },
                                onError = {
                                    persistAppLock(false)
                                }
                            )
                        }
                    )
                }
                if (unavailableMessageVisible) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isArabic) {
                            "لا تتوفر مصادقة بيومترية على هذا الجهاز"
                        } else {
                            "Biometric authentication is not available on this device"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
