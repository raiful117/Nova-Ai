package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.OrbState
import com.example.ui.theme.NovaCyanGlow
import com.example.ui.theme.NovaElectricPurple
import com.example.ui.theme.NovaElectricPurpleLight
import com.example.ui.theme.NovaError
import com.example.ui.theme.NovaSpeakingWaveform
import com.example.ui.theme.NovaVividBlue
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NovaVoiceOrb(
    orbState: OrbState,
    rmsDb: Float = 0f,
    size: Dp = 220.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbInfiniteTransition")

    // Breathing pulse for idle state
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbBreathing"
    )

    // Rotation angle for processing / active states
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    // Waveform oscillation for speaking state
    val waveOscillation by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbWave"
    )

    // Audio amplitude reactive scaling
    val dynamicRms = remember { Animatable(0f) }
    LaunchedEffect(rmsDb) {
        val target = (rmsDb / 10f).coerceIn(0f, 1.5f)
        dynamicRms.animateTo(target, tween(60))
    }

    Box(
        modifier = Modifier
            .size(size)
            .testTag("nova_voice_orb")
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val baseRadius = (size.toPx() / 2f) * 0.65f

            when (orbState) {
                OrbState.IDLE -> {
                    // Soft purple-blue glow and gentle outer ring
                    val pulseRadius = baseRadius * breathingPulse
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NovaElectricPurple.copy(alpha = 0.45f),
                                NovaVividBlue.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = pulseRadius * 1.35f
                        ),
                        radius = pulseRadius * 1.35f,
                        center = center
                    )

                    // Outer thin halo
                    drawCircle(
                        color = NovaElectricPurpleLight.copy(alpha = 0.35f),
                        radius = pulseRadius * 1.12f,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Core glowing sphere
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(NovaElectricPurple, NovaVividBlue),
                            start = Offset(center.x - pulseRadius, center.y - pulseRadius),
                            end = Offset(center.x + pulseRadius, center.y + pulseRadius)
                        ),
                        radius = pulseRadius,
                        center = center
                    )
                }

                OrbState.LISTENING -> {
                    // Reactive acoustic rings driven by amplitude
                    val audioBoost = dynamicRms.value * 28.dp.toPx()
                    val ring1Radius = baseRadius * 1.15f + audioBoost * 0.6f
                    val ring2Radius = baseRadius * 1.35f + audioBoost

                    // Outer audio aura
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NovaCyanGlow.copy(alpha = 0.55f),
                                NovaVividBlue.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = ring2Radius
                        ),
                        radius = ring2Radius,
                        center = center
                    )

                    // Concentric acoustic rings
                    drawCircle(
                        color = NovaCyanGlow.copy(alpha = 0.85f),
                        radius = ring1Radius,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawCircle(
                        color = NovaCyanGlow.copy(alpha = 0.45f),
                        radius = ring2Radius,
                        center = center,
                        style = Stroke(width = 1.8.dp.toPx())
                    )

                    // Cyan-blue core sphere
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NovaCyanGlow, NovaVividBlue, NovaElectricPurple),
                            center = center,
                            radius = baseRadius
                        ),
                        radius = baseRadius,
                        center = center
                    )
                }

                OrbState.PROCESSING -> {
                    // Rotating gradient orbital rings
                    val orbitalRadius = baseRadius * 1.25f
                    val radians = Math.toRadians(rotationAngle.toDouble())

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NovaElectricPurple.copy(alpha = 0.5f),
                                NovaCyanGlow.copy(alpha = 0.2f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = orbitalRadius * 1.2f
                        ),
                        radius = orbitalRadius * 1.2f,
                        center = center
                    )

                    // Rotating orbit particles
                    for (i in 0 until 4) {
                        val angle = radians + (i * Math.PI / 2)
                        val px = center.x + (orbitalRadius * cos(angle)).toFloat()
                        val py = center.y + (orbitalRadius * sin(angle)).toFloat()
                        drawCircle(
                            color = if (i % 2 == 0) NovaCyanGlow else NovaElectricPurpleLight,
                            radius = 6.dp.toPx(),
                            center = Offset(px, py)
                        )
                    }

                    // Rotating dashed ring
                    drawCircle(
                        color = NovaElectricPurple.copy(alpha = 0.7f),
                        radius = orbitalRadius,
                        center = center,
                        style = Stroke(width = 2.5.dp.toPx())
                    )

                    // Core sphere
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(NovaElectricPurple, NovaCyanGlow, NovaVividBlue, NovaElectricPurple),
                            center = center
                        ),
                        radius = baseRadius,
                        center = center
                    )
                }

                OrbState.SPEAKING -> {
                    // Undulating waveform bars across center
                    val pulseRadius = baseRadius * 1.05f

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NovaSpeakingWaveform.copy(alpha = 0.45f),
                                NovaElectricPurple.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = pulseRadius * 1.3f
                        ),
                        radius = pulseRadius * 1.3f,
                        center = center
                    )

                    // Core orb
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(NovaElectricPurple, NovaSpeakingWaveform, NovaVividBlue),
                            start = Offset(center.x - pulseRadius, center.y - pulseRadius),
                            end = Offset(center.x + pulseRadius, center.y + pulseRadius)
                        ),
                        radius = pulseRadius,
                        center = center
                    )

                    // Draw stylized center waveform bars
                    val barCount = 7
                    val barWidth = 4.dp.toPx()
                    val totalWidth = barCount * 10.dp.toPx()
                    val startX = center.x - (totalWidth / 2f)

                    for (i in 0 until barCount) {
                        val factor = sin((i * 0.8f) + (waveOscillation * 1.5f))
                        val barHeight = (16.dp.toPx() + (factor * 22.dp.toPx()).coerceAtLeast(6.dp.toPx()))
                        val x = startX + (i * 10.dp.toPx())
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(x, center.y - (barHeight / 2f)),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                    }
                }

                OrbState.ERROR -> {
                    // Restrained red accent with soft warning glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NovaError.copy(alpha = 0.5f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = baseRadius * 1.3f
                        ),
                        radius = baseRadius * 1.3f,
                        center = center
                    )
                    drawCircle(
                        color = NovaError,
                        radius = baseRadius,
                        center = center
                    )
                }
            }
        }
    }
}
