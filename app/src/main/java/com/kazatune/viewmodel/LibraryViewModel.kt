package com.kazatune.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.model.streams.YouTubeWatchEndpoint
import com.maxrave.domain.extension.toGenericMediaItem
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.toTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LibraryTab {
    ALL, LIKED, DOWNLOADS, PLAYLISTS
}

class LibraryViewModel(
    private val songRepository: SongRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val playlistRepository: PlaylistRepository,
    private val mediaPlayerHandler: MediaPlayerHandler
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(LibraryTab.ALL)
    val selectedTab: StateFlow<LibraryTab> = _selectedTab.asStateFlow()

    private val _likedSongs = MutableStateFlow<List<SongEntity>>(emptyList())
    val likedSongs: StateFlow<List<SongEntity>> = _likedSongs.asStateFlow()

    private val _downloadedSongs = MutableStateFlow<List<SongEntity>>(emptyList())
    val downloadedSongs: StateFlow<List<SongEntity>> = _downloadedSongs.asStateFlow()

    private val _localPlaylists = MutableStateFlow<List<LocalPlaylistEntity>>(emptyList())
    val localPlaylists: StateFlow<List<LocalPlaylistEntity>> = _localPlaylists.asStateFlow()

    private val _ytPlaylists = MutableStateFlow<List<PlaylistsResult>>(emptyList())
    val ytPlaylists: StateFlow<List<PlaylistsResult>> = _ytPlaylists.asStateFlow()

    // For Bug 11: When user clicks on the Liked Songs banner, opens all liked songs inside that banner view
    private val _isLikedSongsBannerOpen = MutableStateFlow(false)
    val isLikedSongsBannerOpen: StateFlow<Boolean> = _isLikedSongsBannerOpen.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch(Dispatchers.IO) {
            songRepository.getLikedSongs().collect { songs ->
                _likedSongs.value = songs
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            songRepository.getDownloadedSongs().collect { songs ->
                _downloadedSongs.value = songs ?: emptyList()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            localPlaylistRepository.getAllLocalPlaylists().collect { playlists ->
                _localPlaylists.value = playlists
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                playlistRepository.getLibraryPlaylist().collect { remoteList ->
                    _ytPlaylists.value = remoteList ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectTab(tab: LibraryTab) {
        _selectedTab.value = tab
        if (tab == LibraryTab.LIKED) {
            _isLikedSongsBannerOpen.value = true
        } else {
            _isLikedSongsBannerOpen.value = false
        }
    }

    fun openLikedSongsBanner() {
        _isLikedSongsBannerOpen.value = true
    }

    fun closeLikedSongsBanner() {
        _isLikedSongsBannerOpen.value = false
        if (_selectedTab.value == LibraryTab.LIKED) {
            _selectedTab.value = LibraryTab.ALL
        }
    }

    fun playSong(song: SongEntity) {
        mediaPlayerHandler.clearMediaItems()
        mediaPlayerHandler.addMediaItem(song.toGenericMediaItem(), playWhenReady = true)
    }

    fun playAll(songs: List<SongEntity>) {
        if (songs.isEmpty()) return
        mediaPlayerHandler.clearMediaItems()
        mediaPlayerHandler.addMediaItemList(songs.map { it.toGenericMediaItem() })
        mediaPlayerHandler.playMediaItemInMediaSource(0)
    }

    fun shuffleAll(songs: List<SongEntity>) {
        if (songs.isEmpty()) return
        val shuffled = songs.shuffled()
        mediaPlayerHandler.clearMediaItems()
        mediaPlayerHandler.addMediaItemList(shuffled.map { it.toGenericMediaItem() })
        mediaPlayerHandler.playMediaItemInMediaSource(0)
    }

    fun playYtPlaylist(playlist: PlaylistsResult, onTracksLoaded: (List<Track>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                songRepository.getRadioFromEndpoint(YouTubeWatchEndpoint(playlistId = playlist.browseId)).collect { res ->
                    if (res is Resource.Success && !res.data?.first.isNullOrEmpty()) {
                        val tracks = res.data!!.first
                        onTracksLoaded(tracks)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
