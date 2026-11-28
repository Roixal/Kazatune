package com.kazatune.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun SkimMusicTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = RessoAccent,
        secondary = RessoAccentCyan,
        background = AppBackground,
        surface = RessoDarkSurface,
        onPrimary = RessoOnAccent,
        onSecondary = RessoBlack,
        onBackground = RessoWhite,
        onSurface = RessoWhite
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = RessoTypography,
        content = content
    )
}
