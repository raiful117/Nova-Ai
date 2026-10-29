package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.pm.ApplicationInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScreenVisionData(
    val appName: String,
    val packageName: String,
    val fullText: String,
    val clickableElements: List<String>,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toPromptContext(): String {
        return buildString {
            appendLine("[ACTIVE PHONE SCREEN VISION]")
            appendLine("Current App: $appName ($packageName)")
            if (clickableElements.isNotEmpty()) {
                appendLine("Interactive Elements / Buttons: ${clickableElements.take(12).joinToString(", ") { "[$it]" }}")
            }
            appendLine("Visible Screen Content:")
            appendLine(fullText.take(1500).ifBlank { "(No text nodes visible or screen is on home launcher)" })
        }
    }
}

class NovaAccessibilityService : AccessibilityService() {

    companion object {
        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private val _currentScreenVision = MutableStateFlow<ScreenVisionData?>(null)
        val currentScreenVision: StateFlow<ScreenVisionData?> = _currentScreenVision.asStateFlow()

        private val _lastCapturedScreenText = MutableStateFlow("")
        val lastCapturedScreenText: StateFlow<String> = _lastCapturedScreenText.asStateFlow()

        private var instance: NovaAccessibilityService? = null

        fun getActiveService(): NovaAccessibilityService? = instance
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true
        captureScreenVision()
        Log.d("NovaAccessibility", "Accessibility Screen Vision service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
        ) {
            captureScreenVision()
        }
    }

    fun captureScreenVision(): ScreenVisionData {
        val rootNode = rootInActiveWindow
        var packageName = rootNode?.packageName?.toString() ?: ""
        var appName = "Unknown App"

        if (packageName.isNotBlank()) {
            try {
                val pm = packageManager
                val appInfo: ApplicationInfo = pm.getApplicationInfo(packageName, 0)
                appName = pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                appName = packageName
            }
        }

        val textBuilder = StringBuilder()
        val clickables = mutableListOf<String>()

        if (rootNode != null) {
            traverseNodes(rootNode, textBuilder, clickables)
        }

        val fullText = textBuilder.toString().trim()
        val vision = ScreenVisionData(
            appName = appName,
            packageName = packageName,
            fullText = fullText,
            clickableElements = clickables.distinct().take(15)
        )

        _currentScreenVision.value = vision
        _lastCapturedScreenText.value = fullText
        return vision
    }

    fun captureVisibleScreenText(): String {
        return captureScreenVision().fullText
    }

    private fun traverseNodes(
        node: AccessibilityNodeInfo?,
        builder: StringBuilder,
        clickables: MutableList<String>
    ) {
        if (node == null) return

        // Privacy safeguard: Never inspect password fields or sensitive secure inputs
        if (node.isPassword) return

        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()

        val label = if (!text.isNullOrBlank()) text else desc
        if (!label.isNullOrBlank()) {
            builder.append(label).append("\n")
            if (node.isClickable) {
                clickables.add(label)
            }
        }

        for (i in 0 until node.childCount) {
            traverseNodes(node.getChild(i), builder, clickables)
        }
    }

    fun findAndClickElement(textQuery: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val nodes = rootNode.findAccessibilityNodeInfosByText(textQuery)
        for (node in nodes) {
            if (node.isClickable) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                parent = parent.parent
            }
        }
        return false
    }

    fun navigateBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun navigateHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun openNotifications(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    fun openRecentApps(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    override fun onInterrupt() {
        Log.w("NovaAccessibility", "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceActive.value = false
    }
}
