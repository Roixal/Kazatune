package com.kazatune.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.canvas.CanvasResult
import com.maxrave.domain.data.model.home.Content
import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.domain.data.model.streams.YouTubeWatchEndpoint
import com.maxrave.domain.extension.toGenericMediaItem
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.NowPlayingTrackState
import com.maxrave.domain.mediaservice.handler.PlayerEvent
import com.maxrave.domain.mediaservice.handler.SimpleMediaState
import com.maxrave.domain.repository.HomeRepository
import com.maxrave.domain.repository.LyricsCanvasRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.repository.StreamRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.toLyrics
import com.maxrave.domain.utils.toLyricsEntity
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toSyncedLyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class SkimTab {
    FOR_YOU,
    EXPLORE
}

class SkimViewModel(
    private val mediaPlayerHandler: MediaPlayerHandler,
    private val homeRepository: HomeRepository,
    private val songRepository: SongRepository,
    private val lyricsCanvasRepository: LyricsCanvasRepository,
    private val streamRepository: StreamRepository
) : ViewModel() {

    private val _skimTab = MutableStateFlow(SkimTab.FOR_YOU)
    val skimTab: StateFlow<SkimTab> = _skimTab.asStateFlow()

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    val nowPlaying: StateFlow<NowPlayingTrackState> = mediaPlayerHandler.nowPlayingState
    val mediaState: StateFlow<SimpleMediaState> = mediaPlayerHandler.simpleMediaState
    val controlState = mediaPlayerHandler.controlState

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _totalDurationMs = MutableStateFlow(0L)
    val totalDurationMs: StateFlow<Long> = _totalDurationMs.asStateFlow()

    private val _currentLyrics = MutableStateFlow<Lyrics?>(null)
    val currentLyrics: StateFlow<Lyrics?> = _currentLyrics.asStateFlow()

    private val _currentCanvas = MutableStateFlow<CanvasResult?>(null)
    val currentCanvas: StateFlow<CanvasResult?> = _currentCanvas.asStateFlow()

    private val _likedVideoIds = MutableStateFlow<Set<String>>(emptySet())
    val likedVideoIds: StateFlow<Set<String>> = _likedVideoIds.asStateFlow()

    private var lyricsJob: Job? = null
    private var radioJob: Job? = null
    private var progressTickerJob: Job? = null

    init {
        loadFeed(SkimTab.FOR_YOU)
        observeLikedSongs()
        observePlayback()
        startPositionTicker()
    }

    private fun startPositionTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                val player = mediaPlayerHandler.player
                _currentPositionMs.value = player.currentPosition
                if (player.duration > 0) {
                    _totalDurationMs.value = player.duration
                }
                delay(250)
            }
        }
    }

    private fun observeLikedSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            songRepository.getLikedSongs().collect { songs ->
                _likedVideoIds.value = songs.map { it.videoId }.toSet()
            }
        }
    }

    fun selectSkimTab(tab: SkimTab) {
        if (_skimTab.value == tab) return
        _skimTab.value = tab
        loadFeed(tab)
    }

    fun refreshFeed() {
        loadFeed(_skimTab.value)
    }

    fun loadFeed(tab: SkimTab = _skimTab.value) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (tab == SkimTab.FOR_YOU) {
                    homeRepository.getHomeData(viewString = "views", songString = "songs").collect { resource ->
                        if (resource is Resource.Success) {
                            val homeItems = resource.data?.second ?: emptyList()
                            val list = mutableListOf<Track>()
                            homeItems.forEach { section ->
                                section.contents.forEach { content ->
                                    if (content != null && !content.videoId.isNullOrBlank()) {
                                        list.add(content.toTrack())
                                    }
                                }
                            }
                            if (list.isNotEmpty()) {
                                _tracks.value = list
                                _currentIndex.value = 0
                                playTrackAt(0)
                            }
                        }
                    }
                } else {
                    // EXPLORE: Trending / New releases
                    homeRepository.getNewRelease("New Releases", "Music Videos").collect { resource ->
                        if (resource is Resource.Success) {
                            val items = resource.data ?: emptyList()
                            val list = mutableListOf<Track>()
                            items.forEach { section ->
                                section.contents.forEach { content ->
                                    if (content != null && !content.videoId.isNullOrBlank()) {
                                        list.add(content.toTrack())
                                    }
                                }
                            }
                            if (list.isNotEmpty()) {
                                _tracks.value = list
                                _currentIndex.value = 0
                                playTrackAt(0)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun observePlayback() {
        viewModelScope.launch {
            mediaPlayerHandler.nowPlayingState
                .distinctUntilChangedBy { it.mediaItem.mediaId }
                .collectLatest { state ->
                    val videoId = state.mediaItem.mediaId
                    if (videoId.isNotBlank()) {
                        val currentList = _tracks.value
                        val foundIndex = currentList.indexOfFirst { it.videoId == videoId }
                        if (foundIndex >= 0) {
                            _currentIndex.value = foundIndex
                        } else {
                            // Started playing externally from Search / Library
                            val newTrack = state.track ?: createFallbackTrack(state)
                            _tracks.value = listOf(newTrack)
                            _currentIndex.value = 0
                            fetchRadioQueue(videoId, newTrack)
                        }

                        // Canvas
                        fetchCanvas(videoId)

                        // Synchronized Lyrics
                        val currentTrack = _tracks.value.getOrNull(_currentIndex.value)
                        val artist = currentTrack?.artists?.firstOrNull()?.name ?: state.mediaItem.metadata.artist ?: ""
                        val title = currentTrack?.title ?: state.mediaItem.metadata.title ?: ""
                        val duration = currentTrack?.durationSeconds ?: 0
                        fetchLyrics(videoId, artist, title, duration)
                    }
                }
        }
    }

    private fun fetchCanvas(videoId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                lyricsCanvasRepository.getAMAnimatedArtwork(videoId).collect { res ->
                    if (res is Resource.Success) {
                        _currentCanvas.value = res.data
                    }
                }
            } catch (_: Exception) {
                _currentCanvas.value = null
            }
        }
    }

    private fun fetchLyrics(videoId: String, artist: String, title: String, durationSeconds: Int) {
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch(Dispatchers.IO) {
            _currentLyrics.value = null
            try {
                // 1. Check local saved lyrics
                val saved = lyricsCanvasRepository.getSavedLyrics(videoId).firstOrNull()
                if (saved != null && !saved.lines.isNullOrEmpty()) {
                    _currentLyrics.value = saved.toLyrics().toSyncedLyrics()
                    return@launch
                }

                // Clean title of typical video annotations for accurate LRCLIB match
                val cleanTitle = title
                    .replace(Regex("""(?i)\s*[\(\[](official\s*video|official\s*music\s*video|official\s*audio|audio|mv|visualizer|lyric\s*video|from\s*["'].*?["'])[\)\]]"""), "")
                    .trim()

                var foundLyrics: Lyrics? = null

                // 2. Query LRCLIB
                lyricsCanvasRepository.getLrclibLyricsData(artist, cleanTitle, durationSeconds).collect { res ->
                    val payload = res.data
                    if (res is Resource.Success && payload != null && !payload.lines.isNullOrEmpty()) {
                        foundLyrics = payload.toSyncedLyrics()
                    }
                }

                val lrclibLyrics = foundLyrics
                if (lrclibLyrics != null) {
                    _currentLyrics.value = lrclibLyrics
                    lyricsCanvasRepository.insertLyrics(lrclibLyrics.toLyricsEntity(videoId))
                    return@launch
                }

                // 3. Fallback: BetterLyrics
                lyricsCanvasRepository.getBetterLyrics(artist, cleanTitle, durationSeconds).collect { res ->
                    val payload = res.data
                    if (res is Resource.Success && payload != null && !payload.lines.isNullOrEmpty()) {
                        foundLyrics = payload.toSyncedLyrics()
                    }
                }

                val betterLyrics = foundLyrics
                if (betterLyrics != null) {
                    _currentLyrics.value = betterLyrics
                    lyricsCanvasRepository.insertLyrics(betterLyrics.toLyricsEntity(videoId))
                    return@launch
                }

                // 4. Fallback: YouTube Captions
                lyricsCanvasRepository.getYouTubeCaption("", videoId).collect { res ->
                    val payload = res.data?.first
                    if (res is Resource.Success && payload != null && !payload.lines.isNullOrEmpty()) {
                        foundLyrics = payload.toSyncedLyrics()
                    }
                }

                val ytLyrics = foundLyrics
                if (ytLyrics != null) {
                    _currentLyrics.value = ytLyrics
                    lyricsCanvasRepository.insertLyrics(ytLyrics.toLyricsEntity(videoId))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playTrackAt(index: Int) {
        val trackList = _tracks.value
        if (index in trackList.indices) {
            _currentIndex.value = index
            val track = trackList[index]
            mediaPlayerHandler.clearMediaItems()
            mediaPlayerHandler.addMediaItem(track.toGenericMediaItem(), playWhenReady = true)
        }
    }

    /**
     * Plays a track and starts a Spotify-style Radio queue based on it,
     * replacing the current Skim list with the song and its recommended watch-next queue.
     */
    fun playTrackAndStartRadio(track: Track) {
        radioJob?.cancel()
        _tracks.value = listOf(track)
        _currentIndex.value = 0
        mediaPlayerHandler.clearMediaItems()
        mediaPlayerHandler.addMediaItem(track.toGenericMediaItem(), playWhenReady = true)
        fetchRadioQueue(track.videoId, track)
    }

    private fun fetchRadioQueue(videoId: String, initialTrack: Track) {
        radioJob?.cancel()
        radioJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                songRepository.getRadioFromEndpoint(YouTubeWatchEndpoint(videoId = videoId)).collect { resource ->
                    if (resource is Resource.Success) {
                        val radioTracks = resource.data?.first ?: emptyList()
                        val filtered = radioTracks.filter { it.videoId != videoId }
                        if (filtered.isNotEmpty()) {
                            _tracks.value = listOf(initialTrack) + filtered
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onPageSwiped(pageIndex: Int) {
        if (pageIndex != _currentIndex.value) {
            playTrackAt(pageIndex)
        }
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            mediaPlayerHandler.onPlayerEvent(PlayerEvent.PlayPause)
        }
    }

    fun seekTo(positionMs: Long) {
        _currentPositionMs.value = positionMs
        mediaPlayerHandler.player.seekTo(positionMs)
    }

    fun toggleLike(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentlyLiked = _likedVideoIds.value.contains(track.videoId)
            if (currentlyLiked) {
                songRepository.updateLikeStatus(track.videoId, 0)
            } else {
                val songEntity = track.toSongEntity().copy(liked = true, likeStatus = "LIKE")
                songRepository.insertSong(songEntity).collect {}
                songRepository.updateLikeStatus(track.videoId, 1)
            }
        }
    }

    private fun Content.toTrack(): Track = Track(
        album = this.album,
        artists = this.artists,
        duration = null,
        durationSeconds = this.durationSeconds,
        isAvailable = true,
        isExplicit = this.isExplicit ?: false,
        likeStatus = null,
        thumbnails = this.thumbnails,
        title = this.title,
        videoId = this.videoId ?: "",
        videoType = this.videoType,
        category = null,
        feedbackTokens = null,
        resultType = null
    )

    private fun createFallbackTrack(state: NowPlayingTrackState): Track = Track(
        album = null,
        artists = null,
        duration = null,
        durationSeconds = 0,
        isAvailable = true,
        isExplicit = false,
        likeStatus = null,
        thumbnails = null,
        title = state.mediaItem.metadata.title ?: "Unknown",
        videoId = state.mediaItem.mediaId,
        videoType = null,
        category = null,
        feedbackTokens = null,
        resultType = null
    )
}
