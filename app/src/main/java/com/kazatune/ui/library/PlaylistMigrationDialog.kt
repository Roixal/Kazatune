package com.kazatune.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kazatune.ui.theme.DinProFamily
import com.kazatune.ui.theme.LiquidGlassCard
import com.kazatune.ui.theme.LiquidGlassColors
import com.kazatune.ui.theme.RessoAccent
import com.kazatune.viewmodel.MigrationState
import com.kazatune.viewmodel.PlaylistMigrationViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlaylistMigrationDialog(
    onDismissRequest: () -> Unit,
    onMigrationComplete: () -> Unit,
    migrationViewModel: PlaylistMigrationViewModel = koinViewModel()
) {
    val state by migrationViewModel.migrationState.collectAsStateWithLifecycle()
    var playlistTitle by remember { mutableStateOf("") }
    var rawInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = {
        migrationViewModel.resetState()
        onDismissRequest()
    }) {
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(26.dp),
            backgroundColor = Color(0xF215141E),
            borderBrush = LiquidGlassColors.GlassBorderBrush
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Title Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SyncAlt,
                            contentDescription = null,
                            tint = RessoAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Playlist Migration",
                            fontFamily = DinProFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            migrationViewModel.resetState()
                            onDismissRequest()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0x99FFFFFF))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (val s = state) {
                    is MigrationState.Idle -> {
                        Text(
                            text = "Paste a YouTube playlist link, or a tracklist (Song - Artist per line) to convert into a Skim local playlist.",
                            fontFamily = DinProFamily,
                            fontSize = 13.sp,
                            color = Color(0xAAFFFFFF),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = playlistTitle,
                            onValueChange = { playlistTitle = it },
                            placeholder = { Text("Playlist Name (Optional)", color = Color(0x55FFFFFF), fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RessoAccent,
                                unfocusedBorderColor = Color(0x25FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0x22FFFFFF),
                                unfocusedContainerColor = Color(0x18FFFFFF)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = rawInput,
                            onValueChange = { rawInput = it },
                            placeholder = {
                                Text(
                                    "Paste YouTube link, Spotify/Apple tracklist, or backup JSON...",
                                    color = Color(0x55FFFFFF),
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 6,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RessoAccent,
                                unfocusedBorderColor = Color(0x25FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0x22FFFFFF),
                                unfocusedContainerColor = Color(0x18FFFFFF)
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { migrationViewModel.migratePlaylist(playlistTitle, rawInput) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RessoAccent)
                        ) {
                            Text(
                                text = "Start Conversion & Import",
                                fontFamily = DinProFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    is MigrationState.Processing -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = RessoAccent, strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = s.progress,
                                fontFamily = DinProFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            if (s.total > 0) {
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { s.current.toFloat() / s.total },
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = RessoAccent,
                                    trackColor = Color(0x22FFFFFF)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${s.current} / ${s.total} songs",
                                    fontFamily = DinProFamily,
                                    fontSize = 12.sp,
                                    color = Color(0x88FFFFFF)
                                )
                            }
                        }
                    }

                    is MigrationState.Success -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Migration Complete!",
                                fontFamily = DinProFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Successfully imported \"${s.playlistTitle}\" with ${s.songCount} songs into your library.",
                                fontFamily = DinProFamily,
                                fontSize = 13.sp,
                                color = Color(0xCCFFFFFF)
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = {
                                    migrationViewModel.resetState()
                                    onMigrationComplete()
                                },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(23.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RessoAccent)
                            ) {
                                Text("Done & View Library", fontFamily = DinProFamily, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    is MigrationState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Migration Notice",
                                fontFamily = DinProFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = s.message,
                                fontFamily = DinProFamily,
                                fontSize = 13.sp,
                                color = Color(0xCCFFFFFF),
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            TextButton(
                                onClick = { migrationViewModel.resetState() }
                            ) {
                                Text("Try Again", color = RessoAccent, fontFamily = DinProFamily, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
