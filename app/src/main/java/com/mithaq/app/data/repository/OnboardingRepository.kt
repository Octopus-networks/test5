package com.mithaq.app.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mithaq.app.Config
import com.mithaq.app.domain.model.OnboardingAnswer
import com.mithaq.app.domain.model.OnboardingStep
import com.mithaq.app.domain.model.QuestionType
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

        // Data-driven: every persisted step writes its value under
        // storageGroup.firestoreKey -> fieldKey inside profiles/{userId}. Private/match-only fields
        // are stored here only; Android never writes publicProfiles (the server Cloud Function owns
        // that mirror — Phase 11.8 privacy closure).
        val profileData = buildProfileGroups(steps, answers) + mapOf(
            "onboardingCompleted" to true,
            "onboardingStep" to "complete",
            "profileCompletionPercent" to completionPercent,
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
            // publicProfiles is mirrored server-side by the mirrorPublicProfile Cloud Function
            // on this profiles/{userId} write. Android no longer writes publicProfiles directly
            // (Phase 11.8 privacy closure).
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

    /**
     * Builds the nested profile map purely from step metadata. Each persisted step's value is placed
     * under its storage group's firestoreKey -> fieldKey. Flow-only steps (breaks/summary) and blank
     * answers are skipped. Everything stays inside profiles/{userId}.
     */
    private fun buildProfileGroups(
        steps: List<OnboardingStep>,
        answers: Map<String, OnboardingAnswer>
    ): Map<String, Any> {
        val groups = mutableMapOf<String, MutableMap<String, Any>>()
        steps.forEach { step ->
            if (!step.isPersisted) return@forEach
            val groupKey = step.storageGroup.firestoreKey
            if (groupKey.isBlank() || step.fieldKey.isBlank()) return@forEach
            val value = answerValue(step, answers[step.id]) ?: return@forEach
            groups.getOrPut(groupKey) { mutableMapOf() }[step.fieldKey] = value
        }
        return groups
    }

    /** Extracts the storable value for a step: a String, a List<String> (multi), or an Int. */
    private fun answerValue(step: OnboardingStep, answer: OnboardingAnswer?): Any? {
        if (answer == null) return null
        return when (step.type) {
            QuestionType.MultiChoice -> answer.selectedOptionIds.takeIf { it.isNotEmpty() }
            QuestionType.SingleChoice,
            QuestionType.YesNo,
            QuestionType.SearchableList,
            QuestionType.PrivacyMode -> answer.selectedOptionIds.firstOrNull()?.takeIf { it.isNotBlank() }
            QuestionType.NumberInput -> answer.number ?: answer.text.toIntOrNull()
            QuestionType.TextInput,
            QuestionType.LongTextInput -> answer.text.trim().takeIf { it.isNotBlank() }
            QuestionType.SectionBreak,
            QuestionType.Summary -> null
        }
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
