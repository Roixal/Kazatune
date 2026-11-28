package com.kazatune.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kazatune.ui.theme.DinProFamily
import com.kazatune.ui.theme.RessoAccent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 60fps GPU-friendly paint-mix DJ transition view:
 * 1. The incoming song's album art rises smoothly from the bottom of the screen.
 * 2. Both covers blend and swirl together using GPU graphicsLayer transforms + radial paint glow
 *    (no per-frame RenderEffect blur reallocation).
 * 3. As [progress] reaches 1f, the outgoing cover dissolves and only the new cover remains.
 */
@Composable
fun PaintMixArtworkTransition(
    outgoingArtworkUrl: String?,
    incomingArtworkUrl: String?,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val smoothProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 160, easing = LinearEasing),
        label = "PaintMixProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "PaintSwirl")
    val swirlAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SwirlAngle"
    )

    // Smoothstep rise from bottom: 0.0 -> 0.50
    val rawRise = (smoothProgress / 0.50f).coerceIn(0f, 1f)
    val easedRise = rawRise * rawRise * (3f - 2f * rawRise)

    // Paint-mix glow peaks in the middle
    val mixIntensity = sin(smoothProgress * PI).toFloat().coerceIn(0f, 1f)

    // Smooth cross-dissolve
    val outgoingAlpha = (1f - ((smoothProgress - 0.30f) / 0.65f).coerceIn(0f, 1f))
    val incomingAlpha = (smoothProgress / 0.55f).coerceIn(0.20f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A090F))
    ) {
        // 1. Outgoing song's album art (gently zooms & dissolves away)
        if (outgoingAlpha > 0.01f && !outgoingArtworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = outgoingArtworkUrl,
                contentDescription = "Outgoing Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = outgoingAlpha
                        scaleX = 1f + mixIntensity * 0.12f
                        scaleY = 1f + mixIntensity * 0.12f
                        rotationZ = -mixIntensity * 4f
                    }
            )
        }

        // 2. Incoming (next) song's album art rising from bottom and blending in
        if (!incomingArtworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = incomingArtworkUrl,
                contentDescription = "Incoming Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = (1f - easedRise) * size.height
                        scaleX = 0.90f + 0.10f * easedRise + mixIntensity * 0.04f
                        scaleY = 0.90f + 0.10f * easedRise + mixIntensity * 0.04f
                        rotationZ = mixIntensity * 3.5f * (1f - smoothProgress)
                        alpha = incomingAlpha
                    }
            )
        }

        // 3. Swirling paint-mix radial sheen overlay (pure GPU Canvas, 60fps)
        if (mixIntensity > 0.04f) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(mixIntensity * 0.50f)
            ) {
                val w = size.width
                val h = size.height

                val c1 = Offset(
                    x = w * (0.5f + 0.25f * cos(swirlAngle)),
                    y = h * (0.5f + 0.25f * sin(swirlAngle))
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            RessoAccent.copy(alpha = 0.42f),
                            Color(0xFF00E5FF).copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = c1,
                        radius = w * 0.62f
                    ),
                    center = c1,
                    radius = w * 0.62f,
                    blendMode = BlendMode.Screen
                )

                val c2 = Offset(
                    x = w * (0.5f - 0.25f * sin(swirlAngle)),
                    y = h * (0.5f + 0.22f * cos(swirlAngle))
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF9D4EDD).copy(alpha = 0.38f),
                            RessoAccent.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = c2,
                        radius = w * 0.55f
                    ),
                    center = c2,
                    radius = w * 0.55f,
                    blendMode = BlendMode.Screen
                )
            }
        }

        // 4. Subtle "DJ MIXING" pill indicator
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xAA100F16),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 110.dp)
                .alpha((mixIntensity * 1.4f).coerceIn(0f, 1f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = RessoAccent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "DJ MIXING",
                    fontFamily = DinProFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
