package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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

class OverlayAssistantService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var windowManager: WindowManager? = null

    private var rootOverlayView: View? = null
    private var orbImageView: ImageView? = null
    private var expandedPanel: LinearLayout? = null
    private var responseBubble: TextView? = null
    private var statusLabel: TextView? = null

    private var isExpanded = false
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private val _isOverlayActive = MutableStateFlow(false)
        val isOverlayActive: StateFlow<Boolean> = _isOverlayActive.asStateFlow()

        private val _floatingStatusText = MutableStateFlow("Ready")
        val floatingStatusText: StateFlow<String> = _floatingStatusText.asStateFlow()

        fun canDrawOverlays(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (canDrawOverlays(this)) {
            setupOverlayUi()
            _isOverlayActive.value = true
        } else {
            stopSelf()
        }
    }

    private fun setupOverlayUi() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 220
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
        }

        // Response subtitle bubble (shows above orb when speaking/active)
        responseBubble = TextView(this).apply {
            visibility = View.GONE
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(24, 16, 24, 16)
            maxWidth = 650
            background = GradientDrawable().apply {
                setColor(0xEE161732.toInt())
                cornerRadius = 24f
                setStroke(2, 0xFF00F0FF.toInt())
            }
        }
        container.addView(responseBubble)

        // Horizontal Row with Orb + Controls
        val mainRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Glowing Voice Orb icon
        orbImageView = ImageView(this).apply {
            setImageResource(R.drawable.ic_nova_logo)
            layoutParams = LinearLayout.LayoutParams(140, 140)
            elevation = 16f
            setPadding(10, 10, 10, 10)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xCC080811.toInt())
                setStroke(3, 0xFF9D50FF.toInt())
            }
        }
        mainRow.addView(orbImageView)

        // Expanded Floating Control Panel
        expandedPanel = LinearLayout(this).apply {
            visibility = View.GONE
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 8, 16, 8)
            background = GradientDrawable().apply {
                setColor(0xDD0F1024.toInt())
                cornerRadius = 32f
                setStroke(2, 0xFF2D305E.toInt())
            }
        }

        // 1. Screen Vision Button (Analyze Screen)
        val visionBtn = Button(this).apply {
            text = "👁️ Vision"
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                analyzeActiveScreen()
            }
        }
        expandedPanel?.addView(visionBtn)

        // 2. Mic / Voice command button
        val micBtn = Button(this).apply {
            text = "🎤 Mic"
            textSize = 12f
            setTextColor(0xFF00F0FF.toInt())
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                startVoiceListening()
            }
        }
        expandedPanel?.addView(micBtn)

        // 3. Stop button
        val stopBtn = Button(this).apply {
            text = "⏹ Stop"
            textSize = 12f
            setTextColor(0xFFFF4B55.toInt())
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                stopAssistant()
            }
        }
        expandedPanel?.addView(stopBtn)

        // 4. Open App button
        val openAppBtn = Button(this).apply {
            text = "↗ App"
            textSize = 12f
            setTextColor(0xFFA6A8C7.toInt())
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                val intent = Intent(this@OverlayAssistantService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
            }
        }
        expandedPanel?.addView(openAppBtn)

        mainRow.addView(expandedPanel)
        container.addView(mainRow)

        // Draggable & Click Handling on the Orb
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = false

        orbImageView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 12 || Math.abs(dy) > 12) {
                        isClick = false
                    }
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager?.updateViewLayout(container, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        toggleExpanded()
                    }
                    true
                }
                else -> false
            }
        }

        rootOverlayView = container
        try {
            windowManager?.addView(rootOverlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleExpanded() {
        isExpanded = !isExpanded
        expandedPanel?.visibility = if (isExpanded) View.VISIBLE else View.GONE
    }

    private fun showFloatingResponse(message: String) {
        mainHandler.post {
            responseBubble?.text = message
            responseBubble?.visibility = View.VISIBLE
        }
    }

    fun analyzeActiveScreen() {
        showFloatingResponse("Analyzing current screen vision...")
        serviceScope.launch {
            val app = NovaApplication.instance
            val visionService = NovaAccessibilityService.getActiveService()
            val visionData = visionService?.captureScreenVision()

            val prompt = if (visionData != null && visionData.fullText.isNotBlank()) {
                """
                ${visionData.toPromptContext()}
                
                The user is asking about what is currently on their Android phone screen.
                Explain concisely what app is open, what the user is looking at or doing, and summarize the key visible information in natural Bengali or English.
                """.trimIndent()
            } else {
                "No accessible screen text detected. Please ensure Accessibility Service is enabled in Settings."
            }

            if (visionData != null && visionData.fullText.isNotBlank()) {
                val lang = app.preferencesRepository.userPreferencesFlow.first().language
                val result = app.geminiRepository.askGemini(prompt = prompt, preferredLanguage = lang)
                result.onSuccess { responseText ->
                    showFloatingResponse(responseText)
                    app.textToSpeechManager.speak(
                        text = responseText,
                        languageCode = lang
                    )
                }.onFailure { err ->
                    showFloatingResponse("Vision error: ${err.message}")
                }
            } else {
                showFloatingResponse("Accessibility Service is not active. Enable it in Settings to read screen vision.")
                app.textToSpeechManager.speak("স্ক্রিন দেখার জন্য Settings থেকে Accessibility অন করুন।")
            }
        }
    }

    fun startVoiceListening() {
        showFloatingResponse("Listening... Speak command or ask about screen")
        val app = NovaApplication.instance
        app.speechRecognitionManager.startListening { query ->
            handleSpokenQuery(query)
        }
    }

    private fun handleSpokenQuery(query: String) {
        showFloatingResponse("Heard: \"$query\"")
        serviceScope.launch {
            val app = NovaApplication.instance
            val visionService = NovaAccessibilityService.getActiveService()
            val visionData = visionService?.captureScreenVision()

            val command = CommandParser.parse(query)

            if (command is CommandIntent.ScreenUnderstanding || query.contains("screen", ignoreCase = true) || query.contains("স্ক্রিন")) {
                analyzeActiveScreen()
                return@launch
            }

            if (command is CommandIntent.GeneralAiQuery) {
                // Enrich query with active screen context if available!
                val enrichedPrompt = if (visionData != null && visionData.fullText.isNotBlank()) {
                    """
                    ${visionData.toPromptContext()}
                    
                    User spoken request: $query
                    (If the user's question relates to the screen, use the screen context above. Otherwise, answer conversationally in concise spoken language.)
                    """.trimIndent()
                } else {
                    query
                }

                val lang = app.preferencesRepository.userPreferencesFlow.first().language
                val result = app.geminiRepository.askGemini(prompt = enrichedPrompt, preferredLanguage = lang)
                result.onSuccess { reply ->
                    showFloatingResponse(reply)
                    app.textToSpeechManager.speak(reply, languageCode = lang)
                }.onFailure { ex ->
                    showFloatingResponse("Error: ${ex.message}")
                }
            } else {
                // Execute system command
                when (command) {
                    is CommandIntent.AppLaunch -> {
                        val res = app.actionDispatcher.launchApp(command.appName, command.packageName)
                        showFloatingResponse(res.spokenConfirmation)
                        app.textToSpeechManager.speak(res.spokenConfirmation)
                    }
                    is CommandIntent.SystemSetting -> {
                        val res = app.actionDispatcher.openSystemSetting(command.intentAction, command.title)
                        showFloatingResponse(res.spokenConfirmation)
                        app.textToSpeechManager.speak(res.spokenConfirmation)
                    }
                    is CommandIntent.WebSearch -> {
                        val res = app.actionDispatcher.searchWeb(command.query)
                        showFloatingResponse(res.spokenConfirmation)
                        app.textToSpeechManager.speak(res.spokenConfirmation)
                    }
                    is CommandIntent.YouTubeSearch -> {
                        val res = app.actionDispatcher.searchYouTube(command.query)
                        showFloatingResponse(res.spokenConfirmation)
                        app.textToSpeechManager.speak(res.spokenConfirmation)
                    }
                    is CommandIntent.Calculator -> {
                        val reply = "হিসাবের ফলাফল: ${command.evaluatedResult}"
                        showFloatingResponse(reply)
                        app.textToSpeechManager.speak(reply)
                    }
                    is CommandIntent.StopAll -> {
                        stopAssistant()
                    }
                    else -> {
                        showFloatingResponse("Executing task...")
                    }
                }
            }
        }
    }

    private fun stopAssistant() {
        val app = NovaApplication.instance
        app.textToSpeechManager.stop()
        app.speechRecognitionManager.stopListening()
        mainHandler.post {
            responseBubble?.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _isOverlayActive.value = false
        rootOverlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        rootOverlayView = null
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
