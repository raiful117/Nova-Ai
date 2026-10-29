package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NovaApplication
import com.example.automation.ActionResult
import com.example.automation.CommandIntent
import com.example.automation.CommandParser
import com.example.automation.ExecutionPlan
import com.example.automation.SuperAgentEngine
import com.example.automation.SuperAgentState
import com.example.automation.SuperAgentStatus
import com.example.data.local.ConversationEntity
import com.example.data.local.MemoryEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.TaskExecutionEntity
import com.example.data.preferences.UserPreferences
import com.example.service.NovaAccessibilityService
import com.example.speech.TtsVoiceInfo
import com.example.util.DeviceHardwareProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class OrbState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

class NovaMainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NovaApplication
    private val database = app.database
    private val preferencesRepo = app.preferencesRepository
    private val geminiRepo = app.geminiRepository
    val speechManager = app.speechRecognitionManager
    val ttsManager = app.textToSpeechManager
    val actionDispatcher = app.actionDispatcher
    val superAgentEngine: SuperAgentEngine = app.superAgentEngine
    val batteryOptManager = app.batteryOptimizationManager

    val userPreferences: StateFlow<UserPreferences> = preferencesRepo.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    // Voice & Orb State
    private val _orbState = MutableStateFlow(OrbState.IDLE)
    val orbState: StateFlow<OrbState> = _orbState.asStateFlow()

    private val _statusText = MutableStateFlow("Ready to listen")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _currentPrompt = MutableStateFlow("")
    val currentPrompt: StateFlow<String> = _currentPrompt.asStateFlow()

    private val _latestResponse = MutableStateFlow("")
    val latestResponse: StateFlow<String> = _latestResponse.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Super-Agent state and structured JSON execution plan
    val superAgentStatus: StateFlow<SuperAgentStatus> = superAgentEngine.statusFlow

    private val _latestExecutionPlanJson = MutableStateFlow("")
    val latestExecutionPlanJson: StateFlow<String> = _latestExecutionPlanJson.asStateFlow()

    // API Test Connection State
    private val _apiTestStatus = MutableStateFlow<String?>(null)
    val apiTestStatus: StateFlow<String?> = _apiTestStatus.asStateFlow()

    private val _isTestingApi = MutableStateFlow(false)
    val isTestingApi: StateFlow<Boolean> = _isTestingApi.asStateFlow()

    // Active session
    private var currentSessionId = UUID.randomUUID().toString()
    private var activeJob: Job? = null

    // Room database flows
    val conversationsFlow: StateFlow<List<ConversationEntity>> = database.conversationDao()
        .getAllConversations()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val memoriesFlow: StateFlow<List<MemoryEntity>> = database.memoryDao()
        .getAllMemories()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val remindersFlow: StateFlow<List<ReminderEntity>> = database.reminderDao()
        .getActiveReminders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val tasksFlow: StateFlow<List<TaskExecutionEntity>> = database.taskDao()
        .getAllTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val availableVoices: StateFlow<List<TtsVoiceInfo>> = ttsManager.availableVoices

    init {
        // Observe TTS speaking state
        viewModelScope.launch {
            ttsManager.isSpeaking.collect { isSpeaking ->
                if (isSpeaking) {
                    _orbState.value = OrbState.SPEAKING
                    _statusText.value = "NOVA is speaking..."
                } else if (_orbState.value == OrbState.SPEAKING) {
                    _orbState.value = OrbState.IDLE
                    _statusText.value = "Ready to listen"
                }
            }
        }

        // Observe SuperAgent transitions and reflect into OrbState
        viewModelScope.launch {
            superAgentEngine.statusFlow.collect { status ->
                when (status.state) {
                    SuperAgentState.IDLE -> {
                        if (!ttsManager.isSpeaking.value) {
                            _orbState.value = OrbState.IDLE
                            _statusText.value = "Ready to listen"
                        }
                    }
                    SuperAgentState.LISTENING -> {
                        _orbState.value = OrbState.LISTENING
                        _statusText.value = "Listening..."
                    }
                    SuperAgentState.THINKING -> {
                        _orbState.value = OrbState.PROCESSING
                        _statusText.value = "Planning execution steps..."
                    }
                    SuperAgentState.EXECUTING -> {
                        _orbState.value = OrbState.PROCESSING
                        _statusText.value = status.activeStepDescription
                    }
                    SuperAgentState.DONE -> {
                        _orbState.value = OrbState.IDLE
                        _statusText.value = "Execution complete"
                    }
                    SuperAgentState.ERROR -> {
                        _orbState.value = OrbState.ERROR
                        _statusText.value = "Execution error"
                    }
                }
                if (status.executionPlanJson.isNotBlank()) {
                    _latestExecutionPlanJson.value = status.executionPlanJson
                }
            }
        }
    }

    fun getHardwareProfile(): DeviceHardwareProfile {
        return batteryOptManager.getDeviceProfile()
    }

    fun startListening() {
        stopAll() // cancel any existing TTS or active jobs
        _errorMessage.value = null
        _orbState.value = OrbState.LISTENING
        _statusText.value = "Listening... Speak now"

        val lang = userPreferences.value.language
        speechManager.startListening(languageCode = lang) { recognizedText ->
            if (recognizedText.isNotBlank()) {
                handleUserInput(recognizedText)
            } else {
                _orbState.value = OrbState.IDLE
                _statusText.value = "Ready to listen"
            }
        }
    }

    fun stopListening() {
        speechManager.stopListening()
        if (_orbState.value == OrbState.LISTENING) {
            _orbState.value = OrbState.IDLE
            _statusText.value = "Ready to listen"
        }
    }

    fun stopAll() {
        activeJob?.cancel()
        activeJob = null
        speechManager.stopListening()
        ttsManager.stop()
        superAgentEngine.updateStatus(SuperAgentState.IDLE, desc = "Stopped")
        _orbState.value = OrbState.IDLE
        _statusText.value = "Ready to listen"
    }

    fun handleUserInput(input: String) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return

        _currentPrompt.value = trimmed
        _errorMessage.value = null

        // Check if this is a compound multi-step command
        val plan = superAgentEngine.createExecutionPlan(trimmed)
        _latestExecutionPlanJson.value = plan.toJsonString()

        if (plan.executionSteps.size > 1 || plan.executionSteps.any { it.action != "ai_assist" }) {
            executeSuperAgentChain(plan)
            return
        }

        // Single command intent
        val command = CommandParser.parse(trimmed)
        executeCommand(command, trimmed)
    }

    fun executeSuperAgentChain(plan: ExecutionPlan) {
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            val executedPlan = superAgentEngine.executePlan(plan)
            _latestExecutionPlanJson.value = executedPlan.toJsonString()
            _latestResponse.value = executedPlan.feedbackMessage

            database.taskDao().insert(
                TaskExecutionEntity(
                    query = plan.userCommand,
                    intentType = plan.taskCategory,
                    targetApp = "Multi-App System",
                    stepsSummary = executedPlan.toJsonString(),
                    status = "COMPLETED",
                    resultMessage = executedPlan.feedbackMessage
                )
            )
            saveConversation(plan.userCommand, executedPlan.feedbackMessage)
            speakResponse(executedPlan.feedbackMessage)
        }
    }

    private fun executeCommand(command: CommandIntent, originalInput: String) {
        when (command) {
            is CommandIntent.StopAll -> {
                stopAll()
                speakResponse("সব কাজ থামানো হয়েছে।")
            }

            is CommandIntent.AppLaunch -> {
                _orbState.value = OrbState.PROCESSING
                _statusText.value = "Launching ${command.appName}..."
                val result = actionDispatcher.launchApp(command.appName, command.packageName)
                handleActionResult(command.appName, "APP_LAUNCH", result, originalInput)
            }

            is CommandIntent.SystemSetting -> {
                _orbState.value = OrbState.PROCESSING
                _statusText.value = "Opening ${command.title}..."
                val result = actionDispatcher.openSystemSetting(command.intentAction, command.title)
                handleActionResult(command.title, "SYSTEM_SETTING", result, originalInput)
            }

            is CommandIntent.Dialer -> {
                _orbState.value = OrbState.PROCESSING
                val result = actionDispatcher.openDialer(command.phoneNumber, command.contactName)
                handleActionResult("Phone Dialer", "DIALER", result, originalInput)
            }

            is CommandIntent.SmsDraft -> {
                _orbState.value = OrbState.PROCESSING
                val result = actionDispatcher.composeSms(command.recipient, command.message)
                handleActionResult("SMS Compose", "SMS", result, originalInput)
            }

            is CommandIntent.EmailDraft -> {
                _orbState.value = OrbState.PROCESSING
                val result = actionDispatcher.composeEmail(command.recipient, command.subject, command.body)
                handleActionResult("Email Draft", "EMAIL", result, originalInput)
            }

            is CommandIntent.WebSearch -> {
                _orbState.value = OrbState.PROCESSING
                val result = actionDispatcher.searchWeb(command.query)
                handleActionResult("Web Search", "WEB_SEARCH", result, originalInput)
            }

            is CommandIntent.YouTubeSearch -> {
                _orbState.value = OrbState.PROCESSING
                val result = actionDispatcher.searchYouTube(command.query)
                handleActionResult("YouTube Search", "YOUTUBE_SEARCH", result, originalInput)
            }

            is CommandIntent.Calculator -> {
                _orbState.value = OrbState.PROCESSING
                val spoken = "হিসাবের ফলাফল হলো: ${command.evaluatedResult}"
                _latestResponse.value = "Calculation result: ${command.evaluatedResult}"
                saveConversation(originalInput, _latestResponse.value)
                speakResponse(spoken)
            }

            is CommandIntent.Reminder -> {
                _orbState.value = OrbState.PROCESSING
                viewModelScope.launch {
                    val result = actionDispatcher.scheduleReminder(command.title, command.delayMinutes)
                    handleActionResult("Reminder", "REMINDER", result, originalInput)
                }
            }

            is CommandIntent.SaveMemory -> {
                _orbState.value = OrbState.PROCESSING
                viewModelScope.launch {
                    val result = actionDispatcher.saveMemory(command.key, command.content, command.category)
                    handleActionResult("Memory", "SAVE_MEMORY", result, originalInput)
                }
            }

            is CommandIntent.DeviceInfo -> {
                _orbState.value = OrbState.PROCESSING
                val result = actionDispatcher.getDeviceInfo(command.infoType)
                handleActionResult("Device Info", "DEVICE_INFO", result, originalInput)
            }

            is CommandIntent.ScreenUnderstanding -> {
                handleScreenUnderstanding(command.query)
            }

            is CommandIntent.GeneralAiQuery -> {
                queryGeminiAi(command.prompt)
            }
        }
    }

    private fun handleActionResult(
        target: String,
        type: String,
        result: ActionResult,
        originalInput: String
    ) {
        _latestResponse.value = result.displayDetails
        viewModelScope.launch {
            database.taskDao().insert(
                TaskExecutionEntity(
                    query = originalInput,
                    intentType = type,
                    targetApp = target,
                    stepsSummary = result.displayDetails,
                    status = if (result.isSuccess) "COMPLETED" else "FAILED",
                    resultMessage = result.spokenConfirmation
                )
            )
            saveConversation(originalInput, result.displayDetails)
        }
        speakResponse(result.spokenConfirmation)
    }

    private fun handleScreenUnderstanding(userQuery: String) {
        val visionService = NovaAccessibilityService.getActiveService()
        val vision = visionService?.captureScreenVision()
        if (vision == null || vision.fullText.isBlank()) {
            val msg = "বর্তমানে স্ক্রিনে দেখার জন্য Settings থেকে Accessibility সার্ভিস অন করুন।"
            _latestResponse.value = msg
            speakResponse(msg)
            return
        }

        _orbState.value = OrbState.PROCESSING
        _statusText.value = "Analyzing screen vision..."

        val prompt = """
            ${vision.toPromptContext()}

            User request: $userQuery
            Please explain, summarize, or answer the user's question clearly based on what is currently active on their screen in natural Bengali or English.
        """.trimIndent()

        queryGeminiAi(prompt, spokenUserQuery = userQuery)
    }

    private fun queryGeminiAi(prompt: String, spokenUserQuery: String? = null) {
        activeJob?.cancel()
        _orbState.value = OrbState.PROCESSING
        _statusText.value = "Thinking..."

        activeJob = viewModelScope.launch {
            val history = conversationsFlow.value.take(4).map {
                Pair(it.userMessage, it.assistantResponse)
            }
            val result = geminiRepo.askGemini(
                prompt = prompt,
                conversationHistory = history,
                preferredLanguage = userPreferences.value.language,
                model = userPreferences.value.selectedGeminiModel
            )

            result.onSuccess { responseText ->
                _latestResponse.value = responseText
                _statusText.value = "Done"
                saveConversation(spokenUserQuery ?: prompt, responseText)
                speakResponse(responseText)
            }.onFailure { ex ->
                _orbState.value = OrbState.ERROR
                _statusText.value = "Error occurred"
                _errorMessage.value = ex.message ?: "Failed to get AI response."
            }
        }
    }

    private fun speakResponse(text: String) {
        if (!userPreferences.value.speechPlaybackEnabled) {
            _orbState.value = OrbState.IDLE
            _statusText.value = "Ready to listen"
            return
        }

        val prefs = userPreferences.value
        ttsManager.speak(
            text = text,
            languageCode = prefs.language,
            speechRate = prefs.speechRate,
            speechPitch = prefs.speechPitch,
            selectedVoiceName = prefs.selectedVoiceName
        )
    }

    private fun saveConversation(userMsg: String, aiResponse: String) {
        viewModelScope.launch {
            database.conversationDao().insert(
                ConversationEntity(
                    sessionId = currentSessionId,
                    userMessage = userMsg,
                    assistantResponse = aiResponse,
                    language = userPreferences.value.language
                )
            )
        }
    }

    fun testApiConnection(keyToTest: String, model: String? = null) {
        _isTestingApi.value = true
        _apiTestStatus.value = null

        viewModelScope.launch {
            val targetModel = model ?: userPreferences.value.selectedGeminiModel
            val result = geminiRepo.testConnection(keyToTest, targetModel)
            _isTestingApi.value = false
            result.onSuccess { msg ->
                _apiTestStatus.value = "Success: $msg"
            }.onFailure { ex ->
                _apiTestStatus.value = "Failed: ${ex.message}"
            }
        }
    }

    fun setSelectedGeminiModel(modelId: String) {
        viewModelScope.launch {
            preferencesRepo.updateSelectedGeminiModel(modelId)
        }
    }

    fun saveApiKey(newKey: String) {
        viewModelScope.launch {
            preferencesRepo.updateApiKey(newKey)
        }
    }

    fun clearApiKey() {
        viewModelScope.launch {
            preferencesRepo.clearApiKey()
            _apiTestStatus.value = null
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            preferencesRepo.updateLanguage(lang)
        }
    }

    fun setSpeechRate(rate: Float) {
        viewModelScope.launch {
            preferencesRepo.updateSpeechRate(rate)
        }
    }

    fun setSelectedVoice(voiceName: String) {
        viewModelScope.launch {
            preferencesRepo.updateSelectedVoice(voiceName)
        }
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.updateWakeWordEnabled(enabled)
        }
    }

    fun setWakeWordPhrase(phrase: String) {
        viewModelScope.launch {
            preferencesRepo.updateWakeWordPhrase(phrase)
        }
    }

    fun setHandsFreeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.updateHandsFreeEnabled(enabled)
        }
    }

    fun setOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.updateOverlayEnabled(enabled)
        }
    }

    fun setSpeechPlaybackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.updateSpeechPlaybackEnabled(enabled)
        }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch {
            preferencesRepo.setOnboardingCompleted(completed)
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            database.conversationDao().deleteAll()
        }
    }

    fun deleteConversationById(id: Long) {
        viewModelScope.launch {
            database.conversationDao().deleteById(id)
        }
    }

    fun deleteMemoryById(id: Long) {
        viewModelScope.launch {
            database.memoryDao().deleteById(id)
        }
    }

    fun deleteReminderById(id: Long) {
        viewModelScope.launch {
            database.reminderDao().deleteById(id)
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            database.conversationDao().deleteAll()
            database.memoryDao().deleteAll()
            database.reminderDao().deleteAll()
            database.taskDao().deleteAll()
            preferencesRepo.resetAllPreferences()
            stopAll()
        }
    }
}
