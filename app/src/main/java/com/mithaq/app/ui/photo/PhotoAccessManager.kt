package com.mithaq.app.ui.photo

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.mithaq.app.data.local.MithaqDatabase

enum class PhotoAccessState {
    NONE,       // Not requested (photo remains blurred)
    PENDING,    // Request sent, waiting for target user approval
    APPROVED    // Approved (photo unblurs)
}

/**
 * Manager class for Feature 2's Multi-Stage Modesty Photo Unlock.
 * Coordinates photo permission requests and approvals in Firestore.
 */
class PhotoAccessManager(
    private val context: android.content.Context? = null,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Sends a request to view a target user's unblurred photo.
     */
    suspend fun requestPhotoAccess(currentUserId: String, targetUserId: String): Boolean {
        if (com.mithaq.app.Config.isMock() && context != null) {
            return try {
                val db = MithaqDatabase.getDatabase(context)
                val cachedUser = db.userDao().getUser(targetUserId)
                if (cachedUser != null) {
                    val updated = (cachedUser.photoAccessRequests + currentUserId).distinct()
                    val updatedUser = cachedUser.copy(photoAccessRequests = updated)
                    db.userDao().insertUser(updatedUser)
                }

                val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
                val loggedInUid = prefs.getString("uid", "")
                if (targetUserId == loggedInUid) {
                    val requestsStr = prefs.getString("photoAccessRequests", "[]") ?: "[]"
                    val arr = org.json.JSONArray(requestsStr)
                    val list = mutableListOf<String>()
                    for (i in 0 until arr.length()) { list.add(arr.getString(i)) }
                    if (!list.contains(currentUserId)) {
                        list.add(currentUserId)
                        val newArr = org.json.JSONArray(list)
                        prefs.edit().putString("photoAccessRequests", newArr.toString()).apply()
                    }
                }
                true
            } catch (e: Exception) {
                false
            }
        }
        return try {
            firestore.collection("users")
                .document(targetUserId)
                .update("photoAccessRequests", FieldValue.arrayUnion(currentUserId))
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Approves a request from another user to view the current user's unblurred photo.
     */
    suspend fun approvePhotoAccess(currentUserId: String, requestingUserId: String): Boolean {
        if (com.mithaq.app.Config.isMock() && context != null) {
            return try {
                val db = MithaqDatabase.getDatabase(context)
                val cachedUser = db.userDao().getUser(currentUserId)
                if (cachedUser != null) {
                    val updatedApproved = (cachedUser.photoAccessApprovedUsers + requestingUserId).distinct()
                    val updatedRequests = cachedUser.photoAccessRequests - requestingUserId
                    val updatedUser = cachedUser.copy(
                        photoAccessApprovedUsers = updatedApproved,
                        photoAccessRequests = updatedRequests
                    )
                    db.userDao().insertUser(updatedUser)
                }

                val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
                val loggedInUid = prefs.getString("uid", "")
                if (currentUserId == loggedInUid) {
                    // Update SharedPreferences Approved
                    val approvedStr = prefs.getString("photoAccessApprovedUsers", "[]") ?: "[]"
                    val approvedArr = org.json.JSONArray(approvedStr)
                    val approvedList = mutableListOf<String>()
                    for (i in 0 until approvedArr.length()) { approvedList.add(approvedArr.getString(i)) }
                    if (!approvedList.contains(requestingUserId)) {
                        approvedList.add(requestingUserId)
                    }
                    prefs.edit().putString("photoAccessApprovedUsers", org.json.JSONArray(approvedList).toString()).apply()

                    // Update SharedPreferences Requests
                    val requestsStr = prefs.getString("photoAccessRequests", "[]") ?: "[]"
                    val requestsArr = org.json.JSONArray(requestsStr)
                    val requestsList = mutableListOf<String>()
                    for (i in 0 until requestsArr.length()) { requestsList.add(requestsArr.getString(i)) }
                    requestsList.remove(requestingUserId)
                    prefs.edit().putString("photoAccessRequests", org.json.JSONArray(requestsList).toString()).apply()
                }
                true
            } catch (e: Exception) {
                false
            }
        }
        return try {
            val userRef = firestore.collection("users").document(currentUserId)
            
            // Perform atomic transaction: approve user and remove from pending requests list
            firestore.runTransaction { transaction ->
                transaction.update(userRef, "photoAccessApprovedUsers", FieldValue.arrayUnion(requestingUserId))
                transaction.update(userRef, "photoAccessRequests", FieldValue.arrayRemove(requestingUserId))
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Revokes another user's access to view the current user's unblurred photo.
     */
    suspend fun revokePhotoAccess(currentUserId: String, approvedUserId: String): Boolean {
        if (com.mithaq.app.Config.isMock() && context != null) {
            return try {
                val db = MithaqDatabase.getDatabase(context)
                val cachedUser = db.userDao().getUser(currentUserId)
                if (cachedUser != null) {
                    val updatedApproved = cachedUser.photoAccessApprovedUsers - approvedUserId
                    val updatedUser = cachedUser.copy(photoAccessApprovedUsers = updatedApproved)
                    db.userDao().insertUser(updatedUser)
                }

                val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
                val loggedInUid = prefs.getString("uid", "")
                if (currentUserId == loggedInUid) {
                    val approvedStr = prefs.getString("photoAccessApprovedUsers", "[]") ?: "[]"
                    val approvedArr = org.json.JSONArray(approvedStr)
                    val approvedList = mutableListOf<String>()
                    for (i in 0 until approvedArr.length()) { approvedList.add(approvedArr.getString(i)) }
                    approvedList.remove(approvedUserId)
                    prefs.edit().putString("photoAccessApprovedUsers", org.json.JSONArray(approvedList).toString()).apply()
                }
                true
            } catch (e: Exception) {
                false
            }
        }
        return try {
            // Storage grants photo downloads from photoRequests/{viewer}_{owner}.status ==
            // "approved", so deleting that document is what actually cuts off access.
            firestore.collection("photoRequests")
                .document("${approvedUserId}_${currentUserId}")
                .delete()
                .await()
            firestore.collection("users")
                .document(currentUserId)
                .update("photoAccessApprovedUsers", FieldValue.arrayRemove(approvedUserId))
                .await()
            true
        } catch (e: Exception) {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            false
        }
    }

    /**
     * Rejects (dismisses) a pending request to view the current user's unblurred photo,
     * without granting access. Simply removes the requester from the pending list.
     */
    suspend fun rejectPhotoAccess(currentUserId: String, requestingUserId: String): Boolean {
        if (com.mithaq.app.Config.isMock() && context != null) {
            return try {
                val db = MithaqDatabase.getDatabase(context)
                val cachedUser = db.userDao().getUser(currentUserId)
                if (cachedUser != null) {
                    val updatedRequests = cachedUser.photoAccessRequests - requestingUserId
                    db.userDao().insertUser(cachedUser.copy(photoAccessRequests = updatedRequests))
                }

                val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
                val loggedInUid = prefs.getString("uid", "")
                if (currentUserId == loggedInUid) {
                    val requestsStr = prefs.getString("photoAccessRequests", "[]") ?: "[]"
                    val requestsArr = org.json.JSONArray(requestsStr)
                    val requestsList = mutableListOf<String>()
                    for (i in 0 until requestsArr.length()) { requestsList.add(requestsArr.getString(i)) }
                    requestsList.remove(requestingUserId)
                    prefs.edit().putString("photoAccessRequests", org.json.JSONArray(requestsList).toString()).apply()
                }
                true
            } catch (e: Exception) {
                false
            }
        }
        return try {
            firestore.collection("users")
                .document(currentUserId)
                .update("photoAccessRequests", FieldValue.arrayRemove(requestingUserId))
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Determines whether the current user is permitted to view the target user's unblurred photo.
     */
    suspend fun checkPhotoAccessState(currentUserId: String, targetUserId: String): PhotoAccessState {
        if (com.mithaq.app.Config.isMock() && context != null) {
            val prefs = context.getSharedPreferences("mithaq_mock_auth", android.content.Context.MODE_PRIVATE)
            val loggedInUid = prefs.getString("uid", "")
            
            val approvedUsers: List<String>
            val pendingRequests: List<String>
            
            if (targetUserId == loggedInUid) {
                // Read from SharedPreferences
                val approvedStr = prefs.getString("photoAccessApprovedUsers", "[]") ?: "[]"
                val approvedArr = org.json.JSONArray(approvedStr)
                val appList = mutableListOf<String>()
                for (i in 0 until approvedArr.length()) { appList.add(approvedArr.getString(i)) }
                approvedUsers = appList
                
                val requestsStr = prefs.getString("photoAccessRequests", "[]") ?: "[]"
                val requestsArr = org.json.JSONArray(requestsStr)
                val reqList = mutableListOf<String>()
                for (i in 0 until requestsArr.length()) { reqList.add(requestsArr.getString(i)) }
                pendingRequests = reqList
            } else {
                // Read from Room DB
                val db = MithaqDatabase.getDatabase(context)
                val cachedUser = db.userDao().getUser(targetUserId)
                if (cachedUser != null) {
                    approvedUsers = cachedUser.photoAccessApprovedUsers
                    pendingRequests = cachedUser.photoAccessRequests
                } else {
                    approvedUsers = emptyList()
                    pendingRequests = emptyList()
                }
            }
            
            return when {
                approvedUsers.contains(currentUserId) -> PhotoAccessState.APPROVED
                pendingRequests.contains(currentUserId) -> PhotoAccessState.PENDING
                else -> PhotoAccessState.NONE
            }
        }
        return try {
            val doc = firestore.collection("users")
                .document(targetUserId)
                .get()
                .await()

            if (!doc.exists()) return PhotoAccessState.NONE

            val approvedUsers = doc.get("photoAccessApprovedUsers") as? List<*> ?: emptyList<Any>()
            val pendingRequests = doc.get("photoAccessRequests") as? List<*> ?: emptyList<Any>()

            when {
                approvedUsers.contains(currentUserId) -> PhotoAccessState.APPROVED
                pendingRequests.contains(currentUserId) -> PhotoAccessState.PENDING
                else -> PhotoAccessState.NONE
            }
        } catch (e: Exception) {
            PhotoAccessState.NONE
        }
    }
}
