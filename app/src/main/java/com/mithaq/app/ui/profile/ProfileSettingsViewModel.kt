package com.mithaq.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mithaq.app.data.repository.UserProfileRepository
import kotlinx.coroutines.launch

/**
 * Thin ViewModel for ProfileSettings — bridges the UI to [UserProfileRepository]
 * so that the Composable never touches Firestore directly.
 *
 * Constructor-injected repository with a default (no Hilt), matching the project
 * convention used by [com.mithaq.app.ui.guardian.GuardianViewModel].
 */
class ProfileSettingsViewModel(
    private val repository: UserProfileRepository = UserProfileRepository()
) : ViewModel() {

    /** Whether the current Firestore instance is a mock/test database. */
    val isMockDatabase: Boolean
        get() = repository.isMockDatabase()

    /**
     * Updates the user's profile image URL in Firestore.
     *
     * @param uid         The user's document ID.
     * @param imageUrl    The new image URL (or avatar ID) to persist.
     * @param onSuccess   Called on the main thread after a successful write.
     * @param onError     Called on the main thread if the write fails.
     */
    fun updateImageUrl(
        uid: String,
        imageUrl: String,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.updateImageUrl(uid, imageUrl)
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
}
