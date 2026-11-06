package com.lumora.gallery.ui.settings

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumora.gallery.data.model.SortOrder
import com.lumora.gallery.data.model.ThemeMode
import com.lumora.gallery.ui.theme.DarkBorder
import com.lumora.gallery.ui.theme.DarkSurfaceElevated
import com.lumora.gallery.ui.theme.PureBlack
import com.lumora.gallery.ui.theme.SoothingBlue
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
    val context = LocalContext.current
    var showThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // --- Top Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
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
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ==========================================
            // Group 1: Backup (Matching Screenshot 3)
            // ==========================================
            item {
                SettingsSection(title = "Backup") {
                    SettingsRowAction(
                        icon = Icons.Rounded.CloudUpload,
                        title = "Back up to Google Photos",
                        subtitle = "Opens system backup settings (No cloud SDK inside Lumora)",
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_SYNC_SETTINGS)
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    )
                }
            }

            // ==========================================
            // Group 2: Appearance & Theme
            // ==========================================
            item {
                SettingsSection(title = "Appearance") {
                    SettingsRowAction(
                        icon = Icons.Rounded.Palette,
                        title = "Theme",
                        subtitle = state.themeMode.displayName,
                        onClick = { showThemeDialog = true }
                    )
                }
            }

            // ==========================================
            // Group 3: Browsing
            // ==========================================
            item {
                SettingsSection(title = "Browsing") {
                    SettingsRowAction(
                        icon = Icons.Rounded.PhotoLibrary,
                        title = "Photos tab sources",
                        subtitle = "${state.selectedBucketIds.size} albums included in timeline",
                        onClick = onOpenPhotosSources
                    )
                    SettingsDivider()
                    SettingsRowAction(
                        icon = Icons.Rounded.Sort,
                        title = "Sort",
                        subtitle = state.sortOrder.displayName,
                        onClick = {
                            val next = when (state.sortOrder) {
                                SortOrder.DATE_TAKEN_DESC -> SortOrder.DATE_TAKEN_ASC
                                SortOrder.DATE_TAKEN_ASC -> SortOrder.DISPLAY_NAME_ASC
                                SortOrder.DISPLAY_NAME_ASC -> SortOrder.DATE_MODIFIED_DESC
                                SortOrder.DATE_MODIFIED_DESC -> SortOrder.DATE_TAKEN_DESC
                            }
                            viewModel.setSortOrder(next)
                        }
                    )
                    SettingsDivider()
                    SettingsRowToggle(
                        title = "Autoplay videos without sound",
                        subtitle = "Play video previews muted in viewer",
                        checked = state.autoplayMuted,
                        onCheckedChange = { viewModel.toggleAutoplayMuted(it) }
                    )
                }
            }

            // ==========================================
            // Group 3: Sharing & Privacy
            // ==========================================
            item {
                SettingsSection(title = "Sharing") {
                    SettingsRowToggle(
                        title = "Erase personal information",
                        subtitle = "Automatically strip EXIF GPS location when sharing",
                        checked = state.eraseExifOnShare,
                        onCheckedChange = { viewModel.toggleEraseExif(it) }
                    )
                }
            }

            // ==========================================
            // Group 4: Storage & Cache (Hard 100MB Cap)
            // ==========================================
            item {
                SettingsSection(title = "Storage & Cache") {
                    SettingsRowAction(
                        icon = Icons.Rounded.Storage,
                        title = "Thumbnail Cache",
                        subtitle = "${state.currentCacheFormatted} used (Cap: ${state.diskCacheLimitMb} MB)",
                        onClick = { /* Info */ }
                    )
                    SettingsDivider()
                    SettingsRowAction(
                        icon = Icons.Rounded.Delete,
                        title = "Clear thumbnail cache",
                        subtitle = "Safely deletes LRU disk cache; never loses original photos",
                        trailingText = "Clear",
                        onClick = { viewModel.clearCache() }
                    )
                }
            }

            // ==========================================
            // Group 5: More
            // ==========================================
            item {
                SettingsSection(title = "More") {
                    SettingsRowAction(
                        icon = Icons.Rounded.Lock,
                        title = "Show Private album",
                        subtitle = "Show Biometric Vault in Albums tab",
                        onClick = { /* Toggle */ }
                    )
                    SettingsDivider()
                    SettingsRowAction(
                        icon = Icons.Rounded.Info,
                        title = "About Lumora",
                        subtitle = "Version 0.5.0 • Fully offline & private",
                        onClick = { /* About */ }
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.themeMode == mode,
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = SoothingBlue,
                                    unselectedColor = TextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = mode.displayName,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = if (state.themeMode == mode) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close", color = SoothingBlue)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurfaceElevated)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = DarkBorder,
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun SettingsRowAction(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SoothingBlue
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
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
private fun SettingsRowToggle(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SoothingBlue,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = DarkBorder
            )
        )
    }
}
