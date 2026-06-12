package com.mithaq.app.ui.verification

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.mithaq.app.model.UserProfile
import com.mithaq.app.data.local.UserDao
import com.mithaq.app.data.local.MithaqDatabase
import com.mithaq.app.data.local.toCached
import com.mithaq.app.service.BackendFunctions
import com.mithaq.app.util.prepareForUpload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class IdentityVerificationViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val context: Context? = null,
    private val _currentUserProfile: MutableStateFlow<UserProfile?>
) : ViewModel() {

    private val db = context?.let { MithaqDatabase.getDatabase(it) }
    private val userDao = db?.userDao()

    fun verifySelfie(imageUri: android.net.Uri, context: android.content.Context, onSuccess: (Boolean) -> Unit) {
        try {
            val isVideo = context.contentResolver.getType(imageUri)?.startsWith("video") == true 
                || imageUri.path?.endsWith(".mp4") == true
                
            val image: InputImage = if (isVideo) {
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(context, imageUri)
                val bitmap = retriever.getFrameAtTime(1000000, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime 
                    ?: throw java.lang.Exception("Could not extract frame from video")
                retriever.release()
                InputImage.fromBitmap(bitmap, 0)
            } else {
                InputImage.fromFilePath(context, imageUri)
            }

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
        val isArabic = java.util.Locale.getDefault().language == "ar"
        verifySelfie(selfieUri, context) { hasFace ->
            if (!hasFace) {
                onResult(false, if (isArabic) "لم يتم اكتشاف وجه في فيديو السيلفي. يرجى تسجيل فيديو سيلفي واضح ومقرب بوجهك." else "No face detected in selfie video. Please record a clear, close-up selfie video of your face.")
                return@verifySelfie
            }
            viewModelScope.launch {
                try {
                    val userId = auth.currentUser?.uid ?: _currentUserProfile.value?.uid
                    if (userId == null) {
                        onResult(false, "المستخدم غير مسجل.")
                        return@launch
                    }
                    val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                        auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
                    } catch (e: Exception) {
                        true
                    }

                    if (isMock) {
                        val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
                        prefs.edit().putString("verificationStatus", "PENDING").apply()
                        _currentUserProfile.value = _currentUserProfile.value?.copy(verificationStatus = "PENDING")
                        com.mithaq.app.notification.MithaqFirebaseMessagingService.showLocalNotification(
                            context,
                            if (isArabic) "ميثاق - تم إرسال طلب التوثيق" else "Mithaq - Verification Request Sent",
                            if (isArabic) 
                                "تم إرسال طلب التوثيق بالفيديو بنجاح إلى مشرفك (ولي أمرك) وإلى الإدمن للمراجعة."
                            else 
                                "Video verification request has been sent to your Wali & Admin for review."
                        )
                        onResult(true, if (isArabic) "تم تقديم طلب التوثيق بنجاح وإرساله إلى مشرفك (ولي أمرك) وإلى الإدمن للمراجعة." else "Verification request submitted successfully and sent to your supervisor (Wali) and the Admin for review.")
                        return@launch
                    }

                    val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
                    val idRef = storageRef.child("verification/$userId/id_card.jpg")
                    
                    val isVideo = context.contentResolver.getType(selfieUri)?.startsWith("video") == true 
                        || selfieUri.path?.endsWith(".mp4") == true
                    val selfieRef = if (isVideo) {
                        storageRef.child("verification/$userId/selfie_video.mp4")
                    } else {
                        storageRef.child("verification/$userId/selfie.jpg")
                    }

                    val imageMetadata = com.google.firebase.storage.storageMetadata {
                        contentType = "image/jpeg"
                    }
                    idRef.putBytes(
                        prepareForUpload(context, idCardUri),
                        imageMetadata
                    ).await()

                    if (isVideo) {
                        val videoMetadata = com.google.firebase.storage.storageMetadata {
                            contentType = "video/mp4"
                        }
                        selfieRef.putFile(selfieUri, videoMetadata).await()
                    } else {
                        selfieRef.putBytes(
                            prepareForUpload(context, selfieUri),
                            imageMetadata
                        ).await()
                    }

                    firestore.collection("users").document(userId)
                        .update("verificationStatus", "PENDING").await()

                    _currentUserProfile.value = _currentUserProfile.value?.copy(verificationStatus = "PENDING")
                    com.mithaq.app.notification.MithaqFirebaseMessagingService.showLocalNotification(
                        context,
                        if (isArabic) "ميثاق - تم إرسال طلب التوثيق" else "Mithaq - Verification Request Sent",
                        if (isArabic) 
                            "تم إرسال طلب التوثيق بنجاح إلى مشرفك (ولي أمرك) وإلى الإدمن للمراجعة."
                        else 
                            "Verification request has been sent to your Wali & Admin for review."
                    )
                    onResult(true, if (isArabic) "تم تقديم طلب التوثيق بنجاح وإرساله إلى مشرفك (ولي أمرك) وإلى الإدمن للمراجعة." else "Verification request submitted successfully and sent to your supervisor (Wali) and the Admin for review.")
                } catch (e: Exception) {
                    onResult(false, "حدث خطأ أثناء رفع المستندات: ${e.localizedMessage}")
                }
            }
        }
    }

    fun mockAdminApproveVerification(context: android.content.Context) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: _currentUserProfile.value?.uid ?: return@launch
            val isMock = if (com.mithaq.app.Config.IS_PRODUCTION) false else try {
                auth.app?.options?.apiKey == "mock-api-key-for-testing" || auth.app?.options?.apiKey?.contains("mock") == true
            } catch (e: Exception) {
                true
            }
            if (isMock) {
                val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
                prefs.edit().putString("verificationStatus", "VERIFIED").apply()
                _currentUserProfile.value = _currentUserProfile.value?.copy(verificationStatus = "VERIFIED")
            } else {
                try {
                    BackendFunctions.setVerificationStatus(userId, "VERIFIED")
                    _currentUserProfile.value = _currentUserProfile.value?.copy(verificationStatus = "VERIFIED")
                } catch (e: Exception) {
                    com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        }
    }

    fun updateMockRole(isWali: Boolean, isAdmin: Boolean, context: android.content.Context) {
        // SECURITY: Never allow role changes in production. This is strictly a dev/demo tool.
        if (com.mithaq.app.Config.IS_PRODUCTION) {
            android.util.Log.w("AuthViewModel", "updateMockRole called in production — blocked.")
            return
        }
        viewModelScope.launch {
            val current = _currentUserProfile.value ?: return@launch
            val updated = current.copy(isWaliAccount = isWali, isAdmin = isAdmin)
            _currentUserProfile.value = updated
            userDao?.insertUser(updated.toCached())
            val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
            prefs.edit().apply {
                putBoolean("isWaliAccount", isWali)
                putBoolean("isAdmin", isAdmin)
                apply()
            }
        }
    }
}
