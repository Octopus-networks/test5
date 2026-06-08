package com.mithaq.app.ui.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mithaq.app.BuildConfig

/**
 * Support & Help (FAQ + contact).
 */
@Composable
fun SupportScreen(
    isArabic: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar Area
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (isArabic) "رجوع" else "Back"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isArabic) "الدعم والمساعدة" else "Support & Help",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // FAQ Section
        Text(
            text = if (isArabic) "الأسئلة الشائعة" else "FAQ",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 8.dp)
        )

        faqItems.forEach { item ->
            FaqCard(item = item, isArabic = isArabic)
        }

        // Contact Section
        Text(
            text = if (isArabic) "التواصل" else "Contact Us",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp)
        )

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isArabic) "تحتاج إلى المزيد من المساعدة؟" else "Need more help?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@mithaq.app")
                            putExtra(Intent.EXTRA_SUBJECT, if (isArabic) "طلب دعم - تطبيق ميثاق" else "Support Request - Mithaq App")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                if (isArabic) "لا يوجد تطبيق بريد إلكتروني" else "No email app found",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isArabic) "راسلنا عبر البريد" else "Email us")
                }
            }
        }

        // App Version
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "${if (isArabic) "الإصدار" else "Version"} ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private data class FaqItem(
    val questionEn: String,
    val questionAr: String,
    val answerEn: String,
    val answerAr: String
)

private val faqItems = listOf(
    FaqItem(
        "How does matching work?",
        "كيف تعمل المطابقة؟",
        "Matching is based on your profile preferences and Islamic compatibility criteria.",
        "تعتمد المطابقة على تفضيلات ملفك الشخصي ومعايير التوافق الإسلامية."
    ),
    FaqItem(
        "How is my privacy and photos protected?",
        "كيف تُحمى خصوصيتي وصوري؟",
        "Your photos can be blurred or hidden, and only shared with matches you approve.",
        "يمكن إخفاء أو تمويه صورك، ولا تُشارك إلا مع المطابقات التي توافق عليها."
    ),
    FaqItem(
        "What is the guardian (Wali) role?",
        "ما دور وليّ الأمر؟",
        "The Wali can optionally be involved in the chat or matching process to ensure Islamic guidelines.",
        "يمكن لولي الأمر المشاركة اختيارياً في المحادثات أو عملية المطابقة لضمان الالتزام بالضوابط الشرعية."
    ),
    FaqItem(
        "How do I report or block someone?",
        "كيف أبلّغ عن مستخدم أو أحظره؟",
        "You can use the report or block button on their profile or inside the chat screen.",
        "يمكنك استخدام زر الإبلاغ أو الحظر من ملفهم الشخصي أو داخل شاشة المحادثة."
    ),
    FaqItem(
        "How do I delete my account?",
        "كيف أحذف حسابي؟",
        "Go to Profile Settings -> Account -> Delete Account. This action is permanent.",
        "اذهب إلى إعدادات الملف الشخصي -> الحساب -> حذف الحساب. هذا الإجراء نهائي ولا يمكن التراجع عنه."
    )
)

@Composable
private fun FaqCard(item: FaqItem, isArabic: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "rotation"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isArabic) item.questionAr else item.questionEn,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotationState),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isArabic) item.answerAr else item.answerEn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
