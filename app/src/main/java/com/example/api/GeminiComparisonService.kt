package com.example.api

import android.util.Log
import com.example.database.Comparison
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiComparisonService {
    private const val TAG = "GeminiComparisonService"

    private fun cleanJson(raw: String): String {
        var cleaned = raw.trim()
        // Remove markdown formatting block if present
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substringAfter("```json")
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substringAfter("```")
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substringBeforeLast("```")
        }
        return cleaned.trim()
    }

    suspend fun performComparison(
        itemA: String,
        itemB: String,
        context: String,
        customCriteria: String = ""
    ): Comparison = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API Key is not configured in Secrets panel or .env file.")
        }

        val criteriaInstruction = if (customCriteria.isNotBlank()) {
            "Please use these custom criteria for comparison: $customCriteria."
        } else {
            "Determine the 3 to 5 most relevant and logical categories to compare these two items."
        }

        val prompt = """
            You are a rigorous, objective decision assistant. Compare the following two items:
            Item A: "$itemA"
            Item B: "$itemB"
            Context or Purpose: "${context.ifBlank { "General everyday use" }}"
            
            $criteriaInstruction
            
            Evaluate both options thoroughly. Give a final score out of 100 for each option based on their evaluation in this context.
            Provide:
            1. An overall score for Item A and Item B (from 0 to 100).
            2. The winner: "A", "B", or "TIE".
            3. A short, highly cohesive final verdict summarizing why one is better than the other in this context, or why they are tied.
            4. A breakdown of scores (out of 100) per comparison criteria category, with a short analysis text of why those scores were given.
            5. Bullet points of pros and cons for Item A and Item B (at least 2 pros and 2 cons for each).
            
            You MUST return ONLY a raw JSON object matching the following structure exactly. Do not wrap the response in any other text besides the JSON itself.
            
            JSON Structure:
            {
              "scoreA": Int,
              "scoreB": Int,
              "winner": "A" or "B" or "TIE",
              "verdict": "String",
              "criteriaScores": [
                {
                  "category": "String",
                  "scoreA": Int,
                  "scoreB": Int,
                  "analysis": "String"
                }
              ],
              "prosConsA": {
                "pros": ["String"],
                "cons": ["String"]
              },
              "prosConsB": {
                "pros": ["String"],
                "cons": ["String"]
              }
            }
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = "You are a professional, neutral decision analyst. Your job is to output structured JSON analyses comparing two options specified by the user."))
            )
        )

        val response = RetrofitClient.service.generateContent(apiKey, request)
        val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Received empty response from Gemini API.")

        Log.d(TAG, "Raw text from API: $rawText")

        val cleanedJson = cleanJson(rawText)
        Log.d(TAG, "Cleaned JSON: $cleanedJson")

        try {
            val adapter = RetrofitClient.moshi.adapter(GeminiComparisonResult::class.java)
            val result = adapter.fromJson(cleanedJson) 
                ?: throw IllegalStateException("Failed to parse Gemini comparison response.")

            Comparison(
                itemA = itemA,
                itemB = itemB,
                context = context,
                scoreA = result.scoreA,
                scoreB = result.scoreB,
                winner = result.winner,
                verdict = result.verdict,
                criteriaScores = result.criteriaScores,
                prosConsA = result.prosConsA,
                prosConsB = result.prosConsB
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON response", e)
            throw IllegalStateException("Failed to parse AI comparison result. Please try again.", e)
        }
    }
}
