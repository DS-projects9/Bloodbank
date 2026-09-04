package com.medkeen.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = White,
    primaryContainer = LightBlue,
    onPrimaryContainer = DarkText,
    secondary = ActiveBlue,
    onSecondary = White,
    secondaryContainer = CardBackground,
    onSecondaryContainer = DarkText,
    tertiary = SuccessGreen,
    onTertiary = White,
    background = White,
    onBackground = DarkText,
    surface = White,
    onSurface = DarkText,
    surfaceTint = Color.Transparent,
    surfaceVariant = White,
    onSurfaceVariant = SecondaryText,
    outline = BorderColor,
    outlineVariant = LightBorderColor,
    error = EmergencyRed,
    onError = White
)

@Composable
fun MedKeenTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
