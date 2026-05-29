package com.mithaq.app.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mithaq.app.Config
import com.mithaq.app.domain.model.OnboardingAnswer
import com.mithaq.app.domain.model.OnboardingStep
import kotlinx.coroutines.tasks.await

class OnboardingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val publicProfileRepository: PublicProfileRepository = PublicProfileRepository(firestore, auth),
    context: Context? = null
) {
    private val prefs = context?.getSharedPreferences("mithaq_onboarding_engine_v2", Context.MODE_PRIVATE)

    suspend fun loadCompletionStatus(userId: String): Boolean {
        if (userId.isBlank()) return false
        return try {
            val snapshot = firestore.collection("profiles").document(userId).get().await()
            val completed = snapshot.getBoolean("onboardingCompleted") == true
            saveCompletionCache(userId, completed)
            completed
        } catch (e: Exception) {
            prefs?.getBoolean("completed_$userId", false) == true
        }
    }

    suspend fun saveOnboardingAnswers(
        userId: String,
        answers: Map<String, OnboardingAnswer>,
        steps: List<OnboardingStep>
    ): OnboardingSaveResult {
        if (userId.isBlank()) {
            return OnboardingSaveResult.Error("Please sign in again before saving profile setup.")
        }
        if (!canSaveForUser(userId)) {
            return OnboardingSaveResult.Error("Please verify your email before saving profile setup.")
        }

        val requiredStepIds = steps
            .filter { step -> step.validationRules.isNotEmpty() && !step.isOptional }
            .map { it.id }
            .toSet()
        val answeredRequired = requiredStepIds.count { stepId -> answers[stepId]?.hasContent() == true }
        val completionPercent = if (requiredStepIds.isEmpty()) {
            0
        } else {
            ((answeredRequired.toFloat() / requiredStepIds.size.toFloat()) * 100f).toInt().coerceIn(0, 100)
        }

        val profileData = mapOf(
            "onboardingCompleted" to true,
            "onboardingStep" to "complete",
            "profileCompletionPercent" to completionPercent,
            "basicInfo" to mapOf(
                "accountType" to selected("account_type", answers),
                "name" to text("name", answers),
                "age" to number("age", answers),
                "country" to selected("country", answers),
                "city" to text("city", answers)
            ),
            "personalStatus" to mapOf(
                "maritalStatus" to selected("marital_status", answers)
            ),
            "religiousPractice" to mapOf(
                "prayerHabit" to selected("prayer_habit", answers)
            ),
            "marriageIntent" to mapOf(
                "timeline" to selected("marriage_timeline", answers)
            ),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (!Config.IS_PRODUCTION && auth.currentUser == null) {
            saveCompletionCache(userId, true)
            return OnboardingSaveResult.Success(
                answeredQuestions = answers.values.count { it.hasContent() },
                profileCompletionPercent = completionPercent
            )
        }

        return try {
            firestore.collection("profiles")
                .document(userId)
                .set(profileData, SetOptions.merge())
                .await()
            when (val publicResult = publicProfileRepository.createOrUpdatePublicProfileFromOnboarding(userId)) {
                PublicProfileWriteResult.Success -> Unit
                is PublicProfileWriteResult.Error -> return OnboardingSaveResult.Error(publicResult.message)
            }
            saveCompletionCache(userId, true)
            OnboardingSaveResult.Success(
                answeredQuestions = answers.values.count { it.hasContent() },
                profileCompletionPercent = completionPercent
            )
        } catch (e: Exception) {
            OnboardingSaveResult.Error(
                e.localizedMessage ?: "Could not save profile setup. Please check your connection and try again."
            )
        }
    }

    fun saveCompletionCache(userId: String, completed: Boolean) {
        if (userId.isBlank()) return
        prefs?.edit()?.putBoolean("completed_$userId", completed)?.apply()
    }

    private fun canSaveForUser(userId: String): Boolean {
        val user = auth.currentUser
        if (!Config.IS_PRODUCTION && user == null) return true
        return user?.uid == userId && user.isEmailVerified
    }

    private fun selected(stepId: String, answers: Map<String, OnboardingAnswer>): String {
        return answers[stepId]?.selectedOptionIds?.firstOrNull().orEmpty()
    }

    private fun text(stepId: String, answers: Map<String, OnboardingAnswer>): String {
        return answers[stepId]?.text?.trim().orEmpty()
    }

    private fun number(stepId: String, answers: Map<String, OnboardingAnswer>): Int? {
        return answers[stepId]?.number ?: answers[stepId]?.text?.toIntOrNull()
    }

    private fun OnboardingAnswer.hasContent(): Boolean {
        return selectedOptionIds.isNotEmpty() || text.isNotBlank() || number != null
    }
}

sealed interface OnboardingSaveResult {
    data class Success(
        val answeredQuestions: Int,
        val profileCompletionPercent: Int
    ) : OnboardingSaveResult

    data class Error(val message: String) : OnboardingSaveResult
}
