package com.example.automation

import org.json.JSONArray
import org.json.JSONObject

data class ExecutionStep(
    val stepId: Int,
    val action: String,
    val target: String,
    val targetField: String? = null,
    val query: String? = null,
    val setting: String? = null,
    val state: String? = null,
    var status: StepStatus = StepStatus.PENDING,
    var statusMessage: String? = null
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("step_id", stepId)
        json.put("action", action)
        json.put("target", target)
        if (!targetField.isNullOrBlank()) json.put("target_field", targetField)
        if (!query.isNullOrBlank()) json.put("query", query)
        if (!setting.isNullOrBlank()) json.put("setting", setting)
        if (!state.isNullOrBlank()) json.put("state", state)
        json.put("status", status.name)
        return json
    }
}

enum class StepStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    SKIPPED
}

data class ExecutionPlan(
    val userCommand: String,
    val taskCategory: String,
    val executionSteps: List<ExecutionStep>,
    val feedbackMessage: String
) {
    fun toJsonString(indent: Int = 2): String {
        val json = JSONObject()
        json.put("user_command", userCommand)
        json.put("task_category", taskCategory)

        val stepsArray = JSONArray()
        for (step in executionSteps) {
            stepsArray.put(step.toJsonObject())
        }
        json.put("execution_steps", stepsArray)
        json.put("feedback_message", feedbackMessage)
        return json.toString(indent)
    }

    companion object {
        fun fromJsonObject(json: JSONObject): ExecutionPlan {
            val userCommand = json.optString("user_command", "")
            val taskCategory = json.optString("task_category", "Automation")
            val feedback = json.optString("feedback_message", "")

            val steps = mutableListOf<ExecutionStep>()
            val stepsArray = json.optJSONArray("execution_steps") ?: JSONArray()
            for (i in 0 until stepsArray.length()) {
                val stepObj = stepsArray.optJSONObject(i) ?: continue
                steps.add(
                    ExecutionStep(
                        stepId = stepObj.optInt("step_id", i + 1),
                        action = stepObj.optString("action", ""),
                        target = stepObj.optString("target", ""),
                        targetField = stepObj.optString("target_field").takeIf { it.isNotBlank() },
                        query = stepObj.optString("query").takeIf { it.isNotBlank() },
                        setting = stepObj.optString("setting").takeIf { it.isNotBlank() },
                        state = stepObj.optString("state").takeIf { it.isNotBlank() }
                    )
                )
            }

            return ExecutionPlan(
                userCommand = userCommand,
                taskCategory = taskCategory,
                executionSteps = steps,
                feedbackMessage = feedback
            )
        }
    }
}

enum class SuperAgentState(val label: String) {
    IDLE("Ready"),
    LISTENING("🔵 Listening"),
    THINKING("🟡 Thinking"),
    EXECUTING("🟢 Executing"),
    DONE("✨ Done"),
    ERROR("⚠️ Issue")
}

data class SuperAgentStatus(
    val state: SuperAgentState,
    val currentStepNumber: Int = 0,
    val totalSteps: Int = 0,
    val activeStepDescription: String = "",
    val feedbackMessage: String = "",
    val executionPlanJson: String = ""
)
