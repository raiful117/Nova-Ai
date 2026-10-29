package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.automation.ExecutionPlan
import com.example.automation.SuperAgentEngine
import com.example.util.BatteryOptimizationManager
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.first

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NOVA AI", appName)
    }

    @Test
    fun `test compound multi-step execution plan creation and json format`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = SuperAgentEngine(context)

        val compoundCommand = "YouTube theke video link copy kore WhatsApp-e Tanha ke pathao, tarpor Bluetooth off kore 7 tar alarm set koro"
        val plan = engine.createExecutionPlan(compoundCommand)

        assertEquals(compoundCommand, plan.userCommand)
        assertEquals("Multi-App Chain Automation", plan.taskCategory)
        assertTrue("Execution steps should be >= 2", plan.executionSteps.size >= 2)

        // Verify JSON serialization follows the exact requested schema
        val jsonStr = plan.toJsonString()
        assertNotNull(jsonStr)

        val jsonObj = JSONObject(jsonStr)
        assertEquals(compoundCommand, jsonObj.getString("user_command"))
        assertEquals("Multi-App Chain Automation", jsonObj.getString("task_category"))
        assertTrue(jsonObj.has("execution_steps"))
        assertTrue(jsonObj.has("feedback_message"))

        val stepsArray = jsonObj.getJSONArray("execution_steps")
        assertTrue(stepsArray.length() >= 2)

        // Verify round-trip parsing
        val parsedPlan = ExecutionPlan.fromJsonObject(jsonObj)
        assertEquals(plan.userCommand, parsedPlan.userCommand)
        assertEquals(plan.executionSteps.size, parsedPlan.executionSteps.size)
    }

    @Test
    fun `test battery optimization manager hardware profile`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = BatteryOptimizationManager(context)
        val profile = manager.getDeviceProfile()

        assertTrue(profile.batteryLevel in 0..100)
        assertTrue(profile.totalRamMb > 0)
        assertTrue(profile.recommendedLoopDelayMs >= 500L)
        assertNotNull(profile.profileSummary)
    }

    @Test
    fun `test gemini model preference persistence`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.preferences.NovaPreferencesRepository(context)

        // Default model should be Gemini 2.5 Flash
        val initialPrefs = repo.userPreferencesFlow.first()
        assertEquals(com.example.data.network.gemini.GeminiModelCatalog.DEFAULT_MODEL_ID, initialPrefs.selectedGeminiModel)

        // Switch to Gemini 2.5 Pro
        repo.updateSelectedGeminiModel("gemini-2.5-pro")
        val updatedPrefs = repo.userPreferencesFlow.first()
        assertEquals("gemini-2.5-pro", updatedPrefs.selectedGeminiModel)
    }
}
