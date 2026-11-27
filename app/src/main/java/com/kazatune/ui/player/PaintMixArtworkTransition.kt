package com.kazatune.ui.player

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
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
 * Paint-mix DJ transition view for the center album art card:
 * 1. The incoming song's album art rises up from the bottom.
 * 2. Both covers swirl and blend together like liquid paint mixing.
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
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "PaintMixProgress"
    )

    // Continuous swirl angle during mixing
    val infiniteTransition = rememberInfiniteTransition(label = "PaintSwirl")
    val swirlAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SwirlAngle"
    )

    // Phase curves:
    // - Rise from bottom: 0.0 -> 0.45
    val riseFraction = (smoothProgress / 0.45f).coerceIn(0f, 1f)
    val easedRise = 1f - (1f - riseFraction) * (1f - riseFraction)

    // - Paint mix intensity peaks in the middle (0.2 -> 0.85)
    val mixIntensity = sin(smoothProgress * PI).toFloat().coerceIn(0f, 1f)

    // - Outgoing dissolves out: 0.35 -> 0.95
    val outgoingAlpha = (1f - ((smoothProgress - 0.35f) / 0.60f).coerceIn(0f, 1f))
    val incomingAlpha = (smoothProgress / 0.55f).coerceIn(0.15f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF14131B))
    ) {
        // 1. Outgoing song's album art (softly scales, rotates slightly, and melts away)
        if (outgoingAlpha > 0.01f && !outgoingArtworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = outgoingArtworkUrl,
                contentDescription = "Outgoing Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = outgoingAlpha
                        scaleX = 1f + mixIntensity * 0.14f
                        scaleY = 1f + mixIntensity * 0.14f
                        rotationZ = -mixIntensity * 6f
                    }
                    .blur((mixIntensity * 14f).dp)
            )
        }

        // 2. Incoming (next) song's album art rising from bottom and blending like paint
        if (!incomingArtworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = incomingArtworkUrl,
                contentDescription = "Incoming Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                        translationY = (1f - easedRise) * size.height
                        scaleX = 0.88f + 0.12f * easedRise + mixIntensity * 0.06f
                        scaleY = 0.88f + 0.12f * easedRise + mixIntensity * 0.06f
                        rotationZ = mixIntensity * 5f * (1f - smoothProgress)
                        alpha = incomingAlpha
                    }
                    .blur(((1f - smoothProgress) * mixIntensity * 10f).dp)
            )
        }

        // 3. Swirling paint-mix ribbon & liquid sheen overlay during peak overlap
        if (mixIntensity > 0.05f) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(mixIntensity * 0.55f)
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
                            RessoAccent.copy(alpha = 0.45f),
                            Color(0xFF00E5FF).copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        center = c1,
                        radius = w * 0.58f
                    ),
                    center = c1,
                    radius = w * 0.58f,
                    blendMode = BlendMode.Screen
                )

                val c2 = Offset(
                    x = w * (0.5f - 0.25f * sin(swirlAngle)),
                    y = h * (0.5f + 0.22f * cos(swirlAngle))
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF9D4EDD).copy(alpha = 0.40f),
                            RessoAccent.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = c2,
                        radius = w * 0.52f
                    ),
                    center = c2,
                    radius = w * 0.52f,
                    blendMode = BlendMode.Screen
                )
            }
        }

        // 4. Subtle "DJ MIXING" pill indicator at the bottom of the cover
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xAA100F16),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
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
