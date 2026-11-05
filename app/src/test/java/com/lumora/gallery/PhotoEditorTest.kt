package com.lumora.gallery

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.lumora.gallery.ui.editor.model.AdjustmentType
import com.lumora.gallery.ui.editor.model.Adjustments
import com.lumora.gallery.ui.editor.model.CropRatio
import com.lumora.gallery.ui.editor.model.DrawingStroke
import com.lumora.gallery.ui.editor.model.EditorState
import com.lumora.gallery.ui.editor.model.ExportFormat
import com.lumora.gallery.ui.editor.model.FilterPreset
import com.lumora.gallery.ui.editor.model.MarkupType
import com.lumora.gallery.ui.editor.util.ColorMatrixGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoEditorTest {

    @Test
    fun testAdjustmentsDefault() {
        val defaultAdjustments = Adjustments()
        assertTrue(defaultAdjustments.isDefault())

        val modified = defaultAdjustments.copy(brightness = 15f)
        assertFalse(modified.isDefault())
    }

    @Test
    fun testEditorStateModifications() {
        val state = EditorState()
        assertFalse(state.hasModifications())

        val rotated = state.copy(rotationDegrees = 90)
        assertTrue(rotated.hasModifications())

        val filtered = state.copy(filterPreset = FilterPreset.SEPIA)
        assertTrue(filtered.hasModifications())

        val withStroke = state.copy(
            strokes = listOf(
                DrawingStroke(
                    points = listOf(Offset(0f, 0f), Offset(10f, 10f)),
                    color = Color.Red,
                    strokeWidth = 5f,
                    markupType = MarkupType.PEN
                )
            )
        )
        assertTrue(withStroke.hasModifications())
    }

    @Test
    fun testColorMatrixIdentity() {
        val matrix = ColorMatrixGenerator.createComposeColorMatrix(
            adjustments = Adjustments(),
            preset = FilterPreset.ORIGINAL,
            intensity = 1.0f
        )
        assertNotNull(matrix)
        val values = matrix.values
        assertEquals(20, values.size)
        // Check diagonal elements of identity matrix
        assertEquals(1f, values[0], 0.001f) // R
        assertEquals(1f, values[6], 0.001f) // G
        assertEquals(1f, values[12], 0.001f) // B
        assertEquals(1f, values[18], 0.001f) // A
    }

    @Test
    fun testFilterPresetsGeneration() {
        for (preset in FilterPreset.entries) {
            val matrix = ColorMatrixGenerator.getPresetColorMatrix(preset, 1.0f)
            assertNotNull(matrix)
            assertEquals(20, matrix.values.size)
        }
    }

    @Test
    fun testFilterIntensityInterpolation() {
        val zeroIntensity = ColorMatrixGenerator.getPresetColorMatrix(FilterPreset.MONOCHROME, 0.0f)
        // At 0 intensity, should be identity
        assertEquals(1f, zeroIntensity.values[0], 0.01f)
        assertEquals(0f, zeroIntensity.values[1], 0.01f)

        val fullIntensity = ColorMatrixGenerator.getPresetColorMatrix(FilterPreset.MONOCHROME, 1.0f)
        // At full monochrome, red channel luminance is ~0.299f
        assertEquals(0.299f, fullIntensity.values[0], 0.01f)
    }

    @Test
    fun testCropRatioCalculations() {
        assertEquals(1.0f, CropRatio.SQUARE_1_1.ratio!!, 0.001f)
        assertEquals(4f / 3f, CropRatio.RATIO_4_3.ratio!!, 0.001f)
        assertEquals(16f / 9f, CropRatio.RATIO_16_9.ratio!!, 0.001f)
        assertEquals(9f / 16f, CropRatio.RATIO_9_16.ratio!!, 0.001f)
    }

    @Test
    fun testExportFormatMimeTypes() {
        assertEquals("image/jpeg", ExportFormat.JPEG.mimeType)
        assertEquals("image/png", ExportFormat.PNG.mimeType)
        assertEquals("image/webp", ExportFormat.WEBP.mimeType)
        assertEquals("jpg", ExportFormat.JPEG.extension)
        assertEquals("png", ExportFormat.PNG.extension)
        assertEquals("webp", ExportFormat.WEBP.extension)
    }

    @Test
    fun testAdjustmentTypeRanges() {
        for (type in AdjustmentType.entries) {
            assertTrue(type.min < type.max)
            assertTrue(type.default in type.min..type.max)
        }
    }
}
