package com.mithaq.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.mithaq.app.data.local.MithaqDatabase
import com.mithaq.app.data.local.toCached
import com.mithaq.app.model.Gender
import com.mithaq.app.model.UserProfile
import com.mithaq.app.ui.auth.AuthState
import kotlinx.coroutines.flow.asStateFlow

class ProfileEditViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val context: android.content.Context? = null,
    private val _currentUserProfile: MutableStateFlow<UserProfile?>,
    private val _authState: MutableStateFlow<AuthState>,
    private val fetchCurrentUserProfile: (String) -> Unit
) : ViewModel() {

    private val db = context?.let { MithaqDatabase.getDatabase(it) }
    private val userDao = db?.userDao()
    val currentUserProfile: kotlinx.coroutines.flow.StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    fun saveQuestionnaireAnswers(answers: Map<String, String>) {
        viewModelScope.launch {
            val current = _currentUserProfile.value ?: return@launch
            val updated = current.copy(questionnaireAnswers = answers)
            _currentUserProfile.value = updated
            userDao?.insertUser(updated.toCached())

            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }

            if (isMock) {
                val jsonStr = org.json.JSONObject(answers).toString()
                context?.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)?.edit()?.apply {
                    putString("questionnaireAnswers", jsonStr)
                    apply()
                }
            } else {
                try {
                    firestore.collection("users").document(current.uid).update("questionnaireAnswers", answers).await()
                } catch (e: Exception) {
                    // Ignored
                }
            }
        }
    }

    fun updateGoogleUserProfile(
        userId: String,
        name: String,
        username: String,
        age: Int,
        gender: Gender,
        country: String,
        city: String,
        oathChecked: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val trimmedCountry = country.trim()
                val trimmedCity = city.trim()
                val trimmedUsername = username.trim()
                val trimmedName = name.trim()
                val derivedTimezone = com.mithaq.app.util.CountryUtils.getTimezoneForCountry(trimmedCountry)
                db.collection("users").document(userId).set(
                    mapOf(
                        "name" to trimmedName,
                        "username" to trimmedUsername,
                        "age" to age,
                        "gender" to gender.name,
                        "country" to trimmedCountry,
                        "city" to trimmedCity,
                        "oathChecked" to oathChecked,
                        "timezone" to derivedTimezone,
                        "profileComplete" to true
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )

                val updated = (_currentUserProfile.value ?: UserProfile(uid = userId)).copy(
                    uid = userId,
                    name = trimmedName,
                    username = trimmedUsername,
                    age = age,
                    gender = gender,
                    country = trimmedCountry,
                    city = trimmedCity,
                    oathChecked = oathChecked,
                    timezone = derivedTimezone
                )
                _currentUserProfile.value = updated
                userDao?.insertUser(updated.toCached())

                fetchCurrentUserProfile(userId)
                _authState.value = AuthState.Authenticated(userId)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to update profile")
            }
        }
    }

    fun updateBio(aboutYourself: String, idealPartner: String, context: android.content.Context) {
        viewModelScope.launch {
            val current = _currentUserProfile.value ?: return@launch
            val updated = current.copy(aboutYourself = aboutYourself, idealPartner = idealPartner)
            _currentUserProfile.value = updated
            userDao?.insertUser(updated.toCached())
            val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("aboutYourself", aboutYourself)
                putString("idealPartner", idealPartner)
                apply()
            }
            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }
            if (!isMock && current.uid.isNotEmpty()) {
                try {
                    firestore.collection("users").document(current.uid)
                        .update("aboutYourself", aboutYourself, "idealPartner", idealPartner).await()
                } catch (e: Exception) {
                    com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        }
    }

    fun updateBasicInfo(name: String, context: android.content.Context) {
        viewModelScope.launch {
            val current = _currentUserProfile.value ?: return@launch
            val updated = current.copy(name = name)
            _currentUserProfile.value = updated
            userDao?.insertUser(updated.toCached())
            val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("name", name)
                apply()
            }
            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }
            if (!isMock && current.uid.isNotEmpty()) {
                try {
                    firestore.collection("users").document(current.uid)
                        .update("name", name).await()
                } catch (e: Exception) {
                    com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        }
    }

    fun updateGender(gender: Gender, context: android.content.Context) {
        viewModelScope.launch {
            val current = _currentUserProfile.value ?: return@launch
            val updated = current.copy(gender = gender)
            _currentUserProfile.value = updated
            userDao?.insertUser(updated.toCached())
            val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("gender", gender.name)
                apply()
            }
            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }
            if (!isMock && current.uid.isNotEmpty()) {
                try {
                    firestore.collection("users").document(current.uid)
                        .update("gender", gender.name).await()
                } catch (e: Exception) {
                    com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        }
    }

    fun completeCoreProfile(
        name: String,
        username: String,
        age: Int,
        gender: Gender,
        country: String,
        city: String,
        oathChecked: Boolean,
        context: android.content.Context,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val current = _currentUserProfile.value ?: UserProfile()
                val derivedTimezone = com.mithaq.app.util.CountryUtils.getTimezoneForCountry(country.trim())
                val updated = current.copy(
                    name = name.trim(),
                    username = username.trim(),
                    age = age,
                    gender = gender,
                    country = country.trim(),
                    city = city.trim(),
                    oathChecked = oathChecked,
                    timezone = derivedTimezone
                )
                _currentUserProfile.value = updated
                userDao?.insertUser(updated.toCached())

                val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putString("name", name.trim())
                    putString("username", username.trim())
                    putInt("age", age)
                    putString("gender", gender.name)
                    putString("country", country.trim())
                    putString("city", city.trim())
                    putBoolean("oathChecked", oathChecked)
                    putString("timezone", derivedTimezone)
                    apply()
                }

                val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                    auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
                } catch (e: Exception) {
                    true
                }

                if (!isMock && updated.uid.isNotEmpty()) {
                    firestore.collection("users").document(updated.uid)
                        .update(
                            mapOf(
                                "name" to name.trim(),
                                "username" to username.trim(),
                                "age" to age,
                                "gender" to gender.name,
                                "country" to country.trim(),
                                "city" to city.trim(),
                                "oathChecked" to oathChecked,
                                "timezone" to derivedTimezone,
                                "profileComplete" to true
                            )
                        ).await()
                }
                onResult(true, null)
            } catch (e: Exception) {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
                onResult(false, e.localizedMessage ?: "Failed to save profile.")
            }
        }
    }

    fun updateAdditionalImages(images: List<String>, context: android.content.Context) {
        viewModelScope.launch {
            val current = _currentUserProfile.value ?: return@launch
            val updated = current.copy(additionalImages = images)
            _currentUserProfile.value = updated
            userDao?.insertUser(updated.toCached())
            val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
            val arr = org.json.JSONArray()
            images.forEach { arr.put(it) }
            prefs.edit().putString("additionalImages", arr.toString()).apply()
            
            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }
            if (!isMock && current.uid.isNotEmpty()) {
                try {
                    firestore.collection("users").document(current.uid)
                        .update("additionalImages", images).await()
                } catch (e: Exception) {
                    com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        }
    }
}
