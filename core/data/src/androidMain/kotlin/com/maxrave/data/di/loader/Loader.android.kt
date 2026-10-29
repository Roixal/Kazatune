package com.maxrave.data.di.loader

import com.maxrave.media3.di.loadMediaService

actual fun loadMediaService() {
    loadMediaService()
}

actual fun loadLoginSyncModule() {
    // No-op for standalone Kazatune Android app
}
