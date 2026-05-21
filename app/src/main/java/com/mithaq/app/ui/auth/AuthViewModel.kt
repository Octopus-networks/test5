package com.mithaq.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
                val isMock = auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
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
                            relocationWillingness = relocation
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
                            relocationWillingness = RelocationWillingness.OPEN
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
                        photoAccessRequests = photoRequests
                    )
                }
            } catch (e: Exception) {
                // Fail-safe
            }
        }
    }

    /**
     * Authenticates user using Firebase Auth Email & Password.
     */
    fun signIn(email: String, emailPassed: String) {
        if (email.isBlank() || emailPassed.isBlank()) {
            _authState.value = AuthState.Error("Please fill out all credentials.")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val isMock = auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
                if (isMock) {
                    kotlinx.coroutines.delay(800)
                    _authState.value = AuthState.Authenticated("mock_user_123")
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
                        apply()
                    }
                    _currentUserProfile.value = profile.copy(uid = "mock_user_123", imageUrl = finalImageUrl)
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
                        "isPremium" to false // Free tier by default
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
}
