package com.kazatune.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kazatune.mix.PairTransition
import com.kazatune.mix.PlaylistMixManager
import com.kazatune.mix.PlaylistMixSettings
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.model.streams.YouTubeWatchEndpoint
import com.maxrave.domain.extension.toGenericMediaItem
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.DownloadHandler
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.repository.StreamRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.toTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TrackAudioMeta(
    val bpm: Int? = null,
    val musicalKey: String? = null,
    val camelotCode: String? = null
)

enum class LibraryTab {
    ALL, LIKED, DOWNLOADS, PLAYLISTS
}

class LibraryViewModel(
    private val songRepository: SongRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val playlistRepository: PlaylistRepository,
    private val mediaPlayerHandler: MediaPlayerHandler,
    private val downloadHandler: DownloadHandler,
    private val dataStoreManager: DataStoreManager,
    private val streamRepository: StreamRepository,
    private val playlistMixManager: PlaylistMixManager
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

    // Spotify-style Playlist Mix configurations & track BPM/Camelot metadata
    val mixConfigs: StateFlow<Map<String, PlaylistMixSettings>> = playlistMixManager.mixConfigs

    private val _trackAudioMeta = MutableStateFlow<Map<String, TrackAudioMeta>>(emptyMap())
    val trackAudioMeta: StateFlow<Map<String, TrackAudioMeta>> = _trackAudioMeta.asStateFlow()

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

    fun unlikeSong(videoId: String) {
        _likedSongs.value = _likedSongs.value.filterNot { it.videoId == videoId }
        viewModelScope.launch(Dispatchers.IO) {
            songRepository.updateLikeStatus(videoId, 0)
        }
    }

    fun deleteDownload(videoId: String) {
        _downloadedSongs.value = _downloadedSongs.value.filterNot { it.videoId == videoId }
        viewModelScope.launch(Dispatchers.IO) {
            downloadHandler.removeDownload(videoId)
            songRepository.updateDownloadState(videoId, DownloadState.STATE_NOT_DOWNLOADED)
        }
    }

    fun deleteAllDownloads() {
        val list = _downloadedSongs.value
        _downloadedSongs.value = emptyList()
        viewModelScope.launch(Dispatchers.IO) {
            downloadHandler.removeAllDownloads()
            for (song in list) {
                songRepository.updateDownloadState(song.videoId, DownloadState.STATE_NOT_DOWNLOADED)
            }
        }
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val newPlaylist = LocalPlaylistEntity(
                title = name.trim(),
                tracks = emptyList()
            )
            localPlaylistRepository.insertLocalPlaylist(newPlaylist, "Playlist created").collect { }
        }
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
                        applyMixSettingsForTracks("yt_${playlist.browseId}", tracks)
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
                        val tracks = songs.map { it.toTrack() }
                        applyMixSettingsForTracks("local_${playlist.id}", tracks)
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

    fun getLocalPlaylistTracks(playlist: LocalPlaylistEntity, onLoaded: (List<Track>) -> Unit) {
        val trackIds = playlist.tracks.orEmpty()
        if (trackIds.isEmpty()) {
            onLoaded(emptyList())
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val songs = songRepository.getSongsByListVideoId(trackIds).firstOrNull().orEmpty()
                val tracks = songs.map { it.toTrack() }
                loadMixTrackMetadata(tracks)
                withContext(Dispatchers.Main) {
                    onLoaded(tracks)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onLoaded(emptyList())
                }
            }
        }
    }

    fun updatePlaylistMix(settings: PlaylistMixSettings) {
        playlistMixManager.saveSettings(settings)
        viewModelScope.launch(Dispatchers.IO) {
            if (settings.mixEnabled || playlistMixManager.activeMixPlaylistId.value == settings.playlistId) {
                playlistMixManager.applyMixForPlayback(settings.playlistId, dataStoreManager)
            }
        }
    }

    fun setPairTransition(
        playlistId: String,
        fromVideoId: String,
        toVideoId: String,
        transition: PairTransition
    ) {
        playlistMixManager.setPairTransition(playlistId, fromVideoId, toVideoId, transition)
    }

    fun applyMixSettingsForTracks(playlistId: String, tracks: List<Track>) {
        viewModelScope.launch(Dispatchers.IO) {
            val firstId = tracks.getOrNull(0)?.videoId
            val secondId = tracks.getOrNull(1)?.videoId
            playlistMixManager.applyMixForPlayback(
                playlistId = playlistId,
                dataStoreManager = dataStoreManager,
                fromVideoId = firstId,
                toVideoId = secondId
            )
        }
    }

    fun loadMixTrackMetadata(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val currentMap = _trackAudioMeta.value.toMutableMap()
            tracks.forEach { track ->
                if (!currentMap.containsKey(track.videoId)) {
                    runCatching {
                        val fmt = streamRepository.getNewFormat(track.videoId).firstOrNull()
                        if (fmt != null && (fmt.bpm != null || fmt.musicKey != null)) {
                            currentMap[track.videoId] = TrackAudioMeta(
                                bpm = fmt.bpm,
                                musicalKey = fmt.musicKey,
                                camelotCode = PlaylistMixManager.toCamelotCode(fmt.musicKey, fmt.keyScale)
                            )
                        }
                    }
                }
            }
            _trackAudioMeta.value = currentMap
        }
    }

    fun exportMixedPlaylist(title: String, tracks: List<Track>, playlistId: String): String {
        return playlistMixManager.exportMixedPlaylistJson(title, tracks, playlistId)
    }
}
