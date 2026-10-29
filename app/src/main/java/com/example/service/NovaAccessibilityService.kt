package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

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
            clickableElements = clickables.distinct().take(20)
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

    /**
     * Dynamic UI node targeting: searches for text, contentDescription, or viewId
     * with parent fallback for clickable containers.
     */
    fun findAndClickElement(textQuery: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val cleanQuery = textQuery.trim().lowercase(Locale.ROOT)

        // 1. Direct search by exact or partial text
        val nodes = rootNode.findAccessibilityNodeInfosByText(textQuery)
        for (node in nodes) {
            if (clickNodeOrParent(node)) return true
        }

        // 2. Deep recursive search for content description or partial match
        return recursiveFindAndClick(rootNode, cleanQuery)
    }

    private fun recursiveFindAndClick(node: AccessibilityNodeInfo?, query: String): Boolean {
        if (node == null) return false
        val nodeText = node.text?.toString()?.lowercase(Locale.ROOT) ?: ""
        val nodeDesc = node.contentDescription?.toString()?.lowercase(Locale.ROOT) ?: ""

        if (nodeText.contains(query) || nodeDesc.contains(query)) {
            if (clickNodeOrParent(node)) return true
        }

        for (i in 0 until node.childCount) {
            if (recursiveFindAndClick(node.getChild(i), query)) {
                return true
            }
        }
        return false
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true
        }
        var parent = node.parent
        var depth = 0
        while (parent != null && depth < 4) {
            if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            parent = parent.parent
            depth++
        }
        return false
    }

    /**
     * Autonomous text insertion into editable text fields.
     */
    fun enterTextIntoField(targetFieldHint: String?, textToEnter: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val editableNode = findEditableNode(rootNode, targetFieldHint?.lowercase(Locale.ROOT))
        if (editableNode != null) {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToEnter)
            val success = editableNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            if (success) {
                editableNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                return true
            }
        }
        return false
    }

    private fun findEditableNode(node: AccessibilityNodeInfo?, hint: String?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable) {
            if (hint.isNullOrBlank()) return node
            val text = node.text?.toString()?.lowercase(Locale.ROOT) ?: ""
            val desc = node.contentDescription?.toString()?.lowercase(Locale.ROOT) ?: ""
            if (text.contains(hint) || desc.contains(hint)) return node
        }

        for (i in 0 until node.childCount) {
            val res = findEditableNode(node.getChild(i), hint)
            if (res != null) return res
        }
        return null
    }

    /**
     * Self-healing pop-up and permission dialog dismissal
     */
    fun dismissPopups(): Boolean {
        val dismissLabels = listOf(
            "cancel", "close", "not now", "skip", "dismiss", "ok", "got it",
            "বাতিল", "বন্ধ", "পরে", "ঠিক আছে", "এখন নয়"
        )
        for (label in dismissLabels) {
            if (findAndClickElement(label)) {
                Log.d("NovaAccessibility", "Auto-dismissed dialog with label: $label")
                return true
            }
        }
        return false
    }

    /**
     * Simulates scrolling down/forward
     */
    fun scrollForward(): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        return performScroll(rootNode, AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }

    /**
     * Simulates scrolling up/backward
     */
    fun scrollBackward(): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        return performScroll(rootNode, AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    }

    private fun performScroll(node: AccessibilityNodeInfo?, action: Int): Boolean {
        if (node == null) return false
        if (node.isScrollable && node.performAction(action)) {
            return true
        }
        for (i in 0 until node.childCount) {
            if (performScroll(node.getChild(i), action)) return true
        }
        return false
    }

    /**
     * Copies active screen text to system clipboard
     */
    fun copyScreenTextToClipboard(): String {
        val text = captureScreenVision().fullText
        if (text.isNotBlank()) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("NOVA Extracted Text", text)
            clipboard?.setPrimaryClip(clip)
        }
        return text
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
