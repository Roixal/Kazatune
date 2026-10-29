package com.kazatune.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.canvas.CanvasResult
import com.maxrave.domain.data.model.home.Content
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.domain.data.model.streams.YouTubeWatchEndpoint
import com.maxrave.domain.extension.toGenericMediaItem
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.DownloadHandler
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class SkimTab {
    FOR_YOU,
    EXPLORE
}

data class ExploreCategory(
    val title: String,
    val tracks: List<Track>
)

class SkimViewModel(
    private val mediaPlayerHandler: MediaPlayerHandler,
    private val homeRepository: HomeRepository,
    private val songRepository: SongRepository,
    private val lyricsCanvasRepository: LyricsCanvasRepository,
    private val streamRepository: StreamRepository,
    private val downloadHandler: DownloadHandler,
    private val dataStoreManager: DataStoreManager
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

    // Room-persisted liked songs
    private val _likedVideoIds = MutableStateFlow<Set<String>>(emptySet())
    val likedVideoIds: StateFlow<Set<String>> = _likedVideoIds.asStateFlow()

    // Download state tracking
    private val _downloadedVideoIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadedVideoIds: StateFlow<Set<String>> = _downloadedVideoIds.asStateFlow()

    private val _downloadingVideoIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingVideoIds: StateFlow<Set<String>> = _downloadingVideoIds.asStateFlow()

    // Rich Explore Shelves
    private val _exploreCategories = MutableStateFlow<List<ExploreCategory>>(emptyList())
    val exploreCategories: StateFlow<List<ExploreCategory>> = _exploreCategories.asStateFlow()

    private val _selectedGenre = MutableStateFlow("All")
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    private var lyricsJob: Job? = null
    private var radioJob: Job? = null
    private var progressTickerJob: Job? = null
    private var refreshCounter = 0

    init {
        loadFeed(SkimTab.FOR_YOU)
        observeLikedAndDownloaded()
        observePlayback()
        startPositionTicker()
        loadExploreData()
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

    private fun observeLikedAndDownloaded() {
        viewModelScope.launch(Dispatchers.IO) {
            songRepository.getLikedSongs().collect { songs ->
                _likedVideoIds.value = songs.map { it.videoId }.toSet()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            songRepository.getDownloadedSongs().collect { songs ->
                _downloadedVideoIds.value = songs?.map { it.videoId }?.toSet() ?: emptySet()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            downloadHandler.downloadTask.collect { tasks ->
                _downloadingVideoIds.value = tasks.filter {
                    it.value == DownloadHandler.STATE_DOWNLOADING || it.value == DownloadHandler.STATE_QUEUED
                }.keys
            }
        }
    }

    fun selectSkimTab(tab: SkimTab) {
        if (_skimTab.value == tab) return
        _skimTab.value = tab
        if (tab == SkimTab.EXPLORE && _exploreCategories.value.isEmpty()) {
            loadExploreData()
        }
    }

    /**
     * Bug 6 Fix: Refresh button actually fetches fresh, varied tracks rather than
     * reloading the identical static 5 songs!
     */
    fun refreshFeed() {
        refreshCounter++
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (_skimTab.value == SkimTab.FOR_YOU) {
                    // Fetch mixed shelf or chart shelf based on refresh counter
                    if (refreshCounter % 2 == 1) {
                        homeRepository.getNewRelease("New Releases", "Videos").collect { res ->
                            if (res is Resource.Success && !res.data.isNullOrEmpty()) {
                                val list = res.data!!.flatMap { it.contents }.filterNotNull().map { it.toTrack() }.shuffled()
                                if (list.isNotEmpty()) {
                                    _tracks.value = list
                                    _currentIndex.value = 0
                                    playTrackAt(0)
                                }
                            }
                        }
                    } else {
                        homeRepository.getHomeData(viewString = "views", songString = "songs").collect { res ->
                            if (res is Resource.Success) {
                                val shelves = res.data?.second ?: emptyList()
                                val list = shelves.flatMap { it.contents }.filterNotNull().map { it.toTrack() }.shuffled()
                                if (list.isNotEmpty()) {
                                    _tracks.value = list
                                    _currentIndex.value = 0
                                    playTrackAt(0)
                                }
                            }
                        }
                    }
                } else {
                    loadExploreData(_selectedGenre.value)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadFeed(tab: SkimTab = _skimTab.value) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectGenre(genre: String) {
        _selectedGenre.value = genre
        loadExploreData(genre)
    }

    /**
     * Bug 10: Rich YouTube Music style Explore screen with categorized carousels
     */
    fun loadExploreData(genre: String = "All") {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val categories = mutableListOf<ExploreCategory>()

                // 1. New Releases
                homeRepository.getNewRelease("New Releases", "Music Videos").collect { res ->
                    if (res is Resource.Success && !res.data.isNullOrEmpty()) {
                        val tracks = res.data!!.flatMap { it.contents }.filterNotNull().map { it.toTrack() }
                        if (tracks.isNotEmpty()) {
                            categories.add(ExploreCategory("🔥 New Releases", tracks.take(12)))
                        }
                    }
                }

                // 2. Charts / Trending
                homeRepository.getHomeData(viewString = "views", songString = "songs").collect { res ->
                    if (res is Resource.Success) {
                        val shelves = res.data?.second ?: emptyList()
                        val chartShelf = shelves.find {
                            it.title.contains("chart", ignoreCase = true) ||
                            it.title.contains("trending", ignoreCase = true) ||
                            it.title.contains("top", ignoreCase = true)
                        }
                        val chartTracks = (chartShelf?.contents ?: shelves.firstOrNull()?.contents)
                            ?.filterNotNull()
                            ?.map { it.toTrack() } ?: emptyList()
                        if (chartTracks.isNotEmpty()) {
                            categories.add(ExploreCategory("📈 Top Trending Charts", chartTracks.take(12)))
                        }
                    }
                }

                // 3. Recommended & Quick Picks
                homeRepository.getHomeData(viewString = "views", songString = "songs").collect { res ->
                    if (res is Resource.Success) {
                        val shelves = res.data?.second ?: emptyList()
                        shelves.take(3).forEach { shelf ->
                            val tracks = shelf.contents.filterNotNull().map { it.toTrack() }
                            if (tracks.isNotEmpty()) {
                                val title = shelf.title.ifBlank { "Recommended For You" }
                                categories.add(ExploreCategory(title, tracks.take(10)))
                            }
                        }
                    }
                }

                if (categories.isNotEmpty()) {
                    _exploreCategories.value = categories
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun observePlayback() {
        // Bug 12: Autoplay next track when a song reaches the end!
        viewModelScope.launch {
            mediaPlayerHandler.simpleMediaState.collectLatest { state ->
                if (state is SimpleMediaState.Ended) {
                    val nextIdx = _currentIndex.value + 1
                    if (nextIdx < _tracks.value.size) {
                        playTrackAt(nextIdx)
                    }
                }
            }
        }

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
                            val newTrack = state.track ?: createFallbackTrack(state)
                            _tracks.value = listOf(newTrack)
                            _currentIndex.value = 0
                            fetchRadioQueue(videoId, newTrack)
                        }

                        // Canvas
                        val currentTrack = _tracks.value.getOrNull(_currentIndex.value)
                        val duration = currentTrack?.durationSeconds ?: 0
                        fetchCanvas(videoId, duration)

                        // Synchronized Lyrics
                        val artist = currentTrack?.artists?.firstOrNull()?.name ?: state.mediaItem.metadata.artist ?: ""
                        val title = currentTrack?.title ?: state.mediaItem.metadata.title ?: ""
                        fetchLyrics(videoId, artist, title, duration)
                    }
                }
        }
    }

    /**
     * Bug 8: Fetches both Spotify Canvas and Apple Music animated artwork
     */
    private fun fetchCanvas(videoId: String, durationSeconds: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _currentCanvas.value = null
            try {
                val canvasEnabled = dataStoreManager.spotifyCanvas.first() == DataStoreManager.Values.TRUE
                if (canvasEnabled) {
                    // Try Spotify Canvas first
                    var found = false
                    lyricsCanvasRepository.getCanvas(dataStoreManager, videoId, durationSeconds).collect { res ->
                        val payload = res.data
                        if (res is Resource.Success && payload != null) {
                            _currentCanvas.value = payload
                            found = true
                        }
                    }
                    if (found) return@launch

                    // Fallback: AM Animated artwork
                    lyricsCanvasRepository.getAMAnimatedArtwork(videoId).collect { res ->
                        val payload = res.data
                        if (res is Resource.Success && payload != null) {
                            _currentCanvas.value = payload
                        }
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

    /**
     * Bug 12: Adds all remaining tracks to MediaPlayerHandler so ExoPlayer
     * autoplays to the next song automatically!
     */
    fun playTrackAt(index: Int) {
        val trackList = _tracks.value
        if (index in trackList.indices) {
            _currentIndex.value = index
            val itemsToQueue = trackList.drop(index).map { it.toGenericMediaItem() }
            mediaPlayerHandler.clearMediaItems()
            mediaPlayerHandler.addMediaItemList(itemsToQueue)
            mediaPlayerHandler.playMediaItemInMediaSource(0)
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
                            val fullQueue = listOf(initialTrack) + filtered
                            _tracks.value = fullQueue
                            // Append upcoming tracks to ExoPlayer so autoplay continues seamlessly!
                            val upcomingMediaItems = filtered.map { it.toGenericMediaItem() }
                            mediaPlayerHandler.addMediaItemList(upcomingMediaItems)
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

    // Bug 5: Liked songs immediate reactive update + Room persistence
    fun toggleLike(track: Track) {
        val isCurrentlyLiked = _likedVideoIds.value.contains(track.videoId)
        // Immediately update in-memory state for instant UI response
        val updatedSet = _likedVideoIds.value.toMutableSet()
        if (isCurrentlyLiked) {
            updatedSet.remove(track.videoId)
        } else {
            updatedSet.add(track.videoId)
        }
        _likedVideoIds.value = updatedSet

        viewModelScope.launch(Dispatchers.IO) {
            if (isCurrentlyLiked) {
                songRepository.updateLikeStatus(track.videoId, 0)
            } else {
                val songEntity = track.toSongEntity().copy(liked = true, likeStatus = "LIKE")
                songRepository.insertSong(songEntity).collect {}
                songRepository.updateLikeStatus(track.videoId, 1)
            }
        }
    }

    // Bug 4: Offline downloads integration
    fun toggleDownload(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            val videoId = track.videoId
            val isDownloaded = _downloadedVideoIds.value.contains(videoId)
            if (isDownloaded) {
                downloadHandler.removeDownload(videoId)
                songRepository.updateDownloadState(videoId, DownloadState.STATE_NOT_DOWNLOADED)
            } else {
                val thumbnail = track.thumbnails?.lastOrNull()?.url ?: ""
                val entity = track.toSongEntity().copy(downloadState = DownloadState.STATE_PREPARING)
                songRepository.insertSong(entity).collect {}
                downloadHandler.downloadTrack(videoId, track.title, thumbnail)
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
