package com.kazatune.updater.model

import java.io.File

data class ReleaseInfo(
    val tagName: String,
    val name: String,
    val versionName: String,
    val body: String,
    val htmlUrl: String,
    val publishedAt: String,
    val apkDownloadUrl: String?,
    val apkFileName: String?,
    val apkSize: Long,
    val sha256Digest: String?,
    val isPrerelease: Boolean,
    val isMandatory: Boolean
)

sealed class UpdateStatus {
    data object Idle : UpdateStatus()
    data object Checking : UpdateStatus()
    data class NoUpdateAvailable(val currentVersion: String, val lastChecked: Long) : UpdateStatus()
    data class UpdateAvailable(val release: ReleaseInfo, val currentVersion: String, val isMandatory: Boolean) : UpdateStatus()
    data class Downloading(
        val release: ReleaseInfo,
        val progress: Float,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val speedFormatted: String
    ) : UpdateStatus()
    data class ReadyToInstall(val release: ReleaseInfo, val apkFile: File) : UpdateStatus()
    data class Error(val message: String, val canRetry: Boolean, val lastChecked: Long = System.currentTimeMillis()) : UpdateStatus()
}
