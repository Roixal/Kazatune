package com.kazatune.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazatune.ui.theme.DinProFamily
import com.kazatune.ui.theme.RessoAccent
import com.kazatune.viewmodel.EQUALIZER_BANDS_HZ
import com.kazatune.viewmodel.EQUALIZER_PRESETS
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerBottomSheet(
    enabled: Boolean,
    activePreset: String,
    bandsDb: List<Float>,
    bassBoostStrength: Float,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectPreset: (String) -> Unit,
    onBandChange: (index: Int, gainDb: Float) -> Unit,
    onBassBoostChange: (Float) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF141318),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Title, Icon, Master Switch, Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FF0050)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = RessoAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "10-Band Equalizer",
                            fontFamily = DinProFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (enabled) "Active • Robert Bristow-Johnson Filters" else "Bypassed (Bit-perfect Flat)",
                            fontFamily = DinProFamily,
                            fontSize = 12.sp,
                            color = if (enabled) Color(0xFF00E5FF) else Color(0x66FFFFFF)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = enabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RessoAccent,
                            uncheckedThumbColor = Color(0x88FFFFFF),
                            uncheckedTrackColor = Color(0x22FFFFFF)
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0x88FFFFFF)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = enabled) {
                Column {
                    // Presets Horizontal Row
                    Text(
                        text = "PRESETS",
                        fontFamily = DinProFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0x88FFFFFF),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val allPresets = listOf("Flat", "Bass Boost", "Hip-Hop", "Rock", "Acoustic", "Vocal Booster", "Electronic", "Pop", "Custom")
                        allPresets.forEach { preset ->
                            val isSelected = activePreset.equals(preset, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) RessoAccent else Color(0xFF1E1D24),
                                border = if (isSelected) null else BorderStroke(1.dp, Color(0x22FFFFFF)),
                                modifier = Modifier.clickable {
                                    if (preset != "Custom") onSelectPreset(preset)
                                }
                            ) {
                                Text(
                                    text = preset,
                                    fontFamily = DinProFamily,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xCCFFFFFF),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Bass Boost Slider
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1B1A22),
                        border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sub-Bass Punch",
                                    fontFamily = DinProFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                val boostPct = (bassBoostStrength * 100).roundToInt()
                                Text(
                                    text = if (boostPct > 0) "+$boostPct%" else "OFF",
                                    fontFamily = DinProFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (boostPct > 0) Color(0xFF00E5FF) else Color(0x66FFFFFF)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
                                value = bassBoostStrength,
                                onValueChange = onBassBoostChange,
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00E5FF),
                                    activeTrackColor = Color(0xFF00E5FF),
                                    inactiveTrackColor = Color(0x22FFFFFF)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 10-Band Graphic EQ Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "10-BAND GRAPHIC EQ",
                            fontFamily = DinProFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0x88FFFFFF),
                            letterSpacing = 1.sp
                        )

                        TextButton(onClick = { onSelectPreset("Flat") }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color(0x88FFFFFF),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reset Flat",
                                fontFamily = DinProFamily,
                                fontSize = 12.sp,
                                color = Color(0x88FFFFFF)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sliders container (Horizontal scroll for 10 bands)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1B1A22),
                        border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            EQUALIZER_BANDS_HZ.forEachIndexed { index, freqLabel ->
                                val gain = bandsDb.getOrNull(index) ?: 0f

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(52.dp)
                                ) {
                                    // Current Gain Label
                                    val formattedGain = if (gain > 0) "+${gain.roundToInt()}" else "${gain.roundToInt()}"
                                    Text(
                                        text = "${formattedGain}dB",
                                        fontFamily = DinProFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            gain > 0.5f -> RessoAccent
                                            gain < -0.5f -> Color(0xFF00E5FF)
                                            else -> Color(0x88FFFFFF)
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Vertical Slider: Box with rotated Slider
                                    Box(
                                        modifier = Modifier
                                            .height(140.dp)
                                            .width(52.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Center zero-reference line
                                        Box(
                                            modifier = Modifier
                                                .width(28.dp)
                                                .height(1.dp)
                                                .background(Color(0x33FFFFFF))
                                        )

                                        Slider(
                                            value = gain,
                                            onValueChange = { newGain ->
                                                onBandChange(index, newGain)
                                            },
                                            valueRange = -12f..12f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = if (gain != 0f) RessoAccent else Color.White,
                                                activeTrackColor = RessoAccent,
                                                inactiveTrackColor = Color(0x22FFFFFF)
                                            ),
                                            modifier = Modifier
                                                .width(130.dp)
                                                .graphicsLayer {
                                                    rotationZ = 270f
                                                }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Frequency Label
                                    Text(
                                        text = freqLabel,
                                        fontFamily = DinProFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!enabled) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1B1A22),
                    border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Equalizer is Turned Off",
                            fontFamily = DinProFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Toggle the switch at top right to shape sound with high-precision 10-band studio filters and bass punch.",
                            fontFamily = DinProFamily,
                            fontSize = 12.sp,
                            color = Color(0x88FFFFFF),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
