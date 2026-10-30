package com.kazatune.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.unit.ColorProvider
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.kazatune.MainActivity
import com.kazatune.R
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.PlayerEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TurntableWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TurntableWidget()
}

/**
 * Turntable Widget: Compact 2x2 home screen widget showing album artwork
 * with play/pause and track skipping controls over an Apple Liquid Glass control pill.
 */
class TurntableWidget : GlanceAppWidget(), KoinComponent {
    override val sizeMode: SizeMode = SizeMode.Exact

    private val mediaPlayerHandler by inject<MediaPlayerHandler>()
    private val scope = CoroutineScope(Dispatchers.Main)

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        scope.launch {
            launch {
                mediaPlayerHandler.controlState.collectLatest { updateWidget(context) }
            }
            launch {
                mediaPlayerHandler.nowPlayingState.collectLatest { updateWidget(context) }
            }
        }

        provideContent {
            GlanceTheme {
                val controlState by mediaPlayerHandler.controlState.collectAsState()
                val nowPlaying by mediaPlayerHandler.nowPlayingState.collectAsState()

                var artwork by remember { mutableStateOf<Bitmap?>(null) }
                val thumbUrl = nowPlaying.mediaItem.metadata.artworkUri
                    ?: nowPlaying.track?.thumbnails?.lastOrNull()?.url

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

                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color(0xFF141318)))
                        .padding(6.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (artwork != null) {
                        Image(
                            provider = ImageProvider(artwork!!),
                            contentDescription = "Cover",
                            contentScale = ContentScale.Crop,
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .cornerRadius(18.dp),
                        )
                    } else {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .background(ColorProvider(Color(0xFF1F1E28)))
                                .cornerRadius(18.dp)
                        ) {}
                    }

                    // Liquid Glass control pill
                    Box(modifier = GlanceModifier.padding(bottom = 12.dp)) {
                        Row(
                            modifier = GlanceModifier
                                .background(ImageProvider(R.drawable.widget_control_pill))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.rounded_fast_rewind_24),
                                contentDescription = "Previous",
                                colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
                                modifier = GlanceModifier
                                    .size(32.dp)
                                    .padding(4.dp)
                                    .clickable {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            mediaPlayerHandler.onPlayerEvent(PlayerEvent.Previous)
                                        }
                                    },
                            )
                            Spacer(GlanceModifier.width(8.dp))
                            Box(
                                modifier = GlanceModifier
                                    .size(38.dp)
                                    .background(ImageProvider(R.drawable.widget_play_disc)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    provider = if (controlState.isPlaying) {
                                        ImageProvider(R.drawable.rounded_pause_24)
                                    } else {
                                        ImageProvider(R.drawable.rounded_play_arrow_24)
                                    },
                                    contentDescription = if (controlState.isPlaying) "Pause" else "Play",
                                    colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
                                    modifier = GlanceModifier
                                        .size(22.dp)
                                        .clickable {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                mediaPlayerHandler.onPlayerEvent(PlayerEvent.PlayPause)
                                            }
                                        },
                                )
                            }
                            Spacer(GlanceModifier.width(8.dp))
                            Image(
                                provider = ImageProvider(R.drawable.rounded_fast_forward_24),
                                contentDescription = "Next",
                                colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
                                modifier = GlanceModifier
                                    .size(32.dp)
                                    .padding(4.dp)
                                    .clickable {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            mediaPlayerHandler.onPlayerEvent(PlayerEvent.Next)
                                        }
                                    },
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun updateWidget(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(this@TurntableWidget.javaClass).forEach { glanceId ->
            this@TurntableWidget.update(context, glanceId)
        }
    }
}
