package com.kazatune.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.extension.toGenericMediaItem
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.SongRepository
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
    }

    fun selectTab(tab: LibraryTab) {
        _selectedTab.value = tab
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
}
