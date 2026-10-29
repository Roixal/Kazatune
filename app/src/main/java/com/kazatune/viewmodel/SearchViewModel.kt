package com.kazatune.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.entities.SearchHistory
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.searchResult.songs.SongsResult
import com.maxrave.domain.extension.toGenericMediaItem
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.repository.HomeRepository
import com.maxrave.domain.repository.SearchRepository
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
    private val mediaPlayerHandler: MediaPlayerHandler
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedCategory = MutableStateFlow(SearchCategory.SONGS)
    val selectedCategory: StateFlow<SearchCategory> = _selectedCategory.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults: StateFlow<List<Track>> = _searchResults.asStateFlow()

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
}
