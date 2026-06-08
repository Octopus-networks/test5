package com.mithaq.app.ui.profile

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * "Privacy settings" screen reached from the Profile hub.
 *
 * Controls which mirrored fields are exposed in the public discovery profile.
 * Toggles are saved under profiles/{uid}.privacyTrust (a group already allow-listed by
 * Firestore rules); the mirrorPublicProfile Cloud Function reads those flags and blanks the
 * corresponding fields in publicProfiles/{uid}.
 *
 * Fields covered are exactly the sensitive ones present in the public mirror:
 * age, location (city/country), marital status, marriage timeline.
 */
@Composable
fun PrivacySettingsScreen(
    currentUserId: String,
    isArabic: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val firestore = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()

    var showAge by remember(currentUserId) { mutableStateOf(true) }
    var showLocation by remember(currentUserId) { mutableStateOf(true) }
    var showMaritalStatus by remember(currentUserId) { mutableStateOf(true) }
    var showMarriageTimeline by remember(currentUserId) { mutableStateOf(true) }
    var loaded by remember(currentUserId) { mutableStateOf(false) }

    // Load current privacy prefs from profiles/{uid}.privacy
    LaunchedEffect(currentUserId) {
        try {
            val snap = firestore.collection("profiles").document(currentUserId).get().await()
            @Suppress("UNCHECKED_CAST")
            val privacy = snap.get("privacyTrust") as? Map<String, Any?> ?: emptyMap()
            showAge = (privacy["showAge"] as? Boolean) ?: true
            showLocation = (privacy["showLocation"] as? Boolean) ?: true
            showMaritalStatus = (privacy["showMaritalStatus"] as? Boolean) ?: true
            showMarriageTimeline = (privacy["showMarriageTimeline"] as? Boolean) ?: true
        } catch (_: Exception) {
            // Keep defaults (all visible) on read failure.
        } finally {
            loaded = true
        }
    }

    suspend fun persist() {
        val privacy = mapOf(
            "showAge" to showAge,
            "showLocation" to showLocation,
            "showMaritalStatus" to showMaritalStatus,
            "showMarriageTimeline" to showMarriageTimeline
        )
        try {
            firestore.collection("profiles")
                .document(currentUserId)
                .set(mapOf("privacyTrust" to privacy), SetOptions.merge())
                .await()
        } catch (_: Exception) {
            Toast.makeText(
                context,
                if (isArabic) "تعذّر حفظ الإعداد" else "Couldn't save setting",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

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
            text = if (isArabic) "الخصوصية" else "Privacy settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isArabic)
                "تحكّم في الحقول التي تظهر للآخرين في صفحتك العامة."
            else
                "Control which fields appear to others on your public profile.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                PrivacyToggleRow(
                    label = if (isArabic) "إظهار العمر" else "Show age",
                    checked = showAge,
                    enabled = loaded,
                    onCheckedChange = { showAge = it; scope.launch { persist() } }
                )
                HorizontalDivider()
                PrivacyToggleRow(
                    label = if (isArabic) "إظهار الموقع (المدينة والبلد)" else "Show location (city & country)",
                    checked = showLocation,
                    enabled = loaded,
                    onCheckedChange = { showLocation = it; scope.launch { persist() } }
                )
                HorizontalDivider()
                PrivacyToggleRow(
                    label = if (isArabic) "إظهار الحالة الاجتماعية" else "Show marital status",
                    checked = showMaritalStatus,
                    enabled = loaded,
                    onCheckedChange = { showMaritalStatus = it; scope.launch { persist() } }
                )
                HorizontalDivider()
                PrivacyToggleRow(
                    label = if (isArabic) "إظهار توقيت الزواج" else "Show marriage timeline",
                    checked = showMarriageTimeline,
                    enabled = loaded,
                    onCheckedChange = { showMarriageTimeline = it; scope.launch { persist() } }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (isArabic)
                "قد يستغرق التحديث في صفحتك العامة لحظات."
            else
                "Changes to your public profile may take a moment to apply.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PrivacyToggleRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}
