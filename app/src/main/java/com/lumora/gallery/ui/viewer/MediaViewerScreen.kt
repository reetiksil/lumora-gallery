package com.lumora.gallery.ui.viewer

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumora.gallery.data.model.ExifMetadata
import com.lumora.gallery.data.model.MediaItem
import com.lumora.gallery.data.util.ExifReader
import com.lumora.gallery.ui.theme.AccentGold
import com.lumora.gallery.ui.theme.PureBlack
import com.lumora.gallery.ui.theme.SoothingBlue

/**
 * ColorOS Photos Media Viewer (Matching Screenshot 3).
 *
 * Supports:
 * - Fluid horizontal swipe paging between photos & videos.
 * - Swipe up to reveal details bottom sheet.
 * - Swipe down to dismiss viewer.
 * - ColorOS video transport bar:
 *   [ 0:01 ] ──────●────── [ 0:06 ]
 *   [Mute]  [-10s]         [+10s]  [1.0x]
 *   [Share] [Favourite] (▶/⏸) [Delete] [More]
 */
@Composable
fun MediaViewerScreen(
    mediaList: List<MediaItem>,
    initialIndex: Int,
    onBackClick: () -> Unit,
    onDeleteMedia: (MediaItem) -> Unit = {},
    onEditMedia: (MediaItem) -> Unit = {},
    eraseExifOnShare: Boolean = true,
    autoplayVideosMuted: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (mediaList.isEmpty()) {
        LaunchedEffect(Unit) { onBackClick() }
        return
    }

    val safeInitialIndex = initialIndex.coerceIn(0, mediaList.size - 1)
    val pagerState = rememberPagerState(
        initialPage = safeInitialIndex,
        pageCount = { mediaList.size }
    )

    val safeIndex = pagerState.currentPage.coerceIn(0, mediaList.size - 1)
    val currentItem = mediaList.getOrNull(safeIndex) ?: mediaList.first()

    var uiVisible by remember { mutableStateOf(true) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var isFavourite by remember { mutableStateOf(false) }
    var currentExif by remember { mutableStateOf<ExifMetadata?>(null) }

    // Video playback states
    var isVideoPlaying by remember { mutableStateOf(true) }
    var isVideoMuted by remember { mutableStateOf(autoplayVideosMuted) }
    var videoSpeed by remember { mutableFloatStateOf(1.0f) }
    var videoCurrentPos by remember { mutableLongStateOf(0L) }
    var videoTotalDur by remember { mutableLongStateOf(0L) }
    var seekFraction by remember { mutableStateOf<Float?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Read EXIF metadata on page change
    LaunchedEffect(currentItem.id) {
        currentExif = ExifReader.extractMetadata(context, currentItem)
        isVideoPlaying = true
        seekFraction = null
    }

    BackHandler {
        onBackClick()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // Horizontal Pager for swiping between media
        HorizontalPager(
            state = pagerState,
            key = { index -> if (index in mediaList.indices) mediaList[index].id else index },
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val item = mediaList.getOrNull(pageIndex) ?: return@HorizontalPager

            if (item.isVideo) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            var verticalDragTotal = 0f
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (verticalDragTotal < -100f) {
                                        showInfoSheet = true
                                    } else if (verticalDragTotal > 150f) {
                                        onBackClick()
                                    }
                                    verticalDragTotal = 0f
                                },
                                onVerticalDrag = { _, dragAmount ->
                                    verticalDragTotal += dragAmount
                                }
                            )
                        }
                ) {
                    VideoPlayer(
                        uri = item.uri,
                        isMutedDefault = autoplayVideosMuted,
                        isPlayingState = isVideoPlaying,
                        isMutedState = isVideoMuted,
                        playbackSpeedState = videoSpeed,
                        seekToFraction = seekFraction,
                        onProgressUpdate = { cur, dur, playing ->
                            videoCurrentPos = cur
                            videoTotalDur = dur
                            isVideoPlaying = playing
                        },
                        onToggleControls = { uiVisible = !uiVisible }
                    )
                }
            } else {
                ZoomableImage(
                    uri = item.uri,
                    onSingleTap = { uiVisible = !uiVisible },
                    onSwipeUp = { showInfoSheet = true },
                    onDismissRequest = onBackClick
                )
            }
        }

        // Top App Bar Overlay (Matching Screenshot 3)
        AnimatedVisibility(
            visible = uiVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${safeIndex + 1} / ${mediaList.size}",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        currentExif?.formattedDate?.let { dateStr ->
                            Text(
                                text = dateStr,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(onClick = { showInfoSheet = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "Info",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Bottom Action Bar Overlay (Exact ColorOS Photos layout matching Screenshot 3)
        AnimatedVisibility(
            visible = uiVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (currentItem.isVideo) {
                        // 1. Video Scrubber Slider (Matching Screenshot 3)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatDuration(videoCurrentPos),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Slider(
                                value = if (videoTotalDur > 0) videoCurrentPos.toFloat() / videoTotalDur else 0f,
                                onValueChange = { fraction ->
                                    seekFraction = fraction
                                    videoCurrentPos = (fraction * videoTotalDur).toLong()
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = SoothingBlue,
                                    activeTrackColor = SoothingBlue,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )

                            Text(
                                text = formatDuration(videoTotalDur),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // 2. Secondary Video Quick Controls (Mute, -10s, +10s, Speed)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { isVideoMuted = !isVideoMuted },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isVideoMuted) Icons.Rounded.VolumeMute else Icons.Rounded.VolumeUp,
                                    contentDescription = "Mute toggle",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                IconButton(
                                    onClick = {
                                        val newPos = (videoCurrentPos - 10_000L).coerceAtLeast(0L)
                                        if (videoTotalDur > 0) seekFraction = newPos.toFloat() / videoTotalDur
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Replay10,
                                        contentDescription = "Rewind 10s",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val newPos = (videoCurrentPos + 10_000L).coerceAtMost(videoTotalDur)
                                        if (videoTotalDur > 0) seekFraction = newPos.toFloat() / videoTotalDur
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Forward10,
                                        contentDescription = "Forward 10s",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        videoSpeed = when (videoSpeed) {
                                            1.0f -> 1.5f
                                            1.5f -> 2.0f
                                            2.0f -> 0.5f
                                            else -> 1.0f
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${videoSpeed}x",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 3. Main Action Bar (Share, Favourite, Play/Pause/Edit, Delete, More)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Share
                        ViewerActionButton(
                            icon = Icons.Rounded.Share,
                            label = "Share",
                            onClick = { shareMedia(context, currentItem) }
                        )

                        // 2. Favourite
                        ViewerActionButton(
                            icon = if (isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            label = "Favourite",
                            tint = if (isFavourite) AccentGold else Color.White,
                            onClick = { isFavourite = !isFavourite }
                        )

                        // 3. Center Action: Circular Play/Pause (Video) or Edit (Photo)
                        if (currentItem.isVideo) {
                            Surface(
                                shape = CircleShape,
                                color = SoothingBlue,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .clickable { isVideoPlaying = !isVideoPlaying }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isVideoPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (isVideoPlaying) "Pause" else "Play",
                                        tint = PureBlack,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        } else {
                            ViewerActionButton(
                                icon = Icons.Rounded.Edit,
                                label = "Edit",
                                onClick = { onEditMedia(currentItem) }
                            )
                        }

                        // 4. Delete
                        ViewerActionButton(
                            icon = Icons.Rounded.Delete,
                            label = "Delete",
                            onClick = { onDeleteMedia(currentItem) }
                        )

                        // 5. More (Overflow Menu)
                        Box {
                            ViewerActionButton(
                                icon = Icons.Rounded.MoreVert,
                                label = "More",
                                onClick = { menuExpanded = true }
                            )

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.background(Color(0xFF1E1E1E))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Details", color = Color.White) },
                                    onClick = {
                                        menuExpanded = false
                                        showInfoSheet = true
                                    }
                                )

                                if (!currentItem.isVideo) {
                                    DropdownMenuItem(
                                        text = { Text("Set as Wallpaper", color = Color.White) },
                                        onClick = {
                                            menuExpanded = false
                                            setAsWallpaper(context, currentItem)
                                        }
                                    )
                                }

                                DropdownMenuItem(
                                    text = { Text("Open with...", color = Color.White) },
                                    onClick = {
                                        menuExpanded = false
                                        openWithExternalApp(context, currentItem)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Details Bottom Sheet
        if (showInfoSheet) {
            MediaInfoSheet(
                item = currentItem,
                exif = currentExif,
                onDismiss = { showInfoSheet = false }
            )
        }
    }
}

@Composable
private fun ViewerActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            color = tint.copy(alpha = 0.85f)
        )
    }
}

private fun formatDuration(millis: Long): String {
    if (millis <= 0L) return "0:00"
    val totalSec = millis / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%d:%02d", min, sec)
}

private fun shareMedia(context: Context, item: MediaItem) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = item.mimeType
        putExtra(Intent.EXTRA_STREAM, item.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share ${item.displayName}"))
}

private fun setAsWallpaper(context: Context, item: MediaItem) {
    val wallpaperIntent = Intent(Intent.ACTION_ATTACH_DATA).apply {
        setDataAndType(item.uri, item.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra("mimeType", item.mimeType)
    }
    try {
        context.startActivity(Intent.createChooser(wallpaperIntent, "Set as Wallpaper"))
    } catch (_: Exception) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            context.contentResolver.openInputStream(item.uri)?.use { stream ->
                wallpaperManager.setStream(stream)
            }
        } catch (_: Exception) {}
    }
}

private fun openWithExternalApp(context: Context, item: MediaItem) {
    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(item.uri, item.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    if (viewIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(viewIntent)
    }
}
