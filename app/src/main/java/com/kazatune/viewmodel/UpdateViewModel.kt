package com.kazatune.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kazatune.updater.ApkInstaller
import com.kazatune.updater.UpdateNotificationHelper
import com.kazatune.updater.UpdatePreferences
import com.kazatune.updater.UpdateRepository
import com.kazatune.updater.model.ReleaseInfo
import com.kazatune.updater.model.UpdateStatus
import com.kazatune.updater.util.SemanticVersion
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class UpdateViewModel(
    private val updateRepository: UpdateRepository,
    private val preferences: UpdatePreferences,
    private val notificationHelper: UpdateNotificationHelper
) : ViewModel() {

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _isAutoUpdateEnabled = MutableStateFlow(preferences.isAutoUpdateEnabled)
    val isAutoUpdateEnabled: StateFlow<Boolean> = _isAutoUpdateEnabled.asStateFlow()

    private val _lastCheckedTimestamp = MutableStateFlow(preferences.lastCheckedTimestamp)
    val lastCheckedTimestamp: StateFlow<Long> = _lastCheckedTimestamp.asStateFlow()

    val currentVersionName: String = updateRepository.getCurrentVersionName()

    private var downloadJob: Job? = null

    fun checkOnAppLaunch() {
        if (!preferences.shouldCheckForUpdates()) return

        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.Checking
            val result = updateRepository.fetchLatestRelease()
            _lastCheckedTimestamp.value = preferences.lastCheckedTimestamp

            result.onSuccess { release ->
                val latestVer = SemanticVersion.parse(release.versionName)
                val currentVer = SemanticVersion.parse(currentVersionName)

                if (latestVer != null && currentVer != null && latestVer.isNewerThan(currentVer)) {
                    _updateStatus.value = UpdateStatus.UpdateAvailable(
                        release = release,
                        currentVersion = currentVersionName,
                        isMandatory = release.isMandatory
                    )

                    // Show notification if not notified yet
                    if (!preferences.isVersionNotified(release.versionName)) {
                        notificationHelper.showUpdateNotification(release)
                        preferences.recordVersionNotified(release.versionName)
                    }

                    // Only show popup automatically if not dismissed or if mandatory
                    if (!preferences.isVersionDismissed(release.versionName) || release.isMandatory) {
                        _showUpdateDialog.value = true
                    }
                } else {
                    _updateStatus.value = UpdateStatus.NoUpdateAvailable(
                        currentVersion = currentVersionName,
                        lastChecked = System.currentTimeMillis()
                    )
                }
            }.onFailure { error ->
                _updateStatus.value = UpdateStatus.Error(
                    message = error.localizedMessage ?: "Failed to check for updates",
                    canRetry = true
                )
            }
        }
    }

    fun checkForUpdates(manual: Boolean = true) {
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.Checking
            val result = updateRepository.fetchLatestRelease()
            _lastCheckedTimestamp.value = preferences.lastCheckedTimestamp

            result.onSuccess { release ->
                val latestVer = SemanticVersion.parse(release.versionName)
                val currentVer = SemanticVersion.parse(currentVersionName)

                if (latestVer != null && currentVer != null && latestVer.isNewerThan(currentVer)) {
                    _updateStatus.value = UpdateStatus.UpdateAvailable(
                        release = release,
                        currentVersion = currentVersionName,
                        isMandatory = release.isMandatory
                    )
                    _showUpdateDialog.value = true
                } else {
                    _updateStatus.value = UpdateStatus.NoUpdateAvailable(
                        currentVersion = currentVersionName,
                        lastChecked = System.currentTimeMillis()
                    )
                }
            }.onFailure { error ->
                _updateStatus.value = UpdateStatus.Error(
                    message = error.localizedMessage ?: "Failed to check for updates",
                    canRetry = true
                )
            }
        }
    }

    fun downloadAndInstall(context: Context, release: ReleaseInfo) {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _updateStatus.value = UpdateStatus.Downloading(
                release = release,
                progress = 0f,
                bytesDownloaded = 0L,
                totalBytes = release.apkSize,
                speedFormatted = "0 KB/s"
            )

            val result = updateRepository.downloadApk(release) { progress, downloaded, total, speed ->
                _updateStatus.value = UpdateStatus.Downloading(
                    release = release,
                    progress = progress,
                    bytesDownloaded = downloaded,
                    totalBytes = total,
                    speedFormatted = speed
                )
            }

            result.onSuccess { file ->
                _updateStatus.value = UpdateStatus.ReadyToInstall(release, file)
                triggerInstall(context, file)
            }.onFailure { error ->
                _updateStatus.value = UpdateStatus.Error(
                    message = error.localizedMessage ?: "Download failed",
                    canRetry = true
                )
            }
        }
    }

    fun triggerInstall(context: Context, file: File) {
        if (!ApkInstaller.canInstallPackages(context)) {
            ApkInstaller.openInstallPermissionSettings(context)
        } else {
            ApkInstaller.installApk(context, file)
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        val current = _updateStatus.value
        if (current is UpdateStatus.Downloading) {
            _updateStatus.value = UpdateStatus.UpdateAvailable(
                release = current.release,
                currentVersion = currentVersionName,
                isMandatory = current.release.isMandatory
            )
        }
    }

    fun dismissDialog(release: ReleaseInfo? = null) {
        if (release != null && !release.isMandatory) {
            preferences.dismissVersion(release.versionName)
        }
        _showUpdateDialog.value = false
    }

    fun openDialog() {
        _showUpdateDialog.value = true
    }

    fun toggleAutoUpdate(enabled: Boolean) {
        preferences.isAutoUpdateEnabled = enabled
        _isAutoUpdateEnabled.value = enabled
    }
}
