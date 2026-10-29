package com.kazatune.ui.skim

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.kazatune.ui.comments.CommentBottomSheet
import com.kazatune.ui.explore.ExploreView
import com.kazatune.ui.theme.DinProFamily
import com.kazatune.ui.theme.RessoAccent
import com.kazatune.viewmodel.CommentViewModel
import com.kazatune.viewmodel.SkimTab
import com.kazatune.viewmodel.SkimViewModel
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.canvas.CanvasResult
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
    val canvas by skimViewModel.currentCanvas.collectAsStateWithLifecycle()
    val likedVideoIds by skimViewModel.likedVideoIds.collectAsStateWithLifecycle()
    val downloadedVideoIds by skimViewModel.downloadedVideoIds.collectAsStateWithLifecycle()
    val downloadingVideoIds by skimViewModel.downloadingVideoIds.collectAsStateWithLifecycle()

    val comments by commentViewModel.comments.collectAsStateWithLifecycle()
    val isCommentsLoading by commentViewModel.isLoading.collectAsStateWithLifecycle()

    var showCommentsSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showMoreMenuSheet by remember { mutableStateOf(false) }
    var isFullscreenLyrics by remember { mutableStateOf(false) }

    val commentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val moreMenuSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
            delay(500)
            playPausePulseVisible = false
        }
    }

    val currentTrack = tracks.getOrNull(currentIndex)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Tab Content: For You vs Explore
        if (skimTab == SkimTab.EXPLORE) {
            ExploreView(
                skimViewModel = skimViewModel,
                onPlayTrack = { track ->
                    skimViewModel.playTrackAndStartRadio(track)
                    skimViewModel.selectSkimTab(SkimTab.FOR_YOU)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 56.dp)
            )
        } else {
            // For You: Vertical Pager Feed
            if (tracks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                            text = "Loading Skim...",
                            fontFamily = DinProFamily,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            } else {
                val pagerState = rememberPagerState(
                    initialPage = currentIndex.coerceIn(0, maxOf(0, tracks.size - 1)),
                    pageCount = { tracks.size }
                )

                LaunchedEffect(currentIndex) {
                    if (currentIndex in tracks.indices && pagerState.currentPage != currentIndex) {
                        pagerState.scrollToPage(currentIndex)
                    }
                }

                LaunchedEffect(pagerState.currentPage) {
                    if (pagerState.currentPage != currentIndex) {
                        skimViewModel.onPageSwiped(pagerState.currentPage)
                    }
                }

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
                        canvas = if (page == pagerState.currentPage) canvas else null,
                        isLiked = likedVideoIds.contains(track.videoId),
                        isDownloaded = downloadedVideoIds.contains(track.videoId),
                        isDownloading = downloadingVideoIds.contains(track.videoId),
                        isFullscreenLyrics = isFullscreenLyrics,
                        onToggleLyricsFullscreen = { isFullscreenLyrics = !isFullscreenLyrics },
                        onToggleLike = { skimViewModel.toggleLike(track) },
                        onToggleDownload = { skimViewModel.toggleDownload(track) },
                        onTogglePlayPause = { triggerPlayPause() },
                        onCommentsClick = {
                            commentViewModel.loadComments(track.videoId)
                            showCommentsSheet = true
                        },
                        onQueueClick = { showQueueSheet = true },
                        onShareClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Listening to ${track.title} on Skim: https://music.youtube.com/watch?v=${track.videoId}"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
                        },
                        onSeek = { positionMs -> skimViewModel.seekTo(positionMs) }
                    )
                }
            }
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

        // Top Navigation Header: [Skim (Refresh)] [For You | Explore] [More (...)]
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

            // Right: Useful Three-Dots Menu (Bug 14)
            IconButton(
                onClick = { showMoreMenuSheet = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color(0xCCFFFFFF),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }

    // 1. Comments Bottom Sheet
    if (showCommentsSheet) {
        CommentBottomSheet(
            sheetState = commentSheetState,
            comments = comments,
            isLoading = isCommentsLoading,
            onDismissRequest = { showCommentsSheet = false }
        )
    }

    // 2. Queue / Up Next Bottom Sheet (Bug 3 Fix)
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            sheetState = queueSheetState,
            containerColor = Color(0xFF141318),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Up Next (${tracks.size})",
                        fontFamily = DinProFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = { showQueueSheet = false }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(tracks) { idx, track ->
                        val isCurrent = idx == currentIndex
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrent) Color(0x33FF0050) else Color(0xFF1C1B24),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    skimViewModel.playTrackAt(idx)
                                    showQueueSheet = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = track.thumbnails?.lastOrNull()?.url,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        fontFamily = DinProFamily,
                                        fontSize = 14.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCurrent) RessoAccent else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.artists?.joinToString(", ") { it.name ?: "" } ?: "",
                                        fontFamily = DinProFamily,
                                        fontSize = 12.sp,
                                        color = Color(0x88FFFFFF),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isCurrent) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = RessoAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 3. More Actions Bottom Sheet (Bug 14)
    if (showMoreMenuSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreMenuSheet = false },
            sheetState = moreMenuSheetState,
            containerColor = Color(0xFF141318),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = currentTrack?.title ?: "Options",
                    fontFamily = DinProFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Start Song Radio
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            currentTrack?.let { skimViewModel.playTrackAndStartRadio(it) }
                            showMoreMenuSheet = false
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Radio, null, tint = RessoAccent, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Start Song Radio", fontFamily = DinProFamily, fontSize = 15.sp, color = Color.White)
                }

                // Sleep Timer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            /* Sleep timer */
                            showMoreMenuSheet = false
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AvTimer, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Sleep Timer (30 min)", fontFamily = DinProFamily, fontSize = 15.sp, color = Color.White)
                }

                // Share URL
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            currentTrack?.let { track ->
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${track.videoId}")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
                            }
                            showMoreMenuSheet = false
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Share Track Link", fontFamily = DinProFamily, fontSize = 15.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun SkimPageCard(
    track: Track,
    isPlaying: Boolean,
    currentPositionMs: Long,
    totalDurationMs: Long,
    lyrics: Lyrics?,
    canvas: CanvasResult?,
    isLiked: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    isFullscreenLyrics: Boolean,
    onToggleLyricsFullscreen: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleDownload: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onCommentsClick: () -> Unit,
    onQueueClick: () -> Unit,
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
        // 1. Background (Blurred Backdrop or Canvas Video)
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

        // 3. Main Content Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 56.dp, bottom = 86.dp)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // A. Top Synchronized Lyrics Banner (Clickable to expand fullscreen - Bug 9)
            if (!isFullscreenLyrics) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleLyricsFullscreen() }
                        .padding(top = 8.dp, bottom = 4.dp)
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
                        text = nextLyricText ?: artistName.ifBlank { "Skim" },
                        fontFamily = DinProFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0x99FFFFFF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // B. Center Content: Album Cover Card OR Fullscreen Lyrics (Bug 9)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isFullscreenLyrics) {
                    // Fullscreen Lyrics Overlay (Bug 9)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xAA141318))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lyrics",
                                fontFamily = DinProFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            IconButton(onClick = onToggleLyricsFullscreen) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Minimize Lyrics",
                                    tint = Color.White
                                )
                            }
                        }

                        if (lines.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No synchronized lyrics available for this song",
                                    fontFamily = DinProFamily,
                                    fontSize = 14.sp,
                                    color = Color(0x88FFFFFF)
                                )
                            }
                        } else {
                            val listState = rememberLazyListState()
                            LaunchedEffect(activeIndex) {
                                if (activeIndex >= 0) {
                                    listState.animateScrollToItem(maxOf(0, activeIndex - 2))
                                }
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                itemsIndexed(lines) { idx, line ->
                                    val isActive = idx == activeIndex
                                    Text(
                                        text = line.words,
                                        fontFamily = DinProFamily,
                                        fontSize = if (isActive) 22.sp else 16.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isActive) Color.White else Color(0x66FFFFFF),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val startMs = line.startTimeMs.toLongOrNull() ?: 0L
                                                onSeek(startMs)
                                            }
                                            .padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Album Cover Card
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
            }

            // C. Song Title & Artist (Follow button removed - Bug 1)
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
                Text(
                    text = artistName.ifBlank { "Unknown Artist" },
                    fontFamily = DinProFamily,
                    fontSize = 14.sp,
                    color = Color(0xCCFFFFFF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // D. Horizontal Action Bar (Bug 3 & 4 & 5 Fix)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Actions: Like, Comment, Share
                Row(
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like Button (Heart filled in pink when liked - Bug 5)
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

                // Right Actions: Download & Queue
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Download Button (Bug 4)
                    IconButton(
                        onClick = onToggleDownload,
                        modifier = Modifier.size(32.dp)
                    ) {
                        when {
                            isDownloading -> {
                                CircularProgressIndicator(
                                    color = RessoAccent,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            isDownloaded -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Downloaded",
                                    tint = RessoAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = Color(0xCCFFFFFF),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Queue Button (Opens Queue sheet, not comments! - Bug 3)
                    IconButton(
                        onClick = onQueueClick,
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

private fun formatTimeMs(millis: Long): String {
    if (millis <= 0L) return "0:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val remainingSeconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%d:%02d", minutes, remainingSeconds)
}
