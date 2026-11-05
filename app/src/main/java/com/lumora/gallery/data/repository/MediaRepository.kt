package com.lumora.gallery.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.lumora.gallery.data.model.Album
import com.lumora.gallery.data.model.MediaItem
import com.lumora.gallery.data.model.SortOrder
import com.lumora.gallery.data.preferences.PreferencesRepository
import com.lumora.gallery.data.util.MediaGrouping
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Repository responsible for querying MediaStore without duplicating metadata.
 * Employs ContentObserver to trigger incremental Flow emissions when media changes.
 */
class MediaRepository(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository
) {
    private val contentResolver: ContentResolver = context.contentResolver

    private val mediaProjection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.MIME_TYPE,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.DATE_TAKEN,
        MediaStore.Files.FileColumns.DATE_MODIFIED,
        MediaStore.Files.FileColumns.DURATION,
        MediaStore.Files.FileColumns.WIDTH,
        MediaStore.Files.FileColumns.HEIGHT,
        MediaStore.Files.FileColumns.BUCKET_ID,
        MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME
    )

    suspend fun getAllAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val albumMap = mutableMapOf<String, Pair<Album, Long>>()

        val queryUri = MediaStore.Files.getContentUri("external")
        val selection = "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?) AND ${MediaStore.Files.FileColumns.SIZE} > 0"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_TAKEN} DESC"

        contentResolver.query(queryUri, mediaProjection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val bucketId = cursor.getString(bucketIdCol) ?: continue
                val bucketName = cursor.getString(bucketNameCol) ?: "Unknown"
                val id = cursor.getLong(idCol)
                val mimeType = cursor.getString(mimeCol) ?: ""
                val dateTaken = cursor.getLong(dateTakenCol)

                val contentUri = if (mimeType.startsWith("video/")) {
                    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                } else {
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                }

                val isCamera = bucketName.equals("Camera", ignoreCase = true) ||
                        bucketName.equals("DCIM", ignoreCase = true)

                val existing = albumMap[bucketId]
                if (existing == null) {
                    val album = Album(
                        bucketId = bucketId,
                        bucketName = bucketName,
                        coverUri = contentUri,
                        itemCount = 1,
                        isCamera = isCamera
                    )
                    albumMap[bucketId] = Pair(album, dateTaken)
                } else {
                    val updated = existing.first.copy(itemCount = existing.first.itemCount + 1)
                    albumMap[bucketId] = Pair(updated, existing.second)
                }
            }
        }

        albumMap.values.map { it.first }.sortedWith(
            compareByDescending<Album> { it.isCamera }.thenBy { it.bucketName.lowercase(Locale.ROOT) }
        )
    }

    fun getMediaFlow(
        selectedBucketIds: Set<String>,
        sortOrder: SortOrder = SortOrder.DATE_TAKEN_DESC
    ): Flow<List<MediaItem>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                trySend(queryMedia(selectedBucketIds, sortOrder))
            }
        }

        contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer)
        contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer)

        trySend(queryMedia(selectedBucketIds, sortOrder))

        awaitClose {
            contentResolver.unregisterContentObserver(observer)
        }
    }.flowOn(Dispatchers.IO)

    fun getAlbumMediaFlow(bucketId: String): Flow<List<MediaItem>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                trySend(queryMedia(setOf(bucketId)))
            }
        }

        contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer)
        contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer)

        trySend(queryMedia(setOf(bucketId)))

        awaitClose {
            contentResolver.unregisterContentObserver(observer)
        }
    }.flowOn(Dispatchers.IO)

    private fun queryMedia(
        selectedBucketIds: Set<String>,
        sortOrder: SortOrder = SortOrder.DATE_TAKEN_DESC
    ): List<MediaItem> {
        val items = mutableListOf<MediaItem>()

        val selectionBuilder = StringBuilder(
            "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?) AND ${MediaStore.Files.FileColumns.SIZE} > 0"
        )
        val selectionArgs = mutableListOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        if (selectedBucketIds.isNotEmpty()) {
            val placeholders = selectedBucketIds.joinToString(",") { "?" }
            selectionBuilder.append(" AND ${MediaStore.Files.FileColumns.BUCKET_ID} IN ($placeholders)")
            selectionArgs.addAll(selectedBucketIds)
        }

        val queryUri = MediaStore.Files.getContentUri("external")
        val sqlSort = when (sortOrder) {
            SortOrder.DATE_TAKEN_DESC -> "${MediaStore.Files.FileColumns.DATE_TAKEN} DESC"
            SortOrder.DATE_TAKEN_ASC -> "${MediaStore.Files.FileColumns.DATE_TAKEN} ASC"
            SortOrder.DATE_MODIFIED_DESC -> "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            SortOrder.DISPLAY_NAME_ASC -> "${MediaStore.Files.FileColumns.DISPLAY_NAME} ASC"
        }

        contentResolver.query(
            queryUri,
            mediaProjection,
            selectionBuilder.toString(),
            selectionArgs.toTypedArray(),
            sqlSort
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)
            val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val displayName = cursor.getString(nameCol) ?: ""
                val mimeType = cursor.getString(mimeCol) ?: "image/jpeg"
                val size = cursor.getLong(sizeCol)
                var dateTaken = cursor.getLong(dateTakenCol)
                val dateModified = cursor.getLong(dateModCol)
                val duration = cursor.getLong(durCol)
                val width = cursor.getInt(widthCol)
                val height = cursor.getInt(heightCol)
                val bucketId = cursor.getString(bucketIdCol) ?: ""
                val bucketName = cursor.getString(bucketNameCol) ?: ""

                if (dateTaken <= 0 && dateModified > 0) {
                    dateTaken = dateModified * 1000
                }

                val contentUri = if (mimeType.startsWith("video/")) {
                    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                } else {
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                }

                items.add(
                    MediaItem(
                        id = id,
                        uri = contentUri,
                        displayName = displayName,
                        mimeType = mimeType,
                        size = size,
                        dateTakenMs = dateTaken,
                        dateModifiedSec = dateModified,
                        durationMs = duration,
                        width = width,
                        height = height,
                        bucketId = bucketId,
                        bucketName = bucketName
                    )
                )
            }
        }
        return items
    }

    /**
     * Resolves a single MediaItem from an external Uri (e.g. ACTION_VIEW from camera or file manager).
     */
    suspend fun getMediaItemFromUri(uri: Uri): MediaItem? = withContext(Dispatchers.IO) {
        val mimeType = contentResolver.getType(uri) ?: if (uri.toString().contains("video", ignoreCase = true)) "video/mp4" else "image/jpeg"

        try {
            contentResolver.query(uri, mediaProjection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idCol = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
                    val nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
                    val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                    val dateTakenCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_TAKEN)
                    val dateModCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
                    val durCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DURATION)
                    val widthCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.WIDTH)
                    val heightCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.HEIGHT)
                    val bucketIdCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.BUCKET_ID)
                    val bucketNameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)

                    val id = if (idCol != -1) cursor.getLong(idCol) else uri.hashCode().toLong()
                    val displayName = if (nameCol != -1) cursor.getString(nameCol) ?: "Media" else "Media"
                    val resolvedMime = if (mimeCol != -1) cursor.getString(mimeCol) ?: mimeType else mimeType
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                    val dateTaken = if (dateTakenCol != -1) cursor.getLong(dateTakenCol) else System.currentTimeMillis()
                    val dateModified = if (dateModCol != -1) cursor.getLong(dateModCol) else 0L
                    val duration = if (durCol != -1) cursor.getLong(durCol) else 0L
                    val width = if (widthCol != -1) cursor.getInt(widthCol) else 0
                    val height = if (heightCol != -1) cursor.getInt(heightCol) else 0
                    val bucketId = if (bucketIdCol != -1) cursor.getString(bucketIdCol) ?: "" else ""
                    val bucketName = if (bucketNameCol != -1) cursor.getString(bucketNameCol) ?: "" else ""

                    return@withContext MediaItem(
                        id = id,
                        uri = uri,
                        displayName = displayName,
                        mimeType = resolvedMime,
                        size = size,
                        dateTakenMs = dateTaken,
                        dateModifiedSec = dateModified,
                        durationMs = duration,
                        width = width,
                        height = height,
                        bucketId = bucketId,
                        bucketName = bucketName
                    )
                }
            }
        } catch (_: Exception) {}

        // Fallback for secure or limited-scope URIs where cursor query might fail
        return@withContext MediaItem(
            id = uri.hashCode().toLong(),
            uri = uri,
            displayName = uri.lastPathSegment ?: "Media",
            mimeType = mimeType,
            size = 0L,
            dateTakenMs = System.currentTimeMillis(),
            dateModifiedSec = System.currentTimeMillis() / 1000,
            durationMs = 0L,
            width = 0,
            height = 0,
            bucketId = "",
            bucketName = ""
        )
    }

    /**
     * Attempts to query neighboring media in the same bucket so swiping works immediately.
     */
    suspend fun getNeighboringMedia(item: MediaItem): List<MediaItem> = withContext(Dispatchers.IO) {
        if (item.bucketId.isBlank()) return@withContext listOf(item)
        val albumItems = queryMedia(setOf(item.bucketId), SortOrder.DATE_TAKEN_DESC)
        if (albumItems.isEmpty() || albumItems.none { it.id == item.id || it.uri == item.uri }) {
            listOf(item)
        } else {
            albumItems
        }
    }

    /**
     * Calculates the on-disk cache size consumed by Coil thumbnails.
     */
    suspend fun getDiskCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        val cacheDir = context.cacheDir.resolve("image_cache")
        calculateDirSize(cacheDir)
    }

    /**
     * Clears disk cache safely and returns true on success.
     */
    suspend fun clearDiskCache(): Boolean = withContext(Dispatchers.IO) {
        val cacheDir = context.cacheDir.resolve("image_cache")
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
            true
        } else {
            true
        }
    }

    private fun calculateDirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) calculateDirSize(file) else file.length()
        }
        return size
    }

    fun groupMediaByDate(items: List<MediaItem>): Map<String, List<MediaItem>> {
        return MediaGrouping.groupMediaByDate(items)
    }

    fun groupMediaByYear(items: List<MediaItem>): Map<String, List<MediaItem>> {
        return MediaGrouping.groupMediaByYear(items)
    }

    fun groupMediaByMonth(items: List<MediaItem>): Map<String, List<MediaItem>> {
        return MediaGrouping.groupMediaByMonth(items)
    }
}
