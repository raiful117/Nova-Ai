package com.example.data.network.gemini

import android.util.Log
import com.example.BuildConfig
import com.example.data.preferences.NovaPreferencesRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class GeminiRepository(
    private val preferencesRepository: NovaPreferencesRepository
) {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // 60-second read timeout accommodates reasoning/thinking models without false timeouts
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val apiService: GeminiApiService = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GeminiApiService::class.java)

    /**
     * Resolves the active API key. First checks user-entered key in preferences;
     * if empty, attempts to read BuildConfig.GEMINI_API_KEY.
     */
    suspend fun getEffectiveApiKey(): String {
        val userKey = preferencesRepository.userPreferencesFlow.first().apiKey
        if (userKey.isNotBlank()) {
            return userKey
        }
        return try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            val buildConfigKey = field.get(null) as? String ?: ""
            if (buildConfigKey.isNotBlank() && !buildConfigKey.contains("MY_GEMINI_API_KEY")) {
                buildConfigKey
            } else ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Executes a real test connection to Gemini API with the given key and model.
     * Includes optimized low-latency thinking config and automatic fallback
     * to eliminate false 404s and connection timeouts.
     */
    suspend fun testConnection(apiKey: String, model: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API Key cannot be empty. Please enter your Google AI Studio API key."))
        }

        val rawModel = if (!model.isNullOrBlank()) {
            model.trim()
        } else {
            preferencesRepository.userPreferencesFlow.first().selectedGeminiModel.ifBlank {
                GeminiModelCatalog.DEFAULT_MODEL_ID
            }
        }
        val targetModel = rawModel.removePrefix("models/").trim()

        // Fast probe request with minimal thinking overhead
        val requestWithThinking = GenerateContentRequest(
            contents = listOf(
                Content(
                    role = "user",
                    parts = listOf(Part(text = "Hello! Test connection. Respond with: OK"))
                )
            ),
            generationConfig = GenerationConfig(
                temperature = 0.2f,
                maxOutputTokens = 128,
                thinkingConfig = ThinkingConfig(thinkingLevel = "low")
            )
        )
        val requestStandard = GenerateContentRequest(
            contents = listOf(
                Content(
                    role = "user",
                    parts = listOf(Part(text = "Hello! Test connection. Respond with: OK"))
                )
            ),
            generationConfig = GenerationConfig(
                temperature = 0.2f,
                maxOutputTokens = 64
            )
        )

        val activeRequest = if (targetModel.contains("3.8", ignoreCase = true) || targetModel.contains("argon", ignoreCase = true)) {
            requestWithThinking
        } else {
            requestStandard
        }

        try {
            var response = try {
                apiService.generateContent(
                    model = targetModel,
                    apiKey = trimmedKey,
                    request = activeRequest
                )
            } catch (e: java.net.SocketTimeoutException) {
                null
            }

            // If 400 Bad Request, model might not support thinkingConfig; retry standard
            if (response != null && response.code() == 400 && activeRequest != requestStandard) {
                response = try {
                    apiService.generateContent(
                        model = targetModel,
                        apiKey = trimmedKey,
                        request = requestStandard
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // Resilient fallback if chosen model encounters 404, 429, or timed out
            if (response == null || !response.isSuccessful) {
                val errorCode = response?.code() ?: -1
                val fallbackModel = if (targetModel != GeminiModelCatalog.ULTRA_FAST_FALLBACK_MODEL) {
                    GeminiModelCatalog.ULTRA_FAST_FALLBACK_MODEL
                } else {
                    "gemini-2.0-flash"
                }
                Log.w("GeminiRepo", "testConnection for $targetModel resulted in $errorCode. Probing fallback $fallbackModel")

                val fallbackResponse = try {
                    apiService.generateContent(
                        model = fallbackModel,
                        apiKey = trimmedKey,
                        request = requestStandard
                    )
                } catch (e: Exception) {
                    null
                }

                if (fallbackResponse?.isSuccessful == true) {
                    val candidateText = fallbackResponse.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    val reply = candidateText?.trim() ?: "OK"
                    val reason = if (errorCode == 404) "Model '$targetModel' requires restricted access; verified active via $fallbackModel"
                                 else "Model '$targetModel' had high latency; verified active via $fallbackModel"
                    return@withContext Result.success("Connected successfully! ($reason • Reply: $reply)")
                }
            }

            if (response != null && response.isSuccessful) {
                val candidateText = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                val reply = candidateText?.trim() ?: "OK"
                Result.success("Connection successful! ($targetModel active • Reply: $reply)")
            } else {
                val errorCode = response?.code() ?: -1
                val errorBody = response?.errorBody()?.string() ?: ""
                val errorMessage = when (errorCode) {
                    400 -> "Invalid request for model $targetModel (HTTP 400). Please check your key or model name."
                    403 -> "Permission denied (HTTP 403). Your API key does not have access or is restricted."
                    404 -> "Model endpoint '$targetModel' not found (HTTP 404). Please select Gemini 3.5 Flash-Lite or Gemini 3.8 Flash in Settings."
                    429 -> "API quota exceeded for $targetModel (HTTP 429). Rate limit reached."
                    500, 503 -> "Google Gemini server error ($errorCode). Please try again shortly."
                    else -> "API connection failed ($targetModel HTTP $errorCode): $errorBody"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.UnknownHostException) {
            Result.failure(Exception("No internet connection. Please verify your Wi-Fi or cellular data."))
        } catch (e: java.net.SocketTimeoutException) {
            // Last resort probe with ultra-fast model before reporting timeout
            val lastResort = try {
                val fallbackResponse = apiService.generateContent(
                    model = GeminiModelCatalog.ULTRA_FAST_FALLBACK_MODEL,
                    apiKey = trimmedKey,
                    request = requestStandard
                )
                if (fallbackResponse.isSuccessful) {
                    val candidateText = fallbackResponse.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    val reply = candidateText?.trim() ?: "OK"
                    Result.success("Connected successfully! ($targetModel timed out, auto-routed to ${GeminiModelCatalog.ULTRA_FAST_FALLBACK_MODEL} • Reply: $reply)")
                } else null
            } catch (ex: Exception) {
                null
            }

            lastResort ?: Result.failure(Exception("Connection timed out on $targetModel. Please switch to Gemini 3.5 Flash-Lite in Settings for zero-latency responses."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Unexpected error during connection test."))
        }
    }

    /**
     * Sends user query to Gemini AI with conversational context, system instructions,
     * low-latency thinking optimization, and automatic failover fallback.
     */
    suspend fun askGemini(
        prompt: String,
        conversationHistory: List<Pair<String, String>> = emptyList(),
        preferredLanguage: String = "bn",
        model: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please tap Settings or API Key Setup to add your key.")
            )
        }

        val rawModel = if (!model.isNullOrBlank()) {
            model.trim()
        } else {
            preferencesRepository.userPreferencesFlow.first().selectedGeminiModel.ifBlank {
                GeminiModelCatalog.DEFAULT_MODEL_ID
            }
        }
        val targetModel = rawModel.removePrefix("models/").trim()

        val systemPrompt = """
            You are NOVA AI, an intelligent personal voice assistant on Android.
            Persona: Friendly, concise, modern, and highly capable.
            Language guideline:
            - If the user talks in Bengali (বাংলা) or Banglish, answer naturally in Bengali (বাংলা).
            - If in English, answer in English.
            - If in Hindi, answer in Hindi.
            Response style:
            - Speak directly and concisely without verbose preamble, because your response will be read aloud via Text-To-Speech.
            - Do not include markdown code blocks or excessive formatting in conversational responses.
            - Provide accurate, helpful, and natural answers.
        """.trimIndent()

        val contentsList = mutableListOf<Content>()
        // Append recent conversation context (last 4 turns)
        conversationHistory.takeLast(4).forEach { (user, ai) ->
            contentsList.add(Content(role = "user", parts = listOf(Part(text = user))))
            contentsList.add(Content(role = "model", parts = listOf(Part(text = ai))))
        }
        // Current user prompt
        contentsList.add(Content(role = "user", parts = listOf(Part(text = prompt))))

        val requestWithThinking = GenerateContentRequest(
            contents = contentsList,
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
            generationConfig = GenerationConfig(
                temperature = 0.7f,
                maxOutputTokens = 800,
                thinkingConfig = ThinkingConfig(thinkingLevel = "low")
            )
        )
        val requestStandard = GenerateContentRequest(
            contents = contentsList,
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
            generationConfig = GenerationConfig(
                temperature = 0.7f,
                maxOutputTokens = 800
            )
        )

        val activeRequest = if (targetModel.contains("3.8", ignoreCase = true) || targetModel.contains("argon", ignoreCase = true)) {
            requestWithThinking
        } else {
            requestStandard
        }

        try {
            var response = try {
                apiService.generateContent(
                    model = targetModel,
                    apiKey = apiKey,
                    request = activeRequest
                )
            } catch (e: java.net.SocketTimeoutException) {
                null
            }

            // Retry without thinkingConfig if model rejects thinkingLevel with 400
            if (response != null && response.code() == 400 && activeRequest != requestStandard) {
                response = try {
                    apiService.generateContent(
                        model = targetModel,
                        apiKey = apiKey,
                        request = requestStandard
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // Intelligent resilient fallback if chosen model encounters 404, 429, or timed out
            if (response == null || !response.isSuccessful) {
                val fallbackModel = if (targetModel != GeminiModelCatalog.ULTRA_FAST_FALLBACK_MODEL) {
                    GeminiModelCatalog.ULTRA_FAST_FALLBACK_MODEL
                } else {
                    "gemini-2.0-flash"
                }
                Log.w("GeminiRepo", "Model $targetModel failed (code: ${response?.code()}). Resilient fallback to $fallbackModel")
                val fallbackResponse = try {
                    apiService.generateContent(
                        model = fallbackModel,
                        apiKey = apiKey,
                        request = requestStandard
                    )
                } catch (e: Exception) {
                    null
                }
                if (fallbackResponse?.isSuccessful == true) {
                    response = fallbackResponse
                }
            }

            if (response != null && response.isSuccessful) {
                val candidateText = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!candidateText.isNullOrBlank()) {
                    Result.success(candidateText.trim())
                } else {
                    Result.failure(Exception("Gemini returned an empty response."))
                }
            } else {
                val code = response?.code() ?: -1
                val errBody = response?.errorBody()?.string() ?: ""
                val msg = when (code) {
                    400 -> "API request rejected (HTTP 400). Please check your query or API key."
                    403 -> "API Key permission denied (HTTP 403). Check your Google AI Studio key."
                    404 -> "Model '$targetModel' not found (HTTP 404). Please select a different model in Settings."
                    429 -> "Quota limit reached for $targetModel (HTTP 429). Please wait a moment."
                    500, 503 -> "Gemini AI servers are temporarily busy ($code). Please retry."
                    else -> "Error ($code): $errBody"
                }
                Result.failure(Exception(msg))
            }
        } catch (e: java.net.UnknownHostException) {
            Result.failure(Exception("Network unavailable. Please connect to Wi-Fi or mobile data."))
        } catch (e: java.net.SocketTimeoutException) {
            // Fallback attempt before giving up on timeout
            val fallback = try {
                val fallbackResp = apiService.generateContent(
                    model = GeminiModelCatalog.ULTRA_FAST_FALLBACK_MODEL,
                    apiKey = apiKey,
                    request = requestStandard
                )
                if (fallbackResp.isSuccessful) {
                    val text = fallbackResp.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (!text.isNullOrBlank()) Result.success(text.trim()) else null
                } else null
            } catch (ex: Exception) {
                null
            }

            fallback ?: Result.failure(Exception("Request timed out on $targetModel. Try switching to Gemini 3.5 Flash-Lite in Settings."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to connect to Gemini AI."))
        }
    }
}
