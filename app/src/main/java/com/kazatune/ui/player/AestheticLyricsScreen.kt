package com.kazatune.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.kazatune.ui.component.DynamicFluidMesh
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kazatune.ui.theme.DinProFamily
import com.kazatune.ui.theme.LiquidGlassColors
import com.kazatune.ui.theme.LiquidGlassPill
import com.kazatune.ui.theme.RessoAccent
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.metadata.Lyrics

/**
 * Aesthetic full-screen lyrics experience inspired by Apple Music and Spotify.
 *
 * Features:
 * - Blurred album art backdrop with multi-layered vignette gradients
 * - Active lyric line is centered, large, bold, and accented with a soft glow
 * - Past/future lines smoothly fade in opacity and scale
 * - Spring-animated auto-scrolling keeps the active line centered
 * - Tap any line to seek to that timestamp
 * - Compact glassmorphism transport controls at the bottom
 * - Lyrics offset calibration controls
 */
@Composable
fun AestheticLyricsScreen(
    track: Track,
    lyrics: Lyrics?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    totalDurationMs: Long,
    lyricsOffsetMs: Long,
    onSeek: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    onShareLyric: () -> Unit,
    onAdjustLyricsOffset: (Long) -> Unit,
    onResetLyricsOffset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val artworkUrl = track.thumbnails?.lastOrNull()?.url
    val artistName = track.artists?.joinToString(", ") { it.name ?: "" } ?: ""
    val lines = lyrics?.lines ?: emptyList()
    val calibratedPositionMs = currentPositionMs + lyricsOffsetMs

    // Find the active lyric line
    val activeIndex by remember(lines, calibratedPositionMs) {
        derivedStateOf {
            val idx = lines.indexOfLast { line ->
                (line.startTimeMs.toLongOrNull() ?: 0L) <= calibratedPositionMs
            }
            if (idx >= 0) idx else 0
        }
    }

    val effectiveDurationMs = when {
        totalDurationMs > 0L -> totalDurationMs
        (track.durationSeconds ?: 0) > 0 -> (track.durationSeconds ?: 0) * 1000L
        else -> 180_000L
    }

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isUserDragging by remember { mutableStateOf(false) }

    LaunchedEffect(currentPositionMs) {
        if (!isUserDragging) {
            sliderPosition = (currentPositionMs.toFloat() / effectiveDurationMs).coerceIn(0f, 1f)
        }
    }

    // Auto-scroll the list to keep the active line centered
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(activeIndex) {
        if (lines.isNotEmpty() && activeIndex in lines.indices) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            listState.animateScrollToItem(
                index = activeIndex,
                scrollOffset = -300  // Offset to visually center the active line
            )
        }
    }

    var showCalibration by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // ─── Layer 0: Dynamic Fluid Mesh Gradient (Palette API) ───
        DynamicFluidMesh(
            seedString = track.videoId,
            artworkUrl = artworkUrl
        )

        // ─── Layer 1: Blurred Album Art Backdrop ───
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artworkUrl)
                .size(200, 200)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(48.dp)
                .alpha(0.65f)
                .scale(1.2f) // Scale up slightly to avoid blur edge artifacts
        )

        // ─── Layer 2: Multi-layered Dark Vignette ───
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xDD0A090F),
                            0.15f to Color(0xBB0A090F),
                            0.4f to Color(0x880A090F),
                            0.6f to Color(0x880A090F),
                            0.85f to Color(0xCC0A090F),
                            1.0f to Color(0xF50A090F)
                        )
                    )
                )
        )

        // ─── Layer 3: Accent Gradient Wash (subtle color from accent) ───
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x18FF0050), // RessoAccent at ~9% opacity
                            Color.Transparent
                        ),
                        center = Offset(0.5f, 0.3f),
                        radius = 900f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ─── Top Bar: Close, Track Info, Share Quote ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close",
                        tint = Color(0xBBFFFFFF),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Track Info - centered
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = track.title,
                        fontFamily = DinProFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = artistName.ifBlank { "Unknown Artist" },
                        fontFamily = DinProFamily,
                        fontSize = 12.sp,
                        color = Color(0x99FFFFFF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    IconButton(onClick = { showCalibration = !showCalibration }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Calibration",
                            tint = if (showCalibration) RessoAccent else Color(0x99FFFFFF),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = onShareLyric) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = "Share Lyric",
                            tint = Color(0x99FFFFFF),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // ─── Calibration Strip (toggled by tune icon) ───
            AnimatedVisibility(
                visible = showCalibration,
                enter = slideInVertically(tween(250)) { -it } + fadeIn(tween(250)),
                exit = slideOutVertically(tween(200)) { -it } + fadeOut(tween(200))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x25FFFFFF))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Sync",
                        fontFamily = DinProFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xAAFFFFFF)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LiquidGlassPill(
                            onClick = { onAdjustLyricsOffset(-500L) },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text("-0.5s", fontFamily = DinProFamily, fontSize = 11.sp, color = Color.White)
                        }
                        LiquidGlassPill(
                            onClick = onResetLyricsOffset,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            val sec = lyricsOffsetMs / 1000.0
                            Text(
                                text = "${if (sec >= 0) "+" else ""}${sec}s",
                                fontFamily = DinProFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RessoAccent
                            )
                        }
                        LiquidGlassPill(onClick = { onAdjustLyricsOffset(+500L) }) {
                            Text("+0.5s", fontFamily = DinProFamily, fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }

            // ─── Main Lyrics Area ───
            if (lines.isEmpty()) {
                // No lyrics state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(0x44FFFFFF),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No synchronized lyrics",
                            fontFamily = DinProFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0x66FFFFFF)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Lyrics aren't available for this song yet",
                            fontFamily = DinProFamily,
                            fontSize = 14.sp,
                            color = Color(0x44FFFFFF)
                        )
                    }
                }
            } else {
                // Synced Lyrics with aesthetic styling
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 24.dp,
                        end = 24.dp,
                        top = 120.dp,  // Large top padding so first lines can scroll to center
                        bottom = 120.dp // Large bottom padding for last lines
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(lines) { index, line ->
                        val isActive = index == activeIndex
                        val isPast = index < activeIndex
                        val distance = kotlin.math.abs(index - activeIndex)

                        // Animated properties for smooth transitions
                        val targetAlpha = when {
                            isActive -> 1f
                            isPast -> (0.35f - distance * 0.04f).coerceAtLeast(0.15f)
                            else -> (0.5f - distance * 0.05f).coerceAtLeast(0.2f)
                        }
                        val alpha by animateFloatAsState(
                            targetValue = targetAlpha,
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = FastOutSlowInEasing
                            ),
                            label = "LyricAlpha$index"
                        )

                        val targetScale = if (isActive) 1f else 0.92f
                        val scale by animateFloatAsState(
                            targetValue = targetScale,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "LyricScale$index"
                        )

                        val textColor by animateColorAsState(
                            targetValue = if (isActive) Color.White else Color(0xCCFFFFFF),
                            animationSpec = tween(350),
                            label = "LyricColor$index"
                        )

                        val fontSize = if (isActive) 28.sp else 22.sp
                        val fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(scale)
                                .alpha(alpha)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    line.startTimeMs
                                        .toLongOrNull()
                                        ?.let { onSeek(it) }
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = line.words,
                                fontFamily = DinProFamily,
                                fontSize = fontSize,
                                fontWeight = fontWeight,
                                color = textColor,
                                textAlign = TextAlign.Start,
                                lineHeight = if (isActive) 36.sp else 30.sp,
                                style = if (isActive) {
                                    TextStyle(
                                        shadow = Shadow(
                                            color = Color.White.copy(alpha = 0.28f),
                                            offset = Offset(0f, 0f),
                                            blurRadius = 16f
                                        )
                                    )
                                } else {
                                    TextStyle.Default
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // ─── Bottom Transport Controls (Glassmorphism) ───
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xCC0A090F)
                            )
                        )
                    )
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp)
            ) {
                // Scrubber
                Slider(
                    value = sliderPosition,
                    onValueChange = { frac ->
                        isUserDragging = true
                        sliderPosition = frac
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onValueChangeFinished = {
                        isUserDragging = false
                        onSeek((sliderPosition * effectiveDurationMs).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color(0x33FFFFFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Time indicators
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTimeMsCompact(currentPositionMs),
                        fontFamily = DinProFamily,
                        fontSize = 11.sp,
                        color = Color(0x88FFFFFF)
                    )
                    Text(
                        text = formatTimeMsCompact(effectiveDurationMs),
                        fontFamily = DinProFamily,
                        fontSize = 11.sp,
                        color = Color(0x88FFFFFF)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Transport buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPrevious()
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTogglePlayPause()
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNext()
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Compact time formatter for the lyrics screen scrubber. */
private fun formatTimeMsCompact(millis: Long): String {
    if (millis <= 0L) return "0:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val remainingSeconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%d:%02d", minutes, remainingSeconds)
}
