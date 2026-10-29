package com.example.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class TtsVoiceInfo(
    val name: String,
    val locale: String,
    val isNetworkConnectionRequired: Boolean
)

class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<TtsVoiceInfo>>(emptyList())
    val availableVoices: StateFlow<List<TtsVoiceInfo>> = _availableVoices.asStateFlow()

    private val _currentSpokenText = MutableStateFlow("")
    val currentSpokenText: StateFlow<String> = _currentSpokenText.asStateFlow()

    private val _ttsError = MutableStateFlow<String?>(null)
    val ttsError: StateFlow<String?> = _ttsError.asStateFlow()

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("TextToSpeechManager", "Failed to instantiate TTS", e)
            _ttsError.value = "Failed to initialize Text-To-Speech engine."
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            _isInitialized.value = true
            setupProgressListener()
            loadAvailableVoices()
        } else {
            _isInitialized.value = false
            _ttsError.value = "Text-to-speech initialization failed ($status)"
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                _currentSpokenText.value = ""
            }

            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                _currentSpokenText.value = ""
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.value = false
                _currentSpokenText.value = ""
                _ttsError.value = "TTS playback error ($errorCode)"
            }
        })
    }

    private fun loadAvailableVoices() {
        try {
            val voices = tts?.voices ?: emptySet()
            _availableVoices.value = voices.map {
                TtsVoiceInfo(
                    name = it.name,
                    locale = it.locale.displayName,
                    isNetworkConnectionRequired = it.isNetworkConnectionRequired
                )
            }.sortedBy { it.name }
        } catch (e: Exception) {
            Log.e("TTS", "Error querying voices", e)
        }
    }

    fun speak(
        text: String,
        languageCode: String = "bn",
        speechRate: Float = 1.0f,
        speechPitch: Float = 1.0f,
        selectedVoiceName: String = "",
        onDone: (() -> Unit)? = null
    ) {
        if (text.isBlank() || tts == null || !_isInitialized.value) return

        stop() // interrupt previous speech if playing

        val targetLocale = when (languageCode) {
            "bn" -> Locale("bn", "BD")
            "hi" -> Locale("hi", "IN")
            "en" -> Locale.US
            "ar" -> Locale("ar")
            "es" -> Locale("es")
            else -> Locale.getDefault()
        }

        val langResult = tts?.setLanguage(targetLocale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to English or default locale
            tts?.setLanguage(Locale.US)
        }

        tts?.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))
        tts?.setPitch(speechPitch.coerceIn(0.5f, 2.0f))

        // Set specific voice if selected
        if (selectedVoiceName.isNotBlank()) {
            try {
                val voice = tts?.voices?.firstOrNull { it.name == selectedVoiceName }
                if (voice != null) {
                    tts?.voice = voice
                }
            } catch (e: Exception) {
                Log.w("TTS", "Unable to select voice $selectedVoiceName", e)
            }
        }

        _currentSpokenText.value = text
        _isSpeaking.value = true

        val utteranceId = "nova_tts_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e("TTS", "Error stopping TTS", e)
        } finally {
            _isSpeaking.value = false
            _currentSpokenText.value = ""
        }
    }

    fun shutdown() {
        try {
            stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.e("TTS", "Error shutting down TTS", e)
        }
    }
}
