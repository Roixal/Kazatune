package com.kazatune.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.model.streams.YouTubeWatchEndpoint
import com.maxrave.domain.extension.toGenericMediaItem
import com.maxrave.domain.mediaservice.handler.DownloadHandler
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
import kotlinx.coroutines.withContext

enum class LibraryTab {
    ALL, LIKED, DOWNLOADS, PLAYLISTS
}

class LibraryViewModel(
    private val songRepository: SongRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val playlistRepository: PlaylistRepository,
    private val mediaPlayerHandler: MediaPlayerHandler,
    private val downloadHandler: DownloadHandler
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

    // Dedicated Downloads Section Banner
    private val _isDownloadedSongsBannerOpen = MutableStateFlow(false)
    val isDownloadedSongsBannerOpen: StateFlow<Boolean> = _isDownloadedSongsBannerOpen.asStateFlow()

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
            _isDownloadedSongsBannerOpen.value = false
        } else if (tab == LibraryTab.DOWNLOADS) {
            _isDownloadedSongsBannerOpen.value = true
            _isLikedSongsBannerOpen.value = false
        } else {
            _isLikedSongsBannerOpen.value = false
            _isDownloadedSongsBannerOpen.value = false
        }
    }

    fun openLikedSongsBanner() {
        _isLikedSongsBannerOpen.value = true
        _isDownloadedSongsBannerOpen.value = false
    }

    fun closeLikedSongsBanner() {
        _isLikedSongsBannerOpen.value = false
        if (_selectedTab.value == LibraryTab.LIKED) {
            _selectedTab.value = LibraryTab.ALL
        }
    }

    fun openDownloadedSongsBanner() {
        _isDownloadedSongsBannerOpen.value = true
        _isLikedSongsBannerOpen.value = false
    }

    fun closeDownloadedSongsBanner() {
        _isDownloadedSongsBannerOpen.value = false
        if (_selectedTab.value == LibraryTab.DOWNLOADS) {
            _selectedTab.value = LibraryTab.ALL
        }
    }

    fun deleteDownload(videoId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            downloadHandler.removeDownload(videoId)
            songRepository.updateDownloadState(videoId, DownloadState.STATE_NOT_DOWNLOADED)
        }
    }

    fun deleteAllDownloads() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = _downloadedSongs.value
            for (song in list) {
                downloadHandler.removeDownload(song.videoId)
                songRepository.updateDownloadState(song.videoId, DownloadState.STATE_NOT_DOWNLOADED)
            }
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
                var loadedTracks: List<Track>? = null
                val browseId = playlist.browseId
                val cleanPlaylistId = if (browseId.startsWith("VL")) browseId.removePrefix("VL") else browseId

                // 1. Try playlistRepository.getPlaylistData with full browseId
                playlistRepository.getPlaylistData(browseId, "").collect { res ->
                    if (res is Resource.Success && !res.data?.first?.tracks.isNullOrEmpty()) {
                        loadedTracks = res.data!!.first.tracks
                    }
                }

                // 2. Try playlistRepository.getPlaylistData with clean playlist ID
                if (loadedTracks.isNullOrEmpty()) {
                    playlistRepository.getPlaylistData(cleanPlaylistId, "").collect { res ->
                        if (res is Resource.Success && !res.data?.first?.tracks.isNullOrEmpty()) {
                            loadedTracks = res.data!!.first.tracks
                        }
                    }
                }

                // 3. Fallback to watch endpoint radio
                if (loadedTracks.isNullOrEmpty()) {
                    songRepository.getRadioFromEndpoint(YouTubeWatchEndpoint(playlistId = cleanPlaylistId)).collect { res ->
                        if (res is Resource.Success && !res.data?.first.isNullOrEmpty()) {
                            loadedTracks = res.data!!.first
                        }
                    }
                }

                loadedTracks?.let { tracks ->
                    if (tracks.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            onTracksLoaded(tracks)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playLocalPlaylist(playlist: LocalPlaylistEntity, onTracksLoaded: (List<Track>) -> Unit) {
        val trackIds = playlist.tracks ?: return
        if (trackIds.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                songRepository.getSongsByListVideoId(trackIds).collect { songs ->
                    if (songs.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            onTracksLoaded(songs.map { it.toTrack() })
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
