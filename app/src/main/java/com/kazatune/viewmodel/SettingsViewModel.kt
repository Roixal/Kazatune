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
}
