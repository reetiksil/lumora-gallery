package com.lumora.gallery.data.util

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.lumora.gallery.data.model.ExifMetadata
import com.lumora.gallery.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object ExifReader {

    suspend fun extractMetadata(context: Context, item: MediaItem): ExifMetadata = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        val formattedDate = if (item.dateTakenMs > 0) {
            dateFormat.format(Date(item.dateTakenMs))
        } else null

        val fileSizeFormatted = MediaGrouping.formatStorageSize(item.size)

        val mpString = if (item.width > 0 && item.height > 0) {
            val mp = (item.width.toLong() * item.height.toLong()).toDouble() / 1_000_000.0
            String.format(Locale.US, "%.1f MP", mp)
        } else null

        val dimensionsString = if (item.width > 0 && item.height > 0) {
            "${item.width} × ${item.height}"
        } else null

        // For video files, EXIF is not applicable
        if (item.isVideo) {
            return@withContext ExifMetadata(
                dimensions = dimensionsString,
                megapixels = mpString,
                fileSize = fileSizeFormatted,
                formattedDate = formattedDate,
                mimeType = item.mimeType
            )
        }

        try {
            context.contentResolver.openInputStream(item.uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)

                val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()

                // Aperture / F-number
                val fNumber = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)
                val aperture = if (fNumber > 0.0) String.format(Locale.US, "f/%.1f", fNumber) else null

                // Focal length
                val focalLen = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
                val focalLength = if (focalLen > 0.0) String.format(Locale.US, "%.1f mm", focalLen) else null

                // ISO
                val isoInt = exif.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 0)
                val iso = if (isoInt > 0) "ISO $isoInt" else null

                // Exposure time
                val expTime = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
                val exposureTime = formatExposureTime(expTime)

                // Flash
                val flashInt = exif.getAttributeInt(ExifInterface.TAG_FLASH, -1)
                val flash = if (flashInt != -1) {
                    if ((flashInt and 1) != 0) "Flash fired" else "No flash"
                } else null

                // Lat / Long
                val latLong = exif.latLong
                val lat = latLong?.get(0)
                val lng = latLong?.get(1)

                return@withContext ExifMetadata(
                    cameraMake = make,
                    cameraModel = model,
                    aperture = aperture,
                    focalLength = focalLength,
                    iso = iso,
                    exposureTime = exposureTime,
                    flash = flash,
                    megapixels = mpString,
                    dimensions = dimensionsString,
                    fileSize = fileSizeFormatted,
                    formattedDate = formattedDate,
                    mimeType = item.mimeType,
                    latitude = lat,
                    longitude = lng
                )
            }
        } catch (_: Exception) {
            // Ignore corrupted or unreadable EXIF header gracefully
        }

        return@withContext ExifMetadata(
            dimensions = dimensionsString,
            megapixels = mpString,
            fileSize = fileSizeFormatted,
            formattedDate = formattedDate,
            mimeType = item.mimeType
        )
    }

    fun formatExposureTime(seconds: Double): String? {
        if (seconds <= 0.0) return null
        return if (seconds >= 1.0) {
            String.format(Locale.US, "%.1fs", seconds)
        } else {
            val denom = (1.0 / seconds).roundToInt()
            "1/${denom}s"
        }
    }
}
