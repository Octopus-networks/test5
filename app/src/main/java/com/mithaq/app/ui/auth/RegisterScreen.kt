package com.mithaq.app.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mithaq.app.model.*
import com.mithaq.app.ui.photo.UserProfileImage
import com.mithaq.app.ui.photo.BrotherhoodAvatars
import com.mithaq.app.ui.photo.SisterhoodAvatars
import com.mithaq.app.ui.filter.FlowRow
import com.mithaq.app.ui.common.MithaqQuestionArtwork
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import com.mithaq.app.BetaFeatureGates

/**
 * A highly-polished Multi-Step Registration Wizard for Mithaq.
 * - Step 1: Account credentials & demographics.
 * - Step 2: Gender preferences & Location/Age Cascading selections.
 * - Step 3: Granular Islamic & Lifestyle Details with Skips.
 * - Step 4: Wali invite, prayer consistency, and sign up.
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: (userId: String) -> Unit,
    viewModel: AuthViewModel,
    isArabic: Boolean,
    onLanguageChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = com.mithaq.app.ui.theme.LocalMithaqStrings.current
    val authState by viewModel.authState.collectAsState()
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Step state: 1 to 7
    var currentStep by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(1) }

    // Screen 1: Account Setup & Oath
    var username by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var name by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var email by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var age by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var gender by androidx.compose.runtime.saveable.rememberSaveable(stateSaver = androidx.compose.runtime.saveable.Saver(
        save = { it.name },
        restore = { runCatching { Gender.valueOf(it) }.getOrDefault(Gender.MALE) }
    )) { mutableStateOf(Gender.MALE) }
    var oathChecked by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var imageUrl by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("avatar_brother_green") }
    var localImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // Screen 2: Physical Attributes
    var height by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("170") }
    var weight by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("70") }
    var skinColor by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(if (isArabic) "حنطي" else "Medium") }
    var healthStatus by remember { mutableStateOf(emptyList<String>()) }

    // Screen 3: Demographics & Socio-Economic Status
    var nationality by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(if (isArabic) "السعودية" else "Saudi Arabia") }
    var maritalStatus by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("single") }
    var educationLevel by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("Bachelor") }
    var jobTitle by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var incomeLevel by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("Medium") }
    var languagesSpoken by remember { mutableStateOf(emptyList<String>()) }
    
    // Previous search preference presets
    var partnerGender by androidx.compose.runtime.saveable.rememberSaveable(stateSaver = androidx.compose.runtime.saveable.Saver(
        save = { it.name },
        restore = { runCatching { Gender.valueOf(it) }.getOrDefault(Gender.FEMALE) }
    )) { mutableStateOf(Gender.FEMALE) }
    var country by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("Saudi Arabia") }
    var stateProvince by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("Riyadh Region") }
    var city by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("Riyadh") }
    var minAgePreference by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(20) }
    var maxAgePreference by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(35) }

    // Screen 4: Religious & Lifestyle Habits
    var sect by androidx.compose.runtime.saveable.rememberSaveable(stateSaver = androidx.compose.runtime.saveable.Saver(
        save = { it.name },
        restore = { runCatching { Sect.valueOf(it) }.getOrDefault(Sect.SUNNI) }
    )) { mutableStateOf(Sect.SUNNI) }
    var prayerFrequency by androidx.compose.runtime.saveable.rememberSaveable(stateSaver = androidx.compose.runtime.saveable.Saver(
        save = { it.name },
        restore = { runCatching { PrayerFrequency.valueOf(it) }.getOrDefault(PrayerFrequency.ALWAYS) }
    )) { mutableStateOf(PrayerFrequency.ALWAYS) }
    var fastingHabit by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(if (isArabic) "دائماً" else "Always") }
    var smokeStatus by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("dont_smoke") }
    var alcoholStatus by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("dont_drink") }

    // Screen 5: Marriage Logistics & Future Plans
    var weddingTimeline by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("Within 6 months") }
    var livingSituation by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("Independent") }
    var relocationWillingness by androidx.compose.runtime.saveable.rememberSaveable(stateSaver = androidx.compose.runtime.saveable.Saver(
        save = { it.name },
        restore = { runCatching { RelocationWillingness.valueOf(it) }.getOrDefault(RelocationWillingness.OPEN) }
    )) { mutableStateOf(RelocationWillingness.OPEN) }
    var haveChildren by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("yes") }

    // Screen 6: Cultural Views & Financial Responsibilities
    var wifeWorking by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("Open") }
    var householdExpenses by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("Shared") }
    var aymaView by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("Negotiable") }
    var shabkaView by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("Negotiable") }
    var polygamyAcceptance by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

    // Screen 7: Profile Description & Privacy/Media
    var aboutYourself by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var idealPartner by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var gpsLocationEnabled by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var blurPictures by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }
    var additionalImages by remember { mutableStateOf(emptyList<String>()) }
    var waliName by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var waliEmail by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }

    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            localImageUri = uri
            imageUrl = uri.toString()
        }
    }

    var tempCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showPhotoOptionDialog by remember { mutableStateOf(false) }

    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = com.mithaq.app.ui.photo.CustomTakePictureContract()
    ) { success ->
        if (success) {
            tempCameraUri?.let { uri ->
                localImageUri = uri
                imageUrl = uri.toString()
            }
        }
    }

    val voiceRecorderManager = remember { com.mithaq.app.ui.photo.VoiceRecorderManager(context) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var localVoiceUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isPlayingVoice by remember { mutableStateOf(false) }

    var hasAudioPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
    }

    LaunchedEffect(gender) {
        imageUrl = if (gender == Gender.MALE) "avatar_brother_green" else "avatar_sister_teal"
        localImageUri = null
    }

    var localError by remember { mutableStateOf<String?>(null) }

    // React to success states
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onRegisterSuccess((authState as AuthState.Authenticated).userId)
        }
    }

    fun validateUsername(u: String): Boolean {
        if (u.length !in 3..14) return false
        val digits = u.filter { it.isDigit() }
        if (digits.length > 4) return false
        return u.all { it.isLetterOrDigit() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Language Switcher
        TextButton(
            onClick = { onLanguageChange(!isArabic) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp)
        ) {
            Text(
                text = if (isArabic) "English" else "العربية",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Wizard Header & Progress Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentStep > 1) {
                        IconButton(onClick = { currentStep-- }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    Text(
                        text = if (isArabic) "الخطوة $currentStep من 7" else "Step $currentStep of 7",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Spacer(modifier = Modifier.size(48.dp))
                }

                LinearProgressIndicator(
                    progress = currentStep / 7f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )

                // Animated step transitions
                AnimatedContent(targetState = currentStep) { step ->
                    when (step) {
                        1 -> {
                            // Screen 1: Account Setup & Oath
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isArabic) "إعداد الحساب والقسم الشرعي" else "Account & Religious Oath",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                MithaqQuestionArtwork(
                                    variant = "account",
                                    title = if (isArabic) "بداية موثوقة" else "Trusted beginning",
                                    subtitle = if (isArabic) "بيانات واضحة وقسم جاد قبل بناء الترشيحات." else "Clear details and serious intent before matching."
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = username,
                                    onValueChange = { username = it },
                                    label = { Text(if (isArabic) "اسم المستخدم (فريد)" else "Username (Unique)") },
                                    placeholder = { Text(if (isArabic) "3-14 أحرف، أرقام فقط" else "3-14 chars, no spaces/symbols") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text(strings.nameLabel) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text(strings.emailLabel) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text(strings.passwordLabel) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = age,
                                    onValueChange = { age = it },
                                    label = { Text(if (isArabic) "العمر (18 - 77)" else "Age (18 - 77)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                // Gender Card Selectors
                                Text(
                                    text = if (isArabic) "الجنس:" else "Gender:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(60.dp)
                                            .clickable {
                                                gender = Gender.MALE
                                                partnerGender = Gender.FEMALE
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (gender == Gender.MALE) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                        ),
                                        border = BorderStroke(
                                            width = if (gender == Gender.MALE) 2.dp else 1.dp,
                                            color = if (gender == Gender.MALE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (isArabic) "أخ (ذكر)" else "Brother (Male)",
                                                fontWeight = FontWeight.Bold,
                                                color = if (gender == Gender.MALE) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(60.dp)
                                            .clickable {
                                                gender = Gender.FEMALE
                                                partnerGender = Gender.MALE
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (gender == Gender.FEMALE) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                        ),
                                        border = BorderStroke(
                                            width = if (gender == Gender.FEMALE) 2.dp else 1.dp,
                                            color = if (gender == Gender.FEMALE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (isArabic) "أخت (أنثى)" else "Sister (Female)",
                                                fontWeight = FontWeight.Bold,
                                                color = if (gender == Gender.FEMALE) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Oath checkbox
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    Checkbox(
                                        checked = oathChecked,
                                        onCheckedChange = { oathChecked = it }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isArabic) "أقسم بالله العظيم أن استخدامي لهذا التطبيق هو لأغراض الزواج الشرعي، وأوافق على الشروط والأحكام" else "I swear to Allah Almighty that my use of this application is for marital purposes. I agree to the Terms & Conditions.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (oathChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = {
                                        val parsedAge = age.toIntOrNull()
                                        if (username.isBlank() || name.isBlank() || email.isBlank() || password.isBlank() || parsedAge == null) {
                                            localError = if (isArabic) "يرجى ملء جميع الحقول بشكل صحيح." else "Please fill in all fields correctly."
                                        } else if (!validateUsername(username)) {
                                            localError = if (isArabic) "اسم المستخدم غير صالح. يجب أن يتراوح طوله بين 3 و14 حرفاً، وبحد أقصى 4 أرقام، ولا يحتوي على مسافات أو رموز." else "Invalid username. Must be 3-14 chars, max 4 digits, and no spaces/symbols."
                                        } else if (parsedAge !in 18..77) {
                                            localError = if (isArabic) "عذرًا، يجب أن يكون عمرك بين 18 و 77 سنة للتسجيل." else "Sorry, you must be between 18 and 77 years to register."
                                        } else if (!oathChecked) {
                                            localError = if (isArabic) "يجب عليك الموافقة على القسم الشرعي للمتابعة." else "You must agree to the religious oath to proceed."
                                        } else {
                                            localError = null
                                            currentStep = 2
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(strings.next)
                                }
                            }
                        }

                        2 -> {
                            // Screen 2: Physical Attributes
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isArabic) "المواصفات الجسدية" else "Physical Attributes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                MithaqQuestionArtwork(
                                    variant = "body",
                                    title = if (isArabic) "ملامح تساعد على التوافق" else "Details that help compatibility",
                                    subtitle = if (isArabic) "معلومات محترمة تُعرض بخصوصية وبدون مبالغة." else "Respectful profile details, private and balanced."
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = height,
                                    onValueChange = { height = it },
                                    label = { Text(if (isArabic) "الطول (سم) (120 - 219)" else "Height (cm) (120 - 219)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = weight,
                                    onValueChange = { weight = it },
                                    label = { Text(if (isArabic) "الوزن (كجم) (30 - 119)" else "Weight (kg) (30 - 119)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                val skinOptions = if (isArabic) {
                                    listOf("أبيض", "حنطي فاتح", "حنطي", "حنطي غامق", "أسمر")
                                } else {
                                    listOf("Light", "Fair", "Medium", "Olive", "Dark")
                                }
                                SimpleDropdown(
                                    label = if (isArabic) "لون البشرة" else "Skin Color",
                                    options = skinOptions,
                                    selectedOption = skinColor,
                                    onOptionSelected = { skinColor = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = if (isArabic) "الحالة الصحية (اختر خيارين كحد أقصى):" else "Health Status (Select max 2):",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                val healthOptions = if (isArabic) {
                                    listOf("سليم", "مرض مزمن", "ذوي احتياجات خاصة", "غير ذلك")
                                } else {
                                    listOf("Healthy", "Chronic Disease", "Special Needs", "Other")
                                }

                                healthOptions.forEach { option ->
                                    val isChecked = healthStatus.contains(option)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isChecked) {
                                                    healthStatus = healthStatus - option
                                                } else if (healthStatus.size < 2) {
                                                    healthStatus = healthStatus + option
                                                }
                                            }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = {
                                                if (isChecked) {
                                                    healthStatus = healthStatus - option
                                                } else if (healthStatus.size < 2) {
                                                    healthStatus = healthStatus + option
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(option)
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    OutlinedButton(
                                        onClick = { currentStep = 1 },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(if (isArabic) "السابق" else "Back")
                                    }
                                    Button(
                                        onClick = {
                                            val hVal = height.toIntOrNull()
                                            val wVal = weight.toIntOrNull()
                                            if (hVal == null || hVal !in 120..219) {
                                                localError = if (isArabic) "يجب أن يكون الطول بين 120 و219 سم." else "Height must be between 120 and 219 cm."
                                            } else if (wVal == null || wVal !in 30..119) {
                                                localError = if (isArabic) "يجب أن يكون الوزن بين 30 و119 كجم." else "Weight must be between 30 and 119 kg."
                                            } else {
                                                localError = null
                                                currentStep = 3
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(if (isArabic) "التالي" else "Next")
                                    }
                                }
                            }
                        }

                        3 -> {
                            // Screen 3: Demographics & Socio-Economic Status
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isArabic) "البيانات الديموغرافية والاجتماعية" else "Demographics & Socio-Economic",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                MithaqQuestionArtwork(
                                    variant = "demographics",
                                    title = if (isArabic) "خريطة شخصية أوضح" else "A clearer personal map",
                                    subtitle = if (isArabic) "الجنسية والتعليم والعمل تساعدنا نرشح بذكاء." else "Nationality, education, and work improve thoughtful matching."
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                SimpleDropdown(
                                    label = if (isArabic) "الجنسية" else "Nationality",
                                    options = listOf("Saudi Arabia", "Egypt", "United Arab Emirates", "Jordan", "Syria", "Yemen", "Morocco", "Other"),
                                    selectedOption = nationality,
                                    onOptionSelected = { nationality = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                val maritalOptions = if (isArabic) {
                                    listOf("single" to "أعزب / عزباء", "divorced" to "مطلق / مطلقة", "widowed" to "أرمل / أرملة")
                                } else {
                                    listOf("single" to "Single", "divorced" to "Divorced", "widowed" to "Widowed")
                                }
                                SimpleDropdown(
                                    label = if (isArabic) "الحالة الاجتماعية" else "Marital Status",
                                    options = maritalOptions.map { it.second },
                                    selectedOption = maritalOptions.firstOrNull { it.first == maritalStatus }?.second ?: (if (isArabic) "أعزب / عزباء" else "Single"),
                                    onOptionSelected = { selected ->
                                        maritalStatus = maritalOptions.firstOrNull { it.second == selected }?.first ?: "single"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                SimpleDropdown(
                                    label = if (isArabic) "المستوى التعليمي" else "Education Level",
                                    options = listOf("High School", "Bachelor", "Master", "PhD", "Other"),
                                    selectedOption = educationLevel,
                                    onOptionSelected = { educationLevel = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = jobTitle,
                                    onValueChange = { jobTitle = it },
                                    label = { Text(if (isArabic) "المسمى الوظيفي" else "Job Title") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                SimpleDropdown(
                                    label = if (isArabic) "مستوى الدخل" else "Income Level",
                                    options = listOf("Low", "Medium", "High", "No Say"),
                                    selectedOption = incomeLevel,
                                    onOptionSelected = { incomeLevel = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = if (isArabic) "اللغات المتحدثة (اختر 3 كحد أقصى):" else "Languages Spoken (Select max 3):",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                val langOptions = listOf("Arabic", "English", "French", "Urdu", "Turkish", "Spanish", "German", "Other")
                                FlowRow(mainAxisSpacing = 6.dp, crossAxisSpacing = 6.dp) {
                                    langOptions.forEach { lang ->
                                        val isChecked = languagesSpoken.contains(lang)
                                        FilterChip(
                                            selected = isChecked,
                                            onClick = {
                                                if (isChecked) {
                                                    languagesSpoken = languagesSpoken - lang
                                                } else if (languagesSpoken.size < 3) {
                                                    languagesSpoken = languagesSpoken + lang
                                                }
                                            },
                                            label = { Text(lang) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    OutlinedButton(
                                        onClick = { currentStep = 2 },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(if (isArabic) "السابق" else "Back")
                                    }
                                    Button(
                                        onClick = {
                                            if (jobTitle.isBlank()) {
                                                localError = if (isArabic) "يرجى كتابة المسمى الوظيفي." else "Please enter your job title."
                                            } else {
                                                localError = null
                                                currentStep = 4
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(if (isArabic) "التالي" else "Next")
                                    }
                                }
                            }
                        }

                        4 -> {
                            // Screen 4: Religious & Lifestyle Habits
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isArabic) "العادات الدينية ونمط الحياة" else "Religious & Lifestyle Habits",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                MithaqQuestionArtwork(
                                    variant = "faith",
                                    title = if (isArabic) "قيم تظهر بهدوء" else "Values, shown calmly",
                                    subtitle = if (isArabic) "أسئلة الالتزام والعادات تُبنى للتفاهم لا للحكم." else "Faith and habits are asked for understanding, not judgment."
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = if (isArabic) "المذهب الديني:" else "Religious Beliefs / Sect:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Sect.values().forEach { s ->
                                        FilterChip(
                                            selected = sect == s,
                                            onClick = { sect = s },
                                            label = { Text(s.getDisplayName(isArabic)) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = strings.selectPrayer,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                FlowRow(mainAxisSpacing = 6.dp, crossAxisSpacing = 6.dp) {
                                    PrayerFrequency.values().forEach { pf ->
                                        FilterChip(
                                            selected = prayerFrequency == pf,
                                            onClick = { prayerFrequency = pf },
                                            label = { Text(pf.getDisplayName(isArabic)) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                SimpleDropdown(
                                    label = if (isArabic) "الالتزام بالصيام" else "Fasting Habit",
                                    options = listOf("Always", "Usually", "Sometimes", "Never"),
                                    selectedOption = fastingHabit,
                                    onOptionSelected = { fastingHabit = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                SimpleDropdown(
                                    label = if (isArabic) "موقف التدخين" else "Smoking Status",
                                    options = listOf("dont_smoke", "smoke", "occasionally"),
                                    selectedOption = smokeStatus,
                                    onOptionSelected = { smokeStatus = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                SimpleDropdown(
                                    label = if (isArabic) "شرب الكحول" else "Alcohol Consumption",
                                    options = listOf("dont_drink", "drink", "occasionally"),
                                    selectedOption = alcoholStatus,
                                    onOptionSelected = { alcoholStatus = it },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    OutlinedButton(
                                        onClick = { currentStep = 3 },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(if (isArabic) "السابق" else "Back")
                                    }
                                    Button(
                                        onClick = { currentStep = 5 },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(if (isArabic) "التالي" else "Next")
                                    }
                                }
                            }
                        }

                        5 -> {
                            // Screen 5: Marriage Logistics & Future Plans
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isArabic) "خطط الزواج والمستقبل" else "Marriage & Future Plans",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                MithaqQuestionArtwork(
                                    variant = "future",
                                    title = if (isArabic) "اتفاق قبل الطريق" else "Agreement before the path",
                                    subtitle = if (isArabic) "السكن، الانتقال، والأطفال تُطرح مبكرًا لتقليل المفاجآت." else "Home, relocation, and children are clarified early."
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                SimpleDropdown(
                                    label = if (isArabic) "موعد الزواج المتوقع" else "Wedding Timeline",
                                    options = listOf("Immediate", "Within 6 months", "Within a year", "More than a year"),
                                    selectedOption = weddingTimeline,
                                    onOptionSelected = { weddingTimeline = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                SimpleDropdown(
                                    label = if (isArabic) "ترتيبات السكن" else "Living Arrangement",
                                    options = listOf("Independent", "Family home", "Other"),
                                    selectedOption = livingSituation,
                                    onOptionSelected = { livingSituation = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = strings.selectRelocation,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                FlowRow(mainAxisSpacing = 6.dp, crossAxisSpacing = 6.dp) {
                                    RelocationWillingness.values().forEach { rw ->
                                        FilterChip(
                                            selected = relocationWillingness == rw,
                                            onClick = { relocationWillingness = rw },
                                            label = { Text(rw.getDisplayName(isArabic)) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                SimpleDropdown(
                                    label = if (isArabic) "الرغبة في الإنجاب" else "Desire for Kids",
                                    options = listOf("yes", "no", "not_sure"),
                                    selectedOption = haveChildren,
                                    onOptionSelected = { haveChildren = it },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    OutlinedButton(
                                        onClick = { currentStep = 4 },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(if (isArabic) "السابق" else "Back")
                                    }
                                    Button(
                                        onClick = { currentStep = 6 },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(if (isArabic) "التالي" else "Next")
                                    }
                                }
                            }
                        }

                        6 -> {
                            // Screen 6: Cultural Views & Financial Responsibilities
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isArabic) "المسؤوليات المالية والآراء الثقافية" else "Cultural & Financial Responsibilities",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                MithaqQuestionArtwork(
                                    variant = "culture",
                                    title = if (isArabic) "وضوح يحفظ الاحترام" else "Clarity that protects respect",
                                    subtitle = if (isArabic) "المال والعادات تُناقش بلطف قبل بداية التواصل." else "Money and customs are handled gently before chatting."
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                SimpleDropdown(
                                    label = if (isArabic) "عمل المرأة" else "Wife Working",
                                    options = listOf("Acceptable", "Not acceptable", "Open"),
                                    selectedOption = wifeWorking,
                                    onOptionSelected = { wifeWorking = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                SimpleDropdown(
                                    label = if (isArabic) "مصاريف المنزل" else "Household Expenses",
                                    options = listOf("Fully by Husband", "Shared", "Other"),
                                    selectedOption = householdExpenses,
                                    onOptionSelected = { householdExpenses = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                SimpleDropdown(
                                    label = if (isArabic) "الرأي في القائمة" else "View on El 2ayma",
                                    options = listOf("Yes/Important", "No/Not important", "Negotiable"),
                                    selectedOption = aymaView,
                                    onOptionSelected = { aymaView = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                SimpleDropdown(
                                    label = if (isArabic) "الرأي في الشبكة" else "View on Shabka",
                                    options = listOf("Yes/Important", "No/Not important", "Negotiable"),
                                    selectedOption = shabkaView,
                                    onOptionSelected = { shabkaView = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = strings.polygamyAcceptance,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Switch(checked = polygamyAcceptance, onCheckedChange = { polygamyAcceptance = it })
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    OutlinedButton(
                                        onClick = { currentStep = 5 },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(if (isArabic) "السابق" else "Back")
                                    }
                                    Button(
                                        onClick = { currentStep = 7 },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(if (isArabic) "التالي" else "Next")
                                    }
                                }
                            }
                        }

                        7 -> {
                            // Screen 7: Profile Description & Privacy/Media
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isArabic) "نبذة التعريف والوسائط" else "Bio, Privacy & Media",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                MithaqQuestionArtwork(
                                    variant = "bio",
                                    title = if (isArabic) "صورتك بكلماتك" else "Your profile, in your words",
                                    subtitle = if (isArabic) "اكتب بصدق، واختر الخصوصية والصورة المناسبة لك." else "Write honestly, then choose privacy and media settings."
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = aboutYourself,
                                    onValueChange = { aboutYourself = it },
                                    label = { Text(if (isArabic) "نبذة عن نفسي (80 - 250 حرفاً)" else "About Yourself (80 - 250 chars)") },
                                    supportingText = { Text("${aboutYourself.length} / 250") },
                                    minLines = 3,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = idealPartner,
                                    onValueChange = { idealPartner = it },
                                    label = { Text(if (isArabic) "مواصفات شريكي المثالي (80 - 250 حرفاً)" else "My Ideal Partner (80 - 250 chars)") },
                                    supportingText = { Text("${idealPartner.length} / 250") },
                                    minLines = 3,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // GPS Location Switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isArabic) "تفعيل الموقع الجغرافي (GPS):" else "Enable GPS Location:",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Switch(checked = gpsLocationEnabled, onCheckedChange = { gpsLocationEnabled = it })
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                // Blur my pictures Switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isArabic) "تعتيم صوري الشخصية افتراضياً:" else "Blur My Photos by Default:",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Switch(checked = blurPictures, onCheckedChange = { blurPictures = it })
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                // Guardian Info
                                Text(
                                    text = if (isArabic) "دعوة ولي الأمر / المشرف الشرعي (اختياري):" else "Invite a Guardian (Optional):",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = waliName,
                                    onValueChange = { waliName = it },
                                    label = { Text(if (isArabic) "اسم ولي الأمر" else "Guardian Name") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = waliEmail,
                                    onValueChange = { waliEmail = it },
                                    label = { Text(if (isArabic) "البريد الإلكتروني لولي الأمر" else "Guardian Email") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                // Profile Avatar / Upload selection
                                Text(
                                    text = if (isArabic) "الصورة الشخصية الرئيسية:" else "Primary Profile Photo:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(com.mithaq.app.ui.theme.AvatarSize.Large)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        UserProfileImage(
                                            imageUrl = imageUrl,
                                            gender = gender,
                                            isBlurred = false,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isArabic) "اختر رمزاً أو ارفع صورة:" else "Choose Avatar or Upload Photo:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            val avatars = if (gender == Gender.MALE) BrotherhoodAvatars else SisterhoodAvatars
                                            avatars.forEach { pair ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(pair.second)
                                                        .clickable { imageUrl = pair.first }
                                                        .border(
                                                            width = if (imageUrl == pair.first) 2.dp else 0.dp,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            shape = CircleShape
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedButton(
                                    onClick = { showPhotoOptionDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (isArabic) "رفع صورة شخصية رئيسية" else "Upload Primary Profile Photo")
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                if (showPhotoOptionDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showPhotoOptionDialog = false },
                                        title = { Text(if (isArabic) "اختر طريقة الرفع" else "Choose Upload Method") },
                                        text = { Text(if (isArabic) "هل تريد التقاط الصورة بالكاميرا أم اختيارها من المعرض؟" else "Would you like to take a photo with the camera or choose from gallery?") },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    showPhotoOptionDialog = false
                                                    val uri = getCameraImageUri(context)
                                                    tempCameraUri = uri
                                                    try {
                                                        cameraLauncher.launch(uri)
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
                                                    showPhotoOptionDialog = false
                                                    galleryLauncher.launch(
                                                        androidx.activity.result.PickVisualMediaRequest(
                                                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                                        )
                                                    )
                                                }
                                            ) {
                                                Text(if (isArabic) "المعرض" else "Gallery")
                                            }
                                        }
                                    )
                                }

                                if (BetaFeatureGates.VOICE_INTRO) {
                                // Voice Introduction
                                Text(
                                    text = if (isArabic) "التعريف الصوتي (اختياري):" else "Voice Intro (Optional):",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (!hasAudioPermission) {
                                                permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                            } else {
                                                if (isRecordingVoice) {
                                                    voiceRecorderManager.stopRecording()
                                                    isRecordingVoice = false
                                                } else {
                                                    val voiceFile = java.io.File(context.cacheDir, "temp_voice_intro.mp4")
                                                    voiceRecorderManager.startRecording(voiceFile)
                                                    localVoiceUri = android.net.Uri.fromFile(voiceFile)
                                                    isRecordingVoice = true
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isRecordingVoice) Color.Red else MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = if (isRecordingVoice) {
                                                if (isArabic) "إيقاف التسجيل" else "Stop Recording"
                                            } else {
                                                if (isArabic) "سجل مقطعاً صوتياً 🎙️" else "Record Voice 🎙️"
                                            }
                                        )
                                    }

                                    if (localVoiceUri != null) {
                                        Button(
                                            onClick = {
                                                if (isPlayingVoice) {
                                                    voiceRecorderManager.stopPlaying()
                                                    isPlayingVoice = false
                                                } else {
                                                    isPlayingVoice = true
                                                    voiceRecorderManager.startPlaying(localVoiceUri.toString()) {
                                                        isPlayingVoice = false
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondary
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = if (isPlayingVoice) {
                                                    if (isArabic) "إيقاف" else "Stop"
                                                } else {
                                                    if (isArabic) "تشغيل ▶️" else "Play ▶️"
                                                }
                                            )
                                        }
                                    }
                                }

                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { currentStep = 6 },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(if (isArabic) "السابق" else "Back")
                                    }

                                    Button(
                                        onClick = {
                                            if (aboutYourself.length !in 80..250) {
                                                localError = if (isArabic) "يجب أن تكون النبذة عن نفسك بين 80 و 250 حرفاً." else "About Yourself must be between 80 and 250 characters."
                                            } else if (idealPartner.length !in 80..250) {
                                                localError = if (isArabic) "يجب أن تكون مواصفات الشريك بين 80 و 250 حرفاً." else "My Ideal Partner must be between 80 and 250 characters."
                                            } else {
                                                localError = null
                                                val ageInt = age.toIntOrNull() ?: 25
                                                val hInt = height.toIntOrNull() ?: 170
                                                val wInt = weight.toIntOrNull() ?: 70
                                                val userProfile = UserProfile(
                                                    name = name,
                                                    gender = gender,
                                                    age = ageInt,
                                                    city = city,
                                                    country = country,
                                                    imageUrl = imageUrl,
                                                    sect = sect,
                                                    prayerFrequency = prayerFrequency,
                                                    modestyPreference = if (gender == Gender.FEMALE) ModestyPreference.HIJAB else ModestyPreference.DOES_NOT_MATTER,
                                                    relocationWillingness = relocationWillingness,
                                                    polygamyAcceptance = polygamyAcceptance,
                                                    
                                                    username = username,
                                                    oathChecked = oathChecked,
                                                    skinColor = skinColor,
                                                    healthStatus = healthStatus,
                                                    nationality = nationality,
                                                    educationLevel = educationLevel,
                                                    jobTitle = jobTitle,
                                                    incomeLevel = incomeLevel,
                                                    fastingHabit = fastingHabit,
                                                    weddingTimeline = weddingTimeline,
                                                    wifeWorking = wifeWorking,
                                                    householdExpenses = householdExpenses,
                                                    aymaView = aymaView,
                                                    shabkaView = shabkaView,
                                                    gpsLocationEnabled = gpsLocationEnabled,
                                                    blurPictures = blurPictures,

                                                    profileCreator = "self",
                                                    regionalCode = "MUS",
                                                    height = hInt,
                                                    weight = wInt,
                                                    bodyType = "average",
                                                    smokeStatus = smokeStatus,
                                                    haveChildren = haveChildren,
                                                    languagesSpoken = languagesSpoken,
                                                    guardianName = if (waliName.isNotBlank()) waliName else null,
                                                    guardianEmail = if (waliEmail.isNotBlank()) waliEmail else null,
                                                    guardianStatus = if (waliEmail.isNotBlank()) "PENDING" else "None",
                                                    aboutYourself = aboutYourself,
                                                    idealPartner = idealPartner
                                                )
                                                viewModel.signUp(
                                                    email = email,
                                                    passwordPass = password,
                                                    profile = userProfile,
                                                    localImageUri = localImageUri,
                                                    localVoiceUri = localVoiceUri,
                                                    context = context
                                                )
                                            }
                                        },
                                        enabled = authState !is AuthState.Loading,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (authState is AuthState.Loading) {
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(strings.registerAccount, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Error Message block
                val errorToDisplay = localError ?: (authState as? AuthState.Error)?.errorMessage
                if (errorToDisplay != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorToDisplay,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Link to Login
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.alreadyHaveAccount,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(
                        onClick = onNavigateToLogin,
                        enabled = authState !is AuthState.Loading
                    ) {
                        Text(
                            text = strings.signIn,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
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

@Composable
private fun SimpleDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown"
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}


