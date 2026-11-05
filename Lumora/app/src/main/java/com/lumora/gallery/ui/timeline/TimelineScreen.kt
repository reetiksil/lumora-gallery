package com.lumora.gallery.ui.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.Scaffold
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
import com.lumora.gallery.ui.navigation.FloatingNavBar
import com.lumora.gallery.ui.navigation.NavTab
import com.lumora.gallery.ui.onboarding.AlbumSelectionSheet
import com.lumora.gallery.ui.theme.AccentGold
import com.lumora.gallery.ui.theme.BadgeBackground
import com.lumora.gallery.ui.theme.DarkSurface
import com.lumora.gallery.ui.theme.PureBlack
import com.lumora.gallery.ui.theme.TextPrimary
import com.lumora.gallery.ui.theme.TextSecondary

@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()
    val allAlbums by viewModel.allAlbums.collectAsState()
    val selectedBucketIds by viewModel.selectedBucketIds.collectAsState()
    val showAlbumPicker by viewModel.showAlbumPicker.collectAsState()

    var currentTab by remember { mutableStateOf(NavTab.PHOTOS) }
    var menuExpanded by remember { mutableStateOf(false) }

    // Pinch-to-zoom gesture tracking
    var scaleZoom by remember { mutableFloatStateOf(1f) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    scaleZoom *= zoom
                    if (scaleZoom > 1.3f) {
                        // Zoom in -> fewer columns (bigger items)
                        if (gridColumns > 2) {
                            viewModel.setGridColumns(gridColumns - 1)
                        }
                        scaleZoom = 1f
                    } else if (scaleZoom < 0.75f) {
                        // Zoom out -> more columns (smaller items)
                        if (gridColumns < 5) {
                            viewModel.setGridColumns(gridColumns + 1)
                        }
                        scaleZoom = 1f
                    }
                }
            },
        containerColor = PureBlack,
        topBar = {
            TimelineTopBar(
                onSearchClick = { /* Search will be connected in milestone e */ },
                onMenuClick = { menuExpanded = true },
                menuExpanded = menuExpanded,
                onDismissMenu = { menuExpanded = false },
                onSelectAlbumsClick = {
                    menuExpanded = false
                    viewModel.openAlbumPicker()
                }
            )
        },
        bottomBar = {
            FloatingNavBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                onToggleGridDensity = { viewModel.cycleGridColumns() },
                onOpenFilter = { viewModel.openAlbumPicker() }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is TimelineUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentGold)
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
                                text = "Check that your selected albums have photos or videos, or pick more albums.",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                is TimelineUiState.Success -> {
                    MediaGrid(
                        groupedMedia = state.groupedMedia,
                        columns = gridColumns,
                        onItemClick = { /* Viewer opens in milestone c */ }
                    )
                }
            }
        }

        // Onboarding / Album selection sheet
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
    onSelectAlbumsClick: () -> Unit
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
                    modifier = Modifier.background(DarkSurface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Photos tab sources", color = TextPrimary) },
                        onClick = onSelectAlbumsClick
                    )
                }
            }
        }
    }
}

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
            // Sticky date header spanning all columns
            item(span = { GridItemSpan(columns) }) {
                Text(
                    text = header,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            // Grid items for this date section
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

        // Video duration badge
        if (item.isVideo && item.formattedDuration.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(BadgeBackground)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = item.formattedDuration,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
