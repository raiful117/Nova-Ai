package com.example.automation

import android.provider.Settings
import java.util.Locale
import java.util.regex.Pattern

object CommandParser {

    fun parse(rawInput: String): CommandIntent {
        val input = rawInput.trim()
        val lower = input.lowercase(Locale.ROOT)

        // 1. Emergency stop commands
        if (lower in listOf("stop", "cancel", "stop everything", "quit") ||
            input.contains("থামো") || input.contains("বন্ধ করো") || input.contains("সব বন্ধ করো") ||
            input.contains("রুকো") || lower.contains("stop now")
        ) {
            return CommandIntent.StopAll
        }

        // 2. Screen understanding
        if (input.contains("স্ক্রিনে কী আছে") || lower.contains("screen-e ki ache") ||
            lower.contains("explain screen") || lower.contains("what is on screen") ||
            input.contains("এই পেজের লেখাটা পড়ে শোনাও") || input.contains("স্ক্রিন পড়ে শোনাও")
        ) {
            return CommandIntent.ScreenUnderstanding(input)
        }

        // 3. Calculator and math
        val mathMatch = extractMathExpression(input)
        if (mathMatch != null) {
            val eval = evaluateMath(mathMatch)
            if (eval != null) {
                return CommandIntent.Calculator(mathMatch, eval)
            }
        }

        // 4. Memory and Notes ("মনে রাখো...", "Remember that...", "নোট লেখো...")
        if (input.startsWith("মনে রাখো") || lower.startsWith("remember that") || lower.startsWith("remember ")) {
            val noteContent = input
                .replaceFirst("(?i)মনে রাখো".toRegex(), "")
                .replaceFirst("(?i)remember that".toRegex(), "")
                .replaceFirst("(?i)remember".toRegex(), "")
                .trim()
            if (noteContent.isNotBlank()) {
                return CommandIntent.SaveMemory(
                    key = "Preference",
                    content = noteContent,
                    category = "PREFERENCE"
                )
            }
        }
        if (input.startsWith("নোট লেখো") || lower.startsWith("take a note") || lower.startsWith("save note")) {
            val noteContent = input
                .replaceFirst("(?i)নোট লেখো".toRegex(), "")
                .replaceFirst("(?i)take a note".toRegex(), "")
                .replaceFirst("(?i)save note".toRegex(), "")
                .trim()
            if (noteContent.isNotBlank()) {
                return CommandIntent.SaveMemory(
                    key = "Quick Note",
                    content = noteContent,
                    category = "NOTE"
                )
            }
        }

        // 5. Reminders ("৫ মিনিট পরে মনে করিয়ে দিও", "remind me in 10 minutes to...")
        val reminderMinutes = extractReminderMinutes(input)
        if (reminderMinutes != null && reminderMinutes > 0) {
            val title = cleanReminderTitle(input)
            return CommandIntent.Reminder(title = title, delayMinutes = reminderMinutes)
        }

        // 6. YouTube search or launch
        if (lower.contains("youtube") || input.contains("ইউটিউব")) {
            val query = extractSearchQuery(input, listOf("youtube", "ইউটিউব", "সার্চ করো", "search for", "search", "খুলে", "খোল", "open"))
            return if (query.isNotBlank()) {
                CommandIntent.YouTubeSearch(query)
            } else {
                CommandIntent.AppLaunch("YouTube", "com.google.android.youtube")
            }
        }

        // 7. Communication: Call / Phone dialer
        if (lower.startsWith("call ") || input.startsWith("কল করো") || input.contains("ফোন করো") || lower.contains("dial ")) {
            val phoneNum = input.replace("[^0-9+]".toRegex(), "")
            return CommandIntent.Dialer(
                phoneNumber = if (phoneNum.length >= 3) phoneNum else null,
                contactName = input.replace("call", "", true).replace("কল করো", "").trim()
            )
        }

        // 8. Communication: SMS draft
        if (lower.contains("send sms") || lower.contains("send message") || input.contains("মেসেজ পাঠাও") || input.contains("মেসেজ লেখো")) {
            val msgBody = input
                .replace("send sms to", "", true)
                .replace("send message", "", true)
                .replace("মেসেজ পাঠাও", "")
                .trim()
            return CommandIntent.SmsDraft(recipient = null, message = msgBody.ifBlank { "Hello!" })
        }

        // 9. Communication: Email draft
        if (lower.contains("send email") || lower.contains("compose email") || input.contains("ইমেইল পাঠাও") || input.contains("ইমেইল লেখো")) {
            val body = input
                .replace("send email", "", true)
                .replace("compose email", "", true)
                .replace("ইমেইল পাঠাও", "")
                .trim()
            return CommandIntent.EmailDraft(recipient = null, subject = "Message from NOVA AI", body = body)
        }

        // 10. System Settings panels
        if (input.contains("ওয়াইফাই") || lower.contains("wi-fi") || lower.contains("wifi")) {
            return CommandIntent.SystemSetting("Wi-Fi Settings", Settings.ACTION_WIFI_SETTINGS)
        }
        if (input.contains("ব্লুটুথ") || lower.contains("bluetooth")) {
            return CommandIntent.SystemSetting("Bluetooth Settings", Settings.ACTION_BLUETOOTH_SETTINGS)
        }
        if (input.contains("ডিসপ্লে") || lower.contains("display") || lower.contains("brightness")) {
            return CommandIntent.SystemSetting("Display Settings", Settings.ACTION_DISPLAY_SETTINGS)
        }
        if (input.contains("শব্দ") || lower.contains("sound") || lower.contains("volume")) {
            return CommandIntent.SystemSetting("Sound Settings", Settings.ACTION_SOUND_SETTINGS)
        }
        if (input.contains("ব্যাটারি") && (input.contains("সেটিংস") || lower.contains("settings"))) {
            return CommandIntent.SystemSetting("Battery Settings", Settings.ACTION_BATTERY_SAVER_SETTINGS)
        }
        if (input.contains("এক্সেসিবিলিটি") || lower.contains("accessibility")) {
            return CommandIntent.SystemSetting("Accessibility Settings", Settings.ACTION_ACCESSIBILITY_SETTINGS)
        }
        if (input.contains("সেটিংস খোলো") || lower == "open settings" || lower == "settings") {
            return CommandIntent.SystemSetting("System Settings", Settings.ACTION_SETTINGS)
        }

        // 11. Device Info queries
        if (input.contains("ব্যাটারি") || lower.contains("battery")) {
            return CommandIntent.DeviceInfo("BATTERY")
        }
        if (input.contains("স্টোরেজ") || lower.contains("storage") || lower.contains("memory available")) {
            return CommandIntent.DeviceInfo("STORAGE")
        }
        if (input.contains("নেটওয়ার্ক") || lower.contains("network") || lower.contains("internet connection")) {
            return CommandIntent.DeviceInfo("NETWORK")
        }
        if (input.contains("সময় কত") || lower.contains("what time") || lower.contains("current time") || input.contains("তারিখ কত")) {
            return CommandIntent.DeviceInfo("TIME")
        }

        // 12. Common App Launchers
        if (lower.contains("chrome") || input.contains("ক্রোম")) {
            return CommandIntent.AppLaunch("Google Chrome", "com.android.chrome")
        }
        if (lower.contains("camera") || input.contains("ক্যামেরা")) {
            return CommandIntent.AppLaunch("Camera", null)
        }
        if (lower.contains("maps") || input.contains("ম্যাপ") || input.contains("গুগল ম্যাপ")) {
            return CommandIntent.AppLaunch("Google Maps", "com.google.android.apps.maps")
        }
        if (lower.contains("whatsapp") || input.contains("হোয়াটসঅ্যাপ")) {
            return CommandIntent.AppLaunch("WhatsApp", "com.whatsapp")
        }
        if (lower.contains("facebook") || input.contains("ফেসবুক")) {
            return CommandIntent.AppLaunch("Facebook", "com.facebook.katana")
        }

        // 13. Web Search
        if (lower.startsWith("search ") || lower.startsWith("google ") || input.startsWith("সার্চ করো") || input.contains("গুগলে খুঁজ")) {
            val query = input
                .replaceFirst("(?i)search for".toRegex(), "")
                .replaceFirst("(?i)search".toRegex(), "")
                .replaceFirst("(?i)google".toRegex(), "")
                .replace("সার্চ করো", "")
                .replace("গুগলে খুঁজ", "")
                .trim()
            if (query.isNotBlank()) {
                return CommandIntent.WebSearch(query)
            }
        }

        // 14. Default to General Gemini AI response
        return CommandIntent.GeneralAiQuery(input)
    }

    private fun extractSearchQuery(input: String, keywordsToRemove: List<String>): String {
        var cleaned = input
        for (kw in keywordsToRemove) {
            cleaned = cleaned.replace(kw, "", ignoreCase = true)
        }
        return cleaned.trim()
    }

    private fun extractReminderMinutes(input: String): Long? {
        // Match English: "in 5 minutes", "5 mins", "10 minute"
        val engRegex = "(\\d+)\\s*(?:minute|min|minutes|mins)".toRegex(RegexOption.IGNORE_CASE)
        val engMatch = engRegex.find(input)
        if (engMatch != null) {
            return engMatch.groupValues[1].toLongOrNull()
        }

        // Match Bengali numerals: ১, ২, ৩, ৪, ৫, ৬, ৭, ৮, ৯, ১০
        val bnDigits = mapOf('০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4', '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9')
        if (input.contains("মিনিট")) {
            var converted = input
            bnDigits.forEach { (bn, en) -> converted = converted.replace(bn, en) }
            val bnMatch = "(\\d+)\\s*মিনিট".toRegex().find(converted)
            if (bnMatch != null) {
                return bnMatch.groupValues[1].toLongOrNull()
            }
        }
        return null
    }

    private fun cleanReminderTitle(input: String): String {
        var title = input
            .replace("\\d+\\s*(?:minute|min|minutes|mins|মিনিট)".toRegex(RegexOption.IGNORE_CASE), "")
            .replace("remind me to", "", true)
            .replace("remind me in", "", true)
            .replace("পরে মনে করিয়ে দিও", "")
            .replace("পর মনে করিয়ে দাও", "")
            .replace("মনে করিয়ে দাও", "")
            .trim()
        return title.ifBlank { "NOVA AI Reminder" }
    }

    private fun extractMathExpression(input: String): String? {
        val mathPattern = Pattern.compile("(?i)(?:calculate|what is|কত)?\\s*(\\d+(?:\\.\\d+)?\\s*(?:[+\\-*/x%]|percent of)\\s*\\d+(?:\\.\\d+)?).*")
        val matcher = mathPattern.matcher(input)
        if (matcher.find()) {
            return matcher.group(1)?.trim()
        }
        return null
    }

    private fun evaluateMath(expr: String): String? {
        return try {
            val clean = expr.replace("x", "*").replace("X", "*")
            if (clean.contains("percent of") || clean.contains("%")) {
                val parts = clean.split("percent of", "%")
                if (parts.size >= 2) {
                    val p = parts[0].trim().toDouble()
                    val total = parts[1].trim().toDouble()
                    val res = (p / 100.0) * total
                    return "$res (বাংলায়: $res)"
                }
            }
            if (clean.contains("+")) {
                val p = clean.split("+")
                return (p[0].trim().toDouble() + p[1].trim().toDouble()).toString()
            }
            if (clean.contains("-")) {
                val p = clean.split("-")
                return (p[0].trim().toDouble() - p[1].trim().toDouble()).toString()
            }
            if (clean.contains("*")) {
                val p = clean.split("*")
                return (p[0].trim().toDouble() * p[1].trim().toDouble()).toString()
            }
            if (clean.contains("/")) {
                val p = clean.split("/")
                val denom = p[1].trim().toDouble()
                if (denom != 0.0) {
                    return (p[0].trim().toDouble() / denom).toString()
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
