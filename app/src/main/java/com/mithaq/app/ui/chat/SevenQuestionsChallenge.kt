package com.mithaq.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HelpCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mithaq.app.model.UserProfile

data class ChallengeQuestion(
    val id: Int,
    val textAr: String,
    val textEn: String
)

object ChallengeData {
    val questions = listOf(
        ChallengeQuestion(1, "ما هي رؤيتك لإدارة ميزانية الأسرة والمسؤوليات المالية؟", "What is your vision for managing the family budget and financial responsibilities?"),
        ChallengeQuestion(2, "ما هي تطلعاتك فيما يخص تربية الأطفال والتعليم الديني لهم؟", "What are your aspirations regarding raising children and their religious education?"),
        ChallengeQuestion(3, "كيف تفضل قضاء وقت الفراغ والإجازات السنوية؟", "How do you prefer to spend your free time and annual vacations?"),
        ChallengeQuestion(4, "ما هي حدود الخصوصية وتدخل الأهل في حياتنا الزوجية من وجهة نظرك؟", "What are the limits of privacy and family involvement in our married life from your perspective?"),
        ChallengeQuestion(5, "كيف تتعامل مع ضغوط العمل وهل تؤثر على حياتك الشخصية؟", "How do you handle work stress and does it affect your personal life?"),
        ChallengeQuestion(6, "ما هو تعريفك للنجاح في العلاقة الزوجية؟", "What is your definition of success in a marital relationship?"),
        ChallengeQuestion(7, "هل لديك خطط لمتابعة الدراسات العليا أو تغيير المسار الوظيفي مستقبلاً؟", "Do you have plans to pursue higher studies or change your career path in the future?")
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SevenQuestionsChallengeScreen(
    partner: UserProfile,
    isArabic: Boolean,
    onBack: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    var answers by remember { mutableStateOf(mutableMapOf<Int, String>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isArabic) "تحدي الـ 7 أسئلة" else "The 7 Questions Challenge") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress
            LinearProgressIndicator(
                progress = { (currentStep + 1) / 7f },
                modifier = Modifier.fillMaxWidth().height(8.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            if (currentStep < 7) {
                val q = ChallengeData.questions[currentStep]
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Default.HelpCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isArabic) q.textAr else q.textEn,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                var currentAnswer by remember(currentStep) { mutableStateOf("") }

                OutlinedTextField(
                    value = currentAnswer,
                    onValueChange = { currentAnswer = it },
                    label = { Text(if (isArabic) "إجابتك الصادقة..." else "Your honest answer...") },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(16.dp)
                )

                Button(
                    onClick = {
                        if (currentAnswer.isNotBlank()) {
                            answers[q.id] = currentAnswer
                            currentStep++
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isArabic) "التالي" else "Next")
                }
            } else {
                // Completed State
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isArabic) "تم حفظ إجاباتك بنجاح!" else "Your answers are saved!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isArabic) "ستظهر الإجابات لك ولـ ${partner.name} فور قيامه بالإجابة أيضاً." else "Answers will be revealed once ${partner.name} answers them too.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isArabic) "العودة للمحادثة" else "Back to Chat")
                }
            }
        }
    }
}
