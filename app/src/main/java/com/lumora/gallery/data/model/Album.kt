package com.lumora.gallery.data.model

import android.net.Uri

/**
 * Model representing a MediaStore album / bucket folder.
 */
data class Album(
    val bucketId: String,
    val bucketName: String,
    val coverUri: Uri?,
    val itemCount: Int,
    val isCamera: Boolean = false,
    val isSelectedForPhotos: Boolean = false
)
