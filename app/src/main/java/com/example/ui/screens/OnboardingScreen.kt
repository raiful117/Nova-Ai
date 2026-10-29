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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NovaBorder
import com.example.ui.theme.NovaCyanGlow
import com.example.ui.theme.NovaDarkNavy
import com.example.ui.theme.NovaElectricPurple
import com.example.ui.theme.NovaNearBlack
import com.example.ui.theme.NovaSurface
import com.example.ui.theme.NovaSurfaceVariant
import com.example.ui.theme.NovaTextPrimary
import com.example.ui.theme.NovaTextSecondary
import com.example.ui.theme.NovaVividBlue

data class OnboardingCard(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val cards = remember {
        listOf(
            OnboardingCard(
                title = "Voice Conversation",
                description = "Talk naturally with NOVA AI in Bengali or English. Experience continuous conversational flow and instant voice replies.",
                icon = Icons.Default.Mic,
                accentColor = NovaCyanGlow
            ),
            OnboardingCard(
                title = "Multilingual Support",
                description = "Seamlessly communicate in বাংলা (Bengali), English, and हिन्दी (Hindi). NOVA AI adapts to your selected spoken tongue.",
                icon = Icons.Default.Translate,
                accentColor = NovaElectricPurple
            ),
            OnboardingCard(
                title = "Real Gemini AI Answers",
                description = "Ask questions, write essays, solve code, or seek advice with genuine Gemini AI intelligence connected to your personal API key.",
                icon = Icons.Default.Psychology,
                accentColor = NovaVividBlue
            ),
            OnboardingCard(
                title = "Natural Speech Output",
                description = "Hear responses read aloud with Android's native Text-to-Speech engine. Easily customize speech speed and preferred voice tone.",
                icon = Icons.Default.VolumeUp,
                accentColor = NovaCyanGlow
            ),
            OnboardingCard(
                title = "Conversation History",
                description = "Review your previous voice sessions, search prior insights, and manage saved records privately on your device with Room.",
                icon = Icons.Default.History,
                accentColor = NovaElectricPurple
            ),
            OnboardingCard(
                title = "Hands-Free & Automation",
                description = "Control apps, launch YouTube, open phone settings, set reminders, and automate daily phone tasks hands-free.",
                icon = Icons.Default.Settings,
                accentColor = NovaVividBlue
            )
        )
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    val currentCard = cards[currentIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NovaNearBlack, NovaDarkNavy, NovaNearBlack)
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar: Skip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NOVA AI Guide",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = NovaCyanGlow
            )
            TextButton(
                onClick = onFinish,
                modifier = Modifier.testTag("onboarding_skip_button")
            ) {
                Text(
                    text = "Skip",
                    color = NovaTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Main Center Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NovaSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, NovaBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon circle with glowing aura
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(currentCard.accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = currentCard.icon,
                        contentDescription = currentCard.title,
                        tint = currentCard.accentColor,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = currentCard.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = NovaTextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = currentCard.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = NovaTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
        }

        // Bottom Controls: Page Indicators & Navigation Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dots indicator
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                cards.indices.forEach { index ->
                    val isSelected = index == currentIndex
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .height(8.dp)
                            .width(if (isSelected) 24.dp else 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) NovaElectricPurple else NovaSurfaceVariant)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                if (currentIndex > 0) {
                    IconButton(
                        onClick = { currentIndex-- },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(NovaSurfaceVariant)
                            .testTag("onboarding_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(50.dp))
                }

                // Next or Get Started button
                Button(
                    onClick = {
                        if (currentIndex < cards.size - 1) {
                            currentIndex++
                        } else {
                            onFinish()
                        }
                    },
                    modifier = Modifier
                        .height(52.dp)
                        .padding(horizontal = 8.dp)
                        .testTag("onboarding_next_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NovaElectricPurple)
                ) {
                    Text(
                        text = if (currentIndex < cards.size - 1) "Next" else "Get Started",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (currentIndex < cards.size - 1) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                        contentDescription = "Proceed",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
