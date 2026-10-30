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
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.mediaservice.handler.SimpleMediaState
import com.maxrave.domain.mediaservice.handler.ToastType
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
import kotlinx.coroutines.withContext

enum class SkimTab {
    FOR_YOU,
    EXPLORE,
    MOODS
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

    // Active Mood Chip Filter (Component 5)
    private val _activeMood = MutableStateFlow("⚡ All")
    val activeMood: StateFlow<String> = _activeMood.asStateFlow()

    private val _moodTracks = MutableStateFlow<List<Track>>(emptyList())
    val moodTracks: StateFlow<List<Track>> = _moodTracks.asStateFlow()

    private val _isMoodLoading = MutableStateFlow(false)
    val isMoodLoading: StateFlow<Boolean> = _isMoodLoading.asStateFlow()

    // Rich Explore Shelves
    private val _exploreCategories = MutableStateFlow<List<ExploreCategory>>(emptyList())
    val exploreCategories: StateFlow<List<ExploreCategory>> = _exploreCategories.asStateFlow()

    private val _selectedGenre = MutableStateFlow("All")
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    // Return YouTube Dislike State
    private val _dislikeCount = MutableStateFlow<Int?>(null)
    val dislikeCount: StateFlow<Int?> = _dislikeCount.asStateFlow()

    private val _isDisliked = MutableStateFlow(false)
    val isDisliked: StateFlow<Boolean> = _isDisliked.asStateFlow()

    // SponsorBlock Skipping & Toast
    private val _sponsorBlockToast = MutableStateFlow<String?>(null)
    val sponsorBlockToast: StateFlow<String?> = _sponsorBlockToast.asStateFlow()

    private val _sponsorBlockEnabled = MutableStateFlow(true)
    val sponsorBlockEnabled: StateFlow<Boolean> = _sponsorBlockEnabled.asStateFlow()

    // Synced Lyrics Timing Offset Calibration (ms)
    private val _lyricsOffsetMs = MutableStateFlow(0L)
    val lyricsOffsetMs: StateFlow<Long> = _lyricsOffsetMs.asStateFlow()

    private var lyricsJob: Job? = null
    private var radioJob: Job? = null
    private var progressTickerJob: Job? = null
    private var refreshCounter = 0
    private var loadingMore = false
    private var lastScrobbledVideoId: String? = null
    private var trackStartEpochSeconds: Long = 0L

    init {
        loadFeed(SkimTab.FOR_YOU)
        observeLikedAndDownloaded()
        observePlayback()
        observeSponsorBlock()
        startPositionTicker()
        // explore data is lazy-loaded when the user selects Explore tab
    }

    private fun startPositionTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                if (controlState.value.isPlaying) {
                    val player = mediaPlayerHandler.player
                    val pos = player.currentPosition
                    if (pos >= 0) {
                        _currentPositionMs.value = pos
                    }
                    val dur = player.duration
                    if (dur > 0) {
                        _totalDurationMs.value = dur
                    }
                    checkLastFmScrobble(pos, dur)
                }
                delay(200)
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
        } else if (tab == SkimTab.MOODS && _moodTracks.value.isEmpty()) {
            loadMoodTracks(_activeMood.value)
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
                        var firstPlaylistId: String? = null
                        homeItems.forEach { section ->
                            section.contents.forEach { content ->
                                if (content != null) {
                                    val vid = content.videoId ?: content.playEndpoint?.videoId
                                    if (!vid.isNullOrBlank()) {
                                        list.add(content.copy(videoId = vid).toTrack())
                                    } else if (firstPlaylistId == null && !content.playlistId.isNullOrBlank()) {
                                        firstPlaylistId = content.playlistId
                                    }
                                }
                            }
                        }
                        val activeVideoId = mediaPlayerHandler.nowPlayingState.value.mediaItem.mediaId
                        val isPlaying = controlState.value.isPlaying || activeVideoId.isNotBlank()

                        if (list.isNotEmpty()) {
                            if (isPlaying && activeVideoId.isNotBlank()) {
                                // Align currently playing song so artwork matches what is playing 100%!
                                val foundIdx = list.indexOfFirst { it.videoId == activeVideoId }
                                if (foundIdx >= 0) {
                                    _tracks.value = list
                                    _currentIndex.value = foundIdx
                                } else {
                                    val currentTrack = _tracks.value.firstOrNull { it.videoId == activeVideoId }
                                        ?: mediaPlayerHandler.nowPlayingState.value.track
                                        ?: createFallbackTrack(mediaPlayerHandler.nowPlayingState.value)
                                    val mergedList = listOf(currentTrack) + list.filter { it.videoId != activeVideoId }
                                    _tracks.value = mergedList
                                    _currentIndex.value = 0
                                }
                                mediaPlayerHandler.setQueueData(
                                    QueueData.Data(
                                        listTracks = _tracks.value,
                                        playlistType = PlaylistType.RADIO,
                                        playlistId = "SKIM_FEED"
                                    )
                                )
                            } else {
                                setTracksAndPlay(list, 0)
                            }
                        } else if (firstPlaylistId != null && !isPlaying) {
                            songRepository.getRadioFromEndpoint(YouTubeWatchEndpoint(playlistId = firstPlaylistId)).collect { res ->
                                val tracks = res.data?.first
                                if (!tracks.isNullOrEmpty()) {
                                    setTracksAndPlay(tracks, 0)
                                }
                            }
                        } else if (!isPlaying) {
                            // Fallback to New Releases if no playlists
                            homeRepository.getNewRelease("New Releases", "Music Videos").collect { nRes ->
                                val nList = nRes.data?.firstOrNull()?.contents?.mapNotNull { c ->
                                    if (c != null) {
                                        val vid = c.videoId ?: c.playEndpoint?.videoId
                                        if (!vid.isNullOrBlank()) c.copy(videoId = vid).toTrack() else null
                                    } else null
                                }
                                if (!nList.isNullOrEmpty()) {
                                    setTracksAndPlay(nList, 0)
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

                if (genre != "All") {
                    homeRepository.getMoodData(genre).collect { res ->
                        if (res is Resource.Success && res.data != null) {
                            val items = res.data!!.items
                            items.forEach { section ->
                                val tracks = section.contents.map { it.toTrack() }.filter { it.videoId.isNotBlank() }
                                if (tracks.isNotEmpty()) {
                                    val title = section.header.ifBlank { "$genre Mix" }
                                    categories.add(ExploreCategory(title, tracks.take(12)))
                                }
                            }
                        }
                    }
                    if (categories.isNotEmpty()) {
                        _exploreCategories.value = categories
                        return@launch
                    }
                }

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
                    } else {
                        val lastTrack = _tracks.value.lastOrNull()
                        if (lastTrack != null && !loadingMore) {
                            fetchRadioQueue(lastTrack.videoId, lastTrack)
                        }
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
                            if (_currentIndex.value != foundIndex) {
                                _currentIndex.value = foundIndex
                            }
                        } else if (currentList.isEmpty()) {
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

                        // Return YouTube Dislike & SponsorBlock & Last.fm
                        fetchDislikeInfo(videoId)
                        resetLyricsOffset()
                        updateLastFmNowPlaying(videoId)
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

    fun setTracksAndPlay(tracks: List<Track>, initialIndex: Int = 0) {
        if (tracks.isEmpty()) return
        _tracks.value = tracks
        val safeIndex = initialIndex.coerceIn(0, tracks.size - 1)
        _currentIndex.value = safeIndex
        val track = tracks[safeIndex]

        _currentPositionMs.value = 0L
        val durSec = track.durationSeconds ?: 0
        if (durSec > 0) {
            _totalDurationMs.value = durSec * 1000L
        }

        fetchCanvas(track.videoId, durSec)
        val artist = track.artists?.firstOrNull()?.name ?: ""
        fetchLyrics(track.videoId, artist, track.title, durSec)
        resetLyricsOffset()
        updateLastFmNowPlaying(track.videoId)

        viewModelScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                songRepository.insertSong(track.toSongEntity()).collect {}
                track.durationSeconds?.let { songRepository.updateDurationSeconds(it, track.videoId) }
            }

            mediaPlayerHandler.setQueueData(
                QueueData.Data(
                    listTracks = tracks,
                    playlistType = PlaylistType.RADIO,
                    playlistId = "SKIM_FEED"
                )
            )

            mediaPlayerHandler.addMediaItem(track.toGenericMediaItem(), playWhenReady = true)
        }

        listOfNotNull(
            tracks.getOrNull(safeIndex + 1),
            tracks.getOrNull(safeIndex + 2)
        ).forEach { preloadTrack(it) }
    }

    /**
     * Seamlessly transitions playback to the specified index without clearing
     * or getting stuck in an asynchronous reset loop.
     */
    fun playTrackAt(index: Int) {
        val trackList = _tracks.value
        if (index !in trackList.indices) return
        _currentIndex.value = index
        val track = trackList[index]

        _currentPositionMs.value = 0L
        val durSec = track.durationSeconds ?: 0
        if (durSec > 0) {
            _totalDurationMs.value = durSec * 1000L
        }

        fetchCanvas(track.videoId, durSec)
        val artist = track.artists?.firstOrNull()?.name ?: ""
        fetchLyrics(track.videoId, artist, track.title, durSec)
        resetLyricsOffset()
        updateLastFmNowPlaying(track.videoId)

        val currentPlayingVideoId = mediaPlayerHandler.nowPlayingState.value.mediaItem.mediaId
        if (currentPlayingVideoId != track.videoId) {
            viewModelScope.launch(Dispatchers.Main) {
                withContext(Dispatchers.IO) {
                    songRepository.insertSong(track.toSongEntity()).collect {}
                    track.durationSeconds?.let { songRepository.updateDurationSeconds(it, track.videoId) }
                }

                mediaPlayerHandler.setQueueData(
                    QueueData.Data(
                        listTracks = trackList,
                        playlistType = PlaylistType.RADIO,
                        playlistId = "SKIM_FEED"
                    )
                )

                mediaPlayerHandler.addMediaItem(track.toGenericMediaItem(), playWhenReady = true)
            }
        }

        listOfNotNull(
            trackList.getOrNull(index + 1),
            trackList.getOrNull(index + 2)
        ).forEach { preloadTrack(it) }

        if (index >= trackList.size - 4 && !loadingMore) {
            fetchRadioQueue(track.videoId, track)
        }
    }

    /**
     * Plays a track and starts a Spotify-style Radio queue based on it,
     * replacing the current Skim list with the song and its recommended watch-next queue.
     */
    fun playTrackAndStartRadio(track: Track) {
        radioJob?.cancel()
        setTracksAndPlay(listOf(track), 0)
        fetchRadioQueue(track.videoId, track)
    }

    private fun fetchRadioQueue(videoId: String, initialTrack: Track) {
        radioJob?.cancel()
        loadingMore = true
        radioJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                songRepository.getRadioFromEndpoint(YouTubeWatchEndpoint(videoId = videoId)).collect { resource ->
                    if (resource is Resource.Success) {
                        val radioTracks = resource.data?.first ?: emptyList()
                        val currentList = _tracks.value
                        val newTracks = radioTracks.filter { rt -> currentList.none { it.videoId == rt.videoId } }
                        if (newTracks.isNotEmpty()) {
                            val updatedList = currentList + newTracks
                            _tracks.value = updatedList
                            mediaPlayerHandler.setQueueData(
                                QueueData.Data(
                                    listTracks = updatedList,
                                    playlistType = PlaylistType.RADIO,
                                    playlistId = "SKIM_FEED"
                                )
                            )
                            newTracks.firstOrNull()?.let { preloadTrack(it) }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                loadingMore = false
            }
        }
    }

    fun onPageSettled(index: Int) {
        playTrackAt(index)
    }

    fun onPageSwiped(pageIndex: Int) {
        playTrackAt(pageIndex)
    }

    /**
     * Reloads the track if it gets stuck due to network/pipeline issues.
     * Invoked when tapping the rotating vinyl disc.
     */
    fun reloadCurrentTrack() {
        val current = _tracks.value.getOrNull(_currentIndex.value) ?: return
        reloadTrack(current)
    }

    fun reloadTrack(track: Track) {
        viewModelScope.launch(Dispatchers.Main) {
            _sponsorBlockToast.value = "🔄 Reloading \"${track.title.take(20)}\"..."
            _currentPositionMs.value = 0L

            withContext(Dispatchers.IO) {
                try {
                    streamRepository.updateFormat(track.videoId)
                } catch (_: Exception) {}
                try {
                    streamRepository.getStream(
                        dataStoreManager = dataStoreManager,
                        videoId = track.videoId,
                        isDownloading = false,
                        isVideo = false
                    ).collect {}
                } catch (_: Exception) {}
            }

            mediaPlayerHandler.setQueueData(
                QueueData.Data(
                    listTracks = _tracks.value,
                    playlistType = PlaylistType.RADIO,
                    playlistId = "SKIM_FEED"
                )
            )

            mediaPlayerHandler.addMediaItem(track.toGenericMediaItem(), playWhenReady = true)
        }
    }

    private fun preloadTrack(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Preload stream URL into cache
                streamRepository.getStream(
                    dataStoreManager = dataStoreManager,
                    videoId = track.videoId,
                    isDownloading = false,
                    isVideo = false
                ).collect {}
                // 2. Preload Canvas
                val duration = track.durationSeconds ?: 0
                fetchCanvas(track.videoId, duration)
                // 3. Preload Lyrics
                val artist = track.artists?.firstOrNull()?.name ?: ""
                fetchLyrics(track.videoId, artist, track.title, duration)
            } catch (_: Exception) {}
        }
    }

    // Component 4: Resso-style Hook/Chorus Jump
    fun jumpToHook(track: Track) {
        val durationSeconds = track.durationSeconds ?: 0
        val hookTargetMs = when {
            durationSeconds > 75 -> (durationSeconds * 0.33 * 1000).toLong()
            durationSeconds > 30 -> (durationSeconds * 0.25 * 1000).toLong()
            else -> 0L
        }
        seekTo(hookTargetMs)
    }

    // Component 5: Mood & Genre Feed Selection
    fun selectMood(mood: String) {
        _activeMood.value = mood
        loadMoodTracks(mood)
    }

    fun loadMoodTracks(mood: String) {
        _activeMood.value = mood
        _isMoodLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cleanMood = mood.replace(Regex("""[^a-zA-Z]"""), "").trim()
                homeRepository.getMoodData(cleanMood.ifBlank { "Chill" }).collect { res ->
                    if (res is Resource.Success && res.data != null) {
                        val tracks = res.data!!.items
                            .flatMap { it.contents }
                            .map { it.toTrack() }
                            .filter { it.videoId.isNotBlank() }
                        _moodTracks.value = tracks
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isMoodLoading.value = false
            }
        }
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            mediaPlayerHandler.onPlayerEvent(PlayerEvent.PlayPause)
        }
    }

    fun skipToNext() {
        val next = _currentIndex.value + 1
        if (next < _tracks.value.size) {
            playTrackAt(next)
        }
    }

    fun skipToPrevious() {
        val prev = _currentIndex.value - 1
        if (prev >= 0) {
            playTrackAt(prev)
        }
    }

    fun seekTo(positionMs: Long) {
        _currentPositionMs.value = positionMs
        mediaPlayerHandler.player.seekTo(positionMs)
    }

    fun setSleepTimer(minutes: Int) {
        if (minutes > 0) {
            mediaPlayerHandler.sleepStart(minutes)
        } else {
            mediaPlayerHandler.sleepStop()
        }
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

    private fun com.maxrave.domain.data.model.mood.moodmoments.Content.toTrack(): Track = Track(
        album = null,
        artists = listOf(com.maxrave.domain.data.model.searchResult.songs.Artist(name = this.subtitle, id = null)),
        duration = null,
        durationSeconds = 0,
        isAvailable = true,
        isExplicit = false,
        likeStatus = null,
        thumbnails = this.thumbnails,
        title = this.title,
        videoId = this.videoId ?: "",
        videoType = null,
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

    private fun observeSponsorBlock() {
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.sponsorBlockEnabled.collect { enabled ->
                _sponsorBlockEnabled.value = enabled == DataStoreManager.TRUE
            }
        }
        mediaPlayerHandler.showToast = { type ->
            if (type is ToastType.SponsorBlockSkip) {
                _sponsorBlockToast.value = "⚡ Skipped ${type.category}"
            }
        }
    }

    private fun fetchDislikeInfo(videoId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _dislikeCount.value = null
            _isDisliked.value = false
            try {
                songRepository.getSongInfo(videoId).collectLatest { songInfo ->
                    _dislikeCount.value = songInfo?.dislike
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleDislike(track: Track) {
        val willBeDisliked = !_isDisliked.value
        _isDisliked.value = willBeDisliked
        val current = _dislikeCount.value ?: 0
        _dislikeCount.value = if (willBeDisliked) current + 1 else (current - 1).coerceAtLeast(0)
        viewModelScope.launch(Dispatchers.IO) {
            if (willBeDisliked) {
                songRepository.addToYouTubeDisliked(track.videoId).collectLatest {}
            }
        }
    }

    fun adjustLyricsOffset(deltaMs: Long) {
        _lyricsOffsetMs.value += deltaMs
    }

    fun resetLyricsOffset() {
        _lyricsOffsetMs.value = 0L
    }

    fun dismissSponsorBlockToast() {
        _sponsorBlockToast.value = null
    }

    fun toggleSponsorBlock() {
        viewModelScope.launch(Dispatchers.IO) {
            val newVal = !_sponsorBlockEnabled.value
            _sponsorBlockEnabled.value = newVal
            dataStoreManager.setSponsorBlockEnabled(newVal)
        }
    }

    private fun updateLastFmNowPlaying(videoId: String) {
        lastScrobbledVideoId = null
        trackStartEpochSeconds = System.currentTimeMillis() / 1000
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sessionKey = dataStoreManager.lastfmSessionKey.firstOrNull() ?: ""
                val scrobbleEnabled = dataStoreManager.lastfmScrobbleEnabled.firstOrNull() == DataStoreManager.TRUE
                if (sessionKey.isNotBlank() && scrobbleEnabled) {
                    val track = _tracks.value.find { it.videoId == videoId }
                    if (track != null) {
                        org.simpmusic.lastfm.updateNowPlaying(
                            sessionKey = sessionKey,
                            track = org.simpmusic.lastfm.LastfmTrack(
                                artist = track.artists?.firstOrNull()?.name ?: "",
                                track = track.title,
                                durationSeconds = track.durationSeconds
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkLastFmScrobble(positionMs: Long, totalMs: Long) {
        val currentVideoId = nowPlaying.value.track?.videoId ?: nowPlaying.value.mediaItem.mediaId
        if (currentVideoId.isBlank() || currentVideoId == lastScrobbledVideoId) return
        val thresholdMs = minOf(totalMs / 2, 240_000L).coerceAtLeast(30_000L)
        if (positionMs >= thresholdMs) {
            lastScrobbledVideoId = currentVideoId
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val sessionKey = dataStoreManager.lastfmSessionKey.firstOrNull() ?: ""
                    val scrobbleEnabled = dataStoreManager.lastfmScrobbleEnabled.firstOrNull() == DataStoreManager.TRUE
                    if (sessionKey.isNotBlank() && scrobbleEnabled) {
                        val track = _tracks.value.find { it.videoId == currentVideoId }
                        if (track != null) {
                            org.simpmusic.lastfm.scrobble(
                                sessionKey = sessionKey,
                                track = org.simpmusic.lastfm.LastfmTrack(
                                    artist = track.artists?.firstOrNull()?.name ?: "",
                                    track = track.title,
                                    durationSeconds = track.durationSeconds
                                ),
                                startedAtEpochSeconds = trackStartEpochSeconds
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
