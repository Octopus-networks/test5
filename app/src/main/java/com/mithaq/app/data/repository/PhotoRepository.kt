package com.mithaq.app.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.mithaq.app.domain.model.PhotoStatus
import com.mithaq.app.domain.model.PhotoType
import com.mithaq.app.domain.model.PhotoVisibility
import com.mithaq.app.domain.model.UserPhoto
import com.mithaq.app.domain.model.VisibleUserPhoto
import kotlinx.coroutines.tasks.await

class PhotoRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun uploadProfilePhoto(
        userId: String,
        uri: Uri,
        type: String,
        context: Context
    ): PhotoOperationResult {
        val normalizedType = if (type == PhotoType.Extra) PhotoType.Extra else PhotoType.Main
        val currentUser = auth.currentUser
        if (userId.isBlank() || currentUser?.uid != userId || !currentUser.isEmailVerified) {
            return PhotoOperationResult.Error("Please verify your email before uploading photos.")
        }

        return try {
            val photoRef = firestore.collection("userPhotos")
                .document(userId)
                .collection("photos")
                .document()
            val photoId = photoRef.id
            val storagePath = "user_photos/$userId/$photoId.jpg"
            val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes()
            } ?: return PhotoOperationResult.Error("Could not read the selected photo.")

            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("userId", userId)
                .setCustomMetadata("photoId", photoId)
                .build()

            storage.reference.child(storagePath).putBytes(bytes, metadata).await()

            val currentVisibility = getPhotoPrivacyMode(userId)
            val photoData = mapOf(
                "photoId" to photoId,
                "userId" to userId,
                "storagePath" to storagePath,
                "type" to normalizedType,
                "status" to PhotoStatus.PendingReview,
                "visibility" to currentVisibility,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            photoRef.set(photoData, SetOptions.merge()).await()
            PhotoOperationResult.Success(photoId)
        } catch (e: Exception) {
            PhotoOperationResult.Error(e.localizedMessage ?: "Could not upload this photo.")
        }
    }

    suspend fun getMyPhotos(userId: String): List<UserPhoto> {
        if (!canActForUser(userId)) return emptyList()
        return firestore.collection("userPhotos")
            .document(userId)
            .collection("photos")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .map { it.toUserPhoto() }
    }

    suspend fun getPhotoPrivacyMode(userId: String): String {
        if (userId.isBlank()) return PhotoVisibility.ApprovedUsersOnly
        return try {
            val snapshot = firestore.collection("profiles").document(userId).get().await()
            val publicSettings = snapshot.get("publicSettings") as? Map<*, *> ?: emptyMap<Any, Any>()
            val mode = publicSettings["photoPrivacyMode"] as? String
            mode?.takeIf { it in PhotoVisibility.AllowedValues } ?: PhotoVisibility.ApprovedUsersOnly
        } catch (e: Exception) {
            PhotoVisibility.ApprovedUsersOnly
        }
    }

    suspend fun updatePhotoPrivacy(userId: String, visibility: String): PhotoOperationResult {
        val currentUser = auth.currentUser
        if (userId.isBlank() || currentUser?.uid != userId || !currentUser.isEmailVerified) {
            return PhotoOperationResult.Error("Please verify your email before updating photo privacy.")
        }
        val normalized = visibility.takeIf { it in PhotoVisibility.AllowedValues }
            ?: PhotoVisibility.ApprovedUsersOnly
        return try {
            firestore.collection("profiles").document(userId).set(
                mapOf(
                    "publicSettings" to mapOf("photoPrivacyMode" to normalized),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()

            val photos = firestore.collection("userPhotos")
                .document(userId)
                .collection("photos")
                .get()
                .await()
            val batch = firestore.batch()
            photos.documents.forEach { doc ->
                batch.update(
                    doc.reference,
                    mapOf(
                        "visibility" to normalized,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            PhotoOperationResult.Success(normalized)
        } catch (e: Exception) {
            PhotoOperationResult.Error(e.localizedMessage ?: "Could not update photo privacy.")
        }
    }

    suspend fun canViewerAccessPhoto(ownerUserId: String, viewerUserId: String): Boolean {
        if (ownerUserId.isBlank() || viewerUserId.isBlank()) return false
        if (ownerUserId == viewerUserId && canActForUser(viewerUserId)) return true
        val viewer = auth.currentUser
        if (viewer?.uid != viewerUserId || !viewer.isEmailVerified) return false

        val mode = getPhotoPrivacyMode(ownerUserId)
        if (mode == PhotoVisibility.Hidden) return false
        if (mode == PhotoVisibility.BlurredByDefault || mode == PhotoVisibility.ApprovedUsersOnly) {
            return hasApprovedPhotoRequest(ownerUserId, viewerUserId)
        }
        if (mode == PhotoVisibility.MatchedUsersOnly) {
            return hasAcceptedInterest(ownerUserId, viewerUserId)
        }
        return false
    }

    suspend fun getVisiblePhotoForViewer(ownerUserId: String, viewerUserId: String): VisibleUserPhoto? {
        if (!canViewerAccessPhoto(ownerUserId, viewerUserId)) return null
        return try {
            val photo = firestore.collection("userPhotos")
                .document(ownerUserId)
                .collection("photos")
                .whereEqualTo("type", PhotoType.Main)
                .whereEqualTo("status", PhotoStatus.Approved)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.toUserPhoto()
                ?: firestore.collection("userPhotos")
                    .document(ownerUserId)
                    .collection("photos")
                    .whereEqualTo("status", PhotoStatus.Approved)
                    .limit(1)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()
                    ?.toUserPhoto()
                ?: return null
            val url = storage.reference.child(photo.storagePath).downloadUrl.await().toString()
            VisibleUserPhoto(photo = photo, downloadUrl = url)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun hasApprovedPhotoRequest(ownerUserId: String, viewerUserId: String): Boolean {
        val requestId = "${viewerUserId}_$ownerUserId"
        return try {
            firestore.collection("photoRequests").document(requestId).get().await().getString("status") == "approved"
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun hasAcceptedInterest(ownerUserId: String, viewerUserId: String): Boolean {
        return try {
            val forward = firestore.collection("interestRequests").document("${viewerUserId}_$ownerUserId").get().await()
            if (forward.exists() && forward.getString("status") == "accepted") return true
            val reverse = firestore.collection("interestRequests").document("${ownerUserId}_$viewerUserId").get().await()
            reverse.exists() && reverse.getString("status") == "accepted"
        } catch (e: Exception) {
            false
        }
    }

    private fun canActForUser(userId: String): Boolean {
        val user = auth.currentUser
        return user?.uid == userId && user.isEmailVerified
    }

    private fun DocumentSnapshot.toUserPhoto(): UserPhoto {
        return UserPhoto(
            photoId = getString("photoId") ?: id,
            userId = getString("userId").orEmpty(),
            storagePath = getString("storagePath").orEmpty(),
            type = getString("type") ?: PhotoType.Main,
            status = getString("status") ?: PhotoStatus.PendingReview,
            visibility = getString("visibility") ?: PhotoVisibility.ApprovedUsersOnly,
            createdAt = getTimestamp("createdAt")?.toDate(),
            updatedAt = getTimestamp("updatedAt")?.toDate()
        )
    }
}

sealed interface PhotoOperationResult {
    data class Success(val value: String) : PhotoOperationResult
    data class Error(val message: String) : PhotoOperationResult
}
