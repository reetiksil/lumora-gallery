package com.lumora.gallery

import android.net.Uri
import com.lumora.gallery.data.model.ExifMetadata
import com.lumora.gallery.data.model.MediaItem
import com.lumora.gallery.data.util.ExifReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaViewerTest {

    private fun createVideoItem(durationMs: Long): MediaItem {
        return MediaItem(
            id = 100L,
            uri = Uri.EMPTY,
            displayName = "sample_video.mp4",
            mimeType = "video/mp4",
            size = 5L * 1024 * 1024,
            dateTakenMs = 1700000000000L,
            dateModifiedSec = 1700000000L,
            durationMs = durationMs,
            width = 1920,
            height = 1080,
            bucketId = "videos_bucket",
            bucketName = "Videos"
        )
    }

    private fun createImageItem(): MediaItem {
        return MediaItem(
            id = 101L,
            uri = Uri.EMPTY,
            displayName = "sample_photo.jpg",
            mimeType = "image/jpeg",
            size = 2L * 1024 * 1024,
            dateTakenMs = 1700000000000L,
            dateModifiedSec = 1700000000L,
            durationMs = 0L,
            width = 4032,
            height = 3024,
            bucketId = "camera_bucket",
            bucketName = "Camera"
        )
    }

    @Test
    fun testFormattedDuration_videoUnderOneMinute() {
        val video = createVideoItem(45_000L) // 45 seconds
        assertEquals("0:45", video.formattedDuration)
    }

    @Test
    fun testFormattedDuration_videoOverOneMinute() {
        val video = createVideoItem(204_000L) // 3 mins 24 secs
        assertEquals("3:24", video.formattedDuration)
    }

    @Test
    fun testFormattedDuration_videoOverOneHour() {
        val video = createVideoItem(3725_000L) // 1 hr, 2 mins, 5 secs
        assertEquals("1:02:05", video.formattedDuration)
    }

    @Test
    fun testFormattedDuration_imageReturnsEmptyString() {
        val photo = createImageItem()
        assertEquals("", photo.formattedDuration)
    }

    @Test
    fun testExifReader_formatExposureTime() {
        assertNull(ExifReader.formatExposureTime(0.0))
        assertNull(ExifReader.formatExposureTime(-1.0))
        assertEquals("2.0s", ExifReader.formatExposureTime(2.0))
        assertEquals("1/250s", ExifReader.formatExposureTime(0.004))
        assertEquals("1/500s", ExifReader.formatExposureTime(0.002))
        assertEquals("1/1000s", ExifReader.formatExposureTime(0.001))
    }

    @Test
    fun testExifMetadata_deviceDisplayName() {
        val meta1 = ExifMetadata(cameraMake = "Google", cameraModel = "Pixel 8 Pro")
        assertEquals("Google Pixel 8 Pro", meta1.deviceDisplayName)

        val meta2 = ExifMetadata(cameraMake = "Sony", cameraModel = "Sony ILCE-7M4")
        assertEquals("Sony ILCE-7M4", meta2.deviceDisplayName)

        val meta3 = ExifMetadata(cameraMake = "Apple", cameraModel = null)
        assertEquals("Apple", meta3.deviceDisplayName)
    }

    @Test
    fun testExifMetadata_cameraShotSummary() {
        val meta = ExifMetadata(
            aperture = "f/1.8",
            exposureTime = "1/500s",
            focalLength = "24.0 mm",
            iso = "ISO 100"
        )
        assertEquals("f/1.8  •  1/500s  •  24.0 mm  •  ISO 100", meta.cameraShotSummary)
    }

    @Test
    fun testExifMetadata_hasGpsCoordinates() {
        val withGps = ExifMetadata(latitude = 37.7749, longitude = -122.4194)
        assertTrue(withGps.hasGpsCoordinates)

        val withoutGps = ExifMetadata(latitude = null, longitude = null)
        assertFalse(withoutGps.hasGpsCoordinates)
    }
}
