package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.service.HandsFreeVoiceService
import com.example.service.NovaAccessibilityService
import com.example.service.OverlayAssistantService
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
import com.example.ui.theme.NovaSurfaceVariant
import com.example.ui.theme.NovaTextPrimary
import com.example.ui.theme.NovaTextSecondary

@Composable
fun HandsFreeCenterScreen(
    viewModel: NovaMainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val userPrefs by viewModel.userPreferences.collectAsState()

    var wakePhraseInput by remember(userPrefs.wakeWordPhrase) { mutableStateOf(userPrefs.wakeWordPhrase) }

    // Check system status
    val hasMicPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    val isAccessibilityActive by NovaAccessibilityService.isServiceActive.collectAsState()
    val isOverlayActive by OverlayAssistantService.isOverlayActive.collectAsState()
    val isSpeechAvailable = viewModel.speechManager.isRecognitionAvailable()
    val isTtsReady by viewModel.ttsManager.isInitialized.collectAsState()

    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val isNetworkOnline = cm?.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NovaNearBlack, NovaDarkNavy, NovaNearBlack)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("handsfree_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Hands-Free Control Center",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = NovaTextPrimary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Master Hands-Free Service Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (userPrefs.handsFreeEnabled) NovaElectricPurple.copy(alpha = 0.15f) else NovaSurface
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (userPrefs.handsFreeEnabled) NovaElectricPurple else NovaBorder
            )
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (userPrefs.handsFreeEnabled) NovaElectricPurple else NovaSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HeadsetMic,
                            contentDescription = null,
                            tint = if (userPrefs.handsFreeEnabled) Color.White else NovaTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Always-On Vision & Voice (সবসময় চালু)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NovaTextPrimary
                        )
                        Text(
                            text = if (userPrefs.handsFreeEnabled) "Active — Observes screen and listens over any app" else "একবার অন করলে সবসময় চলবে (বারবার অ্যাপে ঢুকতে হবে না)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (userPrefs.handsFreeEnabled) NovaCyanGlow else NovaTextSecondary
                        )
                    }
                }

                Switch(
                    checked = userPrefs.handsFreeEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.setHandsFreeEnabled(enabled)
                        val serviceIntent = Intent(context, HandsFreeVoiceService::class.java)
                        if (enabled && hasMicPermission) {
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
                        checkedTrackColor = NovaElectricPurple
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Wake Word Config Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = NovaSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, NovaBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Configurable Wake Phrase",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = NovaTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "When hands-free mode is active, speaking this phrase awakens NOVA AI without touching your phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NovaTextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = wakePhraseInput,
                        onValueChange = { wakePhraseInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wake_word_input_field"),
                        singleLine = true,
                        placeholder = { Text("e.g. Hey Nova, নোভা") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NovaCyanGlow,
                            unfocusedBorderColor = NovaBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { viewModel.setWakeWordPhrase(wakePhraseInput.trim()) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NovaElectricPurple)
                    ) {
                        Text("Save")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // System Capabilities & Verification Grid
        Text(
            text = "System Capability Status",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = NovaCyanGlow
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = NovaSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, NovaBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                CapabilityStatusRow(
                    name = "Microphone Permission",
                    icon = Icons.Default.Mic,
                    isActive = hasMicPermission,
                    activeDesc = "Granted",
                    inactiveDesc = "Denied / Not granted"
                )
                SettingsDivider()
                CapabilityStatusRow(
                    name = "Speech Recognition Engine",
                    icon = Icons.Default.HeadsetMic,
                    isActive = isSpeechAvailable,
                    activeDesc = "Android SpeechRecognizer ready",
                    inactiveDesc = "Service not found"
                )
                SettingsDivider()
                CapabilityStatusRow(
                    name = "Text-to-Speech Engine",
                    icon = Icons.Default.VolumeUp,
                    isActive = isTtsReady,
                    activeDesc = "TTS Engine active",
                    inactiveDesc = "Initializing..."
                )
                SettingsDivider()
                CapabilityStatusRow(
                    name = "Accessibility Automation",
                    icon = Icons.Default.AccessibilityNew,
                    isActive = isAccessibilityActive,
                    activeDesc = "Service connected",
                    inactiveDesc = "Disabled in Android Settings"
                )
                SettingsDivider()
                CapabilityStatusRow(
                    name = "Floating Overlay Window",
                    icon = Icons.Default.Layers,
                    isActive = isOverlayActive,
                    activeDesc = "Overlay active",
                    inactiveDesc = "Disabled / Not running"
                )
                SettingsDivider()
                CapabilityStatusRow(
                    name = "Internet Connectivity",
                    icon = Icons.Default.Wifi,
                    isActive = isNetworkOnline,
                    activeDesc = "Online (Gemini API available)",
                    inactiveDesc = "Offline (Local mode only)"
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Emergency Stop Everything Button
        Button(
            onClick = {
                viewModel.stopAll()
                val serviceIntent = Intent(context, HandsFreeVoiceService::class.java).apply {
                    action = HandsFreeVoiceService.ACTION_STOP_HANDS_FREE
                }
                context.startService(serviceIntent)
                viewModel.setHandsFreeEnabled(false)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("handsfree_emergency_stop_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NovaError)
        ) {
            Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Emergency Stop All (থামো)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun CapabilityStatusRow(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    activeDesc: String,
    inactiveDesc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) NovaCyanGlow else NovaTextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = NovaTextPrimary
                )
                Text(
                    text = if (isActive) activeDesc else inactiveDesc,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) NovaSuccess else NovaError
                )
            }
        }

        Icon(
            imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Close,
            contentDescription = null,
            tint = if (isActive) NovaSuccess else NovaError,
            modifier = Modifier.size(20.dp)
        )
    }
}
