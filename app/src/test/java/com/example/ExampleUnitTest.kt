package com.example

import com.example.automation.CommandIntent
import com.example.automation.CommandParser
import org.junit.Assert.assertEquals
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
}
