package com.example.automation

sealed class CommandIntent {
    data class AppLaunch(val appName: String, val packageName: String?) : CommandIntent()
    data class SystemSetting(val title: String, val intentAction: String) : CommandIntent()
    data class Dialer(val phoneNumber: String?, val contactName: String?) : CommandIntent()
    data class SmsDraft(val recipient: String?, val message: String) : CommandIntent()
    data class EmailDraft(val recipient: String?, val subject: String, val body: String) : CommandIntent()
    data class WebSearch(val query: String) : CommandIntent()
    data class YouTubeSearch(val query: String) : CommandIntent()
    data class Calculator(val expression: String, val evaluatedResult: String) : CommandIntent()
    data class Reminder(val title: String, val delayMinutes: Long) : CommandIntent()
    data class SaveMemory(val key: String, val content: String, val category: String) : CommandIntent()
    data class DeviceInfo(val infoType: String) : CommandIntent()
    object StopAll : CommandIntent()
    data class ScreenUnderstanding(val query: String) : CommandIntent()
    data class GeneralAiQuery(val prompt: String) : CommandIntent()
}

data class ActionResult(
    val isSuccess: Boolean,
    val spokenConfirmation: String,
    val displayDetails: String,
    val requiresUserConfirmation: Boolean = false,
    val confirmationPrompt: String? = null
)
