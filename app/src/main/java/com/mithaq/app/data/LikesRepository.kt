package com.mithaq.app.data

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class LikesRepository(private val context: Context) {

    private val db by lazy { FirebaseFirestore.getInstance() }
    
    private val isMock: Boolean
        get() = com.mithaq.app.Config.isMock()

    private val prefs by lazy {
        context.getSharedPreferences("mithaq_interactions_prefs", Context.MODE_PRIVATE)
    }

    /**
     * Fetches the display name of a user from Firestore.
     * Returns "عضو" as a safe fallback if the document does not exist.
     */
    private suspend fun getUserDisplayName(uid: String): String {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            doc.getString("name") ?: "عضو"
        } catch (e: Exception) {
            "عضو"
        }
    }

    /**
     * Writes a PENDING notification document to the Firestore /notifications collection.
     * The MainActivity real-time listener picks this up and shows a local alert on the recipient's device.
     */
    private suspend fun sendFirestoreNotification(senderUid: String, recipientUid: String, title: String, body: String) {
        try {
            val notif = hashMapOf(
                "senderUid" to senderUid,
                "recipientUid" to recipientUid,
                "title" to title,
                "body" to body,
                "status" to "PENDING",
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("notifications").add(notif).await()
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    // ================= LIKES & MUTUAL MATCHES =================

    suspend fun addLike(fromUid: String, toUid: String): Boolean {
        if (isMock) {
            val likesStr = prefs.getString("likes_$fromUid", "[]") ?: "[]"
            val array = JSONArray(likesStr)
            var exists = false
            for (i in 0 until array.length()) {
                if (array.getString(i) == toUid) exists = true
            }
            if (!exists) {
                array.put(toUid)
                prefs.edit().putString("likes_$fromUid", array.toString()).apply()

                // Register that fromUid liked toUid for toUid's "Who Liked Me" list
                val likedByStr = prefs.getString("who_liked_me_$toUid", "[]") ?: "[]"
                val likedByArray = JSONArray(likedByStr).apply { put(fromUid) }
                prefs.edit().putString("who_liked_me_$toUid", likedByArray.toString()).apply()

                // SEND AUTOMATIC MESSAGE to toUid
                val fromName = context.getSharedPreferences("mithaq_mock_auth", Context.MODE_PRIVATE).getString("name", "عضو")
                // sendMockAutomaticMessage(fromUid, toUid, "لقد أعجب $fromName بملفك الشخصي!")

                // QUEUE NOTIFICATION for the recipient (toUid)
                /*
                queueMockNotification(
                    toUid,
                    "ميثاق - إعجاب جديد",
                    "أعجب $fromName بملفك الشخصي للتو!"
                )
                */
            }

            // Check if toUid liked fromUid (Mutual)
            val targetLikesStr = prefs.getString("likes_$toUid", "[]") ?: "[]"
            val targetArray = JSONArray(targetLikesStr)
            var mutual = false
            for (i in 0 until targetArray.length()) {
                if (targetArray.getString(i) == fromUid) mutual = true
            }
            if (mutual) {
                // Save mutual status
                val mutualsA = prefs.getString("mutuals_$fromUid", "[]") ?: "[]"
                val mutualsB = prefs.getString("mutuals_$toUid", "[]") ?: "[]"
                val arrA = JSONArray(mutualsA).apply { put(toUid) }
                val arrB = JSONArray(mutualsB).apply { put(fromUid) }
                prefs.edit()
                    .putString("mutuals_$fromUid", arrA.toString())
                    .putString("mutuals_$toUid", arrB.toString())
                    .apply()
                
                // Create a mock chat room
                // createMockChatRoom(fromUid, toUid)
            }
            return mutual
        } else {
            try {
                val likeDocId = "${fromUid}_${toUid}"
                val inverseDocId = "${toUid}_${fromUid}"
                
                val isMutual = db.runTransaction { transaction ->
                    val inverseSnap = transaction.get(db.collection("likes").document(inverseDocId))
                    val exists = inverseSnap.exists()

                    val likeData = hashMapOf(
                        "fromUid" to fromUid,
                        "toUid" to toUid,
                        // Clients are never allowed to grant mutual state. A Cloud Function
                        // promotes both like documents after verifying the inverse like.
                        "isMutual" to false,
                        "timestamp" to System.currentTimeMillis()
                    )
                    transaction.set(db.collection("likes").document(likeDocId), likeData)
                    exists
                }.await()

                // Notifications and mutual-state promotion are server-owned. The local
                // return value only lets the UI show an optimistic "mutual" hint.
                return isMutual
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                return false
            }
        }
    }

    /**
     * Removes a like (un-like). Deletes the fromUid -> toUid like document; the inverse like, if any,
     * is left untouched. Mirrors [addLike]. Firestore rules allow the owner to delete their own like.
     */
    suspend fun removeLike(fromUid: String, toUid: String): Boolean {
        if (isMock) {
            removeFromMockArray("likes_$fromUid", toUid)
            removeFromMockArray("who_liked_me_$toUid", fromUid)
            // Undo any mutual flag locally so the UI no longer treats them as matched.
            removeFromMockArray("mutuals_$fromUid", toUid)
            removeFromMockArray("mutuals_$toUid", fromUid)
            return true
        } else {
            return try {
                db.collection("likes").document("${fromUid}_${toUid}").delete().await()
                true
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                false
            }
        }
    }

    private fun removeFromMockArray(key: String, value: String) {
        val source = JSONArray(prefs.getString(key, "[]") ?: "[]")
        val result = JSONArray()
        for (i in 0 until source.length()) {
            val item = source.getString(i)
            if (item != value) result.put(item)
        }
        prefs.edit().putString(key, result.toString()).apply()
    }

    suspend fun getLikesList(userUid: String): List<String> {
        if (isMock) {
            val likesStr = prefs.getString("likes_$userUid", "[]") ?: "[]"
            val array = JSONArray(likesStr)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            return list
        } else {
            try {
                val snapshot = db.collection("likes")
                    .whereEqualTo("fromUid", userUid)
                    .get()
                    .await()
                return snapshot.documents.mapNotNull { it.getString("toUid") }
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                return emptyList()
            }
        }
    }

    suspend fun getWhoLikedMe(userUid: String): List<String> {
        if (isMock) {
            val likedByStr = prefs.getString("who_liked_me_$userUid", "[]") ?: "[]"
            val array = JSONArray(likedByStr)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            return list.distinct()
        } else {
            try {
                val snapshot = db.collection("likes")
                    .whereEqualTo("toUid", userUid)
                    .get()
                    .await()
                return snapshot.documents.mapNotNull { it.getString("fromUid") }
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                return emptyList()
            }
        }
    }

    suspend fun getMutualMatches(userUid: String): List<String> {
        if (isMock) {
            val mutualsStr = prefs.getString("mutuals_$userUid", "[]") ?: "[]"
            val array = JSONArray(mutualsStr)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            return list
        } else {
            try {
                val snap1 = db.collection("likes")
                    .whereEqualTo("fromUid", userUid)
                    .whereEqualTo("isMutual", true)
                    .get()
                    .await()
                val snap2 = db.collection("likes")
                    .whereEqualTo("toUid", userUid)
                    .whereEqualTo("isMutual", true)
                    .get()
                    .await()
                val list1 = snap1.documents.mapNotNull { it.getString("toUid") }
                val list2 = snap2.documents.mapNotNull { it.getString("fromUid") }
                return (list1 + list2).distinct()
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                return emptyList()
            }
        }
    }

    // ================= PROFILE VIEWS =================

    suspend fun addProfileView(viewerUid: String, viewedUid: String) {
        if (viewerUid == viewedUid) return
        
        var isIncognito = false
        if (isMock) {
            val mockAuthPrefs = context.getSharedPreferences("mithaq_mock_auth", Context.MODE_PRIVATE)
            isIncognito = mockAuthPrefs.getBoolean("isIncognito", false)
        } else {
            try {
                val viewerDoc = db.collection("users").document(viewerUid).get().await()
                isIncognito = viewerDoc.getBoolean("isIncognito") ?: false
            } catch (e: Exception) {
                // Ignore
            }
        }
        if (isIncognito) return
        
        if (isMock) {
            // Visitors of viewedUid (who viewed viewedUid)
            val viewsStr = prefs.getString("views_$viewedUid", "[]") ?: "[]"
            val array = JSONArray(viewsStr)
            var exists = false
            for (i in 0 until array.length()) {
                if (array.getString(i) == viewerUid) exists = true
            }
            if (!exists) {
                array.put(viewerUid)
                prefs.edit().putString("views_$viewedUid", array.toString()).apply()

                // QUEUE NOTIFICATION for the viewed user
                /*
                queueMockNotification(
                    viewedUid,
                    "ميثاق - زائر جديد",
                    "هناك عضو قام بزيارة ملفك الشخصي للتو!"
                )
                */
            }

            // Profiles viewed by viewerUid
            val viewerViewsStr = prefs.getString("viewed_by_$viewerUid", "[]") ?: "[]"
            val viewerArray = JSONArray(viewerViewsStr)
            var viewerExists = false
            for (i in 0 until viewerArray.length()) {
                if (viewerArray.getString(i) == viewedUid) viewerExists = true
            }
            if (!viewerExists) {
                viewerArray.put(viewedUid)
                prefs.edit().putString("viewed_by_$viewerUid", viewerArray.toString()).apply()
            }
        } else {
            try {
                val viewData = hashMapOf(
                    "viewerUid" to viewerUid,
                    "viewedUid" to viewedUid,
                    "timestamp" to System.currentTimeMillis()
                )
                db.collection("profile_views").add(viewData).await()
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    suspend fun getProfileVisitors(userUid: String): List<String> {
        if (isMock) {
            val viewsStr = prefs.getString("views_$userUid", "[]") ?: "[]"
            val array = JSONArray(viewsStr)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            return list
        } else {
            try {
                val snapshot = db.collection("profile_views")
                    .whereEqualTo("viewedUid", userUid)
                    .get()
                    .await()
                return snapshot.documents.mapNotNull { it.getString("viewerUid") }.distinct()
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                return emptyList()
            }
        }
    }

    suspend fun getProfilesIViewed(userUid: String): List<String> {
        if (isMock) {
            val viewsStr = prefs.getString("viewed_by_$userUid", "[]") ?: "[]"
            val array = JSONArray(viewsStr)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            return list
        } else {
            try {
                val snapshot = db.collection("profile_views")
                    .whereEqualTo("viewerUid", userUid)
                    .get()
                    .await()
                return snapshot.documents.mapNotNull { it.getString("viewedUid") }.distinct()
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                return emptyList()
            }
        }
    }

    // ================= FAVORITES =================

    suspend fun toggleFavorite(userUid: String, favoriteUid: String): Boolean {
        if (isMock) {
            val favsStr = prefs.getString("favorites_$userUid", "[]") ?: "[]"
            val array = JSONArray(favsStr)
            var exists = false
            var index = -1
            for (i in 0 until array.length()) {
                if (array.getString(i) == favoriteUid) {
                    exists = true
                    index = i
                }
            }
            val newArray = JSONArray()
            if (exists) {
                for (i in 0 until array.length()) {
                    if (i != index) newArray.put(array.get(i))
                }
                prefs.edit().putString("favorites_$userUid", newArray.toString()).apply()

                // Remove userUid from favorited_by_$favoriteUid
                val fByStr = prefs.getString("favorited_by_$favoriteUid", "[]") ?: "[]"
                val fByArray = JSONArray(fByStr)
                val newFByArray = JSONArray()
                for (j in 0 until fByArray.length()) {
                    if (fByArray.getString(j) != userUid) {
                        newFByArray.put(fByArray.get(j))
                    }
                }
                prefs.edit().putString("favorited_by_$favoriteUid", newFByArray.toString()).apply()

                return false
            } else {
                for (i in 0 until array.length()) {
                    newArray.put(array.get(i))
                }
                newArray.put(favoriteUid)
                prefs.edit().putString("favorites_$userUid", newArray.toString()).apply()

                // Add userUid to favorited_by_$favoriteUid
                val fByStr = prefs.getString("favorited_by_$favoriteUid", "[]") ?: "[]"
                val fByArray = JSONArray(fByStr)
                var existsInFBy = false
                for (j in 0 until fByArray.length()) {
                    if (fByArray.getString(j) == userUid) existsInFBy = true
                }
                if (!existsInFBy) {
                    fByArray.put(userUid)
                    prefs.edit().putString("favorited_by_$favoriteUid", fByArray.toString()).apply()
                }

                return true
            }
        } else {
            try {
                val favDocId = "${userUid}_${favoriteUid}"
                val doc = db.collection("favorites").document(favDocId).get().await()
                if (doc.exists()) {
                    db.collection("favorites").document(favDocId).delete().await()
                    return false
                } else {
                    val favData = hashMapOf(
                        "userUid" to userUid,
                        "favoriteUserUid" to favoriteUid,
                        "timestamp" to System.currentTimeMillis()
                    )
                    db.collection("favorites").document(favDocId).set(favData).await()
                    return true
                }
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                return false
            }
        }
    }

    suspend fun getFavorites(userUid: String): List<String> {
        if (isMock) {
            val favsStr = prefs.getString("favorites_$userUid", "[]") ?: "[]"
            val array = JSONArray(favsStr)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            return list
        } else {
            try {
                val snapshot = db.collection("favorites")
                    .whereEqualTo("userUid", userUid)
                    .get()
                    .await()
                return snapshot.documents.mapNotNull { it.getString("favoriteUserUid") }
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                return emptyList()
            }
        }
    }

    suspend fun getWhoFavoritedMe(userUid: String): List<String> {
        if (isMock) {
            val favsStr = prefs.getString("favorited_by_$userUid", "[]") ?: "[]"
            val array = JSONArray(favsStr)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            return list
        } else {
            try {
                val snapshot = db.collection("favorites")
                    .whereEqualTo("favoriteUserUid", userUid)
                    .get()
                    .await()
                return snapshot.documents.mapNotNull { it.getString("userUid") }
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                return emptyList()
            }
        }
    }

    suspend fun getMutualFavorites(userUid: String): List<String> {
        val myFavs = getFavorites(userUid)
        val whoFavsMe = getWhoFavoritedMe(userUid)
        return myFavs.intersect(whoFavsMe.toSet()).toList()
    }

    }
