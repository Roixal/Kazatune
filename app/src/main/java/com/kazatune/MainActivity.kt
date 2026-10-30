package com.kazatune

import android.Manifest
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.kazatune.updater.UpdateNotificationHelper
import com.kazatune.updater.model.UpdateStatus
import com.kazatune.updater.ui.UpdateDialog
import com.kazatune.viewmodel.UpdateViewModel
import com.maxrave.media3.di.setServiceActivitySession
import com.maxrave.media3.di.startService
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
import com.kazatune.ui.component.MainTab
import com.kazatune.ui.component.MiniPlayerBar
import com.kazatune.ui.component.SkimMusicBottomBar
import com.kazatune.ui.library.LibraryScreen
import com.kazatune.ui.search.SearchScreen
import com.kazatune.ui.settings.SettingsScreen
import com.kazatune.ui.skim.SkimScreen
import com.kazatune.ui.theme.SkimMusicTheme
import com.kazatune.viewmodel.SkimViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    private var mBound = false
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            setServiceActivitySession(this@MainActivity, MainActivity::class.java, service)
            mBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mBound = false
        }
    }

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Notification permission handled
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            SkimMusicTheme {
                MainRootScreen()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            startService(this, serviceConnection)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        if (mBound) {
            try {
                unbindService(serviceConnection)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mBound = false
        }
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun MainRootScreen(
    skimViewModel: SkimViewModel = koinViewModel(),
    updateViewModel: UpdateViewModel = koinViewModel()
) {
    val context = LocalContext.current
    var currentTab by rememberSaveable { mutableStateOf(MainTab.SKIM) }
    val nowPlaying by skimViewModel.nowPlaying.collectAsStateWithLifecycle()
    val controlState by skimViewModel.controlState.collectAsStateWithLifecycle()

    val updateStatus by updateViewModel.updateStatus.collectAsStateWithLifecycle()
    val showUpdateDialog by updateViewModel.showUpdateDialog.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        val activity = context as? ComponentActivity
        val showUpdateFromIntent = activity?.intent?.getBooleanExtra(UpdateNotificationHelper.EXTRA_SHOW_UPDATE, false) == true
        if (showUpdateFromIntent) {
            updateViewModel.openDialog()
        }
        updateViewModel.checkOnAppLaunch()
    }

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
                    },
                    onPlayTracks = { tracks, initialIndex ->
                        skimViewModel.setTracksAndPlay(tracks, initialIndex)
                        currentTab = MainTab.SKIM
                    }
                )
                MainTab.LIBRARY -> LibraryScreen(
                    onPlayTrack = { track ->
                        skimViewModel.playTrackAndStartRadio(track)
                        currentTab = MainTab.SKIM
                    },
                    onPlayTracks = { tracks, initialIndex ->
                        skimViewModel.setTracksAndPlay(tracks, initialIndex)
                        currentTab = MainTab.SKIM
                    }
                )
                MainTab.SETTINGS -> SettingsScreen(updateViewModel = updateViewModel)
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
                    onClick = {
                        skimViewModel.selectSkimTab(com.kazatune.viewmodel.SkimTab.FOR_YOU)
                        currentTab = MainTab.SKIM
                    }
                )
            }

            SkimMusicBottomBar(
                currentTab = currentTab,
                onTabSelected = { tab ->
                    if (tab == MainTab.SKIM && currentTab != MainTab.SKIM) {
                        skimViewModel.selectSkimTab(com.kazatune.viewmodel.SkimTab.FOR_YOU)
                    }
                    currentTab = tab
                }
            )
        }

        // In-App Update Dialog (Overlay)
        if (showUpdateDialog) {
            val release = when (val s = updateStatus) {
                is UpdateStatus.UpdateAvailable -> s.release
                is UpdateStatus.Downloading -> s.release
                is UpdateStatus.ReadyToInstall -> s.release
                else -> null
            }

            UpdateDialog(
                status = updateStatus,
                currentVersion = updateViewModel.currentVersionName,
                onDismiss = {
                    updateViewModel.dismissDialog(release)
                },
                onDownload = { rel ->
                    updateViewModel.downloadAndInstall(context, rel)
                },
                onInstall = { file ->
                    updateViewModel.triggerInstall(context, file)
                },
                onCancelDownload = {
                    updateViewModel.cancelDownload()
                },
                onRetry = {
                    updateViewModel.checkForUpdates(manual = true)
                }
            )
        }
    }
}
