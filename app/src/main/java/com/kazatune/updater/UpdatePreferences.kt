package com.kazatune.updater

import android.content.Context
import android.content.SharedPreferences

class UpdatePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isAutoUpdateEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_UPDATE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_UPDATE_ENABLED, value).apply()

    var lastCheckedTimestamp: Long
        get() = prefs.getLong(KEY_LAST_CHECKED_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_CHECKED_TIMESTAMP, value).apply()

    var dismissedVersion: String
        get() = prefs.getString(KEY_DISMISSED_VERSION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DISMISSED_VERSION, value).apply()

    var lastNotifiedVersion: String
        get() = prefs.getString(KEY_LAST_NOTIFIED_VERSION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_NOTIFIED_VERSION, value).apply()

    var includePrereleases: Boolean
        get() = prefs.getBoolean(KEY_INCLUDE_PRERELEASES, false)
        set(value) = prefs.edit().putBoolean(KEY_INCLUDE_PRERELEASES, value).apply()

    fun shouldCheckForUpdates(intervalMs: Long = 6 * 3600 * 1000L): Boolean {
        if (!isAutoUpdateEnabled) return false
        val now = System.currentTimeMillis()
        return (now - lastCheckedTimestamp) >= intervalMs
    }

    fun isVersionDismissed(version: String): Boolean {
        return dismissedVersion.isNotBlank() && dismissedVersion.equals(version, ignoreCase = true)
    }

    fun isVersionNotified(version: String): Boolean {
        return lastNotifiedVersion.isNotBlank() && lastNotifiedVersion.equals(version, ignoreCase = true)
    }

    fun recordCheck(timestamp: Long = System.currentTimeMillis()) {
        lastCheckedTimestamp = timestamp
    }

    fun dismissVersion(version: String) {
        dismissedVersion = version
    }

    fun recordVersionNotified(version: String) {
        lastNotifiedVersion = version
    }

    companion object {
        private const val PREFS_NAME = "kazatune_update_prefs"
        private const val KEY_AUTO_UPDATE_ENABLED = "auto_update_enabled"
        private const val KEY_LAST_CHECKED_TIMESTAMP = "last_checked_timestamp"
        private const val KEY_DISMISSED_VERSION = "dismissed_version"
        private const val KEY_LAST_NOTIFIED_VERSION = "last_notified_version"
        private const val KEY_INCLUDE_PRERELEASES = "include_prereleases"
    }
}
