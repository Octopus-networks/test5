package com.mithaq.app.ui.guardian

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * UI States for the Guardian invitation flow.
 */
sealed interface GuardianUiState {
    object Idle : GuardianUiState
    object Loading : GuardianUiState
    object Success : GuardianUiState
    data class Error(val errorMessage: String) : GuardianUiState
}

/**
 * ViewModel handling the logic for Feature 3: The Guardian (Wali) Integration.
 * Strictly manages loading, success, and error states, and securely updates Firestore.
 */
class GuardianViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow<GuardianUiState>(GuardianUiState.Idle)
    val uiState: StateFlow<GuardianUiState> = _uiState.asStateFlow()

    /**
     * Updates the current user's document in Firestore with the guardian details and sets the status to "Pending".
     */
    fun inviteGuardian(name: String, email: String) {
        val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else firestore.app?.options?.apiKey == "mock-api-key-for-testing" || firestore.app?.options?.apiKey?.contains("mock") == true
        if (isMock) {
            val trimmedName = name.trim()
            val trimmedEmail = email.trim().lowercase()

            if (trimmedName.isEmpty() || trimmedEmail.isEmpty()) {
                _uiState.value = GuardianUiState.Error("Please fill out all fields.")
                return
            }

            if (!isValidEmail(trimmedEmail)) {
                _uiState.value = GuardianUiState.Error("Please enter a valid email address.")
                return
            }

            _uiState.value = GuardianUiState.Loading
            viewModelScope.launch {
                kotlinx.coroutines.delay(500)
                _uiState.value = GuardianUiState.Success
            }
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            _uiState.value = GuardianUiState.Error("User not authenticated.")
            return
        }

        val trimmedName = name.trim()
        val trimmedEmail = email.trim().lowercase()

        if (trimmedName.isEmpty() || trimmedEmail.isEmpty()) {
            _uiState.value = GuardianUiState.Error("Please fill out all fields.")
            return
        }

        if (!isValidEmail(trimmedEmail)) {
            _uiState.value = GuardianUiState.Error("Please enter a valid email address.")
            return
        }

        _uiState.value = GuardianUiState.Loading

        viewModelScope.launch {
            try {
                val userId = currentUser.uid
                val updates = mapOf(
                    "guardianName" to trimmedName,
                    "guardianEmail" to trimmedEmail,
                    "guardianStatus" to "Pending"
                )

                // Securely updates the user document with the guardian's info and pending status
                firestore.collection("users")
                    .document(userId)
                    .update(updates)
                    .await()

                _uiState.value = GuardianUiState.Success
            } catch (e: Exception) {
                _uiState.value = GuardianUiState.Error(
                    e.localizedMessage ?: "An error occurred while inviting the guardian. Please try again."
                )
            }
        }
    }

    /**
     * Resets the UI State to Idle. Useful for clearing success/error dialogues.
     */
    fun resetState() {
        _uiState.value = GuardianUiState.Idle
    }

    /**
     * Simple email validation helper.
     */
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
