package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NovaMainViewModel
import com.example.ui.theme.NovaBorder
import com.example.ui.theme.NovaCyanGlow
import com.example.ui.theme.NovaDarkNavy
import com.example.ui.theme.NovaElectricPurple
import com.example.ui.theme.NovaNearBlack
import com.example.ui.theme.NovaSurface
import com.example.ui.theme.NovaSurfaceElevated
import com.example.ui.theme.NovaTextPrimary
import com.example.ui.theme.NovaTextSecondary
import kotlin.math.roundToInt

@Composable
fun VoiceSettingsScreen(
    viewModel: NovaMainViewModel,
    onNavigateBack: () -> Unit
) {
    val userPrefs by viewModel.userPreferences.collectAsState()
    val availableVoices by viewModel.availableVoices.collectAsState()
    val isSpeaking by viewModel.ttsManager.isSpeaking.collectAsState()

    var speechRate by remember(userPrefs.speechRate) { mutableFloatStateOf(userPrefs.speechRate) }
    var selectedVoice by remember(userPrefs.selectedVoiceName) { mutableStateOf(userPrefs.selectedVoiceName) }

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

        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("voice_settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Voice & Speech Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = NovaTextPrimary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Speech Rate Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NovaSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, NovaBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = NovaCyanGlow,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Speech Speed",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = NovaTextPrimary
                        )
                    }

                    Text(
                        text = "${((speechRate * 10).roundToInt() / 10f)}x",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NovaCyanGlow
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Slider(
                    value = speechRate,
                    onValueChange = { speechRate = it },
                    valueRange = 0.6f..1.6f,
                    steps = 9,
                    modifier = Modifier.testTag("speech_rate_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = NovaCyanGlow,
                        activeTrackColor = NovaElectricPurple,
                        inactiveTrackColor = NovaSurfaceElevated
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Slow (0.6x)", style = MaterialTheme.typography.labelSmall, color = NovaTextSecondary)
                    Text("Normal (1.0x)", style = MaterialTheme.typography.labelSmall, color = NovaTextSecondary)
                    Text("Fast (1.6x)", style = MaterialTheme.typography.labelSmall, color = NovaTextSecondary)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Test Voice Button
                OutlinedButton(
                    onClick = {
                        if (isSpeaking) {
                            viewModel.ttsManager.stop()
                        } else {
                            val sampleText = if (userPrefs.language == "bn") {
                                "নমস্কার! নোভা এআই আপনার ব্যক্তিগত ভয়েস সহকারী।"
                            } else {
                                "Hello! I am NOVA AI, your personal voice assistant."
                            }
                            viewModel.ttsManager.speak(
                                text = sampleText,
                                languageCode = userPrefs.language,
                                speechRate = speechRate,
                                selectedVoiceName = selectedVoice
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("test_voice_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NovaCyanGlow.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = NovaCyanGlow,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSpeaking) "Stop Sample Speech" else "Play Test Voice Sample",
                        color = NovaCyanGlow,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Device Voices Section
        Text(
            text = "Installed Device Voices (${availableVoices.size})",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = NovaCyanGlow
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NovaSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, NovaBorder)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Default option
                val isDefaultSelected = selectedVoice.isBlank()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedVoice = "" }
                        .background(if (isDefaultSelected) NovaSurfaceElevated else Color.Transparent)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "System Default Voice",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isDefaultSelected) NovaCyanGlow else NovaTextPrimary
                        )
                        Text(
                            text = "Uses standard Android Text-To-Speech engine",
                            style = MaterialTheme.typography.bodySmall,
                            color = NovaTextSecondary
                        )
                    }
                    RadioButton(
                        selected = isDefaultSelected,
                        onClick = { selectedVoice = "" },
                        colors = RadioButtonDefaults.colors(selectedColor = NovaCyanGlow)
                    )
                }

                // Available installed voices (top 8)
                availableVoices.take(8).forEach { voice ->
                    val isSelected = selectedVoice == voice.name
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedVoice = voice.name }
                            .background(if (isSelected) NovaSurfaceElevated else Color.Transparent)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = voice.name,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = if (isSelected) NovaCyanGlow else NovaTextPrimary
                            )
                            Text(
                                text = "${voice.locale} • ${if (voice.isNetworkConnectionRequired) "Network required" else "Offline available"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NovaTextSecondary
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedVoice = voice.name },
                            colors = RadioButtonDefaults.colors(selectedColor = NovaCyanGlow)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Save Button
        Button(
            onClick = {
                viewModel.setSpeechRate(speechRate)
                viewModel.setSelectedVoice(selectedVoice)
                onNavigateBack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_voice_settings_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NovaElectricPurple)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Apply Speech Settings",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
