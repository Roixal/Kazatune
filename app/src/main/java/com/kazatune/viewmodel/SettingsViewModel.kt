package com.kazatune.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.repository.CacheRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

val EQUALIZER_BANDS_HZ = listOf("31Hz", "62Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz")

val PRESET_FLAT = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
val PRESET_BASS_BOOST = listOf(6.0f, 5.5f, 4.0f, 2.5f, 1.0f, 0.0f, 0.0f, 0.0f, 0.5f, 1.0f)
val PRESET_HIP_HOP = listOf(5.0f, 4.5f, 3.0f, 1.0f, -0.5f, -1.0f, 1.0f, 2.0f, 3.0f, 3.5f)
val PRESET_ROCK = listOf(4.5f, 3.5f, 2.0f, 0.0f, -1.0f, 0.5f, 2.5f, 3.5f, 4.0f, 4.5f)
val PRESET_ACOUSTIC = listOf(3.0f, 2.5f, 1.5f, 1.0f, 1.5f, 2.0f, 3.0f, 3.5f, 3.0f, 2.5f)
val PRESET_VOCAL = listOf(-2.0f, -1.0f, 0.0f, 1.5f, 3.5f, 4.0f, 3.5f, 2.0f, 0.5f, -1.0f)
val PRESET_ELECTRONIC = listOf(5.5f, 5.0f, 3.5f, 0.5f, -1.5f, 0.5f, 1.5f, 3.0f, 4.5f, 5.0f)
val PRESET_POP = listOf(-1.0f, 1.0f, 2.5f, 3.5f, 3.0f, 1.0f, 0.0f, 1.5f, 2.5f, 3.0f)

val EQUALIZER_PRESETS = mapOf(
    "Flat" to PRESET_FLAT,
    "Bass Boost" to PRESET_BASS_BOOST,
    "Hip-Hop" to PRESET_HIP_HOP,
    "Rock" to PRESET_ROCK,
    "Acoustic" to PRESET_ACOUSTIC,
    "Vocal Booster" to PRESET_VOCAL,
    "Electronic" to PRESET_ELECTRONIC,
    "Pop" to PRESET_POP
)

class SettingsViewModel(
    private val dataStoreManager: DataStoreManager,
    private val cacheRepository: CacheRepository,
    private val mediaPlayerHandler: MediaPlayerHandler
) : ViewModel() {

    private val _quality = MutableStateFlow("High")
    val quality: StateFlow<String> = _quality.asStateFlow()

    private val _canvasEnabled = MutableStateFlow(true)
    val canvasEnabled: StateFlow<Boolean> = _canvasEnabled.asStateFlow()

    private val _normalizeVolume = MutableStateFlow(true)
    val normalizeVolume: StateFlow<Boolean> = _normalizeVolume.asStateFlow()

    private val _cacheSizeMb = MutableStateFlow("0 MB")
    val cacheSizeMb: StateFlow<String> = _cacheSizeMb.asStateFlow()

    private val _isYtMusicLoggedIn = MutableStateFlow(false)
    val isYtMusicLoggedIn: StateFlow<Boolean> = _isYtMusicLoggedIn.asStateFlow()

    private val _sponsorBlockEnabled = MutableStateFlow(true)
    val sponsorBlockEnabled: StateFlow<Boolean> = _sponsorBlockEnabled.asStateFlow()

    private val _lastfmUsername = MutableStateFlow("")
    val lastfmUsername: StateFlow<String> = _lastfmUsername.asStateFlow()

    private val _lastfmScrobbleEnabled = MutableStateFlow(true)
    val lastfmScrobbleEnabled: StateFlow<Boolean> = _lastfmScrobbleEnabled.asStateFlow()

    private val _crossfadeEnabled = MutableStateFlow(false)
    val crossfadeEnabled: StateFlow<Boolean> = _crossfadeEnabled.asStateFlow()

    private val _crossfadeDuration = MutableStateFlow(DataStoreManager.CROSSFADE_DURATION_AUTO)
    val crossfadeDuration: StateFlow<Int> = _crossfadeDuration.asStateFlow()

    private val _crossfadeDjMode = MutableStateFlow(true)
    val crossfadeDjMode: StateFlow<Boolean> = _crossfadeDjMode.asStateFlow()

    private val _equalizerEnabled = MutableStateFlow(false)
    val equalizerEnabled: StateFlow<Boolean> = _equalizerEnabled.asStateFlow()

    private val _equalizerPreset = MutableStateFlow("Flat")
    val equalizerPreset: StateFlow<String> = _equalizerPreset.asStateFlow()

    private val _equalizerBands = MutableStateFlow(PRESET_FLAT)
    val equalizerBands: StateFlow<List<Float>> = _equalizerBands.asStateFlow()

    private val _equalizerBassBoost = MutableStateFlow(0f)
    val equalizerBassBoost: StateFlow<Float> = _equalizerBassBoost.asStateFlow()

    val sleepTimerState = mediaPlayerHandler.sleepTimerState

    init {
        loadSettings()
        calculateCacheSize()
    }

    private fun loadSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.quality.collect { q ->
                _quality.value = if (q.isBlank()) "High (160 kbps)" else q
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.spotifyCanvas.collect { c ->
                _canvasEnabled.value = c == DataStoreManager.Values.TRUE
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.normalizeVolume.collect { n ->
                _normalizeVolume.value = n == DataStoreManager.Values.TRUE
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.loggedIn.collect { logged ->
                _isYtMusicLoggedIn.value = logged == DataStoreManager.Values.TRUE || logged.equals("true", ignoreCase = true)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.sponsorBlockEnabled.collect { s ->
                _sponsorBlockEnabled.value = s == DataStoreManager.Values.TRUE || s.equals("true", ignoreCase = true)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.lastfmUsername.collect { u ->
                _lastfmUsername.value = u
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.lastfmScrobbleEnabled.collect { sc ->
                _lastfmScrobbleEnabled.value = sc == DataStoreManager.Values.TRUE || sc.equals("true", ignoreCase = true)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.crossfadeEnabled.collect { enabled ->
                _crossfadeEnabled.value = enabled == DataStoreManager.Values.TRUE
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.crossfadeDuration.collect { dur ->
                _crossfadeDuration.value = dur
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.crossfadeDjMode.collect { dj ->
                _crossfadeDjMode.value = dj == DataStoreManager.Values.TRUE
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.equalizerEnabled.collect { eq ->
                _equalizerEnabled.value = eq == DataStoreManager.Values.TRUE
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.equalizerBands.collect { bandsStr ->
                val parsed = bandsStr.split(",").mapNotNull { it.trim().toFloatOrNull() }
                if (parsed.size == 10) {
                    _equalizerBands.value = parsed
                    val matched = EQUALIZER_PRESETS.entries.firstOrNull { it.value == parsed }?.key ?: "Custom"
                    _equalizerPreset.value = matched
                } else if (bandsStr.isBlank()) {
                    _equalizerBands.value = PRESET_FLAT
                    _equalizerPreset.value = "Flat"
                }
            }
        }
    }

    fun calculateCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val playerBytes = cacheRepository.getCacheSize(Config.PLAYER_CACHE)
                val downloadBytes = cacheRepository.getCacheSize(Config.DOWNLOAD_CACHE)
                val canvasBytes = cacheRepository.getCacheSize(Config.CANVAS_CACHE)
                val totalMb = (playerBytes + downloadBytes + canvasBytes) / (1024 * 1024)
                _cacheSizeMb.value = "$totalMb MB"
            } catch (e: Exception) {
                _cacheSizeMb.value = "0 MB"
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            cacheRepository.clearCache(Config.PLAYER_CACHE)
            cacheRepository.clearCache(Config.DOWNLOAD_CACHE)
            cacheRepository.clearCache(Config.CANVAS_CACHE)
            calculateCacheSize()
        }
    }

    fun setQuality(newQuality: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.setQuality(newQuality)
            _quality.value = newQuality
        }
    }

    fun toggleCanvas(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.setSpotifyCanvas(enabled)
            _canvasEnabled.value = enabled
        }
    }

    fun toggleNormalizeVolume(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.setNormalizeVolume(enabled)
            _normalizeVolume.value = enabled
        }
    }

    fun setSleepTimer(minutes: Int) {
        if (minutes > 0) {
            mediaPlayerHandler.sleepStart(minutes)
        } else {
            mediaPlayerHandler.sleepStop()
        }
    }

    fun toggleSponsorBlock(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.setSponsorBlockEnabled(enabled)
            _sponsorBlockEnabled.value = enabled
        }
    }

    fun toggleLastfmScrobble(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.setLastfmScrobbleEnabled(enabled)
            _lastfmScrobbleEnabled.value = enabled
        }
    }

    fun toggleCrossfade(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.setCrossfadeEnabled(enabled)
            _crossfadeEnabled.value = enabled
        }
    }

    fun setCrossfadeDuration(durationMs: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.setCrossfadeDuration(durationMs)
            _crossfadeDuration.value = durationMs
        }
    }

    fun toggleCrossfadeDjMode(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.setCrossfadeDjMode(enabled)
            _crossfadeDjMode.value = enabled
        }
    }

    fun saveLastfmSession(sessionKey: String, username: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.setLastfmSession(sessionKey.trim(), username.trim())
            dataStoreManager.setLastfmScrobbleEnabled(true)
            dataStoreManager.setLocalTrackingEnabled(true)
            _lastfmUsername.value = username.trim()
            _lastfmScrobbleEnabled.value = true
        }
    }

    fun logoutLastfm() {
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.setLastfmSession("", "")
            _lastfmUsername.value = ""
        }
    }

    fun loginWithCookie(cookie: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.setCookie(cookie.trim(), "")
            dataStoreManager.setLoggedIn(true)
            _isYtMusicLoggedIn.value = true
        }
    }

    fun logoutYtMusic() {
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.setCookie("", "")
            dataStoreManager.setLoggedIn(false)
            _isYtMusicLoggedIn.value = false
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.setEqualizerEnabled(enabled)
            _equalizerEnabled.value = enabled
        }
    }

    fun selectEqualizerPreset(presetName: String) {
        val bands = EQUALIZER_PRESETS[presetName] ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _equalizerPreset.value = presetName
            applyBandsAndBass(bands, _equalizerBassBoost.value)
        }
    }

    fun setEqualizerBand(index: Int, gainDb: Float) {
        val current = _equalizerBands.value.toMutableList()
        if (index in current.indices) {
            current[index] = gainDb.coerceIn(-12f, 12f)
            _equalizerPreset.value = "Custom"
            viewModelScope.launch(Dispatchers.IO) {
                applyBandsAndBass(current, _equalizerBassBoost.value)
            }
        }
    }

    fun setBassBoost(strength: Float) {
        _equalizerBassBoost.value = strength.coerceIn(0f, 1f)
        viewModelScope.launch(Dispatchers.IO) {
            applyBandsAndBass(_equalizerBands.value, _equalizerBassBoost.value)
        }
    }

    private suspend fun applyBandsAndBass(baseBands: List<Float>, bassBoost: Float) {
        val adjusted = baseBands.toMutableList()
        val extraBassDb = bassBoost * 6.0f
        if (adjusted.isNotEmpty()) adjusted[0] = (adjusted[0] + extraBassDb).coerceIn(-12f, 12f)
        if (adjusted.size > 1) adjusted[1] = (adjusted[1] + extraBassDb * 0.75f).coerceIn(-12f, 12f)

        _equalizerBands.value = adjusted
        dataStoreManager.setEqualizerBands(adjusted)
        val maxGain = maxOf(0f, adjusted.maxOrNull() ?: 0f)
        val preamp = if (maxGain > 0f) -maxGain else 0f
        dataStoreManager.setEqualizerPreamp(preamp)
    }
}
