package com.example.automation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import com.example.data.local.MemoryEntity
import com.example.data.local.NovaDatabase
import com.example.data.local.ReminderEntity
import com.example.receiver.ReminderAlarmReceiver
import com.example.util.BatteryOptimizationManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActionDispatcher(private val context: Context) {

    private val database = NovaDatabase.getDatabase(context)
    private val batteryOptManager = BatteryOptimizationManager(context)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    companion object {
        private var isFlashlightOn = false
    }

    fun launchApp(appName: String, packageName: String?): ActionResult {
        return try {
            val pm = context.packageManager
            var launchIntent: Intent? = null

            if (!packageName.isNullOrBlank()) {
                launchIntent = pm.getLaunchIntentForPackage(packageName)
            }

            if (launchIntent == null) {
                // Try fallback intents for well-known targets
                launchIntent = when (appName.lowercase(Locale.ROOT)) {
                    "camera" -> Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    "youtube" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
                    "chrome" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                    "whatsapp" -> pm.getLaunchIntentForPackage("com.whatsapp")
                    "telegram" -> pm.getLaunchIntentForPackage("org.telegram.messenger")
                    "spotify" -> pm.getLaunchIntentForPackage("com.spotify.music")
                    else -> null
                }
            }

            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                ActionResult(
                    isSuccess = true,
                    spokenConfirmation = "$appName খোলা হচ্ছে।",
                    displayDetails = "Launched application: $appName"
                )
            } else {
                ActionResult(
                    isSuccess = false,
                    spokenConfirmation = "$appName ইনস্টল করা নেই বা চালু করা যায়নি।",
                    displayDetails = "App '$appName' is not installed or exposes no launcher activity."
                )
            }
        } catch (e: Exception) {
            ActionResult(
                isSuccess = false,
                spokenConfirmation = "অ্যাপটি চালু করতে সমস্যা হয়েছে।",
                displayDetails = "Error launching $appName: ${e.message}"
            )
        }
    }

    fun toggleFlashlight(desiredState: Boolean? = null): ActionResult {
        return try {
            if (cameraManager == null) {
                return ActionResult(false, "ফ্ল্যাশলাইট সমর্থন করে না।", "CameraManager unavailable")
            }
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cameraManager.cameraIdList.firstOrNull()

            if (cameraId == null) {
                return ActionResult(false, "ডিভাইসে কোনো ফ্ল্যাশলাইট পাওয়া যায়নি।", "No camera with flash found")
            }

            val targetState = desiredState ?: !isFlashlightOn
            cameraManager.setTorchMode(cameraId, targetState)
            isFlashlightOn = targetState

            val msg = if (targetState) "ফ্ল্যাশলাইট অন করা হয়েছে।" else "ফ্ল্যাশলাইট বন্ধ করা হয়েছে।"
            ActionResult(
                isSuccess = true,
                spokenConfirmation = msg,
                displayDetails = "Flashlight set to: ${if (targetState) "ON" else "OFF"}"
            )
        } catch (e: Exception) {
            ActionResult(
                isSuccess = false,
                spokenConfirmation = "ফ্ল্যাশলাইট পরিবর্তন করা যায়নি।",
                displayDetails = "Flashlight error: ${e.message}"
            )
        }
    }

    fun adjustVolume(direction: String): ActionResult {
        return try {
            if (audioManager == null) return ActionResult(false, "অডিও ম্যানেজার পাওয়া যায়নি।", "AudioManager null")

            when (direction.lowercase(Locale.ROOT)) {
                "up" -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    ActionResult(true, "সাউন্ড ভলিউম বাড়ানো হয়েছে।", "Volume increased")
                }
                "down" -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    ActionResult(true, "সাউন্ড ভলিউম কমানো হয়েছে।", "Volume decreased")
                }
                "mute" -> {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
                    ActionResult(true, "সাউন্ড মিউট করা হয়েছে।", "Volume muted")
                }
                else -> {
                    ActionResult(false, "ভলিউম নির্দেশনা বোঝা যায়নি।", "Unknown volume direction: $direction")
                }
            }
        } catch (e: Exception) {
            ActionResult(false, "ভলিউম নিয়ন্ত্রণ করা যায়নি।", "Volume error: ${e.message}")
        }
    }

    fun controlMedia(mediaAction: String): ActionResult {
        return try {
            if (audioManager == null) return ActionResult(false, "মিডিয়া কন্ট্রোলার পাওয়া যায়নি।", "AudioManager null")

            val keyCode = when (mediaAction.lowercase(Locale.ROOT)) {
                "play", "pause", "toggle" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                "next", "skip" -> KeyEvent.KEYCODE_MEDIA_NEXT
                "previous", "prev" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
                "stop" -> KeyEvent.KEYCODE_MEDIA_STOP
                else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            }

            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))

            ActionResult(
                isSuccess = true,
                spokenConfirmation = "মিডিয়া $mediaAction নির্দেশ পাঠানো হয়েছে।",
                displayDetails = "Dispatched media event: $mediaAction"
            )
        } catch (e: Exception) {
            ActionResult(false, "মিডিয়া নিয়ন্ত্রণ করা যায়নি।", "Media error: ${e.message}")
        }
    }

    fun sendWhatsAppMessage(recipientOrPhone: String?, messageText: String): ActionResult {
        return try {
            val phoneDigits = recipientOrPhone?.replace("[^0-9+]".toRegex(), "") ?: ""
            val intent = Intent(Intent.ACTION_VIEW).apply {
                if (phoneDigits.length >= 7) {
                    val cleanPhone = phoneDigits.replace("+", "")
                    data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(messageText)}")
                } else {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, messageText)
                    `package` = "com.whatsapp"
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(
                isSuccess = true,
                spokenConfirmation = "হোয়াটসঅ্যাপে মেসেজ পাঠানো হচ্ছে: $messageText",
                displayDetails = "WhatsApp message dispatched to ${recipientOrPhone ?: "chat"}"
            )
        } catch (e: Exception) {
            ActionResult(
                isSuccess = false,
                spokenConfirmation = "হোয়াটসঅ্যাপ মেসেজ পাঠানো যায়নি।",
                displayDetails = "WhatsApp dispatch error: ${e.message}"
            )
        }
    }

    fun sendTelegramMessage(messageText: String): ActionResult {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, messageText)
                `package` = "org.telegram.messenger"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(
                isSuccess = true,
                spokenConfirmation = "টেলিগ্রামে মেসেজ পাঠানো হচ্ছে।",
                displayDetails = "Dispatched text to Telegram: $messageText"
            )
        } catch (e: Exception) {
            ActionResult(
                isSuccess = false,
                spokenConfirmation = "টেলিগ্রাম অ্যাপ খোলা যায়নি।",
                displayDetails = "Telegram dispatch error: ${e.message}"
            )
        }
    }

    fun setAlarm(hour: Int, minute: Int, label: String = "NOVA Alarm"): ActionResult {
        return try {
            val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(alarmIntent)
            val minStr = if (minute < 10) "0$minute" else "$minute"
            ActionResult(
                isSuccess = true,
                spokenConfirmation = "সকাল/সন্ধ্যা $hour টা $minStr মিনিটে অ্যালার্ম সেট করা হয়েছে।",
                displayDetails = "Set system alarm for $hour:$minStr ($label)"
            )
        } catch (e: Exception) {
            ActionResult(
                isSuccess = false,
                spokenConfirmation = "অ্যালার্ম সেট করা যায়নি।",
                displayDetails = "Alarm error: ${e.message}"
            )
        }
    }

    fun openSystemSetting(intentAction: String, title: String): ActionResult {
        return try {
            val intent = Intent(intentAction).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(
                isSuccess = true,
                spokenConfirmation = "$title প্যানেল খোলা হচ্ছে।",
                displayDetails = "Opened Android setting panel: $title"
            )
        } catch (e: Exception) {
            ActionResult(
                isSuccess = false,
                spokenConfirmation = "$title খুলতে সমস্যা হয়েছে।",
                displayDetails = "Failed to open setting $title: ${e.message}"
            )
        }
    }

    fun openDialer(phoneNumber: String?, contactName: String?): ActionResult {
        return try {
            val uri = if (!phoneNumber.isNullOrBlank()) {
                Uri.parse("tel:$phoneNumber")
            } else {
                Uri.parse("tel:")
            }
            val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val confirmation = if (!phoneNumber.isNullOrBlank()) {
                "$phoneNumber নম্বরে ডায়াল প্যাড খোলা হচ্ছে।"
            } else {
                "ডায়াল প্যাড খোলা হচ্ছে।"
            }
            ActionResult(
                isSuccess = true,
                spokenConfirmation = confirmation,
                displayDetails = "Opened Phone Dialer with $phoneNumber"
            )
        } catch (e: Exception) {
            ActionResult(
                isSuccess = false,
                spokenConfirmation = "ফোন ডায়াল প্যাড খোলা যায়নি।",
                displayDetails = "Dialer error: ${e.message}"
            )
        }
    }

    fun composeSms(recipient: String?, message: String): ActionResult {
        return try {
            val uri = if (!recipient.isNullOrBlank()) Uri.parse("smsto:$recipient") else Uri.parse("smsto:")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(
                isSuccess = true,
                spokenConfirmation = "মেসেজ ড্রাফট প্রস্তুত করা হয়েছে।",
                displayDetails = "Drafted SMS: \"$message\""
            )
        } catch (e: Exception) {
            ActionResult(
                isSuccess = false,
                spokenConfirmation = "মেসেজ ড্রাফট খুলতে সমস্যা হয়েছে।",
                displayDetails = "SMS compose error: ${e.message}"
            )
        }
    }

    fun composeEmail(recipient: String?, subject: String, body: String): ActionResult {
        return try {
            val uri = Uri.parse("mailto:${recipient ?: ""}")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(
                isSuccess = true,
                spokenConfirmation = "ইমেইল ড্রাফট খোলা হচ্ছে।",
                displayDetails = "Drafted Email to ${recipient ?: "unspecified"}"
            )
        } catch (e: Exception) {
            ActionResult(
                isSuccess = false,
                spokenConfirmation = "ইমেইল অ্যাপ খুলতে সমস্যা হয়েছে।",
                displayDetails = "Email compose error: ${e.message}"
            )
        }
    }

    fun searchWeb(query: String): ActionResult {
        return try {
            val uri = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(
                isSuccess = true,
                spokenConfirmation = "গুগলে \"$query\" সার্চ করা হচ্ছে।",
                displayDetails = "Web search opened for: $query"
            )
        } catch (e: Exception) {
            ActionResult(
                isSuccess = false,
                spokenConfirmation = "সার্চ করতে সমস্যা হয়েছে।",
                displayDetails = "Web search error: ${e.message}"
            )
        }
    }

    fun searchYouTube(query: String): ActionResult {
        return try {
            val uri = Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(
                isSuccess = true,
                spokenConfirmation = "ইউটিউবে \"$query\" সার্চ করা হচ্ছে।",
                displayDetails = "YouTube search opened for: $query"
            )
        } catch (e: Exception) {
            ActionResult(
                isSuccess = false,
                spokenConfirmation = "ইউটিউব সার্চ খুলতে সমস্যা হয়েছে।",
                displayDetails = "YouTube search error: ${e.message}"
            )
        }
    }

    suspend fun saveMemory(key: String, content: String, category: String): ActionResult {
        return try {
            database.memoryDao().insert(
                MemoryEntity(
                    category = category,
                    key = key,
                    content = content
                )
            )
            val spoken = if (category == "PREFERENCE") {
                "আমি মনে রাখব: $content"
            } else {
                "নোটটি সংরক্ষণ করা হয়েছে।"
            }
            ActionResult(
                isSuccess = true,
                spokenConfirmation = spoken,
                displayDetails = "Saved to Nova Memory: \"$content\""
            )
        } catch (e: Exception) {
            ActionResult(
                isSuccess = false,
                spokenConfirmation = "সংরক্ষণ করতে সমস্যা হয়েছে।",
                displayDetails = "Memory save error: ${e.message}"
            )
        }
    }

    suspend fun scheduleReminder(title: String, delayMinutes: Long): ActionResult {
        return try {
            val scheduledTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000)
            val id = database.reminderDao().insert(
                ReminderEntity(
                    title = title,
                    scheduledTimeMillis = scheduledTime
                )
            )

            // Register system alarm
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
                putExtra("reminder_id", id)
                putExtra("reminder_title", title)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager?.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                scheduledTime,
                pendingIntent
            )

            ActionResult(
                isSuccess = true,
                spokenConfirmation = "$delayMinutes মিনিট পরে আপনাকে মনে করিয়ে দেব: $title",
                displayDetails = "Scheduled reminder for $delayMinutes mins: $title"
            )
        } catch (e: Exception) {
            ActionResult(
                isSuccess = false,
                spokenConfirmation = "রিমাইন্ডার সেট করতে সমস্যা হয়েছে।",
                displayDetails = "Reminder error: ${e.message}"
            )
        }
    }

    fun getDeviceInfo(type: String): ActionResult {
        return when (type) {
            "BATTERY" -> {
                val profile = batteryOptManager.getDeviceProfile()
                val statusText = if (profile.isCharging) "চার্জ হচ্ছে" else "চার্জে নেই"
                ActionResult(
                    isSuccess = true,
                    spokenConfirmation = "আপনার ব্যাটারি ${profile.batteryLevel} শতাংশ এবং এটি বর্তমানে $statusText। পাওয়ার সেভার: ${if (profile.isPowerSaveMode) "চালু" else "বন্ধ"}।",
                    displayDetails = "Battery: ${profile.batteryLevel}% ($statusText), Power Saver: ${profile.isPowerSaveMode}"
                )
            }
            "PERFORMANCE", "RAM" -> {
                val profile = batteryOptManager.getDeviceProfile()
                val ramMsg = "র‍্যামের ${profile.availableRamMb} মেগাবাইট ফাঁকা রয়েছে মোট ${profile.totalRamMb} মেগাবাইটের মধ্যে।"
                ActionResult(
                    isSuccess = true,
                    spokenConfirmation = ramMsg,
                    displayDetails = profile.profileSummary
                )
            }
            "STORAGE" -> {
                try {
                    val stat = StatFs(Environment.getDataDirectory().path)
                    val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
                    val gbAvailable = bytesAvailable / (1024 * 1024 * 1024)
                    ActionResult(
                        isSuccess = true,
                        spokenConfirmation = "আপনার ডিভাইসে প্রায় $gbAvailable গিগাবাইট ফাঁকা স্টোরেজ রয়েছে।",
                        displayDetails = "Available Storage: ~$gbAvailable GB"
                    )
                } catch (e: Exception) {
                    ActionResult(
                        isSuccess = false,
                        spokenConfirmation = "স্টোরেজ তথ্য পড়া যায়নি।",
                        displayDetails = "Storage query error: ${e.message}"
                    )
                }
            }
            "NETWORK" -> {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val activeNetwork = cm?.activeNetwork
                val capabilities = cm?.getNetworkCapabilities(activeNetwork)
                val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                val isCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
                val typeName = if (isWifi) "ওয়াই-ফাই" else if (isCellular) "মোবাইল ডাটা" else "সংযুক্ত"
                if (isConnected) {
                    ActionResult(
                        isSuccess = true,
                        spokenConfirmation = "আপনার ডিভাইসটি ইন্টারনেটে $typeName এর মাধ্যমে সংযুক্ত আছে।",
                        displayDetails = "Network: Connected via $typeName"
                    )
                } else {
                    ActionResult(
                        isSuccess = false,
                        spokenConfirmation = "বর্তমানে কোনো সক্রিয় ইন্টারনেট সংযোগ নেই।",
                        displayDetails = "Network: Offline"
                    )
                }
            }
            "TIME" -> {
                val sdf = SimpleDateFormat("h:mm a, d MMMM yyyy", Locale.getDefault())
                val now = sdf.format(Date())
                ActionResult(
                    isSuccess = true,
                    spokenConfirmation = "বর্তমানে সময় $now",
                    displayDetails = "Current Time: $now"
                )
            }
            else -> ActionResult(false, "তথ্য পাওয়া যায়নি।", "Unknown device info query")
        }
    }
}
