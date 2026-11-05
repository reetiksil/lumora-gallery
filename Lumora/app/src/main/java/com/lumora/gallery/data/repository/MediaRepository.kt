package com.lumora.gallery.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.lumora.gallery.data.model.Album
import com.lumora.gallery.data.model.MediaItem
import com.lumora.gallery.data.preferences.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Repository responsible for querying MediaStore without duplicating metadata.
 * Uses ContentObserver to trigger incremental Flow emissions when media changes on device.
 */
class MediaRepository(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository
) {
    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * Minimal MediaStore projection to keep query memory tiny and execution fast.
     */
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

    /**
     * Scans all distinct albums/buckets on the device.
     */
    suspend fun getAllAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val albumMap = mutableMapOf<String, Pair<Album, Long>>() // bucketId -> (Album, latestDateTaken)

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

        // Return albums with Camera first, then alphabetically
        albumMap.values.map { it.first }.sortedWith(
            compareByDescending<Album> { it.isCamera }.thenBy { it.bucketName.lowercase(Locale.ROOT) }
        )
    }

    /**
     * Emits the list of MediaItems filtered by the provided selected bucket IDs.
     * Automatically re-emits whenever MediaStore changes via ContentObserver.
     */
    fun getMediaFlow(selectedBucketIds: Set<String>): Flow<List<MediaItem>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                trySend(queryMedia(selectedBucketIds))
            }
        }

        // Register observer for Images and Videos
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
        contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )

        // Initial emission
        trySend(queryMedia(selectedBucketIds))

        awaitClose {
            contentResolver.unregisterContentObserver(observer)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Queries MediaStore synchronously on the IO dispatcher.
     */
    private fun queryMedia(selectedBucketIds: Set<String>): List<MediaItem> {
        val items = mutableListOf<MediaItem>()

        // If bucket filtering is active, only include items from those buckets
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
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_TAKEN} DESC, ${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        contentResolver.query(
            queryUri,
            mediaProjection,
            selectionBuilder.toString(),
            selectionArgs.toTypedArray(),
            sortOrder
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

                // Fallback: If date_taken is missing, use date_modified in milliseconds
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
     * Groups a flat list of media items into section headers (Today, Yesterday, 24 Oct, or Oct 2025).
     */
    fun groupMediaByDate(items: List<MediaItem>): Map<String, List<MediaItem>> {
        val nowCal = Calendar.getInstance()
        val currentYear = nowCal.get(Calendar.YEAR)
        val todayDayOfYear = nowCal.get(Calendar.DAY_OF_YEAR)

        val itemCal = Calendar.getInstance()
        val dayMonthFormat = SimpleDateFormat("d MMMM", Locale.getDefault())
        val fullDateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())

        val grouped = linkedMapOf<String, MutableList<MediaItem>>()

        for (item in items) {
            itemCal.timeInMillis = item.dateTakenMs
            val itemYear = itemCal.get(Calendar.YEAR)
            val itemDayOfYear = itemCal.get(Calendar.DAY_OF_YEAR)

            val header = when {
                itemYear == currentYear && itemDayOfYear == todayDayOfYear -> "Today"
                itemYear == currentYear && itemDayOfYear == todayDayOfYear - 1 -> "Yesterday"
                itemYear == currentYear -> dayMonthFormat.format(Date(item.dateTakenMs))
                else -> fullDateFormat.format(Date(item.dateTakenMs))
            }

            grouped.getOrPut(header) { mutableListOf() }.add(item)
        }
        return grouped
    }
}
