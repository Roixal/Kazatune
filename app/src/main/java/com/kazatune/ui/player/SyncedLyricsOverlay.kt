package com.kazatune.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazatune.ui.theme.DinProFamily
import com.maxrave.domain.data.model.metadata.Lyrics
import kotlin.math.abs

@Composable
fun SyncedLyricsOverlay(
    lyrics: Lyrics?,
    currentPositionMs: Long,
    onLineClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
    activeFontSize: TextUnit = 21.sp,
    inactiveFontSize: TextUnit = 16.sp,
    contentPadding: PaddingValues = PaddingValues(vertical = 24.dp),
    scrollOffset: Int = -120
) {
    val lines = lyrics?.lines ?: emptyList()
    if (lines.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No synchronized lyrics found",
                fontFamily = DinProFamily,
                fontSize = 15.sp,
                color = Color(0x77FFFFFF)
            )
        }
        return
    }

    // Find the current active line based on playback timestamp
    val activeIndex by remember(lines, currentPositionMs) {
        derivedStateOf {
            val idx = lines.indexOfLast { line ->
                (line.startTimeMs.toLongOrNull() ?: 0L) <= currentPositionMs
            }
            if (idx >= 0) idx else 0
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex) {
        if (activeIndex in lines.indices) {
            listState.animateScrollToItem(
                index = activeIndex,
                scrollOffset = scrollOffset
            )
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            horizontalAlignment = if (textAlign == TextAlign.Start) Alignment.Start else Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(lines) { index, line ->
                val isActive = index == activeIndex
                val distance = abs(index - activeIndex)
                val targetAlpha = when {
                    isActive -> 1f
                    index < activeIndex -> (0.38f - distance * 0.04f).coerceAtLeast(0.16f)
                    else -> (0.52f - distance * 0.05f).coerceAtLeast(0.20f)
                }
                val textColor by animateColorAsState(
                    targetValue = if (isActive) Color.White else Color(0xCCFFFFFF),
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                    label = "LyricColor"
                )
                val textAlpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
                    label = "LyricAlpha"
                )
                val lineScale by animateFloatAsState(
                    targetValue = if (isActive) 1f else 0.94f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "LyricScale"
                )
                val fontSize = if (isActive) activeFontSize else inactiveFontSize
                val fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold

                Text(
                    text = line.words,
                    fontFamily = DinProFamily,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    color = textColor,
                    textAlign = textAlign,
                    lineHeight = (fontSize.value * 1.28f).sp,
                    style = if (isActive) {
                        TextStyle(
                            shadow = Shadow(
                                color = Color.White.copy(alpha = 0.25f),
                                offset = Offset(0f, 0f),
                                blurRadius = 14f
                            )
                        )
                    } else {
                        TextStyle.Default
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (textAlign == TextAlign.Start) 16.dp else 24.dp)
                        .graphicsLayer {
                            scaleX = lineScale
                            scaleY = lineScale
                            transformOrigin = if (textAlign == TextAlign.Start) {
                                TransformOrigin(0f, 0.5f)
                            } else {
                                TransformOrigin.Center
                            }
                        }
                        .alpha(textAlpha)
                        .clickable {
                            line.startTimeMs.toLongOrNull()?.let { onLineClick(it) }
                        }
                )
            }
        }
    }
}
