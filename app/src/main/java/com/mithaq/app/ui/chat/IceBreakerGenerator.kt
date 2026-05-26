package com.mithaq.app.ui.chat

/**
 * Utility to generate respectful ice-breaker questions for serious matchmaking.
 */
object IceBreakerGenerator {

    data class IceBreaker(val english: String, val arabic: String)

    val questions = listOf(
        IceBreaker(
            "What are the most important qualities you are looking for in a spouse?",
            "ما هي أهم الصفات التي تبحث عنها في شريك الحياة؟"
        ),
        IceBreaker(
            "How do you balance your career and family life?",
            "كيف توازن بين حياتك المهنية وحياتك الأسرية؟"
        ),
        IceBreaker(
            "What does a typical day look like for you in terms of religious practice?",
            "كيف يبدو يومك المعتاد من حيث الممارسة الدينية؟"
        ),
        IceBreaker(
            "Are you open to relocating after marriage?",
            "هل أنت مستعد للانتقال للسكن في مدينة أخرى بعد الزواج؟"
        ),
        IceBreaker(
            "What are your thoughts on the role of the extended family in marriage?",
            "ما هو رأيك في دور العائلة الكبيرة (الأقارب) في الحياة الزوجية؟"
        ),
        IceBreaker(
            "What are your long-term goals for the next 5-10 years?",
            "ما هي أهدافك طويلة المدى للسنوات الخمس إلى العشر القادمة؟"
        )
    )

    fun getRecommended(isArabic: Boolean): List<String> {
        return questions.map { if (isArabic) it.arabic else it.english }.shuffled().take(3)
    }
}
