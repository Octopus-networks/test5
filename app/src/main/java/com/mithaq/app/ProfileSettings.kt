package com.mithaq.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Done
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.mithaq.app.model.*
import com.mithaq.app.ui.auth.AuthViewModel
import com.mithaq.app.ui.auth.LoginScreen
import com.mithaq.app.ui.auth.RegisterScreen
import com.mithaq.app.ui.chat.ChaperonedChatBanner
import com.mithaq.app.ui.chat.ChatBubble
import com.mithaq.app.ui.chat.ChaperonedChatViewModel
import com.mithaq.app.ui.filter.SearchFilterBottomSheet
import com.mithaq.app.ui.filter.SearchViewModel
import com.mithaq.app.ui.guardian.GuardianViewModel
import com.mithaq.app.ui.guardian.InviteGuardianDialog
import com.mithaq.app.ui.match.MatchScoreBadge
import com.mithaq.app.ui.match.MatchScoreCalculator
import com.mithaq.app.ui.photo.PhotoAccessRequestCard
import com.mithaq.app.ui.photo.PhotoAccessState
import com.mithaq.app.ui.photo.UserProfileImage
import com.mithaq.app.ui.photo.BrotherhoodAvatars
import com.mithaq.app.ui.photo.SisterhoodAvatars
import com.mithaq.app.ui.theme.MithaqTheme
import com.mithaq.app.ui.theme.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Add
import com.mithaq.app.ui.admin.AdminConsoleScreen
import com.mithaq.app.ui.limit.PremiumStoreScreen
import com.mithaq.app.ui.match.QuestionnaireScreen
import com.mithaq.app.ui.match.CompatibilityBreakdownDialog
import com.mithaq.app.ui.onboarding.OnboardingWizardScreen
import com.mithaq.app.ui.chat.ChaperonedVoiceCallScreen
import com.mithaq.app.ui.chat.CallState
import com.mithaq.app.security.SecureScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import com.mithaq.app.ui.stats.MyStatsScreen
import com.mithaq.app.ui.splash.SplashScreen



@Composable
fun GuardianTabContent(
    currentUser: UserProfile,
    viewModel: GuardianViewModel,
    isArabic: Boolean,
    onInviteSuccess: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    val guardianName = currentUser.guardianName
    val guardianEmail = currentUser.guardianEmail
    val guardianStatus = currentUser.guardianStatus ?: "None"

    LaunchedEffect(uiState) {
        if (uiState is com.mithaq.app.ui.guardian.GuardianUiState.Success) {
            onInviteSuccess()
        }
    }

    val strings = com.mithaq.app.ui.theme.LocalMithaqStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = strings.waliIntegration,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = strings.waliDescription,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        if (!guardianName.isNullOrBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(if (strings.appName == "ميثاق") "اسم الولي: $guardianName" else "Guardian Name: $guardianName", fontWeight = FontWeight.Bold)
                    Text(if (strings.appName == "ميثاق") "بريد الولي: $guardianEmail" else "Guardian Email: $guardianEmail")
                    Text(if (strings.appName == "ميثاق") "الحالة: $guardianStatus" else "Status: $guardianStatus", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Button(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (guardianName.isNullOrBlank()) strings.inviteWali else if (strings.appName == "ميثاق") "تعديل بيانات الولي" else "Update Wali Details")
        }

        if (showDialog) {
            InviteGuardianDialog(
                viewModel = viewModel,
                onDismissRequest = { showDialog = false },
                titleText = strings.inviteWali,
                subtitleText = if (strings.appName == "ميثاق") "في الزواج الإسلامي، إشراك ولي الأمر يضمن رحلة مباركة، شفافة، وآمنة." else "In Islamic matchmaking, involving a guardian ensures a blessed, transparent, and safe journey.",
                nameLabel = strings.nameLabel,
                emailLabel = strings.emailLabel,
                submitButtonText = if (strings.appName == "ميثاق") "إرسال الدعوة" else "Send Invitation",
                cancelButtonText = if (strings.appName == "ميثاق") "إلغاء" else "Cancel",
                successTitle = if (strings.appName == "ميثاق") "تم إرسال الدعوة" else "Invitation Sent",
                successSubtitle = if (strings.appName == "ميثاق") "تم إرسال دعوة إلى ولي أمرك. سنقوم بإشعارك بمجرد قبوله." else "An invitation has been sent to your Guardian. We will notify you once they accept.",
                closeButtonText = if (strings.appName == "ميثاق") "إغلاق" else "Close",
                isArabic = isArabic
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModestyTabContent(
    currentUser: UserProfile,
    targetUser: UserProfile?,
    onRefreshProfile: () -> Unit,
    isArabic: Boolean,
    authViewModel: AuthViewModel,
    onNavigateToScreen: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val photoAccessManager = remember { com.mithaq.app.ui.photo.PhotoAccessManager(context) }
    var photoState by remember { mutableStateOf(com.mithaq.app.ui.photo.PhotoAccessState.NONE) }

    var aboutYourselfText by remember { mutableStateOf(currentUser.aboutYourself) }
    var idealPartnerText by remember { mutableStateOf(currentUser.idealPartner) }
    var improvingBio by remember { mutableStateOf(false) }
    var savingBio by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser.aboutYourself, currentUser.idealPartner) {
        aboutYourselfText = currentUser.aboutYourself
        idealPartnerText = currentUser.idealPartner
    }

    var isUploadingImage by remember { mutableStateOf(false) }

    var tempCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    fun handleProfileImageUpload(uri: android.net.Uri) {
        isUploadingImage = true
        coroutineScope.launch {
            val isMock = com.mithaq.app.Config.isMock()

            val finalUrl = if (isMock) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val directory = java.io.File(context.filesDir, "profiles")
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }
                    val localFile = java.io.File(directory, "${currentUser.uid}.jpg")
                    val outputStream = java.io.FileOutputStream(localFile)
                    inputStream?.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                    android.net.Uri.fromFile(localFile).toString()
                } catch (e: Exception) {
                    uri.toString()
                }
            } else {
                try {
                    val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
                    val profileImageRef = storageRef.child("profiles/${currentUser.uid}.jpg")
                    val inputStream = context.contentResolver.openInputStream(uri) ?: throw java.io.IOException("Unable to open input stream")
                    val bytes = inputStream.readBytes()
                    inputStream.close()
                    profileImageRef.putBytes(bytes).await()
                    profileImageRef.downloadUrl.await().toString()
                } catch (e: Exception) {
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val directory = java.io.File(context.filesDir, "profiles")
                        if (!directory.exists()) {
                            directory.mkdirs()
                        }
                        val localFile = java.io.File(directory, "${currentUser.uid}.jpg")
                        val outputStream = java.io.FileOutputStream(localFile)
                        inputStream?.use { input ->
                            outputStream.use { output ->
                                input.copyTo(output)
                            }
                        }
                        android.net.Uri.fromFile(localFile).toString()
                    } catch (ex: Exception) {
                        uri.toString()
                    }
                }
            }

            if (isMock) {
                context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putString("imageUrl", finalUrl)
                    .apply()
            } else {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("users")
                    .document(currentUser.uid)
                    .update("imageUrl", finalUrl)
                    .await()
            }
            isUploadingImage = false
            onRefreshProfile()
        }
    }

    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            handleProfileImageUpload(uri)
        }
    }

    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = com.mithaq.app.ui.photo.CustomTakePictureContract()
    ) { success ->
        if (success) {
            tempCameraUri?.let { handleProfileImageUpload(it) }
        }
    }

    var tempAdditionalCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showAdditionalSourceDialog by remember { mutableStateOf(false) }

    fun handleAdditionalImageUpload(uri: android.net.Uri) {
        isUploadingImage = true
        coroutineScope.launch {
            val isMock = com.mithaq.app.Config.isMock()
            val newIndex = currentUser.additionalImages.size
            val finalUrl = if (isMock) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val directory = java.io.File(context.filesDir, "profiles")
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }
                    val localFile = java.io.File(directory, "${currentUser.uid}_additional_${newIndex}.jpg")
                    val outputStream = java.io.FileOutputStream(localFile)
                    inputStream?.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                    android.net.Uri.fromFile(localFile).toString()
                } catch (e: Exception) {
                    uri.toString()
                }
            } else {
                try {
                    val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
                    val profileImageRef = storageRef.child("profiles/${currentUser.uid}_additional_${newIndex}.jpg")
                    val inputStream = context.contentResolver.openInputStream(uri) ?: throw java.io.IOException("Unable to open input stream")
                    val bytes = inputStream.readBytes()
                    inputStream.close()
                    profileImageRef.putBytes(bytes).await()
                    profileImageRef.downloadUrl.await().toString()
                } catch (e: Exception) {
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val directory = java.io.File(context.filesDir, "profiles")
                        if (!directory.exists()) {
                            directory.mkdirs()
                        }
                        val localFile = java.io.File(directory, "${currentUser.uid}_additional_${newIndex}.jpg")
                        val outputStream = java.io.FileOutputStream(localFile)
                        inputStream?.use { input ->
                            outputStream.use { output ->
                                input.copyTo(output)
                            }
                        }
                        android.net.Uri.fromFile(localFile).toString()
                    } catch (ex: Exception) {
                        uri.toString()
                    }
                }
            }

            val newList = currentUser.additionalImages + finalUrl
            authViewModel.updateAdditionalImages(newList, context)
            isUploadingImage = false
            onRefreshProfile()
        }
    }

    val additionalGalleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            handleAdditionalImageUpload(uri)
        }
    }

    val additionalCameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = com.mithaq.app.ui.photo.CustomTakePictureContract()
    ) { success ->
        if (success) {
            tempAdditionalCameraUri?.let { handleAdditionalImageUpload(it) }
        }
    }

    LaunchedEffect(currentUser.uid, targetUser?.uid) {
        if (targetUser != null) {
            photoState = photoAccessManager.checkPhotoAccessState(currentUser.uid, targetUser.uid)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isArabic) "الملف الشخصي وإعدادات الحشمة" else "Profile & Modesty Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Own Basic Info Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isArabic) "البيانات الأساسية" else "Basic Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                var nameText by remember { mutableStateOf(currentUser.name) }
                var savingName by remember { mutableStateOf(false) }
                
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text(if (isArabic) "الاسم الكامل" else "Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Read-only Gender field
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "الجنس" else "Gender",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currentUser.gender.getDisplayName(isArabic),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = {
                        savingName = true
                        coroutineScope.launch {
                            try {
                                authViewModel.updateBasicInfo(nameText, context)
                                android.widget.Toast.makeText(
                                    context,
                                    if (isArabic) "تم تحديث الاسم بنجاح!" else "Name updated successfully!",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                onRefreshProfile()
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "فشل حفظ الاسم: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                            } finally {
                                savingName = false
                            }
                        }
                    },
                    enabled = !savingName && nameText.isNotBlank() && nameText != currentUser.name,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (savingName) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (isArabic) "حفظ الاسم" else "Save Name")
                    }
                }
            }
        }

        // Own Profile Photo Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (currentUser.gender == com.mithaq.app.model.Gender.MALE) "الصورة الشخصية (أخ)" else "Profile Photo (Sister)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.size(96.dp)) {
                    UserProfileImage(
                        imageUrl = currentUser.imageUrl,
                        gender = currentUser.gender,
                        isBlurred = false, // Never blurred for oneself
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                var showImageEdit by remember { mutableStateOf(false) }
                if (!showImageEdit) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(onClick = { showImageEdit = true }) {
                            Text(if (currentUser.gender == com.mithaq.app.model.Gender.MALE) "تعديل الصورة الشخصية" else "Edit Profile Photo")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isUploadingImage) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        galleryLauncher.launch(
                                            androidx.activity.result.PickVisualMediaRequest(
                                                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isArabic) "المعرض" else "Gallery")
                                }
                                Button(
                                    onClick = {
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
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isArabic) "الكاميرا" else "Camera")
                                }
                            }
                        }
                    }
                } else {
                    var newImageUrl by remember { mutableStateOf(currentUser.imageUrl) }
                    
                    Text(
                        text = if (currentUser.gender == com.mithaq.app.model.Gender.MALE) "اختر رمزاً محتشماً:" else "Choose Modest Avatar:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val avatars = if (currentUser.gender == com.mithaq.app.model.Gender.MALE) BrotherhoodAvatars else SisterhoodAvatars
                        avatars.forEach { (avatarId, color) ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (newImageUrl == avatarId) 2.dp else 0.dp,
                                        color = if (newImageUrl == avatarId) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { newImageUrl = avatarId }
                            ) {
                                if (newImageUrl == avatarId) {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.align(Alignment.Center).size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = if (newImageUrl.startsWith("avatar_")) "" else newImageUrl,
                        onValueChange = { input ->
                            newImageUrl = input.ifBlank {
                                if (currentUser.gender == com.mithaq.app.model.Gender.MALE) "avatar_brother_green" else "avatar_sister_teal"
                            }
                        },
                        label = { Text(if (currentUser.gender == com.mithaq.app.model.Gender.MALE) "أو رابط صورة مخصصة" else "Or Custom Image URL") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(onClick = { showImageEdit = false }) {
                            Text(if (currentUser.gender == com.mithaq.app.model.Gender.MALE) "إلغاء" else "Cancel")
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    db.collection("users")
                                        .document(currentUser.uid)
                                        .update("imageUrl", newImageUrl)
                                        .addOnSuccessListener {
                                            showImageEdit = false
                                            onRefreshProfile()
                                        }
                                }
                            }
                        ) {
                            Text(if (currentUser.gender == com.mithaq.app.model.Gender.MALE) "حفظ" else "Save")
                        }
                    }
                }

                // ------------------ ADDITIONAL PHOTOS SECTION ------------------
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = if (isArabic) "صور إضافية (حتى 4 صور)" else "Additional Photos (Up to 4)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    currentUser.additionalImages.forEach { img ->
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            UserProfileImage(
                                imageUrl = img,
                                gender = currentUser.gender,
                                isBlurred = false,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxSize()
                            )
                            // Delete button overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                                    .clickable {
                                        val newList = currentUser.additionalImages.filter { it != img }
                                        authViewModel.updateAdditionalImages(newList, context)
                                        onRefreshProfile()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                    
                    if (currentUser.additionalImages.size < 4) {
                        // Add Button Slot
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                .clickable { showAdditionalSourceDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Photo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                if (showAdditionalSourceDialog) {
                    AlertDialog(
                        onDismissRequest = { showAdditionalSourceDialog = false },
                        title = { Text(if (isArabic) "إضافة صورة إضافية" else "Add Additional Photo") },
                        text = { Text(if (isArabic) "اختر مصدر الصورة:" else "Choose photo source:") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showAdditionalSourceDialog = false
                                    additionalGalleryLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                            ) {
                                Text(if (isArabic) "المعرض" else "Gallery")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showAdditionalSourceDialog = false
                                    val uri = getCameraImageUri(context)
                                    tempAdditionalCameraUri = uri
                                    try {
                                        additionalCameraLauncher.launch(uri)
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
                        }
                    )
                }
            }
        }

        // ------------------ BIOGRAPHY & IDEAL PARTNER CARD ------------------
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isArabic) "النبذة التعريفية والشريك المثالي" else "Profile Biography & Ideal Partner",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // About Yourself Field
                OutlinedTextField(
                    value = aboutYourselfText,
                    onValueChange = { aboutYourselfText = it },
                    label = { Text(if (isArabic) "عن نفسي (اهتماماتك، طبيعتك)" else "About Yourself (Interests, Nature)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 5
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Improve Bio with AI Button
                Button(
                    onClick = {
                        if (aboutYourselfText.isNotBlank()) {
                            improvingBio = true
                            coroutineScope.launch {
                                val apiKey = com.mithaq.app.Config.GEMINI_API_KEY
                                if (apiKey.isNotEmpty() && apiKey != "YOUR_GEMINI_API_KEY") {
                                    try {
                                        val service = com.mithaq.app.service.GeminiService(apiKey)
                                        val improved = service.improveProfile(aboutYourselfText)
                                        aboutYourselfText = improved
                                        android.widget.Toast.makeText(context, if (isArabic) "تم تحسين النبذة!" else "Bio improved!", android.widget.Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "فشل تحسين النبذة: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, if (isArabic) "يرجى تهيئة مفتاح GEMINI_API_KEY في ملف Config.kt أولاً." else "Please configure GEMINI_API_KEY in Config.kt first.", android.widget.Toast.LENGTH_LONG).show()
                                }
                                improvingBio = false
                            }
                        }
                    },
                    enabled = !improvingBio && aboutYourselfText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (improvingBio) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (isArabic) "✨ تحسين بالذكاء الاصطناعي" else "✨ Improve with AI")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Ideal Partner Field
                OutlinedTextField(
                    value = idealPartnerText,
                    onValueChange = { idealPartnerText = it },
                    label = { Text(if (isArabic) "مواصفات الشريك المثالي" else "Ideal Partner Specifications") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 5
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Save Biography Button
                Button(
                    onClick = {
                        savingBio = true
                        coroutineScope.launch {
                            try {
                                authViewModel.updateBio(aboutYourselfText, idealPartnerText, context)
                                android.widget.Toast.makeText(
                                    context,
                                    if (isArabic) "تم حفظ التعديلات بنجاح!" else "Changes saved successfully!",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                onRefreshProfile()
                            } catch(e: Exception) {
                                android.widget.Toast.makeText(context, "فشل الحفظ: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                            } finally {
                                savingBio = false
                            }
                        }
                    },
                    enabled = !savingBio,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (savingBio) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (isArabic) "حفظ التعديلات" else "Save Changes")
                    }
                }
            }
        }

        // ------------------ IDENTITY VERIFICATION CENTER ------------------
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isArabic) "مركز توثيق الهوية (الشارة الزرقاء)" else "Identity Verification Center (Blue Badge)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (currentUser.verificationStatus == "VERIFIED") {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "Verified Profile",
                            tint = Color.White,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2196F3))
                                .padding(2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                when (currentUser.verificationStatus) {
                    "VERIFIED" -> {
                        Text(
                            text = if (isArabic)
                                "حسابك موثق بالشارة الزرقاء. هويتك مؤكدة الآن!"
                            else
                                "Your account is verified. Your identity is confirmed!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4CAF50),
                            textAlign = TextAlign.Center
                        )
                    }
                    "PENDING" -> {
                        Text(
                            text = if (isArabic)
                                "طلب التحقق قيد المراجعة حالياً. سنقوم بتحديث حالتك قريباً."
                            else
                                "Your verification request is pending review. We will update your status shortly.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFF9800),
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {
                        var idCardUri by remember { mutableStateOf<android.net.Uri?>(null) }
                        var selfieUri by remember { mutableStateOf<android.net.Uri?>(null) }
                        var isSubmitting by remember { mutableStateOf(false) }
                        var statusMsg by remember { mutableStateOf<String?>(null) }

                        var tempIdCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
                        var tempSelfieCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

                        val idCardLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                            contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
                        ) { uri ->
                            if (uri != null) idCardUri = uri
                        }

                        val idCardCameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                            contract = com.mithaq.app.ui.photo.CustomTakePictureContract()
                        ) { success ->
                            if (success) {
                                tempIdCameraUri?.let { idCardUri = it }
                            }
                        }

                        val selfieLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                            contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
                        ) { uri ->
                            if (uri != null) selfieUri = uri
                        }

                        val selfieCameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                            contract = androidx.activity.result.contract.ActivityResultContracts.CaptureVideo()
                        ) { success ->
                            if (success) {
                                tempSelfieCameraUri?.let { selfieUri = it }
                            }
                        }

                        // ID Card Selection
                        Text(
                            text = if (isArabic) "صورة الهوية الوطنية (البطاقة/جواز السفر)" else "National Identity Document (ID/Passport)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    idCardLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (idCardUri != null && idCardUri != tempIdCameraUri) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text(if (isArabic) "الهوية (المعرض)" else "ID (Gallery)")
                            }
                            Button(
                                onClick = {
                                    try {
                                        val uri = getCameraImageUri(context)
                                        tempIdCameraUri = uri
                                        idCardCameraLauncher.launch(uri)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(
                                            context,
                                            if (isArabic) "عذرًا، فشل فتح الكاميرا: ${e.localizedMessage}" else "Sorry, failed to open camera: ${e.localizedMessage}",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (idCardUri != null && idCardUri == tempIdCameraUri) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(if (isArabic) "الهوية (الكاميرا)" else "ID (Camera)")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Selfie Video Selection
                        Text(
                            text = if (isArabic) "فيديو شخصي حي (سيلفي لمطابقة الوجه)" else "Live Selfie Video (for face matching)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    selfieLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VideoOnly
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selfieUri != null && selfieUri != tempSelfieCameraUri) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text(if (isArabic) "فيديو السيلفي (المعرض)" else "Selfie Video (Gallery)")
                            }
                            Button(
                                onClick = {
                                    try {
                                        val uri = getCameraVideoUri(context)
                                        tempSelfieCameraUri = uri
                                        selfieCameraLauncher.launch(uri)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(
                                            context,
                                            if (isArabic) "عذرًا، فشل فتح الكاميرا لتسجيل الفيديو: ${e.localizedMessage}" else "Sorry, failed to open camera for video recording: ${e.localizedMessage}",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selfieUri != null && selfieUri == tempSelfieCameraUri) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(if (isArabic) "تسجيل فيديو (الكاميرا)" else "Record Video (Camera)")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (statusMsg != null) {
                            Text(
                                text = statusMsg!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (statusMsg!!.contains("نجاح") || statusMsg!!.contains("success") || statusMsg!!.contains("قيد")) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Button(
                            onClick = {
                                if (idCardUri == null || selfieUri == null) {
                                    statusMsg = if (isArabic) "يرجى اختيار كل من صورة الهوية وفيديو السيلفي الحي." else "Please select both ID Card and Live Selfie Video."
                                    return@Button
                                }
                                isSubmitting = true
                                statusMsg = if (isArabic) "جاري معالجة التحقق وإثبات الحيوية (Liveness)..." else "Processing verification & live face verification..."
                                authViewModel.submitVerification(idCardUri!!, selfieUri!!, context) { success, message ->
                                    isSubmitting = false
                                    statusMsg = message
                                    if (success) {
                                        onRefreshProfile()
                                    }
                                }
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text(if (isArabic) "تقديم طلب التحقق" else "Submit Verification")
                            }
                        }
                    }
                }

            }
        }
        // ------------------------------------------------------------------

        Spacer(modifier = Modifier.height(16.dp))

        if (targetUser != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(
                    text = if (isArabic) "عرض صورة الشريك: ${targetUser.name}" else "Viewing Match's Photo: ${targetUser.name}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                VerificationBadge(status = targetUser.verificationStatus)
            }
            
            PhotoAccessRequestCard(
                isOwnProfile = false,
                accessState = photoState,
                onRequestAccessClicked = {
                    coroutineScope.launch {
                        val success = photoAccessManager.requestPhotoAccess(currentUser.uid, targetUser.uid)
                        if (success) {
                            photoState = com.mithaq.app.ui.photo.PhotoAccessState.PENDING
                            onRefreshProfile()

                            // Simulate target user (or Wali) approving the request after 3 seconds
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(3000)
                                val approveSuccess = photoAccessManager.approvePhotoAccess(targetUser.uid, currentUser.uid)
                                if (approveSuccess) {
                                    photoState = com.mithaq.app.ui.photo.PhotoAccessState.APPROVED
                                    onRefreshProfile()
                                    // Trigger notification
                                    com.mithaq.app.notification.MithaqFirebaseMessagingService.showLocalNotification(
                                        context = context,
                                        title = if (isArabic) "ميثاق - الموافقة على طلب الصور" else "Mithaq - Photo Access Approved",
                                        body = if (isArabic) "تمت الموافقة على طلبك لرؤية الصورة الشخصية لـ ${targetUser.name}" 
                                               else "Your request to view the profile photo of ${targetUser.name} has been approved."
                                    )
                                }
                            }
                        }
                    }
                }
            )
        } else {
            Text(
                text = if (isArabic) "اختر عضواً من تبويب البحث لعرض خيارات إلغاء قفل الصورة." else "Select a user on the Search tab to view their photo unlock options.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isArabic) "طلبات مشاهدة صورتك المعلقة" else "Pending Requests to View Your Photo",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.align(Alignment.Start)
        )

        val requests = currentUser.photoAccessRequests
        if (requests.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isArabic) "لا توجد طلبات معلقة حالياً." else "No pending requests.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            PhotoAccessRequestCard(
                isOwnProfile = true,
                accessState = com.mithaq.app.ui.photo.PhotoAccessState.NONE,
                onRequestAccessClicked = {},
                pendingRequests = requests,
                onApproveClicked = { userId ->
                    coroutineScope.launch {
                        val success = photoAccessManager.approvePhotoAccess(currentUser.uid, userId)
                        if (success) {
                            onRefreshProfile()
                            // Trigger notification
                            com.mithaq.app.notification.MithaqFirebaseMessagingService.showLocalNotification(
                                context = context,
                                title = if (isArabic) "ميثاق - تم منح صلاحية الصورة" else "Mithaq - Photo Access Granted",
                                body = if (isArabic) "لقد قمت بنجاح بمشاركة صورتك الشخصية مع الطرف الآخر." 
                                       else "You have successfully shared your profile photo with the other member."
                            )
                        }
                    }
                }
            )
        }

        var devTapCount by remember { mutableStateOf(0) }
        var isDevMenuVisible by remember { mutableStateOf(false) }

        if (isDevMenuVisible && !com.mithaq.app.Config.IS_PRODUCTION) {
            // ------------------ DEVELOPER OPTIONS & SETTINGS CARD ------------------
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with Gear Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = if (isArabic) "خيارات المطور والإعدادات" else "Developer Settings & Options",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    val isMockDatabase = try {
                        val firestoreDb = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        firestoreDb.app?.options?.apiKey == "mock-api-key-for-testing" || firestoreDb.app?.options?.apiKey?.contains("mock") == true
                    } catch (e: Exception) {
                        true
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isMockDatabase) Color(0xFFFF9800) else Color(0xFF4CAF50))
                        )
                        Text(
                            text = if (isMockDatabase) {
                                if (isArabic) "وضع الاتصال: وضع التجربة (قاعدة بيانات وهمية)" else "Connection: Demo Mode (Mock Database)"
                            } else {
                                if (isArabic) "وضع الاتصال: متصل بالسحابة (سيرفر حقيقي)" else "Connection: Connected to Cloud (Real Server)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isMockDatabase) Color(0xFFFF9800) else Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulated Offline Switch Row
                    val devPrefs = context.getSharedPreferences("mithaq_dev_options", android.content.Context.MODE_PRIVATE)
                    var isOfflineSimulatedLocal by remember {
                        mutableStateOf(devPrefs.getBoolean("is_offline_simulated", false))
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isArabic) "محاكاة وضع عدم الاتصال" else "Simulate Offline Mode",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isArabic) "تشغيل وضع عدم الاتصال بالشبكة لاختبار التخزين المحلي" else "Simulate network loss to test offline Room cache",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isOfflineSimulatedLocal,
                            onCheckedChange = { checked ->
                                isOfflineSimulatedLocal = checked
                                devPrefs.edit().putBoolean("is_offline_simulated", checked).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Mock Role Switcher Section
                    Text(
                        text = if (isArabic) "تبديل دور المستخدم (للاختبار)" else "Switch Mock User Role (Testing)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Regular Member Chip
                        FilterChip(
                            selected = !currentUser.isWaliAccount && !currentUser.isAdmin,
                            onClick = {
                                authViewModel.updateMockRole(isWali = false, isAdmin = false, context = context)
                            },
                            label = { Text(if (isArabic) "يوزر عادي (عضو)" else "Regular Member") }
                        )
                        // Wali Chip
                        FilterChip(
                            selected = currentUser.isWaliAccount,
                            onClick = {
                                authViewModel.updateMockRole(isWali = true, isAdmin = false, context = context)
                            },
                            label = { Text(if (isArabic) "مشرف (ولي أمر)" else "Wali / Guardian") }
                        )
                        // Admin Chip
                        FilterChip(
                            selected = currentUser.isAdmin,
                            onClick = {
                                authViewModel.updateMockRole(isWali = false, isAdmin = true, context = context)
                            },
                            label = { Text(if (isArabic) "إدمن (مسؤول)" else "Admin") }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(20.dp))

                    // Navigation Shortcuts Section
                    Text(
                        text = if (isArabic) "روابط التنقل السريع" else "Navigation Shortcuts",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Premium Store Button
                    Button(
                        onClick = { onNavigateToScreen("premium_store") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isArabic) "الذهاب إلى متجر الاشتراكات المميز" else "Go to Premium Store")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Questionnaire Button
                    Button(
                        onClick = { onNavigateToScreen("questionnaire") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isArabic) "الذهاب إلى استبيان التوافق" else "Go to Compatibility Questionnaire")
                    }

                    if (currentUser.isAdmin) {
                        Spacer(modifier = Modifier.height(8.dp))
                        // Admin Console Button
                        Button(
                            onClick = { onNavigateToScreen("admin") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFC62828) // Admin Red
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isArabic) "الذهاب إلى لوحة تحكم الإدارة" else "Go to Admin Console")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = if (isArabic) "ميثاق v2.0" else "Mithaq v2.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .clickable {
                    if (!com.mithaq.app.Config.IS_PRODUCTION) {
                        devTapCount++
                        if (devTapCount >= 5) {
                            isDevMenuVisible = true
                            authViewModel.updateMockRole(isWali = false, isAdmin = true, context = context)
                            android.widget.Toast.makeText(
                                context,
                                if (isArabic) "وضع المطور نشط الآن! وتمت ترقيتك إلى مسؤول (Admin)." else "Developer mode activated! You have been promoted to Admin.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                .padding(8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    currentUser: UserProfile,
    onRefreshProfile: () -> Unit,
    isArabic: Boolean,
    authViewModel: AuthViewModel,
    guardianViewModel: GuardianViewModel,
    onNavigateToScreen: (String) -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        onRefreshProfile()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isArabic) "الملف الشخصي والإعدادات" else "Profile & Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModestyTabContent(
                currentUser = currentUser,
                targetUser = null,
                onRefreshProfile = onRefreshProfile,
                isArabic = isArabic,
                authViewModel = authViewModel,
                onNavigateToScreen = onNavigateToScreen
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))


            
            GuardianTabContent(
                currentUser = currentUser,
                viewModel = guardianViewModel,
                isArabic = isArabic,
                onInviteSuccess = onRefreshProfile
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onNavigateToScreen("app_settings") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isArabic) "إعدادات التطبيق" else "App Settings",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


