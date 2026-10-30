package com.kazatune.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val importRepository: ImportRepository
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
                    // 1. JSON Backup Format
                    trimmed.startsWith("{") && trimmed.endsWith("}") -> {
                        importRepository.import(trimmed, "Invalid backup file format").collect { progress ->
                            _migrationState.value = MigrationState.Processing("Importing backup...", 1, 1)
                        }
                        _migrationState.value = MigrationState.Success(name.ifBlank { "Restored Playlist" }, 1)
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

                    // 3. Spotify URL or Tracklist (Text lines of Song - Artist)
                    else -> {
                        val lines = trimmed.lines()
                            .map { it.trim().removePrefix("-").removePrefix("•").trim() }
                            .filter { it.isNotBlank() && !it.startsWith("http") }

                        if (lines.isEmpty()) {
                            // If it was a Spotify HTTP URL, prompt user
                            if (trimmed.contains("spotify.com")) {
                                _migrationState.value = MigrationState.Error(
                                    "Spotify link detected. Paste the tracklist (Song - Artist per line) or use Spotify account login in Settings."
                                )
                            } else {
                                _migrationState.value = MigrationState.Error("No valid song names found to convert.")
                            }
                            return@launch
                        }

                        val resolvedTracks = mutableListOf<Track>()
                        val total = lines.size

                        lines.forEachIndexed { index, line ->
                            _migrationState.value = MigrationState.Processing(
                                progress = "Converting \"$line\"...",
                                current = index + 1,
                                total = total
                            )

                            try {
                                val searchRes = searchRepository.getSearchDataSong(line).firstOrNull()
                                val topMatch = searchRes?.data?.firstOrNull()?.toTrack()
                                if (topMatch != null) {
                                    resolvedTracks.add(topMatch)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

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

    private suspend fun saveMigratedPlaylist(title: String, tracks: List<Track>) {
        val videoIds = tracks.map { it.videoId }
        tracks.forEach { track ->
            try {
                songRepository.insertSong(track.toSongEntity())
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
