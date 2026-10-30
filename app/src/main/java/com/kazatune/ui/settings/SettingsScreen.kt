package com.kazatune.ui.settings

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wallpaper
import com.kazatune.viewmodel.UpdateViewModel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kazatune.ui.theme.DinProFamily
import com.kazatune.ui.theme.RessoAccent
import com.kazatune.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = koinViewModel(),
    updateViewModel: UpdateViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val quality by settingsViewModel.quality.collectAsStateWithLifecycle()
    val canvasEnabled by settingsViewModel.canvasEnabled.collectAsStateWithLifecycle()
    val normalizeVolume by settingsViewModel.normalizeVolume.collectAsStateWithLifecycle()
    val cacheSizeMb by settingsViewModel.cacheSizeMb.collectAsStateWithLifecycle()
    val sleepTimerState by settingsViewModel.sleepTimerState.collectAsStateWithLifecycle()
    val isYtMusicLoggedIn by settingsViewModel.isYtMusicLoggedIn.collectAsStateWithLifecycle()
    val sponsorBlockEnabled by settingsViewModel.sponsorBlockEnabled.collectAsStateWithLifecycle()
    val lastfmUsername by settingsViewModel.lastfmUsername.collectAsStateWithLifecycle()
    val lastfmScrobbleEnabled by settingsViewModel.lastfmScrobbleEnabled.collectAsStateWithLifecycle()
    val isAutoUpdateEnabled by updateViewModel.isAutoUpdateEnabled.collectAsStateWithLifecycle()

    val context = LocalContext.current

    var showAboutScreen by remember { mutableStateOf(false) }

    if (showAboutScreen) {
        AboutScreen(
            onBack = { showAboutScreen = false },
            updateViewModel = updateViewModel
        )
        return
    }

    var showQualityDialog by remember { mutableStateOf(false) }
    var showWebLoginDialog by remember { mutableStateOf(false) }
    var showCookieImportDialog by remember { mutableStateOf(false) }
    var showLastfmDialog by remember { mutableStateOf(false) }
    var cookieInputText by remember { mutableStateOf("") }
    var lastfmUsernameInput by remember { mutableStateOf("") }
    var lastfmSessionKeyInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0E13))
            .statusBarsPadding()
            .padding(top = 12.dp)
    ) {
        // Screen Header
        Text(
            text = "Settings",
            fontFamily = DinProFamily,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section: YouTube Music Account
            item {
                SettingsSectionHeader("Account & Library Sync")
            }

            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF181720),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF252430)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isYtMusicLoggedIn) Icons.Default.CheckCircle else Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = if (isYtMusicLoggedIn) Color(0xFF4CAF50) else RessoAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "YouTube Music Account",
                                    fontFamily = DinProFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isYtMusicLoggedIn) "Connected • Playlists & Library Synced" else "Sign in to access your YouTube Music library",
                                    fontFamily = DinProFamily,
                                    fontSize = 12.sp,
                                    color = if (isYtMusicLoggedIn) Color(0xFF81C784) else Color(0x99FFFFFF)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (isYtMusicLoggedIn) {
                            Button(
                                onClick = { settingsViewModel.logoutYtMusic() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0x33FF0050),
                                    contentColor = RessoAccent
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = null,
                                    tint = RessoAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sign Out",
                                    fontFamily = DinProFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showWebLoginDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RessoAccent,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Login,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Web Sign In",
                                        fontFamily = DinProFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = { showCookieImportDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF282732),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Paste Cookie",
                                        fontFamily = DinProFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section: Audio & Playback
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SettingsSectionHeader("Audio & Playback")
            }

            item {
                SettingsItemCard(
                    icon = Icons.Default.HighQuality,
                    title = "Audio Quality",
                    subtitle = quality,
                    onClick = { showQualityDialog = true }
                )
            }

            item {
                SettingsSwitchCard(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = "Normalize Volume",
                    subtitle = "Maintains consistent volume across songs",
                    checked = normalizeVolume,
                    onCheckedChange = { settingsViewModel.toggleNormalizeVolume(it) }
                )
            }

            item {
                SettingsSwitchCard(
                    icon = Icons.Default.Wallpaper,
                    title = "Spotify Canvas Loops",
                    subtitle = "Stream short animated video loops when available",
                    checked = canvasEnabled,
                    onCheckedChange = { settingsViewModel.toggleCanvas(it) }
                )
            }

            item {
                SettingsSwitchCard(
                    icon = Icons.Default.Bolt,
                    title = "SponsorBlock & Segment Skipping",
                    subtitle = "Skip sponsor segments, intros, and non-music interludes",
                    checked = sponsorBlockEnabled,
                    onCheckedChange = { settingsViewModel.toggleSponsorBlock(it) }
                )
            }

            // Section: Storage & Cache
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SettingsSectionHeader("Storage & Cache")
            }

            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF181720),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF252430)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = RessoAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cached Audio Data",
                                fontFamily = DinProFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = cacheSizeMb,
                                fontFamily = DinProFamily,
                                fontSize = 13.sp,
                                color = Color(0x99FFFFFF)
                            )
                        }

                        Button(
                            onClick = { settingsViewModel.clearCache() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2C2B36),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Clear",
                                fontFamily = DinProFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Section: Sleep Timer
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SettingsSectionHeader("Sleep Timer")
            }

            item {
                val timerMinutes = if (sleepTimerState.timeRemaining > 0) sleepTimerState.timeRemaining else 0
                val timerText = if (timerMinutes > 0) "$timerMinutes min remaining" else "Timer Off"

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF181720),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF252430)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AvTimer,
                                    contentDescription = null,
                                    tint = RessoAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Auto Sleep Timer",
                                    fontFamily = DinProFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = timerText,
                                    fontFamily = DinProFamily,
                                    fontSize = 13.sp,
                                    color = if (timerMinutes > 0) RessoAccent else Color(0x99FFFFFF)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val presetMinutes = listOf(15, 30, 45, 60)
                            items(presetMinutes) { min ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF252430),
                                    modifier = Modifier.clickable { settingsViewModel.setSleepTimer(min) }
                                ) {
                                    Text(
                                        text = "${min}m",
                                        fontFamily = DinProFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            item {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0x33FF0050),
                                    modifier = Modifier.clickable { settingsViewModel.setSleepTimer(0) }
                                ) {
                                    Text(
                                        text = "Stop",
                                        fontFamily = DinProFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = RessoAccent,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section: Last.fm Scrobbling
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SettingsSectionHeader("Last.fm Scrobbling")
            }

            item {
                SettingsSwitchCard(
                    icon = Icons.Default.Radio,
                    title = "Background Scrobbling",
                    subtitle = if (lastfmUsername.isNotBlank()) "Scrobbling active for $lastfmUsername" else "Enable scrobbling to your Last.fm profile",
                    checked = lastfmScrobbleEnabled && lastfmUsername.isNotBlank(),
                    onCheckedChange = { settingsViewModel.toggleLastfmScrobble(it) }
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF181720),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLastfmDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF252430)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (lastfmUsername.isNotBlank()) Icons.Default.CheckCircle else Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = if (lastfmUsername.isNotBlank()) Color(0xFF4CAF50) else RessoAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (lastfmUsername.isNotBlank()) "Last.fm: $lastfmUsername" else "Connect Last.fm Account",
                                fontFamily = DinProFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (lastfmUsername.isNotBlank()) "Tap to manage account or disconnect" else "Tap to connect your Last.fm username",
                                fontFamily = DinProFamily,
                                fontSize = 13.sp,
                                color = Color(0x99FFFFFF)
                            )
                        }
                    }
                }
            }

            // Section: App Updates
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SettingsSectionHeader("App Updates")
            }

            item {
                AppUpdatesCard(
                    updateViewModel = updateViewModel,
                    onOpenDialog = { updateViewModel.openDialog() }
                )
            }

            item {
                SettingsSwitchCard(
                    icon = Icons.Default.Sync,
                    title = "Automatic Update Checking",
                    subtitle = "Periodically check for updates in the background",
                    checked = isAutoUpdateEnabled,
                    onCheckedChange = { updateViewModel.toggleAutoUpdate(it) }
                )
            }

            // Section: About Skim Music
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SettingsSectionHeader("About")
            }

            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF181720),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAboutScreen = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF252430)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = RessoAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "About Skim Music",
                                    fontFamily = DinProFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Version ${updateViewModel.currentVersionName} • Features & Developers",
                                    fontFamily = DinProFamily,
                                    fontSize = 12.sp,
                                    color = Color(0x88FFFFFF)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                            contentDescription = "Open About",
                            tint = Color(0x66FFFFFF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Interactive In-App WebView Login Dialog
    if (showWebLoginDialog) {
        Dialog(
            onDismissRequest = { showWebLoginDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sign in to YouTube Music",
                            fontFamily = DinProFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = { showWebLoginDialog = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }

                    // Native WebView
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        url?.let { currentUrl ->
                                            val cookies = CookieManager.getInstance().getCookie("https://music.youtube.com") ?: ""
                                            if (currentUrl.contains("music.youtube.com") && cookies.contains("SAPISID")) {
                                                settingsViewModel.loginWithCookie(cookies)
                                                showWebLoginDialog = false
                                            }
                                        }
                                    }
                                }
                                loadUrl("https://accounts.google.com/ServiceLogin?continue=https://music.youtube.com/")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }

    // Paste Cookie Import Dialog
    if (showCookieImportDialog) {
        AlertDialog(
            onDismissRequest = { showCookieImportDialog = false },
            containerColor = Color(0xFF1C1B24),
            title = {
                Text(
                    text = "Import YouTube Cookie",
                    fontFamily = DinProFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text(
                        text = "Paste your full cookie header from music.youtube.com (containing SAPISID, SSID, etc.):",
                        fontFamily = DinProFamily,
                        fontSize = 13.sp,
                        color = Color(0x99FFFFFF)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = cookieInputText,
                        onValueChange = { cookieInputText = it },
                        placeholder = { Text("SAPISID=...; SSID=...; ...", color = Color(0x44FFFFFF)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RessoAccent,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (cookieInputText.isNotBlank()) {
                            settingsViewModel.loginWithCookie(cookieInputText)
                            showCookieImportDialog = false
                        }
                    }
                ) {
                    Text(
                        text = "Save",
                        fontFamily = DinProFamily,
                        color = RessoAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCookieImportDialog = false }) {
                    Text(
                        text = "Cancel",
                        fontFamily = DinProFamily,
                        color = Color(0x88FFFFFF)
                    )
                }
            }
        )
    }

    // Audio Quality Dialog
    if (showQualityDialog) {
        val qualities = listOf(
            "High (160 kbps Opus)" to "High",
            "Medium (128 kbps)" to "Medium",
            "Low (64 kbps)" to "Low"
        )

        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            containerColor = Color(0xFF1C1B24),
            title = {
                Text(
                    text = "Streaming Audio Quality",
                    fontFamily = DinProFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column {
                    qualities.forEach { (label, key) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsViewModel.setQuality(key)
                                    showQualityDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = quality.startsWith(key, ignoreCase = true),
                                onClick = {
                                    settingsViewModel.setQuality(key)
                                    showQualityDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = RessoAccent,
                                    unselectedColor = Color(0x66FFFFFF)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                fontFamily = DinProFamily,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text(
                        text = "Done",
                        fontFamily = DinProFamily,
                        color = RessoAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    // Last.fm Connection Dialog
    if (showLastfmDialog) {
        AlertDialog(
            onDismissRequest = { showLastfmDialog = false },
            containerColor = Color(0xFF1E1D26),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = if (lastfmUsername.isNotBlank()) "Last.fm Account" else "Connect Last.fm",
                    fontFamily = DinProFamily,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    if (lastfmUsername.isNotBlank()) {
                        Text(
                            text = "Currently connected as: $lastfmUsername",
                            fontFamily = DinProFamily,
                            color = Color(0xCCFFFFFF),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                settingsViewModel.logoutLastfm()
                                showLastfmDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3366)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Disconnect Account", fontFamily = DinProFamily, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = "Enter your Last.fm username and optional session key to enable scrobbling.",
                            fontFamily = DinProFamily,
                            color = Color(0xAAFFFFFF),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = lastfmUsernameInput,
                            onValueChange = { lastfmUsernameInput = it },
                            label = { Text("Last.fm Username", color = Color(0x88FFFFFF)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = lastfmSessionKeyInput,
                            onValueChange = { lastfmSessionKeyInput = it },
                            label = { Text("Session Key (Optional)", color = Color(0x88FFFFFF)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                if (lastfmUsername.isBlank()) {
                    TextButton(
                        onClick = {
                            if (lastfmUsernameInput.isNotBlank()) {
                                val sk = lastfmSessionKeyInput.ifBlank { "skim_session_${lastfmUsernameInput.trim()}" }
                                settingsViewModel.saveLastfmSession(sk, lastfmUsernameInput.trim())
                                settingsViewModel.toggleLastfmScrobble(true)
                            }
                            showLastfmDialog = false
                        }
                    ) {
                        Text("Connect", fontFamily = DinProFamily, color = RessoAccent, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showLastfmDialog = false }) {
                    Text("Close", fontFamily = DinProFamily, color = Color(0x88FFFFFF))
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontFamily = DinProFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = RessoAccent,
        modifier = Modifier.padding(bottom = 2.dp)
    )
}

@Composable
fun SettingsItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF181720),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF252430)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = RessoAccent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = DinProFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontFamily = DinProFamily,
                    fontSize = 13.sp,
                    color = Color(0x99FFFFFF)
                )
            }
        }
    }
}

@Composable
fun SettingsSwitchCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF181720),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF252430)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = RessoAccent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = DinProFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontFamily = DinProFamily,
                    fontSize = 12.sp,
                    color = Color(0x99FFFFFF)
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = RessoAccent,
                    uncheckedThumbColor = Color(0x88FFFFFF),
                    uncheckedTrackColor = Color(0xFF2C2B36)
                )
            )
        }
    }
}

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    updateViewModel: UpdateViewModel = koinViewModel()
) {
    BackHandler(enabled = true) { onBack() }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0E13))
            .statusBarsPadding()
            .padding(top = 12.dp)
    ) {
        // Top Navigation Bar with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "About Skim Music",
                fontFamily = DinProFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF181720),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF252430)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = RessoAccent,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Skim Music",
                                    fontFamily = DinProFamily,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Version ${updateViewModel.currentVersionName} (Stable Release)",
                                    fontFamily = DinProFamily,
                                    fontSize = 12.sp,
                                    color = RessoAccent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "A next-generation music streaming experience blending TikTok and Resso's fluid vertical discovery feed with Apple's Liquid Glass aesthetics and the entire YouTube Music catalog.",
                            fontFamily = DinProFamily,
                            fontSize = 13.sp,
                            color = Color(0xCCFFFFFF),
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            // Updates Card inside About Screen
            item {
                AppUpdatesCard(
                    updateViewModel = updateViewModel,
                    onOpenDialog = { updateViewModel.openDialog() }
                )
            }

            // Core Features
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF181720),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Core Features",
                            fontFamily = DinProFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val features = listOf(
                            "📱 Vertical Discovery Feed" to "Continuous swipe through endless songs with instant previews",
                            "🎯 Hook Jump" to "One-tap jump straight to the song's chorus or peak moment",
                            "🎤 Word-by-Word Synced Lyrics" to "Real-time scrolling karaoke lyrics with timing offset calibration",
                            "💿 Self-Healing Playback" to "Tap the spinning vinyl disc to instantly purge stalls and reload stream tokens",
                            "🎛️ Dual-Pane Landscape Player" to "Dedicated tablet and landscape player with vinyl turntable and synced lyrics",
                            "📲 Home Screen Glance Widgets" to "Interactive Turntable widget, Recent Playlists, and 30-Day Insights",
                            "🧠 On-Device AI Music Taste Engine" to "Private listening analysis and dynamic personas without paid API keys",
                            "🔄 Universal Playlist Migration" to "One-tap conversion from Spotify, YouTube, or raw tracklist text",
                            "🛡️ SponsorBlock & Last.fm" to "Automatic non-music segment skipping and background scrobbling"
                        )

                        features.forEach { (title, desc) ->
                            Column(modifier = Modifier.padding(vertical = 5.dp)) {
                                Text(
                                    text = title,
                                    fontFamily = DinProFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE2E2E8)
                                )
                                Text(
                                    text = desc,
                                    fontFamily = DinProFamily,
                                    fontSize = 11.sp,
                                    color = Color(0x88FFFFFF),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            // Official Stable Release Page Button
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x33FF0050),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/Roixal/Kazatune/releases/tag/Stable")
                            )
                            context.startActivity(intent)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Official Stable Release",
                                fontFamily = DinProFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "github.com/Roixal/Kazatune/releases/tag/Stable",
                                fontFamily = DinProFamily,
                                fontSize = 11.sp,
                                color = Color(0xAAFFFFFF)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Release",
                            tint = RessoAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Developers Section
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF181720),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Developers",
                            fontFamily = DinProFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Developer 1: KRISH KUMAR
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF201F2B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/kkrriish10")
                                    )
                                    context.startActivity(intent)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2D2C3D)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "KK",
                                            fontFamily = DinProFamily,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        AsyncImage(
                                            model = "https://github.com/kkrriish10.png",
                                            contentDescription = "KRISH KUMAR",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = "KRISH KUMAR",
                                            fontFamily = DinProFamily,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "@kkrriish10",
                                            fontFamily = DinProFamily,
                                            fontSize = 12.sp,
                                            color = Color(0x88FFFFFF)
                                        )
                                    }
                                }
                                Text(
                                    text = "Developer",
                                    fontFamily = DinProFamily,
                                    fontSize = 12.sp,
                                    color = RessoAccent,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Developer 2: Roixal
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF201F2B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/Roixal")
                                    )
                                    context.startActivity(intent)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2D2C3D)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "RX",
                                            fontFamily = DinProFamily,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        AsyncImage(
                                            model = "https://github.com/Roixal.png",
                                            contentDescription = "Roixal",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = "Roixal",
                                            fontFamily = DinProFamily,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "@Roixal",
                                            fontFamily = DinProFamily,
                                            fontSize = 12.sp,
                                            color = Color(0x88FFFFFF)
                                        )
                                    }
                                }
                                Text(
                                    text = "Developer",
                                    fontFamily = DinProFamily,
                                    fontSize = 12.sp,
                                    color = RessoAccent,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
