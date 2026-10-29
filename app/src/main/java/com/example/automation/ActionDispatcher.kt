package com.example.automation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.provider.Settings
import com.example.data.local.MemoryEntity
import com.example.data.local.NovaDatabase
import com.example.data.local.ReminderEntity
import com.example.receiver.ReminderAlarmReceiver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActionDispatcher(private val context: Context) {

    private val database = NovaDatabase.getDatabase(context)

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
                val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                val batteryStatus = context.registerReceiver(null, batteryFilter)
                val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val pct = if (level >= 0 && scale > 0) (level * 100) / scale else -1
                val isCharging = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING)
                val statusText = if (isCharging) "চার্জ হচ্ছে" else "চার্জে নেই"
                ActionResult(
                    isSuccess = true,
                    spokenConfirmation = "আপনার ব্যাটারি $pct শতাংশ এবং এটি বর্তমানে $statusText।",
                    displayDetails = "Battery: $pct% ($statusText)"
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
