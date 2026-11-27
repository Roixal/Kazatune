package com.kazatune.ui.component

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.kazatune.ui.theme.RessoAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

private data class MeshPaletteColors(
    val vibrant: Color,
    val darkVibrant: Color,
    val dominant: Color
)

/**
 * Animated dynamic fluid mesh gradient backdrop powered by Android's Palette API.
 *
 * Extracts 3 dominant swatches (Vibrant, DarkVibrant, Dominant) from [artworkUrl]
 * (falling back to a deterministic palette from [seedString] while loading) and
 * animates them with infinitely drifting radial gradients for a living backdrop.
 */
@Composable
fun DynamicFluidMesh(
    seedString: String,
    modifier: Modifier = Modifier,
    artworkUrl: String? = null,
    primaryColor: Color = RessoAccent,
    secondaryColor: Color = Color(0xFF00E5FF),
    tertiaryColor: Color = Color(0xFF7928CA),
    orbAlpha: Float = 0.72f
) {
    val context = LocalContext.current
    val fallbackPalette = remember(seedString, primaryColor, secondaryColor, tertiaryColor) {
        val hash = seedString.hashCode()
        when (kotlin.math.abs(hash % 4)) {
            0 -> MeshPaletteColors(primaryColor, tertiaryColor, secondaryColor)
            1 -> MeshPaletteColors(secondaryColor, primaryColor, tertiaryColor)
            2 -> MeshPaletteColors(tertiaryColor, secondaryColor, primaryColor)
            else -> MeshPaletteColors(Color(0xFFFF2E93), Color(0xFF7000FF), Color(0xFF00F0FF))
        }
    }

    var paletteColors by remember(seedString) { mutableStateOf(fallbackPalette) }

    LaunchedEffect(artworkUrl, seedString) {
        if (artworkUrl.isNullOrBlank()) {
            paletteColors = fallbackPalette
            return@LaunchedEffect
        }
        val extracted = withContext(Dispatchers.IO) {
            extractPaletteColors(context, artworkUrl, fallbackPalette)
        }
        if (extracted != null) {
            paletteColors = extracted
        }
    }

    val color1 by animateColorAsState(
        targetValue = paletteColors.vibrant,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "FluidMeshVibrant"
    )
    val color2 by animateColorAsState(
        targetValue = paletteColors.darkVibrant,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "FluidMeshDarkVibrant"
    )
    val color3 by animateColorAsState(
        targetValue = paletteColors.dominant,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "FluidMeshDominant"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "FluidMeshDrift")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "FluidMeshPhase"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FluidMeshPulse"
    )

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF0A090F))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val maxDim = maxOf(width, height)

            // Orb 1: Vibrant swatch drifting in upper-center Lissajous curve
            val x1 = width * (0.35f + 0.22f * sin(phase))
            val y1 = height * (0.28f + 0.18f * cos(phase * 0.8f))
            val radius1 = maxDim * 0.72f * pulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color1.copy(alpha = orbAlpha * 0.85f),
                        color1.copy(alpha = orbAlpha * 0.30f),
                        Color.Transparent
                    ),
                    center = Offset(x1, y1),
                    radius = radius1
                ),
                center = Offset(x1, y1),
                radius = radius1
            )

            // Orb 2: Dominant swatch drifting in middle-right arc
            val x2 = width * (0.68f + 0.22f * cos(phase * 0.7f))
            val y2 = height * (0.55f + 0.20f * sin(phase * 0.9f))
            val radius2 = maxDim * 0.78f * (2.02f - pulse)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color3.copy(alpha = orbAlpha * 0.90f),
                        color3.copy(alpha = orbAlpha * 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(x2, y2),
                    radius = radius2
                ),
                center = Offset(x2, y2),
                radius = radius2
            )

            // Orb 3: DarkVibrant swatch anchoring depth in lower-left
            val x3 = width * (0.30f + 0.20f * sin(phase + 2.1f))
            val y3 = height * (0.76f + 0.16f * cos(phase + 1.2f))
            val radius3 = maxDim * 0.82f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color2.copy(alpha = orbAlpha),
                        color2.copy(alpha = orbAlpha * 0.40f),
                        Color.Transparent
                    ),
                    center = Offset(x3, y3),
                    radius = radius3
                ),
                center = Offset(x3, y3),
                radius = radius3
            )
        }
    }
}

private suspend fun extractPaletteColors(
    context: android.content.Context,
    artworkUrl: String,
    fallback: MeshPaletteColors
): MeshPaletteColors? {
    return try {
        val loader = SingletonImageLoader.get(context)
        val request = ImageRequest.Builder(context)
            .data(artworkUrl)
            .size(64, 64)
            .allowHardware(false)
            .build()
        val result = loader.execute(request) as? SuccessResult ?: return null
        val bitmap: Bitmap = result.image.toBitmap()
        val palette = withContext(Dispatchers.Default) {
            Palette.from(bitmap).generate()
        }

        val vibrantRgb = palette.vibrantSwatch?.rgb
            ?: palette.lightVibrantSwatch?.rgb
            ?: palette.mutedSwatch?.rgb
        val darkVibrantRgb = palette.darkVibrantSwatch?.rgb
            ?: palette.darkMutedSwatch?.rgb
        val dominantRgb = palette.dominantSwatch?.rgb
            ?: palette.mutedSwatch?.rgb

        MeshPaletteColors(
            vibrant = vibrantRgb?.let(::Color) ?: fallback.vibrant,
            darkVibrant = darkVibrantRgb?.let(::Color) ?: fallback.darkVibrant,
            dominant = dominantRgb?.let(::Color) ?: fallback.dominant
        )
    } catch (_: Exception) {
        null
    }
}
