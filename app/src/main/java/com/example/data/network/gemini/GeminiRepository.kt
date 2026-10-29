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

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
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
     * Returns Result.success with model response or Result.failure with human-readable error.
     */
    suspend fun testConnection(apiKey: String, model: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API Key cannot be empty. Please enter your Google AI Studio API key."))
        }

        val targetModel = if (!model.isNullOrBlank()) {
            model.trim()
        } else {
            preferencesRepository.userPreferencesFlow.first().selectedGeminiModel.ifBlank {
                GeminiModelCatalog.DEFAULT_MODEL_ID
            }
        }

        try {
            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        role = "user",
                        parts = listOf(Part(text = "Hello! Test connection. Respond with: OK"))
                    )
                ),
                generationConfig = GenerationConfig(maxOutputTokens = 16)
            )

            var response = apiService.generateContent(
                model = targetModel,
                apiKey = trimmedKey,
                request = request
            )

            // Resilient fallback if chosen model encounters 404 or 429
            if (!response.isSuccessful && (response.code() == 404 || response.code() == 429)) {
                val fallbackModel = if (targetModel != "gemini-2.5-flash") "gemini-2.5-flash" else "gemini-2.0-flash"
                val fallbackResponse = apiService.generateContent(
                    model = fallbackModel,
                    apiKey = trimmedKey,
                    request = request
                )
                if (fallbackResponse.isSuccessful) {
                    val candidateText = fallbackResponse.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    val reply = candidateText?.trim() ?: "OK"
                    return@withContext Result.success("Connected successfully! ($targetModel active • Verified: $reply)")
                }
            }

            if (response.isSuccessful) {
                val candidateText = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                val reply = candidateText?.trim() ?: "OK"
                Result.success("Connection successful! ($targetModel active • Reply: $reply)")
            } else {
                val errorCode = response.code()
                val errorBody = response.errorBody()?.string() ?: ""
                val errorMessage = when (errorCode) {
                    400 -> "Invalid request for model $targetModel (HTTP 400). Please check your key or model name."
                    403 -> "Permission denied (HTTP 403). Your API key does not have access or is restricted."
                    404 -> "Model endpoint '$targetModel' not found (HTTP 404). Check model availability for your account."
                    429 -> "API quota exceeded for $targetModel (HTTP 429). Rate limit reached."
                    500, 503 -> "Google Gemini server error ($errorCode). Please try again shortly."
                    else -> "API connection failed ($targetModel HTTP $errorCode): $errorBody"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.UnknownHostException) {
            Result.failure(Exception("No internet connection. Please verify your Wi-Fi or cellular data."))
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("Connection timed out. Please check your network and try again."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Unexpected error during connection test."))
        }
    }

    /**
     * Sends user query to Gemini AI with conversational context, system instructions,
     * and dynamic model routing with automatic fallback.
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

        val targetModel = if (!model.isNullOrBlank()) {
            model.trim()
        } else {
            preferencesRepository.userPreferencesFlow.first().selectedGeminiModel.ifBlank {
                GeminiModelCatalog.DEFAULT_MODEL_ID
            }
        }

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

        val request = GenerateContentRequest(
            contents = contentsList,
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
            generationConfig = GenerationConfig(temperature = 0.7f, maxOutputTokens = 800)
        )

        try {
            var response = apiService.generateContent(
                model = targetModel,
                apiKey = apiKey,
                request = request
            )

            // Intelligent resilient fallback if chosen model encounters 404 or 429
            if (!response.isSuccessful && (response.code() == 404 || response.code() == 429)) {
                val fallbackModel = if (targetModel != "gemini-2.5-flash") "gemini-2.5-flash" else "gemini-2.0-flash"
                Log.w("GeminiRepo", "Model $targetModel returned ${response.code()}. Resilient fallback to $fallbackModel")
                val fallbackResponse = apiService.generateContent(
                    model = fallbackModel,
                    apiKey = apiKey,
                    request = request
                )
                if (fallbackResponse.isSuccessful) {
                    response = fallbackResponse
                }
            }

            if (response.isSuccessful) {
                val candidateText = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!candidateText.isNullOrBlank()) {
                    Result.success(candidateText.trim())
                } else {
                    Result.failure(Exception("Gemini returned an empty response."))
                }
            } else {
                val code = response.code()
                val errBody = response.errorBody()?.string() ?: ""
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
            Result.failure(Exception("Request timed out. Please try asking again."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to connect to Gemini AI."))
        }
    }
}
