package com.mithaq.app.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Security settings (app lock via biometric / device credential).
 *
 * NOTE: Phase 0 stub. The full implementation is handed off to a coding agent
 * (Codex / Antigravity) per the task spec. The agent must keep this exact
 * signature and only edit this file. Existing helper to use:
 * [com.mithaq.app.security.BiometricAuthManager].
 */
@Composable
fun SecuritySettingsScreen(
    currentUserId: String,
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
            Text(text = if (isArabic) "رجوع" else "Back")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isArabic) "الأمان" else "Security",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isArabic)
                "إعدادات الأمان (قفل التطبيق بالبصمة) قيد التطوير."
            else
                "Security settings (biometric app lock) are under development.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
