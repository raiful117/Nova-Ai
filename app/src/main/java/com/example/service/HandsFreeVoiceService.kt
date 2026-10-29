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
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.NovaApplication
import com.example.R
import com.example.automation.CommandIntent
import com.example.automation.CommandParser
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

        val notification = createNotification("Active — Listening continuously & observing screen")
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

    private fun scheduleNextListen(delayMs: Long) {
        if (!isLoopActive) return
        mainHandler.postDelayed({
            if (!isLoopActive) return@postDelayed
            listenOnce()
        }, delayMs)
    }

    private fun listenOnce() {
        val app = NovaApplication.instance
        if (app.textToSpeechManager.isSpeaking.value) {
            // Wait for TTS to finish speaking before listening again to avoid hearing ourselves
            scheduleNextListen(1200)
            return
        }

        serviceScope.launch {
            val lang = app.preferencesRepository.userPreferencesFlow.first().language
            app.speechRecognitionManager.startListening(languageCode = lang) { recognizedText ->
                if (recognizedText.isNotBlank()) {
                    _lastRecognizedCommand.value = recognizedText
                    processSpokenInput(recognizedText)
                } else {
                    scheduleNextListen(800)
                }
            }
        }
    }

    private fun processSpokenInput(input: String) {
        val app = NovaApplication.instance
        serviceScope.launch {
            // Update notification
            updateNotification("Heard: \"$input\" — Processing...")

            // Real-time Screen Vision: capture what user is currently seeing on screen
            val visionService = NovaAccessibilityService.getActiveService()
            val visionData = visionService?.captureScreenVision()

            val command = CommandParser.parse(input)

            if (command is CommandIntent.StopAll) {
                app.textToSpeechManager.stop()
                app.speechRecognitionManager.stopListening()
                updateNotification("Stopped. Listening again...")
                scheduleNextListen(1500)
                return@launch
            }

            if (command is CommandIntent.ScreenUnderstanding || input.contains("স্ক্রিন", ignoreCase = true) || input.contains("screen", ignoreCase = true)) {
                // User asked about the screen
                val prompt = if (visionData != null && visionData.fullText.isNotBlank()) {
                    """
                    ${visionData.toPromptContext()}
                    
                    The user asks: "$input"
                    Explain clearly what is on the screen, what app the user is using, and answer their query in natural Bengali or English.
                    """.trimIndent()
                } else {
                    "স্ক্রিনের তথ্য পড়ার জন্য Settings থেকে Accessibility Service চালু করুন।"
                }

                if (visionData != null && visionData.fullText.isNotBlank()) {
                    val lang = app.preferencesRepository.userPreferencesFlow.first().language
                    val aiResult = app.geminiRepository.askGemini(prompt = prompt, preferredLanguage = lang)
                    aiResult.onSuccess { reply ->
                        updateNotification("NOVA: $reply")
                        app.textToSpeechManager.speak(reply, languageCode = lang)
                    }.onFailure { err ->
                        app.textToSpeechManager.speak("সমস্যা হয়েছে: ${err.message}")
                    }
                } else {
                    app.textToSpeechManager.speak("স্ক্রিন দেখার জন্য Accessibility চালু করুন।")
                }
            } else if (command is CommandIntent.GeneralAiQuery) {
                // Enrich query with active screen context so Gemini always knows what the user is doing!
                val enrichedPrompt = if (visionData != null && visionData.fullText.isNotBlank()) {
                    """
                    ${visionData.toPromptContext()}
                    
                    User query: "$input"
                    If the user refers to something on screen, use the screen vision context above. Answer concisely for voice output.
                    """.trimIndent()
                } else {
                    input
                }

                val lang = app.preferencesRepository.userPreferencesFlow.first().language
                val aiResult = app.geminiRepository.askGemini(prompt = enrichedPrompt, preferredLanguage = lang)
                aiResult.onSuccess { reply ->
                    updateNotification("NOVA: $reply")
                    app.textToSpeechManager.speak(reply, languageCode = lang)
                }.onFailure { err ->
                    app.textToSpeechManager.speak("দুঃখিত, সংযোগে সমস্যা হয়েছে।")
                }
            } else {
                // Deterministic Phone Automation Action
                when (command) {
                    is CommandIntent.AppLaunch -> {
                        val res = app.actionDispatcher.launchApp(command.appName, command.packageName)
                        app.textToSpeechManager.speak(res.spokenConfirmation)
                    }
                    is CommandIntent.SystemSetting -> {
                        val res = app.actionDispatcher.openSystemSetting(command.intentAction, command.title)
                        app.textToSpeechManager.speak(res.spokenConfirmation)
                    }
                    is CommandIntent.WebSearch -> {
                        val res = app.actionDispatcher.searchWeb(command.query)
                        app.textToSpeechManager.speak(res.spokenConfirmation)
                    }
                    is CommandIntent.YouTubeSearch -> {
                        val res = app.actionDispatcher.searchYouTube(command.query)
                        app.textToSpeechManager.speak(res.spokenConfirmation)
                    }
                    is CommandIntent.Calculator -> {
                        val res = "হিসাব: ${command.evaluatedResult}"
                        app.textToSpeechManager.speak(res)
                    }
                    is CommandIntent.Reminder -> {
                        val res = app.actionDispatcher.scheduleReminder(command.title, command.delayMinutes)
                        app.textToSpeechManager.speak(res.spokenConfirmation)
                    }
                    is CommandIntent.SaveMemory -> {
                        val res = app.actionDispatcher.saveMemory(command.key, command.content, command.category)
                        app.textToSpeechManager.speak(res.spokenConfirmation)
                    }
                    is CommandIntent.DeviceInfo -> {
                        val res = app.actionDispatcher.getDeviceInfo(command.infoType)
                        app.textToSpeechManager.speak(res.spokenConfirmation)
                    }
                    is CommandIntent.Dialer -> {
                        val res = app.actionDispatcher.openDialer(command.phoneNumber, command.contactName)
                        app.textToSpeechManager.speak(res.spokenConfirmation)
                    }
                    is CommandIntent.SmsDraft -> {
                        val res = app.actionDispatcher.composeSms(command.recipient, command.message)
                        app.textToSpeechManager.speak(res.spokenConfirmation)
                    }
                    is CommandIntent.EmailDraft -> {
                        val res = app.actionDispatcher.composeEmail(command.recipient, command.subject, command.body)
                        app.textToSpeechManager.speak(res.spokenConfirmation)
                    }
                    else -> {}
                }
            }

            // Once spoken response is dispatched, schedule next listen
            scheduleNextListen(2000)
        }
    }

    private fun stopLoop() {
        isLoopActive = false
        mainHandler.removeCallbacksAndMessages(null)
        val app = NovaApplication.instance
        app.speechRecognitionManager.stopListening()
        app.textToSpeechManager.stop()
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
                "NOVA AI Always-On Voice & Vision Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Always-on voice recognition and real-time screen vision"
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
            .setContentTitle("NOVA AI — Always-On Voice & Vision")
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
