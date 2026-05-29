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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class ProfileHubItem(
    val englishTitle: String,
    val arabicTitle: String,
    val englishSubtitle: String,
    val arabicSubtitle: String,
    val icon: ImageVector
)

private val profileItems = listOf(
    ProfileHubItem("My profile", "ملفي الشخصي", "Review profile completion and public details.", "راجع اكتمال الملف والبيانات الظاهرة.", Icons.Filled.Person),
    ProfileHubItem("Privacy settings", "إعدادات الخصوصية", "Control who can see sensitive fields.", "تحكم في من يرى البيانات الحساسة.", Icons.Filled.Lock),
    ProfileHubItem("Photo privacy", "خصوصية الصور", "Manage blurred and approved photo access.", "إدارة الصور المموهة والموافقات.", Icons.Filled.Visibility),
    ProfileHubItem("Guardian / Wali", "الولي", "Invite or manage guardian permissions.", "إضافة أو إدارة صلاحيات الولي.", Icons.Filled.CheckCircle),
    ProfileHubItem("Prayer settings", "إعدادات الصلاة", "Manage prayer time visibility and reminders.", "إدارة مواقيت الصلاة والظهور والتذكير.", Icons.Filled.Settings),
    ProfileHubItem("Notifications", "الإشعارات", "Choose important alerts only.", "اختر التنبيهات المهمة فقط.", Icons.Filled.Info),
    ProfileHubItem("Language", "اللغة", "Arabic and English ready.", "جاهز للعربية والإنجليزية.", Icons.Filled.Settings),
    ProfileHubItem("Account security", "أمان الحساب", "Email verification and safe account actions.", "توثيق البريد وإجراءات الحساب الآمنة.", Icons.Filled.Lock),
    ProfileHubItem("Help and support", "المساعدة والدعم", "Get help with account or safety concerns.", "احصل على مساعدة للحساب أو السلامة.", Icons.Filled.Info)
)

@Composable
fun MithaqProfileHubScreen(
    isArabic: Boolean,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Text(
            text = if (isArabic) "حسابي" else "Profile",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isArabic) "مركز منظم للخصوصية، الولي، الصلاة، وأمان الحساب."
            else "A calm hub for privacy, guardian, prayer, and account safety.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))
        profileItems.forEach { item ->
            ProfileHubRow(item = item, isArabic = isArabic)
            Spacer(modifier = Modifier.height(10.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isArabic) "تسجيل الخروج" else "Sign out",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ProfileHubRow(
    item: ProfileHubItem,
    isArabic: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
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
                    text = if (isArabic) item.arabicTitle else item.englishTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (isArabic) item.arabicSubtitle else item.englishSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
