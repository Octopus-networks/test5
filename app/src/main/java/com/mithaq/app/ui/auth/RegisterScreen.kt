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
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke

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

    // Step state: 1 = Core Credentials, 2 = Gender/Location preferences, 3 = Profile Details (Muslima values), 4 = Finalize & Register
    var currentStep by remember { mutableStateOf(1) }

    // Account Inputs (Step 1)
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(Gender.MALE) }
    var imageUrl by remember { mutableStateOf("avatar_brother_green") }
    var localImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // Step 2 variables
    var partnerGender by remember { mutableStateOf(Gender.FEMALE) }
    var country by remember { mutableStateOf("Saudi Arabia") }
    var stateProvince by remember { mutableStateOf("Riyadh Region") }
    var city by remember { mutableStateOf("Riyadh") }
    var minAgePreference by remember { mutableStateOf(20) }
    var maxAgePreference by remember { mutableStateOf(35) }

    // Step 3 variables (granularity with skips)
    var sect by remember { mutableStateOf(Sect.SUNNI) }
    var ethnicity by remember { mutableStateOf("arab_middle_eastern") }
    var smokeStatus by remember { mutableStateOf("dont_smoke") }
    var haveChildren by remember { mutableStateOf("no") }
    var bodyType by remember { mutableStateOf("average") }
    var modestyPreference by remember { mutableStateOf(ModestyPreference.HIJAB) }

    // Step 4 variables
    var prayerFrequency by remember { mutableStateOf(PrayerFrequency.ALWAYS) }
    var relocationWillingness by remember { mutableStateOf(RelocationWillingness.OPEN) }
    var polygamyAcceptance by remember { mutableStateOf(false) }
    var waliName by remember { mutableStateOf("") }
    var waliEmail by remember { mutableStateOf("") }

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
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    Text(
                        text = if (isArabic) "الخطوة $currentStep من 4" else "Step $currentStep of 4",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Spacer(modifier = Modifier.size(48.dp))
                }

                LinearProgressIndicator(
                    progress = currentStep / 4f,
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
                            // Step 1 Layout: Account details
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = strings.registerTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))

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
                                    label = { Text(if (isArabic) "العمر" else "Age") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                // Profile Photo Selection UI
                                Text(
                                    text = if (isArabic) "الصورة الشخصية" else "Profile Photo",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
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
                                            text = if (isArabic) "اختر رمزاً محتشماً:" else "Choose Modest Avatar:",
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
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Text(if (isArabic) "إضافة صورة شخصية" else "Add Profile Photo")
                                }

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

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = {
                                        val parsedAge = age.toIntOrNull()
                                        if (name.isBlank() || email.isBlank() || password.isBlank() || parsedAge == null) {
                                            localError = if (isArabic) "يرجى ملء جميع الحقول الأساسية بشكل صحيح." else "Please fill in all core fields correctly."
                                        } else if (parsedAge < 18) {
                                            localError = if (isArabic) "عذرًا، يجب أن يكون عمرك 18 سنة أو أكثر للتسجيل في ميثاق." else "Sorry, you must be 18 years or older to register on Mithaq."
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
                            // Step 2 Layout: Gender Preferences & Cascading Locations/Ages
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isArabic) "خيارات الشريك والموقع" else "Partner preferences & Location",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                // Gender preference card selector
                                Text(
                                    text = if (isArabic) "أنا:" else "I am a:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(80.dp)
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
                                                text = if (isArabic) "أخ (ذكر)" else "Brother / Male",
                                                fontWeight = FontWeight.Bold,
                                                color = if (gender == Gender.MALE) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(80.dp)
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
                                                text = if (isArabic) "أخت (أنثى)" else "Sister / Female",
                                                fontWeight = FontWeight.Bold,
                                                color = if (gender == Gender.FEMALE) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = if (isArabic) "أبحث عن:" else "Looking for a:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                                        Text(
                                            text = if (partnerGender == Gender.FEMALE) {
                                                if (isArabic) "أخت (أنثى)" else "Sister / Female"
                                            } else {
                                                if (isArabic) "أخ (ذكر)" else "Brother / Male"
                                            },
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Cascading Location Dropdowns
                                Text(
                                    text = if (isArabic) "الموقع الجغرافي:" else "Your Location:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val countriesList = listOf("Saudi Arabia", "Egypt", "United Arab Emirates", "Other")
                                val statesMap = mapOf(
                                    "Saudi Arabia" to listOf("Riyadh Region", "Makkah Region", "Eastern Province", "Other"),
                                    "Egypt" to listOf("Cairo Governorate", "Giza Governorate", "Alexandria Governorate", "Other"),
                                    "United Arab Emirates" to listOf("Dubai", "Abu Dhabi", "Sharjah", "Other"),
                                    "Other" to listOf("Other")
                                )
                                val citiesMap = mapOf(
                                    "Riyadh Region" to listOf("Riyadh", "Al Kharj", "Other"),
                                    "Makkah Region" to listOf("Makkah", "Jeddah", "Taif", "Other"),
                                    "Eastern Province" to listOf("Dammam", "Khobar", "Jubail", "Other"),
                                    "Cairo Governorate" to listOf("Cairo", "New Cairo", "Other"),
                                    "Giza Governorate" to listOf("Giza", "6th of October", "Other"),
                                    "Alexandria Governorate" to listOf("Alexandria", "Other"),
                                    "Dubai" to listOf("Dubai City", "Other"),
                                    "Abu Dhabi" to listOf("Abu Dhabi City", "Al Ain", "Other"),
                                    "Sharjah" to listOf("Sharjah City", "Other"),
                                    "Other" to listOf("Other")
                                )

                                SimpleDropdown(
                                    label = if (isArabic) "البلد" else "Country",
                                    options = countriesList,
                                    selectedOption = country,
                                    onOptionSelected = {
                                        country = it
                                        stateProvince = statesMap[it]?.firstOrNull() ?: "Other"
                                        city = citiesMap[stateProvince]?.firstOrNull() ?: "Other"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                SimpleDropdown(
                                    label = if (isArabic) "المنطقة / المحافظة" else "State / Province",
                                    options = statesMap[country] ?: listOf("Other"),
                                    selectedOption = stateProvince,
                                    onOptionSelected = {
                                        stateProvince = it
                                        city = citiesMap[it]?.firstOrNull() ?: "Other"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                SimpleDropdown(
                                    label = if (isArabic) "المدينة" else "City",
                                    options = citiesMap[stateProvince] ?: listOf("Other"),
                                    selectedOption = city,
                                    onOptionSelected = { city = it },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Cascading Dropdowns for Age Limits
                                Text(
                                    text = if (isArabic) "العمر المفضل للشريك:" else "Preferred Partner Age Limits:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    val ageOptions = (18..70).map { it.toString() }
                                    SimpleDropdown(
                                        label = if (isArabic) "الحد الأدنى" else "Min Age",
                                        options = ageOptions,
                                        selectedOption = minAgePreference.toString(),
                                        onOptionSelected = {
                                            minAgePreference = it.toIntOrNull() ?: 18
                                            if (maxAgePreference < minAgePreference) {
                                                maxAgePreference = minAgePreference
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )

                                    val maxAgeOptions = (minAgePreference..70).map { it.toString() }
                                    SimpleDropdown(
                                        label = if (isArabic) "الحد الأقصى" else "Max Age",
                                        options = maxAgeOptions,
                                        selectedOption = maxAgePreference.toString(),
                                        onOptionSelected = {
                                            maxAgePreference = it.toIntOrNull() ?: 70
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
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
                                        onClick = { currentStep = 3 },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(if (isArabic) "التالي" else "Next")
                                    }
                                }
                            }
                        }

                        3 -> {
                            // Step 3: Granular Muslima Profile Details (with Skips)
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isArabic) "تفاصيل الملف الشخصي" else "Granular Profile Details",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                // Sect
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

                                // Ethnicity
                                val ethnicities = listOf(
                                    "arab_middle_eastern" to (if (isArabic) "عربي / شرق أوسطي" else "Arab / Middle Eastern"),
                                    "north_african" to (if (isArabic) "شمال أفريقي" else "North African"),
                                    "south_asian" to (if (isArabic) "جنوب آسيوي" else "South Asian"),
                                    "turkish" to (if (isArabic) "تركي" else "Turkish"),
                                    "caucasian" to (if (isArabic) "قوقازي" else "Caucasian"),
                                    "african" to (if (isArabic) "أفريقي" else "African"),
                                    "east_asian" to (if (isArabic) "شرق آسيوي" else "East Asian"),
                                    "other" to (if (isArabic) "آخر" else "Other")
                                )
                                Text(
                                    text = if (isArabic) "الأصل العرقي:" else "Ethnicity:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                FlowRow(mainAxisSpacing = 6.dp, crossAxisSpacing = 6.dp) {
                                    ethnicities.forEach { eth ->
                                        FilterChip(
                                            selected = ethnicity == eth.first,
                                            onClick = { ethnicity = eth.first },
                                            label = { Text(eth.second) }
                                        )
                                    }
                                    FilterChip(
                                        selected = ethnicity == "no_say",
                                        onClick = { ethnicity = "no_say" },
                                        label = { Text(if (isArabic) "تخطي" else "Skip") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                // Smoking
                                val smokeOptions = listOf(
                                    "dont_smoke" to (if (isArabic) "لا أدخن" else "Don't Smoke"),
                                    "smoke" to (if (isArabic) "أدخن" else "Smoke"),
                                    "occasionally" to (if (isArabic) "أحياناً" else "Occasionally"),
                                    "planning_to_quit" to (if (isArabic) "أخطط للإقلاع" else "Planning to quit")
                                )
                                Text(
                                    text = if (isArabic) "موقف التدخين:" else "Smoking Preference:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                FlowRow(mainAxisSpacing = 6.dp, crossAxisSpacing = 6.dp) {
                                    smokeOptions.forEach { opt ->
                                        FilterChip(
                                            selected = smokeStatus == opt.first,
                                            onClick = { smokeStatus = opt.first },
                                            label = { Text(opt.second) }
                                        )
                                    }
                                    FilterChip(
                                        selected = smokeStatus == "no_say",
                                        onClick = { smokeStatus = "no_say" },
                                        label = { Text(if (isArabic) "تخطي" else "Skip") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                // Children
                                val childrenOptions = listOf(
                                    "no" to (if (isArabic) "لا أرغب" else "No"),
                                    "yes" to (if (isArabic) "نعم أرغب" else "Yes"),
                                    "not_sure" to (if (isArabic) "غير متأكد" else "Not sure"),
                                    "open" to (if (isArabic) "قابل للنقاش" else "Open to discussion")
                                )
                                Text(
                                    text = if (isArabic) "الرغبة في الأطفال:" else "Desire for Children:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                FlowRow(mainAxisSpacing = 6.dp, crossAxisSpacing = 6.dp) {
                                    childrenOptions.forEach { opt ->
                                        FilterChip(
                                            selected = haveChildren == opt.first,
                                            onClick = { haveChildren = opt.first },
                                            label = { Text(opt.second) }
                                        )
                                    }
                                    FilterChip(
                                        selected = haveChildren == "no_say",
                                        onClick = { haveChildren = "no_say" },
                                        label = { Text(if (isArabic) "تخطي" else "Skip") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                // Body Type
                                val bodyOptions = listOf(
                                    "slim" to (if (isArabic) "نحيف" else "Slim"),
                                    "athletic" to (if (isArabic) "رياضي" else "Athletic"),
                                    "average" to (if (isArabic) "متوسط" else "Average"),
                                    "full_figured" to (if (isArabic) "ممتلئ" else "Full figured"),
                                    "large" to (if (isArabic) "ضخم" else "Large")
                                )
                                Text(
                                    text = if (isArabic) "بنية الجسم:" else "Body Type:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                FlowRow(mainAxisSpacing = 6.dp, crossAxisSpacing = 6.dp) {
                                    bodyOptions.forEach { opt ->
                                        FilterChip(
                                            selected = bodyType == opt.first,
                                            onClick = { bodyType = opt.first },
                                            label = { Text(opt.second) }
                                        )
                                    }
                                    FilterChip(
                                        selected = bodyType == "no_say",
                                        onClick = { bodyType = "no_say" },
                                        label = { Text(if (isArabic) "تخطي" else "Skip") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                // Modesty Preference
                                val modestyLabel = if (gender == Gender.MALE) {
                                    if (isArabic) "الالتزام بالزي الشرعي للشريك:" else "Partner's Modesty / Hijab:"
                                } else {
                                    if (isArabic) "الالتزام بالزي الشرعي:" else "Your Modesty / Hijab:"
                                }
                                Text(
                                    text = modestyLabel,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                FlowRow(mainAxisSpacing = 6.dp, crossAxisSpacing = 6.dp) {
                                    ModestyPreference.values().forEach { mp ->
                                        FilterChip(
                                            selected = modestyPreference == mp,
                                            onClick = { modestyPreference = mp },
                                            label = { Text(mp.getDisplayName(isArabic)) }
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
                                        onClick = { currentStep = 4 },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(if (isArabic) "التالي" else "Next")
                                    }
                                }
                            }
                        }

                        4 -> {
                            // Step 4: Finalize & Register
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isArabic) "إكمال التسجيل" else "Complete Registration",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = if (isArabic) "دعوة ولي الأمر (اختياري):" else "Invite a Guardian / Wali (Optional):",
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

                                // Voice Introduction
                                Text(
                                    text = if (isArabic) "التعريف الصوتي (اختياري - 30 ثانية)" else "Voice Introduction (Optional - 30s)",
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
                                        onClick = { currentStep = 3 },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(if (isArabic) "السابق" else "Back")
                                    }

                                    Button(
                                        onClick = {
                                            val ageInt = age.toIntOrNull() ?: 25
                                            val userProfile = UserProfile(
                                                name = name,
                                                gender = gender,
                                                age = ageInt,
                                                city = city,
                                                country = country,
                                                imageUrl = imageUrl,
                                                sect = sect,
                                                prayerFrequency = prayerFrequency,
                                                modestyPreference = modestyPreference,
                                                relocationWillingness = relocationWillingness,
                                                polygamyAcceptance = polygamyAcceptance,
                                                
                                                profileCreator = "self",
                                                regionalCode = "MUS",
                                                ethnicity = ethnicity,
                                                smokeStatus = smokeStatus,
                                                haveChildren = haveChildren,
                                                bodyType = bodyType,
                                                guardianName = if (waliName.isNotBlank()) waliName else null,
                                                guardianEmail = if (waliEmail.isNotBlank()) waliEmail else null,
                                                guardianStatus = if (waliEmail.isNotBlank()) "PENDING" else "None"
                                            )
                                            viewModel.signUp(
                                                email = email,
                                                passwordPass = password,
                                                profile = userProfile,
                                                localImageUri = localImageUri,
                                                localVoiceUri = localVoiceUri,
                                                context = context
                                            )
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


