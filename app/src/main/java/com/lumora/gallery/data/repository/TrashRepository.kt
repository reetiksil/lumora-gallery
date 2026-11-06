package com.lumora.gallery.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.lumora.gallery.data.model.MediaItem
import com.lumora.gallery.ui.trash.TrashedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

/**
 * Manages the offline 30-day Recently Deleted Vault.
 * Trashed items are moved into an isolated app-private sandbox (trash_bin/)
 * with original metadata, allowing instantaneous safe deletion,
 * automatic 30-day purge, and one-tap restoration back to the Android gallery.
 */
class TrashRepository(private val context: Context) {

    private val trashDir: File by lazy {
        context.filesDir.resolve("trash_bin").apply {
            if (!exists()) mkdirs()
        }
    }

    suspend fun trashMedia(item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!trashDir.exists()) trashDir.mkdirs()

            val safeName = "${System.currentTimeMillis()}_${item.id}_${item.displayName.replace('/', '_')}"
            val targetFile = trashDir.resolve(safeName)
            val metaFile = trashDir.resolve("$safeName.meta")

            // Copy file bytes into private trash vault
            context.contentResolver.openInputStream(item.uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext false

            // Save original metadata
            val props = Properties().apply {
                setProperty("id", item.id.toString())
                setProperty("displayName", item.displayName)
                setProperty("mimeType", item.mimeType)
                setProperty("dateTakenMs", item.dateTakenMs.toString())
                setProperty("size", item.size.toString())
                setProperty("bucketId", item.bucketId)
                setProperty("bucketName", item.bucketName)
                setProperty("deletedTimestamp", System.currentTimeMillis().toString())
                setProperty("originalUri", item.uri.toString())
            }

            FileOutputStream(metaFile).use { outStream ->
                props.store(outStream, "Lumora Trash Item Metadata")
            }

            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getTrashedItems(): List<TrashedItem> = withContext(Dispatchers.IO) {
        if (!trashDir.exists()) return@withContext emptyList()

        val files = trashDir.listFiles()?.filter { it.isFile && !it.name.endsWith(".meta") } ?: emptyList()
        val items = mutableListOf<TrashedItem>()
        val now = System.currentTimeMillis()

        for (file in files) {
            val metaFile = trashDir.resolve("${file.name}.meta")
            var deletedTs = file.lastModified()
            var displayName = file.name

            if (metaFile.exists()) {
                try {
                    val props = Properties().apply {
                        FileInputStream(metaFile).use { load(it) }
                    }
                    deletedTs = props.getProperty("deletedTimestamp")?.toLongOrNull() ?: file.lastModified()
                    displayName = props.getProperty("displayName") ?: file.name
                } catch (_: Exception) {}
            }

            val trashedItem = TrashedItem(
                file = file,
                displayName = displayName,
                deletedTimestamp = deletedTs
            )

            // Purge expired items (older than 30 days)
            if (trashedItem.daysRemaining <= 0) {
                file.delete()
                metaFile.delete()
            } else {
                items.add(trashedItem)
            }
        }

        items.sortedByDescending { it.deletedTimestamp }
    }

    suspend fun restoreItem(item: TrashedItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val metaFile = trashDir.resolve("${item.file.name}.meta")
            var mimeType = "image/jpeg"
            var displayName = item.displayName

            if (metaFile.exists()) {
                try {
                    val props = Properties().apply {
                        FileInputStream(metaFile).use { load(it) }
                    }
                    mimeType = props.getProperty("mimeType") ?: mimeType
                    displayName = props.getProperty("displayName") ?: displayName
                } catch (_: Exception) {}
            }

            val isVideo = mimeType.startsWith("video/")
            val targetCollection = if (isVideo) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        if (isVideo) Environment.DIRECTORY_MOVIES + "/Lumora" else Environment.DIRECTORY_PICTURES + "/Lumora"
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val restoredUri = context.contentResolver.insert(targetCollection, contentValues)
            if (restoredUri != null) {
                context.contentResolver.openOutputStream(restoredUri)?.use { out ->
                    FileInputStream(item.file).use { input ->
                        input.copyTo(out)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(restoredUri, contentValues, null, null)
                }

                item.file.delete()
                metaFile.delete()
                return@withContext true
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deletePermanently(item: TrashedItem): Boolean = withContext(Dispatchers.IO) {
        val metaFile = trashDir.resolve("${item.file.name}.meta")
        metaFile.delete()
        item.file.delete()
    }

    suspend fun emptyTrash(): Boolean = withContext(Dispatchers.IO) {
        trashDir.listFiles()?.forEach { it.delete() }
        true
    }
}
