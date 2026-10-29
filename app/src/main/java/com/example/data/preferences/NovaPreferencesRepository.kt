package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nova_preferences")

data class UserPreferences(
    val apiKey: String = "",
    val language: String = "bn", // "bn", "en", "hi"
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val selectedVoiceName: String = "",
    val wakeWordEnabled: Boolean = false,
    val wakeWordPhrase: String = "Hey Nova",
    val handsFreeEnabled: Boolean = false,
    val overlayEnabled: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val speechPlaybackEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false
)

class NovaPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val API_KEY = stringPreferencesKey("gemini_api_key")
        val LANGUAGE = stringPreferencesKey("app_language")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val SELECTED_VOICE = stringPreferencesKey("selected_voice_name")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val WAKE_WORD_PHRASE = stringPreferencesKey("wake_word_phrase")
        val HANDS_FREE_ENABLED = booleanPreferencesKey("hands_free_enabled")
        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SPEECH_PLAYBACK_ENABLED = booleanPreferencesKey("speech_playback_enabled")
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            apiKey = preferences[PreferencesKeys.API_KEY] ?: "",
            language = preferences[PreferencesKeys.LANGUAGE] ?: "bn",
            speechRate = preferences[PreferencesKeys.SPEECH_RATE] ?: 1.0f,
            speechPitch = preferences[PreferencesKeys.SPEECH_PITCH] ?: 1.0f,
            selectedVoiceName = preferences[PreferencesKeys.SELECTED_VOICE] ?: "",
            wakeWordEnabled = preferences[PreferencesKeys.WAKE_WORD_ENABLED] ?: false,
            wakeWordPhrase = preferences[PreferencesKeys.WAKE_WORD_PHRASE] ?: "Hey Nova",
            handsFreeEnabled = preferences[PreferencesKeys.HANDS_FREE_ENABLED] ?: false,
            overlayEnabled = preferences[PreferencesKeys.OVERLAY_ENABLED] ?: false,
            onboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
            speechPlaybackEnabled = preferences[PreferencesKeys.SPEECH_PLAYBACK_ENABLED] ?: true,
            quietHoursEnabled = preferences[PreferencesKeys.QUIET_HOURS_ENABLED] ?: false
        )
    }

    suspend fun updateApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.API_KEY] = apiKey.trim()
        }
    }

    suspend fun clearApiKey() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.API_KEY)
        }
    }

    suspend fun updateLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language
        }
    }

    suspend fun updateSpeechRate(rate: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SPEECH_RATE] = rate
        }
    }

    suspend fun updateSelectedVoice(voiceName: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_VOICE] = voiceName
        }
    }

    suspend fun updateWakeWordEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WAKE_WORD_ENABLED] = enabled
        }
    }

    suspend fun updateWakeWordPhrase(phrase: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WAKE_WORD_PHRASE] = phrase
        }
    }

    suspend fun updateHandsFreeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HANDS_FREE_ENABLED] = enabled
        }
    }

    suspend fun updateOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OVERLAY_ENABLED] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun updateSpeechPlaybackEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SPEECH_PLAYBACK_ENABLED] = enabled
        }
    }

    suspend fun updateQuietHoursEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.QUIET_HOURS_ENABLED] = enabled
        }
    }

    suspend fun resetAllPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
