package com.mithaq.app.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.mithaq.app.domain.model.PhotoAccessLevel
import com.mithaq.app.domain.model.PhotoStatus
import com.mithaq.app.domain.model.PhotoType
import com.mithaq.app.domain.model.PhotoVisibility
import com.mithaq.app.domain.model.UserPhoto
import com.mithaq.app.util.prepareForUpload
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Phase 11 secure photo upload + approved photo display foundation.
 *
 * Privacy boundaries:
 *  - Photos are uploaded to `user_photos/{userId}/{photoId}.jpg` (owner-only writes).
 *  - Metadata lives at `userPhotos/{userId}/photos/{photoId}` (owner-only writes).
 *  - No download URL is ever stored or returned. Viewers only ever receive raw bytes,
 *    and only when Storage security rules permit (owner / admin / approved photo request).
 */
class PhotoRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val photoRequestRepository: PhotoRequestRepository = PhotoRequestRepository(firestore, auth)
) {
    companion object {
        private const val MAX_PHOTO_BYTES = 5L * 1024 * 1024
        const val MAIN_PHOTO_ID = "main"
    }

    private fun photosCollection(userId: String) =
        firestore.collection("userPhotos").document(userId).collection("photos")

    private fun storagePathFor(userId: String, photoId: String) =
        "user_photos/$userId/$photoId.jpg"

    /** Uploads (or replaces) one of the caller's own photos and writes its metadata. */
    suspend fun uploadPhoto(type: PhotoType, imageUri: Uri): PhotoUploadResult {
        val user = auth.currentUser
        if (user == null || !user.isEmailVerified) {
            return PhotoUploadResult.Error("Please verify your email before uploading photos.")
        }
        val userId = user.uid
        // Main photo uses a deterministic id so display can resolve it without reading metadata.
        val photoId = if (type == PhotoType.MAIN) MAIN_PHOTO_ID else "extra_${UUID.randomUUID()}"
        val storagePath = storagePathFor(userId, photoId)
        return try {
            val imageBytes = prepareForUpload(auth.app.applicationContext, imageUri)
            val uploadMetadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()
            storage.reference.child(storagePath)
                .putBytes(imageBytes, uploadMetadata)
                .await()

            val metadata = mapOf(
                "photoId" to photoId,
                "userId" to userId,
                "storagePath" to storagePath,
                "type" to type.raw,
                "status" to PhotoStatus.PENDING_REVIEW.raw,
                "visibility" to PhotoVisibility.BLURRED_BY_DEFAULT.raw,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            photosCollection(userId).document(photoId).set(metadata, SetOptions.merge()).await()
            PhotoUploadResult.Success(photoId)
        } catch (e: Exception) {
            PhotoUploadResult.Error(e.localizedMessage ?: "Could not upload this photo.")
        }
    }

    /** Lists the caller's own photos. Owner-only. */
    suspend fun getMyPhotos(): List<UserPhoto> {
        val user = auth.currentUser ?: return emptyList()
        if (!user.isEmailVerified) return emptyList()
        return try {
            photosCollection(user.uid)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
                .documents
                .map { it.toUserPhoto() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Owner updates the privacy/visibility of one of their own photos. */
    suspend fun updateVisibility(photoId: String, visibility: PhotoVisibility): Boolean {
        val user = auth.currentUser ?: return false
        if (!user.isEmailVerified || photoId.isBlank()) return false
        return try {
            photosCollection(user.uid).document(photoId).update(
                mapOf(
                    "visibility" to visibility.raw,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Resolves how much of [ownerId]'s photo the current viewer may see, from the owner's
     * public privacy mode plus any approved photo request. Never returns a URL.
     */
    suspend fun resolveAccessLevel(ownerId: String, photoPrivacyMode: String): PhotoAccessLevel {
        val viewerId = auth.currentUser?.uid ?: return PhotoAccessLevel.LOCKED
        if (viewerId == ownerId) return PhotoAccessLevel.FULL
        val approved = hasApprovedPhotoRequest(viewerId, ownerId)
        return when (PhotoVisibility.from(photoPrivacyMode)) {
            PhotoVisibility.HIDDEN -> PhotoAccessLevel.LOCKED
            PhotoVisibility.BLURRED_BY_DEFAULT ->
                if (approved) PhotoAccessLevel.FULL else PhotoAccessLevel.BLURRED
            PhotoVisibility.APPROVED_USERS_ONLY ->
                if (approved) PhotoAccessLevel.FULL else PhotoAccessLevel.LOCKED
            // Matched-only access is a future policy; keep it locked in this foundation.
            PhotoVisibility.MATCHED_USERS_ONLY -> PhotoAccessLevel.LOCKED
        }
    }

    private suspend fun hasApprovedPhotoRequest(viewerId: String, ownerId: String): Boolean {
        return photoRequestRepository.getPhotoRequestStatusBetweenUsers(viewerId, ownerId) == "approved"
    }

    /**
     * Loads the owner's photo bytes for an authorised viewer. Storage security rules are the
     * real boundary: this returns null when access is not granted. No URL is exposed.
     */
    suspend fun loadAccessiblePhotoBytes(ownerId: String, photoId: String = MAIN_PHOTO_ID): ByteArray? {
        if (ownerId.isBlank()) return null
        return try {
            storage.reference.child(storagePathFor(ownerId, photoId)).getBytes(MAX_PHOTO_BYTES).await()
        } catch (e: Exception) {
            null
        }
    }

    private fun DocumentSnapshot.toUserPhoto(): UserPhoto {
        return UserPhoto(
            photoId = getString("photoId") ?: id,
            userId = getString("userId").orEmpty(),
            storagePath = getString("storagePath").orEmpty(),
            type = getString("type") ?: PhotoType.EXTRA.raw,
            status = getString("status") ?: PhotoStatus.PENDING_REVIEW.raw,
            visibility = getString("visibility") ?: PhotoVisibility.BLURRED_BY_DEFAULT.raw,
            createdAt = getTimestamp("createdAt")?.toDate(),
            updatedAt = getTimestamp("updatedAt")?.toDate()
        )
    }
}

sealed interface PhotoUploadResult {
    data class Success(val photoId: String) : PhotoUploadResult
    data class Error(val message: String) : PhotoUploadResult
}
