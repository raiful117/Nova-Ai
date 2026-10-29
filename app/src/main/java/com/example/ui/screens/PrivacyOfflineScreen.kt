package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NovaMainViewModel
import com.example.ui.theme.NovaBorder
import com.example.ui.theme.NovaCyanGlow
import com.example.ui.theme.NovaDarkNavy
import com.example.ui.theme.NovaError
import com.example.ui.theme.NovaNearBlack
import com.example.ui.theme.NovaSuccess
import com.example.ui.theme.NovaSurface
import com.example.ui.theme.NovaSurfaceElevated
import com.example.ui.theme.NovaTextPrimary
import com.example.ui.theme.NovaTextSecondary

@Composable
fun PrivacyOfflineScreen(
    viewModel: NovaMainViewModel,
    onNavigateBack: () -> Unit
) {
    var showWipeConfirmDialog by remember { mutableStateOf(false) }

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
                modifier = Modifier.testTag("privacy_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Privacy & Offline Mode",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = NovaTextPrimary
                )
                Text(
                    text = "Local data security & offline features",
                    style = MaterialTheme.typography.labelSmall,
                    color = NovaCyanGlow
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Privacy Guarantee Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NovaSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, NovaBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = NovaCyanGlow,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Zero-Cloud Data Storage",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NovaTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "• All conversation histories, remembered notes, and timers are stored 100% locally on your phone in SQLite/Room.\n• NOVA AI never uploads your conversations to external cloud databases.\n• Audio from speech recognition is processed directly by Android Speech Services.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NovaTextSecondary,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Offline vs Online Capability Matrix
        Text(
            text = "Offline vs Online Feature Matrix",
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
                MatrixItem(name = "Deterministic Calculator & Math", isOffline = true)
                SettingsDivider()
                MatrixItem(name = "App Launching & Settings Panels", isOffline = true)
                SettingsDivider()
                MatrixItem(name = "Local Notes & Personal Memories", isOffline = true)
                SettingsDivider()
                MatrixItem(name = "AlarmManager Reminders & Timers", isOffline = true)
                SettingsDivider()
                MatrixItem(name = "Text-to-Speech (Installed Voices)", isOffline = true)
                SettingsDivider()
                MatrixItem(name = "Gemini AI Intelligent Responses", isOffline = false, note = "Requires Internet")
                SettingsDivider()
                MatrixItem(name = "Web & YouTube Online Searches", isOffline = false, note = "Requires Internet")
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Wipe Data Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = NovaError.copy(alpha = 0.08f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, NovaError.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Factory Reset & Wipe Data",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = NovaError
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Permanently deletes all saved conversations, notes, memories, scheduled reminders, and clears your stored API key.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NovaTextSecondary,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { showWipeConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("wipe_all_data_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NovaError)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Local Data", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (showWipeConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showWipeConfirmDialog = false },
                title = { Text("Reset NOVA AI Data?") },
                text = { Text("This will permanently erase all local conversation history, saved memories, reminders, and reset your API key. Proceed?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.resetAllData()
                            showWipeConfirmDialog = false
                            onNavigateBack()
                        }
                    ) {
                        Text("Reset Everything", color = NovaError)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWipeConfirmDialog = false }) {
                        Text("Cancel", color = NovaTextSecondary)
                    }
                },
                containerColor = NovaSurface,
                titleContentColor = NovaTextPrimary,
                textContentColor = NovaTextSecondary
            )
        }
    }
}

@Composable
fun MatrixItem(
    name: String,
    isOffline: Boolean,
    note: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = NovaTextPrimary
            )
            if (note != null) {
                Text(text = note, style = MaterialTheme.typography.labelSmall, color = NovaCyanGlow)
            }
        }
        Text(
            text = if (isOffline) "Offline Capable" else "Online Only",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isOffline) NovaSuccess else NovaCyanGlow
        )
    }
}
