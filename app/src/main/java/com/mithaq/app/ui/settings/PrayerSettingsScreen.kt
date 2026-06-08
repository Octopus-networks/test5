package com.mithaq.app.ui.settings

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
import com.mithaq.app.model.UserProfile
import com.mithaq.app.ui.auth.AuthViewModel

/**
 * Dedicated "Prayer settings" screen reached from the Profile hub.
 * Reuses the existing [AdhanSettingsSectionFixed] section (adhan alerts,
 * calculation method, sound, location) so prayer settings live in one place.
 */
@Composable
fun PrayerSettingsScreen(
    currentUser: UserProfile,
    authViewModel: AuthViewModel,
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
            text = if (isArabic) "إعدادات الصلاة" else "Prayer settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        AdhanSettingsSectionFixed(
            currentUser = currentUser,
            authViewModel = authViewModel,
            isArabic = isArabic
        )
    }
}
