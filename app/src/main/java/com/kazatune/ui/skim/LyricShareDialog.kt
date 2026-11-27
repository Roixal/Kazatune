package com.kazatune.ui.skim

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.kazatune.ui.theme.DinProFamily
import com.kazatune.ui.theme.RessoAccent
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.metadata.Lyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricShareDialog(
    track: Track,
    lyrics: Lyrics?,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lines = lyrics?.lines ?: emptyList()

    val selectedLineIndices = remember { mutableStateListOf<Int>() }
    var isGenerating by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF14131A),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = RessoAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share Lyric Quote Card",
                        fontFamily = DinProFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0x99FFFFFF)
                    )
                }
            }

            // 9:16 Card Preview
            val selectedText = if (selectedLineIndices.isNotEmpty()) {
                selectedLineIndices.sorted().joinToString("\n") { lines[it].words }
            } else {
                lines.firstOrNull()?.words ?: track.title
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1C1B24),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Blurred Artwork Backdrop
                    AsyncImage(
                        model = track.thumbnails?.lastOrNull()?.url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(32.dp)
                    )
                    // Dark Scrim
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0x99000000), Color(0xEE000000))
                                )
                            )
                    )
                    // Card Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Quote Text
                        Text(
                            text = "\"$selectedText\"",
                            fontFamily = DinProFamily,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 24.sp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Bottom: Track Info + Skim Music Watermark
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    fontFamily = DinProFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artists?.joinToString(", ") { it.name ?: "" } ?: "",
                                    fontFamily = DinProFamily,
                                    fontSize = 11.sp,
                                    color = Color(0xAAFFFFFF),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0x33FF0050)
                            ) {
                                Text(
                                    text = "⚡ Skim Music",
                                    fontFamily = DinProFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RessoAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Tap up to 4 lines to quote:",
                fontFamily = DinProFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0x88FFFFFF)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Lines Picker List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(lines) { index, line ->
                    val isSelected = selectedLineIndices.contains(index)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0x33FF0050) else Color(0xFF1B1A24),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) RessoAccent else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                if (isSelected) {
                                    selectedLineIndices.remove(index)
                                } else if (selectedLineIndices.size < 4) {
                                    selectedLineIndices.add(index)
                                }
                            }
                    ) {
                        Text(
                            text = line.words,
                            fontFamily = DinProFamily,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xAAFFFFFF),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Share Action Button (9:16 Story Card)
            Button(
                onClick = {
                    if (isGenerating) return@Button
                    isGenerating = true
                    scope.launch {
                        try {
                            val bitmap = generateLyricStoryBitmap(context, track, selectedText)
                            val cacheDir = File(context.cacheDir, "shared")
                            if (!cacheDir.exists()) cacheDir.mkdirs()
                            val imageFile = File(cacheDir, "skim_lyric_story.png")
                            FileOutputStream(imageFile).use { out ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                            val contentUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                imageFile
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, contentUri)
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "\"$selectedText\"\n\n— ${track.title} on Skim Music"
                                )
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Story to Instagram / WhatsApp"))
                            onDismissRequest()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Fallback to text share
                            val shareText = "\"$selectedText\"\n\n— ${track.title} on Skim Music\nDownload Skim: https://github.com/Roixal/Kazatune/releases/tag/Stable"
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Lyric Card"))
                            onDismissRequest()
                        } finally {
                            isGenerating = false
                        }
                    }
                },
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = RessoAccent),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(bottom = 6.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Rendering Story Card...",
                        fontFamily = DinProFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share 9:16 Story (Instagram / WhatsApp)",
                        fontFamily = DinProFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Renders a vertical 9:16 (1080x1920) story graphic with high-res artwork,
 * blurred ambient glow background, typography, and watermark.
 */
private suspend fun generateLyricStoryBitmap(
    context: Context,
    track: Track,
    selectedQuote: String
): Bitmap = withContext(Dispatchers.IO) {
    val width = 1080
    val height = 1920
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // 1. Fetch artwork bitmap if available
    var albumBitmap: Bitmap? = null
    val artUrl = track.thumbnails?.lastOrNull()?.url
    if (!artUrl.isNullOrEmpty()) {
        try {
            val req = ImageRequest.Builder(context)
                .data(artUrl)
                .allowHardware(false)
                .build()
            albumBitmap = (ImageLoader(context).execute(req) as? SuccessResult)?.image?.toBitmap()
        } catch (_: Exception) {}
    }

    // 2. Base dark gradient background
    val baseBgPaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            intArrayOf(
                android.graphics.Color.rgb(18, 17, 24),
                android.graphics.Color.rgb(11, 10, 16),
                android.graphics.Color.rgb(8, 7, 12)
            ),
            null,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), baseBgPaint)

    // 3. Draw blurred/stretched backdrop if artwork exists
    if (albumBitmap != null) {
        val src = Rect(0, 0, albumBitmap.width, albumBitmap.height)
        val dst = Rect(0, 0, width, height)
        canvas.drawBitmap(albumBitmap, src, dst, Paint(Paint.FILTER_BITMAP_FLAG))

        // Heavy dark overlay for story readability
        val scrimPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(
                    android.graphics.Color.argb(210, 10, 9, 14),
                    android.graphics.Color.argb(240, 14, 13, 20),
                    android.graphics.Color.argb(250, 8, 7, 11)
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
    }

    // 4. Accent ambient glow circles (Resso Red + Cyan)
    val glowPaintAccent = Paint().apply {
        isAntiAlias = true
        shader = android.graphics.RadialGradient(
            200f, 300f, 600f,
            intArrayOf(android.graphics.Color.argb(70, 255, 0, 80), android.graphics.Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(200f, 300f, 600f, glowPaintAccent)

    val glowPaintCyan = Paint().apply {
        isAntiAlias = true
        shader = android.graphics.RadialGradient(
            880f, 1500f, 550f,
            intArrayOf(android.graphics.Color.argb(55, 0, 229, 255), android.graphics.Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(880f, 1500f, 550f, glowPaintCyan)

    // 5. High-Res Central Album Art (rounded rect)
    val artSize = 440f
    val artLeft = (width - artSize) / 2f
    val artTop = 260f
    val artRight = artLeft + artSize
    val artBottom = artTop + artSize
    val artRadius = 36f

    if (albumBitmap != null) {
        canvas.save()
        val artPath = Path().apply {
            addRoundRect(
                RectF(artLeft, artTop, artRight, artBottom),
                artRadius,
                artRadius,
                Path.Direction.CW
            )
        }
        canvas.clipPath(artPath)
        val src = Rect(0, 0, albumBitmap.width, albumBitmap.height)
        val dst = Rect(artLeft.toInt(), artTop.toInt(), artRight.toInt(), artBottom.toInt())
        canvas.drawBitmap(albumBitmap, src, dst, Paint(Paint.FILTER_BITMAP_FLAG))
        canvas.restore()

        // Border around album art
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = android.graphics.Color.argb(80, 255, 255, 255)
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(artLeft, artTop, artRight, artBottom), artRadius, artRadius, borderPaint)
    }

    // 6. Track Title & Artist
    val titlePaint = TextPaint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        textSize = 44f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val titleText = if (track.title.length > 36) track.title.take(33) + "..." else track.title
    canvas.drawText(titleText, width / 2f, artBottom + 70f, titlePaint)

    val artistPaint = TextPaint().apply {
        isAntiAlias = true
        color = android.graphics.Color.argb(180, 255, 255, 255)
        textSize = 32f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }
    val artistsStr = track.artists?.joinToString(", ") { it.name ?: "" } ?: ""
    val artistText = if (artistsStr.length > 42) artistsStr.take(39) + "..." else artistsStr
    canvas.drawText(artistText, width / 2f, artBottom + 120f, artistPaint)

    // 7. Quotation Symbol
    val quoteIconPaint = TextPaint().apply {
        isAntiAlias = true
        color = android.graphics.Color.rgb(255, 0, 80) // RessoAccent
        textSize = 90f
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val quoteY = artBottom + 210f
    canvas.drawText("“", width / 2f, quoteY, quoteIconPaint)

    // 8. Lyric Quote Block (multi-line StaticLayout)
    val quotePaint = TextPaint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        textSize = 46f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    val margin = 110
    val quoteLayoutWidth = width - (margin * 2)
    val staticLayout = StaticLayout.Builder.obtain(
        selectedQuote,
        0,
        selectedQuote.length,
        quotePaint,
        quoteLayoutWidth
    ).setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setLineSpacing(16f, 1.25f)
        .build()

    canvas.save()
    canvas.translate(margin.toFloat(), quoteY + 30f)
    staticLayout.draw(canvas)
    canvas.restore()

    // 9. Bottom Branding Badge ("⚡ SKIM MUSIC")
    val badgeWidth = 420f
    val badgeHeight = 76f
    val badgeLeft = (width - badgeWidth) / 2f
    val badgeTop = 1720f
    val badgeRight = badgeLeft + badgeWidth
    val badgeBottom = badgeTop + badgeHeight
    val badgeRadius = badgeHeight / 2f

    val badgeBgPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.argb(45, 255, 0, 80)
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(RectF(badgeLeft, badgeTop, badgeRight, badgeBottom), badgeRadius, badgeRadius, badgeBgPaint)

    val badgeStrokePaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.argb(120, 255, 0, 80)
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    canvas.drawRoundRect(RectF(badgeLeft, badgeTop, badgeRight, badgeBottom), badgeRadius, badgeRadius, badgeStrokePaint)

    val badgeTextPaint = TextPaint().apply {
        isAntiAlias = true
        color = android.graphics.Color.rgb(255, 0, 80)
        textSize = 28f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("⚡ SKIM MUSIC", width / 2f, badgeTop + 48f, badgeTextPaint)

    val footerSubPaint = TextPaint().apply {
        isAntiAlias = true
        color = android.graphics.Color.argb(110, 255, 255, 255)
        textSize = 22f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("Play songs seamlessly on Skim", width / 2f, badgeBottom + 40f, footerSubPaint)

    bitmap
}
