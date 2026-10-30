package com.kazatune.ui.skim

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import com.kazatune.ui.theme.LiquidGlassCard
import com.kazatune.ui.theme.LiquidGlassColors
import com.kazatune.ui.theme.LiquidGlassPill
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
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kazatune.ui.comments.CommentBottomSheet
import com.kazatune.ui.explore.ExploreView
import com.kazatune.ui.player.SyncedLyricsOverlay
import com.kazatune.ui.player.VinylDisc
import com.kazatune.ui.theme.DinProFamily
import com.kazatune.ui.theme.RessoAccent
import com.kazatune.viewmodel.CommentViewModel
import com.kazatune.viewmodel.SkimTab
import com.kazatune.viewmodel.SkimViewModel
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.canvas.CanvasResult
import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.domain.data.model.ui.ScreenSizeInfo
import com.maxrave.media3.ui.MediaPlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

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
    val activeMood by skimViewModel.activeMood.collectAsStateWithLifecycle()
    val sponsorBlockToast by skimViewModel.sponsorBlockToast.collectAsStateWithLifecycle()
    val lyricsOffsetMs by skimViewModel.lyricsOffsetMs.collectAsStateWithLifecycle()
    val isRefreshing by skimViewModel.isRefreshing.collectAsStateWithLifecycle()
    val sleepTimerState by skimViewModel.sleepTimerState.collectAsStateWithLifecycle()

    val infiniteTransition = rememberInfiniteTransition(label = "RefreshRotation")
    val refreshRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RefreshSpin"
    )

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val comments by commentViewModel.comments.collectAsStateWithLifecycle()
    val isCommentsLoading by commentViewModel.isLoading.collectAsStateWithLifecycle()

    var showCommentsSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showMoreMenuSheet by remember { mutableStateOf(false) }
    var showLyricShareDialog by remember { mutableStateOf(false) }
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
        // Landscape / Tablet Split Player vs Standard Feed vs Explore vs Moods
        if (isLandscape && currentTrack != null) {
            SkimLandscapePlayer(
                track = currentTrack,
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                totalDurationMs = totalDurationMs,
                lyrics = lyrics,
                isLiked = likedVideoIds.contains(currentTrack.videoId),
                lyricsOffsetMs = lyricsOffsetMs,
                onTogglePlayPause = { triggerPlayPause() },
                onPrevious = { skimViewModel.skipToPrevious() },
                onNext = { skimViewModel.skipToNext() },
                onToggleLike = { skimViewModel.toggleLike(currentTrack) },
                onAdjustLyricsOffset = { delta -> skimViewModel.adjustLyricsOffset(delta) },
                onResetLyricsOffset = { skimViewModel.resetLyricsOffset() },
                onSeek = { pos -> skimViewModel.seekTo(pos) },
                onReload = { skimViewModel.reloadTrack(currentTrack) }
            )
        } else if (skimTab == SkimTab.EXPLORE) {
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
        } else if (skimTab == SkimTab.MOODS) {
            MoodsView(
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
                            text = "Loading Skim Music...",
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

                // Instantly sync pager to currently playing track whenever entering SkimScreen
                LaunchedEffect(Unit) {
                    val activeIndex = skimViewModel.currentIndex.value
                    if (activeIndex in tracks.indices && pagerState.currentPage != activeIndex) {
                        pagerState.scrollToPage(activeIndex)
                    }
                }

                // Sync pager to currentIndex changes (e.g., skip next/prev, queue tap)
                LaunchedEffect(currentIndex) {
                    if (currentIndex in tracks.indices && pagerState.currentPage != currentIndex) {
                        if (kotlin.math.abs(pagerState.currentPage - currentIndex) <= 1) {
                            pagerState.animateScrollToPage(currentIndex)
                        } else {
                            pagerState.scrollToPage(currentIndex)
                        }
                    }
                }

                // Smooth Settled Swipe: starts playback only when user completes swipe gesture
                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.settledPage }
                        .distinctUntilChanged()
                        .collect { settled ->
                            if (settled in tracks.indices && settled != currentIndex) {
                                skimViewModel.onPageSettled(settled)
                            }
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
                        onHookClick = { skimViewModel.jumpToHook(track) },
                        onCommentsClick = {
                            commentViewModel.loadComments(track.videoId)
                            showCommentsSheet = true
                        },
                        onQueueClick = { showQueueSheet = true },
                        onSeek = { positionMs -> skimViewModel.seekTo(positionMs) },
                        onReload = { skimViewModel.reloadTrack(track) }
                    )
                }
            }
        }

        // Floating SponsorBlock Notification Pill
        LaunchedEffect(sponsorBlockToast) {
            if (sponsorBlockToast != null) {
                delay(2500)
                skimViewModel.dismissSponsorBlockToast()
            }
        }

        AnimatedVisibility(
            visible = sponsorBlockToast != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 56.dp)
        ) {
            LiquidGlassPill(
                onClick = { skimViewModel.dismissSponsorBlockToast() },
                backgroundColor = Color(0x70181726),
                borderBrush = LiquidGlassColors.GlassAccentBorderBrush
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color(0xFFFFD600),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = sponsorBlockToast ?: "",
                        fontFamily = DinProFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
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
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer {
                                rotationZ = if (isRefreshing) refreshRotation else 0f
                            }
                    )
                }

                // Center: "For You" | "Explore" | "Moods" Tab Toggle
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
                            .padding(horizontal = 10.dp, vertical = 5.dp),
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
                            .padding(horizontal = 10.dp, vertical = 5.dp),
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

                    // "Moods"
                    val moodsSelected = skimTab == SkimTab.MOODS
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (moodsSelected) Color(0x44FFFFFF) else Color.Transparent)
                            .clickable { skimViewModel.selectSkimTab(SkimTab.MOODS) }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Moods",
                            fontFamily = DinProFamily,
                            fontSize = 13.sp,
                            fontWeight = if (moodsSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (moodsSelected) Color.White else Color(0x88FFFFFF)
                        )
                    }
                }

                // Right: Options Menu
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

    // 2. Queue / Up Next Bottom Sheet
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

    // 3. More Actions Bottom Sheet
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

                // Share Lyric Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showMoreMenuSheet = false
                            showLyricShareDialog = true
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FormatQuote, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Share Lyric Quote Card", fontFamily = DinProFamily, fontSize = 15.sp, color = Color.White)
                }

                // Sleep Timer
                val sleepRemaining = sleepTimerState.timeRemaining
                val isSleepTimerActive = sleepRemaining > 0 || sleepRemaining == -1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isSleepTimerActive) {
                                skimViewModel.setSleepTimer(0)
                            } else {
                                skimViewModel.setSleepTimer(30)
                            }
                            showMoreMenuSheet = false
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AvTimer, null, tint = RessoAccent, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = when {
                            sleepRemaining == -1 -> "Sleep Timer: End of song (Tap to stop)"
                            sleepRemaining > 0 -> "Sleep Timer: ${sleepRemaining}m left (Tap to stop)"
                            else -> "Sleep Timer (30 min)"
                        },
                        fontFamily = DinProFamily,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }

                // Share URL
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            currentTrack?.let { track ->
                                val artist = track.artists?.firstOrNull()?.name ?: "Unknown Artist"
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Listening to \"${track.title}\" by $artist on Skim Music 🎵\nDownload Skim: https://github.com/Roixal/Kazatune/releases/tag/Stable"
                                    )
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

    // 4. Component 8: Lyric Quote Card Generator
    if (showLyricShareDialog && currentTrack != null) {
        LyricShareDialog(
            track = currentTrack,
            lyrics = lyrics,
            onDismissRequest = { showLyricShareDialog = false }
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
    canvas: CanvasResult?,
    isLiked: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    isFullscreenLyrics: Boolean,
    onToggleLyricsFullscreen: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleDownload: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onHookClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onQueueClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onReload: () -> Unit = {}
) {
    val artworkUrl = track.thumbnails?.lastOrNull()?.url
    val artistName = track.artists?.joinToString(", ") { it.name ?: "" } ?: ""
    val haptic = LocalHapticFeedback.current

    var doubleTapHeartOffset by remember { mutableStateOf<Offset?>(null) }

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
        modifier = Modifier.fillMaxSize()
    ) {
        // Component 2: Background (Spotify Canvas Video Loop vs Downscaled 60fps Ambient Backdrop)
        val canvasUrl = canvas?.canvasUrl
        val isCanvasVideo = canvas?.isVideo == true && !canvasUrl.isNullOrBlank()
        val context = LocalContext.current
        val density = LocalDensity.current
        val configuration = LocalConfiguration.current
        val screenSize = remember(configuration, density) {
            ScreenSizeInfo(
                hDP = configuration.screenHeightDp,
                wDP = configuration.screenWidthDp,
                hPX = with(density) { configuration.screenHeightDp.dp.roundToPx() },
                wPX = with(density) { configuration.screenWidthDp.dp.roundToPx() }
            )
        }

        if (isCanvasVideo && isPlaying) {
            MediaPlayerView(
                context = context,
                density = density,
                url = canvasUrl!!,
                screenSize = screenSize,
                modifier = Modifier.fillMaxSize(),
                cropToBounds = true
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artworkUrl)
                    .size(120, 120) // Downsample to 120x120 for 99% GPU blur speedup
                    .crossfade(true)
                    .build(),
                contentDescription = "Ambient Backdrop",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(24.dp)
            )
        }

        // Dark Vignette Overlays
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

        // Main Content Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 96.dp, bottom = 86.dp)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // A. Top Synchronized Lyrics Banner (Clickable to expand fullscreen - Apple Liquid Glass)
            if (!isFullscreenLyrics) {
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleLyricsFullscreen() },
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = Color(0x351E1C2C),
                    borderBrush = LiquidGlassColors.GlassBorderBrush
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = currentLyricText ?: track.title,
                            fontFamily = DinProFamily,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 26.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = nextLyricText ?: artistName.ifBlank { "Skim Music" },
                            fontFamily = DinProFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0x99FFFFFF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // B. Center Content: Album Cover Card OR Component 1 Interactive Karaoke Lyrics
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isFullscreenLyrics) {
                    // Fullscreen Karaoke Lyrics Overlay
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xAA141318))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lyrics (Tap line to seek)",
                                fontFamily = DinProFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            IconButton(onClick = onToggleLyricsFullscreen) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White
                                )
                            }
                        }

                        SyncedLyricsOverlay(
                            lyrics = lyrics,
                            currentPositionMs = currentPositionMs,
                            onLineClick = onSeek,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    // Center Album Cover Card (Tapping toggles Play/Pause, Double-tap Likes)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1E1D24),
                        shadowElevation = 16.dp,
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .aspectRatio(1f)
                            .pointerInput(track.videoId) {
                                detectTapGestures(
                                    onTap = { onTogglePlayPause() },
                                    onDoubleTap = { offset ->
                                        doubleTapHeartOffset = offset
                                        if (!isLiked) {
                                            onToggleLike()
                                        }
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                )
                            }
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

            // C. Song Title & Artist + Rotating Vinyl Disc
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
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

                Spacer(modifier = Modifier.width(12.dp))

                VinylDisc(
                    artworkUrl = artworkUrl,
                    isPlaying = isPlaying,
                    onClick = onReload,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // D. Horizontal Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Actions: Like, Comment, Share, Hook
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
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
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Like",
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
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Chat",
                            fontFamily = DinProFamily,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }

                    // Component 4: 🎯 Hook / Chorus Instant Jump Chip
                    LiquidGlassPill(
                        onClick = onHookClick,
                        backgroundColor = Color(0x33FF0050),
                        borderBrush = LiquidGlassColors.GlassAccentBorderBrush
                    ) {
                        Text(
                            text = "🎯 Hook",
                            fontFamily = DinProFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RessoAccent
                        )
                    }
                }

                // Right Actions: Download & Queue
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Download Button
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

                    // Queue Button
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

            Spacer(modifier = Modifier.height(12.dp))

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

        // Component 3: Double-Tap Bouncy Heart Burst Animation
        doubleTapHeartOffset?.let { offset ->
            DoubleTapHeartBurst(
                offset = offset,
                onDismiss = { doubleTapHeartOffset = null }
            )
        }
    }
}

// Component 3: TikTok-Style Double-Tap Heart Burst
@Composable
private fun DoubleTapHeartBurst(
    offset: Offset,
    onDismiss: () -> Unit
) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1.3f, spring(dampingRatio = 0.5f, stiffness = 600f))
        scale.animateTo(1.0f, tween(100))
        alpha.animateTo(0f, tween(250))
        onDismiss()
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offset.x.roundToInt() - 40, offset.y.roundToInt() - 40) }
            .scale(scale.value)
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            tint = Color(0xFFFF0050).copy(alpha = alpha.value),
            modifier = Modifier.size(80.dp)
        )
    }
}

private fun formatTimeMs(millis: Long): String {
    if (millis <= 0L) return "0:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val remainingSeconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%d:%02d", minutes, remainingSeconds)
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", count / 1_000_000f)
        count >= 1_000 -> String.format(java.util.Locale.US, "%.1fk", count / 1_000f)
        else -> count.toString()
    }
}

/**
 * Dedicated Landscape & Tablet Player:
 * Horizontal Apple Liquid Glass layout with Vinyl disc, audio scrubber, timing offset calibration,
 * and word-by-word synced scrolling lyrics.
 */
@Composable
fun SkimLandscapePlayer(
    track: Track,
    isPlaying: Boolean,
    currentPositionMs: Long,
    totalDurationMs: Long,
    lyrics: Lyrics?,
    isLiked: Boolean,
    lyricsOffsetMs: Long,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleLike: () -> Unit,
    onAdjustLyricsOffset: (Long) -> Unit,
    onResetLyricsOffset: () -> Unit,
    onSeek: (Long) -> Unit,
    onReload: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val artworkUrl = track.thumbnails?.lastOrNull()?.url
    val artistName = track.artists?.joinToString(", ") { it.name ?: "" } ?: ""
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

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Ambient backdrop
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artworkUrl)
                .size(140, 140)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(28.dp)
        )

        // Dark gradient vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xEE0D0C13), Color(0xCC0D0C13), Color(0xF50D0C13))
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // LEFT PANE: Vinyl, Metadata, Controls & Timing Calibration
            LiquidGlassCard(
                modifier = Modifier
                    .weight(0.48f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = Color(0x35191825),
                borderBrush = LiquidGlassColors.GlassBorderBrush
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Vinyl Disc
                    VinylDisc(
                        artworkUrl = artworkUrl,
                        isPlaying = isPlaying,
                        onClick = onReload,
                        modifier = Modifier.size(110.dp)
                    )

                    // Track Title & Artist
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = track.title,
                            fontFamily = DinProFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = artistName.ifBlank { "Unknown Artist" },
                            fontFamily = DinProFamily,
                            fontSize = 13.sp,
                            color = Color(0x99FFFFFF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Scrubber
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = sliderPosition,
                            onValueChange = {
                                isUserDragging = true
                                sliderPosition = it
                            },
                            onValueChangeFinished = {
                                isUserDragging = false
                                onSeek((sliderPosition * effectiveDurationMs).toLong())
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = RessoAccent,
                                activeTrackColor = RessoAccent,
                                inactiveTrackColor = Color(0x30FFFFFF)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatTimeMs(currentPositionMs), fontFamily = DinProFamily, fontSize = 11.sp, color = Color(0x88FFFFFF))
                            Text(formatTimeMs(effectiveDurationMs), fontFamily = DinProFamily, fontSize = 11.sp, color = Color(0x88FFFFFF))
                        }
                    }

                    // Transport Row: Prev, Play/Pause, Next, Like, Dislike
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPrevious) {
                            Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        IconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        IconButton(onClick = onNext) {
                            Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        IconButton(onClick = onToggleLike) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isLiked) RessoAccent else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Timing Offset Calibration Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x25FFFFFF))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Sync Calibrate",
                            fontFamily = DinProFamily,
                            fontSize = 11.sp,
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
                                Text("${if (sec >= 0) "+" else ""}${sec}s", fontFamily = DinProFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RessoAccent)
                            }
                            LiquidGlassPill(
                                onClick = { onAdjustLyricsOffset(+500L) }
                            ) {
                                Text("+0.5s", fontFamily = DinProFamily, fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // RIGHT PANE: Synced Lyrics Overlay with Calibrated Offset
            LiquidGlassCard(
                modifier = Modifier
                    .weight(0.52f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = Color(0x35191825),
                borderBrush = LiquidGlassColors.GlassBorderBrush
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Synchronized Lyrics",
                        fontFamily = DinProFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SyncedLyricsOverlay(
                        lyrics = lyrics,
                        currentPositionMs = currentPositionMs + lyricsOffsetMs,
                        onLineClick = onSeek,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Dedicated Moods & Vibes Section alongside For You and Explore.
 */
@Composable
fun MoodsView(
    skimViewModel: SkimViewModel,
    onPlayTrack: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeMood by skimViewModel.activeMood.collectAsStateWithLifecycle()
    val moodTracks by skimViewModel.moodTracks.collectAsStateWithLifecycle()
    val isMoodLoading by skimViewModel.isMoodLoading.collectAsStateWithLifecycle()

    val moods = listOf(
        "⚡ All" to "Trending beats across all energizing tracks",
        "⚡ Workout" to "High-tempo hits to power your workout & cardio",
        "🌙 Chill" to "Smooth, laid-back melodies to relax and unwind",
        "🔥 Trending" to "Viral music setting the charts on fire right now",
        "💔 Sad" to "Deep emotional tracks and heartfelt ballads",
        "☕ Focus" to "Ambient, lo-fi and deep work background rhythms",
        "🎉 Party" to "Dance, club and upbeat anthems to ignite the room",
        "🎧 Study" to "Calm instrumental soundscapes for learning",
        "🚗 Road Trip" to "Sing-along road trip anthems for the open road",
        "😴 Sleep" to "Gentle soothing frequencies for restful nights"
    )

    LaunchedEffect(Unit) {
        if (moodTracks.isEmpty()) {
            skimViewModel.loadMoodTracks(activeMood)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Moods Selector Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(moods) { (moodName, _) ->
                val isSelected = moodName == activeMood
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) RessoAccent else Color(0x33252433),
                    modifier = Modifier.clickable {
                        skimViewModel.selectMood(moodName)
                    }
                ) {
                    Text(
                        text = moodName,
                        fontFamily = DinProFamily,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xCCFFFFFF),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Active Mood Card Banner
        val currentDesc = moods.find { it.first == activeMood }?.second ?: "Handpicked mood collection"
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            backgroundColor = LiquidGlassColors.GlassSurfaceDark,
            borderBrush = LiquidGlassColors.GlassAccentBorderBrush
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeMood,
                        fontFamily = DinProFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentDesc,
                        fontFamily = DinProFamily,
                        fontSize = 12.sp,
                        color = Color(0xAAFFFFFF),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                if (moodTracks.isNotEmpty()) {
                    LiquidGlassPill(
                        onClick = {
                            skimViewModel.setTracksAndPlay(moodTracks, 0)
                            skimViewModel.selectSkimTab(SkimTab.FOR_YOU)
                        },
                        backgroundColor = RessoAccent,
                        borderBrush = LiquidGlassColors.GlassAccentBorderBrush
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play All", fontFamily = DinProFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isMoodLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = RessoAccent, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
            }
        } else if (moodTracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No songs found for this mood.",
                    fontFamily = DinProFamily,
                    fontSize = 14.sp,
                    color = Color(0x88FFFFFF)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                itemsIndexed(moodTracks) { index, track ->
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                skimViewModel.setTracksAndPlay(moodTracks, index)
                                skimViewModel.selectSkimTab(SkimTab.FOR_YOU)
                            },
                        shape = RoundedCornerShape(14.dp),
                        backgroundColor = Color(0x221F1E28),
                        borderBrush = LiquidGlassColors.GlassBorderBrush,
                        borderWidth = 0.5.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val thumb = track.thumbnails?.lastOrNull()?.url
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF2C2B33)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!thumb.isNullOrBlank()) {
                                    AsyncImage(
                                        model = thumb,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(50.dp)
                                    )
                                } else {
                                    Icon(Icons.Default.MusicNote, null, tint = RessoAccent, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    fontFamily = DinProFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = track.artists?.joinToString(", ") { it.name ?: "" } ?: "Unknown Artist",
                                    fontFamily = DinProFamily,
                                    fontSize = 12.sp,
                                    color = Color(0xAAFFFFFF),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = RessoAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
