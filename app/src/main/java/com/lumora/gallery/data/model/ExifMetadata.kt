package com.lumora.gallery.data.model

/**
 * Immutable model containing extracted EXIF and hardware camera metadata.
 */
data class ExifMetadata(
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val aperture: String? = null,
    val focalLength: String? = null,
    val iso: String? = null,
    val exposureTime: String? = null,
    val flash: String? = null,
    val megapixels: String? = null,
    val dimensions: String? = null,
    val fileSize: String? = null,
    val formattedDate: String? = null,
    val mimeType: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val deviceDisplayName: String?
        get() {
            if (cameraModel.isNullOrBlank()) return cameraMake
            if (cameraMake.isNullOrBlank()) return cameraModel
            return if (cameraModel.startsWith(cameraMake, ignoreCase = true)) {
                cameraModel
            } else {
                "$cameraMake $cameraModel"
            }
        }

    val cameraShotSummary: String?
        get() {
            val parts = listOfNotNull(aperture, exposureTime, focalLength, iso)
            return if (parts.isNotEmpty()) parts.joinToString("  •  ") else null
        }

    val hasGpsCoordinates: Boolean
        get() = latitude != null && longitude != null
}
