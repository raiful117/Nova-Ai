package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.NovaBorder
import com.example.ui.theme.NovaCyanGlow
import com.example.ui.theme.NovaDarkNavy
import com.example.ui.theme.NovaElectricPurple
import com.example.ui.theme.NovaNearBlack
import com.example.ui.theme.NovaSurface
import com.example.ui.theme.NovaSurfaceElevated
import com.example.ui.theme.NovaTextPrimary
import com.example.ui.theme.NovaTextSecondary

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NovaNearBlack, NovaDarkNavy, NovaNearBlack)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("about_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "About NOVA AI",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = NovaTextPrimary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Hero Logo & Title
        Image(
            painter = painterResource(id = R.drawable.ic_nova_logo),
            contentDescription = "NOVA AI Logo",
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "NOVA AI",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            ),
            color = NovaTextPrimary
        )

        Text(
            text = "Your Personal Voice Assistant • Version 1.0",
            style = MaterialTheme.typography.bodyMedium,
            color = NovaCyanGlow
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Architecture & Technologies Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = NovaSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, NovaBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Technology Stack & Architecture",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = NovaTextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                AboutTechRow(
                    icon = Icons.Default.Psychology,
                    title = "AI Intelligence",
                    desc = "Google Gemini 2.5 Flash via REST API (Google AI Studio)"
                )
                SettingsDivider()
                AboutTechRow(
                    icon = Icons.Default.RecordVoiceOver,
                    title = "Speech-to-Text & TTS",
                    desc = "Native Android SpeechRecognizer & TextToSpeech with Bengali (বাংলা), English & Hindi"
                )
                SettingsDivider()
                AboutTechRow(
                    icon = Icons.Default.Android,
                    title = "UI Framework",
                    desc = "Jetpack Compose & Material Design 3 with custom Cyber Glowing Orb canvas engine"
                )
                SettingsDivider()
                AboutTechRow(
                    icon = Icons.Default.Storage,
                    title = "Local Persistence",
                    desc = "Room Database (SQLite) for conversations, notes & reminders; DataStore for preferences"
                )
                SettingsDivider()
                AboutTechRow(
                    icon = Icons.Default.Code,
                    title = "Phone Automation",
                    desc = "PackageManager app launcher, system settings intents, and AccessibilityService automation"
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Native Android Build & APK Workflow
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = NovaSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = NovaCyanGlow,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Build & APK Export Instructions",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = NovaTextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "1. Export Project: Download ZIP from AI Studio header menu.\n2. Open in Android Studio: Select 'Open an Existing Project' and choose the unzipped folder.\n3. Gradle Sync: Ensure Java 17+ / Android SDK 36 is configured.\n4. Build APK: In Android Studio, go to Build > Build Bundle(s) / APK(s) > Build APK(s).\n5. Install on Device: Transfer app-debug.apk to your phone or run via adb install.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NovaTextSecondary,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "© 2026 NOVA AI. All rights reserved.",
            style = MaterialTheme.typography.labelSmall,
            color = NovaTextSecondary.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun AboutTechRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(NovaElectricPurple.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NovaCyanGlow,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = NovaTextPrimary
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = NovaTextSecondary
            )
        }
    }
}
