package com.kazatune.mix

import android.content.Context
import android.content.SharedPreferences
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.manager.DataStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class MixPreset(val label: String, val description: String) {
    AUTO("Auto", "Beat & Camelot key-matched DJ mix"),
    CROSSFADE("Crossfade", "Equal-power blend with DJ filter sweep"),
    OVERLAP("Overlap", "Smooth dual-track beat overlap"),
    FADE("Fade", "Gentle fade out & fade in"),
    CUT("Cut", "Clean instant beat cut")
}

enum class MixEqFilter(val label: String) {
    AUTO("Auto Sweep"),
    LOW_PASS("Low-Pass"),
    HIGH_PASS("High-Pass"),
    OFF("Off")
}

data class PairTransition(
    val preset: MixPreset = MixPreset.AUTO,
    val bars: Int = 8,
    val eqFilter: MixEqFilter = MixEqFilter.AUTO
)

data class PlaylistMixSettings(
    val playlistId: String,
    val mixEnabled: Boolean = false,
    val preset: MixPreset = MixPreset.AUTO,
    val bars: Int = 8,
    val djFilterEnabled: Boolean = true,
    val pairOverrides: Map<String, PairTransition> = emptyMap()
)

data class ImportedMixPlaylist(
    val title: String,
    val trackQueries: List<Pair<String, String>>, // videoId to "title artist"
    val settings: PlaylistMixSettings
)

/**
 * Manages Spotify-style DJ Playlist Mixing configurations per playlist,
 * per-pair transition overrides, Camelot key formatting, and sharing/importing
 * mixed playlists, driving CrossfadeExoPlayerAdapter via DataStoreManager.
 */
class PlaylistMixManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("skim_playlist_mix", Context.MODE_PRIVATE)

    private val _mixConfigs = MutableStateFlow<Map<String, PlaylistMixSettings>>(loadAll())
    val mixConfigs: StateFlow<Map<String, PlaylistMixSettings>> = _mixConfigs.asStateFlow()

    private val _activeMixPlaylistId = MutableStateFlow<String?>(null)
    val activeMixPlaylistId: StateFlow<String?> = _activeMixPlaylistId.asStateFlow()

    fun getSettings(playlistId: String): PlaylistMixSettings {
        return _mixConfigs.value[playlistId] ?: PlaylistMixSettings(playlistId = playlistId)
    }

    fun toggleMix(playlistId: String): PlaylistMixSettings {
        val current = getSettings(playlistId)
        val updated = current.copy(mixEnabled = !current.mixEnabled)
        saveSettings(updated)
        return updated
    }

    fun saveSettings(settings: PlaylistMixSettings) {
        val updatedMap = _mixConfigs.value.toMutableMap().apply {
            put(settings.playlistId, settings)
        }
        _mixConfigs.value = updatedMap
        prefs.edit().putString(settings.playlistId, serializeSettings(settings)).apply()
    }

    fun setPairTransition(
        playlistId: String,
        fromVideoId: String,
        toVideoId: String,
        transition: PairTransition
    ) {
        val current = getSettings(playlistId)
        val key = "${fromVideoId}_$toVideoId"
        val updatedOverrides = current.pairOverrides.toMutableMap().apply {
            put(key, transition)
        }
        saveSettings(current.copy(pairOverrides = updatedOverrides))
    }

    fun getPairTransition(
        playlistId: String,
        fromVideoId: String,
        toVideoId: String
    ): PairTransition {
        val settings = getSettings(playlistId)
        val key = "${fromVideoId}_$toVideoId"
        return settings.pairOverrides[key] ?: PairTransition(
            preset = settings.preset,
            bars = settings.bars,
            eqFilter = if (settings.djFilterEnabled) MixEqFilter.AUTO else MixEqFilter.OFF
        )
    }

    suspend fun applyMixForPlayback(
        playlistId: String?,
        dataStoreManager: DataStoreManager,
        fromVideoId: String? = null,
        toVideoId: String? = null
    ) {
        _activeMixPlaylistId.value = playlistId
        if (playlistId.isNullOrBlank()) return
        val settings = getSettings(playlistId)
        if (!settings.mixEnabled) {
            return
        }

        val transition = if (!fromVideoId.isNullOrBlank() && !toVideoId.isNullOrBlank()) {
            getPairTransition(playlistId, fromVideoId, toVideoId)
        } else {
            PairTransition(
                preset = settings.preset,
                bars = settings.bars,
                eqFilter = if (settings.djFilterEnabled) MixEqFilter.AUTO else MixEqFilter.OFF
            )
        }

        when (transition.preset) {
            MixPreset.AUTO -> {
                dataStoreManager.setCrossfadeEnabled(true)
                dataStoreManager.setCrossfadeDuration(DataStoreManager.CROSSFADE_DURATION_AUTO)
                dataStoreManager.setCrossfadeDjMode(transition.eqFilter != MixEqFilter.OFF)
            }
            MixPreset.CROSSFADE -> {
                val durationMs = (transition.bars * 1500).coerceIn(3000, 15000)
                dataStoreManager.setCrossfadeEnabled(true)
                dataStoreManager.setCrossfadeDuration(durationMs)
                dataStoreManager.setCrossfadeDjMode(transition.eqFilter != MixEqFilter.OFF)
            }
            MixPreset.OVERLAP -> {
                val durationMs = (transition.bars * 1200).coerceIn(2500, 12000)
                dataStoreManager.setCrossfadeEnabled(true)
                dataStoreManager.setCrossfadeDuration(durationMs)
                dataStoreManager.setCrossfadeDjMode(false)
            }
            MixPreset.FADE -> {
                dataStoreManager.setCrossfadeEnabled(true)
                dataStoreManager.setCrossfadeDuration(3500)
                dataStoreManager.setCrossfadeDjMode(false)
            }
            MixPreset.CUT -> {
                dataStoreManager.setCrossfadeEnabled(false)
                dataStoreManager.setCrossfadeDjMode(false)
            }
        }
    }

    fun exportMixedPlaylistJson(title: String, tracks: List<Track>, playlistId: String): String {
        val settings = getSettings(playlistId)
        val root = JSONObject()
        root.put("skim_mix_version", 1)
        root.put("title", title)
        root.put("mix", JSONObject(serializeSettings(settings)))
        val tracksArray = JSONArray()
        tracks.forEach { t ->
            val obj = JSONObject()
            obj.put("videoId", t.videoId)
            obj.put("title", t.title)
            obj.put("artist", t.artists?.firstOrNull()?.name ?: "")
            tracksArray.put(obj)
        }
        root.put("tracks", tracksArray)
        return root.toString(2)
    }

    fun parseImportedMixedPlaylist(rawJson: String, newPlaylistId: String): ImportedMixPlaylist? {
        return try {
            val root = JSONObject(rawJson)
            if (!root.has("skim_mix_version") || !root.has("tracks")) return null
            val title = root.optString("title", "Mixed Playlist")
            val tracksArray = root.getJSONArray("tracks")
            val queries = mutableListOf<Pair<String, String>>()
            for (i in 0 until tracksArray.length()) {
                val item = tracksArray.getJSONObject(i)
                val vid = item.optString("videoId", "")
                val tTitle = item.optString("title", "")
                val artist = item.optString("artist", "")
                queries.add(vid to "$tTitle $artist".trim())
            }
            val mixObj = root.optJSONObject("mix")
            val parsedSettings = if (mixObj != null) {
                deserializeSettings(newPlaylistId, mixObj.toString()).copy(mixEnabled = true)
            } else {
                PlaylistMixSettings(playlistId = newPlaylistId, mixEnabled = true)
            }
            ImportedMixPlaylist(title = title, trackQueries = queries, settings = parsedSettings)
        } catch (_: Exception) {
            null
        }
    }

    private fun loadAll(): Map<String, PlaylistMixSettings> {
        val result = mutableMapOf<String, PlaylistMixSettings>()
        prefs.all.forEach { (key, value) ->
            if (value is String) {
                runCatching {
                    result[key] = deserializeSettings(key, value)
                }
            }
        }
        return result
    }

    private fun serializeSettings(settings: PlaylistMixSettings): String {
        val obj = JSONObject()
        obj.put("mixEnabled", settings.mixEnabled)
        obj.put("preset", settings.preset.name)
        obj.put("bars", settings.bars)
        obj.put("djFilterEnabled", settings.djFilterEnabled)
        val overridesObj = JSONObject()
        settings.pairOverrides.forEach { (pairKey, t) ->
            val tObj = JSONObject()
            tObj.put("preset", t.preset.name)
            tObj.put("bars", t.bars)
            tObj.put("eqFilter", t.eqFilter.name)
            overridesObj.put(pairKey, tObj)
        }
        obj.put("pairOverrides", overridesObj)
        return obj.toString()
    }

    private fun deserializeSettings(playlistId: String, json: String): PlaylistMixSettings {
        val obj = JSONObject(json)
        val mixEnabled = obj.optBoolean("mixEnabled", false)
        val preset = runCatching { MixPreset.valueOf(obj.optString("preset", "AUTO")) }
            .getOrDefault(MixPreset.AUTO)
        val bars = obj.optInt("bars", 8)
        val djFilterEnabled = obj.optBoolean("djFilterEnabled", true)
        val overridesMap = mutableMapOf<String, PairTransition>()
        val overridesObj = obj.optJSONObject("pairOverrides")
        if (overridesObj != null) {
            val keys = overridesObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val tObj = overridesObj.optJSONObject(key) ?: continue
                val tPreset = runCatching { MixPreset.valueOf(tObj.optString("preset", "AUTO")) }
                    .getOrDefault(MixPreset.AUTO)
                val tBars = tObj.optInt("bars", 8)
                val tEq = runCatching { MixEqFilter.valueOf(tObj.optString("eqFilter", "AUTO")) }
                    .getOrDefault(MixEqFilter.AUTO)
                overridesMap[key] = PairTransition(tPreset, tBars, tEq)
            }
        }
        return PlaylistMixSettings(
            playlistId = playlistId,
            mixEnabled = mixEnabled,
            preset = preset,
            bars = bars,
            djFilterEnabled = djFilterEnabled,
            pairOverrides = overridesMap
        )
    }

    companion object {
        private val MAJOR_CAMELOT = mapOf(
            "C" to "8B", "G" to "9B", "D" to "10B", "A" to "11B", "E" to "12B", "B" to "1B",
            "F#" to "2B", "GB" to "2B", "C#" to "3B", "DB" to "3B", "G#" to "4B", "AB" to "4B",
            "D#" to "5B", "EB" to "5B", "A#" to "6B", "BB" to "6B", "F" to "7B"
        )
        private val MINOR_CAMELOT = mapOf(
            "A" to "8A", "E" to "9A", "B" to "10A", "F#" to "11A", "GB" to "11A",
            "C#" to "12A", "DB" to "12A", "G#" to "1A", "AB" to "1A", "D#" to "2A", "EB" to "2A",
            "A#" to "3A", "BB" to "3A", "F" to "4A", "C" to "5A", "G" to "6A", "D" to "7A"
        )

        fun toCamelotCode(musicKey: String?, keyScale: String?): String? {
            if (musicKey.isNullOrBlank()) return null
            val cleanKey = musicKey.trim().uppercase().removeSuffix("M")
            val isMinor = keyScale?.contains("minor", ignoreCase = true) == true ||
                musicKey.trim().endsWith("m", ignoreCase = false)
            return if (isMinor) MINOR_CAMELOT[cleanKey] else MAJOR_CAMELOT[cleanKey]
        }
    }
}
