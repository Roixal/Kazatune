package com.kazatune.di

import com.kazatune.viewmodel.CommentViewModel
import com.kazatune.viewmodel.LibraryViewModel
import com.kazatune.viewmodel.SearchViewModel
import com.kazatune.viewmodel.SettingsViewModel
import com.kazatune.viewmodel.SkimViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel {
        SkimViewModel(
            mediaPlayerHandler = get(),
            homeRepository = get(),
            songRepository = get(),
            lyricsCanvasRepository = get(),
            streamRepository = get()
        )
    }
    viewModel {
        SearchViewModel(
            searchRepository = get(),
            homeRepository = get(),
            mediaPlayerHandler = get()
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
            mediaPlayerHandler = get()
        )
    }
    viewModel {
        SettingsViewModel(
            dataStoreManager = get(),
            cacheRepository = get(),
            mediaPlayerHandler = get()
        )
    }
}
