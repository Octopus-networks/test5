package com.mithaq.app.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mithaq.app.GuardianTabContent
import com.mithaq.app.model.UserProfile
import com.mithaq.app.ui.guardian.GuardianViewModel

/**
 * "Guardian / Wali" screen reached from the Profile hub.
 * Lets the user invite and manage their guardian (wali) by reusing the existing
 * [GuardianTabContent] (invite form + current guardian status), which is already
 * beta-safe and shown inside ProfileSettings.
 *
 * NOTE: This is the user-side "invite my guardian" flow — NOT the gated wali-account
 * dashboard (LEGACY_WALI_DASHBOARD), which is a separate surface.
 */
@Composable
fun GuardianScreen(
    currentUser: UserProfile,
    viewModel: GuardianViewModel,
    isArabic: Boolean,
    onRefreshProfile: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        TextButton(onClick = onBack, modifier = Modifier.padding(start = 6.dp, top = 6.dp)) {
            Text(text = if (isArabic) "رجوع" else "Back")
        }
        Text(
            text = if (isArabic) "وليّ الأمر" else "Guardian / Wali",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        GuardianTabContent(
            currentUser = currentUser,
            viewModel = viewModel,
            isArabic = isArabic,
            onInviteSuccess = onRefreshProfile
        )
    }
}
