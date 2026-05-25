package com.mithaq.app.service

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.mithaq.app.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray

/**
 * Service to interact with Gemini AI API using Google Generative AI SDK for Android.
 * Provides features tailored for the Mithaq Islamic Matchmaking Application.
 */
class GeminiService(private val apiKey: String) {

    // Standard model for freeform text generation (e.g. bio improvements)
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )

    // JSON-enforced model for structured data responses (e.g. compatibility score, opening messages)
    private val jsonModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
        }
    )

    /**
     * 1. Improves raw profile description into an attractive, respectful Islamic-oriented text.
     * Returns the improved text in Arabic.
     */
    suspend fun improveProfile(rawText: String): String = withContext(Dispatchers.IO) {
        if (rawText.trim().isEmpty()) {
            return@withContext "يرجى كتابة نبذة أولاً ليتم تحسينها."
        }

        val prompt = """
أنت كاتب محترف وخبير في صياغة الملفات الشخصية لتطبيقات الزواج الإسلامي المحافظ.
قم بتحسين النص التالي المكتوب بواسطة مستخدم لتطبيق "ميثاق للزواج الإسلامي" ليكون جذاباً، بليغاً، ومحترماً متوافقاً مع القيم الإسلامية والشرعية (بدون أي ابتذال أو خروج عن الآداب).
يجب أن تركز الصياغة على الجدية في طلب الزواج والاستقرار والالتزام الديني والأسري.

القواعد:
- يجب أن يكون الرد باللغة العربية الفصحى البسيطة والجميلة فقط.
- لا تضف أي تعليقات أو نصوص خارج النص المحسن نفسه.
- حافظ على جوهر المعلومات التي ذكرها المستخدم دون تأليف معلومات كاذبة.

النص المراد تحسينه:
"$rawText"
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            response.text?.trim() ?: rawText
        } catch (e: Exception) {
            Log.e("GeminiService", "Error in improveProfile: ${e.message}", e)
            rawText
        }
    }

    /**
     * Compatibility Result class holding score (out of 100) and reason in Arabic.
     */
    data class CompatibilityResult(val score: Int, val reason: String)

    /**
     * 2. Calculates compatibility score and explains reasoning between two user profiles.
     * Returns CompatibilityResult containing the score and detailed reason in Arabic.
     */
    suspend fun calculateCompatibility(userA: UserProfile, userB: UserProfile): CompatibilityResult = withContext(Dispatchers.IO) {
        val userAJson = userProfileToJson(userA)
        val userBJson = userProfileToJson(userB)

        val prompt = """
أنت مستشار علاقات زوجية خبير في الشريعة الإسلامية وعلم الاجتماع الأسري.
قم بتحليل بيانات الملفين الشخصيين التاليين لتطبيق زواج إسلامي محافظ (ميثاق)، واحسب نسبة التوافق بينهما من 100 مع كتابة تقرير تحليلي يوضح الأسباب بشكل محترم ومقنع باللغة العربية.

البيانات:
ملف الطرف الأول (User A):
$userAJson

ملف الطرف الثاني (User B):
$userBJson

يجب أن تقيس التوافق بناءً على:
1. الجانب الديني والفكري (المذهب، الصلوات، الالتزام بالضوابط الشرعية).
2. الجانب الاجتماعي والموقع الجغرافي والقدرة على الانتقال.
3. التوافق في المظهر والتعليم والتطلعات المستقبلية والرغبة في الأطفال.

يجب أن ترجع النتيجة كـ JSON كالتالي حصراً دون أي كلام إضافي قبله أو بعده:
{
  "score": 85,
  "reason": "كتابة شرح مفصل لنقاط الالتقاء والتحديات المحتملة بالعربية الفصحى بطريقة محترمة وإيجابية."
}
        """.trimIndent()

        try {
            val response = jsonModel.generateContent(prompt)
            val jsonText = response.text ?: throw Exception("Empty response")
            val jsonObject = JSONObject(jsonText)
            val score = jsonObject.optInt("score", 50)
            val reason = jsonObject.optString("reason", "التوافق بناءً على المعايير المشتركة.")
            CompatibilityResult(score, reason)
        } catch (e: Exception) {
            Log.e("GeminiService", "Error in calculateCompatibility: ${e.message}", e)
            CompatibilityResult(50, "حدث خطأ أثناء حساب التوافق تلقائياً. يرجى مراجعة تفاصيل الملفين يدوياً.")
        }
    }

    /**
     * 3. Suggests 3 respectful, Islamic-compliant opening messages.
     * Returns a List of 3 strings starting with greeting (السلام عليكم).
     */
    suspend fun suggestOpeningMessages(sender: UserProfile, receiver: UserProfile): List<String> = withContext(Dispatchers.IO) {
        val senderJson = userProfileToJson(sender)
        val receiverJson = userProfileToJson(receiver)

        val prompt = """
أنت مستشار محترم لتطبيق "ميثاق للزواج الإسلامي".
اقترح 3 رسائل افتتاحية (Opening Icebreakers) مختلفة ومميزة يستطيع المرسل (Sender) إرسالها للمستقبل (Receiver) لبدء التعارف بهدف الزواج الشرعي.

البيانات:
بيانات المرسل (Sender):
$senderJson

بيانات المستقبل (Receiver):
$receiverJson

القواعد الصارمة للرسائل:
1. يجب أن تبدأ كل رسالة بالسلام الشرعي الكامل "السلام عليكم ورحمة الله وبركاته" أو "السلام عليكم".
2. يجب أن تكون الرسائل محترمة جداً، راقية، وخالية تماماً من أي كلام غزل أو تلميحات غير لائقة.
3. يجب أن تكون الرسالة مبنية على اهتمام أو قاسم مشترك واضح في البيانات لجعلها طبيعية وغير مصطنعة.
4. يجب أن تكون الصياغة باللغة العربية الفصحى.

يجب أن ترجع المخرجات كـ JSON كالتالي حصراً دون أي كلام إضافي قبله أو بعده:
{
  "messages": [
    "الرسالة الأولى الاقتراح الأول...",
    "الرسالة الثانية الاقتراح الثاني...",
    "الرسالة الثالثة الاقتراح الثالث..."
  ]
}
        """.trimIndent()

        val defaultMessages = listOf(
            "السلام عليكم ورحمة الله وبركاته. لقد لفت انتباهي ملفك الشخصي الموقر ورغبتك الجادة في الزواج، ويشرفني التواصل معك للتعارف الشرعي.",
            "السلام عليكم. قرأت أهدافك في ملفك الشخصي وأتمنى لك التوفيق، ووددت الاستفسار عن مدى توافق تطلعاتنا لبناء أسرة صالحة.",
            "السلام عليكم ورحمة الله وبركاته. أرجو أن تكون بخير، يسعدني التعرف على اهتماماتك الدينية والاجتماعية المشتركة وبحث مدى التوافق بيننا."
        )

        try {
            val response = jsonModel.generateContent(prompt)
            val jsonText = response.text ?: throw Exception("Empty response")
            val jsonObject = JSONObject(jsonText)
            val jsonArray = jsonObject.optJSONArray("messages") ?: return@withContext defaultMessages
            val messagesList = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                messagesList.add(jsonArray.getString(i))
            }
            if (messagesList.isNotEmpty()) messagesList else defaultMessages
        } catch (e: Exception) {
            Log.e("GeminiService", "Error in suggestOpeningMessages: ${e.message}", e)
            defaultMessages
        }
    }

    /**
     * Converts a UserProfile domain object to a lightweight JSON structure for model consumption.
     */
    private fun userProfileToJson(profile: UserProfile): String {
        return try {
            JSONObject().apply {
                put("uid", "anonymous_user")
                put("name", "عضو ميثاق")
                put("gender", profile.gender.name)
                put("age", profile.age)
                put("city", profile.city)
                put("country", profile.country)
                put("sect", profile.sect.name)
                put("prayerFrequency", profile.prayerFrequency.name)
                put("modestyPreference", profile.modestyPreference.name)
                put("relocationWillingness", profile.relocationWillingness.name)
                put("maritalStatus", profile.maritalStatus)
                put("aboutYourself", profile.aboutYourself)
                put("idealPartner", profile.idealPartner)
                put("partnerPreferences", profile.partnerPreferences)
                put("occupation", profile.occupation)
                put("religiousValues", profile.religiousValues)
            }.toString()
        } catch (e: Exception) {
            "{}"
        }
    }
}
