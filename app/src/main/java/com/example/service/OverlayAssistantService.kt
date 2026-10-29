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
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity
import com.example.NovaApplication
import com.example.R
import com.example.automation.SuperAgentState
import com.example.automation.SuperAgentStatus
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
    private var statusBadge: TextView? = null
    private var batteryBadge: TextView? = null
    private var expandedPanel: LinearLayout? = null
    private var responseBubble: TextView? = null

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
            observeSuperAgentStatus()
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
            x = 24
            y = 200
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
        }

        // 1. Hardware & Battery Saver Pill
        batteryBadge = TextView(this).apply {
            visibility = View.VISIBLE
            setTextColor(0xFF00F0FF.toInt())
            textSize = 10f
            setPadding(16, 6, 16, 6)
            background = GradientDrawable().apply {
                setColor(0xEE0B0C1E.toInt())
                cornerRadius = 16f
                setStroke(1, 0xFF282B5A.toInt())
            }
        }
        updateHardwareBadge()
        container.addView(batteryBadge)

        // 2. Real-Time Super-Agent Status Transition Pill: 🔵 Listening ➔ 🟡 Thinking ➔ 🟢 Executing ➔ ✨ Done
        statusBadge = TextView(this).apply {
            text = "🔵 Ready"
            setTextColor(Color.WHITE)
            textSize = 11f
            setPadding(20, 8, 20, 8)
            background = GradientDrawable().apply {
                setColor(0xDD12132A.toInt())
                cornerRadius = 20f
                setStroke(2, 0xFF9D50FF.toInt())
            }
        }
        val statusParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 6, 0, 6)
        }
        container.addView(statusBadge, statusParams)

        // 3. Response / Subtitle Bubble (Shows query, speech output, or multi-step execution details)
        responseBubble = TextView(this).apply {
            visibility = View.GONE
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(24, 16, 24, 16)
            maxWidth = 650
            background = GradientDrawable().apply {
                setColor(0xF0101228.toInt())
                cornerRadius = 24f
                setStroke(2, 0xFF00F0FF.toInt())
            }
        }
        container.addView(responseBubble)

        // 4. Main Interactive Row: Glowing Orb + Quick Action Dock
        val mainRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Glowing Voice Orb icon
        orbImageView = ImageView(this).apply {
            setImageResource(R.drawable.ic_nova_logo)
            layoutParams = LinearLayout.LayoutParams(136, 136)
            elevation = 16f
            setPadding(10, 10, 10, 10)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xEE080811.toInt())
                setStroke(3, 0xFF9D50FF.toInt())
            }
        }
        mainRow.addView(orbImageView)

        // Expanded Action Dock
        expandedPanel = LinearLayout(this).apply {
            visibility = View.GONE
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 6, 12, 6)
            background = GradientDrawable().apply {
                setColor(0xEE0F1024.toInt())
                cornerRadius = 30f
                setStroke(2, 0xFF2D305E.toInt())
            }
        }

        // 1. Screen Vision Button
        val visionBtn = Button(this).apply {
            text = "👁️ Vision"
            textSize = 11f
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
            textSize = 11f
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
            textSize = 11f
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
            textSize = 11f
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

        // Draggable & Click Handling
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

    private fun observeSuperAgentStatus() {
        serviceScope.launch {
            val engine = NovaApplication.instance.superAgentEngine
            engine.statusFlow.collect { status ->
                updateStatusTransition(status)
            }
        }
    }

    private fun updateStatusTransition(status: SuperAgentStatus) {
        mainHandler.post {
            val (badgeText, strokeColor) = when (status.state) {
                SuperAgentState.IDLE -> Pair("Ready", 0xFF9D50FF.toInt())
                SuperAgentState.LISTENING -> Pair("🔵 Listening...", 0xFF00F0FF.toInt())
                SuperAgentState.THINKING -> Pair("🟡 Thinking & Planning...", 0xFFFFB800.toInt())
                SuperAgentState.EXECUTING -> {
                    val stepStr = if (status.totalSteps > 0) " (${status.currentStepNumber}/${status.totalSteps})" else ""
                    Pair("🟢 Executing Step$stepStr", 0xFF00E676.toInt())
                }
                SuperAgentState.DONE -> Pair("✨ Done", 0xFF9D50FF.toInt())
                SuperAgentState.ERROR -> Pair("⚠️ Issue", 0xFFFF4B55.toInt())
            }

            statusBadge?.text = badgeText
            (statusBadge?.background as? GradientDrawable)?.setStroke(2, strokeColor)
            (orbImageView?.background as? GradientDrawable)?.setStroke(3, strokeColor)

            if (status.activeStepDescription.isNotBlank() || status.feedbackMessage.isNotBlank()) {
                val detail = status.activeStepDescription.ifBlank { status.feedbackMessage }
                responseBubble?.text = detail
                responseBubble?.visibility = View.VISIBLE
            }

            updateHardwareBadge()
        }
    }

    private fun updateHardwareBadge() {
        val app = NovaApplication.instance
        val profile = app.batteryOptimizationManager.getDeviceProfile()
        val pwr = if (profile.isPowerSaveMode) "⚡Saver" else "Normal"
        batteryBadge?.text = "🔋${profile.batteryLevel}% | RAM: ${profile.availableRamMb}MB Free | $pwr"
    }

    private fun toggleExpanded() {
        isExpanded = !isExpanded
        expandedPanel?.visibility = if (isExpanded) View.VISIBLE else View.GONE
        updateHardwareBadge()
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

            if (visionData != null && visionData.fullText.isNotBlank()) {
                val prompt = """
                    ${visionData.toPromptContext()}
                    
                    The user is asking about what is currently on their Android phone screen.
                    Explain concisely what app is open, what the user is looking at or doing, and summarize the key visible information in natural Bengali or English.
                """.trimIndent()

                val lang = app.preferencesRepository.userPreferencesFlow.first().language
                val result = app.geminiRepository.askGemini(prompt = prompt, preferredLanguage = lang)
                result.onSuccess { responseText ->
                    showFloatingResponse(responseText)
                    app.textToSpeechManager.speak(text = responseText, languageCode = lang)
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
        val app = NovaApplication.instance
        app.superAgentEngine.updateStatus(SuperAgentState.LISTENING, desc = "Listening for your commands...")
        app.speechRecognitionManager.startListening { query ->
            handleSpokenQuery(query)
        }
    }

    private fun handleSpokenQuery(query: String) {
        serviceScope.launch {
            val app = NovaApplication.instance
            val engine = app.superAgentEngine

            // 1. Generate Structured Execution Plan
            val plan = engine.createExecutionPlan(query)

            // If it's a multi-app chain or hardware action, execute autonomously!
            if (plan.executionSteps.any { it.action != "ai_assist" } || plan.executionSteps.size > 1) {
                showFloatingResponse("Executing multi-app chain...")
                engine.executePlan(plan)
                app.textToSpeechManager.speak(plan.feedbackMessage)
            } else {
                // Conversational or single query
                val visionService = NovaAccessibilityService.getActiveService()
                val visionData = visionService?.captureScreenVision()

                val prompt = if (visionData != null && visionData.fullText.isNotBlank()) {
                    """
                    ${visionData.toPromptContext()}
                    
                    User query: "$query"
                    Answer concisely for voice output.
                    """.trimIndent()
                } else {
                    query
                }

                engine.updateStatus(SuperAgentState.THINKING, desc = "Asking Gemini AI...")
                val lang = app.preferencesRepository.userPreferencesFlow.first().language
                val result = app.geminiRepository.askGemini(prompt = prompt, preferredLanguage = lang)
                result.onSuccess { reply ->
                    engine.updateStatus(SuperAgentState.DONE, desc = reply, feedback = reply)
                    showFloatingResponse(reply)
                    app.textToSpeechManager.speak(reply, languageCode = lang)
                }.onFailure { ex ->
                    engine.updateStatus(SuperAgentState.ERROR, desc = ex.message ?: "AI Error")
                    showFloatingResponse("Error: ${ex.message}")
                }
            }
        }
    }

    private fun stopAssistant() {
        val app = NovaApplication.instance
        app.textToSpeechManager.stop()
        app.speechRecognitionManager.stopListening()
        app.superAgentEngine.updateStatus(SuperAgentState.IDLE, desc = "Stopped")
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
