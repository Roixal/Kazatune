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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel = koinViewModel(),
    onPlayTrack: (Track) -> Unit = { /* default */ },
    modifier: Modifier = Modifier
) {
    val selectedTab by libraryViewModel.selectedTab.collectAsStateWithLifecycle()
    val likedSongs by libraryViewModel.likedSongs.collectAsStateWithLifecycle()
    val downloadedSongs by libraryViewModel.downloadedSongs.collectAsStateWithLifecycle()
    val localPlaylists by libraryViewModel.localPlaylists.collectAsStateWithLifecycle()
    val ytPlaylists by libraryViewModel.ytPlaylists.collectAsStateWithLifecycle()
    val isLikedSongsBannerOpen by libraryViewModel.isLikedSongsBannerOpen.collectAsStateWithLifecycle()

    // Handle back button when the Liked Songs banner view is open
    BackHandler(enabled = isLikedSongsBannerOpen) {
        libraryViewModel.closeLikedSongsBanner()
    }

    if (isLikedSongsBannerOpen) {
        // Bug 11: All liked songs inside the expanded banner view
        LikedSongsBannerExpandedView(
            likedSongs = likedSongs,
            onBack = { libraryViewModel.closeLikedSongsBanner() },
            onPlayAll = {
                val first = likedSongs.firstOrNull()
                if (first != null) {
                    onPlayTrack(first.toTrack())
                }
            },
            onShuffleAll = {
                if (likedSongs.isNotEmpty()) {
                    val shuffled = likedSongs.shuffled()
                    onPlayTrack(shuffled.first().toTrack())
                }
            },
            onPlayTrack = onPlayTrack,
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
                // Bug 11: Hero Card for Liked Songs banner (tapping expands to view all liked songs inside)
                if (selectedTab == LibraryTab.ALL || selectedTab == LibraryTab.LIKED) {
                    item {
                        LikedSongsHeroCard(
                            count = likedSongs.size,
                            onClick = { libraryViewModel.openLikedSongsBanner() },
                            onPlayAll = {
                                val first = likedSongs.firstOrNull()
                                if (first != null) {
                                    onPlayTrack(first.toTrack())
                                }
                            }
                        )
                    }
                }

                // Bug 7: YouTube Music remote library section
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
                                    color = Color(0xFFFF0000)
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
                            YtPlaylistListItem(
                                playlist = ytPlaylist,
                                onPlay = {
                                    libraryViewModel.playYtPlaylist(ytPlaylist) { tracks ->
                                        if (tracks.isNotEmpty()) {
                                            onPlayTrack(tracks.first())
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
                            PlaylistListItem(playlist = playlist)
                        }
                    } else if (selectedTab == LibraryTab.PLAYLISTS && ytPlaylists.isEmpty()) {
                        item {
                            EmptyLibraryState(
                                icon = Icons.Default.QueueMusic,
                                title = "No playlists found",
                                subtitle = "Your custom playlists will appear here"
                            )
                        }
                    }
                }

                // Downloads Section
                if (selectedTab == LibraryTab.ALL || selectedTab == LibraryTab.DOWNLOADS) {
                    if (downloadedSongs.isNotEmpty()) {
                        item {
                            Text(
                                text = "Offline Downloads",
                                fontFamily = DinProFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(downloadedSongs) { song ->
                            SongListItem(
                                song = song,
                                onPlay = { libraryViewModel.playSong(song) }
                            )
                        }
                    } else if (selectedTab == LibraryTab.DOWNLOADS) {
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
    onPlayTrack: (Track) -> Unit,
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
                    SongListItem(
                        song = song,
                        onPlay = { onPlayTrack(song.toTrack()) }
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
 * YouTube Music Playlist Item for displaying remote YouTube playlists
 */
@Composable
fun YtPlaylistListItem(
    playlist: PlaylistsResult,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbnailUrl = playlist.thumbnails.lastOrNull()?.url

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
                        imageVector = Icons.Default.QueueMusic,
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
fun SongListItem(
    song: SongEntity,
    onPlay: () -> Unit,
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

            // Duration & Play
            if (song.duration.isNotBlank()) {
                Text(
                    text = song.duration,
                    fontFamily = DinProFamily,
                    fontSize = 12.sp,
                    color = Color(0x66FFFFFF),
                    modifier = Modifier.padding(end = 8.dp)
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
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF181720),
        modifier = modifier.fillMaxWidth()
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
                        imageVector = Icons.Default.QueueMusic,
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
