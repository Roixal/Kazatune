package com.kazatune.ui.skim

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.kazatune.ui.comments.CommentBottomSheet
import com.kazatune.ui.theme.DinProFamily
import com.kazatune.ui.theme.RessoAccent
import com.kazatune.viewmodel.CommentViewModel
import com.kazatune.viewmodel.SkimTab
import com.kazatune.viewmodel.SkimViewModel
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.metadata.Lyrics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkimScreen(
    skimViewModel: SkimViewModel = koinViewModel(),
    commentViewModel: CommentViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val tracks by skimViewModel.tracks.collectAsStateWithLifecycle()
    val skimTab by skimViewModel.skimTab.collectAsStateWithLifecycle()
    val currentIndex by skimViewModel.currentIndex.collectAsStateWithLifecycle()
    val controlState by skimViewModel.controlState.collectAsStateWithLifecycle()
    val isPlaying = controlState.isPlaying

    val currentPositionMs by skimViewModel.currentPositionMs.collectAsStateWithLifecycle()
    val totalDurationMs by skimViewModel.totalDurationMs.collectAsStateWithLifecycle()
    val lyrics by skimViewModel.currentLyrics.collectAsStateWithLifecycle()
    val likedVideoIds by skimViewModel.likedVideoIds.collectAsStateWithLifecycle()

    val comments by commentViewModel.comments.collectAsStateWithLifecycle()
    val isCommentsLoading by commentViewModel.isLoading.collectAsStateWithLifecycle()

    var showCommentsSheet by remember { mutableStateOf(false) }
    val commentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    // Feedback pulse animation on Play/Pause
    var playPausePulseVisible by remember { mutableStateOf(false) }
    var playPausePulseIcon by remember { mutableStateOf(Icons.Default.PlayArrow) }
    val coroutineScope = rememberCoroutineScope()

    fun triggerPlayPause() {
        playPausePulseIcon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
        playPausePulseVisible = true
        skimViewModel.togglePlayPause()
        coroutineScope.launch {
            delay(600)
            playPausePulseVisible = false
        }
    }

    if (tracks.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    color = RessoAccent,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading Skim Music Feed...",
                    fontFamily = DinProFamily,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = currentIndex.coerceIn(0, maxOf(0, tracks.size - 1)),
        pageCount = { tracks.size }
    )

    // Sync pager when currentIndex changes externally (e.g. from Search or Radio)
    LaunchedEffect(currentIndex) {
        if (currentIndex in tracks.indices && pagerState.currentPage != currentIndex) {
            pagerState.scrollToPage(currentIndex)
        }
    }

    // Auto-sync playback when user swipes to a new page
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != currentIndex) {
            skimViewModel.onPageSwiped(pagerState.currentPage)
        }
    }

    val currentTrack = tracks.getOrNull(pagerState.currentPage)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val track = tracks[page]
            SkimPageCard(
                track = track,
                isPlaying = isPlaying && page == pagerState.currentPage,
                currentPositionMs = currentPositionMs,
                totalDurationMs = totalDurationMs,
                lyrics = if (page == pagerState.currentPage) lyrics else null,
                isLiked = likedVideoIds.contains(track.videoId),
                onToggleLike = { skimViewModel.toggleLike(track) },
                onTogglePlayPause = { triggerPlayPause() },
                onCommentsClick = {
                    commentViewModel.loadComments(track.videoId)
                    showCommentsSheet = true
                },
                onShareClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Listening to ${track.title} on Kazatune: https://music.youtube.com/watch?v=${track.videoId}"
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
                },
                onSeek = { positionMs -> skimViewModel.seekTo(positionMs) }
            )
        }

        // Animated Play/Pause Center Splash Indicator
        AnimatedVisibility(
            visible = playPausePulseVisible,
            enter = scaleIn(initialScale = 0.7f) + fadeIn(),
            exit = scaleOut(targetScale = 1.3f) + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0x88000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = playPausePulseIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // Top Navigation Header: [Skim (Refresh)] [For You | Explore] [More]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: "Skim" refresh button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { skimViewModel.refreshFeed() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Skim",
                    fontFamily = DinProFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = RessoAccent,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Center: "For You" | "Explore" Tab Toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x33000000))
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "For You"
                val forYouSelected = skimTab == SkimTab.FOR_YOU
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (forYouSelected) Color(0x44FFFFFF) else Color.Transparent)
                        .clickable { skimViewModel.selectSkimTab(SkimTab.FOR_YOU) }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "For You",
                        fontFamily = DinProFamily,
                        fontSize = 13.sp,
                        fontWeight = if (forYouSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (forYouSelected) Color.White else Color(0x88FFFFFF)
                    )
                }

                // "Explore"
                val exploreSelected = skimTab == SkimTab.EXPLORE
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (exploreSelected) Color(0x44FFFFFF) else Color.Transparent)
                        .clickable { skimViewModel.selectSkimTab(SkimTab.EXPLORE) }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Explore",
                        fontFamily = DinProFamily,
                        fontSize = 13.sp,
                        fontWeight = if (exploreSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (exploreSelected) Color.White else Color(0x88FFFFFF)
                    )
                }
            }

            // Right: Subtle Options Menu
            IconButton(
                onClick = { /* More actions */ },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = Color(0xCCFFFFFF),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // Lyric Comments Sheet
    if (showCommentsSheet) {
        CommentBottomSheet(
            sheetState = commentSheetState,
            comments = comments,
            isLoading = isCommentsLoading,
            onDismissRequest = { showCommentsSheet = false }
        )
    }
}

@Composable
fun SkimPageCard(
    track: Track,
    isPlaying: Boolean,
    currentPositionMs: Long,
    totalDurationMs: Long,
    lyrics: Lyrics?,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onCommentsClick: () -> Unit,
    onShareClick: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val artworkUrl = track.thumbnails?.lastOrNull()?.url
    val artistName = track.artists?.joinToString(", ") { it.name ?: "" } ?: ""

    // Calculate active synchronized lyric line
    val lines = lyrics?.lines ?: emptyList()
    val activeIndex = lines.indexOfLast { line ->
        val start = line.startTimeMs.toLongOrNull() ?: 0L
        currentPositionMs >= start
    }
    val currentLyricText = if (activeIndex >= 0) lines[activeIndex].words else null
    val nextLyricText = if (activeIndex >= 0 && activeIndex + 1 < lines.size) lines[activeIndex + 1].words else null

    // Effective Duration in Milliseconds
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTogglePlayPause
            )
    ) {
        // 1. High-Res Blurred Ambient Backdrop
        AsyncImage(
            model = artworkUrl,
            contentDescription = "Ambient Backdrop",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(48.dp)
        )

        // 2. Dark Vignette Overlays
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x99000000),
                            Color(0x33000000),
                            Color(0x66000000),
                            Color(0xF5000000)
                        )
                    )
                )
        )

        // 3. Main Content Column (Matching Resso Screenshot)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 56.dp, bottom = 86.dp)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // A. Top Floating Lyrics Display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = currentLyricText ?: track.title,
                    fontFamily = DinProFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 28.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = nextLyricText ?: artistName.ifBlank { "Kazatune" },
                    fontFamily = DinProFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0x99FFFFFF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // B. Center Album Cover Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E1D24),
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .aspectRatio(1f)
                ) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // C. Song Title, Artist & Follow Pill
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = track.title,
                    fontFamily = DinProFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = artistName.ifBlank { "Unknown Artist" },
                        fontFamily = DinProFamily,
                        fontSize = 14.sp,
                        color = Color(0xCCFFFFFF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x33FFFFFF))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Follow",
                            fontFamily = DinProFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // D. Horizontal Action Bar (Like, Comment, Share | Download, Queue)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like Button
                    val heartColor by animateColorAsState(
                        targetValue = if (isLiked) RessoAccent else Color.White,
                        label = "HeartColor"
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onToggleLike() }
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = heartColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isLiked) "1" else "Like",
                            fontFamily = DinProFamily,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }

                    // Comments Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onCommentsClick() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = "Comments",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Chat",
                            fontFamily = DinProFamily,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }

                    // Share Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onShareClick() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Share",
                            fontFamily = DinProFamily,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }

                // Right Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { /* Offline download toggle */ },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = Color(0xCCFFFFFF),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { onCommentsClick() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Queue",
                            tint = Color(0xCCFFFFFF),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // E. Playback Controls & Working Audio Scrubber
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause Button with Toggle Scale Animation
                val playButtonScale by animateFloatAsState(
                    targetValue = if (isPlaying) 1f else 1.08f,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = "PlayScale"
                )

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(42.dp)
                        .scale(playButtonScale)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Interactive Audio Scrubber & Live Time Indicator
                Column(modifier = Modifier.weight(1f)) {
                    Slider(
                        value = sliderPosition,
                        onValueChange = { frac ->
                            isUserDragging = true
                            sliderPosition = frac
                        },
                        onValueChangeFinished = {
                            isUserDragging = false
                            val seekTargetMs = (sliderPosition * effectiveDurationMs).toLong()
                            onSeek(seekTargetMs)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = RessoAccent,
                            activeTrackColor = RessoAccent,
                            inactiveTrackColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTimeMs(currentPositionMs),
                            fontFamily = DinProFamily,
                            fontSize = 11.sp,
                            color = Color(0x88FFFFFF)
                        )
                        Text(
                            text = formatTimeMs(effectiveDurationMs),
                            fontFamily = DinProFamily,
                            fontSize = 11.sp,
                            color = Color(0x88FFFFFF)
                        )
                    }
                }
            }
        }
    }
}

fun formatTimeMs(timeMs: Long): String {
    if (timeMs <= 0) return "00:00"
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
