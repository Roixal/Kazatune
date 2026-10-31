package com.kazatune.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import com.kazatune.viewmodel.SearchDetailState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.kazatune.ui.theme.DinProFamily
import com.kazatune.ui.theme.RessoAccent
import com.kazatune.viewmodel.SearchCategory
import com.kazatune.viewmodel.SearchViewModel
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.searchResult.albums.AlbumsResult
import com.maxrave.domain.data.model.searchResult.artists.ArtistsResult
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import org.koin.androidx.compose.koinViewModel

@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = koinViewModel(),
    onPlayTrack: (Track) -> Unit = { searchViewModel.playTrack(it) },
    onPlayTracks: (List<Track>, Int) -> Unit = { list, idx -> onPlayTrack(list[idx]) },
    modifier: Modifier = Modifier
) {
    val query by searchViewModel.query.collectAsStateWithLifecycle()
    val searchResults by searchViewModel.searchResults.collectAsStateWithLifecycle()
    val searchAlbumsResult by searchViewModel.searchAlbumsResult.collectAsStateWithLifecycle()
    val searchArtistsResult by searchViewModel.searchArtistsResult.collectAsStateWithLifecycle()
    val searchPlaylistsResult by searchViewModel.searchPlaylistsResult.collectAsStateWithLifecycle()
    val searchHistory by searchViewModel.searchHistory.collectAsStateWithLifecycle()
    val suggestions by searchViewModel.suggestions.collectAsStateWithLifecycle()
    val isSearching by searchViewModel.isSearching.collectAsStateWithLifecycle()
    val selectedCategory by searchViewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchDetailState by searchViewModel.searchDetailState.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current

    // Reset search every time user opens the Search section
    LaunchedEffect(Unit) {
        searchViewModel.resetSearch()
    }

    val hasResults = when (selectedCategory) {
        SearchCategory.SONGS -> searchResults.isNotEmpty()
        SearchCategory.ALBUMS -> searchAlbumsResult.isNotEmpty()
        SearchCategory.ARTISTS -> searchArtistsResult.isNotEmpty()
        SearchCategory.PLAYLISTS -> searchPlaylistsResult.isNotEmpty()
    }

    // If detail view is open for an Album, Artist, or Playlist
    if (searchDetailState !is SearchDetailState.None) {
        SearchDetailView(
            state = searchDetailState,
            onBack = { searchViewModel.dismissDetail() },
            onPlayTracks = onPlayTracks,
            onPlayTrack = onPlayTrack
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0E13))
            .statusBarsPadding()
            .padding(top = 12.dp)
    ) {
        // Screen Title
        Text(
            text = "Search",
            fontFamily = DinProFamily,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        // Search Input Bar
        OutlinedTextField(
            value = query,
            onValueChange = { searchViewModel.onQueryChange(it) },
            placeholder = {
                Text(
                    text = "Search songs, artists, albums, playlists...",
                    fontFamily = DinProFamily,
                    fontSize = 14.sp,
                    color = Color(0x66FFFFFF)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0x88FFFFFF)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { searchViewModel.onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color.White
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RessoAccent,
                unfocusedBorderColor = Color(0x22FFFFFF),
                focusedContainerColor = Color(0xFF1B1A22),
                unfocusedContainerColor = Color(0xFF16151D),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    searchViewModel.search(query)
                    focusManager.clearFocus()
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        )

        // Category Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SearchCategory.entries) { category ->
                val isSelected = category == selectedCategory
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) RessoAccent else Color(0xFF201F28),
                    modifier = Modifier.clickable {
                        searchViewModel.selectCategory(category)
                    }
                ) {
                    Text(
                        text = category.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontFamily = DinProFamily,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color(0xAAFFFFFF),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when {
                isSearching -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = RessoAccent,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Searching Skim Music...",
                            fontFamily = DinProFamily,
                            fontSize = 14.sp,
                            color = Color(0x88FFFFFF)
                        )
                    }
                }

                // If user is typing query and suggestions are available
                suggestions.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp)
                    ) {
                        items(suggestions) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        searchViewModel.search(item)
                                        focusManager.clearFocus()
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0x66FFFFFF),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = item,
                                    fontFamily = DinProFamily,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Search Results
                hasResults -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (selectedCategory) {
                            SearchCategory.SONGS -> {
                                items(searchResults) { track ->
                                    SearchResultItem(
                                        track = track,
                                        onPlay = { onPlayTrack(track) }
                                    )
                                }
                            }
                            SearchCategory.ALBUMS -> {
                                items(searchAlbumsResult) { album ->
                                    AlbumSearchResultItem(
                                        album = album,
                                        onPlay = {
                                            searchViewModel.loadAlbumDetail(album)
                                        }
                                    )
                                }
                            }
                            SearchCategory.ARTISTS -> {
                                items(searchArtistsResult) { artist ->
                                    ArtistSearchResultItem(
                                        artist = artist,
                                        onPlay = {
                                            searchViewModel.loadArtistDetail(artist)
                                        }
                                    )
                                }
                            }
                            SearchCategory.PLAYLISTS -> {
                                items(searchPlaylistsResult) { playlist ->
                                    PlaylistSearchResultItem(
                                        playlist = playlist,
                                        onPlay = {
                                            searchViewModel.loadPlaylistDetail(playlist)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Empty Query: Show Search History
                query.isBlank() -> {
                    if (searchHistory.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Searches",
                                    fontFamily = DinProFamily,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                TextButton(onClick = { searchViewModel.clearSearchHistory() }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Clear History",
                                        tint = Color(0x88FFFFFF),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Clear",
                                        fontFamily = DinProFamily,
                                        fontSize = 13.sp,
                                        color = Color(0x88FFFFFF)
                                    )
                                }
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp)
                            ) {
                                items(searchHistory, key = { it.query }) { history ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                searchViewModel.search(history.query)
                                                focusManager.clearFocus()
                                            }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = Color(0x66FFFFFF),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = history.query,
                                            fontFamily = DinProFamily,
                                            fontSize = 15.sp,
                                            color = Color.White,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { searchViewModel.deleteSearchHistoryItem(history.query) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete search",
                                                tint = Color(0x88FFFFFF),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Empty State Placeholder
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0x33FFFFFF),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Search Skim Music",
                                fontFamily = DinProFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Find your favorite songs, trending artists, and curated albums",
                                fontFamily = DinProFamily,
                                fontSize = 13.sp,
                                color = Color(0x66FFFFFF),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Query entered but no results found
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No results found for ${selectedCategory.name.lowercase()}",
                            fontFamily = DinProFamily,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try searching for a different keyword or category",
                            fontFamily = DinProFamily,
                            fontSize = 13.sp,
                            color = Color(0x66FFFFFF)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    track: Track,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val artworkUrl = track.thumbnails?.lastOrNull()?.url
    val artistName = track.artists?.joinToString(", ") { it.name ?: "" } ?: ""

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
            // Thumbnail
            AsyncImage(
                model = artworkUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF262530))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Artist
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontFamily = DinProFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = artistName.ifBlank { "YouTube Music" },
                    fontFamily = DinProFamily,
                    fontSize = 12.sp,
                    color = Color(0x99FFFFFF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!track.duration.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.duration ?: "",
                        fontFamily = DinProFamily,
                        fontSize = 11.sp,
                        color = Color(0x66FFFFFF)
                    )
                }
            }

            // Play Icon Button
            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(RessoAccent)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AlbumSearchResultItem(
    album: AlbumsResult,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val artworkUrl = album.thumbnails.lastOrNull()?.url
    val artistName = album.artists.joinToString(", ") { it.name ?: "" }

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
                if (!artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = album.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(54.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = null,
                        tint = RessoAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.title,
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
                        if (artistName.isNotBlank()) append(artistName)
                        if (album.year.isNotBlank()) {
                            if (isNotEmpty()) append(" • ")
                            append(album.year)
                        }
                    }.ifBlank { "Album" },
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
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(RessoAccent)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Album",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ArtistSearchResultItem(
    artist: ArtistsResult,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val avatarUrl = artist.thumbnails.lastOrNull()?.url

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
                    .clip(CircleShape)
                    .background(Color(0xFF262530)),
                contentAlignment = Alignment.Center
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = artist.artist,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = RessoAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.artist,
                    fontFamily = DinProFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Artist",
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
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(RessoAccent)
            ) {
                Icon(
                    imageVector = Icons.Default.Radio,
                    contentDescription = "Start Artist Radio",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun PlaylistSearchResultItem(
    playlist: PlaylistsResult,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val artworkUrl = playlist.thumbnails.lastOrNull()?.url

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
                if (!artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = playlist.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(54.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        tint = RessoAccent,
                        modifier = Modifier.size(26.dp)
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
                        if (playlist.author.isNotBlank()) append(playlist.author)
                        if (playlist.itemCount.isNotBlank()) {
                            if (isNotEmpty()) append(" • ")
                            append("${playlist.itemCount} tracks")
                        }
                    }.ifBlank { "Playlist" },
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
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(RessoAccent)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Playlist",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


@Composable
fun SearchDetailView(
    state: SearchDetailState,
    onBack: () -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onPlayTrack: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0E13))
            .statusBarsPadding()
    ) {
        // Top App Bar with Back Button
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
            Text(
                text = when (state) {
                    is SearchDetailState.Loading -> state.type
                    is SearchDetailState.Success -> state.type
                    else -> "Details"
                },
                fontFamily = DinProFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        when (state) {
            is SearchDetailState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (!state.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = state.imageUrl,
                            contentDescription = state.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(180.dp)
                                .clip(if (state.type == "Artist") CircleShape else RoundedCornerShape(16.dp))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Text(
                        text = state.title,
                        fontFamily = DinProFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    CircularProgressIndicator(
                        color = RessoAccent,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                }
            }
            is SearchDetailState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.message,
                        fontFamily = DinProFamily,
                        fontSize = 15.sp,
                        color = Color(0xAAFFFFFF)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = RessoAccent)
                    ) {
                        Text("Back", color = Color.White, fontFamily = DinProFamily)
                    }
                }
            }
            is SearchDetailState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // Header Card
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (!state.imageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = state.imageUrl,
                                    contentDescription = state.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(190.dp)
                                        .clip(if (state.type == "Artist") CircleShape else RoundedCornerShape(16.dp))
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            Text(
                                text = state.title,
                                fontFamily = DinProFamily,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (!state.subtitle.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = state.subtitle,
                                    fontFamily = DinProFamily,
                                    fontSize = 14.sp,
                                    color = Color(0xAAFFFFFF)
                                )
                            }

                            if (!state.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.description,
                                    fontFamily = DinProFamily,
                                    fontSize = 12.sp,
                                    color = Color(0x77FFFFFF),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Play and Shuffle buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (state.tracks.isNotEmpty()) {
                                            onPlayTracks(state.tracks, 0)
                                        }
                                    },
                                    enabled = state.tracks.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = RessoAccent),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Play",
                                        fontFamily = DinProFamily,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (state.tracks.isNotEmpty()) {
                                            val shuffled = state.tracks.shuffled()
                                            onPlayTracks(shuffled, 0)
                                        }
                                    },
                                    enabled = state.tracks.isNotEmpty(),
                                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF1B1A24)),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Shuffle",
                                        fontFamily = DinProFamily,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Section Header
                    item {
                        Text(
                            text = if (state.type == "Artist") "Top Songs" else "Tracks (${state.tracks.size})",
                            fontFamily = DinProFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }

                    // Tracks List
                    itemsIndexed(state.tracks) { index, track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlayTracks(state.tracks, index) }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontFamily = DinProFamily,
                                fontSize = 13.sp,
                                color = Color(0x66FFFFFF),
                                modifier = Modifier.width(28.dp)
                            )

                            val thumb = track.thumbnails?.firstOrNull()?.url
                            if (!thumb.isNullOrBlank()) {
                                AsyncImage(
                                    model = thumb,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    fontFamily = DinProFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = track.artists?.joinToString(", ") { it.name ?: "" } ?: "Unknown Artist",
                                    fontFamily = DinProFamily,
                                    fontSize = 12.sp,
                                    color = Color(0x88FFFFFF),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            track.duration?.let { dur ->
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = dur,
                                    fontFamily = DinProFamily,
                                    fontSize = 12.sp,
                                    color = Color(0x66FFFFFF)
                                )
                            }
                        }
                    }
                }
            }
            SearchDetailState.None -> Unit
        }
    }
}
