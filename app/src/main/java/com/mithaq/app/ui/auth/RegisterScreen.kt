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

/**
 * A highly-polished Multi-Step Registration Wizard for Mithaq.
 * - Step 1: Account credentials & demographics.
 * - Step 2: Religious preferences (Sect, Prayer, Modesty, Relocation, Polygamy).
 * Uses structured selections matching our [UserProfile] data classes.
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

    // Step state: 1 = Core Credentials, 2 = Onboarding Islamic Profile Values
    var currentStep by remember { mutableStateOf(1) }

    // Account Inputs
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(Gender.MALE) }
    var imageUrl by remember { mutableStateOf("avatar_brother_green") }
    var localImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

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
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicture()
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

    // Religious Inputs
    var sect by remember { mutableStateOf(Sect.SUNNI) }
    var prayerFrequency by remember { mutableStateOf(PrayerFrequency.ALWAYS) }
    var modestyPreference by remember { mutableStateOf(ModestyPreference.HIJAB) }
    var relocationWillingness by remember { mutableStateOf(RelocationWillingness.OPEN) }
    var polygamyAcceptance by remember { mutableStateOf(false) }

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
                        text = if (isArabic) "الخطوة $currentStep من 2" else "Step $currentStep of 2",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Spacer(modifier = Modifier.size(48.dp))
                }

                LinearProgressIndicator(
                    progress = currentStep / 2f,
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

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = age,
                                        onValueChange = { age = it },
                                        label = { Text(if (isArabic) "العمر" else "Age") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 6.dp)
                                    )

                                    OutlinedTextField(
                                        value = city,
                                        onValueChange = { city = it },
                                        label = { Text(if (isArabic) "المدينة" else "City") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 6.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = country,
                                    onValueChange = { country = it },
                                    label = { Text(if (isArabic) "البلد" else "Country") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                // Gender Selector
                                Text(
                                    text = if (isArabic) "الجنس" else "Your Gender",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = gender == Gender.MALE,
                                            onClick = { gender = Gender.MALE }
                                        )
                                        Text(if (isArabic) "أخ / ذكر" else "Brother / Male")
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = gender == Gender.FEMALE,
                                            onClick = { gender = Gender.FEMALE }
                                        )
                                        Text(if (isArabic) "أخت / أنثى" else "Sister / Female")
                                    }
                                }

                                // Profile Photo Selection UI
                                Spacer(modifier = Modifier.height(16.dp))
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
                                    // Live preview of selected photo
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        UserProfileImage(
                                            imageUrl = imageUrl,
                                            gender = gender,
                                            isBlurred = false, // Not blurred in registration preview
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
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val avatars = if (gender == Gender.MALE) BrotherhoodAvatars else SisterhoodAvatars
                                            avatars.forEach { (avatarId, color) ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(color)
                                                        .border(
                                                            width = if (imageUrl == avatarId) 2.dp else 0.dp,
                                                            color = if (imageUrl == avatarId) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                            shape = CircleShape
                                                        )
                                                        .clickable { 
                                                            imageUrl = avatarId
                                                            localImageUri = null
                                                        }
                                                ) {
                                                    if (imageUrl == avatarId) {
                                                        Icon(
                                                            imageVector = Icons.Default.Done,
                                                            contentDescription = "Selected",
                                                            tint = Color.White,
                                                            modifier = Modifier
                                                                .align(Alignment.Center)
                                                                .size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = if (imageUrl.startsWith("avatar_") || localImageUri != null) "" else imageUrl,
                                    onValueChange = { input ->
                                        localImageUri = null
                                        imageUrl = input.ifBlank {
                                            if (gender == Gender.MALE) "avatar_brother_green" else "avatar_sister_teal"
                                        }
                                    },
                                    label = { Text(if (isArabic) "رابط صورة مخصصة (اختياري)" else "Custom Photo URL (Optional)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        showPhotoOptionDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
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
                                                    cameraLauncher.launch(uri)
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
                                        if (name.isBlank() || email.isBlank() || password.isBlank() || parsedAge == null || city.isBlank() || country.isBlank()) {
                                            localError = if (isArabic) "يرجى ملء جميع الحقول الأساسية بشكل صحيح." else "Please fill in all core fields correctly."
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
                            // Step 2 Layout: Modesty & Religion details
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isArabic) "تفضيلات الشريك" else "Match Preferences",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                // Sect Dropdown
                                Text(strings.selectSect, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Sect.values().forEach { s ->
                                        FilterChip(
                                            selected = sect == s,
                                            onClick = { sect = s },
                                            label = { Text(s.getDisplayName(isArabic)) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                // Prayer consistency Selector
                                Text(strings.selectPrayer, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
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

                                // Modesty Preference
                                val modestyLabel = if (gender == Gender.MALE) strings.selectModesty else (if (isArabic) "الالتزام بالزي الشرعي" else "Modesty / Hijab")
                                Text(modestyLabel, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                FlowRow(mainAxisSpacing = 6.dp, crossAxisSpacing = 6.dp) {
                                    ModestyPreference.values().forEach { mp ->
                                        FilterChip(
                                            selected = modestyPreference == mp,
                                            onClick = { modestyPreference = mp },
                                            label = { Text(mp.getDisplayName(isArabic)) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                // Relocation
                                Text(strings.selectRelocation, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                FlowRow(mainAxisSpacing = 6.dp, crossAxisSpacing = 6.dp) {
                                    RelocationWillingness.values().forEach { rw ->
                                        FilterChip(
                                            selected = relocationWillingness == rw,
                                            onClick = { relocationWillingness = rw },
                                            label = { Text(rw.getDisplayName(isArabic)) }
                                        )
                                    }
                                }
                                    // Voice Introduction (Optional)
                                 Spacer(modifier = Modifier.height(16.dp))
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
                                             containerColor = if (isRecordingVoice) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
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

                                 // Polygamy Switch
                                 Row(
                                     modifier = Modifier.fillMaxWidth(),
                                     horizontalArrangement = Arrangement.SpaceBetween,
                                     verticalAlignment = Alignment.CenterVertically
                                 ) {
                                     Text(strings.polygamyAcceptance, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                     Switch(checked = polygamyAcceptance, onCheckedChange = { polygamyAcceptance = it })
                                 }

                                 Spacer(modifier = Modifier.height(24.dp))

                                // Finish Button
                                Button(
                                    onClick = {
                                        val ageInt = age.toIntOrNull() ?: 18
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
                                            polygamyAcceptance = polygamyAcceptance
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
                                    modifier = Modifier.fillMaxWidth()
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

