package com.kazatune

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kazatune.ui.component.KazatuneBottomBar
import com.kazatune.ui.component.MainTab
import com.kazatune.ui.component.MiniPlayerBar
import com.kazatune.ui.library.LibraryScreen
import com.kazatune.ui.search.SearchScreen
import com.kazatune.ui.settings.SettingsScreen
import com.kazatune.ui.skim.SkimScreen
import com.kazatune.ui.theme.KazatuneTheme
import com.kazatune.viewmodel.SkimViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KazatuneTheme {
                MainRootScreen()
            }
        }
    }
}

@Composable
fun MainRootScreen(
    skimViewModel: SkimViewModel = koinViewModel()
) {
    var currentTab by rememberSaveable { mutableStateOf(MainTab.SKIM) }
    val nowPlaying by skimViewModel.nowPlaying.collectAsStateWithLifecycle()
    val controlState by skimViewModel.controlState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Tab Content
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentTab) {
                MainTab.SKIM -> SkimScreen(skimViewModel = skimViewModel)
                MainTab.SEARCH -> SearchScreen(
                    onPlayTrack = { track ->
                        skimViewModel.playTrackAndStartRadio(track)
                        currentTab = MainTab.SKIM
                    }
                )
                MainTab.LIBRARY -> LibraryScreen(
                    onPlayTrack = { track ->
                        skimViewModel.playTrackAndStartRadio(track)
                        currentTab = MainTab.SKIM
                    }
                )
                MainTab.SETTINGS -> SettingsScreen()
            }
        }

        // Bottom Bar & Mini Player Bar
        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            // Show Mini Player when NOT on Skim tab
            if (currentTab != MainTab.SKIM) {
                MiniPlayerBar(
                    nowPlaying = nowPlaying,
                    isPlaying = controlState.isPlaying,
                    onTogglePlayPause = { skimViewModel.togglePlayPause() },
                    onClick = { currentTab = MainTab.SKIM }
                )
            }

            KazatuneBottomBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    }
}
