package com.kazatune.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.entities.SearchHistory
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.browse.artist.ResultSong
import com.maxrave.domain.data.model.searchResult.albums.AlbumsResult
import com.maxrave.domain.data.model.searchResult.artists.ArtistsResult
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.model.searchResult.songs.SongsResult
import com.maxrave.domain.data.model.streams.YouTubeWatchEndpoint
import com.maxrave.domain.extension.toGenericMediaItem
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.domain.repository.ArtistRepository
import com.maxrave.domain.repository.HomeRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.SearchRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SearchDetailState {
    data object None : SearchDetailState()
    data class Loading(
        val title: String,
        val subtitle: String?,
        val imageUrl: String?,
        val type: String
    ) : SearchDetailState()
    data class Success(
        val title: String,
        val subtitle: String?,
        val description: String?,
        val imageUrl: String?,
        val type: String,
        val tracks: List<Track>
    ) : SearchDetailState()
    data class Error(val message: String) : SearchDetailState()
}

enum class SearchCategory {
    SONGS, ARTISTS, ALBUMS, PLAYLISTS
}

class SearchViewModel(
    private val searchRepository: SearchRepository,
    private val homeRepository: HomeRepository,
    private val mediaPlayerHandler: MediaPlayerHandler,
    private val albumRepository: AlbumRepository,
    private val playlistRepository: PlaylistRepository,
    private val artistRepository: ArtistRepository,
    private val songRepository: SongRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedCategory = MutableStateFlow(SearchCategory.SONGS)
    val selectedCategory: StateFlow<SearchCategory> = _selectedCategory.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults: StateFlow<List<Track>> = _searchResults.asStateFlow()

    private val _searchAlbumsResult = MutableStateFlow<List<AlbumsResult>>(emptyList())
    val searchAlbumsResult: StateFlow<List<AlbumsResult>> = _searchAlbumsResult.asStateFlow()

    private val _searchArtistsResult = MutableStateFlow<List<ArtistsResult>>(emptyList())
    val searchArtistsResult: StateFlow<List<ArtistsResult>> = _searchArtistsResult.asStateFlow()

    private val _searchPlaylistsResult = MutableStateFlow<List<PlaylistsResult>>(emptyList())
    val searchPlaylistsResult: StateFlow<List<PlaylistsResult>> = _searchPlaylistsResult.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<SearchHistory>>(emptyList())
    val searchHistory: StateFlow<List<SearchHistory>> = _searchHistory.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchDetailState = MutableStateFlow<SearchDetailState>(SearchDetailState.None)
    val searchDetailState: StateFlow<SearchDetailState> = _searchDetailState.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    init {
        loadSearchHistory()
    }

    private fun loadSearchHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            searchRepository.getSearchHistory().collect { history ->
                _searchHistory.value = history
            }
        }
    }

    fun resetSearch() {
        _searchDetailState.value = SearchDetailState.None
        _query.value = ""
        _searchResults.value = emptyList()
        _searchAlbumsResult.value = emptyList()
        _searchArtistsResult.value = emptyList()
        _searchPlaylistsResult.value = emptyList()
        _suggestions.value = emptyList()
        _isSearching.value = false
        _selectedCategory.value = SearchCategory.SONGS
        loadSearchHistory()
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        // Clear stale search results while modifying query so suggestions show immediately
        _searchResults.value = emptyList()
        _searchAlbumsResult.value = emptyList()
        _searchArtistsResult.value = emptyList()
        _searchPlaylistsResult.value = emptyList()

        if (newQuery.length >= 2) {
            viewModelScope.launch(Dispatchers.IO) {
                searchRepository.getSuggestQuery(newQuery).collect { resource ->
                    if (resource is Resource.Success) {
                        _suggestions.value = resource.data?.queries ?: emptyList()
                    }
                }
            }
        } else {
            _suggestions.value = emptyList()
        }
    }

    fun search(q: String = _query.value) {
        if (q.isBlank()) return
        _query.value = q
        _isSearching.value = true
        _suggestions.value = emptyList()

        viewModelScope.launch(Dispatchers.IO) {
            // Save to search history
            searchRepository.insertSearchHistory(SearchHistory(q)).collect {}

            when (_selectedCategory.value) {
                SearchCategory.SONGS -> {
                    searchRepository.getSearchDataSong(q).collect { resource ->
                        when (resource) {
                            is Resource.Success -> {
                                val tracks = resource.data?.map { it.toTrack() } ?: emptyList()
                                _searchResults.value = tracks
                                _isSearching.value = false
                            }
                            is Resource.Error -> {
                                _isSearching.value = false
                            }
                        }
                    }
                }
                SearchCategory.ALBUMS -> {
                    searchRepository.getSearchDataAlbum(q).collect { resource ->
                        when (resource) {
                            is Resource.Success -> {
                                _searchAlbumsResult.value = resource.data ?: emptyList()
                                _isSearching.value = false
                            }
                            is Resource.Error -> {
                                _isSearching.value = false
                            }
                        }
                    }
                }
                SearchCategory.ARTISTS -> {
                    searchRepository.getSearchDataArtist(q).collect { resource ->
                        when (resource) {
                            is Resource.Success -> {
                                _searchArtistsResult.value = resource.data ?: emptyList()
                                _isSearching.value = false
                            }
                            is Resource.Error -> {
                                _isSearching.value = false
                            }
                        }
                    }
                }
                SearchCategory.PLAYLISTS -> {
                    searchRepository.getSearchDataPlaylist(q).collect { resource ->
                        when (resource) {
                            is Resource.Success -> {
                                _searchPlaylistsResult.value = resource.data ?: emptyList()
                                _isSearching.value = false
                            }
                            is Resource.Error -> {
                                _isSearching.value = false
                            }
                        }
                    }
                }
            }
        }
    }

    fun selectCategory(category: SearchCategory) {
        _selectedCategory.value = category
        if (_query.value.isNotBlank()) {
            search(_query.value)
        }
    }

    fun playTrack(track: Track) {
        mediaPlayerHandler.clearMediaItems()
        mediaPlayerHandler.addMediaItem(track.toGenericMediaItem(), playWhenReady = true)
    }

    fun playAlbum(album: AlbumsResult, onTracksLoaded: (List<Track>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                albumRepository.getAlbumData(album.browseId).collect { res ->
                    if (res is Resource.Success && !res.data?.tracks.isNullOrEmpty()) {
                        onTracksLoaded(res.data!!.tracks)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playPlaylist(playlist: PlaylistsResult, onTracksLoaded: (List<Track>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                playlistRepository.getFullPlaylistData(playlist.browseId, "views").collect { res ->
                    if (res is Resource.Success && !res.data?.tracks.isNullOrEmpty()) {
                        onTracksLoaded(res.data!!.tracks)
                    } else {
                        songRepository.getRadioFromEndpoint(YouTubeWatchEndpoint(playlistId = playlist.browseId)).collect { radioRes ->
                            if (radioRes is Resource.Success && !radioRes.data?.first.isNullOrEmpty()) {
                                onTracksLoaded(radioRes.data!!.first)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playArtist(artist: ArtistsResult, onTracksLoaded: (List<Track>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                artistRepository.getArtistData(artist.browseId).collect { res ->
                    if (res is Resource.Success && res.data != null) {
                        val artistData = res.data!!
                        val topSongs = artistData.songs?.results?.map { it.toTrack() }
                        if (!topSongs.isNullOrEmpty()) {
                            onTracksLoaded(topSongs)
                        } else if (artistData.radioId != null) {
                            songRepository.getRadioFromEndpoint(artistData.radioId!!).collect { rRes ->
                                if (rRes is Resource.Success && !rRes.data?.first.isNullOrEmpty()) {
                                    onTracksLoaded(rRes.data!!.first)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteSearchHistoryItem(query: String) {
        _searchHistory.value = _searchHistory.value.filter { it.query != query }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                searchRepository.deleteSearchHistoryItem(query)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                searchRepository.deleteSearchHistory()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun dismissDetail() {
        _searchDetailState.value = SearchDetailState.None
    }

    fun loadAlbumDetail(album: AlbumsResult) {
        val title = album.title
        val subtitle = album.artists?.joinToString(", ") { it.name } ?: album.year
        val imageUrl = album.thumbnails?.lastOrNull()?.url
        _searchDetailState.value = SearchDetailState.Loading(
            title = title,
            subtitle = subtitle,
            imageUrl = imageUrl,
            type = "Album"
        )
        viewModelScope.launch(Dispatchers.IO) {
            try {
                albumRepository.getAlbumData(album.browseId).collect { res ->
                    if (res is Resource.Success && res.data != null) {
                        val data = res.data!!
                        _searchDetailState.value = SearchDetailState.Success(
                            title = data.title.ifBlank { title },
                            subtitle = data.artists.joinToString(", ") { it.name }.ifBlank { subtitle },
                            description = data.description ?: (data.year?.let { "Released $it • ${data.trackCount} tracks" }),
                            imageUrl = data.thumbnails?.lastOrNull()?.url ?: imageUrl,
                            type = "Album",
                            tracks = data.tracks
                        )
                    } else if (res is Resource.Error) {
                        _searchDetailState.value = SearchDetailState.Error(res.message ?: "Failed to load album")
                    }
                }
            } catch (e: Exception) {
                _searchDetailState.value = SearchDetailState.Error(e.message ?: "Failed to load album")
            }
        }
    }

    fun loadArtistDetail(artist: ArtistsResult) {
        val title = artist.artist
        val subtitle = "Artist"
        val imageUrl = artist.thumbnails?.lastOrNull()?.url
        _searchDetailState.value = SearchDetailState.Loading(
            title = title,
            subtitle = subtitle,
            imageUrl = imageUrl,
            type = "Artist"
        )
        viewModelScope.launch(Dispatchers.IO) {
            try {
                artistRepository.getArtistData(artist.browseId).collect { res ->
                    if (res is Resource.Success && res.data != null) {
                        val data = res.data!!
                        val topTracks = data.songs?.results?.map { it.toTrack() } ?: emptyList()
                        if (topTracks.isNotEmpty()) {
                            _searchDetailState.value = SearchDetailState.Success(
                                title = data.name.ifBlank { title },
                                subtitle = data.subscribers?.let { "$it subscribers" } ?: subtitle,
                                description = data.description,
                                imageUrl = data.thumbnails?.lastOrNull()?.url ?: imageUrl,
                                type = "Artist",
                                tracks = topTracks
                            )
                        } else if (data.radioId != null) {
                            songRepository.getRadioFromEndpoint(data.radioId!!).collect { rRes ->
                                if (rRes is Resource.Success && !rRes.data?.first.isNullOrEmpty()) {
                                    _searchDetailState.value = SearchDetailState.Success(
                                        title = data.name.ifBlank { title },
                                        subtitle = data.subscribers?.let { "$it subscribers" } ?: subtitle,
                                        description = data.description,
                                        imageUrl = data.thumbnails?.lastOrNull()?.url ?: imageUrl,
                                        type = "Artist",
                                        tracks = rRes.data!!.first
                                    )
                                } else {
                                    _searchDetailState.value = SearchDetailState.Success(
                                        title = data.name.ifBlank { title },
                                        subtitle = subtitle,
                                        description = data.description,
                                        imageUrl = data.thumbnails?.lastOrNull()?.url ?: imageUrl,
                                        type = "Artist",
                                        tracks = emptyList()
                                    )
                                }
                            }
                        } else {
                            _searchDetailState.value = SearchDetailState.Success(
                                title = data.name.ifBlank { title },
                                subtitle = subtitle,
                                description = data.description,
                                imageUrl = data.thumbnails?.lastOrNull()?.url ?: imageUrl,
                                type = "Artist",
                                tracks = emptyList()
                            )
                        }
                    } else if (res is Resource.Error) {
                        _searchDetailState.value = SearchDetailState.Error(res.message ?: "Failed to load artist")
                    }
                }
            } catch (e: Exception) {
                _searchDetailState.value = SearchDetailState.Error(e.message ?: "Failed to load artist")
            }
        }
    }

    fun loadPlaylistDetail(playlist: PlaylistsResult) {
        val title = playlist.title
        val subtitle = playlist.author ?: "Playlist"
        val imageUrl = playlist.thumbnails?.lastOrNull()?.url
        _searchDetailState.value = SearchDetailState.Loading(
            title = title,
            subtitle = subtitle,
            imageUrl = imageUrl,
            type = "Playlist"
        )
        viewModelScope.launch(Dispatchers.IO) {
            try {
                playlistRepository.getFullPlaylistData(playlist.browseId, "views").collect { res ->
                    if (res is Resource.Success && !res.data?.tracks.isNullOrEmpty()) {
                        val data = res.data!!
                        _searchDetailState.value = SearchDetailState.Success(
                            title = data.title.ifBlank { title },
                            subtitle = "${data.author.name} • ${data.trackCount} tracks",
                            description = data.description,
                            imageUrl = data.thumbnails.lastOrNull()?.url ?: imageUrl,
                            type = "Playlist",
                            tracks = data.tracks
                        )
                    } else {
                        songRepository.getRadioFromEndpoint(YouTubeWatchEndpoint(playlistId = playlist.browseId)).collect { radioRes ->
                            if (radioRes is Resource.Success && !radioRes.data?.first.isNullOrEmpty()) {
                                _searchDetailState.value = SearchDetailState.Success(
                                    title = title,
                                    subtitle = subtitle,
                                    description = null,
                                    imageUrl = imageUrl,
                                    type = "Playlist",
                                    tracks = radioRes.data!!.first
                                )
                            } else {
                                _searchDetailState.value = SearchDetailState.Error("No tracks found for this playlist")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _searchDetailState.value = SearchDetailState.Error(e.message ?: "Failed to load playlist")
            }
        }
    }

    private fun SongsResult.toTrack(): Track = Track(
        album = this.album,
        artists = this.artists,
        duration = this.duration,
        durationSeconds = this.durationSeconds,
        isAvailable = true,
        isExplicit = this.isExplicit ?: false,
        likeStatus = null,
        thumbnails = this.thumbnails,
        title = this.title ?: "Unknown",
        videoId = this.videoId,
        videoType = this.videoType,
        category = this.category,
        feedbackTokens = this.feedbackTokens,
        resultType = this.resultType
    )

    private fun ResultSong.toTrack(): Track = Track(
        album = this.album,
        artists = this.artists,
        duration = null,
        durationSeconds = this.durationSeconds,
        isAvailable = this.isAvailable,
        isExplicit = this.isExplicit,
        likeStatus = this.likeStatus,
        thumbnails = this.thumbnails,
        title = this.title,
        videoId = this.videoId,
        videoType = this.videoType,
        category = null,
        feedbackTokens = null,
        resultType = null
    )
}
