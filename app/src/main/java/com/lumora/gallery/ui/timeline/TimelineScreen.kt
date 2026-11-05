package com.lumora.gallery.ui.timeline

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lumora.gallery.data.model.MediaItem
import com.lumora.gallery.ui.navigation.TimeScale
import com.lumora.gallery.ui.onboarding.AlbumSelectionSheet
import com.lumora.gallery.ui.theme.BadgeBackground
import com.lumora.gallery.ui.theme.DarkBorder
import com.lumora.gallery.ui.theme.DarkSurface
import com.lumora.gallery.ui.theme.DarkSurfaceElevated
import com.lumora.gallery.ui.theme.PureBlack
import com.lumora.gallery.ui.theme.SoothingBlue
import com.lumora.gallery.ui.theme.TextPrimary
import com.lumora.gallery.ui.theme.TextSecondary

@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    onOpenSettings: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onItemClick: (MediaItem, List<MediaItem>) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()
    val allAlbums by viewModel.allAlbums.collectAsState()
    val selectedBucketIds by viewModel.selectedBucketIds.collectAsState()
    val showAlbumPicker by viewModel.showAlbumPicker.collectAsState()

    var menuExpanded by remember { mutableStateOf(false) }
    var scaleZoom by remember { mutableFloatStateOf(1f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    scaleZoom *= zoom
                    if (scaleZoom > 1.3f) {
                        if (gridColumns > 2) {
                            viewModel.setGridColumns(gridColumns - 1)
                        }
                        scaleZoom = 1f
                    } else if (scaleZoom < 0.75f) {
                        if (gridColumns < 5) {
                            viewModel.setGridColumns(gridColumns + 1)
                        }
                        scaleZoom = 1f
                    }
                }
            }
    ) {
        TimelineTopBar(
            onSearchClick = onOpenSearch,
            onMenuClick = { menuExpanded = true },
            menuExpanded = menuExpanded,
            onDismissMenu = { menuExpanded = false },
            onSelectAlbumsClick = {
                menuExpanded = false
                viewModel.openAlbumPicker()
            },
            onSettingsClick = {
                menuExpanded = false
                onOpenSettings()
            }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is TimelineUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SoothingBlue)
                    }
                }

                is TimelineUiState.Empty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No media to show",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Check that your selected albums have photos or videos, or pick more albums in Settings.",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                is TimelineUiState.Success -> {
                    val allItems = remember(state.groupedMedia) {
                        state.groupedMedia.values.flatten()
                    }

                    AnimatedContent(
                        targetState = state.timeScale,
                        transitionSpec = {
                            fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                                    fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        },
                        label = "timeline_scale_transition"
                    ) { currentScale ->
                        when (currentScale) {
                            TimeScale.YEARS -> {
                                YearsView(
                                    groupedMedia = state.groupedMedia,
                                    onItemClick = { clickedItem ->
                                        onItemClick(clickedItem, allItems)
                                    }
                                )
                            }
                            TimeScale.MONTHS -> {
                                MonthsView(
                                    groupedMedia = state.groupedMedia,
                                    onItemClick = { clickedItem ->
                                        onItemClick(clickedItem, allItems)
                                    }
                                )
                            }
                            TimeScale.DAYS -> {
                                MediaGrid(
                                    groupedMedia = state.groupedMedia,
                                    columns = gridColumns,
                                    onItemClick = { clickedItem ->
                                        onItemClick(clickedItem, allItems)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAlbumPicker) {
            AlbumSelectionSheet(
                albums = allAlbums,
                initialSelectedBucketIds = selectedBucketIds,
                onConfirm = { chosenIds ->
                    viewModel.saveSelectedAlbums(chosenIds)
                },
                onDismiss = { viewModel.closeAlbumPicker() }
            )
        }
    }
}

@Composable
private fun TimelineTopBar(
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit,
    menuExpanded: Boolean,
    onDismissMenu: () -> Unit,
    onSelectAlbumsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Photos",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = TextPrimary
                )
            }

            Box {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More options",
                        tint = TextPrimary
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = onDismissMenu,
                    modifier = Modifier.background(DarkSurfaceElevated)
                ) {
                    DropdownMenuItem(
                        text = { Text("Photos tab sources", color = TextPrimary) },
                        onClick = onSelectAlbumsClick
                    )
                    DropdownMenuItem(
                        text = { Text("Settings", color = TextPrimary) },
                        onClick = onSettingsClick
                    )
                }
            }
        }
    }
}

/**
 * Years View (Matching Screenshot 4)
 * Displays large cover hero cards with big year title (e.g. "2025") overlaid.
 */
@Composable
private fun YearsView(
    groupedMedia: Map<String, List<MediaItem>>,
    onItemClick: (MediaItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        groupedMedia.forEach { (year, items) ->
            val heroItem = items.firstOrNull()
            if (heroItem != null) {
                item(key = "year_$year") {
                    YearCoverCard(
                        year = year,
                        heroItem = heroItem,
                        photoCount = items.size,
                        onClick = { onItemClick(heroItem) }
                    )
                }
            }
        }
    }
}

@Composable
private fun YearCoverCard(
    year: String,
    heroItem: MediaItem,
    photoCount: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(DarkSurfaceElevated)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(heroItem.uri)
                .crossfade(true)
                .build(),
            contentDescription = year,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay for contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        // Year Title at top-left (e.g. "2025" like Screenshot 4)
        Text(
            text = year,
            color = Color.White,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
        )

        // Photo count badge at bottom-right
        Text(
            text = "$photoCount items",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BadgeBackground)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/**
 * Months View
 * 2-column grid of month cover cards (e.g. "November 2026").
 */
@Composable
private fun MonthsView(
    groupedMedia: Map<String, List<MediaItem>>,
    onItemClick: (MediaItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        groupedMedia.forEach { (monthName, items) ->
            val heroItem = items.firstOrNull()
            if (heroItem != null) {
                item(key = "month_$monthName") {
                    MonthCoverCard(
                        monthName = monthName,
                        heroItem = heroItem,
                        photoCount = items.size,
                        onClick = { onItemClick(heroItem) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthCoverCard(
    monthName: String,
    heroItem: MediaItem,
    photoCount: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurfaceElevated)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(DarkSurface)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(heroItem.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = monthName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = monthName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$photoCount photos",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

/**
 * Days View: Standard date-grouped timeline (Matching Screenshot 2)
 */
@Composable
private fun MediaGrid(
    groupedMedia: Map<String, List<MediaItem>>,
    columns: Int,
    onItemClick: (MediaItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        groupedMedia.forEach { (header, items) ->
            item(span = { GridItemSpan(columns) }) {
                Text(
                    text = header,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }

            items(
                items = items,
                key = { it.id }
            ) { item ->
                MediaGridItem(
                    item = item,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
private fun MediaGridItem(
    item: MediaItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(DarkSurface)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = item.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (item.isVideo && item.formattedDuration.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(BadgeBackground)
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.size(2.dp))
                Text(
                    text = item.formattedDuration,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
