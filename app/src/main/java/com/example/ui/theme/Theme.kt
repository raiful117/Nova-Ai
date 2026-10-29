package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NovaDarkColorScheme = darkColorScheme(
    primary = NovaElectricPurple,
    onPrimary = Color.White,
    primaryContainer = NovaSurfaceElevated,
    onPrimaryContainer = NovaElectricPurpleLight,
    secondary = NovaVividBlue,
    onSecondary = Color.White,
    secondaryContainer = NovaSurfaceVariant,
    onSecondaryContainer = NovaVividBlueLight,
    tertiary = NovaCyanGlow,
    onTertiary = NovaNearBlack,
    tertiaryContainer = NovaSurfaceVariant,
    onTertiaryContainer = NovaCyanGlow,
    background = NovaNearBlack,
    onBackground = NovaTextPrimary,
    surface = NovaSurface,
    onSurface = NovaTextPrimary,
    surfaceVariant = NovaSurfaceVariant,
    onSurfaceVariant = NovaTextSecondary,
    outline = NovaBorder,
    outlineVariant = NovaBorder.copy(alpha = 0.5f),
    error = NovaError,
    onError = Color.White
)

private val NovaLightColorScheme = lightColorScheme(
    primary = Color(0xFF6B21A8),
    onPrimary = Color.White,
    secondary = Color(0xFF1D4ED8),
    onSecondary = Color.White,
    tertiary = Color(0xFF0284C7),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = NovaError
)

@Composable
fun NovaTheme(
    darkTheme: Boolean = true, // Default to premium dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NovaDarkColorScheme else NovaLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
