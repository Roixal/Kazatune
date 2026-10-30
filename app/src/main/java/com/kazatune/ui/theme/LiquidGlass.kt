package com.kazatune.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * High-performance Apple-style "Liquid Glass" theme utilities.
 *
 * Provides translucent glassmorphism with specular refraction borders,
 * subtle surface sheen, and depth gradients without GPU render penalties.
 */
object LiquidGlassColors {
    // Glass bases
    val GlassSurface = Color(0x2D252836)
    val GlassSurfaceDark = Color(0x4014131D)
    val GlassSurfaceLight = Color(0x353B3E52)
    val GlassHighlight = Color(0x3DFFFFFF)
    val GlassHighlightSubtle = Color(0x1AFFFFFF)

    // Specular border gradients
    val GlassBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color(0x65FFFFFF),
            Color(0x20FFFFFF),
            Color(0x08FFFFFF),
            Color(0x30FFFFFF),
        )
    )

    val GlassBorderSubtleBrush = Brush.linearGradient(
        colors = listOf(
            Color(0x40FFFFFF),
            Color(0x12FFFFFF),
            Color(0x25FFFFFF),
        )
    )

    val GlassAccentBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color(0x80FF3B5C),
            Color(0x407B61FF),
            Color(0x3000E5FF),
        )
    )

    // Specular surface sheen
    val GlassSheenBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0x24FFFFFF),
            Color(0x0AFFFFFF),
            Color(0x00FFFFFF),
        )
    )

    val GlassPillBackground = Color(0x33222330)
}

/**
 * Applies Apple Liquid Glass background with specular border to any composable.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = LiquidGlassColors.GlassSurface,
    borderBrush: Brush = LiquidGlassColors.GlassBorderBrush,
    borderWidth: Dp = 1.dp,
): Modifier = this
    .clip(shape)
    .background(backgroundColor)
    .border(borderWidth, borderBrush, shape)

/**
 * Translucent liquid glass card container.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = LiquidGlassColors.GlassSurface,
    borderBrush: Brush = LiquidGlassColors.GlassBorderBrush,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderBrush, shape)
    ) {
        // Subtle specular highlight sheen on the top half
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(LiquidGlassColors.GlassSheenBrush)
        )
        content()
    }
}

/**
 * Liquid Glass Action Pill button.
 */
@Composable
fun LiquidGlassPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    backgroundColor: Color = LiquidGlassColors.GlassPillBackground,
    borderBrush: Brush = LiquidGlassColors.GlassBorderSubtleBrush,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderBrush, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White),
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
        content = content
    )
}
