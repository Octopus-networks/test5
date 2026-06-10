package com.mithaq.app.ui.onboarding

import android.content.Context
import androidx.annotation.StringRes
import com.mithaq.app.domain.model.LocalizedStringRes
import com.mithaq.app.domain.model.OnboardingPrivacy
import com.mithaq.app.domain.model.OnboardingSection
import com.mithaq.app.domain.model.OnboardingStep
import com.mithaq.app.domain.model.OnboardingStorageGroup
import com.mithaq.app.domain.model.OnboardingValidationRule
import com.mithaq.app.domain.model.QuestionOption
import com.mithaq.app.domain.model.QuestionType
import org.json.JSONArray
import org.json.JSONObject

class OnboardingConfigLoader(private val context: Context) {

    fun loadStepsFromJson(jsonString: String): List<OnboardingStep> {
        val steps = mutableListOf<OnboardingStep>()
        val jsonArray = JSONArray(jsonString)
        
        val sections = mapOf(
            "identity_location" to OnboardingSection(
                "identity_location",
                loc("onb_sec_identity")
            )
        )

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            steps.add(parseStep(obj, sections))
        }
        
        return steps
    }

    private fun parseStep(obj: JSONObject, sections: Map<String, OnboardingSection>): OnboardingStep {
        val id = obj.getString("id")
        val sectionId = obj.getString("sectionId")
        val section = sections[sectionId] ?: OnboardingSection(sectionId, loc("onb_sec_identity"))
        val type = QuestionType.valueOf(obj.getString("type"))
        val titleResName = obj.getString("titleRes")
        val title = loc(titleResName)
        
        val helperText = if (obj.has("helperRes")) loc(obj.getString("helperRes")) else null
        
        val options = mutableListOf<QuestionOption>()
        if (obj.has("options")) {
            val optsArray = obj.getJSONArray("options")
            for (i in 0 until optsArray.length()) {
                val optObj = optsArray.getJSONObject(i)
                options.add(QuestionOption(
                    id = optObj.getString("id"),
                    label = loc(optObj.getString("labelRes")),
                    helperText = if (optObj.has("helperRes")) loc(optObj.getString("helperRes")) else null
                ))
            }
        }

        val rules = mutableListOf<OnboardingValidationRule>()
        if (obj.has("validation")) {
            val valArray = obj.getJSONArray("validation")
            for (i in 0 until valArray.length()) {
                val valObj = valArray.getJSONObject(i)
                when (valObj.getString("rule")) {
                    "Required" -> rules.add(OnboardingValidationRule.Required)
                    "MinLength" -> rules.add(OnboardingValidationRule.MinLength(valObj.getInt("value")))
                    "MaxLength" -> rules.add(OnboardingValidationRule.MaxLength(valObj.getInt("value")))
                    "NumberRange" -> rules.add(OnboardingValidationRule.NumberRange(valObj.getInt("min"), valObj.getInt("max")))
                    "SelectionRange" -> rules.add(OnboardingValidationRule.SelectionRange(valObj.getInt("min"), valObj.getInt("max")))
                }
            }
        } else if (!obj.optBoolean("optional", false) && type != QuestionType.SectionBreak && type != QuestionType.Summary) {
            // Add implicit Required rule if not optional and no explicit rules provided
            rules.add(OnboardingValidationRule.Required)
        }
        
        val isOptional = obj.optBoolean("optional", false)
        val privacy = if (obj.has("privacy")) OnboardingPrivacy.valueOf(obj.getString("privacy")) else OnboardingPrivacy.PRIVATE
        val storageGroup = if (obj.has("storageGroup")) OnboardingStorageGroup.valueOf(obj.getString("storageGroup")) else OnboardingStorageGroup.NONE
        val fieldKey = obj.optString("fieldKey", "")
        val mirrorToPublic = obj.optBoolean("mirror", false)
        val contributesToMatch = obj.optBoolean("match", false)

        return OnboardingStep(
            id = id,
            section = section,
            type = type,
            title = title,
            imageKey = "onb_$id",
            helperText = helperText,
            options = options,
            validationRules = rules,
            isOptional = isOptional,
            privacy = privacy,
            storageGroup = storageGroup,
            fieldKey = fieldKey,
            mirrorToPublic = mirrorToPublic,
            contributesToMatch = contributesToMatch
        )
    }

    private fun loc(resNameBase: String): LocalizedStringRes {
        val enResId = getResId(resNameBase)
        val arResId = getResId("${resNameBase}_ar")
        if (enResId == 0 || arResId == 0) {
            throw IllegalArgumentException("Resource not found for base name: $resNameBase")
        }
        return LocalizedStringRes(enResId, arResId)
    }

    @StringRes
    private fun getResId(resName: String): Int {
        return context.resources.getIdentifier(resName, "string", context.packageName)
    }
}
