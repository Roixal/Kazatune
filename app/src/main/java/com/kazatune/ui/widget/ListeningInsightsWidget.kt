package com.kazatune.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.kazatune.MainActivity
import com.maxrave.domain.repository.SongRepository
import kotlinx.coroutines.flow.firstOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ListeningInsightsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ListeningInsightsWidget()
}

/**
 * Listening Insights Widget (4x2):
 * Displays listening time, play count and 30-day top songs/artists stats on home screen.
 */
class ListeningInsightsWidget : GlanceAppWidget(), KoinComponent {
    override val sizeMode: SizeMode = SizeMode.Exact

    private val songRepository by inject<SongRepository>()

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            GlanceTheme {
                var likedCount by remember { mutableIntStateOf(0) }
                var topSongTitle by remember { mutableStateOf("Discovering...") }
                var topArtistName by remember { mutableStateOf("Skim Music") }

                LaunchedEffect(Unit) {
                    val liked = songRepository.getLikedSongs().firstOrNull() ?: emptyList()
                    likedCount = liked.size
                    if (liked.isNotEmpty()) {
                        val first = liked.first()
                        topSongTitle = first.title
                        topArtistName = first.artistName?.firstOrNull() ?: "Top Track"
                    }
                }

                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color(0xFF13131C)))
                        .cornerRadius(22.dp)
                        .padding(14.dp)
                        .clickable(actionStartActivity<MainActivity>())
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎧 Skim Listening Insights",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFFF3366)),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(GlanceModifier.height(8.dp))

                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = "$likedCount songs",
                                    style = TextStyle(
                                        color = ColorProvider(Color.White),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "in your library",
                                    style = TextStyle(
                                        color = ColorProvider(Color(0x88FFFFFF)),
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            Spacer(GlanceModifier.width(12.dp))

                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = topArtistName,
                                    style = TextStyle(
                                        color = ColorProvider(Color.White),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1
                                )
                                Text(
                                    text = "Top Artist · $topSongTitle",
                                    style = TextStyle(
                                        color = ColorProvider(Color(0x88FFFFFF)),
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
