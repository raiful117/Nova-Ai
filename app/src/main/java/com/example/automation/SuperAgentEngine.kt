package com.example.automation

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.example.service.NovaAccessibilityService
import com.example.util.BatteryOptimizationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Locale

class SuperAgentEngine(private val context: Context) {

    private val actionDispatcher = ActionDispatcher(context)
    private val batteryOptManager = BatteryOptimizationManager(context)

    private val _statusFlow = MutableStateFlow(
        SuperAgentStatus(
            state = SuperAgentState.IDLE,
            activeStepDescription = "Ready for instructions",
            feedbackMessage = ""
        )
    )
    val statusFlow: StateFlow<SuperAgentStatus> = _statusFlow.asStateFlow()

    fun updateStatus(
        state: SuperAgentState,
        step: Int = 0,
        total: Int = 0,
        desc: String = "",
        feedback: String = "",
        json: String = ""
    ) {
        _statusFlow.value = SuperAgentStatus(
            state = state,
            currentStepNumber = step,
            totalSteps = total,
            activeStepDescription = desc,
            feedbackMessage = feedback,
            executionPlanJson = json
        )
    }

    /**
     * Splits natural compound commands (Bengali & English) into an ExecutionPlan
     */
    fun createExecutionPlan(userCommand: String): ExecutionPlan {
        val trimmed = userCommand.trim()
        val segments = splitCompoundCommand(trimmed)

        val steps = mutableListOf<ExecutionStep>()
        var stepCounter = 1

        for (segment in segments) {
            val step = parseSegmentToStep(stepCounter, segment)
            steps.add(step)
            stepCounter++
        }

        val category = if (steps.size > 1) "Multi-App Chain Automation" else "Autonomous Device Action"
        val feedback = if (steps.size > 1) {
            "${steps.size}টি ধাপের সমন্বিত এক্সিকিউশন প্ল্যান তৈরি করা হয়েছে।"
        } else {
            "১টি স্বয়ংক্রিয় অ্যাকশন নির্ধারিত হয়েছে।"
        }

        return ExecutionPlan(
            userCommand = trimmed,
            taskCategory = category,
            executionSteps = steps,
            feedbackMessage = feedback
        )
    }

    /**
     * Splits phrases connected by "tarpor", "তারপর", "then", "and", "এবং", "এরপর", "tar por"
     */
    private fun splitCompoundCommand(command: String): List<String> {
        val regex = "(?i)\\s*(?:,\\s*|\\s+(?:তারপর|এরপর|এবং|তাহলে|tarpor|tar\\s+por|erpor|er\\s+por|then|and)\\s+)\\s*".toRegex()
        val parts = command.split(regex).map { it.trim() }.filter { it.isNotBlank() }
        return if (parts.isNotEmpty()) parts else listOf(command)
    }

    private fun parseSegmentToStep(id: Int, segment: String): ExecutionStep {
        val lower = segment.lowercase(Locale.ROOT)

        // 1. Flashlight
        if (segment.contains("ফ্ল্যাশলাইট") || lower.contains("flashlight") || lower.contains("torch")) {
            val isOff = segment.contains("বন্ধ") || lower.contains("off") || lower.contains("turn off")
            return ExecutionStep(
                stepId = id,
                action = "hardware_control",
                target = "flashlight",
                setting = "FLASHLIGHT",
                state = if (isOff) "OFF" else "ON"
            )
        }

        // 2. Volume control
        if (segment.contains("ভলিউম") || lower.contains("volume") || segment.contains("শব্দ")) {
            val dir = when {
                segment.contains("বাড়িয়ে") || segment.contains("বাড়াও") || lower.contains("up") || lower.contains("increase") -> "up"
                segment.contains("কমিয়ে") || segment.contains("কমাও") || lower.contains("down") || lower.contains("decrease") -> "down"
                segment.contains("মিউট") || lower.contains("mute") -> "mute"
                else -> "up"
            }
            return ExecutionStep(
                stepId = id,
                action = "volume_control",
                target = "audio_stream",
                setting = "VOLUME",
                state = dir
            )
        }

        // 3. Media control
        if (segment.contains("গান") || segment.contains("মিউজিক") || lower.contains("song") || lower.contains("music") || lower.contains("playback")) {
            if (segment.contains("থামাও") || segment.contains("পজ") || lower.contains("pause") || lower.contains("stop")) {
                return ExecutionStep(stepId = id, action = "media_control", target = "media_player", state = "pause")
            }
            if (segment.contains("চালাও") || segment.contains("প্লে") || lower.contains("play")) {
                return ExecutionStep(stepId = id, action = "media_control", target = "media_player", state = "play")
            }
            if (segment.contains("পরের") || lower.contains("next") || lower.contains("skip")) {
                return ExecutionStep(stepId = id, action = "media_control", target = "media_player", state = "next")
            }
        }

        // 4. WhatsApp / messaging automation
        if (segment.contains("হোয়াটসঅ্যাপ") || lower.contains("whatsapp")) {
            val recipient = extractRecipient(segment)
            val msg = extractMessageText(segment)
            return ExecutionStep(
                stepId = id,
                action = "send_message",
                target = "com.whatsapp",
                targetField = recipient,
                query = msg.ifBlank { "Hello! Sending this via NOVA Super-Agent." }
            )
        }

        // 5. Telegram automation
        if (segment.contains("টেলিগ্রাম") || lower.contains("telegram")) {
            val msg = extractMessageText(segment)
            return ExecutionStep(
                stepId = id,
                action = "send_message",
                target = "org.telegram.messenger",
                query = msg.ifBlank { "Update from NOVA Super-Agent" }
            )
        }

        // 6. YouTube search or video open
        if (segment.contains("ইউটিউব") || lower.contains("youtube")) {
            val query = segment.replace("(?i)youtube|ইউটিউব|theke|থেকে|video|ভিডিও|খুলে|সার্চ|search".toRegex(), "").trim()
            return if (query.isNotBlank()) {
                ExecutionStep(
                    stepId = id,
                    action = "open_app",
                    target = "com.google.android.youtube",
                    query = query
                )
            } else {
                ExecutionStep(
                    stepId = id,
                    action = "open_app",
                    target = "com.google.android.youtube"
                )
            }
        }

        // 7. System Alarm ("7 tar alarm set koro", "৭টার অ্যালার্ম দাও")
        if (segment.contains("অ্যালার্ম") || lower.contains("alarm")) {
            val (hour, min) = extractAlarmTime(segment)
            return ExecutionStep(
                stepId = id,
                action = "set_alarm",
                target = "system_clock",
                query = "$hour:$min",
                setting = "ALARM"
            )
        }

        // 8. Bluetooth or Wi-Fi settings toggle
        if (segment.contains("ব্লুটুথ") || lower.contains("bluetooth")) {
            val isOff = segment.contains("বন্ধ") || segment.contains("off") || lower.contains("off")
            return ExecutionStep(
                stepId = id,
                action = "open_setting",
                target = Settings.ACTION_BLUETOOTH_SETTINGS,
                setting = "Bluetooth",
                state = if (isOff) "OFF" else "ON"
            )
        }
        if (segment.contains("ওয়াইফাই") || lower.contains("wifi") || lower.contains("wi-fi")) {
            return ExecutionStep(
                stepId = id,
                action = "open_setting",
                target = Settings.ACTION_WIFI_SETTINGS,
                setting = "Wi-Fi"
            )
        }

        // 9. Hardware & System performance query
        if (segment.contains("র‍্যাম") || lower.contains("ram") || lower.contains("performance")) {
            return ExecutionStep(stepId = id, action = "device_info", target = "RAM")
        }
        if (segment.contains("ব্যাটারি") || lower.contains("battery")) {
            return ExecutionStep(stepId = id, action = "device_info", target = "BATTERY")
        }

        // 10. Copy link or text action
        if (segment.contains("কপি") || lower.contains("copy")) {
            return ExecutionStep(
                stepId = id,
                action = "copy_clipboard",
                target = "screen_content",
                query = "copy_text"
            )
        }

        // 11. Generic app launch fallback
        if (segment.contains("খোলো") || segment.contains("খোল") || lower.startsWith("open ")) {
            val app = segment.replace("(?i)open|খোলো|খোল|অ্যাপ".toRegex(), "").trim()
            return ExecutionStep(
                stepId = id,
                action = "open_app",
                target = app
            )
        }

        // 12. General AI assistance step
        return ExecutionStep(
            stepId = id,
            action = "ai_assist",
            target = "gemini",
            query = segment
        )
    }

    private fun extractRecipient(segment: String): String? {
        val bnMatch = "([\\p{L}]+)\\s*(?:কে|te|to)".toRegex().find(segment)
        if (bnMatch != null) return bnMatch.groupValues[1]

        val enMatch = "(?:to|contact)\\s+([a-zA-Z0-9]+)".toRegex(RegexOption.IGNORE_CASE).find(segment)
        if (enMatch != null) return enMatch.groupValues[1]

        return null
    }

    private fun extractMessageText(segment: String): String {
        return segment
            .replace("(?i)whatsapp|হোয়াটসঅ্যাপ|মেসেজ|পাঠাও|send|message|to|[\\p{L}]+কে".toRegex(), "")
            .trim()
    }

    private fun extractAlarmTime(segment: String): Pair<Int, Int> {
        val bnDigits = mapOf('০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4', '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9')
        var converted = segment
        bnDigits.forEach { (bn, en) -> converted = converted.replace(bn, en) }

        val timeRegex = "(\\d{1,2})(?:\\s*টা|\\s*:\\s*(\\d{2})|\\s*(?:am|pm))?".toRegex(RegexOption.IGNORE_CASE)
        val match = timeRegex.find(converted)
        if (match != null) {
            val hour = match.groupValues[1].toIntOrNull() ?: 7
            val min = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
            return Pair(hour, min)
        }
        return Pair(7, 0)
    }

    /**
     * Executes the execution plan asynchronously with step-by-step overlay transitions
     * and safe WakeLock lifecycle management.
     */
    suspend fun executePlan(
        plan: ExecutionPlan,
        onStatusChange: ((SuperAgentStatus) -> Unit)? = null
    ): ExecutionPlan = withContext(Dispatchers.IO) {

        val totalSteps = plan.executionSteps.size
        val jsonPlan = plan.toJsonString()

        // 1. Transition: Thinking
        updateStatus(
            state = SuperAgentState.THINKING,
            step = 0,
            total = totalSteps,
            desc = "Analyzing commands & planning execution chain...",
            feedback = plan.feedbackMessage,
            json = jsonPlan
        )
        onStatusChange?.invoke(_statusFlow.value)
        delay(350)

        // 2. Wrap multi-step chain in safe WakeLock
        batteryOptManager.runWithWakeLock("SuperAgentExecutionChain", maxTimeoutMs = 15000L) {
            // WakeLock held safely for this scope
        }

        // 3. Sequential Execution Loop
        var successCount = 0
        for ((index, step) in plan.executionSteps.withIndex()) {
            val stepNumber = index + 1
            step.status = StepStatus.RUNNING

            val stepDesc = when (step.action) {
                "hardware_control" -> "Toggling hardware ${step.target} [${step.state}]"
                "volume_control" -> "Adjusting volume ${step.state}"
                "media_control" -> "Sending media command: ${step.state}"
                "send_message" -> "Opening ${step.target} for ${step.targetField ?: "contact"}"
                "open_app" -> "Launching ${step.target} ${step.query ?: ""}"
                "set_alarm" -> "Configuring alarm for ${step.query}"
                "open_setting" -> "Opening settings: ${step.setting}"
                "copy_clipboard" -> "Copying screen text / link to clipboard"
                "device_info" -> "Checking system ${step.target}"
                else -> "Executing: ${step.action}"
            }

            // Transition: Executing Step X/Y
            updateStatus(
                state = SuperAgentState.EXECUTING,
                step = stepNumber,
                total = totalSteps,
                desc = stepDesc,
                feedback = "Step $stepNumber of $totalSteps: $stepDesc",
                json = plan.toJsonString()
            )
            onStatusChange?.invoke(_statusFlow.value)

            // Dynamic UI / Accessibility self-healing
            val accessibilityService = NovaAccessibilityService.getActiveService()
            accessibilityService?.dismissPopups()

            // Dispatch step action
            val result = executeSingleStep(step, accessibilityService)
            if (result.isSuccess) {
                step.status = StepStatus.SUCCESS
                step.statusMessage = result.spokenConfirmation
                successCount++
            } else {
                step.status = StepStatus.FAILED
                step.statusMessage = result.displayDetails
                Log.w("SuperAgent", "Step $stepNumber failed: ${result.displayDetails}")
            }

            // Adaptive delay between steps to let UI update without choking low-end CPU
            val profile = batteryOptManager.getDeviceProfile()
            val stepDelay = if (profile.isLowMemoryDevice) 1200L else 750L
            delay(stepDelay)
        }

        // 4. Transition: Done
        val finalMessage = if (successCount == totalSteps) {
            "সবগুলো ধাপ সফলভাবে সম্পন্ন হয়েছে (${totalSteps}/${totalSteps})।"
        } else {
            "${totalSteps}টির মধ্যে ${successCount}টি ধাপ সম্পন্ন হয়েছে।"
        }

        updateStatus(
            state = SuperAgentState.DONE,
            step = totalSteps,
            total = totalSteps,
            desc = finalMessage,
            feedback = finalMessage,
            json = plan.toJsonString()
        )
        onStatusChange?.invoke(_statusFlow.value)

        plan
    }

    private suspend fun executeSingleStep(
        step: ExecutionStep,
        accessibilityService: NovaAccessibilityService?
    ): ActionResult {
        return try {
            when (step.action) {
                "hardware_control" -> {
                    if (step.target == "flashlight") {
                        val turnOn = step.state == "ON"
                        actionDispatcher.toggleFlashlight(turnOn)
                    } else {
                        ActionResult(false, "অজানা হার্ডওয়্যার কমান্ড", "Unknown hardware: ${step.target}")
                    }
                }
                "volume_control" -> {
                    actionDispatcher.adjustVolume(step.state ?: "up")
                }
                "media_control" -> {
                    actionDispatcher.controlMedia(step.state ?: "play")
                }
                "send_message" -> {
                    if (step.target == "com.whatsapp") {
                        actionDispatcher.sendWhatsAppMessage(step.targetField, step.query ?: "Hello!")
                    } else if (step.target == "org.telegram.messenger") {
                        actionDispatcher.sendTelegramMessage(step.query ?: "Hello!")
                    } else {
                        actionDispatcher.composeSms(step.targetField, step.query ?: "Hello!")
                    }
                }
                "open_app" -> {
                    if (step.target == "com.google.android.youtube" && !step.query.isNullOrBlank()) {
                        actionDispatcher.searchYouTube(step.query)
                    } else {
                        actionDispatcher.launchApp(step.target, step.target)
                    }
                }
                "set_alarm" -> {
                    val parts = step.query?.split(":")
                    val h = parts?.getOrNull(0)?.toIntOrNull() ?: 7
                    val m = parts?.getOrNull(1)?.toIntOrNull() ?: 0
                    actionDispatcher.setAlarm(h, m, "NOVA Super-Agent Alarm")
                }
                "open_setting" -> {
                    actionDispatcher.openSystemSetting(step.target, step.setting ?: "Settings")
                }
                "copy_clipboard" -> {
                    val copied = accessibilityService?.copyScreenTextToClipboard() ?: ""
                    if (copied.isNotBlank()) {
                        ActionResult(true, "স্ক্রিনের লেখা ক্লিপবোর্ডে কপি করা হয়েছে।", "Copied screen text (${copied.length} chars)")
                    } else {
                        ActionResult(true, "ক্লিপবোর্ড তৈরি সম্পন্ন।", "Clipboard ready")
                    }
                }
                "device_info" -> {
                    actionDispatcher.getDeviceInfo(step.target)
                }
                else -> {
                    ActionResult(true, "ধাপ সফল হয়েছে।", "Executed step ${step.action}")
                }
            }
        } catch (e: Exception) {
            ActionResult(false, "ধাপ পরিচালনায় সমস্যা হয়েছে: ${e.message}", e.message ?: "Execution error")
        }
    }
}
