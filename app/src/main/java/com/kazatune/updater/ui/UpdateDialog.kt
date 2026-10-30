package com.kazatune.updater.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kazatune.ui.theme.DinProFamily
import com.kazatune.ui.theme.RessoAccent
import com.kazatune.updater.model.ReleaseInfo
import com.kazatune.updater.model.UpdateStatus
import java.io.File

@Composable
fun UpdateDialog(
    status: UpdateStatus,
    currentVersion: String,
    onDismiss: () -> Unit,
    onDownload: (ReleaseInfo) -> Unit,
    onInstall: (File) -> Unit,
    onCancelDownload: () -> Unit,
    onRetry: () -> Unit
) {
    val context = LocalContext.current

    val release: ReleaseInfo? = when (status) {
        is UpdateStatus.UpdateAvailable -> status.release
        is UpdateStatus.Downloading -> status.release
        is UpdateStatus.ReadyToInstall -> status.release
        else -> null
    }

    val isMandatory = release?.isMandatory == true

    Dialog(
        onDismissRequest = {
            if (!isMandatory && status !is UpdateStatus.Downloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isMandatory && status !is UpdateStatus.Downloading,
            dismissOnClickOutside = !isMandatory && status !is UpdateStatus.Downloading,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp)),
            color = Color(0xFF181720),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isMandatory) Color(0xFFFF3366) else RessoAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isMandatory) Icons.Default.Security else Icons.Default.NewReleases,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isMandatory) "Mandatory Update" else "New Version Available",
                                fontFamily = DinProFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Skim Music",
                                fontFamily = DinProFamily,
                                fontSize = 12.sp,
                                color = Color(0x88FFFFFF)
                            )
                        }
                    }

                    if (!isMandatory && status !is UpdateStatus.Downloading) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0x88FFFFFF)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Version Badge Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF201F2B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Current Version",
                                fontFamily = DinProFamily,
                                fontSize = 11.sp,
                                color = Color(0x88FFFFFF)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "v$currentVersion",
                                fontFamily = DinProFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xDDFFFFFF)
                            )
                        }

                        Text(
                            text = "➔",
                            fontFamily = DinProFamily,
                            fontSize = 16.sp,
                            color = RessoAccent
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Latest Version",
                                fontFamily = DinProFamily,
                                fontSize = 11.sp,
                                color = Color(0x88FFFFFF)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (release != null) "v${release.versionName}" else "Checking...",
                                fontFamily = DinProFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = RessoAccent
                            )
                        }
                    }
                }

                // Mandatory notice banner
                if (isMandatory) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0x33FF0050),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ This critical update contains essential security and playback fixes. Updating is required to continue.",
                            fontFamily = DinProFamily,
                            fontSize = 12.sp,
                            color = Color(0xFFFFB3C6),
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Release Notes / What's New
                if (release != null && release.body.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "What's New:",
                        fontFamily = DinProFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xEEFFFFFF)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val scrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF14131A))
                            .padding(12.dp)
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            text = release.body.trim(),
                            fontFamily = DinProFamily,
                            fontSize = 12.sp,
                            color = Color(0xCCFFFFFF),
                            lineHeight = 17.sp
                        )
                    }
                }

                // Download Progress Section
                AnimatedVisibility(visible = status is UpdateStatus.Downloading) {
                    if (status is UpdateStatus.Downloading) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Downloading APK: ${(status.progress * 100).toInt()}%",
                                    fontFamily = DinProFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = status.speedFormatted,
                                    fontFamily = DinProFamily,
                                    fontSize = 12.sp,
                                    color = RessoAccent
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { status.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = RessoAccent,
                                trackColor = Color(0xFF2C2B36)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            val downloadedMb = String.format("%.1f", status.bytesDownloaded / (1024.0 * 1024.0))
                            val totalMb = String.format("%.1f", status.totalBytes / (1024.0 * 1024.0))

                            Text(
                                text = "$downloadedMb MB / $totalMb MB",
                                fontFamily = DinProFamily,
                                fontSize = 11.sp,
                                color = Color(0x88FFFFFF)
                            )
                        }
                    }
                }

                // Error Message Section
                if (status is UpdateStatus.Error) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0x33FF3366),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = status.message,
                            fontFamily = DinProFamily,
                            fontSize = 12.sp,
                            color = Color(0xFFFF8888),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Action Buttons
                when (status) {
                    is UpdateStatus.Downloading -> {
                        OutlinedButton(
                            onClick = onCancelDownload,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel Download", fontFamily = DinProFamily, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    is UpdateStatus.ReadyToInstall -> {
                        Button(
                            onClick = { onInstall(status.apkFile) },
                            colors = ButtonDefaults.buttonColors(containerColor = RessoAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Install Now", fontFamily = DinProFamily, fontWeight = FontWeight.Bold)
                        }
                    }

                    is UpdateStatus.Error -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Close", fontFamily = DinProFamily, color = Color(0xCCFFFFFF))
                            }
                            if (status.canRetry) {
                                Button(
                                    onClick = onRetry,
                                    colors = ButtonDefaults.buttonColors(containerColor = RessoAccent),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Retry", fontFamily = DinProFamily, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    else -> {
                        // UpdateAvailable or Checking
                        if (release != null) {
                            if (release.apkDownloadUrl != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (!isMandatory) {
                                        TextButton(
                                            onClick = onDismiss,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "Maybe Later",
                                                fontFamily = DinProFamily,
                                                color = Color(0x88FFFFFF),
                                                fontSize = 14.sp
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { onDownload(release) },
                                        colors = ButtonDefaults.buttonColors(containerColor = RessoAccent),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Update Now",
                                            fontFamily = DinProFamily,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                // Release found but no APK asset uploaded yet
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "APK asset is being packaged on GitHub.",
                                        fontFamily = DinProFamily,
                                        fontSize = 12.sp,
                                        color = Color(0xAAFFFFFF)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl))
                                            context.startActivity(browserIntent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2B36)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("View Release on GitHub", fontFamily = DinProFamily)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
