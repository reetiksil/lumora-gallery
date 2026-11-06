package com.lumora.gallery.ui.albums

import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Gif
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MotionPhotosOn
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lumora.gallery.data.model.Album
import com.lumora.gallery.ui.theme.DarkBorder
import com.lumora.gallery.ui.theme.DarkSurface
import com.lumora.gallery.ui.theme.DarkSurfaceElevated
import com.lumora.gallery.ui.theme.PureBlack
import com.lumora.gallery.ui.theme.SoftRed
import com.lumora.gallery.ui.theme.SoothingBlue
import com.lumora.gallery.ui.theme.TextPrimary
import com.lumora.gallery.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.io.File

data class AlbumSectionConfig(
    val id: String,
    val title: String,
    var isVisible: Boolean = true
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    viewModel: AlbumsViewModel,
    onAlbumClick: (Album) -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenPrivateVault: () -> Unit = {},
    onOpenCleanup: () -> Unit = {},
    onOpenRecentlyDeleted: () -> Unit = {},
    showCustomArrangeSheet: Boolean = false,
    onDismissCustomArrangeSheet: () -> Unit = {},
    showNewAlbumDialog: Boolean = false,
    onDismissNewAlbumDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedAlbumForActions by remember { mutableStateOf<Album?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    // Section ordering and visibility configuration (Matching Screenshot 4)
    val sections = remember {
        mutableStateListOf(
            AlbumSectionConfig("more_albums", "More albums", isVisible = true),
            AlbumSectionConfig("featured", "Featured", isVisible = false),
            AlbumSectionConfig("media_type", "Media type", isVisible = true),
            AlbumSectionConfig("more", "More", isVisible = true)
        )
    }

    var localShowCustomSheet by remember { mutableStateOf(false) }
    val effectiveShowCustom = showCustomArrangeSheet || localShowCustomSheet

    var localShowNewAlbumDialog by remember { mutableStateOf(false) }
    val effectiveShowNewAlbum = showNewAlbumDialog || localShowNewAlbumDialog
    var newAlbumName by remember { mutableStateOf("") }

    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SoothingBlue)
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
    ) {
        // --- Top Bar: "Albums", Search & Overflow ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Albums",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenSearch) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = TextPrimary
                        )
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Options",
                                tint = TextPrimary
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(DarkSurfaceElevated)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Custom arrangement", color = TextPrimary) },
                                onClick = {
                                    menuExpanded = false
                                    localShowCustomSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings", color = TextPrimary) },
                                onClick = {
                                    menuExpanded = false
                                    onOpenSettings()
                                }
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 1. PIN Section (Always on top)
        // ==========================================
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pin",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Adjust",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SoothingBlue,
                    modifier = Modifier.clickable { localShowCustomSheet = true }
                )
            }
        }

        item {
            val pinnedList = if (state.pinnedAlbums.isNotEmpty()) {
                state.pinnedAlbums
            } else {
                state.otherAlbums.take(5)
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(pinnedList, key = { it.bucketId }) { album ->
                    PinnedAlbumTile(
                        album = album,
                        onClick = { onAlbumClick(album) },
                        onLongClick = { selectedAlbumForActions = album }
                    )
                }
            }
        }

        // ==========================================
        // Dynamic Configured Sections (Customizable Order & Visibility)
        // ==========================================
        sections.forEach { sectionConfig ->
            if (sectionConfig.isVisible) {
                when (sectionConfig.id) {
                    "more_albums" -> {
                        if (state.otherAlbums.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp, bottom = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "More albums",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            item {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(bottom = 24.dp)
                                ) {
                                    items(state.otherAlbums, key = { it.bucketId }) { album ->
                                        MoreAlbumTile(
                                            album = album,
                                            onClick = { onAlbumClick(album) },
                                            onLongClick = { selectedAlbumForActions = album }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "featured" -> {
                        item {
                            Text(
                                text = "Featured",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FeaturedCategoryCard(
                                    title = "People & pets",
                                    icon = Icons.Rounded.People,
                                    modifier = Modifier.weight(1f)
                                )
                                FeaturedCategoryCard(
                                    title = "Smart categories",
                                    icon = Icons.Rounded.CollectionsBookmark,
                                    modifier = Modifier.weight(1f)
                                )
                                FeaturedCategoryCard(
                                    title = "Memories",
                                    icon = Icons.Rounded.AutoAwesome,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    "media_type" -> {
                        item {
                            Text(
                                text = "Media type",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(DarkSurfaceElevated)
                            ) {
                                MediaTypeListItem(
                                    icon = Icons.Rounded.Gif,
                                    title = "GIF",
                                    count = "27",
                                    onClick = { }
                                )
                                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                MediaTypeListItem(
                                    icon = Icons.Rounded.SlowMotionVideo,
                                    title = "Slo-mo",
                                    count = "1",
                                    onClick = { }
                                )
                                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                MediaTypeListItem(
                                    icon = Icons.Rounded.VideoLibrary,
                                    title = "Videos",
                                    count = "63",
                                    onClick = { }
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    "more" -> {
                        item {
                            Text(
                                text = "More",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(DarkSurfaceElevated)
                            ) {
                                MoreListItem(
                                    icon = Icons.Rounded.Lock,
                                    title = "Private",
                                    hasTrailingArrow = true,
                                    onClick = onOpenPrivateVault
                                )
                                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                MoreListItem(
                                    icon = Icons.Rounded.CleaningServices,
                                    title = "Cleanup suggestions",
                                    hasRedBadge = true,
                                    hasTrailingArrow = true,
                                    onClick = onOpenCleanup
                                )
                                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                MoreListItem(
                                    icon = Icons.Rounded.Delete,
                                    title = "Recently deleted",
                                    hasTrailingArrow = true,
                                    onClick = onOpenRecentlyDeleted
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ========================================================
    // Custom Arrange & Hide Albums Bottom Sheet (Screenshot 4)
    // ========================================================
    if (effectiveShowCustom) {
        ModalBottomSheet(
            onDismissRequest = {
                localShowCustomSheet = false
                onDismissCustomArrangeSheet()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = DarkSurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Top Action Bar: Cancel | Custom | Done
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cancel",
                        color = SoothingBlue,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            localShowCustomSheet = false
                            onDismissCustomArrangeSheet()
                        }
                    )

                    Text(
                        text = "Custom",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Done",
                        color = SoothingBlue,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            localShowCustomSheet = false
                            onDismissCustomArrangeSheet()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Sort albums by custom order or hide albums",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // List of section controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface)
                ) {
                    sections.forEachIndexed { index, section ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Menu,
                                    contentDescription = "Reorder",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = section.title,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Checkbox(
                                checked = section.isVisible,
                                onCheckedChange = { checked ->
                                    sections[index] = section.copy(isVisible = checked)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = SoothingBlue,
                                    checkmarkColor = Color.White,
                                    uncheckedColor = TextSecondary
                                )
                            )
                        }

                        if (index < sections.size - 1) {
                            HorizontalDivider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // New Album Dialog (triggered by + button on bottom bar)
    if (effectiveShowNewAlbum) {
        AlertDialog(
            onDismissRequest = {
                localShowNewAlbumDialog = false
                onDismissNewAlbumDialog()
            },
            title = { Text("Create New Album", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter album name:", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newAlbumName,
                        onValueChange = { newAlbumName = it },
                        placeholder = { Text("e.g. Vacation 2026", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = SoothingBlue,
                            unfocusedBorderColor = DarkBorder
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newAlbumName.isNotBlank()) {
                        try {
                            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                            val newDir = File(picturesDir, newAlbumName.trim())
                            if (!newDir.exists()) newDir.mkdirs()
                            Toast.makeText(context, "Album folder created", Toast.LENGTH_SHORT).show()
                            viewModel.loadAlbums()
                        } catch (_: Exception) {}
                    }
                    newAlbumName = ""
                    localShowNewAlbumDialog = false
                    onDismissNewAlbumDialog()
                }) {
                    Text("Create", color = SoothingBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    newAlbumName = ""
                    localShowNewAlbumDialog = false
                    onDismissNewAlbumDialog()
                }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    // Long-press Context Action Sheet
    selectedAlbumForActions?.let { album ->
        val isShown = state.selectedInPhotosIds.contains(album.bucketId)
        val isPinned = state.pinnedAlbums.any { it.bucketId == album.bucketId }

        AlbumActionBottomSheet(
            album = album,
            isShownInPhotos = isShown,
            isPinned = isPinned,
            onToggleShowInPhotos = {
                viewModel.toggleShowInPhotos(album.bucketId)
                selectedAlbumForActions = null
            },
            onTogglePin = {
                viewModel.togglePinAlbum(album.bucketId)
                selectedAlbumForActions = null
            },
            onDismiss = { selectedAlbumForActions = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PinnedAlbumTile(
    album: Album,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(136.dp)
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(
            modifier = Modifier
                .size(136.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface)
        ) {
            AsyncImage(
                model = album.coverUri,
                contentDescription = album.bucketName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = album.bucketName,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            text = "${album.itemCount}",
            color = TextSecondary,
            fontSize = 12.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MoreAlbumTile(
    album: Album,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(112.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurface)
        ) {
            AsyncImage(
                model = album.coverUri,
                contentDescription = album.bucketName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = album.bucketName,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Text(
            text = "${album.itemCount}",
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun FeaturedCategoryCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurfaceElevated)
            .clickable { }
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SoothingBlue,
            modifier = Modifier.size(26.dp)
        )
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MediaTypeListItem(
    icon: ImageVector,
    title: String,
    count: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SoothingBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = count,
                color = TextSecondary,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MoreListItem(
    icon: ImageVector,
    title: String,
    hasRedBadge: Boolean = false,
    hasTrailingArrow: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (hasRedBadge) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SoftRed)
                )
            }
        }
        if (hasTrailingArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumActionBottomSheet(
    album: Album,
    isShownInPhotos: Boolean,
    isPinned: Boolean,
    onToggleShowInPhotos: () -> Unit,
    onTogglePin: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSurfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = album.bucketName,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${album.itemCount} items",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )

            HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleShowInPhotos)
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isShownInPhotos) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (isShownInPhotos) "Hide from Photos tab" else "Show in Photos tab",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isShownInPhotos) "Keep this album organized in Albums only" else "Include this album's media in your main timeline",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTogglePin)
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.PushPin,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = if (isPinned) "Unpin album" else "Pin to top",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
