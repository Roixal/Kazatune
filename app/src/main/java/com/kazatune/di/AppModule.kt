package com.kazatune.di

import com.kazatune.updater.UpdateNotificationHelper
import com.kazatune.updater.UpdatePreferences
import com.kazatune.updater.UpdateRepository
import com.kazatune.viewmodel.CommentViewModel
import com.kazatune.viewmodel.LibraryViewModel
import com.kazatune.viewmodel.PlaylistMigrationViewModel
import com.kazatune.viewmodel.SearchViewModel
import com.kazatune.viewmodel.SettingsViewModel
import com.kazatune.viewmodel.SkimViewModel
import com.kazatune.viewmodel.TasteViewModel
import com.kazatune.viewmodel.UpdateViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { UpdatePreferences(get()) }
    single { UpdateNotificationHelper(get()) }
    single { UpdateRepository(get(), get()) }

    viewModel {
        UpdateViewModel(
            updateRepository = get(),
            preferences = get(),
            notificationHelper = get()
        )
    }
    viewModel {
        SkimViewModel(
            mediaPlayerHandler = get(),
            homeRepository = get(),
            songRepository = get(),
            lyricsCanvasRepository = get(),
            streamRepository = get(),
            downloadHandler = get(),
            dataStoreManager = get(),
            searchRepository = get()
        )
    }
    viewModel {
        SearchViewModel(
            searchRepository = get(),
            homeRepository = get(),
            mediaPlayerHandler = get(),
            albumRepository = get(),
            playlistRepository = get(),
            artistRepository = get(),
            songRepository = get()
        )
    }
    viewModel {
        CommentViewModel(
            songRepository = get()
        )
    }
    viewModel {
        LibraryViewModel(
            songRepository = get(),
            localPlaylistRepository = get(),
            playlistRepository = get(),
            mediaPlayerHandler = get(),
            downloadHandler = get()
        )
    }
    viewModel {
        SettingsViewModel(
            dataStoreManager = get(),
            cacheRepository = get(),
            mediaPlayerHandler = get()
        )
    }
    viewModel {
        TasteViewModel(
            songRepository = get(),
            homeRepository = get()
        )
    }
    viewModel {
        PlaylistMigrationViewModel(
            localPlaylistRepository = get(),
            playlistRepository = get(),
            searchRepository = get(),
            songRepository = get(),
            importRepository = get()
        )
    }
}
