package com.kazatune.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.kazatune.MainActivity
import com.kazatune.R
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.PlayerEvent
import com.maxrave.domain.repository.LocalPlaylistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PlaylistsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PlaylistsWidget()
}

/**
 * Playlists Widget (4x2): Displays currently playing track with quick player controls
 * and a shelf of recent playlist shortcuts.
 */
class PlaylistsWidget : GlanceAppWidget(), KoinComponent {
    override val sizeMode: SizeMode = SizeMode.Exact

    private val mediaPlayerHandler by inject<MediaPlayerHandler>()
    private val localPlaylistRepository by inject<LocalPlaylistRepository>()
    private val scope = CoroutineScope(Dispatchers.Main)

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        scope.launch {
            launch { mediaPlayerHandler.controlState.collectLatest { updateWidget(context) } }
            launch { mediaPlayerHandler.nowPlayingState.collectLatest { updateWidget(context) } }
        }

        provideContent {
            GlanceTheme {
                val controlState by mediaPlayerHandler.controlState.collectAsState()
                val nowPlaying by mediaPlayerHandler.nowPlayingState.collectAsState()

                var playlists by remember { mutableStateOf<List<LocalPlaylistEntity>>(emptyList()) }
                var artwork by remember { mutableStateOf<Bitmap?>(null) }

                val title = nowPlaying.mediaItem.metadata.title ?: nowPlaying.track?.title ?: "Skim Music"
                val artist = nowPlaying.mediaItem.metadata.artist ?: nowPlaying.track?.artists?.firstOrNull()?.name ?: "Ready to Play"
                val thumbUrl = nowPlaying.mediaItem.metadata.artworkUri
                    ?: nowPlaying.track?.thumbnails?.lastOrNull()?.url

                LaunchedEffect(Unit) {
                    localPlaylistRepository.getAllLocalPlaylists().collectLatest { list ->
                        playlists = list.take(3)
                    }
                }

                LaunchedEffect(thumbUrl) {
                    if (!thumbUrl.isNullOrBlank()) {
                        val request = ImageRequest.Builder(context)
                            .data(thumbUrl)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .allowHardware(false)
                            .build()
                        val loaded = (ImageLoader(context).execute(request) as? SuccessResult)?.image?.toBitmap()
                        artwork = loaded
                    }
                }

                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color(0xFF14131B)))
                        .cornerRadius(20.dp)
                        .padding(10.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Artwork + Playback Info
                    Box(
                        modifier = GlanceModifier
                            .size(96.dp)
                            .cornerRadius(14.dp)
                            .background(ColorProvider(Color(0xFF232230))),
                        contentAlignment = Alignment.Center
                    ) {
                        if (artwork != null) {
                            Image(
                                provider = ImageProvider(artwork!!),
                                contentDescription = "Cover",
                                contentScale = ContentScale.Crop,
                                modifier = GlanceModifier.fillMaxSize().cornerRadius(14.dp)
                            )
                        }
                    }

                    Spacer(GlanceModifier.width(12.dp))

                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        Spacer(GlanceModifier.height(2.dp))
                        Text(
                            text = artist,
                            style = TextStyle(
                                color = ColorProvider(Color(0x99FFFFFF)),
                                fontSize = 12.sp
                            ),
                            maxLines = 1
                        )

                        Spacer(GlanceModifier.height(10.dp))

                        // Player controls
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                provider = ImageProvider(R.drawable.rounded_fast_rewind_24),
                                contentDescription = "Previous",
                                colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
                                modifier = GlanceModifier
                                    .size(28.dp)
                                    .padding(2.dp)
                                    .clickable {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            mediaPlayerHandler.onPlayerEvent(PlayerEvent.Previous)
                                        }
                                    }
                            )
                            Spacer(GlanceModifier.width(10.dp))
                            Image(
                                provider = if (controlState.isPlaying) {
                                    ImageProvider(R.drawable.rounded_pause_24)
                                } else {
                                    ImageProvider(R.drawable.rounded_play_arrow_24)
                                },
                                contentDescription = if (controlState.isPlaying) "Pause" else "Play",
                                colorFilter = ColorFilter.tint(ColorProvider(Color(0xFFFF0050))),
                                modifier = GlanceModifier
                                    .size(32.dp)
                                    .clickable {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            mediaPlayerHandler.onPlayerEvent(PlayerEvent.PlayPause)
                                        }
                                    }
                            )
                            Spacer(GlanceModifier.width(10.dp))
                            Image(
                                provider = ImageProvider(R.drawable.rounded_fast_forward_24),
                                contentDescription = "Next",
                                colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
                                modifier = GlanceModifier
                                    .size(28.dp)
                                    .padding(2.dp)
                                    .clickable {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            mediaPlayerHandler.onPlayerEvent(PlayerEvent.Next)
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun updateWidget(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(this@PlaylistsWidget.javaClass).forEach { glanceId ->
            this@PlaylistsWidget.update(context, glanceId)
        }
    }
}
