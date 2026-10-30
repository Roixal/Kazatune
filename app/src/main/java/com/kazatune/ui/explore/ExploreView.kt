package com.kazatune.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.kazatune.ui.theme.DinProFamily
import com.kazatune.ui.theme.RessoAccent
import com.kazatune.viewmodel.ExploreCategory
import com.kazatune.viewmodel.SkimViewModel
import com.maxrave.domain.data.model.browse.album.Track

@Composable
fun ExploreView(
    skimViewModel: SkimViewModel,
    onPlayTrack: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories by skimViewModel.exploreCategories.collectAsStateWithLifecycle()
    val selectedGenre by skimViewModel.selectedGenre.collectAsStateWithLifecycle()

    val genres = listOf("All", "Hip-Hop", "Pop", "Rock", "Electronic", "R&B", "Indie", "Dance", "Chill")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0B10))
    ) {
        // Genre Chips Header
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(genres) { genre ->
                val isSelected = genre.equals(selectedGenre, ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) RessoAccent else Color(0xFF1E1D26),
                    modifier = Modifier.clickable { skimViewModel.selectGenre(genre) }
                ) {
                    Text(
                        text = genre,
                        fontFamily = DinProFamily,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xAAFFFFFF),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        if (categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = RessoAccent,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Curating Explore Shelves...",
                        fontFamily = DinProFamily,
                        fontSize = 14.sp,
                        color = Color(0x99FFFFFF)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(categories) { category ->
                    ExploreShelfSection(
                        category = category,
                        onPlayTrack = onPlayTrack
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreShelfSection(
    category: ExploreCategory,
    onPlayTrack: (Track) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = category.title,
            fontFamily = DinProFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(category.tracks) { track ->
                ExploreTrackCard(track = track, onClick = { onPlayTrack(track) })
            }
        }
    }
}

@Composable
private fun ExploreTrackCard(
    track: Track,
    onClick: () -> Unit
) {
    val artworkUrl = track.thumbnails?.lastOrNull()?.url
    val artistName = track.artists?.joinToString(", ") { it.name ?: "" } ?: ""

    Column(
        modifier = Modifier
            .width(136.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(136.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1E1D26))
        ) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Play overlay badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = track.title,
            fontFamily = DinProFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = artistName.ifBlank { "Skim Music" },
            fontFamily = DinProFamily,
            fontSize = 11.sp,
            color = Color(0x88FFFFFF),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
