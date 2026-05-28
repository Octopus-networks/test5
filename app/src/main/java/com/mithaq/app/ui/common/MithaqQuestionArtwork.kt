package com.mithaq.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun MithaqQuestionArtwork(
    variant: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val style = rememberArtworkStyle(variant)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(style.backgroundStart, style.backgroundEnd)
                )
            )
            .border(1.dp, style.accent.copy(alpha = 0.28f), RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = style.accent.copy(alpha = 0.20f),
                radius = size.minDimension * 0.42f,
                center = Offset(size.width * 0.86f, size.height * 0.14f)
            )
            drawCircle(
                color = style.secondary.copy(alpha = 0.18f),
                radius = size.minDimension * 0.28f,
                center = Offset(size.width * 0.12f, size.height * 0.88f)
            )
            drawLine(
                color = style.accent.copy(alpha = 0.35f),
                start = Offset(size.width * 0.52f, size.height * 0.18f),
                end = Offset(size.width * 0.94f, size.height * 0.78f),
                strokeWidth = 5.dp.toPx()
            )
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.26f))
                    .border(1.dp, Color.White.copy(alpha = 0.42f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.86f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private data class ArtworkStyle(
    val backgroundStart: Color,
    val backgroundEnd: Color,
    val accent: Color,
    val secondary: Color,
    val icon: ImageVector
)

private fun rememberArtworkStyle(variant: String): ArtworkStyle {
    return when (variant) {
        "account" -> ArtworkStyle(Color(0xFF123A36), Color(0xFF1B6B61), Color(0xFFF2C94C), Color(0xFF6FCF97), Icons.Default.Lock)
        "body" -> ArtworkStyle(Color(0xFF32424A), Color(0xFF607D8B), Color(0xFFFFB86B), Color(0xFF80DEEA), Icons.Default.Person)
        "demographics" -> ArtworkStyle(Color(0xFF23395B), Color(0xFF4979B8), Color(0xFF9AE6B4), Color(0xFFFFD166), Icons.Default.Public)
        "faith" -> ArtworkStyle(Color(0xFF2B3A2E), Color(0xFF5D7C45), Color(0xFFF4D35E), Color(0xFFB7E4C7), Icons.Default.Star)
        "future" -> ArtworkStyle(Color(0xFF3E3147), Color(0xFF8C5E9E), Color(0xFFFFC857), Color(0xFFB8E0D2), Icons.Default.Home)
        "culture" -> ArtworkStyle(Color(0xFF3B2F2F), Color(0xFF8A5A44), Color(0xFFFFD6A5), Color(0xFF95D5B2), Icons.Default.Work)
        "bio" -> ArtworkStyle(Color(0xFF293241), Color(0xFF3D5A80), Color(0xFFFFB4A2), Color(0xFF98C1D9), Icons.Default.Favorite)
        "location" -> ArtworkStyle(Color(0xFF1F3D4D), Color(0xFF3B8EA5), Color(0xFFFFD166), Color(0xFFA8DADC), Icons.Default.LocationOn)
        "deen" -> ArtworkStyle(Color(0xFF173B33), Color(0xFF2F7D66), Color(0xFFF2C94C), Color(0xFF8FE3CF), Icons.Default.Star)
        "family" -> ArtworkStyle(Color(0xFF442C2E), Color(0xFF9A5C66), Color(0xFFFFCDB2), Color(0xFFBDE0FE), Icons.Default.Home)
        "lifestyle" -> ArtworkStyle(Color(0xFF263238), Color(0xFF607D8B), Color(0xFFFFC857), Color(0xFFB2DFDB), Icons.Default.Work)
        else -> ArtworkStyle(Color(0xFF1B4332), Color(0xFF40916C), Color(0xFFF2C94C), Color(0xFFB7E4C7), Icons.Default.CheckCircle)
    }
}

fun mithaqQuestionVariant(category: String): String {
    return when (category) {
        "deen" -> "deen"
        "family" -> "family"
        "lifestyle" -> "lifestyle"
        else -> "bio"
    }
}

fun mithaqQuestionArtworkTitle(questionId: String, isArabic: Boolean): String {
    return when (questionId) {
        "q1" -> if (isArabic) "هوية دينية واضحة" else "Clear faith identity"
        "q2" -> if (isArabic) "إيقاع الصلاة" else "Prayer rhythm"
        "q3" -> if (isArabic) "وضوح مالي" else "Financial clarity"
        "q4" -> if (isArabic) "الانتقال والاستقرار" else "Relocation comfort"
        "q5" -> if (isArabic) "العمل والبيت" else "Work and home"
        "q6" -> if (isArabic) "صوت العائلة" else "Family voice"
        "q7" -> if (isArabic) "الاحتشام والاختيار" else "Modesty preference"
        "q8" -> if (isArabic) "رؤية الزواج" else "Marriage outlook"
        "q9" -> if (isArabic) "طريق التعلم" else "Learning path"
        "q10" -> if (isArabic) "طبيعة المناسبات" else "Social rhythm"
        else -> if (isArabic) "سؤال توافق" else "Compatibility question"
    }
}

fun mithaqQuestionArtworkSubtitle(questionId: String, isArabic: Boolean): String {
    return when (questionId) {
        "q1" -> if (isArabic) "نقطة بداية محترمة لفهم القيم." else "A respectful starting point for values."
        "q2" -> if (isArabic) "يساعدنا نفهم نمط الالتزام اليومي." else "Helps understand daily commitment."
        "q3" -> if (isArabic) "تقليل الحساسية يبدأ من وضوح التوقعات." else "Clear expectations reduce tension."
        "q4" -> if (isArabic) "الاستقرار قرار مشترك قبل التواصل." else "Stability is a shared decision."
        "q5" -> if (isArabic) "توازن الحياة والعمل من أهم نقاط التفاهم." else "Work-life balance is a key match point."
        "q6" -> if (isArabic) "نحترم وجود الأهل وحدود القرار." else "Honors family presence and boundaries."
        "q7" -> if (isArabic) "اختيارات اللباس تُذكر بلطف واحترام." else "Modesty preferences stay gentle and respectful."
        "q8" -> if (isArabic) "رؤية مبكرة تمنع سوء الفهم لاحقًا." else "Early clarity prevents later confusion."
        "q9" -> if (isArabic) "مساحة لفهم النمو الديني بهدوء." else "A calm view of spiritual growth."
        "q10" -> if (isArabic) "نمط الحياة الاجتماعي جزء من الراحة." else "Social rhythm shapes everyday comfort."
        else -> if (isArabic) "سؤال يساعد على ترشيح أنسب." else "A question that improves matching."
    }
}
