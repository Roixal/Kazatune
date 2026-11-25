package com.kazatune.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kazatune.mix.PlaylistMixManager
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.PairSongLocalPlaylist
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.searchResult.songs.SongsResult
import com.maxrave.domain.repository.ImportRepository
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.SearchRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.toSongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

sealed interface MigrationState {
    data object Idle : MigrationState
    data class Processing(val progress: String, val current: Int, val total: Int) : MigrationState
    data class Success(val playlistTitle: String, val songCount: Int) : MigrationState
    data class Error(val message: String) : MigrationState
}

/**
 * Playlist Migration & Import ViewModel.
 *
 * Imports and converts playlists from Spotify (links or tracklists),
 * YouTube / YouTube Music (links or IDs), Apple Music text exports,
 * and Skim/SimpMusic JSON backups.
 */
class PlaylistMigrationViewModel(
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val playlistRepository: PlaylistRepository,
    private val searchRepository: SearchRepository,
    private val songRepository: SongRepository,
    private val importRepository: ImportRepository,
    private val playlistMixManager: PlaylistMixManager
) : ViewModel() {

    private val _migrationState = MutableStateFlow<MigrationState>(MigrationState.Idle)
    val migrationState: StateFlow<MigrationState> = _migrationState.asStateFlow()

    fun resetState() {
        _migrationState.value = MigrationState.Idle
    }

    fun migratePlaylist(name: String, rawInput: String) {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            _migrationState.value = MigrationState.Error("Please enter a playlist URL, tracklist, or JSON data")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _migrationState.value = MigrationState.Processing("Analyzing import data...", 0, 0)
            try {
                when {
                    // 1. Skim Mixed Playlist JSON or Backup Format
                    trimmed.startsWith("{") && trimmed.endsWith("}") -> {
                        val mixedPayload = playlistMixManager.parseImportedMixedPlaylist(trimmed, "temp")
                        if (mixedPayload != null && mixedPayload.trackQueries.isNotEmpty()) {
                            val resolvedTracks = resolveQueriesToTracks(mixedPayload.trackQueries.map { it.second })
                            if (resolvedTracks.isEmpty()) {
                                _migrationState.value = MigrationState.Error("Could not match tracks from shared Mixed Playlist.")
                                return@launch
                            }
                            val finalTitle = name.ifBlank { mixedPayload.title }
                            saveMigratedPlaylist(title = finalTitle, tracks = resolvedTracks)
                            val savedPlaylist = localPlaylistRepository.getAllLocalPlaylists().firstOrNull()
                                ?.firstOrNull { it.title == finalTitle }
                            if (savedPlaylist != null) {
                                val remappedId = "local_${savedPlaylist.id}"
                                playlistMixManager.saveSettings(
                                    mixedPayload.settings.copy(playlistId = remappedId, mixEnabled = true)
                                )
                            }
                        } else {
                            importRepository.import(trimmed, "Invalid backup file format").collect {
                                _migrationState.value = MigrationState.Processing("Importing backup...", 1, 1)
                            }
                            _migrationState.value = MigrationState.Success(name.ifBlank { "Restored Playlist" }, 1)
                        }
                    }

                    // 2. YouTube / YouTube Music Playlist URL
                    trimmed.contains("youtube.com") || trimmed.contains("youtu.be") -> {
                        val playlistId = extractYouTubePlaylistId(trimmed)
                        if (playlistId.isNullOrBlank()) {
                            _migrationState.value = MigrationState.Error("Could not parse YouTube playlist ID from URL")
                            return@launch
                        }

                        _migrationState.value = MigrationState.Processing("Fetching playlist from YouTube Music...", 0, 1)
                        playlistRepository.getFullPlaylistData(playlistId, "views").collect { res ->
                            if (res is Resource.Success && res.data != null) {
                                val playlistData = res.data!!
                                val tracks = playlistData.tracks
                                if (tracks.isEmpty()) {
                                    _migrationState.value = MigrationState.Error("Playlist contains no playable tracks")
                                    return@collect
                                }

                                saveMigratedPlaylist(
                                    title = name.ifBlank { playlistData.title ?: "Imported Playlist" },
                                    tracks = tracks
                                )
                            } else if (res is Resource.Error) {
                                _migrationState.value = MigrationState.Error(res.message ?: "Failed to fetch playlist")
                            }
                        }
                    }

                    // 3. Spotify Playlist / Album URL
                    trimmed.contains("spotify.com") || trimmed.contains("spotify.link") || trimmed.contains("spoti.fi") -> {
                        _migrationState.value = MigrationState.Processing("Fetching Spotify playlist...", 0, 1)
                        val spotifyResult = fetchSpotifyPlaylistTracks(trimmed)
                        if (spotifyResult == null || spotifyResult.second.isEmpty()) {
                            _migrationState.value = MigrationState.Error(
                                "Could not read this Spotify link. Make sure the playlist is public, or paste the tracklist (Song - Artist per line)."
                            )
                            return@launch
                        }

                        val (spotifyTitle, spotifyQueries) = spotifyResult
                        val resolvedTracks = resolveQueriesToTracks(spotifyQueries)

                        if (resolvedTracks.isEmpty()) {
                            _migrationState.value = MigrationState.Error("Could not match any Spotify tracks on YouTube Music.")
                            return@launch
                        }

                        saveMigratedPlaylist(
                            title = name.ifBlank { spotifyTitle.ifBlank { "Spotify Import" } },
                            tracks = resolvedTracks
                        )
                    }

                    // 4. Plain Tracklist (Text lines of Song - Artist)
                    else -> {
                        val lines = trimmed.lines()
                            .map { it.trim().removePrefix("-").removePrefix("•").trim() }
                            .filter { it.isNotBlank() && !it.startsWith("http") }

                        if (lines.isEmpty()) {
                            _migrationState.value = MigrationState.Error("No valid song names found to convert.")
                            return@launch
                        }

                        val resolvedTracks = resolveQueriesToTracks(lines)

                        if (resolvedTracks.isEmpty()) {
                            _migrationState.value = MigrationState.Error("Could not match any tracks on YouTube Music.")
                            return@launch
                        }

                        saveMigratedPlaylist(
                            title = name.ifBlank { "Migrated Playlist" },
                            tracks = resolvedTracks
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _migrationState.value = MigrationState.Error("Migration error: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    private suspend fun resolveQueriesToTracks(queries: List<String>): List<Track> {
        val resolvedTracks = mutableListOf<Track>()
        val total = queries.size

        queries.forEachIndexed { index, query ->
            _migrationState.value = MigrationState.Processing(
                progress = "Matching \"$query\"...",
                current = index + 1,
                total = total
            )

            try {
                val searchRes = searchRepository.getSearchDataSong(query).firstOrNull { it is Resource.Success || it is Resource.Error }
                val topMatch = searchRes?.data?.firstOrNull()?.toTrack()
                if (topMatch != null) {
                    resolvedTracks.add(topMatch)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return resolvedTracks
    }

    private fun fetchSpotifyPlaylistTracks(rawUrl: String): Pair<String, List<String>>? {
        return try {
            var resolvedUrl = rawUrl.lines().firstOrNull { it.contains("http") }?.trim() ?: rawUrl.trim()
            if (resolvedUrl.contains("spotify.link") || resolvedUrl.contains("spoti.fi")) {
                val conn = (URL(resolvedUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }
                conn.inputStream.use {}
                resolvedUrl = conn.url.toString()
            }

            val match = Regex("""open\.spotify\.com/(?:intl-[a-z-]+/)?(playlist|album)/([a-zA-Z0-9]+)""")
                .find(resolvedUrl) ?: return null
            val entityType = match.groupValues[1]
            val entityId = match.groupValues[2]

            val embedUrl = "https://open.spotify.com/embed/$entityType/$entityId"
            val conn = (URL(embedUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 12_000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }
            val html = conn.inputStream.bufferedReader().use { it.readText() }
            val nextDataMatch = Regex("""<script id="__NEXT_DATA__" type="application/json">(.*?)</script>""")
                .find(html) ?: return null

            val root = JSONObject(nextDataMatch.groupValues[1])
            val entity = root
                .getJSONObject("props")
                .getJSONObject("pageProps")
                .getJSONObject("state")
                .getJSONObject("data")
                .getJSONObject("entity")

            val title = entity.optString("name", "").ifBlank { entity.optString("title", "Spotify Playlist") }
            val trackList = entity.optJSONArray("trackList") ?: return null
            val queries = mutableListOf<String>()
            for (i in 0 until trackList.length()) {
                val item = trackList.getJSONObject(i)
                val trackTitle = item.optString("title", "").replace('\u00A0', ' ').trim()
                val subtitle = item.optString("subtitle", "").replace('\u00A0', ' ').trim()
                if (trackTitle.isNotBlank()) {
                    queries.add(if (subtitle.isNotBlank()) "$trackTitle $subtitle" else trackTitle)
                }
            }
            Pair(title, queries)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun saveMigratedPlaylist(title: String, tracks: List<Track>) {
        val videoIds = tracks.map { it.videoId }
        tracks.forEach { track ->
            try {
                songRepository.insertSong(track.toSongEntity()).collect {}
            } catch (_: Exception) {}
        }

        val playlistEntity = LocalPlaylistEntity(
            title = title,
            tracks = videoIds,
            thumbnail = tracks.firstOrNull()?.thumbnails?.lastOrNull()?.url
        )

        localPlaylistRepository.insertLocalPlaylist(playlistEntity, "Success").collect {
            _migrationState.value = MigrationState.Success(
                playlistTitle = title,
                songCount = tracks.size
            )
        }
    }

    private fun extractYouTubePlaylistId(url: String): String? {
        val regex = Regex("""[?&]list=([^&#]+)""")
        return regex.find(url)?.groupValues?.getOrNull(1)
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
