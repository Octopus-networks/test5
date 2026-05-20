package com.mithaq.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mithaq.app.model.UserProfile
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
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

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
                val result = auth.signInWithEmailAndPassword(email.trim(), emailPassed).await()
                val user = result.user
                if (user != null) {
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
    fun signUp(email: String, passwordPass: String, profile: UserProfile) {
        if (email.isBlank() || passwordPass.isBlank() || profile.name.isBlank()) {
            _authState.value = AuthState.Error("Core credentials cannot be blank.")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                // 1. Create User in Firebase Auth
                val authResult = auth.createUserWithEmailAndPassword(email.trim(), passwordPass).await()
                val userId = authResult.user?.uid

                if (userId != null) {
                    // 2. Save profile to Firestore users database
                    val userProfilePayload = mapOf(
                        "uid" to userId,
                        "name" to profile.name.trim(),
                        "gender" to profile.gender.name,
                        "age" to profile.age,
                        "city" to profile.city.trim(),
                        "country" to profile.country.trim(),
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

                    _authState.value = AuthState.Authenticated(userId)
                } else {
                    _authState.value = AuthState.Error("Could not retrieve created user ID.")
                }

            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to sign up.")
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
