package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automation.SuperAgentState
import com.example.ui.NovaMainViewModel
import com.example.ui.theme.NovaBorder
import com.example.ui.theme.NovaCyanGlow
import com.example.ui.theme.NovaDarkNavy
import com.example.ui.theme.NovaElectricPurple
import com.example.ui.theme.NovaError
import com.example.ui.theme.NovaNearBlack
import com.example.ui.theme.NovaSuccess
import com.example.ui.theme.NovaSurface
import com.example.ui.theme.NovaSurfaceElevated
import com.example.ui.theme.NovaTextPrimary
import com.example.ui.theme.NovaTextSecondary
import com.example.ui.theme.NovaWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TaskPlannerScreen(
    viewModel: NovaMainViewModel,
    onNavigateBack: () -> Unit
) {
    val tasks by viewModel.tasksFlow.collectAsState()
    val superAgentStatus by viewModel.superAgentStatus.collectAsState()
    val planJson by viewModel.latestExecutionPlanJson.collectAsState()
    val dateFormatter = remember { SimpleDateFormat("h:mm a, d MMM", Locale.getDefault()) }
    var showJsonModal by remember { mutableStateOf(false) }

    val hardwareProfile = remember { viewModel.getHardwareProfile() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NovaNearBlack, NovaDarkNavy, NovaNearBlack)
                )
            )
            .padding(18.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("task_planner_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "OS Super-Agent Planner",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = NovaTextPrimary
                )
                Text(
                    text = "Autonomous multi-app chaining & battery optimizer",
                    style = MaterialTheme.typography.labelSmall,
                    color = NovaCyanGlow
                )
            }

            if (planJson.isNotBlank()) {
                OutlinedButton(
                    onClick = { showJsonModal = !showJsonModal },
                    modifier = Modifier.testTag("toggle_plan_json_btn"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NovaCyanGlow),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (showJsonModal) "Tasks" else "JSON", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Super-Agent Transition Banner (🔵 Listening ➔ 🟡 Thinking ➔ 🟢 Executing ➔ ✨ Done)
        val (statePillText, stateColor) = when (superAgentStatus.state) {
            SuperAgentState.IDLE -> Pair("Ready", NovaElectricPurple)
            SuperAgentState.LISTENING -> Pair("🔵 Listening...", NovaCyanGlow)
            SuperAgentState.THINKING -> Pair("🟡 Thinking & Planning...", NovaWarning)
            SuperAgentState.EXECUTING -> {
                val s = if (superAgentStatus.totalSteps > 0) " (${superAgentStatus.currentStepNumber}/${superAgentStatus.totalSteps})" else ""
                Pair("🟢 Executing Step$s", NovaSuccess)
            }
            SuperAgentState.DONE -> Pair("✨ Done", NovaCyanGlow)
            SuperAgentState.ERROR -> Pair("⚠️ Issue", NovaError)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NovaSurfaceElevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, stateColor.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(stateColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statePillText,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = stateColor
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = NovaTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${hardwareProfile.availableRamMb}MB Free",
                            style = MaterialTheme.typography.labelSmall,
                            color = NovaTextSecondary
                        )
                    }
                }

                if (superAgentStatus.activeStepDescription.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = superAgentStatus.activeStepDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaTextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Test Chain Presets
        Text(
            text = "Autonomous Multi-App Chain Presets:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = NovaTextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    viewModel.handleUserInput(
                        "YouTube theke video link copy kore WhatsApp-e Tanha ke pathao, tarpor Bluetooth off kore 7 tar alarm set koro"
                    )
                },
                modifier = Modifier.weight(1f).testTag("preset_compound_chain_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NovaSurface)
            ) {
                Text("Compound Chain", fontSize = 11.sp, color = NovaTextPrimary)
            }

            Button(
                onClick = {
                    viewModel.handleUserInput("ফ্ল্যাশলাইট অন করো তারপর সাউন্ড ভলিউম বাড়াও")
                },
                modifier = Modifier.weight(1f).testTag("preset_torch_volume_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NovaSurface)
            ) {
                Text("Torch & Sound", fontSize = 11.sp, color = NovaTextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (showJsonModal && planJson.isNotBlank()) {
            // Live JSON Execution Plan Viewer
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NovaSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NovaCyanGlow.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Standard Structured JSON Execution Plan:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = NovaCyanGlow
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        item {
                            Text(
                                text = planJson,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                color = NovaTextPrimary
                            )
                        }
                    }
                }
            }
        } else if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(NovaSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            tint = NovaTextSecondary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No automated chains executed yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = NovaTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Speak multi-step instructions joined with 'তারপর' or 'then'",
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaTextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    val isSuccess = task.status == "COMPLETED"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_item_${task.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = NovaSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NovaBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSuccess) NovaSuccess.copy(alpha = 0.15f) else NovaError.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                            contentDescription = null,
                                            tint = if (isSuccess) NovaSuccess else NovaError,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = task.status,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSuccess) NovaSuccess else NovaError
                                        )
                                    }
                                }

                                Text(
                                    text = dateFormatter.format(Date(task.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NovaTextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Command: \"${task.query}\"",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = NovaTextPrimary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Action: ${task.targetApp} (${task.intentType})",
                                style = MaterialTheme.typography.bodySmall,
                                color = NovaCyanGlow
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Result: ${task.resultMessage}",
                                style = MaterialTheme.typography.bodySmall,
                                color = NovaTextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
