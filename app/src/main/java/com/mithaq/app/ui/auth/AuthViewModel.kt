package com.mithaq.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.mithaq.app.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    data class Authenticated(val userId: String) : AuthState
    data class Error(val errorMessage: String) : AuthState
}

/**
 * ViewModel managing Authentication flows (Sign In, Sign Up, and Sign Out).
 * Saves user demographic & Islamic preferences to Firestore on registration.
 */
class AuthViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val context: android.content.Context? = null
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    init {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _authState.value = AuthState.Authenticated(currentUser.uid)
            fetchCurrentUserProfile(currentUser.uid)
        }
    }

    fun fetchCurrentUserProfile(uid: String) {
        viewModelScope.launch {
            try {
                val isMock = try {
                    auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
                } catch (e: Exception) {
                    true
                }

                if (isMock) {
                    val prefs = context?.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
                    val savedUid = prefs?.getString("uid", null)
                    if (savedUid != null) {
                        val name = prefs.getString("name", "Mock User") ?: "Mock User"
                        val genderStr = prefs.getString("gender", "MALE") ?: "MALE"
                        val gender = if (genderStr == "FEMALE") Gender.FEMALE else Gender.MALE
                        val age = prefs.getInt("age", 26)
                        val city = prefs.getString("city", "Cairo") ?: "Cairo"
                        val country = prefs.getString("country", "Egypt") ?: "Egypt"
                        val imageUrl = prefs.getString("imageUrl", "") ?: ""
                        val sectStr = prefs.getString("sect", "SUNNI") ?: "SUNNI"
                        val sect = try { Sect.valueOf(sectStr) } catch(e: Exception) { Sect.SUNNI }
                        val prayerStr = prefs.getString("prayerFrequency", "ALWAYS") ?: "ALWAYS"
                        val prayer = try { PrayerFrequency.valueOf(prayerStr) } catch(e: Exception) { PrayerFrequency.ALWAYS }
                        val modestyStr = prefs.getString("modestyPreference", "HIJAB") ?: "HIJAB"
                        val modesty = try { ModestyPreference.valueOf(modestyStr) } catch(e: Exception) { ModestyPreference.HIJAB }
                        val relocationStr = prefs.getString("relocationWillingness", "OPEN") ?: "OPEN"
                        val relocation = try { RelocationWillingness.valueOf(relocationStr) } catch(e: Exception) { RelocationWillingness.OPEN }
                        
                        val isWaliAccount = prefs.getBoolean("isWaliAccount", false)
                        val wardUid = prefs.getString("wardUid", null)
                        val verificationStatus = prefs.getString("verificationStatus", "NONE") ?: "NONE"
                        val voiceIntroUrl = prefs.getString("voiceIntroUrl", null)
                        val fcmToken = prefs.getString("fcmToken", null)

                        _currentUserProfile.value = UserProfile(
                            uid = savedUid,
                            name = name,
                            gender = gender,
                            age = age,
                            city = city,
                            country = country,
                            imageUrl = imageUrl,
                            sect = sect,
                            prayerFrequency = prayer,
                            modestyPreference = modesty,
                            relocationWillingness = relocation,
                            isWaliAccount = isWaliAccount,
                            wardUid = wardUid,
                            verificationStatus = verificationStatus,
                            voiceIntroUrl = voiceIntroUrl,
                            fcmToken = fcmToken
                        )
                    } else {
                        _currentUserProfile.value = UserProfile(
                            uid = uid,
                            name = "Mock User",
                            gender = Gender.MALE,
                            age = 26,
                            city = "Cairo",
                            country = "Egypt",
                            imageUrl = "avatar_brother_green",
                            sect = Sect.SUNNI,
                            prayerFrequency = PrayerFrequency.ALWAYS,
                            modestyPreference = ModestyPreference.HIJAB,
                            relocationWillingness = RelocationWillingness.OPEN,
                            isWaliAccount = false,
                            verificationStatus = "NONE"
                        )
                    }
                    return@launch
                }
                val doc = firestore.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    val name = doc.getString("name") ?: ""
                    val genderStr = doc.getString("gender") ?: "MALE"
                    val gender = if (genderStr.equals("FEMALE", ignoreCase = true)) Gender.FEMALE else Gender.MALE
                    val age = doc.getLong("age")?.toInt() ?: 25
                    val city = doc.getString("city") ?: ""
                    val country = doc.getString("country") ?: ""
                    val imageUrl = doc.getString("imageUrl") ?: ""
                    val sectStr = doc.getString("sect") ?: "SUNNI"
                    val sect = try { Sect.valueOf(sectStr.uppercase()) } catch(e: Exception) { Sect.SUNNI }
                    val prayerStr = doc.getString("prayerFrequency") ?: "ALWAYS"
                    val prayer = try { PrayerFrequency.valueOf(prayerStr.uppercase()) } catch(e: Exception) { PrayerFrequency.ALWAYS }
                    val modestyStr = doc.getString("modestyPreference") ?: "HIJAB"
                    val modesty = try { ModestyPreference.valueOf(modestyStr.uppercase()) } catch(e: Exception) { ModestyPreference.HIJAB }
                    val relocationStr = doc.getString("relocationWillingness") ?: "OPEN"
                    val relocation = try { RelocationWillingness.valueOf(relocationStr.uppercase()) } catch(e: Exception) { RelocationWillingness.OPEN }
                    
                    val guardianName = doc.getString("guardianName")
                    val guardianEmail = doc.getString("guardianEmail")
                    val guardianStatus = doc.getString("guardianStatus")
                    
                    val photoApproved = doc.get("photoAccessApprovedUsers") as? List<String> ?: emptyList()
                    val photoRequests = doc.get("photoAccessRequests") as? List<String> ?: emptyList()

                    val isWaliAccount = doc.getBoolean("isWaliAccount") ?: false
                    val wardUid = doc.getString("wardUid")
                    val verificationStatus = doc.getString("verificationStatus") ?: "NONE"
                    val voiceIntroUrl = doc.getString("voiceIntroUrl")
                    val fcmToken = doc.getString("fcmToken")

                    _currentUserProfile.value = UserProfile(
                        uid = uid,
                        name = name,
                        gender = gender,
                        age = age,
                        city = city,
                        country = country,
                        imageUrl = imageUrl,
                        sect = sect,
                        prayerFrequency = prayer,
                        modestyPreference = modesty,
                        relocationWillingness = relocation,
                        guardianName = guardianName,
                        guardianEmail = guardianEmail,
                        guardianStatus = guardianStatus,
                        photoAccessApprovedUsers = photoApproved,
                        photoAccessRequests = photoRequests,
                        isWaliAccount = isWaliAccount,
                        wardUid = wardUid,
                        verificationStatus = verificationStatus,
                        voiceIntroUrl = voiceIntroUrl,
                        fcmToken = fcmToken
                    )

                    // Retrieve & register FCM token automatically
                    try {
                        val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                        firestore.collection("users").document(uid).update("fcmToken", token)
                        _currentUserProfile.value = _currentUserProfile.value?.copy(fcmToken = token)
                    } catch(e: Exception) {
                        // Firebase messaging not configured or network error
                    }
                }
            } catch (e: Exception) {
                // Fail-safe
            }
        }
    }

    /**
     * Authenticates user using Firebase Auth Email & Password.
     */
    fun signIn(email: String, emailPassed: String, isWali: Boolean = false) {
        if (email.isBlank() || emailPassed.isBlank()) {
            _authState.value = AuthState.Error("Please fill out all credentials.")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val isMock = try {
                    auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
                } catch (e: Exception) {
                    true
                }

                if (isMock) {
                    kotlinx.coroutines.delay(800)
                    val uid = if (isWali) "mock_wali_123" else "mock_user_123"
                    context?.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)?.edit()?.apply {
                        putString("uid", uid)
                        putBoolean("isWaliAccount", isWali)
                        if (isWali) {
                            putString("name", "Guardian / ولي أمر")
                            putString("wardUid", "mock_user_123")
                        } else {
                            putString("name", "Mock User")
                        }
                        apply()
                    }
                    _authState.value = AuthState.Authenticated(uid)
                    fetchCurrentUserProfile(uid)
                    return@launch
                }

                val result = auth.signInWithEmailAndPassword(email.trim(), emailPassed).await()
                val user = result.user
                if (user != null) {
                    fetchCurrentUserProfile(user.uid)
                    _authState.value = AuthState.Authenticated(user.uid)
                } else {
                    _authState.value = AuthState.Error("Failed to authenticate.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Invalid email or password.")
            }
        }
    }

    /**
     * Creates new user credentials on Firebase Auth and inserts profile preferences into Firestore.
     */
    fun signUp(
        email: String,
        passwordPass: String,
        profile: UserProfile,
        localImageUri: android.net.Uri? = null,
        localVoiceUri: android.net.Uri? = null,
        context: android.content.Context? = null
    ) {
        if (email.isBlank() || passwordPass.isBlank() || profile.name.isBlank()) {
            _authState.value = AuthState.Error("Core credentials cannot be blank.")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val isMock = try {
                    auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
                } catch (e: Exception) {
                    true
                }

                if (isMock) {
                    kotlinx.coroutines.delay(800)
                    val finalImageUrl = if (localImageUri != null && context != null) {
                        saveImageLocally(context, localImageUri, "mock_user_123")
                    } else {
                        profile.imageUrl
                    }
                    val finalVoiceUrl = if (localVoiceUri != null && context != null) {
                        saveVoiceLocally(context, localVoiceUri, "mock_user_123")
                    } else {
                        null
                    }
                    context?.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)?.edit()?.apply {
                        putString("uid", "mock_user_123")
                        putString("name", profile.name.trim())
                        putString("gender", profile.gender.name)
                        putInt("age", profile.age)
                        putString("city", profile.city.trim())
                        putString("country", profile.country.trim())
                        putString("imageUrl", finalImageUrl)
                        putString("sect", profile.sect.name)
                        putString("prayerFrequency", profile.prayerFrequency.name)
                        putString("modestyPreference", profile.modestyPreference.name)
                        putString("relocationWillingness", profile.relocationWillingness.name)
                        putString("voiceIntroUrl", finalVoiceUrl)
                        putString("verificationStatus", "NONE")
                        putBoolean("isWaliAccount", false)
                        apply()
                    }
                    _currentUserProfile.value = profile.copy(
                        uid = "mock_user_123", 
                        imageUrl = finalImageUrl,
                        voiceIntroUrl = finalVoiceUrl,
                        verificationStatus = "NONE"
                    )
                    _authState.value = AuthState.Authenticated("mock_user_123")
                    return@launch
                }

                // 1. Create User in Firebase Auth
                val authResult = auth.createUserWithEmailAndPassword(email.trim(), passwordPass).await()
                val userId = authResult.user?.uid

                if (userId != null) {
                    // Upload Image to Storage (or fallback to local if upload fails)
                    val finalImageUrl = if (localImageUri != null && context != null) {
                        try {
                            uploadProfileImage(userId, localImageUri, context)
                        } catch (e: Exception) {
                            saveImageLocally(context, localImageUri, userId)
                        }
                    } else {
                        profile.imageUrl
                    }

                    // Upload Voice Intro to Storage
                    val finalVoiceUrl = if (localVoiceUri != null && context != null) {
                        try {
                            uploadVoiceIntro(userId, localVoiceUri, context)
                        } catch (e: Exception) {
                            saveVoiceLocally(context, localVoiceUri, userId)
                        }
                    } else {
                        null
                    }

                    // 2. Save profile to Firestore users database
                    val userProfilePayload = mapOf(
                        "uid" to userId,
                        "name" to profile.name.trim(),
                        "gender" to profile.gender.name,
                        "age" to profile.age,
                        "city" to profile.city.trim(),
                        "country" to profile.country.trim(),
                        "imageUrl" to finalImageUrl,
                        "sect" to profile.sect.name,
                        "prayerFrequency" to profile.prayerFrequency.name,
                        "modestyPreference" to profile.modestyPreference.name,
                        "relocationWillingness" to profile.relocationWillingness.name,
                        "polygamyAcceptance" to profile.polygamyAcceptance,
                        "guardianStatus" to "None",
                        "isPremium" to false,
                        "isWaliAccount" to false,
                        "verificationStatus" to "NONE",
                        "voiceIntroUrl" to (finalVoiceUrl ?: "")
                    )

                    firestore.collection("users")
                        .document(userId)
                        .set(userProfilePayload)
                        .await()

                    fetchCurrentUserProfile(userId)
                    _authState.value = AuthState.Authenticated(userId)
                } else {
                    _authState.value = AuthState.Error("Could not retrieve created user ID.")
                }

            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to sign up.")
            }
        }
    }

    private fun saveVoiceLocally(context: android.content.Context, voiceUri: android.net.Uri, userId: String): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(voiceUri) ?: return ""
            val directory = java.io.File(context.filesDir, "voices")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val localFile = java.io.File(directory, "$userId.mp4")
            val outputStream = java.io.FileOutputStream(localFile)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            android.net.Uri.fromFile(localFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private suspend fun uploadVoiceIntro(userId: String, voiceUri: android.net.Uri, context: android.content.Context): String {
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
        val voiceRef = storageRef.child("voices/$userId.mp4")
        val inputStream = context.contentResolver.openInputStream(voiceUri) ?: throw java.io.IOException("Unable to open input stream")
        val bytes = inputStream.readBytes()
        inputStream.close()
        voiceRef.putBytes(bytes).await()
        return voiceRef.downloadUrl.await().toString()
    }

    private suspend fun uploadProfileImage(userId: String, imageUri: android.net.Uri, context: android.content.Context): String {
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
        val profileImageRef = storageRef.child("profiles/$userId.jpg")
        val inputStream = context.contentResolver.openInputStream(imageUri) ?: throw java.io.IOException("Unable to open input stream")
        val bytes = inputStream.readBytes()
        inputStream.close()
        profileImageRef.putBytes(bytes).await()
        return profileImageRef.downloadUrl.await().toString()
    }

    private fun saveImageLocally(context: android.content.Context, imageUri: android.net.Uri, userId: String): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri) ?: return ""
            val directory = java.io.File(context.filesDir, "profiles")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val localFile = java.io.File(directory, "$userId.jpg")
            val outputStream = java.io.FileOutputStream(localFile)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            android.net.Uri.fromFile(localFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun signInWithGoogle(idToken: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val isMock = auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
                if (isMock) {
                    kotlinx.coroutines.delay(800)
                    _authState.value = AuthState.Authenticated("mock_user_google_123")
                    return@launch
                }

                val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user
                if (user != null) {
                    val doc = firestore.collection("users").document(user.uid).get().await()
                    if (!doc.exists()) {
                        val name = user.displayName ?: user.email?.substringBefore("@") ?: "Google User"
                        val userProfilePayload = mapOf(
                            "uid" to user.uid,
                            "name" to name,
                            "gender" to "Male",
                            "age" to 25,
                            "city" to "Cairo",
                            "country" to "Egypt",
                            "imageUrl" to (user.photoUrl?.toString() ?: "avatar_brother_green"),
                            "sect" to "Sunni",
                            "prayerFrequency" to "Always",
                            "modestyPreference" to "High",
                            "relocationWillingness" to "Open",
                            "polygamyAcceptance" to false,
                            "guardianStatus" to "None",
                            "isPremium" to false
                        )
                        firestore.collection("users")
                            .document(user.uid)
                            .set(userProfilePayload)
                            .await()
                    }
                    fetchCurrentUserProfile(user.uid)
                    _authState.value = AuthState.Authenticated(user.uid)
                } else {
                    _authState.value = AuthState.Error("Failed to authenticate with Google.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to sign in with Google.")
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _currentUserProfile.value = null
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun verifySelfie(imageUri: android.net.Uri, context: android.content.Context, onSuccess: (Boolean) -> Unit) {
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build()
            val detector = FaceDetection.getClient(options)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    onSuccess(faces.isNotEmpty())
                }
                .addOnFailureListener {
                    onSuccess(false)
                }
        } catch (e: Exception) {
            onSuccess(false)
        }
    }

    fun submitVerification(idCardUri: android.net.Uri, selfieUri: android.net.Uri, context: android.content.Context, onResult: (Boolean, String) -> Unit) {
        verifySelfie(selfieUri, context) { hasFace ->
            if (!hasFace) {
                onResult(false, "لم يتم اكتشاف وجه في الصورة الشخصية. يرجى التقاط صورة شخصية واضحة.")
                return@verifySelfie
            }
            viewModelScope.launch {
                try {
                    val userId = auth.currentUser?.uid ?: _currentUserProfile.value?.uid
                    if (userId == null) {
                        onResult(false, "المستخدم غير مسجل.")
                        return@launch
                    }
                    val isMock = try {
                        auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
                    } catch (e: Exception) {
                        true
                    }

                    if (isMock) {
                        val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
                        prefs.edit().putString("verificationStatus", "PENDING").apply()
                        _currentUserProfile.value = _currentUserProfile.value?.copy(verificationStatus = "PENDING")
                        onResult(true, "تم تقديم طلب التحقق بنجاح، وهو قيد المراجعة.")
                        return@launch
                    }

                    val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
                    val idRef = storageRef.child("verification/$userId/id_card.jpg")
                    val selfieRef = storageRef.child("verification/$userId/selfie.jpg")

                    val idStream = context.contentResolver.openInputStream(idCardUri) ?: throw java.io.IOException("Cannot open ID card")
                    val selfieStream = context.contentResolver.openInputStream(selfieUri) ?: throw java.io.IOException("Cannot open Selfie")

                    idRef.putBytes(idStream.readBytes()).await()
                    selfieRef.putBytes(selfieStream.readBytes()).await()
                    idStream.close()
                    selfieStream.close()

                    firestore.collection("users").document(userId)
                        .update("verificationStatus", "PENDING").await()

                    _currentUserProfile.value = _currentUserProfile.value?.copy(verificationStatus = "PENDING")
                    onResult(true, "تم تقديم طلب التحقق بنجاح، وهو قيد المراجعة.")
                } catch (e: Exception) {
                    onResult(false, "حدث خطأ أثناء رفع المستندات: ${e.localizedMessage}")
                }
            }
        }
    }

    fun mockAdminApproveVerification(context: android.content.Context) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: _currentUserProfile.value?.uid ?: return@launch
            val isMock = try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }
            if (isMock) {
                val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
                prefs.edit().putString("verificationStatus", "VERIFIED").apply()
                _currentUserProfile.value = _currentUserProfile.value?.copy(verificationStatus = "VERIFIED")
            } else {
                firestore.collection("users").document(userId)
                    .update("verificationStatus", "VERIFIED").await()
                _currentUserProfile.value = _currentUserProfile.value?.copy(verificationStatus = "VERIFIED")
            }
        }
    }
}
