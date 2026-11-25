package com.kazatune.ui.library

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kazatune.mix.MixEqFilter
import com.kazatune.mix.MixPreset
import com.kazatune.mix.PairTransition
import com.kazatune.mix.PlaylistMixSettings
import com.kazatune.ui.theme.DinProFamily
import com.kazatune.ui.theme.RessoAccent
import com.kazatune.viewmodel.TrackAudioMeta
import com.maxrave.domain.data.model.browse.album.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistMixSheet(
    playlistTitle: String,
    tracks: List<Track>,
    settings: PlaylistMixSettings,
    trackAudioMeta: Map<String, TrackAudioMeta>,
    onDismissRequest: () -> Unit,
    onUpdateSettings: (PlaylistMixSettings) -> Unit,
    onSetPairTransition: (fromVideoId: String, toVideoId: String, PairTransition) -> Unit,
    onPlayMixed: () -> Unit,
    onShareMixedJson: () -> String
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editingPair by remember { mutableStateOf<Pair<Track, Track>?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF121218),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .padding(horizontal = 20.dp)
        ) {
            // Header: Mix Badge, Title, Close & Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(RessoAccent, Color(0xFF00E5FF))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Playlist Mix",
                            fontFamily = DinProFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = playlistTitle,
                            fontFamily = DinProFamily,
                            fontSize = 12.sp,
                            color = Color(0x99FFFFFF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (tracks.isNotEmpty()) {
                        IconButton(onClick = {
                            val json = onShareMixedJson()
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "$playlistTitle (Skim Mix)")
                                putExtra(Intent.EXTRA_TEXT, json)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Mixed Playlist"))
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Mixed Playlist",
                                tint = Color(0xFF00E5FF)
                            )
                        }
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Master Mix Toggle + Play Mixed Button
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1C1B24),
                border = BorderStroke(
                    1.dp,
                    if (settings.mixEnabled) Color(0x6600E5FF) else Color(0x18FFFFFF)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (settings.mixEnabled) "DJ Mix Enabled" else "Turn On Mix",
                            fontFamily = DinProFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Seamless beat & key-matched transitions",
                            fontFamily = DinProFamily,
                            fontSize = 12.sp,
                            color = Color(0x88FFFFFF)
                        )
                    }
                    Switch(
                        checked = settings.mixEnabled,
                        onCheckedChange = { enabled ->
                            onUpdateSettings(settings.copy(mixEnabled = enabled))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RessoAccent,
                            uncheckedThumbColor = Color(0x88FFFFFF),
                            uncheckedTrackColor = Color(0xFF2B2A36)
                        )
                    )
                }
            }

            AnimatedVisibility(visible = settings.mixEnabled) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset Selector
                    Text(
                        text = "Transition Style",
                        fontFamily = DinProFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xAAFFFFFF)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MixPreset.entries.forEach { preset ->
                            val selected = settings.preset == preset
                            Surface(
                                shape = CircleShape,
                                color = if (selected) RessoAccent else Color(0xFF22212C),
                                modifier = Modifier.clickable {
                                    onUpdateSettings(settings.copy(preset = preset))
                                }
                            ) {
                                Text(
                                    text = preset.label,
                                    fontFamily = DinProFamily,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bar Length & DJ EQ Filter Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(4, 8, 16).forEach { barCount ->
                                val selected = settings.bars == barCount
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) Color(0x3300E5FF) else Color(0xFF1F1E28),
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) Color(0xFF00E5FF) else Color.Transparent
                                    ),
                                    modifier = Modifier.clickable {
                                        onUpdateSettings(settings.copy(bars = barCount))
                                    }
                                ) {
                                    Text(
                                        text = "$barCount bars",
                                        fontFamily = DinProFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (selected) Color(0xFF00E5FF) else Color(0xBBFFFFFF),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (settings.djFilterEnabled) Color(0x33FF0050) else Color(0xFF1F1E28),
                            border = BorderStroke(
                                1.dp,
                                if (settings.djFilterEnabled) RessoAccent else Color.Transparent
                            ),
                            modifier = Modifier.clickable {
                                onUpdateSettings(settings.copy(djFilterEnabled = !settings.djFilterEnabled))
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = if (settings.djFilterEnabled) RessoAccent else Color(0x99FFFFFF),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (settings.djFilterEnabled) "DJ EQ: On" else "DJ EQ: Off",
                                    fontFamily = DinProFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Play Button
            Button(
                onClick = {
                    onPlayMixed()
                    onDismissRequest()
                },
                enabled = tracks.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RessoAccent,
                    contentColor = Color.White
                ),
                shape = CircleShape,
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (settings.mixEnabled) "Play Mixed Playlist" else "Play Playlist",
                    fontFamily = DinProFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Track-by-Track Transition Timeline
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(tracks) { index, track ->
                    val meta = trackAudioMeta[track.videoId]
                    val bpmLabel = meta?.bpm?.let { "$it BPM" }
                    val camelotLabel = meta?.camelotCode

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1A1922),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = track.thumbnails?.lastOrNull()?.url,
                                contentDescription = track.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    fontFamily = DinProFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artists?.joinToString(", ") { it.name } ?: "Artist",
                                    fontFamily = DinProFamily,
                                    fontSize = 12.sp,
                                    color = Color(0x88FFFFFF),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (bpmLabel != null || camelotLabel != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x2200E5FF)
                                ) {
                                    Text(
                                        text = listOfNotNull(bpmLabel, camelotLabel).joinToString(" • "),
                                        fontFamily = DinProFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E5FF),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Transition Connector between Track[index] -> Track[index + 1]
                    val nextTrack = tracks.getOrNull(index + 1)
                    if (settings.mixEnabled && nextTrack != null) {
                        val pairKey = "${track.videoId}_${nextTrack.videoId}"
                        val override = settings.pairOverrides[pairKey]
                        val activeTransition = override ?: PairTransition(
                            preset = settings.preset,
                            bars = settings.bars,
                            eqFilter = if (settings.djFilterEnabled) MixEqFilter.AUTO else MixEqFilter.OFF
                        )
                        val isCustom = override != null

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCustom) Color(0x28FF0050) else Color(0x18FFFFFF))
                                .clickable { editingPair = track to nextTrack }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TransitionCurvePreview(
                                    preset = activeTransition.preset,
                                    modifier = Modifier.size(width = 36.dp, height = 16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${if (isCustom) "Custom" else "Mix"}: ${activeTransition.preset.label}" +
                                        if (activeTransition.preset != MixPreset.CUT && activeTransition.preset != MixPreset.FADE) {
                                            " • ${activeTransition.bars} bars"
                                        } else "",
                                    fontFamily = DinProFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isCustom) RessoAccent else Color(0xCCFFFFFF)
                                )
                            }
                            Text(
                                text = "Edit",
                                fontFamily = DinProFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }
                }
            }
        }
    }

    // Per-Pair Transition Customizer Dialog
    val pairToEdit = editingPair
    if (pairToEdit != null) {
        val (fromTrack, toTrack) = pairToEdit
        val pairKey = "${fromTrack.videoId}_${toTrack.videoId}"
        val initial = settings.pairOverrides[pairKey] ?: PairTransition(
            preset = settings.preset,
            bars = settings.bars,
            eqFilter = if (settings.djFilterEnabled) MixEqFilter.AUTO else MixEqFilter.OFF
        )
        var selectedPreset by remember(pairKey) { mutableStateOf(initial.preset) }
        var selectedBars by remember(pairKey) { mutableStateOf(initial.bars) }
        var selectedEq by remember(pairKey) { mutableStateOf(initial.eqFilter) }

        AlertDialog(
            onDismissRequest = { editingPair = null },
            containerColor = Color(0xFF1C1B24),
            title = {
                Column {
                    Text(
                        text = "Customize Transition",
                        fontFamily = DinProFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${fromTrack.title} → ${toTrack.title}",
                        fontFamily = DinProFamily,
                        fontSize = 12.sp,
                        color = Color(0x99FFFFFF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Curve preview
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF121118),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TransitionCurvePreview(
                                preset = selectedPreset,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                            )
                        }
                    }

                    Text("Mode", color = Color(0xAAFFFFFF), fontSize = 12.sp, fontFamily = DinProFamily)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MixPreset.entries.forEach { preset ->
                            val isSel = selectedPreset == preset
                            Surface(
                                shape = CircleShape,
                                color = if (isSel) RessoAccent else Color(0xFF282734),
                                modifier = Modifier.clickable { selectedPreset = preset }
                            ) {
                                Text(
                                    text = preset.label,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontFamily = DinProFamily,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    if (selectedPreset != MixPreset.CUT && selectedPreset != MixPreset.FADE) {
                        Text("Overlap Length", color = Color(0xAAFFFFFF), fontSize = 12.sp, fontFamily = DinProFamily)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(4, 8, 16).forEach { bars ->
                                val isSel = selectedBars == bars
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) Color(0x3300E5FF) else Color(0xFF282734),
                                    border = BorderStroke(1.dp, if (isSel) Color(0xFF00E5FF) else Color.Transparent),
                                    modifier = Modifier.clickable { selectedBars = bars }
                                ) {
                                    Text(
                                        text = "$bars bars",
                                        color = if (isSel) Color(0xFF00E5FF) else Color.White,
                                        fontSize = 12.sp,
                                        fontFamily = DinProFamily,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text("DJ Filter / EQ", color = Color(0xAAFFFFFF), fontSize = 12.sp, fontFamily = DinProFamily)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MixEqFilter.entries.forEach { eq ->
                            val isSel = selectedEq == eq
                            Surface(
                                shape = CircleShape,
                                color = if (isSel) Color(0xFF00E5FF) else Color(0xFF282734),
                                modifier = Modifier.clickable { selectedEq = eq }
                            ) {
                                Text(
                                    text = eq.label,
                                    color = if (isSel) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = DinProFamily,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSetPairTransition(
                        fromTrack.videoId,
                        toTrack.videoId,
                        PairTransition(selectedPreset, selectedBars, selectedEq)
                    )
                    editingPair = null
                }) {
                    Text("Save Mix", color = RessoAccent, fontWeight = FontWeight.Bold, fontFamily = DinProFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPair = null }) {
                    Text("Cancel", color = Color(0xAAFFFFFF), fontFamily = DinProFamily)
                }
            }
        )
    }
}

@Composable
private fun TransitionCurvePreview(
    preset: MixPreset,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val outgoingPath = Path()
        val incomingPath = Path()

        when (preset) {
            MixPreset.CUT -> {
                outgoingPath.moveTo(0f, h * 0.2f)
                outgoingPath.lineTo(w * 0.48f, h * 0.2f)
                outgoingPath.lineTo(w * 0.5f, h * 0.9f)

                incomingPath.moveTo(w * 0.5f, h * 0.9f)
                incomingPath.lineTo(w * 0.52f, h * 0.2f)
                incomingPath.lineTo(w, h * 0.2f)
            }
            MixPreset.FADE -> {
                outgoingPath.moveTo(0f, h * 0.2f)
                outgoingPath.lineTo(w * 0.48f, h * 0.9f)

                incomingPath.moveTo(w * 0.52f, h * 0.9f)
                incomingPath.lineTo(w, h * 0.2f)
            }
            MixPreset.OVERLAP -> {
                outgoingPath.moveTo(0f, h * 0.2f)
                outgoingPath.lineTo(w * 0.7f, h * 0.2f)
                outgoingPath.lineTo(w * 0.85f, h * 0.9f)

                incomingPath.moveTo(w * 0.15f, h * 0.9f)
                incomingPath.lineTo(w * 0.3f, h * 0.2f)
                incomingPath.lineTo(w, h * 0.2f)
            }
            MixPreset.AUTO, MixPreset.CROSSFADE -> {
                outgoingPath.moveTo(0f, h * 0.2f)
                outgoingPath.cubicTo(w * 0.4f, h * 0.2f, w * 0.5f, h * 0.9f, w * 0.85f, h * 0.9f)

                incomingPath.moveTo(w * 0.15f, h * 0.9f)
                incomingPath.cubicTo(w * 0.5f, h * 0.9f, w * 0.6f, h * 0.2f, w, h * 0.2f)
            }
        }

        drawPath(path = outgoingPath, color = Color(0xFFFF0050), style = Stroke(width = 3f))
        drawPath(path = incomingPath, color = Color(0xFF00E5FF), style = Stroke(width = 3f))
        drawLine(
            color = Color(0x33FFFFFF),
            start = Offset(w * 0.5f, 0f),
            end = Offset(w * 0.5f, h),
            strokeWidth = 1.5f
        )
    }
}
