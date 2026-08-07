package com.trackpay.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = VerdantGreen,
    onPrimary = VerdantOnPrimary,
    primaryContainer = VerdantPrimaryContainer,
    onPrimaryContainer = VerdantOnPrimaryContainer,
    secondary = VerdantGreenLight,
    onSecondary = VerdantOnPrimary,
    background = VerdantDarkBackground,
    onBackground = VerdantDarkOnSurface,
    surface = VerdantDarkSurface,
    onSurface = VerdantDarkOnSurface,
    surfaceVariant = VerdantDarkSurfaceContainer,
    onSurfaceVariant = VerdantDarkOnSurfaceVariant,
    surfaceContainer = VerdantDarkSurfaceContainer,
    surfaceContainerLow = VerdantDarkBackground,
    surfaceContainerHigh = VerdantDarkSurfaceContainer,
    error = VerdantError,
    onError = Color(0xFF690005),
    outline = VerdantDarkOutline,
)

private val LightColorScheme = lightColorScheme(
    primary = VerdantLightPrimary,
    onPrimary = VerdantLightOnPrimary,
    primaryContainer = VerdantLightPrimaryContainer,
    onPrimaryContainer = VerdantLightOnPrimaryContainer,
    secondary = VerdantGreen,
    onSecondary = VerdantLightOnPrimary,
    background = VerdantLightBackground,
    onBackground = VerdantLightOnSurface,
    surface = VerdantLightSurface,
    onSurface = VerdantLightOnSurface,
    surfaceVariant = Color(0xFFDCE5DE),
    onSurfaceVariant = VerdantLightOnSurfaceVariant,
    surfaceContainer = Color(0xFFE8F2EB),
    surfaceContainerLow = VerdantLightBackground,
    surfaceContainerHigh = Color(0xFFDCE5DE),
    error = VerdantLightError,
    onError = Color(0xFFFFFFFF),
    outline = VerdantLightOutline,
)

@Composable
fun TrackPayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = TrackPayTypography,
        content = content,
    )
}
