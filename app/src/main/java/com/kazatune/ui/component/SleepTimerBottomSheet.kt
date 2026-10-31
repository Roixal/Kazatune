package com.kazatune.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazatune.ui.theme.DinProFamily
import com.kazatune.ui.theme.RessoAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerBottomSheet(
    timeRemainingMinutes: Int,
    onSetTimer: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customMinutesText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val isTimerActive = timeRemainingMinutes > 0 || timeRemainingMinutes == -1

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
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header
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
                            imageVector = Icons.Default.AvTimer,
                            contentDescription = null,
                            tint = RessoAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sleep Timer",
                        fontFamily = DinProFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0x88FFFFFF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current Status Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1D24),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Timer Status",
                            fontFamily = DinProFamily,
                            fontSize = 12.sp,
                            color = Color(0x88FFFFFF)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when {
                                timeRemainingMinutes == -1 -> "End of current track"
                                timeRemainingMinutes > 0 -> "$timeRemainingMinutes min remaining"
                                else -> "Timer is off"
                            },
                            fontFamily = DinProFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTimerActive) RessoAccent else Color.White
                        )
                    }

                    if (isTimerActive) {
                        Button(
                            onClick = {
                                onSetTimer(0)
                                onDismissRequest()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF0050)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = "Turn Off",
                                fontFamily = DinProFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = RessoAccent
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Presets
            Text(
                text = "Quick Presets",
                fontFamily = DinProFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xCCFFFFFF)
            )

            Spacer(modifier = Modifier.height(10.dp))

            val presets = listOf(
                15 to "15 min",
                30 to "30 min",
                45 to "45 min",
                60 to "1 hour",
                90 to "1.5 hours",
                -1 to "End of track"
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presets) { (mins, label) ->
                    val isSelected = timeRemainingMinutes == mins
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) RessoAccent else Color(0xFF22212B),
                        modifier = Modifier.clickable {
                            onSetTimer(mins)
                            onDismissRequest()
                        }
                    ) {
                        Text(
                            text = label,
                            fontFamily = DinProFamily,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xDDFFFFFF),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Custom Time Option
            Text(
                text = "Custom Duration",
                fontFamily = DinProFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xCCFFFFFF)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customMinutesText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 4) {
                            customMinutesText = input
                        }
                    },
                    placeholder = {
                        Text(
                            text = "Enter minutes (e.g. 25)",
                            fontFamily = DinProFamily,
                            fontSize = 13.sp,
                            color = Color(0x66FFFFFF)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val parsed = customMinutesText.toIntOrNull()
                            if (parsed != null && parsed > 0) {
                                onSetTimer(parsed)
                                focusManager.clearFocus()
                                onDismissRequest()
                            }
                        }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RessoAccent,
                        unfocusedBorderColor = Color(0x22FFFFFF),
                        focusedContainerColor = Color(0xFF1B1A22),
                        unfocusedContainerColor = Color(0xFF16151D),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = {
                        val parsed = customMinutesText.toIntOrNull()
                        if (parsed != null && parsed > 0) {
                            onSetTimer(parsed)
                            focusManager.clearFocus()
                            onDismissRequest()
                        }
                    },
                    enabled = (customMinutesText.toIntOrNull() ?: 0) > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = RessoAccent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text(
                        text = "Set",
                        fontFamily = DinProFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

