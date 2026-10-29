package com.example

import android.app.Application
import com.example.automation.ActionDispatcher
import com.example.automation.SuperAgentEngine
import com.example.data.local.NovaDatabase
import com.example.data.network.gemini.GeminiRepository
import com.example.data.preferences.NovaPreferencesRepository
import com.example.speech.SpeechRecognitionManager
import com.example.speech.TextToSpeechManager
import com.example.util.BatteryOptimizationManager

class NovaApplication : Application() {

    lateinit var database: NovaDatabase
        private set

    lateinit var preferencesRepository: NovaPreferencesRepository
        private set

    lateinit var geminiRepository: GeminiRepository
        private set

    lateinit var speechRecognitionManager: SpeechRecognitionManager
        private set

    lateinit var textToSpeechManager: TextToSpeechManager
        private set

    lateinit var actionDispatcher: ActionDispatcher
        private set

    lateinit var batteryOptimizationManager: BatteryOptimizationManager
        private set

    lateinit var superAgentEngine: SuperAgentEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = NovaDatabase.getDatabase(this)
        preferencesRepository = NovaPreferencesRepository(this)
        geminiRepository = GeminiRepository(preferencesRepository)
        speechRecognitionManager = SpeechRecognitionManager(this)
        textToSpeechManager = TextToSpeechManager(this)
        actionDispatcher = ActionDispatcher(this)
        batteryOptimizationManager = BatteryOptimizationManager(this)
        superAgentEngine = SuperAgentEngine(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        textToSpeechManager.shutdown()
        speechRecognitionManager.stopListening()
        batteryOptimizationManager.releaseWakeLockSafely()
    }

    companion object {
        lateinit var instance: NovaApplication
            private set
    }
}
