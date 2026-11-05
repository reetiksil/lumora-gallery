package com.lumora.gallery.ui.editor.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

enum class EditorTool {
    NONE,
    CROP_ROTATE,
    ADJUST,
    FILTERS,
    MARKUP,
    TEXT
}

enum class CropRatio(val displayName: String, val ratio: Float?) {
    FREE("Free", null),
    ORIGINAL("Original", -1f),
    SQUARE_1_1("1:1", 1.0f),
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_3_4("3:4", 3f / 4f),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_9_16("9:16", 9f / 16f)
}

enum class AdjustmentType(val displayName: String, val min: Float, val max: Float, val default: Float) {
    BRIGHTNESS("Brightness", -100f, 100f, 0f),
    CONTRAST("Contrast", -100f, 100f, 0f),
    SATURATION("Saturation", -100f, 100f, 0f),
    WARMTH("Warmth", -100f, 100f, 0f),
    EXPOSURE("Exposure", -100f, 100f, 0f),
    VIGNETTE("Vignette", 0f, 100f, 0f)
}

enum class FilterPreset(val displayName: String) {
    ORIGINAL("Original"),
    VIVID("Vivid"),
    WARM_SUN("Warm"),
    COOL_BREEZE("Cool"),
    MONOCHROME("Mono"),
    NOIR("Noir"),
    SEPIA("Sepia"),
    VINTAGE("Vintage"),
    CINEMATIC("Cinema"),
    FADE("Fade"),
    DRAMATIC("Dramatic"),
    PASTEL("Pastel"),
    GOLDEN_HOUR("Golden"),
    EMERALD("Emerald"),
    BLEACH_BYPASS("Bleach"),
    CYANOTYPE("Cyan"),
    SILVER("Silver")
}

enum class MarkupType {
    PEN,
    HIGHLIGHTER,
    BLUR,
    ERASER
}

data class DrawingStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val markupType: MarkupType
)

data class TextAnnotation(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val position: Offset = Offset(200f, 300f),
    val color: Color = Color.White,
    val fontSizeSp: Float = 24f,
    val hasBackground: Boolean = true,
    val backgroundColor: Color = Color.Black.copy(alpha = 0.6f)
)

data class Adjustments(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val exposure: Float = 0f,
    val vignette: Float = 0f
) {
    fun isDefault(): Boolean =
        brightness == 0f && contrast == 0f && saturation == 0f &&
                warmth == 0f && exposure == 0f && vignette == 0f
}

data class EditorState(
    val adjustments: Adjustments = Adjustments(),
    val filterPreset: FilterPreset = FilterPreset.ORIGINAL,
    val filterIntensity: Float = 1.0f,
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val cropRatio: CropRatio = CropRatio.FREE,
    val strokes: List<DrawingStroke> = emptyList(),
    val textAnnotations: List<TextAnnotation> = emptyList()
) {
    fun hasModifications(): Boolean =
        !adjustments.isDefault() ||
                filterPreset != FilterPreset.ORIGINAL ||
                rotationDegrees % 360 != 0 ||
                flipHorizontal ||
                flipVertical ||
                cropRatio != CropRatio.FREE ||
                strokes.isNotEmpty() ||
                textAnnotations.isNotEmpty()
}

enum class ExportFormat(val extension: String, val mimeType: String) {
    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    WEBP("webp", "image/webp")
}
