package com.mithaq.app.ui.match

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mithaq.app.BetaFeatureGates
import com.mithaq.app.model.UserProfile
import com.mithaq.app.ui.common.MithaqQuestionArtwork
import com.mithaq.app.ui.common.mithaqQuestionArtworkSubtitle
import com.mithaq.app.ui.common.mithaqQuestionArtworkTitle
import com.mithaq.app.ui.common.mithaqQuestionVariant
import com.mithaq.app.ui.theme.LocalMithaqStrings

data class QuestionnaireQuestion(
    val id: String,
    val textAr: String,
    val textEn: String,
    val category: String, // "deen", "lifestyle", "family"
    val options: List<QuestionnaireOption>
)

data class QuestionnaireOption(
    val id: String,
    val textAr: String,
    val textEn: String
)

object QuestionnaireData {
    val questions = listOf(
        QuestionnaireQuestion(
            id = "q1",
            textEn = "What is your school of thought / Sect?",
            textAr = "ما هو المذهب أو التوجه الديني الذي تتبعه؟",
            category = "deen",
            options = listOf(
                QuestionnaireOption("opt1", "Sunni / سني", "Sunni / سني"),
                QuestionnaireOption("opt2", "Shia / شيعي", "Shia / شيعي"),
                QuestionnaireOption("opt3", "Ibadi / إباضي", "Ibadi / إباضي"),
                QuestionnaireOption("opt4", "Just Muslim / مسلم فقط", "Just Muslim / مسلم فقط")
            )
        ),
        QuestionnaireQuestion(
            id = "q2",
            textEn = "How strictly do you perform daily prayers?",
            textAr = "كيف تحافظ على الصلوات الخمس في اليوم؟",
            category = "deen",
            options = listOf(
                QuestionnaireOption("opt1", "Always on time / دائماً في وقتها", "Always on time / دائماً في وقتها"),
                QuestionnaireOption("opt2", "Usually / غالباً أحافظ عليها", "Usually / غالباً أحافظ عليها"),
                QuestionnaireOption("opt3", "Sometimes / أحياناً أتقاعس", "Sometimes / أحياناً أتقاعس"),
                QuestionnaireOption("opt4", "Trying to improve / أحاول الالتزام والتحسن", "Trying to improve / أحاول الالتزام والتحسن")
            )
        ),
        QuestionnaireQuestion(
            id = "q3",
            textEn = "What are your expectations for financial roles?",
            textAr = "ما هي رؤيتك لتوزيع المسؤولية المالية بين الزوجين؟",
            category = "lifestyle",
            options = listOf(
                QuestionnaireOption("opt1", "Husband is the sole provider / الزوج هو المسؤول المالي الكامل", "Husband is the sole provider / الزوج هو المسؤول المالي الكامل"),
                QuestionnaireOption("opt2", "Mutual sharing of expenses / المشاركة والتعاون المالي بين الطرفين", "Mutual sharing of expenses / المشاركة والتعاون المالي بين الطرفين"),
                QuestionnaireOption("opt3", "Depends on capabilities / يعتمد على مقدرة وظروف كل طرف", "Depends on capabilities / يعتمد على مقدرة وظروف كل طرف")
            )
        ),
        QuestionnaireQuestion(
            id = "q4",
            textEn = "Are you willing to relocate after marriage?",
            textAr = "هل لديك الرغبة أو الاستعداد للانتقال والعيش في مكان آخر بعد الزواج؟",
            category = "family",
            options = listOf(
                QuestionnaireOption("opt1", "Willing to relocate internationally / مستعد للانتقال دولياً", "Willing to relocate internationally / مستعد للانتقال دولياً"),
                QuestionnaireOption("opt2", "Within the same country only / داخل نفس البلد فقط", "Within the same country only / داخل نفس البلد فقط"),
                QuestionnaireOption("opt3", "No, prefer staying in my city / لا، أفضل الاستقرار في مدينتي", "No, prefer staying in my city / لا، أفضل الاستقرار في مدينتي"),
                QuestionnaireOption("opt4", "Open to discussion / الأمر قابل للنقاش والاتفاق", "Open to discussion / الأمر قابل للنقاش والاتفاق")
            )
        ),
        QuestionnaireQuestion(
            id = "q5",
            textEn = "What is your preference regarding career vs. homemaking?",
            textAr = "ما هو موقفك من عمل الزوجة خارج المنزل؟",
            category = "lifestyle",
            options = listOf(
                QuestionnaireOption("opt1", "Support career and ambition / دعم العمل والمسيرة المهنية للزوجة", "Support career and ambition / دعم العمل والمسيرة المهنية للزوجة"),
                QuestionnaireOption("opt2", "Prefer focus on homemaking / أفضل التفرغ للمنزل وتربية الأطفال", "Prefer focus on homemaking / أفضل التفرغ للمنزل وتربية الأطفال"),
                QuestionnaireOption("opt3", "Part-time or flexible working / العمل الجزئي أو المرن مقبول", "Part-time or flexible working / العمل الجزئي أو المرن مقبول")
            )
        ),
        QuestionnaireQuestion(
            id = "q6",
            textEn = "How involved should parents/family be in core decisions?",
            textAr = "ما مدى مشاركة الأهل والوالدين في القرارات الأساسية للزوجين؟",
            category = "family",
            options = listOf(
                QuestionnaireOption("opt1", "Highly involved & consulted / مشاركة كاملة واستشارة مستمرة للوالدين", "Highly involved & consulted / مشاركة كاملة واستشارة مستمرة للوالدين"),
                QuestionnaireOption("opt2", "Completely independent / استقلالية تامة للزوجين في القرارات", "Completely independent / استقلالية تامة للزوجين في القرارات"),
                QuestionnaireOption("opt3", "Balanced (consultation with autonomy) / توازن (استشارة الأهل مع استقلالية القرار)", "Balanced (consultation with autonomy) / توازن (استشارة الأهل مع استقلالية القرار)")
            )
        ),
        QuestionnaireQuestion(
            id = "q7",
            textEn = "What is your stance on Islamic modesty guidelines (Hijab/Niqab)?",
            textAr = "ما هو توجهك أو تفضيلك بشأن اللباس والالتزام بالضوابط الشرعية (الحجاب/النقاب)؟",
            category = "deen",
            options = listOf(
                QuestionnaireOption("opt1", "Strict Niqab / التزام كامل بالنقاب", "Strict Niqab / التزام كامل بالنقاب"),
                QuestionnaireOption("opt2", "Hijab / التزام بالحجاب الشرعي", "Hijab / التزام بالحجاب الشرعي"),
                QuestionnaireOption("opt3", "Moderate / Modest clothing / اللباس المحتشم العام", "Moderate / Modest clothing / اللباس المحتشم العام"),
                QuestionnaireOption("opt4", "Flexible / Open / مرونة وقابلية للنقاش", "Flexible / Open / مرونة وقابلية للنقاش")
            )
        ),
        QuestionnaireQuestion(
            id = "q8",
            textEn = "What is your perspective on polygamy?",
            textAr = "ما هو موقفك أو نظرتك للتعدد (الزوجة الثانية)؟",
            category = "lifestyle",
            options = listOf(
                QuestionnaireOption("opt1", "Acceptable / أقبل التعدد ومستعد له", "Acceptable / أقبل التعدد ومستعد له"),
                QuestionnaireOption("opt2", "Prefer monogamy / أفضل الاكتفاء بزوجة واحدة فقط", "Prefer monogamy / أفضل الاكتفاء بزوجة واحدة فقط"),
                QuestionnaireOption("opt3", "Strictly refuse / أرفض الفكرة تماماً", "Strictly refuse / أرفض الفكرة تماماً")
            )
        ),
        QuestionnaireQuestion(
            id = "q9",
            textEn = "How do you spend time acquiring religious knowledge?",
            textAr = "كيف تقضي وقتك في طلب العلم الشرعي وتطوير الجانب الديني؟",
            category = "deen",
            options = listOf(
                QuestionnaireOption("opt1", "Daily learning, lectures / تعلم يومي، استماع ومطالعة الدروس", "Daily learning, lectures / تعلم يومي، استماع ومطالعة الدروس"),
                QuestionnaireOption("opt2", "Basic practices and Friday sermon / الممارسات العامة وخطبة الجمعة فقط", "Basic practices and Friday sermon / الممارسات العامة وخطبة الجمعة فقط"),
                QuestionnaireOption("opt3", "Mainly cultural identity / هوية ثقافية واجتماعية عامة", "Mainly cultural identity / هوية ثقافية واجتماعية عامة")
            )
        ),
        QuestionnaireQuestion(
            id = "q10",
            textEn = "What is your preference regarding social gatherings?",
            textAr = "ما هي طبيعة التجمعات الاجتماعية والمناسبات التي تفضلها؟",
            category = "lifestyle",
            options = listOf(
                QuestionnaireOption("opt1", "Strictly gender-segregated / تجمعات عائلية منفصلة تماماً بين الجنسين", "Strictly gender-segregated / تجمعات عائلية منفصلة تماماً بين الجنسين"),
                QuestionnaireOption("opt2", "Mixed family gatherings / تجمعات عائلية مختلطة محافظة", "Mixed family gatherings / تجمعات عائلية مختلطة محافظة"),
                QuestionnaireOption("opt3", "Quiet / quiet home life preference / حياة هادئة وبيتوتية دون تجمعات كبرى", "Quiet / quiet home life preference / حياة هادئة وبيتوتية دون تجمعات كبرى")
            )
        )
    )

    fun calculateDetailedScore(userAnswers: Map<String, String>, partnerAnswers: Map<String, String>): DetailedMatchResult {
        var deenMatches = 0
        var deenTotal = 0
        var lifestyleMatches = 0
        var lifestyleTotal = 0
        var familyMatches = 0
        var familyTotal = 0

        questions.forEach { question ->
            val userAns = userAnswers[question.id]
            val partnerAns = partnerAnswers[question.id]
            if (userAns != null && partnerAns != null) {
                val isMatch = userAns == partnerAns
                when (question.category) {
                    "deen" -> {
                        deenTotal++
                        if (isMatch) deenMatches++
                    }
                    "lifestyle" -> {
                        lifestyleTotal++
                        if (isMatch) lifestyleMatches++
                    }
                    "family" -> {
                        familyTotal++
                        if (isMatch) familyMatches++
                    }
                }
            }
        }

        val deenPct = if (deenTotal > 0) (deenMatches.toFloat() / deenTotal * 100).toInt() else 80
        val lifestylePct = if (lifestyleTotal > 0) (lifestyleMatches.toFloat() / lifestyleTotal * 100).toInt() else 75
        val familyPct = if (familyTotal > 0) (familyMatches.toFloat() / familyTotal * 100).toInt() else 85
        
        val overallPct = ((deenPct + lifestylePct + familyPct) / 3)
        return DetailedMatchResult(overallPct, deenPct, lifestylePct, familyPct)
    }
}

data class DetailedMatchResult(
    val overall: Int,
    val deen: Int,
    val lifestyle: Int,
    val family: Int
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun QuestionnaireScreen(
    currentAnswers: Map<String, String>,
    isArabic: Boolean,
    onSaveAnswers: (Map<String, String>) -> Unit,
    onBack: () -> Unit
) {
    val questions = QuestionnaireData.questions
    var currentStep by remember { mutableStateOf(0) }
    val answers = remember { mutableStateMapOf<String, String>().apply { putAll(currentAnswers) } }
    
    val strings = LocalMithaqStrings.current
    val currentQuestion = questions[currentStep]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "${currentStep + 1} / ${questions.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = (currentStep + 1).toFloat() / questions.size,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Question Box with animation
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally { width -> if (targetState > initialState) width else -width } + fadeIn() with
                            slideOutHorizontally { width -> if (targetState > initialState) -width else width } + fadeOut()
                },
                modifier = Modifier.weight(1f)
            ) { step ->
                val q = questions[step]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    MithaqQuestionArtwork(
                        variant = mithaqQuestionVariant(q.category),
                        title = mithaqQuestionArtworkTitle(q.id, isArabic),
                        subtitle = mithaqQuestionArtworkSubtitle(q.id, isArabic)
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    Text(
                        text = if (isArabic) q.textAr else q.textEn,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = if (isArabic) TextAlign.Right else TextAlign.Left,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    q.options.forEach { option ->
                        val isSelected = answers[q.id] == option.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { answers[q.id] = option.id },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { answers[q.id] = option.id }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (isArabic) option.textAr else option.textEn,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface,
                                    textAlign = if (isArabic) TextAlign.Right else TextAlign.Left,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { if (currentStep > 0) currentStep-- },
                    enabled = currentStep > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = if (isArabic) "السابق" else "Back",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (currentStep < questions.size - 1) {
                    Button(
                        onClick = { if (answers.containsKey(currentQuestion.id)) currentStep++ },
                        enabled = answers.containsKey(currentQuestion.id)
                    ) {
                        Text(text = if (isArabic) "التالي" else "Next")
                    }
                } else {
                    Button(
                        onClick = { onSaveAnswers(answers.toMap()) },
                        enabled = answers.size >= questions.size,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Done, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (isArabic) "حفظ وإرسال" else "Save & Submit")
                    }
                }
            }
        }
    }
}

@Composable
fun CompatibilityBreakdownDialog(
    currentUser: UserProfile,
    partner: UserProfile,
    isArabic: Boolean,
    onDismiss: () -> Unit
) {
    val userAnswers = currentUser.questionnaireAnswers
    val partnerAnswers = partner.questionnaireAnswers
    val partnerName = partner.name

    val result = remember(userAnswers, partnerAnswers) {
        QuestionnaireData.calculateDetailedScore(userAnswers, partnerAnswers)
    }

    var aiLoading by remember { mutableStateOf(false) }
    var aiResult by remember { mutableStateOf<com.mithaq.app.service.GeminiService.CompatibilityResult?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser.uid, partner.uid) {
        val apiKey = com.mithaq.app.Config.GEMINI_API_KEY
        if (BetaFeatureGates.GEMINI_AI && apiKey.isNotEmpty() && apiKey != "YOUR_GEMINI_API_KEY") {
            aiLoading = true
            try {
                val service = com.mithaq.app.service.GeminiService(apiKey)
                val res = service.calculateCompatibility(currentUser, partner)
                aiResult = res
            } catch(e: Exception) {
                errorMsg = e.localizedMessage
            } finally {
                aiLoading = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = if (isArabic) "مؤشر التوافق التفصيلي" else "Detailed Compatibility Breakdown",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isArabic) "نسب التوافق مع $partnerName في مختلف الجوانب" 
                           else "Compatibility percentages with $partnerName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                // Canvas Circular Chart
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val colorDeen = MaterialTheme.colorScheme.primary
                    val colorLifestyle = Color(0xFFE28B15) // Amber
                    val colorFamily = Color(0xFF6B4FA8)    // Purple
                    
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 14.dp.toPx()
                        val size = Size(size.width - strokeWidth * 2, size.height - strokeWidth * 2)
                        
                        // Background track
                        drawCircle(
                            color = Color.LightGray.copy(alpha = 0.15f),
                            radius = size.width / 2f + strokeWidth / 2f,
                            style = Stroke(width = strokeWidth)
                        )

                        // 1. Deen Arc (Outer)
                        drawArc(
                            color = colorDeen,
                            startAngle = -90f,
                            sweepAngle = (result.deen.toFloat() / 100f) * 360f,
                            useCenter = false,
                            topLeft = Offset(strokeWidth, strokeWidth),
                            size = size,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // 2. Lifestyle Arc (Middle)
                        val strokeLifestyle = strokeWidth + 20.dp.toPx()
                        val sizeLifestyle = Size(size.width - 40.dp.toPx(), size.height - 40.dp.toPx())
                        drawArc(
                            color = colorLifestyle,
                            startAngle = -90f,
                            sweepAngle = (result.lifestyle.toFloat() / 100f) * 360f,
                            useCenter = false,
                            topLeft = Offset(strokeLifestyle, strokeLifestyle),
                            size = sizeLifestyle,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // 3. Family Arc (Inner)
                        val strokeFamily = strokeWidth + 40.dp.toPx()
                        val sizeFamily = Size(size.width - 80.dp.toPx(), size.height - 80.dp.toPx())
                        drawArc(
                            color = colorFamily,
                            startAngle = -90f,
                            sweepAngle = (result.family.toFloat() / 100f) * 360f,
                            useCenter = false,
                            topLeft = Offset(strokeFamily, strokeFamily),
                            size = sizeFamily,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // Inner overall match text
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${result.overall}%",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isArabic) "التوافق العام" else "Overall Match",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Legend cards
                CategoryLegendItem(
                    title = if (isArabic) "الالتزام والقيم (الدين)" else "Deen & Values",
                    percentage = result.deen,
                    color = MaterialTheme.colorScheme.primary,
                    isArabic = isArabic
                )
                CategoryLegendItem(
                    title = if (isArabic) "نمط الحياة والأهداف" else "Lifestyle & Goals",
                    percentage = result.lifestyle,
                    color = Color(0xFFE28B15),
                    isArabic = isArabic
                )
                CategoryLegendItem(
                    title = if (isArabic) "العلاقات العائلية والانتقال" else "Family & Relocation",
                    percentage = result.family,
                    color = Color(0xFF6B4FA8),
                    isArabic = isArabic
                )

                if (BetaFeatureGates.GEMINI_AI) {
                // AI Compatibility Report
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = if (isArabic) "تقرير التوافق بالذكاء الاصطناعي (AI)" else "AI Compatibility Report",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (com.mithaq.app.Config.GEMINI_API_KEY == "YOUR_GEMINI_API_KEY" || com.mithaq.app.Config.GEMINI_API_KEY.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isArabic) 
                                "تنبيه: ميزة الذكاء الاصطناعي غير مفعلة حالياً. يرجى تهيئة مفتاح GEMINI_API_KEY في ملف Config.kt لتفعيل تحليل التوافق الذكي."
                                else "Note: AI analysis is not active. Please configure GEMINI_API_KEY in Config.kt to enable smart compatibility analysis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (aiLoading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isArabic) "جاري تحليل التوافق ذكياً بواسطة Gemini AI..." else "Analyzing compatibility with Gemini AI...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (aiResult != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isArabic) "نسبة التوافق الذكي:" else "Smart Match Score:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${aiResult!!.score}%",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = aiResult!!.reason,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (errorMsg != null) {
                    Text(
                        text = if (isArabic) "فشل التحليل: $errorMsg" else "Analysis failed: $errorMsg",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (isArabic) "إغلاق" else "Close")
                }
            }
        }
    }
}

@Composable
fun CategoryLegendItem(
    title: String,
    percentage: Int,
    color: Color,
    isArabic: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
