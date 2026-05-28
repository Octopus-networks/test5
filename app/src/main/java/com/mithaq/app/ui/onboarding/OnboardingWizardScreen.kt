package com.mithaq.app.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mithaq.app.ui.auth.AuthViewModel
import com.mithaq.app.ui.guardian.GuardianViewModel
import com.mithaq.app.ui.guardian.GuardianUiState
import com.mithaq.app.ui.match.QuestionnaireData
import com.mithaq.app.ui.match.QuestionnaireQuestion
import com.mithaq.app.ui.match.QuestionnaireOption
import com.mithaq.app.ui.common.MithaqQuestionArtwork
import com.mithaq.app.ui.common.mithaqQuestionArtworkSubtitle
import com.mithaq.app.ui.common.mithaqQuestionArtworkTitle
import com.mithaq.app.ui.common.mithaqQuestionVariant
import com.mithaq.app.ui.theme.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingWizardScreen(
    authViewModel: AuthViewModel,
    guardianViewModel: GuardianViewModel,
    isArabic: Boolean,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(1) } // 1: Gender, 2: ID, 3: Compatibility, 4: Guardian
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val scrollState = rememberScrollState()

    // Glassmorphism styling based on theme
    val glassBgColor = if (isDark) GlassSurfaceDark else GlassSurfaceLight
    val glassBorderColor = if (isDark) GlassBorderDark else GlassBorderLight

    // Step 2 State: Verification
    var idCardUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selfieUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isSubmittingVer by remember { mutableStateOf(false) }
    var verStatusMsg by remember { mutableStateOf<String?>(null) }

    var tempIdCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var tempSelfieCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showIdOptionDialog by remember { mutableStateOf(false) }
    var showSelfieOptionDialog by remember { mutableStateOf(false) }

    val idCardLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) idCardUri = uri
    }

    val idCardCameraLauncher = rememberLauncherForActivityResult(
        contract = com.mithaq.app.ui.photo.CustomTakePictureContract()
    ) { success ->
        if (success) {
            tempIdCameraUri?.let { idCardUri = it }
        }
    }

    val selfieLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) selfieUri = uri
    }

    val selfieCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) {
            tempSelfieCameraUri?.let { selfieUri = it }
        }
    }

    // Step 3 State: Questionnaire
    val questions = QuestionnaireData.questions
    var currentQuestionIdx by remember { mutableStateOf(0) }
    val answers = remember { mutableStateMapOf<String, String>() }

    // Step 4 State: Guardian Invite
    var guardianName by remember { mutableStateOf("") }
    var guardianEmail by remember { mutableStateOf("") }
    val guardianState by guardianViewModel.uiState.collectAsState()

    // UI texts depending on language
    val titleText = if (isArabic) "أهلاً بك في ميثاق" else "Welcome to Mithaq"
    val subtitleText = if (isArabic) "لنبدأ رحلتك لبناء زواج مبارك ومترابط" else "Let's start your journey to a blessed marriage"
    val skipText = if (isArabic) "تخطي الآن" else "Skip for now"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(Color(0xFF0F1713), Color(0xFF071F15))
                    } else {
                        listOf(Color(0xFFFDFBF7), Color(0xFFE6F3ED))
                    }
                )
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Premium Header & Title
            Text(
                text = titleText,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else PrimaryEmeraldDark,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // Step Progress Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepIndicator(step = 1, active = currentStep >= 1, completed = currentStep > 1, label = if (isArabic) "الجنس" else "Gender", isArabic = isArabic)
                Divider(modifier = Modifier.weight(1f).padding(horizontal = 4.dp), color = if (currentStep > 1) PrimaryEmeraldLight else MaterialTheme.colorScheme.outlineVariant, thickness = 2.dp)
                StepIndicator(step = 2, active = currentStep >= 2, completed = currentStep > 2, label = if (isArabic) "التوثيق" else "ID Check", isArabic = isArabic)
                Divider(modifier = Modifier.weight(1f).padding(horizontal = 4.dp), color = if (currentStep > 2) PrimaryEmeraldLight else MaterialTheme.colorScheme.outlineVariant, thickness = 2.dp)
                StepIndicator(step = 3, active = currentStep >= 3, completed = currentStep > 3, label = if (isArabic) "التوافق" else "Quiz", isArabic = isArabic)
                Divider(modifier = Modifier.weight(1f).padding(horizontal = 4.dp), color = if (currentStep > 3) PrimaryEmeraldLight else MaterialTheme.colorScheme.outlineVariant, thickness = 2.dp)
                StepIndicator(step = 4, active = currentStep >= 4, completed = false, label = if (isArabic) "ولي الأمر" else "Guardian", isArabic = isArabic)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Premium Glassmorphic container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(glassBgColor)
                    .border(1.dp, glassBorderColor, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { width -> width } + fadeIn() with
                                    slideOutHorizontally { width -> -width } + fadeOut()
                        } else {
                            slideInHorizontally { width -> -width } + fadeIn() with
                                    slideOutHorizontally { width -> width } + fadeOut()
                        }.using(SizeTransform(clip = false))
                    }
                ) { step ->
                    when (step) {
                        1 -> {
                            val currentUserProfile by authViewModel.currentUserProfile.collectAsState()
                            StepGenderSelectionContent(
                                isArabic = isArabic,
                                initialGender = currentUserProfile?.gender ?: com.mithaq.app.model.Gender.MALE,
                                onGenderSelected = { selectedGender ->
                                    authViewModel.updateGender(selectedGender, context)
                                    currentStep = 2
                                }
                            )
                        }
                        2 -> {
                            StepVerificationContent(
                                isArabic = isArabic,
                                idCardUri = idCardUri,
                                selfieUri = selfieUri,
                                isSubmitting = isSubmittingVer,
                                statusMsg = verStatusMsg,
                                onSelectId = { showIdOptionDialog = true },
                                onSelectSelfie = { showSelfieOptionDialog = true },
                                onSubmit = {
                                    if (idCardUri == null || selfieUri == null) {
                                        verStatusMsg = if (isArabic) "يرجى اختيار كل من صورة الهوية والصورة الشخصية." else "Please select both ID Card and Selfie."
                                        return@StepVerificationContent
                                    }
                                    isSubmittingVer = true
                                    verStatusMsg = if (isArabic) "جاري التحقق الفوري باستخدام الذكاء الاصطناعي..." else "Analyzing with instant AI Face Recognition..."
                                    authViewModel.submitVerification(idCardUri!!, selfieUri!!, context) { success, message ->
                                        isSubmittingVer = false
                                        verStatusMsg = message
                                        if (success) {
                                            currentStep = 3
                                        }
                                    }
                                }
                            )

                            if (showIdOptionDialog) {
                                AlertDialog(
                                    onDismissRequest = { showIdOptionDialog = false },
                                    title = { Text(if (isArabic) "اختر طريقة الرفع" else "Choose Upload Method") },
                                    text = { Text(if (isArabic) "هل تريد التقاط صورة الهوية بالكاميرا أم اختيارها من المعرض؟" else "Would you like to take a photo of your ID with the camera or choose from gallery?") },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                showIdOptionDialog = false
                                                val uri = getCameraImageUri(context)
                                                tempIdCameraUri = uri
                                                try {
                                                    idCardCameraLauncher.launch(uri)
                                                } catch (e: Exception) {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        if (isArabic) "عذرًا، فشل فتح الكاميرا: ${e.localizedMessage}" else "Sorry, failed to open camera: ${e.localizedMessage}",
                                                        android.widget.Toast.LENGTH_LONG
                                                     ).show()
                                                }
                                            }
                                        ) {
                                            Text(if (isArabic) "الكاميرا" else "Camera")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(
                                            onClick = {
                                                showIdOptionDialog = false
                                                idCardLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                            }
                                        ) {
                                            Text(if (isArabic) "المعرض" else "Gallery")
                                        }
                                    }
                                )
                            }

                            if (showSelfieOptionDialog) {
                                AlertDialog(
                                    onDismissRequest = { showSelfieOptionDialog = false },
                                    title = { Text(if (isArabic) "توثيق الفيديو الحي" else "Selfie Video Verification") },
                                    text = {
                                        Text(
                                            if (isArabic)
                                                "يرجى تسجيل فيديو سيلفي قصير (3 ثوانٍ) وأنت تحرك رأسك أو تلوح بيدك لإثبات الهوية الحية (Liveness)."
                                            else
                                                "Please record a short selfie video (3 seconds) waving or nodding to confirm live verification."
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                showSelfieOptionDialog = false
                                                val uri = getCameraVideoUri(context)
                                                tempSelfieCameraUri = uri
                                                try {
                                                    selfieCameraLauncher.launch(uri)
                                                } catch (e: Exception) {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        if (isArabic) "عذرًا، فشل تسجيل الفيديو: ${e.localizedMessage}" else "Sorry, failed to start video recording: ${e.localizedMessage}",
                                                        android.widget.Toast.LENGTH_LONG
                                                     ).show()
                                                }
                                            }
                                        ) {
                                            Text(if (isArabic) "تسجيل فيديو" else "Record Video")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(
                                            onClick = {
                                                showSelfieOptionDialog = false
                                                selfieLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                            }
                                        ) {
                                            Text(if (isArabic) "اختيار فيديو" else "Choose Video")
                                        }
                                    }
                                )
                            }
                        }
                        3 -> StepQuestionnaireContent(
                            questions = questions,
                            currentQuestionIdx = currentQuestionIdx,
                            answers = answers,
                            isArabic = isArabic,
                            onAnswerSelected = { qId, optId ->
                                answers[qId] = optId
                                if (currentQuestionIdx < questions.size - 1) {
                                    currentQuestionIdx++
                                } else {
                                    // Save compatibility answers to ViewModel
                                    authViewModel.saveQuestionnaireAnswers(answers.toMap())
                                    currentStep = 4
                                }
                            },
                            onPrevQuestion = {
                                if (currentQuestionIdx > 0) {
                                    currentQuestionIdx--
                                } else {
                                    currentStep = 2
                                }
                            }
                        )
                        4 -> StepGuardianContent(
                            isArabic = isArabic,
                            guardianName = guardianName,
                            guardianEmail = guardianEmail,
                            uiState = guardianState,
                            onNameChange = { guardianName = it },
                            onEmailChange = { guardianEmail = it },
                            onSendInvitation = {
                                guardianViewModel.inviteGuardian(guardianName, guardianEmail)
                            },
                            onPrev = { currentStep = 3 },
                            onFinish = onComplete
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            // Footer navigation options: Skip for now
            // Gender selection (Step 1) is mandatory, so hide skip button on Step 1
            if (currentStep > 1) {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = skipText,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StepGenderSelectionContent(
    isArabic: Boolean,
    initialGender: com.mithaq.app.model.Gender,
    onGenderSelected: (com.mithaq.app.model.Gender) -> Unit
) {
    var selectedGender by remember { mutableStateOf(initialGender) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (isArabic) "الخطوة 1: تحديد الجنس" else "Step 1: Select Gender",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = if (isArabic) 
                "يرجى اختيار جنسك بدقة. يظهر للرجال النساء فقط، وللنساء الرجال فقط لضمان بيئة شرعية ومحافظة."
            else 
                "Please select your gender. Males only see females, and females only see males to ensure a serious and Islamic environment.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Male Option
            val isMale = selectedGender == com.mithaq.app.model.Gender.MALE
            val maleBg = if (isMale) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            val maleBorder = if (isMale) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            
            OutlinedButton(
                onClick = { selectedGender = com.mithaq.app.model.Gender.MALE },
                modifier = Modifier
                    .weight(1f)
                    .height(140.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, maleBorder),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = maleBg)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Male,
                        contentDescription = "Male",
                        tint = if (isMale) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isArabic) "ذكر" else "Male",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isMale) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Female Option
            val isFemale = selectedGender == com.mithaq.app.model.Gender.FEMALE
            val femaleBg = if (isFemale) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            val femaleBorder = if (isFemale) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

            OutlinedButton(
                onClick = { selectedGender = com.mithaq.app.model.Gender.FEMALE },
                modifier = Modifier
                    .weight(1f)
                    .height(140.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, femaleBorder),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = femaleBg)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Female,
                        contentDescription = "Female",
                        tint = if (isFemale) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isArabic) "أنثى" else "Female",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isFemale) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onGenderSelected(selectedGender) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (isArabic) "حفظ ومتابعة" else "Save & Continue",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun StepIndicator(
    step: Int,
    active: Boolean,
    completed: Boolean,
    label: String,
    isArabic: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (completed) PrimaryEmeraldLight
                    else if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            if (completed) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = step.toString(),
                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StepVerificationContent(
    isArabic: Boolean,
    idCardUri: android.net.Uri?,
    selfieUri: android.net.Uri?,
    isSubmitting: Boolean,
    statusMsg: String?,
    onSelectId: () -> Unit,
    onSelectSelfie: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (isArabic) "الخطوة 2: توثيق الهوية والوجه" else "Step 2: Face & ID Verification",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = if (isArabic) 
                "يرجى توثيق حسابك للحصول على شارة التوثيق. هذا يضمن بيئة آمنة وجدية لجميع الأعضاء."
            else 
                "Please verify your identity to get the verification badge. This ensures a safe and serious environment for everyone.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onSelectId,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (idCardUri != null) PrimaryEmeraldLight.copy(alpha = 0.15f) else Color.Transparent
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 8.dp)) {
                    Icon(
                        imageVector = if (idCardUri != null) Icons.Default.CheckCircle else Icons.Default.Face,
                        contentDescription = null,
                        tint = if (idCardUri != null) SuccessGreen else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (idCardUri != null)
                            (if (isArabic) "تم اختيار الهوية" else "ID Selected")
                        else
                            (if (isArabic) "صورة الهوية" else "ID Card Photo"),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp
                    )
                }
            }

            OutlinedButton(
                onClick = onSelectSelfie,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (selfieUri != null) PrimaryEmeraldLight.copy(alpha = 0.15f) else Color.Transparent
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 8.dp)) {
                    Icon(
                        imageVector = if (selfieUri != null) Icons.Default.CheckCircle else Icons.Default.Videocam,
                        contentDescription = null,
                        tint = if (selfieUri != null) SuccessGreen else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (selfieUri != null)
                            (if (isArabic) "تم تسجيل الفيديو" else "Video Recorded")
                        else
                            (if (isArabic) "فيديو سيلفي" else "Selfie Video"),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (statusMsg != null) {
            Text(
                text = statusMsg,
                style = MaterialTheme.typography.bodySmall,
                color = if (statusMsg.contains("نجاح") || statusMsg.contains("success") || statusMsg.contains("قيد") || statusMsg.contains("approved")) SuccessGreen else MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
        }

        Button(
            onClick = onSubmit,
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    text = if (isArabic) "تحقق وتابع" else "Verify & Continue",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StepQuestionnaireContent(
    questions: List<QuestionnaireQuestion>,
    currentQuestionIdx: Int,
    answers: Map<String, String>,
    isArabic: Boolean,
    onAnswerSelected: (String, String) -> Unit,
    onPrevQuestion: () -> Unit
) {
    val currentQuestion = questions[currentQuestionIdx]
    val selectedOption = answers[currentQuestion.id]

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (isArabic) "الخطوة 3: استبيان التوافق" else "Step 3: Compatibility Quiz",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isArabic) "سؤال ${currentQuestionIdx + 1} من ${questions.size}" else "Question ${currentQuestionIdx + 1} of ${questions.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Linear Progress Indicator for the Quiz
        LinearProgressIndicator(
            progress = (currentQuestionIdx + 1).toFloat() / questions.size,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        MithaqQuestionArtwork(
            variant = mithaqQuestionVariant(currentQuestion.category),
            title = mithaqQuestionArtworkTitle(currentQuestion.id, isArabic),
            subtitle = mithaqQuestionArtworkSubtitle(currentQuestion.id, isArabic)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (isArabic) currentQuestion.textAr else currentQuestion.textEn,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Options List
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            currentQuestion.options.forEach { option ->
                val isSelected = selectedOption == option.id
                val optionBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                val optionBorder = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(optionBg)
                        .border(1.5.dp, optionBorder, RoundedCornerShape(16.dp))
                        .clickable { onAnswerSelected(currentQuestion.id, option.id) }
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isArabic) option.textAr else option.textEn,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            TextButton(onClick = onPrevQuestion) {
                Icon(
                    imageVector = if (isArabic) Icons.Default.ArrowForward else Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isArabic) "السابق" else "Back")
            }
        }
    }
}

@Composable
fun StepGuardianContent(
    isArabic: Boolean,
    guardianName: String,
    guardianEmail: String,
    uiState: GuardianUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSendInvitation: () -> Unit,
    onPrev: () -> Unit,
    onFinish: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (isArabic) "الخطوة 4: إشراك ولي الأمر" else "Step 4: Guardian Invitation",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = if (isArabic)
                "لتأكيد الجدية وتيسير التواصل الشرعي، يرجى دعوة ولي أمرك لمتابعة المحادثات."
            else
                "To ensure seriousness and facilitate halal chaperoned chats, please invite your guardian (Wali) to oversee interactions.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = guardianName,
            onValueChange = onNameChange,
            label = { Text(if (isArabic) "اسم ولي الأمر" else "Guardian Name") },
            placeholder = { Text(if (isArabic) "أدخل الاسم الكامل" else "Enter full name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = guardianEmail,
            onValueChange = onEmailChange,
            label = { Text(if (isArabic) "البريد الإلكتروني لولي الأمر" else "Guardian Email") },
            placeholder = { Text(if (isArabic) "email@example.com" else "email@example.com") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        when (uiState) {
            is GuardianUiState.Error -> {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp),
                    textAlign = TextAlign.Center
                )
            }
            is GuardianUiState.Success -> {
                Text(
                    text = if (isArabic) "تم إرسال دعوة ولي الأمر بنجاح!" else "Guardian invite sent successfully!",
                    color = SuccessGreen,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp),
                    textAlign = TextAlign.Center
                )
            }
            is GuardianUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).padding(bottom = 12.dp),
                    strokeWidth = 2.dp
                )
            }
            else -> {}
        }

        val context = androidx.compose.ui.platform.LocalContext.current

        if (uiState is GuardianUiState.Success) {
            Button(
                onClick = {
                    sendWaliEmailInvitation(context, guardianName, guardianEmail, isArabic)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text(text = if (isArabic) "أرسل بريد الدعوة الآن" else "Send Email Invite Now", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (uiState is GuardianUiState.Success) {
                    onFinish()
                } else if (guardianName.isNotBlank() && guardianEmail.isNotBlank()) {
                    onSendInvitation()
                } else {
                    onFinish()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            val buttonText = if (uiState is GuardianUiState.Success) {
                if (isArabic) "إنهاء الإعداد" else "Finish Setup"
            } else if (guardianName.isNotBlank() && guardianEmail.isNotBlank()) {
                if (isArabic) "إرسال الدعوة والمتابعة" else "Send Invite & Continue"
            } else {
                if (isArabic) "إنهاء وتخطي ولي الأمر حالياً" else "Finish & Skip Guardian for now"
            }
            Text(text = buttonText, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            TextButton(onClick = onPrev) {
                Icon(
                    imageVector = if (isArabic) Icons.Default.ArrowForward else Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isArabic) "السابق" else "Back")
            }
        }
    }
}

private fun getCameraImageUri(context: android.content.Context): android.net.Uri {
    val directory = java.io.File(context.cacheDir, "camera")
    if (!directory.exists()) {
        directory.mkdirs()
    }
    val file = java.io.File(directory, "camera_capture_${System.currentTimeMillis()}.jpg")
    return androidx.core.content.FileProvider.getUriForFile(
        context,
        "com.mithaq.app.provider",
        file
    )
}

private fun getCameraVideoUri(context: android.content.Context): android.net.Uri {
    val directory = java.io.File(context.cacheDir, "camera")
    if (!directory.exists()) {
        directory.mkdirs()
    }
    val file = java.io.File(directory, "camera_capture_${System.currentTimeMillis()}.mp4")
    return androidx.core.content.FileProvider.getUriForFile(
        context,
        "com.mithaq.app.provider",
        file
    )
}

private fun sendWaliEmailInvitation(
    context: android.content.Context,
    guardianName: String,
    guardianEmail: String,
    isArabic: Boolean
) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:")
            putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(guardianEmail.trim().lowercase()))
            putExtra(android.content.Intent.EXTRA_SUBJECT, if (isArabic) "دعوة إشراك ولي أمر - تطبيق ميثاق" else "Guardian Invitation - Mithaq App")
            putExtra(android.content.Intent.EXTRA_TEXT, if (isArabic) {
                "السلام عليكم يا $guardianName،\n\nلقد قمت بإضافتك كولي أمر لي في تطبيق ميثاق للزواج الإسلامي المحافظ.\n\nيرجى تحميل التطبيق والتسجيل فيه بحساب ولي أمر (Guardian) باستخدام بريدك الإلكتروني هذا ($guardianEmail) لمتابعة المحادثات والطلبات بشكل مباشر وآمن لضمان بيئة شرعية ومحافظة.\n\nرابط تحميل التطبيق: https://github.com/ahmedbebars/test5/raw/main/Mithaq-v2.0-Release.apk\n\nشكراً لك."
            } else {
                "Assalamu Alaikum $guardianName,\n\nI have invited you to be my Guardian (Wali) on the Mithaq matchmaking application.\n\nPlease download the app and register a Guardian account using this email address ($guardianEmail) to oversee my chats and requests securely.\n\nApp Download Link: https://github.com/ahmedbebars/test5/raw/main/Mithaq-v2.0-Release.apk\n\nThank you."
            })
        }
        val chooser = android.content.Intent.createChooser(intent, if (isArabic) "اختر تطبيق البريد الإلكتروني" else "Choose Email Client")
        context.startActivity(chooser)
    } catch (e: Exception) {
        android.widget.Toast.makeText(
            context,
            if (isArabic) "تعذر فتح تطبيق البريد" else "Could not open email application",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}
