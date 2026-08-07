package com.trackpay.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.trackpay.app.domain.model.ThemeIds

/**
 * Applies the catalog [themeId] light/dark schemes.
 * Active theme wins over dynamic color (dynamic color not used).
 */
@Composable
fun TrackPayTheme(
    themeId: String = ThemeIds.DEFAULT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = ThemePacks.schemeFor(themeId, darkTheme)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = TrackPayTypography,
        content = content,
    )
}
