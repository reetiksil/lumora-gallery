package com.lumora.gallery.ui.editor.util

import androidx.compose.ui.graphics.ColorMatrix
import com.lumora.gallery.ui.editor.model.Adjustments
import com.lumora.gallery.ui.editor.model.FilterPreset

object ColorMatrixGenerator {

    /**
     * Builds a Compose ColorMatrix combining user adjustments and filter preset with intensity.
     */
    fun createComposeColorMatrix(
        adjustments: Adjustments,
        preset: FilterPreset,
        intensity: Float = 1.0f
    ): ColorMatrix {
        val result = ColorMatrix()

        // 1. Apply Preset Matrix if not original
        if (preset != FilterPreset.ORIGINAL) {
            val presetMatrix = getPresetColorMatrix(preset, intensity.coerceIn(0f, 1f))
            result.timesAssign(presetMatrix)
        }

        // 2. Brightness (-100 to 100 maps to offset -100 to +100)
        if (adjustments.brightness != 0f) {
            val offset = (adjustments.brightness / 100f) * 80f
            val brightMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, offset,
                    0f, 1f, 0f, 0f, offset,
                    0f, 0f, 1f, 0f, offset,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            result.timesAssign(brightMatrix)
        }

        // 3. Exposure (-100 to 100 maps to scale multiplier 0.5 to 1.5)
        if (adjustments.exposure != 0f) {
            val scale = 1f + (adjustments.exposure / 100f) * 0.7f
            val exposureMatrix = ColorMatrix(
                floatArrayOf(
                    scale, 0f, 0f, 0f, 0f,
                    0f, scale, 0f, 0f, 0f,
                    0f, 0f, scale, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            result.timesAssign(exposureMatrix)
        }

        // 4. Contrast (-100 to 100)
        if (adjustments.contrast != 0f) {
            val contrastFactor = if (adjustments.contrast > 0) {
                1f + (adjustments.contrast / 100f) * 1.5f
            } else {
                1f + (adjustments.contrast / 100f) * 0.7f
            }
            val offset = 128f * (1f - contrastFactor)
            val contrastMatrix = ColorMatrix(
                floatArrayOf(
                    contrastFactor, 0f, 0f, 0f, offset,
                    0f, contrastFactor, 0f, 0f, offset,
                    0f, 0f, contrastFactor, 0f, offset,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            result.timesAssign(contrastMatrix)
        }

        // 5. Saturation (-100 to 100 maps to 0.0 to 2.0)
        if (adjustments.saturation != 0f) {
            val satFactor = 1f + (adjustments.saturation / 100f)
            val satMatrix = ColorMatrix()
            satMatrix.setToSaturation(satFactor.coerceAtLeast(0f))
            result.timesAssign(satMatrix)
        }

        // 6. Warmth / Temperature (-100 cool/blue to +100 warm/amber)
        if (adjustments.warmth != 0f) {
            val warmth = adjustments.warmth / 100f
            val rOffset = (warmth * 30f).coerceIn(-30f, 30f)
            val bOffset = (-warmth * 30f).coerceIn(-30f, 30f)
            val warmthMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, rOffset,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, bOffset,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            result.timesAssign(warmthMatrix)
        }

        return result
    }

    /**
     * Converts compose ColorMatrix values to an Android graphics ColorMatrix.
     */
    fun toAndroidColorMatrix(composeMatrix: ColorMatrix): android.graphics.ColorMatrix {
        return android.graphics.ColorMatrix(composeMatrix.values)
    }

    /**
     * Generate filter matrix array with intensity lerping.
     */
    fun getPresetColorMatrix(preset: FilterPreset, intensity: Float): ColorMatrix {
        val targetValues = when (preset) {
            FilterPreset.ORIGINAL -> getIdentityArray()
            FilterPreset.MONOCHROME -> floatArrayOf(
                0.299f, 0.587f, 0.114f, 0f, 0f,
                0.299f, 0.587f, 0.114f, 0f, 0f,
                0.299f, 0.587f, 0.114f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            FilterPreset.SEPIA -> floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            FilterPreset.VIVID -> floatArrayOf(
                1.3f, -0.1f, -0.1f, 0f, 0f,
                -0.1f, 1.3f, -0.1f, 0f, 0f,
                -0.1f, -0.1f, 1.3f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            FilterPreset.WARM_SUN -> floatArrayOf(
                1.15f, 0f, 0f, 0f, 15f,
                0f, 1.05f, 0f, 0f, 5f,
                0f, 0f, 0.85f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            )
            FilterPreset.COOL_BREEZE -> floatArrayOf(
                0.85f, 0f, 0f, 0f, -10f,
                0f, 1.05f, 0f, 0f, 5f,
                0f, 0f, 1.2f, 0f, 15f,
                0f, 0f, 0f, 1f, 0f
            )
            FilterPreset.NOIR -> floatArrayOf(
                0.35f, 0.65f, 0.15f, 0f, -20f,
                0.35f, 0.65f, 0.15f, 0f, -20f,
                0.35f, 0.65f, 0.15f, 0f, -20f,
                0f, 0f, 0f, 1f, 0f
            )
            FilterPreset.VINTAGE -> floatArrayOf(
                0.9f, 0.2f, 0.1f, 0f, 10f,
                0.1f, 0.8f, 0.1f, 0f, 15f,
                0.1f, 0.2f, 0.6f, 0f, 20f,
                0f, 0f, 0f, 1f, 0f
            )
            FilterPreset.CINEMATIC -> floatArrayOf(
                1.2f, 0.1f, -0.1f, 0f, 10f,
                -0.05f, 1.1f, 0.1f, 0f, 0f,
                -0.1f, 0.2f, 1.25f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            )
            FilterPreset.FADE -> floatArrayOf(
                0.85f, 0f, 0f, 0f, 35f,
                0f, 0.85f, 0f, 0f, 35f,
                0f, 0f, 0.85f, 0f, 35f,
                0f, 0f, 0f, 1f, 0f
            )
            FilterPreset.DRAMATIC -> floatArrayOf(
                1.4f, 0f, 0f, 0f, -30f,
                0f, 1.4f, 0f, 0f, -30f,
                0f, 0f, 1.4f, 0f, -30f,
                0f, 0f, 0f, 1f, 0f
            )
            FilterPreset.PASTEL -> floatArrayOf(
                0.8f, 0.1f, 0.1f, 0f, 40f,
                0.1f, 0.8f, 0.1f, 0f, 40f,
                0.1f, 0.1f, 0.8f, 0f, 40f,
                0f, 0f, 0f, 1f, 0f
            )
            FilterPreset.GOLDEN_HOUR -> floatArrayOf(
                1.25f, 0f, 0f, 0f, 25f,
                0f, 1.1f, 0f, 0f, 15f,
                0f, 0f, 0.75f, 0f, -20f,
                0f, 0f, 0f, 1f, 0f
            )
            FilterPreset.EMERALD -> floatArrayOf(
                0.8f, 0.1f, 0f, 0f, 0f,
                0f, 1.25f, 0.1f, 0f, 15f,
                0f, 0.1f, 0.9f, 0f, 5f,
                0f, 0f, 0f, 1f, 0f
            )
            FilterPreset.BLEACH_BYPASS -> floatArrayOf(
                0.8f, 0.2f, 0.2f, 0f, 10f,
                0.2f, 0.8f, 0.2f, 0f, 10f,
                0.2f, 0.2f, 0.8f, 0f, 10f,
                0f, 0f, 0f, 1f, 0f
            )
            FilterPreset.CYANOTYPE -> floatArrayOf(
                0.1f, 0.2f, 0.2f, 0f, -15f,
                0.2f, 0.4f, 0.4f, 0f, 10f,
                0.3f, 0.4f, 0.7f, 0f, 40f,
                0f, 0f, 0f, 1f, 0f
            )
            FilterPreset.SILVER -> floatArrayOf(
                0.33f, 0.33f, 0.33f, 0f, 15f,
                0.33f, 0.33f, 0.33f, 0f, 15f,
                0.33f, 0.33f, 0.33f, 0f, 25f,
                0f, 0f, 0f, 1f, 0f
            )
        }

        if (intensity >= 0.99f) {
            return ColorMatrix(targetValues)
        }

        // Lerp between identity and target
        val identity = getIdentityArray()
        val interpolated = FloatArray(20)
        for (i in 0 until 20) {
            interpolated[i] = identity[i] + (targetValues[i] - identity[i]) * intensity
        }
        return ColorMatrix(interpolated)
    }

    private fun getIdentityArray(): FloatArray {
        return floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }
}
