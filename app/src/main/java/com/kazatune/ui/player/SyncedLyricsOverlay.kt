package com.kazatune.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazatune.ui.theme.DinProFamily
import com.maxrave.domain.data.model.metadata.Lyrics

@Composable
fun SyncedLyricsOverlay(
    lyrics: Lyrics?,
    currentPositionMs: Long,
    onLineClick: (Long) -> Unit,
    modifier: Modifier = Modifier
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
                scrollOffset = -120
            )
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(lines) { index, line ->
                val isActive = index == activeIndex
                val textColor by animateColorAsState(
                    targetValue = if (isActive) Color.White else Color(0x66FFFFFF),
                    animationSpec = tween(durationMillis = 300),
                    label = "LyricColor"
                )
                val textAlpha by animateFloatAsState(
                    targetValue = if (isActive) 1f else 0.45f,
                    animationSpec = tween(durationMillis = 300),
                    label = "LyricAlpha"
                )
                val fontSize = if (isActive) 21.sp else 16.sp
                val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium

                Text(
                    text = line.words,
                    fontFamily = DinProFamily,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .alpha(textAlpha)
                        .clickable {
                            line.startTimeMs.toLongOrNull()?.let { onLineClick(it) }
                        }
                )
            }
        }
    }
}
