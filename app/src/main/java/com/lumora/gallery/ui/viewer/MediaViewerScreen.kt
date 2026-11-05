package com.lumora.gallery.ui.viewer

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumora.gallery.data.model.ExifMetadata
import com.lumora.gallery.data.model.MediaItem
import com.lumora.gallery.data.util.ExifReader
import com.lumora.gallery.ui.theme.AccentGold
import com.lumora.gallery.ui.theme.PureBlack
import kotlinx.coroutines.launch

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
    val pagerState = rememberPagerState(initialPage = safeInitialIndex, pageCount = { mediaList.size })
    val currentItem = mediaList[pagerState.currentPage]

    var uiVisible by remember { mutableStateOf(true) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var isFavourite by remember { mutableStateOf(false) }
    var currentExif by remember { mutableStateOf<ExifMetadata?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Read EXIF when swiping to new item
    LaunchedEffect(currentItem.id) {
        currentExif = ExifReader.extractMetadata(context, currentItem)
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
            key = { index -> mediaList[index].id },
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val item = mediaList[pageIndex]

            if (item.isVideo) {
                VideoPlayer(
                    uri = item.uri,
                    isMutedDefault = autoplayVideosMuted,
                    controlsVisible = uiVisible,
                    onToggleControls = { uiVisible = !uiVisible }
                )
            } else {
                ZoomableImage(
                    uri = item.uri,
                    onSingleTap = { uiVisible = !uiVisible },
                    onDismissRequest = onBackClick
                )
            }
        }

        // Top App Bar Overlay
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
                            colors = listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
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
                            text = "${pagerState.currentPage + 1} / ${mediaList.size}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        currentExif?.formattedDate?.let { dateStr ->
                            Text(
                                text = dateStr,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
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

        // Bottom Action Bar Overlay
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
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share Button
                    ViewerActionButton(
                        icon = Icons.Rounded.Share,
                        label = "Share",
                        onClick = {
                            shareMedia(context, currentItem)
                        }
                    )

                    // Edit Button (Hook for Milestone d)
                    ViewerActionButton(
                        icon = Icons.Rounded.Edit,
                        label = "Edit",
                        onClick = { onEditMedia(currentItem) }
                    )

                    // Favourite Button
                    ViewerActionButton(
                        icon = if (isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        label = "Favourite",
                        tint = if (isFavourite) AccentGold else Color.White,
                        onClick = { isFavourite = !isFavourite }
                    )

                    // Delete Button
                    ViewerActionButton(
                        icon = Icons.Rounded.Delete,
                        label = "Delete",
                        onClick = { onDeleteMedia(currentItem) }
                    )

                    // Overflow Menu
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
        // Fallback to WallpaperManager
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

