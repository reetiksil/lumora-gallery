package com.lumora.gallery.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lumora.gallery.data.preferences.PreferencesRepository
import com.lumora.gallery.ui.theme.AccentGold
import com.lumora.gallery.ui.theme.DarkBorder
import com.lumora.gallery.ui.theme.DarkSurface
import com.lumora.gallery.ui.theme.DarkSurfaceElevated
import com.lumora.gallery.ui.theme.PureBlack
import com.lumora.gallery.ui.theme.TextPrimary
import com.lumora.gallery.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun PhotosSourcesScreen(
    viewModel: SettingsViewModel,
    preferencesRepository: PreferencesRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var selectedIds by remember(state.selectedBucketIds) { mutableStateOf(state.selectedBucketIds) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Photos Tab Sources",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "Select which folders appear in your main Photos timeline. Hidden folders remain accessible in the Albums tab.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Include new albums automatically", fontSize = 14.sp, color = TextPrimary)
                        Text("When a new camera or app folder is created", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = state.includeNewAlbums,
                        onCheckedChange = { viewModel.setIncludeNewAlbums(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = PureBlack, checkedTrackColor = AccentGold)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        val all = state.allAlbums.map { it.bucketId }.toSet()
                        selectedIds = all
                        CoroutineScope(Dispatchers.IO).launch { preferencesRepository.setSelectedBucketIds(all) }
                    }) {
                        Text("Select all", color = AccentGold, fontSize = 13.sp)
                    }
                    TextButton(onClick = {
                        selectedIds = emptySet()
                        CoroutineScope(Dispatchers.IO).launch { preferencesRepository.setSelectedBucketIds(emptySet()) }
                    }) {
                        Text("Clear", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }

            items(state.allAlbums, key = { it.bucketId }) { album ->
                val isChecked = selectedIds.contains(album.bucketId)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                        .clickable {
                            val updated = if (isChecked) selectedIds - album.bucketId else selectedIds + album.bucketId
                            selectedIds = updated
                            CoroutineScope(Dispatchers.IO).launch { preferencesRepository.setSelectedBucketIds(updated) }
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (album.coverUri != null) {
                            AsyncImage(
                                model = album.coverUri,
                                contentDescription = null,
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(imageVector = Icons.Rounded.Folder, contentDescription = null, tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = album.bucketName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            if (album.isCamera) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("(Camera)", fontSize = 11.sp, color = AccentGold)
                            }
                        }
                        Text(text = "${album.itemCount} items", fontSize = 12.sp, color = TextSecondary)
                    }

                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { checked ->
                            val updated = if (checked) selectedIds + album.bucketId else selectedIds - album.bucketId
                            selectedIds = updated
                            CoroutineScope(Dispatchers.IO).launch { preferencesRepository.setSelectedBucketIds(updated) }
                        },
                        colors = CheckboxDefaults.colors(checkedColor = AccentGold, checkmarkColor = PureBlack, uncheckedColor = DarkBorder)
                    )
                }
            }
        }
    }
}
