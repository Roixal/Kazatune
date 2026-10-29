package com.kazatune.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = RessoAccentRed,
    secondary = RessoAccentCyan,
    background = RessoBlack,
    surface = RessoDarkSurface,
    onPrimary = RessoWhite,
    onSecondary = RessoBlack,
    onBackground = RessoWhite,
    onSurface = RessoWhite
)

@Composable
fun KazatuneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = RessoTypography,
        content = content
    )
}
