package com.mithaq.app.security

import java.util.Locale

/**
 * Utility for safety-related checks.
 */
object SafetyUtils {

    /**
     * Checks if a text contains potential contact information (phone, email, social media handles).
     */
    fun containsContactInfo(text: String): Boolean {
        val lowercase = text.lowercase(Locale.ROOT)
        
        // Email pattern
        val emailPattern = kotlin.text.Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        if (emailPattern.containsMatchIn(lowercase)) return true
        
        // Phone number pattern: specifically looking for international prefixes or local prefixes
        val phoneWithPrefixPattern = kotlin.text.Regex("(?:\\+|00|\\b0)\\d{8,14}\\b")
        if (phoneWithPrefixPattern.containsMatchIn(lowercase)) return true
        
        // Check for common social media platforms and keywords
        val forbiddenKeywords = listOf(
            "insta", "انستا", "إنستا", "سناب", "snap", "telegram", "تليجرام", "تليغرام",
            "whatsapp", "واتساب", "واتس", "رقمي", "phone", "email", "إيميل", "ايميل", "بريد", "فيسبوك",
            "facebook", "twitter", "تويتر", "لينكد", "linkedin", "رقم الهاتف", "رقم الجوال"
        )
        for (keyword in forbiddenKeywords) {
            if (lowercase.contains(keyword)) return true
        }
        
        // Check for @ symbol followed by text (potential handle)
        val handlePattern = kotlin.text.Regex("@[a-zA-Z0-9._]+")
        if (handlePattern.containsMatchIn(lowercase)) return true
        
        return false
    }
}
