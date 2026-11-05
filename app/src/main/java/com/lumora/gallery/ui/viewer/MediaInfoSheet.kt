package com.lumora.gallery.ui.viewer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.ShutterSpeed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumora.gallery.data.model.ExifMetadata
import com.lumora.gallery.data.model.MediaItem
import com.lumora.gallery.ui.theme.AccentGold
import com.lumora.gallery.ui.theme.DarkSurface
import com.lumora.gallery.ui.theme.TextPrimary
import com.lumora.gallery.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaInfoSheet(
    item: MediaItem,
    exif: ExifMetadata?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            // Title Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Details",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // File Name & Folder
            InfoRow(
                icon = Icons.Rounded.Description,
                title = item.displayName,
                subtitle = "Album: ${item.bucketName.ifBlank { "Unknown" }} • ${item.mimeType}"
            )

            // Date & Time
            exif?.formattedDate?.let { dateStr ->
                Spacer(modifier = Modifier.height(14.dp))
                InfoRow(
                    icon = Icons.Rounded.CalendarToday,
                    title = dateStr,
                    subtitle = "Date captured"
                )
            }

            // Resolution & Size
            val dimensionsSummary = listOfNotNull(
                exif?.dimensions ?: if (item.width > 0) "${item.width} × ${item.height}" else null,
                exif?.megapixels,
                exif?.fileSize
            ).joinToString("  •  ")

            if (dimensionsSummary.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                InfoRow(
                    icon = Icons.Rounded.Image,
                    title = dimensionsSummary,
                    subtitle = if (item.isVideo) "Video specifications" else "Image resolution & size"
                )
            }

            // Camera Device
            exif?.deviceDisplayName?.let { device ->
                Spacer(modifier = Modifier.height(14.dp))
                InfoRow(
                    icon = Icons.Rounded.CameraAlt,
                    title = device,
                    subtitle = "Camera hardware"
                )
            }

            // EXIF Camera Shot Summary (Aperture, Exposure, Focal Length, ISO)
            exif?.cameraShotSummary?.let { shotParams ->
                Spacer(modifier = Modifier.height(14.dp))
                InfoRow(
                    icon = Icons.Rounded.ShutterSpeed,
                    title = shotParams,
                    subtitle = exif.flash ?: "Shot parameters"
                )
            }

            // GPS Coordinates & Map Link
            if (exif?.hasGpsCoordinates == true) {
                Spacer(modifier = Modifier.height(14.dp))
                InfoRow(
                    icon = Icons.Rounded.LocationOn,
                    title = String.format("%.4f°, %.4f°", exif.latitude, exif.longitude),
                    subtitle = "GPS Location"
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val geoUri = Uri.parse("geo:${exif.latitude},${exif.longitude}?q=${exif.latitude},${exif.longitude}(${Uri.encode(item.displayName)})")
                        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(mapIntent)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGold),
                    modifier = Modifier.padding(start = 34.dp)
                ) {
                    Text("View on Map", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
    }
}
