package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.NovaMainViewModel
import com.example.ui.theme.NovaBorder
import com.example.ui.theme.NovaCyanGlow
import com.example.ui.theme.NovaDarkNavy
import com.example.ui.theme.NovaElectricPurple
import com.example.ui.theme.NovaNearBlack
import com.example.ui.theme.NovaSurface
import com.example.ui.theme.NovaSurfaceVariant
import com.example.ui.theme.NovaTextPrimary
import com.example.ui.theme.NovaTextSecondary

@Composable
fun SettingsScreen(
    viewModel: NovaMainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToApi: () -> Unit,
    onNavigateToHandsFree: () -> Unit,
    onNavigateToOverlay: () -> Unit,
    onNavigateToAccessibility: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToPrivacyOffline: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val userPrefs by viewModel.userPreferences.collectAsState()

    val currentLangLabel = when (userPrefs.language) {
        "bn" -> "বাংলা (Bengali)"
        "hi" -> "हिन्दी (Hindi)"
        "en" -> "English"
        "ar" -> "العربية (Arabic)"
        "es" -> "Español"
        else -> userPrefs.language
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NovaNearBlack, NovaDarkNavy, NovaNearBlack)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = NovaTextPrimary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Profile / Assistant Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NovaSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, NovaBorder)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_nova_logo),
                    contentDescription = "NOVA Profile",
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "NOVA AI",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NovaTextPrimary
                    )
                    Text(
                        text = "Your Personal Voice Assistant",
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaCyanGlow
                    )
                    Text(
                        text = "Powered by Google Gemini 2.5 Flash",
                        style = MaterialTheme.typography.labelSmall,
                        color = NovaTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Voice & Language Section
        Text(
            text = "Voice & Language",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = NovaCyanGlow
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = NovaSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, NovaBorder)
        ) {
            Column {
                SettingsRow(
                    icon = Icons.Default.Translate,
                    title = "Preferred Language",
                    subtitle = currentLangLabel,
                    onClick = onNavigateToLanguage,
                    testTag = "settings_language_row"
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.VolumeUp,
                    title = "Speech Speed & Voice",
                    subtitle = "${userPrefs.speechRate}x rate • ${if (userPrefs.selectedVoiceName.isNotBlank()) userPrefs.selectedVoiceName else "System Default"}",
                    onClick = onNavigateToVoice,
                    testTag = "settings_voice_row"
                )
                SettingsDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Spoken Audio Replies",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = NovaTextPrimary
                        )
                        Text(
                            text = "Hear assistant speak answers aloud via Text-to-Speech",
                            style = MaterialTheme.typography.bodySmall,
                            color = NovaTextSecondary
                        )
                    }
                    Switch(
                        checked = userPrefs.speechPlaybackEnabled,
                        onCheckedChange = { viewModel.setSpeechPlaybackEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NovaElectricPurple
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // AI & Hands-Free Automation Section
        Text(
            text = "AI & Phone Automation",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = NovaCyanGlow
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = NovaSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, NovaBorder)
        ) {
            Column {
                SettingsRow(
                    icon = Icons.Default.VpnKey,
                    title = "Gemini API Configuration",
                    subtitle = if (userPrefs.apiKey.isNotBlank()) "Configured (${userPrefs.apiKey.take(4)}...)" else "Not configured — Tap to add",
                    onClick = onNavigateToApi,
                    testTag = "settings_api_row"
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.HeadsetMic,
                    title = "Hands-Free Control Center",
                    subtitle = if (userPrefs.handsFreeEnabled) "Active (${userPrefs.wakeWordPhrase})" else "Wake-word & background listening",
                    onClick = onNavigateToHandsFree,
                    testTag = "settings_handsfree_row"
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.Layers,
                    title = "Floating Assistant Overlay",
                    subtitle = if (userPrefs.overlayEnabled) "Enabled (Draws over other apps)" else "Quick-access floating orb",
                    onClick = onNavigateToOverlay,
                    testTag = "settings_overlay_row"
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.AccessibilityNew,
                    title = "Accessibility UI Automation",
                    subtitle = "Automate apps & read accessible on-screen text",
                    onClick = onNavigateToAccessibility,
                    testTag = "settings_accessibility_row"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Memory & Productivity Section
        Text(
            text = "Memory & Productivity",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = NovaCyanGlow
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = NovaSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, NovaBorder)
        ) {
            Column {
                SettingsRow(
                    icon = Icons.Default.Psychology,
                    title = "Personal Memory & Notes",
                    subtitle = "Saved preferences, facts, and voice notes",
                    onClick = onNavigateToMemory,
                    testTag = "settings_memory_row"
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.Alarm,
                    title = "Reminders & Scheduled Tasks",
                    subtitle = "Manage scheduled voice timers and alarms",
                    onClick = onNavigateToReminders,
                    testTag = "settings_reminders_row"
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.Security,
                    title = "Privacy, Security & Offline Mode",
                    subtitle = "Local storage, offline tools, data wipe",
                    onClick = onNavigateToPrivacyOffline,
                    testTag = "settings_privacy_row"
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.Info,
                    title = "About NOVA AI",
                    subtitle = "Version, architecture, build instructions",
                    onClick = onNavigateToAbout,
                    testTag = "settings_about_row"
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NovaSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NovaCyanGlow,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = NovaTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = NovaTextSecondary
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = "Navigate",
            tint = NovaTextSecondary.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(NovaBorder.copy(alpha = 0.6f))
    )
}
