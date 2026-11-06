package com.lumora.gallery

import android.net.Uri
import com.lumora.gallery.data.model.MediaItem
import com.lumora.gallery.data.util.MediaGrouping
import com.lumora.gallery.ui.navigation.TimeScale
import com.lumora.gallery.ui.trash.TrashedItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito
import java.io.File
import java.util.Calendar

class MilestoneETest {

    private fun createDummyItem(id: Long, dateTakenMs: Long, sizeBytes: Long = 1024L): MediaItem {
        val mockUri = Mockito.mock(Uri::class.java)
        return MediaItem(
            id = id,
            uri = mockUri,
            displayName = "IMG_$id.jpg",
            mimeType = "image/jpeg",
            size = sizeBytes,
            dateTakenMs = dateTakenMs,
            dateModifiedSec = dateTakenMs / 1000,
            durationMs = 0L,
            width = 1920,
            height = 1080,
            bucketId = "camera",
            bucketName = "Camera"
        )
    }

    @Test
    fun testGroupMediaByYear() {
        val cal2025 = Calendar.getInstance().apply { set(2025, Calendar.JUNE, 15) }
        val cal2026 = Calendar.getInstance().apply { set(2026, Calendar.JANUARY, 10) }

        val item1 = createDummyItem(1L, cal2025.timeInMillis)
        val item2 = createDummyItem(2L, cal2026.timeInMillis)

        val grouped = MediaGrouping.groupMediaByYear(listOf(item1, item2))
        assertEquals(2, grouped.size)
        assertTrue(grouped.containsKey("2025"))
        assertTrue(grouped.containsKey("2026"))
        assertEquals(1, grouped["2025"]!!.size)
        assertEquals(1, grouped["2026"]!!.size)
    }

    @Test
    fun testGroupMediaByMonth() {
        val calNov = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 1) }
        val calOct = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 31) }

        val item1 = createDummyItem(1L, calNov.timeInMillis)
        val item2 = createDummyItem(2L, calOct.timeInMillis)

        val grouped = MediaGrouping.groupMediaByMonth(listOf(item1, item2))
        assertEquals(2, grouped.size)
        assertTrue(grouped.keys.any { it.contains("November", ignoreCase = true) })
        assertTrue(grouped.keys.any { it.contains("October", ignoreCase = true) })
    }

    @Test
    fun testStorageFormatting() {
        assertEquals("0 B", MediaGrouping.formatStorageSize(0L))
        assertEquals("500 B", MediaGrouping.formatStorageSize(500L))
        assertTrue(MediaGrouping.formatStorageSize(1024L).contains("KB"))
        assertTrue(MediaGrouping.formatStorageSize(1024L * 1024 * 15).contains("MB"))
        assertTrue(MediaGrouping.formatStorageSize(1024L * 1024 * 1024 * 2).contains("GB"))
    }

    @Test
    fun testTrashRetentionCalculation() {
        val now = System.currentTimeMillis()
        val dummyFile = File("dummy.jpg")
        val freshItem = TrashedItem(dummyFile, "dummy.jpg", now)
        assertEquals(30, freshItem.daysRemaining)

        val tenDaysAgo = now - 10L * 24 * 60 * 60 * 1000
        val olderItem = TrashedItem(dummyFile, "dummy.jpg", tenDaysAgo)
        assertEquals(20, olderItem.daysRemaining)
    }

    @Test
    fun testTimeScaleEnumValues() {
        val scales = TimeScale.entries
        assertEquals(3, scales.size)
        assertTrue(scales.contains(TimeScale.YEARS))
        assertTrue(scales.contains(TimeScale.MONTHS))
        assertTrue(scales.contains(TimeScale.DAYS))
    }

    @Test
    fun testThemeModeEnumValues() {
        val modes = com.lumora.gallery.data.model.ThemeMode.entries
        assertEquals(3, modes.size)
        assertTrue(modes.contains(com.lumora.gallery.data.model.ThemeMode.AMOLED_DARK))
        assertTrue(modes.contains(com.lumora.gallery.data.model.ThemeMode.LIGHT))
        assertTrue(modes.contains(com.lumora.gallery.data.model.ThemeMode.SYSTEM))
    }

    @Test
    fun testNavTabEnumValues() {
        val tabs = com.lumora.gallery.ui.navigation.NavTab.entries
        assertEquals(2, tabs.size)
        assertTrue(tabs.contains(com.lumora.gallery.ui.navigation.NavTab.PHOTOS))
        assertTrue(tabs.contains(com.lumora.gallery.ui.navigation.NavTab.ALBUMS))
    }
}

