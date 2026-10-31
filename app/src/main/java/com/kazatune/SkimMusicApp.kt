package com.kazatune

import android.app.Application
import com.aptabase.Aptabase
import com.kazatune.di.appModule
import com.maxrave.common.AppIdentity
import com.maxrave.data.di.loader.loadAllModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class SkimMusicApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Privacy-friendly telemetry via Aptabase
        try {
            Aptabase.instance.initialize(this, "A-EU-6636938585")
            Aptabase.instance.trackEvent("app_started")
        } catch (_: Exception) {
            // Analytics should never crash the app
        }

        val identity = AppIdentity(
            applicationId = packageName,
            versionName = "1.0.2",
            platform = "Android",
            isDevBuild = false
        )

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@SkimMusicApp)
            loadAllModules(identity)
            modules(appModule)
        }
    }
}

typealias KazatuneApp = SkimMusicApp
