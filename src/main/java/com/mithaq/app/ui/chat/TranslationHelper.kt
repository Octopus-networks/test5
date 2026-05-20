package com.mithaq.app.ui.chat

import kotlinx.coroutines.delay

/**
 * Interface for Feature 4: In-Chat Instant Translation.
 * Handles text translation asynchronously (typically connected to Google Cloud Translation or ML Kit).
 */
interface TranslationHelper {
    suspend fun translateText(text: String, targetLanguageCode: String): String
}

/**
 * Robust mock implementation of [TranslationHelper].
 * Includes localized common phrase mapping for authentic cross-cultural matchmaking testing (Arabic <-> English).
 */
class MockTranslationHelper : TranslationHelper {

    private val translations = mapOf(
        // Arabic to English
        "السلام عليكم" to "Peace be upon you",
        "السلام عليكم ورحمة الله وبركاته" to "Peace be upon you and the mercy of Allah and His blessings",
        "كيف حالك؟" to "How are you?",
        "الحمد لله، بخير" to "Praise be to God, I am doing well",
        "أنا أبحث عن زواج جاد" to "I am looking for a serious marriage",
        "هل تقبل الانتقال إلى بلد آخر؟" to "Do you accept relocating to another country?",
        "أنا مهتم بملفك الشخصي" to "I am interested in your profile",
        "إن شاء الله" to "God willing / Hopefully",
        "ما شاء الله" to "What God has willed (expressing appreciation)",
        
        // English to Arabic
        "Peace be upon you" to "السلام عليكم",
        "How are you?" to "كيف حالك؟",
        "I am doing well, thank God" to "أنا بخير، والحمد لله",
        "I am looking for marriage" to "أنا أبحث عن الزواج",
        "Where do you live?" to "أين تعيش؟",
        "Nice to meet you" to "سعدت بلقائك"
    )

    override suspend fun translateText(text: String, targetLanguageCode: String): String {
        // Simulate network delay for realistic UI state changes (loading indicator on chat bubble)
        delay(1000)

        val trimmed = text.trim()
        
        // Search direct mappings
        translations[trimmed]?.let { return it }

        // Fallback: If not found, simulate a smart machine translation
        return if (targetLanguageCode.lowercase() == "ar") {
            "‏[مترجم]: $trimmed"
        } else {
            "[Translated]: $trimmed"
        }
    }
}
