package com.kazatune.ui.library

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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Hero Card for Liked Songs (Shown in ALL and LIKED)
            if (selectedTab == LibraryTab.ALL || selectedTab == LibraryTab.LIKED) {
                item {
                    LikedSongsHeroCard(
                        count = likedSongs.size,
                        onPlayAll = {
                            val first = likedSongs.firstOrNull()
                            if (first != null) {
                                onPlayTrack(first.toTrack())
                            } else {
                                libraryViewModel.playAll(likedSongs)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Liked Songs Section
            if (selectedTab == LibraryTab.LIKED || selectedTab == LibraryTab.ALL) {
                if (likedSongs.isEmpty() && selectedTab == LibraryTab.LIKED) {
                    item {
                        EmptyLibraryState(
                            icon = Icons.Default.Favorite,
                            title = "No liked tracks yet",
                            subtitle = "Heart tracks in the Skim feed to add them to your collection"
                        )
                    }
                } else {
                    items(likedSongs) { song ->
                        SongListItem(
                            song = song,
                            onPlay = { onPlayTrack(song.toTrack()) }
                        )
                    }
                }
            }

            // Downloads Section
            if (selectedTab == LibraryTab.DOWNLOADS) {
                if (downloadedSongs.isEmpty()) {
                    item {
                        EmptyLibraryState(
                            icon = Icons.Default.Download,
                            title = "No downloads found",
                            subtitle = "Downloaded songs for offline playback will appear here"
                        )
                    }
                } else {
                    items(downloadedSongs) { song ->
                        SongListItem(
                            song = song,
                            onPlay = { libraryViewModel.playSong(song) }
                        )
                    }
                }
            }

            // Playlists Section
            if (selectedTab == LibraryTab.PLAYLISTS) {
                if (localPlaylists.isEmpty()) {
                    item {
                        EmptyLibraryState(
                            icon = Icons.Default.QueueMusic,
                            title = "No playlists found",
                            subtitle = "Your custom playlists will appear here"
                        )
                    }
                } else {
                    items(localPlaylists) { playlist ->
                        PlaylistListItem(playlist = playlist)
                    }
                }
            }
        }
    }
}

@Composable
fun LikedSongsHeroCard(
    count: Int,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
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

                    if (count > 0) {
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
                    text = "$count tracks saved to device",
                    fontFamily = DinProFamily,
                    fontSize = 13.sp,
                    color = Color(0xCCFFFFFF)
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
