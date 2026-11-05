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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumora.gallery.data.model.SortOrder
import com.lumora.gallery.data.model.ThemeMode
import com.lumora.gallery.ui.theme.AccentGold
import com.lumora.gallery.ui.theme.DarkBorder
import com.lumora.gallery.ui.theme.DarkSurface
import com.lumora.gallery.ui.theme.DarkSurfaceElevated
import com.lumora.gallery.ui.theme.PureBlack
import com.lumora.gallery.ui.theme.TextPrimary
import com.lumora.gallery.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onOpenPhotosSources: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

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
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section: Photos Tab Sources
            item {
                SectionHeader("Photos Tab")
                SettingNavigationItem(
                    icon = Icons.Rounded.FolderSpecial,
                    title = "Photos tab sources",
                    subtitle = "${state.selectedBucketIds.size} albums selected",
                    onClick = onOpenPhotosSources
                )
            }

            // Section: Storage & Cache (Hard Cap Guarantee)
            item {
                SectionHeader("Storage & Cache")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceElevated)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Disk Cache Usage", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
                            Text("Current: ${state.currentCacheFormatted} / Cap: ${state.diskCacheLimitMb} MB", fontSize = 13.sp, color = TextSecondary)
                        }
                        Button(
                            onClick = { viewModel.clearCache() },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = PureBlack),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Clear cache", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Cache Cap Limit", fontSize = 13.sp, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(50, 100, 250).forEach { mb ->
                            val isSelected = state.diskCacheLimitMb == mb
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) AccentGold else DarkSurface)
                                    .clickable { viewModel.setCacheLimitMb(mb) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$mb MB",
                                    color = if (isSelected) PureBlack else TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Guarantee: Lumora strictly evicts old thumbnails via LRU. It never creates duplicate databases or runs background AI clustering.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }

            // Section: Sorting & Display
            item {
                SectionHeader("Sorting & Playback")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceElevated)
                        .padding(16.dp)
                ) {
                    Text("Timeline Sort Order", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    SortOrder.values().forEach { order ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setSortOrder(order) }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(order.displayName, fontSize = 14.sp, color = if (state.sortOrder == order) AccentGold else TextSecondary)
                            if (state.sortOrder == order) {
                                Icon(imageVector = Icons.Rounded.Check, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Autoplay videos muted", fontSize = 14.sp, color = TextPrimary)
                            Text("Videos in timeline and viewer start muted", fontSize = 12.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = state.autoplayMuted,
                            onCheckedChange = { viewModel.toggleAutoplayMuted(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = PureBlack, checkedTrackColor = AccentGold)
                        )
                    }
                }
            }

            // Section: Privacy
            item {
                SectionHeader("Privacy")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceElevated)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Erase personal info on share", fontSize = 14.sp, color = TextPrimary)
                            Text("Strip EXIF metadata and GPS location before sharing", fontSize = 12.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = state.eraseExifOnShare,
                            onCheckedChange = { viewModel.toggleEraseExif(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = PureBlack, checkedTrackColor = AccentGold)
                        )
                    }
                }
            }

            // Section: About
            item {
                SectionHeader("About")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceElevated)
                        .padding(16.dp)
                ) {
                    Text("Lumora Gallery", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    Text("Version 0.2.0-milestone-b", fontSize = 13.sp, color = AccentGold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Zero trackers. Zero ads. No internet permission. Pure MediaStore speed.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = AccentGold,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingNavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceElevated)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(DarkSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Text(text = subtitle, fontSize = 12.sp, color = TextSecondary)
        }

        Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null, tint = TextSecondary)
    }
}
