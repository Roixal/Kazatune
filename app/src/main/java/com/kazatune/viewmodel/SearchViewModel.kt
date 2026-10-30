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

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
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

    fun clearSearchHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            searchRepository.deleteSearchHistory()
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
