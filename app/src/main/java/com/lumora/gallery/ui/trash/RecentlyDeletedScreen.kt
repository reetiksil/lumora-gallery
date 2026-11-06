package com.lumora.gallery.ui.trash

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lumora.gallery.LumoraApplication
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

data class TrashedItem(
    val file: File,
    val displayName: String,
    val deletedTimestamp: Long = System.currentTimeMillis()
) {
    val daysRemaining: Int
        get() {
            val elapsedMs = System.currentTimeMillis() - deletedTimestamp
            val remainingDays = 30 - (elapsedMs / (1000L * 60 * 60 * 24)).toInt()
            return remainingDays.coerceAtLeast(0)
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentlyDeletedScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as LumoraApplication
    val trashRepo = app.trashRepository
    val scope = rememberCoroutineScope()

    val trashedItems = remember { mutableStateListOf<TrashedItem>() }
    var isLoading by remember { mutableStateOf(true) }
    var selectedItemForAction by remember { mutableStateOf<TrashedItem?>(null) }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }

    fun refreshTrash() {
        scope.launch {
            isLoading = true
            val items = trashRepo.getTrashedItems()
            trashedItems.clear()
            trashedItems.addAll(items)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshTrash()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // --- Top App Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Text(
                    text = "Recently Deleted",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            if (trashedItems.isNotEmpty()) {
                TextButton(onClick = { showEmptyTrashDialog = true }) {
                    Text("Empty Trash", color = SoftRed, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SoothingBlue)
            }
        } else if (trashedItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Recently deleted is empty",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Photos and videos deleted in Lumora are safely retained here for 30 days before being permanently removed.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        } else {
            Text(
                text = "${trashedItems.size} items • Automatically purged after 30 days",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(trashedItems, key = { it.file.absolutePath }) { item ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(DarkSurface)
                            .clickable { selectedItemForAction = item }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(item.file)
                                .crossfade(true)
                                .build(),
                            contentDescription = item.displayName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        Text(
                            text = "${item.daysRemaining}d",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }

    // Action Bottom Sheet for selected item
    selectedItemForAction?.let { item ->
        ModalBottomSheet(
            onDismissRequest = { selectedItemForAction = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = DarkSurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item.file)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = item.displayName,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${item.daysRemaining} days remaining in vault",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                val success = trashRepo.restoreItem(item)
                                if (success) {
                                    Toast.makeText(context, "Restored to gallery", Toast.LENGTH_SHORT).show()
                                    selectedItemForAction = null
                                    refreshTrash()
                                } else {
                                    Toast.makeText(context, "Failed to restore", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SoothingBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore")
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                trashRepo.deletePermanently(item)
                                Toast.makeText(context, "Deleted permanently", Toast.LENGTH_SHORT).show()
                                selectedItemForAction = null
                                refreshTrash()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Forever")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Empty Trash confirmation dialog
    if (showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text("Empty Recently Deleted?", color = TextPrimary) },
            text = { Text("All photos and videos in Recently Deleted will be permanently destroyed. This action cannot be undone.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showEmptyTrashDialog = false
                    scope.launch {
                        trashRepo.emptyTrash()
                        refreshTrash()
                    }
                }) {
                    Text("Delete Forever", color = SoftRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }
}
