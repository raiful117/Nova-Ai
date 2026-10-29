package com.example.data.network.gemini

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "role") val role: String? = null, // "user" or "model" or "system"
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Float? = 0.7f,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = 1024
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null,
    @Json(name = "error") val error: GeminiError? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null,
    @Json(name = "finishReason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiError(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "status") val status: String? = null
)

data class GeminiModelInfo(
    val id: String,
    val displayName: String,
    val tag: String,
    val description: String,
    val contextWindow: String,
    val bestFor: String
)

object GeminiModelCatalog {
    const val DEFAULT_MODEL_ID = "gemini-3.8-flash"

    val AVAILABLE_MODELS = listOf(
        GeminiModelInfo(
            id = "gemini-3.8-flash",
            displayName = "Gemini 3.8 Flash",
            tag = "Newest Flagship • Free Tier",
            description = "Google's latest next-generation ultra-fast reasoning model with cutting-edge real-time conversation speed.",
            contextWindow = "1M tokens",
            bestFor = "Primary voice assistant, autonomous device actions, real-time replies"
        ),
        GeminiModelInfo(
            id = "gemini-2.5-flash",
            displayName = "Gemini 2.5 Flash",
            tag = "Fast & Free Tier",
            description = "High-speed hybrid reasoning with ultra-low latency and multimodal intelligence.",
            contextWindow = "1M tokens",
            bestFor = "Daily voice queries, quick screen analysis, fast answers"
        ),
        GeminiModelInfo(
            id = "gemini-2.5-pro",
            displayName = "Gemini 2.5 Pro",
            tag = "Pro Reasoning • Free Tier",
            description = "Google's premier reasoning model for complex thinking, logic, and multi-step tasks.",
            contextWindow = "2M tokens",
            bestFor = "Complex multi-step automation chains, deep research, problem solving"
        ),
        GeminiModelInfo(
            id = "gemini-2.0-flash",
            displayName = "Gemini 2.0 Flash",
            tag = "Ultra Fast • Free Tier",
            description = "Next-generation multimodal model engineered for instant interactive latency.",
            contextWindow = "1M tokens",
            bestFor = "Instant voice interactions, live UI monitoring"
        ),
        GeminiModelInfo(
            id = "gemini-2.0-flash-lite",
            displayName = "Gemini 2.0 Flash-Lite",
            tag = "Battery & Low RAM • Free Tier",
            description = "Ultra-lean, cost-effective model optimized for low-end phones and zero battery drain.",
            contextWindow = "1M tokens",
            bestFor = "Budget Android devices, low battery usage, weak networks"
        ),
        GeminiModelInfo(
            id = "gemini-2.0-pro-exp-02-05",
            displayName = "Gemini 2.0 Pro Experimental",
            tag = "Frontier Logic • Free Tier",
            description = "Cutting-edge experimental reasoning model with state-of-the-art coding and math.",
            contextWindow = "2M tokens",
            bestFor = "Coding, tough math, frontier intelligence"
        )
    )

    fun findModel(id: String): GeminiModelInfo {
        val trimmed = id.trim()
        return AVAILABLE_MODELS.firstOrNull { it.id.equals(trimmed, ignoreCase = true) }
            ?: GeminiModelInfo(
                id = trimmed,
                displayName = if (trimmed.isNotBlank()) trimmed else DEFAULT_MODEL_ID,
                tag = "Custom",
                description = "User-specified custom Gemini model identifier.",
                contextWindow = "Dynamic",
                bestFor = "Custom Gemini model deployment"
            )
    }
}

