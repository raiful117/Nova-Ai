package com.example.util

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log

data class DeviceHardwareProfile(
    val batteryLevel: Int,
    val isCharging: Boolean,
    val isPowerSaveMode: Boolean,
    val availableRamMb: Long,
    val totalRamMb: Long,
    val isLowMemoryDevice: Boolean,
    val recommendedLoopDelayMs: Long,
    val profileSummary: String
)

class BatteryOptimizationManager(private val context: Context) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    private var wakeLock: PowerManager.WakeLock? = null

    fun getDeviceProfile(): DeviceHardwareProfile {
        // 1. Battery stats
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, batteryFilter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 100
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else 100
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        // 2. Power Saver
        val isPowerSave = powerManager?.isPowerSaveMode == true

        // 3. RAM info
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        val rawAvailMb = memoryInfo.availMem / (1024 * 1024)
        val rawTotalMb = memoryInfo.totalMem / (1024 * 1024)
        val runtimeMb = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        val totalMb = if (rawTotalMb > 0) rawTotalMb else if (runtimeMb > 0) runtimeMb else 2048L
        val availMb = if (rawAvailMb > 0) rawAvailMb else (totalMb / 2)
        val isLowRam = memoryInfo.lowMemory || totalMb < 3500 // budget 2-3GB device

        // 4. Adaptive loop interval calculation
        val recommendedDelayMs = when {
            isPowerSave || (batteryPct < 15 && !isCharging) -> 2500L
            batteryPct < 25 && !isCharging -> 1600L
            isLowRam -> 1200L
            isCharging -> 600L
            else -> 800L
        }

        val summary = buildString {
            append("Battery: $batteryPct% (")
            append(if (isCharging) "Charging" else "Discharging")
            append("), Power Saver: ${if (isPowerSave) "ON" else "OFF"}, ")
            append("RAM: $availMb MB free / $totalMb MB total ")
            if (isLowRam) append("[Optimized Mode: Low-End Hardware]")
        }

        return DeviceHardwareProfile(
            batteryLevel = batteryPct,
            isCharging = isCharging,
            isPowerSaveMode = isPowerSave,
            availableRamMb = availMb,
            totalRamMb = totalMb,
            isLowMemoryDevice = isLowRam,
            recommendedLoopDelayMs = recommendedDelayMs,
            profileSummary = summary
        )
    }

    /**
     * Safely acquire a short-lived wake lock for critical multi-step tasks.
     * Always guarded with a timeout so it never drains battery indefinitely.
     */
    @Synchronized
    fun runWithWakeLock(taskTag: String, maxTimeoutMs: Long = 10000L, block: () -> Unit) {
        try {
            if (wakeLock == null) {
                wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "NovaSuperAgent:$taskTag"
                )?.apply {
                    setReferenceCounted(false)
                }
            }
            wakeLock?.acquire(maxTimeoutMs)
            block()
        } catch (e: Exception) {
            Log.e("BatteryOpt", "WakeLock error for $taskTag: ${e.message}")
        } finally {
            releaseWakeLockSafely()
        }
    }

    @Synchronized
    fun releaseWakeLockSafely() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.w("BatteryOpt", "Error releasing wake lock: ${e.message}")
        }
    }
}
