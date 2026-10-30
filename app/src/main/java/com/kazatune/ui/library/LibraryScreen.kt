package com.kazatune.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kazatune.ui.taste.TasteBottomSheet
import com.kazatune.ui.theme.LiquidGlassCard
import com.kazatune.ui.theme.LiquidGlassColors
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.kazatune.ui.theme.DinProFamily
import com.kazatune.ui.theme.RessoAccent
import com.kazatune.viewmodel.LibraryTab
import com.kazatune.viewmodel.LibraryViewModel
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.utils.toTrack
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel = koinViewModel(),
    onPlayTrack: (Track) -> Unit = { /* default */ },
    onPlayTracks: (List<Track>, Int) -> Unit = { list, idx -> if (list.isNotEmpty()) onPlayTrack(list[idx]) },
    modifier: Modifier = Modifier
) {
    val selectedTab by libraryViewModel.selectedTab.collectAsStateWithLifecycle()
    val likedSongs by libraryViewModel.likedSongs.collectAsStateWithLifecycle()
    val downloadedSongs by libraryViewModel.downloadedSongs.collectAsStateWithLifecycle()
    val localPlaylists by libraryViewModel.localPlaylists.collectAsStateWithLifecycle()
    val ytPlaylists by libraryViewModel.ytPlaylists.collectAsStateWithLifecycle()
    val isLikedSongsBannerOpen by libraryViewModel.isLikedSongsBannerOpen.collectAsStateWithLifecycle()
    val isDownloadedSongsBannerOpen by libraryViewModel.isDownloadedSongsBannerOpen.collectAsStateWithLifecycle()

    var showTasteSheet by remember { mutableStateOf(false) }
    var showMigrationDialog by remember { mutableStateOf(false) }
    var loadingYtPlaylistId by remember { mutableStateOf<String?>(null) }
    val tasteSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Handle back button when either banner view is open
    BackHandler(enabled = isLikedSongsBannerOpen || isDownloadedSongsBannerOpen) {
        if (isLikedSongsBannerOpen) {
            libraryViewModel.closeLikedSongsBanner()
        } else {
            libraryViewModel.closeDownloadedSongsBanner()
        }
    }

    if (isLikedSongsBannerOpen) {
        // Bug 11: All liked songs inside the expanded banner view
        LikedSongsBannerExpandedView(
            likedSongs = likedSongs,
            onBack = { libraryViewModel.closeLikedSongsBanner() },
            onPlayAll = {
                if (likedSongs.isNotEmpty()) {
                    onPlayTracks(likedSongs.map { it.toTrack() }, 0)
                }
            },
            onShuffleAll = {
                if (likedSongs.isNotEmpty()) {
                    onPlayTracks(likedSongs.map { it.toTrack() }.shuffled(), 0)
                }
            },
            onPlayTrackAtIndex = { idx ->
                if (likedSongs.isNotEmpty()) {
                    onPlayTracks(likedSongs.map { it.toTrack() }, idx)
                }
            },
            modifier = modifier
        )
    } else if (isDownloadedSongsBannerOpen) {
        // Dedicated Downloaded Songs Expanded Banner View
        DownloadedSongsBannerExpandedView(
            downloadedSongs = downloadedSongs,
            onBack = { libraryViewModel.closeDownloadedSongsBanner() },
            onPlayAll = {
                if (downloadedSongs.isNotEmpty()) {
                    onPlayTracks(downloadedSongs.map { it.toTrack() }, 0)
                }
            },
            onShuffleAll = {
                if (downloadedSongs.isNotEmpty()) {
                    onPlayTracks(downloadedSongs.map { it.toTrack() }.shuffled(), 0)
                }
            },
            onPlayTrackAtIndex = { idx ->
                if (downloadedSongs.isNotEmpty()) {
                    onPlayTracks(downloadedSongs.map { it.toTrack() }, idx)
                }
            },
            onDeleteSong = { song -> libraryViewModel.deleteDownload(song.videoId) },
            onDeleteAll = { libraryViewModel.deleteAllDownloads() },
            modifier = modifier
        )
    } else {
        // Main Library overview
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0F0E13))
                .statusBarsPadding()
                .padding(top = 12.dp)
        ) {
            // Screen Header
            Text(
                text = "Your Library",
                fontFamily = DinProFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            // Tab Row Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(LibraryTab.entries) { tab ->
                    val isSelected = tab == selectedTab
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) RessoAccent else Color(0xFF201F28),
                        modifier = Modifier.clickable { libraryViewModel.selectTab(tab) }
                    ) {
                        Text(
                            text = tab.name.lowercase().replaceFirstChar { it.uppercase() },
                            fontFamily = DinProFamily,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xAAFFFFFF),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Body Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // AI Music Taste & Playlist Migration Liquid Glass Quick Actions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // AI Music Taste Profile Card
                        LiquidGlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showTasteSheet = true },
                            shape = RoundedCornerShape(18.dp),
                            backgroundColor = Color(0x35232235),
                            borderBrush = LiquidGlassColors.GlassBorderBrush
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = RessoAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "AI Taste",
                                        fontFamily = DinProFamily,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Sonic DNA & Recs",
                                    fontFamily = DinProFamily,
                                    fontSize = 11.sp,
                                    color = Color(0x99FFFFFF)
                                )
                            }
                        }

                        // Playlist Migration Card
                        LiquidGlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showMigrationDialog = true },
                            shape = RoundedCornerShape(18.dp),
                            backgroundColor = Color(0x35232235),
                            borderBrush = LiquidGlassColors.GlassBorderBrush
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SyncAlt,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Migration",
                                        fontFamily = DinProFamily,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Import Playlists",
                                    fontFamily = DinProFamily,
                                    fontSize = 11.sp,
                                    color = Color(0x99FFFFFF)
                                )
                            }
                        }
                    }
                }

                // Bug 11: Hero Card for Liked Songs banner
                if (selectedTab == LibraryTab.ALL || selectedTab == LibraryTab.LIKED) {
                    item {
                        LikedSongsHeroCard(
                            count = likedSongs.size,
                            onClick = { libraryViewModel.openLikedSongsBanner() },
                            onPlayAll = {
                                if (likedSongs.isNotEmpty()) {
                                    onPlayTracks(likedSongs.map { it.toTrack() }, 0)
                                }
                            }
                        )
                    }
                }

                // Dedicated Hero Card for Downloaded Songs
                if (selectedTab == LibraryTab.ALL || selectedTab == LibraryTab.DOWNLOADS) {
                    item {
                        DownloadedSongsHeroCard(
                            count = downloadedSongs.size,
                            onClick = { libraryViewModel.openDownloadedSongsBanner() },
                            onPlayAll = {
                                if (downloadedSongs.isNotEmpty()) {
                                    onPlayTracks(downloadedSongs.map { it.toTrack() }, 0)
                                }
                            }
                        )
                    }
                }

                // YouTube Music remote library section
                if (selectedTab == LibraryTab.ALL || selectedTab == LibraryTab.PLAYLISTS) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "YouTube Music Library",
                                    fontFamily = DinProFamily,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFF0050)
                                ) {
                                    Text(
                                        text = "SYNCED",
                                        fontFamily = DinProFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (ytPlaylists.isNotEmpty()) {
                                Text(
                                    text = "${ytPlaylists.size} playlists",
                                    fontFamily = DinProFamily,
                                    fontSize = 12.sp,
                                    color = Color(0x88FFFFFF)
                                )
                            }
                        }
                    }

                    if (ytPlaylists.isNotEmpty()) {
                        items(ytPlaylists) { ytPlaylist ->
                            val isItemLoading = loadingYtPlaylistId == ytPlaylist.browseId
                            YtPlaylistListItem(
                                playlist = ytPlaylist,
                                isLoading = isItemLoading,
                                onPlay = {
                                    loadingYtPlaylistId = ytPlaylist.browseId
                                    libraryViewModel.playYtPlaylist(ytPlaylist) { tracks ->
                                        loadingYtPlaylistId = null
                                        if (tracks.isNotEmpty()) {
                                            onPlayTracks(tracks, 0)
                                        }
                                    }
                                }
                            )
                        }
                    } else if (selectedTab == LibraryTab.PLAYLISTS) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF181720),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No YouTube Music Playlists",
                                        fontFamily = DinProFamily,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Sign in to your YouTube Music account in Settings to sync your cloud library.",
                                        fontFamily = DinProFamily,
                                        fontSize = 12.sp,
                                        color = Color(0x88FFFFFF),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Local Playlists Section
                if (selectedTab == LibraryTab.ALL || selectedTab == LibraryTab.PLAYLISTS) {
                    if (localPlaylists.isNotEmpty()) {
                        item {
                            Text(
                                text = "Local Playlists",
                                fontFamily = DinProFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(localPlaylists) { playlist ->
                            PlaylistListItem(
                                playlist = playlist,
                                onPlay = {
                                    libraryViewModel.playLocalPlaylist(playlist) { tracks ->
                                        if (tracks.isNotEmpty()) {
                                            onPlayTracks(tracks, 0)
                                        }
                                    }
                                }
                            )
                        }
                    } else if (selectedTab == LibraryTab.PLAYLISTS && ytPlaylists.isEmpty()) {
                        item {
                            EmptyLibraryState(
                                icon = Icons.AutoMirrored.Filled.QueueMusic,
                                title = "No playlists found",
                                subtitle = "Your custom playlists will appear here"
                            )
                        }
                    }
                }

                // Downloads Section (Overview when DOWNLOADS tab selected)
                if (selectedTab == LibraryTab.DOWNLOADS) {
                    if (downloadedSongs.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "All Downloaded Songs (${downloadedSongs.size})",
                                    fontFamily = DinProFamily,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x22FF4D4F),
                                    modifier = Modifier.clickable { libraryViewModel.deleteAllDownloads() }
                                ) {
                                    Text(
                                        text = "Clear All",
                                        fontFamily = DinProFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF4D4F),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        items(downloadedSongs) { song ->
                            val songIndex = downloadedSongs.indexOf(song).coerceAtLeast(0)
                            SongListItem(
                                song = song,
                                onPlay = { onPlayTracks(downloadedSongs.map { it.toTrack() }, songIndex) },
                                onDelete = { libraryViewModel.deleteDownload(song.videoId) }
                            )
                        }
                    } else {
                        item {
                            EmptyLibraryState(
                                icon = Icons.Default.Download,
                                title = "No downloads found",
                                subtitle = "Downloaded songs for offline playback will appear here"
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTasteSheet) {
        TasteBottomSheet(
            sheetState = tasteSheetState,
            onDismissRequest = { showTasteSheet = false },
            onPlayTrack = { track ->
                showTasteSheet = false
                onPlayTrack(track)
            },
            onPlayAllRecommendations = { tracks ->
                showTasteSheet = false
                if (tracks.isNotEmpty()) {
                    onPlayTracks(tracks, 0)
                }
            }
        )
    }

    if (showMigrationDialog) {
        PlaylistMigrationDialog(
            onDismissRequest = { showMigrationDialog = false },
            onMigrationComplete = {
                showMigrationDialog = false
            }
        )
    }
}

/**
 * Bug 11: Dedicated expanded view when user opens the Liked Songs banner.
 * Displays all liked songs contained cleanly within the banner view.
 */
@Composable
fun LikedSongsBannerExpandedView(
    likedSongs: List<SongEntity>,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onPlayTrack: (Track) -> Unit = {},
    onPlayTrackAtIndex: (Int) -> Unit = { idx -> onPlayTrack(likedSongs[idx].toTrack()) },
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0E13))
            .statusBarsPadding()
    ) {
        // Top navigation bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Liked Songs",
                    fontFamily = DinProFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${likedSongs.size} tracks saved",
                    fontFamily = DinProFamily,
                    fontSize = 12.sp,
                    color = Color(0x99FFFFFF)
                )
            }
        }

        // Header Banner Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF5E115D),
                                Color(0xFFFF0050)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        if (likedSongs.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = onShuffleAll,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    ),
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Shuffle",
                                        fontFamily = DinProFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }

                                Button(
                                    onClick = onPlayAll,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    ),
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play All",
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Play All",
                                        fontFamily = DinProFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Your Favorited Tracks",
                        fontFamily = DinProFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "All tracks you've hearted in Skim are collected right here.",
                        fontFamily = DinProFamily,
                        fontSize = 12.sp,
                        color = Color(0xCCFFFFFF)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tracks List inside the banner view
        if (likedSongs.isEmpty()) {
            EmptyLibraryState(
                icon = Icons.Default.Favorite,
                title = "No liked tracks yet",
                subtitle = "Heart tracks in the Skim feed to add them inside this banner"
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(likedSongs) { song ->
                    val songIndex = likedSongs.indexOf(song).coerceAtLeast(0)
                    SongListItem(
                        song = song,
                        onPlay = { onPlayTrackAtIndex(songIndex) }
                    )
                }
            }
        }
    }
}

/**
 * Liked Songs Hero Card shown on the main library overview.
 * Clicking this banner expands to show all liked songs inside it.
 */
@Composable
fun LikedSongsHeroCard(
    count: Int,
    onClick: () -> Unit,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF5E115D),
                            Color(0xFFFF0050)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (count > 0) {
                            Button(
                                onClick = onPlayAll,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                ),
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Play",
                                    fontFamily = DinProFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color(0x33FFFFFF),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Open Banner",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Liked Songs",
                    fontFamily = DinProFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$count tracks saved • Tap banner to view all",
                    fontFamily = DinProFamily,
                    fontSize = 13.sp,
                    color = Color(0xCCFFFFFF)
                )
            }
        }
    }
}

/**
 * Dedicated Downloaded Songs Hero Card shown on the library overview.
 * Clicking this banner expands to show all downloaded songs inside it with delete management.
 */
@Composable
fun DownloadedSongsHeroCard(
    count: Int,
    onClick: () -> Unit,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F3B4A),
                            Color(0xFF007A87),
                            Color(0xFF00A896)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color(0xFF00F2FE),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (count > 0) {
                            Button(
                                onClick = onPlayAll,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                ),
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Play",
                                    fontFamily = DinProFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color(0x33FFFFFF),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Open Downloads Banner",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Downloaded Songs",
                    fontFamily = DinProFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$count tracks offline • Tap banner to view & delete",
                    fontFamily = DinProFamily,
                    fontSize = 13.sp,
                    color = Color(0xCCFFFFFF)
                )
            }
        }
    }
}

/**
 * Dedicated expanded banner view for all offline downloaded songs with individual and batch delete support.
 */
@Composable
fun DownloadedSongsBannerExpandedView(
    downloadedSongs: List<SongEntity>,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onPlayTrack: (Track) -> Unit = {},
    onPlayTrackAtIndex: (Int) -> Unit = { idx -> onPlayTrack(downloadedSongs[idx].toTrack()) },
    onDeleteSong: (SongEntity) -> Unit,
    onDeleteAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0E13))
            .statusBarsPadding()
    ) {
        // Top navigation bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Downloaded Songs",
                    fontFamily = DinProFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${downloadedSongs.size} tracks offline",
                    fontFamily = DinProFamily,
                    fontSize = 12.sp,
                    color = Color(0x99FFFFFF)
                )
            }
            if (downloadedSongs.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x22FF4D4F),
                    modifier = Modifier.clickable { onDeleteAll() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear All",
                            tint = Color(0xFFFF4D4F),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Clear All",
                            fontFamily = DinProFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFF4D4F)
                        )
                    }
                }
            }
        }

        // Header Banner Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0F3B4A),
                                Color(0xFF007A87),
                                Color(0xFF00A896)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = Color(0xFF00F2FE),
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        if (downloadedSongs.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = onShuffleAll,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    ),
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Shuffle",
                                        fontFamily = DinProFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }

                                Button(
                                    onClick = onPlayAll,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    ),
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play All",
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Play All",
                                        fontFamily = DinProFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Offline Downloads",
                        fontFamily = DinProFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Listen anywhere with zero network or data usage. Tap trash icon to delete.",
                        fontFamily = DinProFamily,
                        fontSize = 12.sp,
                        color = Color(0xCCFFFFFF)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tracks List inside the banner view
        if (downloadedSongs.isEmpty()) {
            EmptyLibraryState(
                icon = Icons.Default.Download,
                title = "No downloaded tracks yet",
                subtitle = "Download songs in the Skim player to listen offline without internet"
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(downloadedSongs) { song ->
                    val songIndex = downloadedSongs.indexOf(song).coerceAtLeast(0)
                    SongListItem(
                        song = song,
                        onPlay = { onPlayTrackAtIndex(songIndex) },
                        onDelete = { onDeleteSong(song) }
                    )
                }
            }
        }
    }
}

/**
 * YouTube Music Playlist Item for displaying remote YouTube playlists
 */
@Composable
fun YtPlaylistListItem(
    playlist: PlaylistsResult,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    val thumbnailUrl = playlist.thumbnails.lastOrNull()?.url

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF181720),
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading) { onPlay() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF262530)),
                contentAlignment = Alignment.Center
            ) {
                if (!thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = playlist.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(54.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        tint = Color(0xFFFF0050),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.title,
                    fontFamily = DinProFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = buildString {
                        if (playlist.itemCount.isNotBlank()) append("${playlist.itemCount} tracks")
                        if (playlist.author.isNotBlank()) {
                            if (isNotEmpty()) append(" • ")
                            append(playlist.author)
                        }
                    }.ifBlank { "YouTube Music Playlist" },
                    fontFamily = DinProFamily,
                    fontSize = 12.sp,
                    color = Color(0x99FFFFFF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onPlay,
                enabled = !isLoading,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2B2A36))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color(0xFFFF0050),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Playlist",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SongListItem(
    song: SongEntity,
    onPlay: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val artistName = song.artistName?.joinToString(", ") ?: ""

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF181720),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlay() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artwork
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF262530)),
                contentAlignment = Alignment.Center
            ) {
                if (!song.thumbnails.isNullOrBlank()) {
                    AsyncImage(
                        model = song.thumbnails,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(50.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color(0x66FFFFFF),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontFamily = DinProFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = artistName.ifBlank { "Unknown Artist" },
                    fontFamily = DinProFamily,
                    fontSize = 12.sp,
                    color = Color(0x99FFFFFF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Duration
            if (song.duration.isNotBlank()) {
                Text(
                    text = song.duration,
                    fontFamily = DinProFamily,
                    fontSize = 12.sp,
                    color = Color(0x66FFFFFF),
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

            // Delete button if available
            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FF4D4F))
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Download",
                        tint = Color(0xFFFF4D4F),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2B2A36))
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun PlaylistListItem(
    playlist: LocalPlaylistEntity,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF181720),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlay() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF262530)),
                contentAlignment = Alignment.Center
            ) {
                if (!playlist.thumbnail.isNullOrBlank()) {
                    AsyncImage(
                        model = playlist.thumbnail,
                        contentDescription = playlist.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(50.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        tint = Color(0xFFFF0050),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.title,
                    fontFamily = DinProFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${playlist.tracks?.size ?: 0} tracks",
                    fontFamily = DinProFamily,
                    fontSize = 12.sp,
                    color = Color(0x99FFFFFF)
                )
            }

            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2B2A36))
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Playlist",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyLibraryState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFF1C1B24)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0x44FFFFFF),
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontFamily = DinProFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            fontFamily = DinProFamily,
            fontSize = 13.sp,
            color = Color(0x66FFFFFF),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
