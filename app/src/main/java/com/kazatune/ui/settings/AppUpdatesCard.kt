package com.kazatune.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kazatune.ui.theme.DinProFamily
import com.kazatune.ui.theme.RessoAccent
import com.kazatune.updater.UpdateRepository
import com.kazatune.updater.model.UpdateStatus
import com.kazatune.viewmodel.UpdateViewModel

@Composable
fun AppUpdatesCard(
    updateViewModel: UpdateViewModel,
    onOpenDialog: () -> Unit
) {
    val context = LocalContext.current
    val status by updateViewModel.updateStatus.collectAsStateWithLifecycle()
    val lastChecked by updateViewModel.lastCheckedTimestamp.collectAsStateWithLifecycle()
    val currentVersion = updateViewModel.currentVersionName

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF181720),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: App Update Title and Current Version
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF252430)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Update",
                        tint = RessoAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kazatune Version: v$currentVersion",
                        fontFamily = DinProFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Last Checked: ${formatLastChecked(lastChecked)}",
                        fontFamily = DinProFamily,
                        fontSize = 12.sp,
                        color = Color(0x88FFFFFF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Status Details Container
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF201F2B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    when (val currentStatus = status) {
                        is UpdateStatus.Idle -> {
                            Text(
                                text = "Ready to check GitHub for updates.",
                                fontFamily = DinProFamily,
                                fontSize = 13.sp,
                                color = Color(0xAAFFFFFF)
                            )
                        }

                        is UpdateStatus.Checking -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = RessoAccent
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Checking GitHub Releases...",
                                    fontFamily = DinProFamily,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }

                        is UpdateStatus.NoUpdateAvailable -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "You're using the latest version (v${currentStatus.currentVersion})",
                                    fontFamily = DinProFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF81C784)
                                )
                            }
                        }

                        is UpdateStatus.UpdateAvailable -> {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Update Available: v${currentStatus.release.versionName}",
                                        fontFamily = DinProFamily,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RessoAccent
                                    )
                                    if (currentStatus.release.isMandatory) {
                                        Text(
                                            text = "REQUIRED",
                                            fontFamily = DinProFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF3366)
                                        )
                                    }
                                }
                                if (currentStatus.release.body.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentStatus.release.body.lines().take(2).joinToString("\n").take(120) + "...",
                                        fontFamily = DinProFamily,
                                        fontSize = 11.sp,
                                        color = Color(0x99FFFFFF),
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }

                        is UpdateStatus.Downloading -> {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Downloading: ${(currentStatus.progress * 100).toInt()}%",
                                        fontFamily = DinProFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = currentStatus.speedFormatted,
                                        fontFamily = DinProFamily,
                                        fontSize = 12.sp,
                                        color = RessoAccent
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                LinearProgressIndicator(
                                    progress = { currentStatus.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape),
                                    color = RessoAccent,
                                    trackColor = Color(0xFF2C2B36)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                val downloadedMb = String.format("%.1f", currentStatus.bytesDownloaded / (1024.0 * 1024.0))
                                val totalMb = String.format("%.1f", currentStatus.totalBytes / (1024.0 * 1024.0))

                                Text(
                                    text = "$downloadedMb MB / $totalMb MB",
                                    fontFamily = DinProFamily,
                                    fontSize = 10.sp,
                                    color = Color(0x88FFFFFF)
                                )
                            }
                        }

                        is UpdateStatus.ReadyToInstall -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.InstallMobile,
                                    contentDescription = null,
                                    tint = RessoAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "v${currentStatus.release.versionName} downloaded! Tap Install.",
                                    fontFamily = DinProFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }

                        is UpdateStatus.Error -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = currentStatus.message,
                                    fontFamily = DinProFamily,
                                    fontSize = 12.sp,
                                    color = Color(0xFFFF8888)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (val currentStatus = status) {
                    is UpdateStatus.UpdateAvailable -> {
                        Button(
                            onClick = {
                                if (currentStatus.release.apkDownloadUrl != null) {
                                    updateViewModel.downloadAndInstall(context, currentStatus.release)
                                } else {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(currentStatus.release.htmlUrl))
                                    context.startActivity(browserIntent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RessoAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (currentStatus.release.apkDownloadUrl != null) "Update Now" else "View Release",
                                fontFamily = DinProFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenDialog,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Release Notes",
                                fontFamily = DinProFamily,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    }

                    is UpdateStatus.Downloading -> {
                        OutlinedButton(
                            onClick = { updateViewModel.cancelDownload() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Cancel Download",
                                fontFamily = DinProFamily,
                                fontSize = 13.sp,
                                color = Color(0xFFFF5252)
                            )
                        }
                    }

                    is UpdateStatus.ReadyToInstall -> {
                        Button(
                            onClick = { updateViewModel.triggerInstall(context, currentStatus.apkFile) },
                            colors = ButtonDefaults.buttonColors(containerColor = RessoAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Install Update",
                                fontFamily = DinProFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    else -> {
                        // Idle, Checking, NoUpdateAvailable, Error
                        Button(
                            onClick = { updateViewModel.checkForUpdates(manual = true) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF252430)),
                            shape = RoundedCornerShape(10.dp),
                            enabled = status !is UpdateStatus.Checking,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (status is UpdateStatus.Checking) "Checking..." else "Check for Updates",
                                fontFamily = DinProFamily,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // GitHub Releases Link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(UpdateRepository.RELEASES_PAGE_URL))
                        context.startActivity(browserIntent)
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = Color(0x88FFFFFF),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "View GitHub Releases",
                    fontFamily = DinProFamily,
                    fontSize = 12.sp,
                    color = Color(0xAAFFFFFF)
                )
            }
        }
    }
}

private fun formatLastChecked(timestamp: Long): String {
    if (timestamp <= 0L) return "Never"
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "$minutes min${if (minutes > 1) "s" else ""} ago"
        hours < 24 -> "$hours hour${if (hours > 1) "s" else ""} ago"
        else -> "$days day${if (days > 1) "s" else ""} ago"
    }
}
