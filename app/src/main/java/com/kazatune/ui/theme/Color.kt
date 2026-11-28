package com.kazatune.ui.theme

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

data class AccentColorOption(
    val name: String,
    val color: Color
)

val AccentColorPresets = listOf(
    AccentColorOption("Skim Pink", Color(0xFFFF0050)),
    AccentColorOption("Emerald Green", Color(0xFF1DB954)),
    AccentColorOption("Electric Cyan", Color(0xFF00E5FF)),
    AccentColorOption("Royal Violet", Color(0xFF9D4EDD)),
    AccentColorOption("Sunset Orange", Color(0xFFFF6D00)),
    AccentColorOption("Gold Amber", Color(0xFFFFB300)),
    AccentColorOption("Ocean Blue", Color(0xFF2979FF)),
    AccentColorOption("Crimson Red", Color(0xFFE53935)),
    AccentColorOption("Monochrome", Color(0xFFFFFFFF))
)

/**
 * Global reactive UI state (accent color, AMOLED mode, DJ mix animation) backed by SharedPreferences.
 */
object AppAccentState {
    private const val PREFS_NAME = "skim_ui_prefs"
    private const val KEY_ACCENT_ARGB = "accent_color_argb"
    private const val KEY_DJ_MIX_ANIMATION = "dj_mix_animation_enabled"
    private const val KEY_AMOLED_MODE = "amoled_mode_enabled"
    private const val DEFAULT_ARGB = 0xFFFF0050.toInt()

    var accentColor by mutableStateOf(Color(0xFFFF0050))
        private set

    var djMixAnimationEnabled by mutableStateOf(true)
        private set

    var amoledModeEnabled by mutableStateOf(false)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getInt(KEY_ACCENT_ARGB, DEFAULT_ARGB)
        accentColor = Color(saved)
        djMixAnimationEnabled = prefs.getBoolean(KEY_DJ_MIX_ANIMATION, true)
        amoledModeEnabled = prefs.getBoolean(KEY_AMOLED_MODE, false)
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

    fun setAmoledModeEnabled(context: Context, enabled: Boolean) {
        amoledModeEnabled = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AMOLED_MODE, enabled)
            .apply()
    }
}

val RessoBlack: Color
    get() = if (AppAccentState.amoledModeEnabled) Color.Black else Color(0xFF000000)
val AppBackground: Color
    get() = if (AppAccentState.amoledModeEnabled) Color.Black else Color(0xFF0F0E13)
val AppCardBackground: Color
    get() = if (AppAccentState.amoledModeEnabled) Color(0xFF08080A) else Color(0xFF181720)
val AppCardBorder: BorderStroke?
    get() = if (AppAccentState.amoledModeEnabled) BorderStroke(1.dp, Color(0x18FFFFFF)) else null
val RessoDarkSurface: Color
    get() = if (AppAccentState.amoledModeEnabled) Color.Black else Color(0xFF141318)
val RessoCardDark: Color
    get() = if (AppAccentState.amoledModeEnabled) Color(0xFF0A0A0D) else Color(0xFF1C1B22)
val RessoAccentRed = Color(0xFFFF0050)
val RessoAccent: Color
    get() = AppAccentState.accentColor
val RessoOnAccent: Color
    get() = if (RessoAccent.luminance() > 0.65f) Color.Black else Color.White
val RessoAccentCyan = Color(0xFF00F2FE)
val RessoWhite = Color(0xFFFFFFFF)
val RessoWhite80 = Color(0xCCFFFFFF)
val RessoWhite60 = Color(0x99FFFFFF)
val RessoWhite30 = Color(0x4DFFFFFF)
val RessoDivider = Color(0x1AFFFFFF)
val RessoSheetBackground: Color
    get() = if (AppAccentState.amoledModeEnabled) Color(0xFF050507) else Color(0xFF171717)
