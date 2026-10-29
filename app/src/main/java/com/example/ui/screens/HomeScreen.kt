package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import android.content.Intent
import android.os.Build
import com.example.service.HandsFreeVoiceService
import com.example.service.OverlayAssistantService
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.NovaMainViewModel
import com.example.ui.OrbState
import com.example.ui.components.NovaVoiceOrb
import com.example.ui.theme.NovaBorder
import com.example.ui.theme.NovaCyanGlow
import com.example.ui.theme.NovaDarkNavy
import com.example.ui.theme.NovaElectricPurple
import com.example.ui.theme.NovaError
import com.example.ui.theme.NovaNearBlack
import com.example.ui.theme.NovaSpeakingWaveform
import com.example.ui.theme.NovaSuccess
import com.example.ui.theme.NovaSurface
import com.example.ui.theme.NovaSurfaceElevated
import com.example.ui.theme.NovaSurfaceVariant
import com.example.ui.theme.NovaTextPrimary
import com.example.ui.theme.NovaTextSecondary
import com.example.ui.theme.NovaVividBlue

@Composable
fun HomeScreen(
    viewModel: NovaMainViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHandsFreeCenter: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val orbState by viewModel.orbState.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val currentPrompt by viewModel.currentPrompt.collectAsState()
    val latestResponse by viewModel.latestResponse.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val userPrefs by viewModel.userPreferences.collectAsState()
    val liveRecognizedText by viewModel.speechManager.recognizedText.collectAsState()
    val rmsDb by viewModel.speechManager.rmsDb.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        }
    }

    val quickPrompts = remember {
        listOf(
            "Tell me a story",
            "ইউটিউব খোলো",
            "ব্যাটারি কত পারসেন্ট",
            "আজকের আবহাওয়া বলো",
            "Open Chrome",
            "15% of 850 কত",
            "পাঁচ মিনিট পরে মনে করিয়ে দিও",
            "স্ক্রিনে কী আছে বলো"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NovaNearBlack, NovaDarkNavy, NovaNearBlack)
                )
            )
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("home_header_branding")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_nova_logo),
                    contentDescription = "NOVA AI Logo",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "NOVA AI",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = NovaTextPrimary
                    )
                    Text(
                        text = if (userPrefs.language == "bn") "ব্যক্তিগত সহকারী" else "Personal Voice Assistant",
                        style = MaterialTheme.typography.labelSmall,
                        color = NovaCyanGlow
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Hands-free pill indicator
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (userPrefs.handsFreeEnabled) NovaSuccess.copy(alpha = 0.2f) else NovaSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (userPrefs.handsFreeEnabled) NovaSuccess.copy(alpha = 0.6f) else NovaBorder
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToHandsFreeCenter() }
                        .testTag("hands_free_status_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HeadsetMic,
                            contentDescription = "Hands-free status",
                            tint = if (userPrefs.handsFreeEnabled) NovaSuccess else NovaTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (userPrefs.handsFreeEnabled) "Hands-Free ON" else "Hands-Free",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (userPrefs.handsFreeEnabled) NovaSuccess else NovaTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.testTag("home_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = NovaTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Scrollable content area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Master Always-On Vision & Voice Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (userPrefs.handsFreeEnabled) NovaElectricPurple.copy(alpha = 0.18f) else NovaSurface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (userPrefs.handsFreeEnabled) NovaCyanGlow.copy(alpha = 0.7f) else NovaBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (userPrefs.handsFreeEnabled) NovaCyanGlow.copy(alpha = 0.2f) else NovaSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = if (userPrefs.handsFreeEnabled) NovaCyanGlow else NovaTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Always-On Vision & Voice",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = NovaTextPrimary
                            )
                            Text(
                                text = if (userPrefs.handsFreeEnabled) "Always running • Observes screen over any app" else "একবার অন করলে সবসময় চলবে (বারবার অ্যাপে ঢুকতে হবে না)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (userPrefs.handsFreeEnabled) NovaCyanGlow else NovaTextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = userPrefs.handsFreeEnabled,
                        onCheckedChange = { enable ->
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!hasPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                return@Switch
                            }
                            viewModel.setHandsFreeEnabled(enable)
                            val serviceIntent = Intent(context, HandsFreeVoiceService::class.java)
                            if (enable) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(serviceIntent)
                                } else {
                                    context.startService(serviceIntent)
                                }
                                if (OverlayAssistantService.canDrawOverlays(context)) {
                                    context.startService(Intent(context, OverlayAssistantService::class.java))
                                }
                            } else {
                                serviceIntent.action = HandsFreeVoiceService.ACTION_STOP_HANDS_FREE
                                context.startService(serviceIntent)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NovaCyanGlow
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Central Glowing AI Voice Orb
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                NovaVoiceOrb(
                    orbState = orbState,
                    rmsDb = rmsDb,
                    size = 230.dp,
                    onClick = {
                        if (orbState == OrbState.LISTENING) {
                            viewModel.stopListening()
                        } else if (orbState == OrbState.SPEAKING) {
                            viewModel.stopAll()
                        } else {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                viewModel.startListening()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Status and instruction
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = when (orbState) {
                    OrbState.LISTENING -> NovaCyanGlow
                    OrbState.PROCESSING -> NovaElectricPurple
                    OrbState.SPEAKING -> NovaSpeakingWaveform
                    OrbState.ERROR -> NovaError
                    OrbState.IDLE -> NovaTextPrimary
                },
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = when (orbState) {
                    OrbState.IDLE -> "Tap the mic and speak in Bengali or English"
                    OrbState.LISTENING -> "Listening to your voice..."
                    OrbState.PROCESSING -> "NOVA is processing your request..."
                    OrbState.SPEAKING -> "Tap orb or Stop button to interrupt speech"
                    OrbState.ERROR -> "Please check your network or API key"
                },
                style = MaterialTheme.typography.bodySmall,
                color = NovaTextSecondary,
                textAlign = TextAlign.Center
            )

            // Live speech recognition display during active listening
            if (orbState == OrbState.LISTENING && liveRecognizedText.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NovaCyanGlow.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NovaCyanGlow.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"$liveRecognizedText\"",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Error display card
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NovaError.copy(alpha = 0.12f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NovaError.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.stopAll() }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = NovaError)
                        }
                    }
                }
            }

            // Response Preview Card
            if (latestResponse.isNotBlank()) {
                Spacer(modifier = Modifier.height(18.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("latest_response_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NovaSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NovaBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (currentPrompt.isNotBlank()) {
                            Text(
                                text = "You: $currentPrompt",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = NovaCyanGlow
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text(
                            text = latestResponse,
                            style = MaterialTheme.typography.bodyMedium,
                            color = NovaTextPrimary,
                            lineHeight = 22.sp,
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(latestResponse))
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy text",
                                    tint = NovaTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = {
                                viewModel.handleUserInput(currentPrompt)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry",
                                    tint = NovaTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Prompt Chips
            Text(
                text = "Quick Voice Prompts",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = NovaTextSecondary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPrompts.forEach { prompt ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = NovaSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NovaBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { viewModel.handleUserInput(prompt) }
                            .testTag("quick_prompt_$prompt")
                    ) {
                        Text(
                            text = prompt,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = NovaTextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Bottom Controls Row: Chat Mode, Main Mic FAB, Stop Speaking / Cancel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text Chat Mode Button
            IconButton(
                onClick = onNavigateToChat,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(NovaSurfaceVariant)
                    .testTag("switch_to_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Keyboard / Chat Mode",
                    tint = NovaCyanGlow,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Primary Large Microphone Button
            FloatingActionButton(
                onClick = {
                    if (orbState == OrbState.LISTENING) {
                        viewModel.stopListening()
                    } else if (orbState == OrbState.SPEAKING) {
                        viewModel.stopAll()
                    } else {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            viewModel.startListening()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                modifier = Modifier
                    .size(72.dp)
                    .testTag("primary_mic_fab"),
                shape = CircleShape,
                containerColor = if (orbState == OrbState.LISTENING) NovaCyanGlow else NovaElectricPurple,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = when (orbState) {
                        OrbState.LISTENING -> Icons.Default.Stop
                        OrbState.SPEAKING -> Icons.Default.VolumeUp
                        else -> Icons.Default.Mic
                    },
                    contentDescription = "Microphone action",
                    modifier = Modifier.size(36.dp),
                    tint = if (orbState == OrbState.LISTENING) NovaNearBlack else Color.White
                )
            }

            // Interrupt / Emergency Stop Button
            IconButton(
                onClick = { viewModel.stopAll() },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(NovaSurfaceVariant)
                    .testTag("emergency_stop_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop All",
                    tint = NovaError,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
