package com.example

import com.example.automation.CommandIntent
import com.example.automation.CommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testEmergencyStopCommands() {
        val stopBn = CommandParser.parse("থামো")
        assertTrue(stopBn is CommandIntent.StopAll)

        val stopEn = CommandParser.parse("stop everything")
        assertTrue(stopEn is CommandIntent.StopAll)

        val cancel = CommandParser.parse("cancel")
        assertTrue(cancel is CommandIntent.StopAll)
    }

    @Test
    fun testMathCalculationIntent() {
        val math1 = CommandParser.parse("500 + 20")
        assertTrue(math1 is CommandIntent.Calculator)
        assertEquals("520.0", (math1 as CommandIntent.Calculator).evaluatedResult)

        val mathPercent = CommandParser.parse("15 percent of 500")
        assertTrue(mathPercent is CommandIntent.Calculator)
        assertTrue((mathPercent as CommandIntent.Calculator).evaluatedResult.startsWith("75.0"))
    }

    @Test
    fun testAppLaunchCommands() {
        val ytBn = CommandParser.parse("ইউটিউব খোলো")
        assertTrue(ytBn is CommandIntent.AppLaunch || ytBn is CommandIntent.YouTubeSearch)

        val chrome = CommandParser.parse("open chrome")
        assertTrue(chrome is CommandIntent.AppLaunch)
        assertEquals("Google Chrome", (chrome as CommandIntent.AppLaunch).appName)
    }

    @Test
    fun testSystemSettingsCommands() {
        val wifi = CommandParser.parse("ওয়াইফাই সেটিংস")
        assertTrue(wifi is CommandIntent.SystemSetting)

        val bluetooth = CommandParser.parse("bluetooth settings")
        assertTrue(bluetooth is CommandIntent.SystemSetting)
    }

    @Test
    fun testReminderCommands() {
        val remBn = CommandParser.parse("৫ মিনিট পরে মনে করিয়ে দিও মিটিং")
        assertTrue(remBn is CommandIntent.Reminder)
        assertEquals(5L, (remBn as CommandIntent.Reminder).delayMinutes)

        val remEn = CommandParser.parse("remind me in 10 minutes to call doctor")
        assertTrue(remEn is CommandIntent.Reminder)
        assertEquals(10L, (remEn as CommandIntent.Reminder).delayMinutes)
    }

    @Test
    fun testMemoryCommands() {
        val memBn = CommandParser.parse("মনে রাখো আমি বাংলায় কথা বলতে পছন্দ করি")
        assertTrue(memBn is CommandIntent.SaveMemory)
        assertEquals("আমি বাংলায় কথা বলতে পছন্দ করি", (memBn as CommandIntent.SaveMemory).content)
    }

    @Test
    fun testGeminiModelCatalog() {
        val models = com.example.data.network.gemini.GeminiModelCatalog.AVAILABLE_MODELS
        assertTrue(models.size >= 6)

        // Verify key flagship & free tier models are present
        val modelIds = models.map { it.id }
        assertTrue(modelIds.contains("gemini-3.8-flash"))
        assertTrue(modelIds.contains("gemini-2.5-flash"))
        assertTrue(modelIds.contains("gemini-2.5-pro"))
        assertTrue(modelIds.contains("gemini-2.0-flash"))
        assertTrue(modelIds.contains("gemini-2.0-flash-lite"))
        assertTrue(modelIds.contains("gemini-2.0-pro-exp-02-05"))

        // Verify all old 1.5 models are completely removed
        assertFalse(modelIds.contains("gemini-1.5-flash"))
        assertFalse(modelIds.contains("gemini-1.5-pro"))
        assertFalse(modelIds.contains("gemini-1.5-flash-8b"))

        // Verify default model is gemini-3.8-flash
        val defaultModel = com.example.data.network.gemini.GeminiModelCatalog.findModel(
            com.example.data.network.gemini.GeminiModelCatalog.DEFAULT_MODEL_ID
        )
        assertEquals("gemini-3.8-flash", defaultModel.id)
        assertEquals("Gemini 3.8 Flash", defaultModel.displayName)
        assertTrue(defaultModel.tag.contains("Newest Flagship"))

        // Verify custom model fallback
        val customModel = com.example.data.network.gemini.GeminiModelCatalog.findModel("gemini-custom-finetuned")
        assertEquals("gemini-custom-finetuned", customModel.id)
        assertEquals("Custom", customModel.tag)
    }
}
