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
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = 1024,
    @Json(name = "thinkingConfig") val thinkingConfig: ThinkingConfig? = null
)

@JsonClass(generateAdapter = true)
data class ThinkingConfig(
    @Json(name = "thinkingLevel") val thinkingLevel: String? = null // "low", "medium", "high"
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
    const val ULTRA_FAST_FALLBACK_MODEL = "gemini-3.5-flash-lite"

    val AVAILABLE_MODELS = listOf(
        GeminiModelInfo(
            id = "gemini-3.8-flash",
            displayName = "Gemini 3.8 Flash",
            tag = "Flagship • Fast Thinking",
            description = "Google's latest flagship high-intelligence model with optimized low-latency thinking for rapid voice replies.",
            contextWindow = "1M tokens",
            bestFor = "Real-time personal voice assistant & intelligent automation"
        ),
        GeminiModelInfo(
            id = "gemini-3.5-flash-lite",
            displayName = "Gemini 3.5 Flash-Lite",
            tag = "Ultra Fast • Zero Timeout",
            description = "Google's lowest-latency, highly optimized free-tier model. Responds in under 1 second without timing out on any phone.",
            contextWindow = "1M tokens",
            bestFor = "Instant voice replies, weak cellular networks & budget devices"
        ),
        GeminiModelInfo(
            id = "gemini-4-argon",
            displayName = "Gemini 4 Argon",
            tag = "Frontier Reasoning",
            description = "Google's next-generation frontier reasoning architecture for advanced multi-step problem solving.",
            contextWindow = "1M tokens",
            bestFor = "Complex logic, deep multi-step analysis & programming"
        ),
        GeminiModelInfo(
            id = "gemini-2.0-flash",
            displayName = "Gemini 2.0 Flash",
            tag = "High Speed Multimodal",
            description = "High-throughput multimodal engine with instant latency for voice and screen assistance.",
            contextWindow = "1M tokens",
            bestFor = "Quick voice chat & live screen automation"
        ),
        GeminiModelInfo(
            id = "gemini-2.0-flash-lite",
            displayName = "Gemini 2.0 Flash-Lite",
            tag = "Battery & Low RAM",
            description = "Ultra-lean, power-saving model tuned for low memory usage and minimum battery drain.",
            contextWindow = "1M tokens",
            bestFor = "Low memory devices, background monitoring & power efficiency"
        ),
        GeminiModelInfo(
            id = "gemini-2.5-flash",
            displayName = "Gemini 2.5 Flash",
            tag = "Fast Hybrid (Auto-Fallback)",
            description = "High-speed hybrid reasoning with automatic seamless routing to Flash-Lite if restricted.",
            contextWindow = "1M tokens",
            bestFor = "Daily voice queries & quick actions"
        ),
        GeminiModelInfo(
            id = "gemini-2.5-pro",
            displayName = "Gemini 2.5 Pro",
            tag = "Pro Reasoning (Auto-Fallback)",
            description = "Deep thinking model with automatic seamless fallback for complex questions.",
            contextWindow = "2M tokens",
            bestFor = "Deep analysis & comprehensive research"
        ),
        GeminiModelInfo(
            id = "gemini-2.0-pro-exp-02-05",
            displayName = "Gemini 2.0 Pro Experimental",
            tag = "Frontier Logic",
            description = "Cutting-edge experimental reasoning model with state-of-the-art coding and math.",
            contextWindow = "2M tokens",
            bestFor = "Coding, tough math, frontier intelligence"
        )
    )

    fun findModel(id: String): GeminiModelInfo {
        val trimmed = id.trim().removePrefix("models/")
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

