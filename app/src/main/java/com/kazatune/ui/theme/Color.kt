package com.kazatune.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

data class AccentColorOption(
    val name: String,
    val color: Color
)

val AccentColorPresets = listOf(
    AccentColorOption("Resso Pink", Color(0xFFFF0050)),
    AccentColorOption("Spotify Green", Color(0xFF1DB954)),
    AccentColorOption("Electric Cyan", Color(0xFF00E5FF)),
    AccentColorOption("Royal Violet", Color(0xFF9D4EDD)),
    AccentColorOption("Sunset Orange", Color(0xFFFF6D00)),
    AccentColorOption("Gold Amber", Color(0xFFFFB300)),
    AccentColorOption("Ocean Blue", Color(0xFF2979FF)),
    AccentColorOption("Crimson Red", Color(0xFFE53935)),
    AccentColorOption("Monochrome", Color(0xFFFFFFFF))
)

/**
 * Global reactive accent color state backed by SharedPreferences.
 * Reading [RessoAccent] inside any Composable automatically subscribes it to color changes.
 */
object AppAccentState {
    private const val PREFS_NAME = "skim_ui_prefs"
    private const val KEY_ACCENT_ARGB = "accent_color_argb"
    private const val KEY_DJ_MIX_ANIMATION = "dj_mix_animation_enabled"
    private const val DEFAULT_ARGB = 0xFFFF0050.toInt()

    var accentColor by mutableStateOf(Color(0xFFFF0050))
        private set

    var djMixAnimationEnabled by mutableStateOf(true)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getInt(KEY_ACCENT_ARGB, DEFAULT_ARGB)
        accentColor = Color(saved)
        djMixAnimationEnabled = prefs.getBoolean(KEY_DJ_MIX_ANIMATION, true)
    }

    fun setAccentColor(context: Context, color: Color) {
        accentColor = color
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_ACCENT_ARGB, color.toArgb())
            .apply()
    }

    fun setDjMixAnimationEnabled(context: Context, enabled: Boolean) {
        djMixAnimationEnabled = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DJ_MIX_ANIMATION, enabled)
            .apply()
    }
}

val RessoBlack = Color(0xFF000000)
val RessoDarkSurface = Color(0xFF141318)
val RessoCardDark = Color(0xFF1C1B22)
val RessoAccentRed = Color(0xFFFF0050)
val RessoAccent: Color
    get() = AppAccentState.accentColor
val RessoAccentCyan = Color(0xFF00F2FE)
val RessoWhite = Color(0xFFFFFFFF)
val RessoWhite80 = Color(0xCCFFFFFF)
val RessoWhite60 = Color(0x99FFFFFF)
val RessoWhite30 = Color(0x4DFFFFFF)
val RessoDivider = Color(0x1AFFFFFF)
val RessoSheetBackground = Color(0xFF171717)
