package com.lumora.gallery

import android.net.Uri
import com.lumora.gallery.data.model.MediaItem
import com.lumora.gallery.data.model.SortOrder
import com.lumora.gallery.data.util.MediaGrouping
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class MediaGroupingTest {

    private fun createDummyItem(
        id: Long,
        name: String,
        bucketId: String,
        dateTakenMs: Long,
        dateModifiedSec: Long
    ): MediaItem {
        return MediaItem(
            id = id,
            uri = Uri.EMPTY,
            displayName = name,
            mimeType = "image/jpeg",
            size = 1024L * 1024L, // 1 MB
            dateTakenMs = dateTakenMs,
            dateModifiedSec = dateModifiedSec,
            durationMs = 0L,
            width = 1920,
            height = 1080,
            bucketId = bucketId,
            bucketName = "Album_$bucketId"
        )
    }

    @Test
    fun testFilterByBuckets_strictlyMatchesAllowedBucketIds() {
        val items = listOf(
            createDummyItem(1, "camera1.jpg", "bucket_camera", 1000L, 100L),
            createDummyItem(2, "screenshot1.png", "bucket_screenshots", 2000L, 200L),
            createDummyItem(3, "camera2.jpg", "bucket_camera", 3000L, 300L),
            createDummyItem(4, "whatsapp.jpg", "bucket_whatsapp", 4000L, 400L)
        )

        val allowedBuckets = setOf("bucket_camera")
        val filtered = MediaGrouping.filterByBuckets(items, allowedBuckets)

        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.bucketId == "bucket_camera" })
    }

    @Test
    fun testFilterByBuckets_emptySetReturnsEmptyList() {
        val items = listOf(
            createDummyItem(1, "camera1.jpg", "bucket_camera", 1000L, 100L)
        )
        val filtered = MediaGrouping.filterByBuckets(items, emptySet())
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun testSortMedia_dateTakenDescending() {
        val items = listOf(
            createDummyItem(1, "old.jpg", "bucket", 1000L, 100L),
            createDummyItem(2, "new.jpg", "bucket", 5000L, 500L),
            createDummyItem(3, "mid.jpg", "bucket", 3000L, 300L)
        )

        val sorted = MediaGrouping.sortMedia(items, SortOrder.DATE_TAKEN_DESC)
        assertEquals(listOf(2L, 3L, 1L), sorted.map { it.id })
    }

    @Test
    fun testSortMedia_displayNameAlphabetical() {
        val items = listOf(
            createDummyItem(1, "Zebra.jpg", "bucket", 1000L, 100L),
            createDummyItem(2, "Apple.jpg", "bucket", 2000L, 200L),
            createDummyItem(3, "Mango.jpg", "bucket", 3000L, 300L)
        )

        val sorted = MediaGrouping.sortMedia(items, SortOrder.DISPLAY_NAME_ASC)
        assertEquals(listOf("Apple.jpg", "Mango.jpg", "Zebra.jpg"), sorted.map { it.displayName })
    }

    @Test
    fun testGroupMediaByDate_todayAndYesterday() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 5, 12, 0, 0)
        }
        val referenceNow = cal.timeInMillis

        // Today item
        val todayItem = createDummyItem(1, "today.jpg", "b", referenceNow, 100L)

        // Yesterday item (subtract 24 hours)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayItem = createDummyItem(2, "yesterday.jpg", "b", cal.timeInMillis, 200L)

        // 2 years ago item
        cal.set(2024, Calendar.JANUARY, 15, 12, 0, 0)
        val olderItem = createDummyItem(3, "older.jpg", "b", cal.timeInMillis, 300L)

        val grouped = MediaGrouping.groupMediaByDate(
            listOf(todayItem, yesterdayItem, olderItem),
            referenceTimeMillis = referenceNow
        )

        assertTrue(grouped.containsKey("Today"))
        assertTrue(grouped.containsKey("Yesterday"))
        assertEquals(1, grouped["Today"]?.size)
        assertEquals(1, grouped["Yesterday"]?.size)
    }

    @Test
    fun testFormatStorageSize_formatsUnitsCorrectly() {
        assertEquals("0 B", MediaGrouping.formatStorageSize(0L))
        assertEquals("500 B", MediaGrouping.formatStorageSize(500L))
        assertEquals("1.5 KB", MediaGrouping.formatStorageSize(1536L))
        assertEquals("100.0 MB", MediaGrouping.formatStorageSize(100L * 1024L * 1024L))
        assertEquals("2.4 GB", MediaGrouping.formatStorageSize((2.4 * 1024L * 1024L * 1024L).toLong()))
    }
}
