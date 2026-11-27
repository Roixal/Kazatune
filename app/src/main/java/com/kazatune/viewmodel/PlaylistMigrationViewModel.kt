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
                        val resolvedTracks = resolveTrackQueries(spotifyQueries)

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

    private data class TrackQuery(
        val title: String,
        val artist: String = "",
        val durationSec: Int? = null
    ) {
        val displayQuery: String
            get() = if (artist.isNotBlank()) "$title $artist" else title
    }

    private suspend fun resolveQueriesToTracks(queries: List<String>): List<Track> {
        val structured = queries.map { raw ->
            val parts = raw.split(" - ", " – ", limit = 2)
            if (parts.size == 2) TrackQuery(parts[0].trim(), parts[1].trim())
            else TrackQuery(raw.trim())
        }
        return resolveTrackQueries(structured)
    }

    private suspend fun resolveTrackQueries(queries: List<TrackQuery>): List<Track> {
        val resolvedTracks = mutableListOf<Track>()
        val total = queries.size

        queries.forEachIndexed { index, query ->
            _migrationState.value = MigrationState.Processing(
                progress = "Matching \"${query.title}\"...",
                current = index + 1,
                total = total
            )

            try {
                val primaryArtist = query.artist.split(",").firstOrNull()?.trim().orEmpty()
                val primarySearch = if (primaryArtist.isNotBlank()) "${query.title} $primaryArtist" else query.title
                var bestMatch = searchAndScoreBestTrack(primarySearch, query)

                if (bestMatch == null) {
                    val cleanedTitle = cleanTrackTitleForSearch(query.title)
                    val fallbackSearch = if (primaryArtist.isNotBlank()) "$cleanedTitle $primaryArtist" else cleanedTitle
                    if (fallbackSearch != primarySearch && fallbackSearch.isNotBlank()) {
                        bestMatch = searchAndScoreBestTrack(fallbackSearch, query.copy(title = cleanedTitle))
                    }
                }

                if (bestMatch != null) {
                    resolvedTracks.add(bestMatch)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return resolvedTracks
    }

    private suspend fun searchAndScoreBestTrack(searchText: String, target: TrackQuery): Track? {
        val searchRes = searchRepository.getSearchDataSong(searchText)
            .firstOrNull { it is Resource.Success || it is Resource.Error }
        val candidates = searchRes?.data.orEmpty()
        if (candidates.isEmpty()) return null

        val scored = candidates.mapNotNull { candidate ->
            val score = scoreCandidate(candidate, target)
            candidate to score
        }.maxByOrNull { it.second } ?: return null

        // Require a minimum relevance threshold so random/unrelated songs are never imported
        val minThreshold = if (target.artist.isNotBlank()) 35 else 25
        return if (scored.second >= minThreshold) scored.first.toTrack() else null
    }

    private fun scoreCandidate(candidate: SongsResult, target: TrackQuery): Int {
        val candTitle = normalizeText(candidate.title.orEmpty())
        val targetTitle = normalizeText(target.title)
        if (candTitle.isBlank() || targetTitle.isBlank()) return -100

        var score = 0

        // 1. Title similarity
        when {
            candTitle == targetTitle -> score += 60
            candTitle.contains(targetTitle) || targetTitle.contains(candTitle) -> score += 38
            else -> {
                val targetTokens = targetTitle.split(" ").filter { it.length > 1 }.toSet()
                val candTokens = candTitle.split(" ").filter { it.length > 1 }.toSet()
                if (targetTokens.isNotEmpty()) {
                    val overlap = targetTokens.intersect(candTokens).size.toFloat() / targetTokens.size
                    score += (overlap * 35).toInt()
                }
            }
        }

        // 2. Artist similarity
        if (target.artist.isNotBlank()) {
            val targetArtists = target.artist.split(",").map { normalizeText(it) }.filter { it.isNotBlank() }
            val candArtists = candidate.artists?.mapNotNull { normalizeText(it.name.orEmpty()).takeIf { n -> n.isNotBlank() } }.orEmpty()
            val artistMatched = targetArtists.any { ta ->
                candArtists.any { ca -> ca == ta || ca.contains(ta) || ta.contains(ca) }
            }
            if (artistMatched) score += 35 else score -= 15
        }

        // 3. Duration match (when available)
        val targetDur = target.durationSec
        val candDur = candidate.durationSeconds
        if (targetDur != null && targetDur > 0 && candDur != null && candDur > 0) {
            val diff = kotlin.math.abs(targetDur - candDur)
            when {
                diff <= 5 -> score += 20
                diff <= 12 -> score += 10
                diff > 45 -> score -= 25
            }
        }

        // 4. Penalize unwanted covers/remixes unless requested in original title
        val unwantedTags = listOf("karaoke", "instrumental", "8d audio", "slowed", "reverb", "nightcore", "sped up")
        for (tag in unwantedTags) {
            if (candTitle.contains(tag) && !targetTitle.contains(tag)) {
                score -= 40
            }
        }

        return score
    }

    private fun cleanTrackTitleForSearch(title: String): String {
        return title
            .replace(Regex("""(?i)\s*[\(\[](feat\.?|ft\.?|with|remaster(?:ed)?|explicit|album\s*version)[^\)\]]*[\)\]]"""), "")
            .replace(Regex("""(?i)\s*-\s*(remaster(?:ed)?|single\s*version|radio\s*edit).*$"""), "")
            .trim()
    }

    private fun normalizeText(input: String): String {
        return input.lowercase()
            .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun fetchSpotifyPlaylistTracks(rawUrl: String): Pair<String, List<TrackQuery>>? {
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
            val queries = mutableListOf<TrackQuery>()
            for (i in 0 until trackList.length()) {
                val item = trackList.getJSONObject(i)
                val trackTitle = item.optString("title", "").replace('\u00A0', ' ').trim()
                val subtitle = item.optString("subtitle", "").replace('\u00A0', ' ').trim()
                val durationMs = item.optInt("duration", 0)
                val durationSec = if (durationMs > 0) durationMs / 1000 else null
                if (trackTitle.isNotBlank()) {
                    queries.add(TrackQuery(title = trackTitle, artist = subtitle, durationSec = durationSec))
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
