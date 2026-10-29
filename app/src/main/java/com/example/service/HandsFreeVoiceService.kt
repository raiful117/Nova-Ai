package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.NovaApplication
import com.example.R
import com.example.automation.CommandIntent
import com.example.automation.CommandParser
import com.example.automation.SuperAgentState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HandsFreeVoiceService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isLoopActive = false

    companion object {
        const val ACTION_START_HANDS_FREE = "com.example.service.START_HANDS_FREE"
        const val ACTION_STOP_HANDS_FREE = "com.example.service.STOP_HANDS_FREE"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "nova_hands_free_channel"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _lastRecognizedCommand = MutableStateFlow("")
        val lastRecognizedCommand: StateFlow<String> = _lastRecognizedCommand.asStateFlow()

        fun isServiceRunning(): Boolean = _isRunning.value
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_HANDS_FREE) {
            stopLoop()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            _isRunning.value = false
            return START_NOT_STICKY
        }

        val notification = createNotification("Active — Listening continuously with Battery Optimizer & Screen Vision")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        _isRunning.value = true

        // Launch floating overlay assistant if permitted
        if (OverlayAssistantService.canDrawOverlays(this)) {
            val overlayIntent = Intent(this, OverlayAssistantService::class.java)
            startService(overlayIntent)
        }

        startContinuousListeningLoop()

        return START_STICKY
    }

    private fun startContinuousListeningLoop() {
        isLoopActive = true
        scheduleNextListen(500)
    }

    /**
     * Adaptive scheduling based on battery level, charging state, and RAM profile
     */
    private fun scheduleNextListen(defaultDelayMs: Long? = null) {
        if (!isLoopActive) return
        val app = NovaApplication.instance
        val profile = app.batteryOptimizationManager.getDeviceProfile()
        val delayMs = defaultDelayMs ?: profile.recommendedLoopDelayMs

        mainHandler.postDelayed({
            if (!isLoopActive) return@postDelayed
            listenOnce()
        }, delayMs)
    }

    private fun listenOnce() {
        val app = NovaApplication.instance
        if (app.textToSpeechManager.isSpeaking.value) {
            // Avoid hearing our own voice response
            scheduleNextListen(1500)
            return
        }

        serviceScope.launch {
            val lang = app.preferencesRepository.userPreferencesFlow.first().language
            app.superAgentEngine.updateStatus(SuperAgentState.LISTENING, desc = "Always-on listening active...")

            app.speechRecognitionManager.startListening(languageCode = lang) { recognizedText ->
                if (recognizedText.isNotBlank()) {
                    _lastRecognizedCommand.value = recognizedText
                    processSpokenInput(recognizedText)
                } else {
                    scheduleNextListen()
                }
            }
        }
    }

    private fun processSpokenInput(input: String) {
        val app = NovaApplication.instance
        serviceScope.launch {
            updateNotification("Heard: \"$input\" — Autonomous Processing...")

            val engine = app.superAgentEngine

            // 1. Check for emergency stop
            val parsed = CommandParser.parse(input)
            if (parsed is CommandIntent.StopAll) {
                app.textToSpeechManager.stop()
                app.speechRecognitionManager.stopListening()
                engine.updateStatus(SuperAgentState.IDLE, desc = "Stopped by user command")
                updateNotification("Stopped. Listening resumed.")
                scheduleNextListen(2000)
                return@launch
            }

            // 2. Generate Super-Agent Execution Plan (Single or Multi-App Chain)
            val plan = engine.createExecutionPlan(input)

            if (plan.executionSteps.any { it.action != "ai_assist" } || plan.executionSteps.size > 1) {
                // Execute multi-app cross chaining autonomously!
                updateNotification("Executing ${plan.executionSteps.size} automated steps...")
                val executedPlan = engine.executePlan(plan) { status ->
                    updateNotification(status.activeStepDescription)
                }
                app.textToSpeechManager.speak(executedPlan.feedbackMessage)
            } else {
                // Conversational query enriched with real-time screen vision
                val visionService = NovaAccessibilityService.getActiveService()
                val visionData = visionService?.captureScreenVision()

                val prompt = if (visionData != null && visionData.fullText.isNotBlank()) {
                    """
                    ${visionData.toPromptContext()}
                    
                    User query: "$input"
                    Answer concisely for voice output. If the user refers to the screen, use the screen vision context above.
                    """.trimIndent()
                } else {
                    input
                }

                engine.updateStatus(SuperAgentState.THINKING, desc = "Querying Gemini AI...")
                val lang = app.preferencesRepository.userPreferencesFlow.first().language
                val aiResult = app.geminiRepository.askGemini(prompt = prompt, preferredLanguage = lang)
                aiResult.onSuccess { reply ->
                    engine.updateStatus(SuperAgentState.DONE, desc = reply, feedback = reply)
                    updateNotification("NOVA: $reply")
                    app.textToSpeechManager.speak(reply, languageCode = lang)
                }.onFailure { err ->
                    engine.updateStatus(SuperAgentState.ERROR, desc = err.message ?: "AI Error")
                    app.textToSpeechManager.speak("সংযোগ সমস্যা হয়েছে।")
                }
            }

            // Schedule next listen using battery-optimized delay
            scheduleNextListen(2200)
        }
    }

    private fun stopLoop() {
        isLoopActive = false
        mainHandler.removeCallbacksAndMessages(null)
        val app = NovaApplication.instance
        app.speechRecognitionManager.stopListening()
        app.textToSpeechManager.stop()
        app.batteryOptimizationManager.releaseWakeLockSafely()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLoop()
        _isRunning.value = false
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NOVA AI Super-Agent Always-On Voice & Vision",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Autonomous multi-app execution and screen vision daemon"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(statusText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, HandsFreeVoiceService::class.java).apply {
            action = ACTION_STOP_HANDS_FREE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("NOVA AI — OS Super-Agent")
            .setContentText(statusText)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop (বন্ধ করো)", stopPendingIntent)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val notification = createNotification(statusText)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }
}
