package com.lumora.gallery.data.model

import android.net.Uri

/**
 * Immutable model representing a single photo or video queried from MediaStore.
 */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val size: Long,
    val dateTakenMs: Long,
    val dateModifiedSec: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val bucketId: String,
    val bucketName: String
) {
    val isVideo: Boolean
        get() = mimeType.startsWith("video/")

    val formattedDuration: String
        get() {
            if (!isVideo || durationMs <= 0) return ""
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val remainingSeconds = totalSeconds % 60
            return if (minutes >= 60) {
                val hours = minutes / 60
                val remMinutes = minutes % 60
                String.format("%d:%02d:%02d", hours, remMinutes, remainingSeconds)
            } else {
                String.format("%d:%02d", minutes, remainingSeconds)
            }
        }
}
