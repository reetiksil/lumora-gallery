package com.lumora.gallery.ui.editor.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.lumora.gallery.ui.editor.model.CropRatio
import com.lumora.gallery.ui.editor.model.DrawingStroke
import com.lumora.gallery.ui.editor.model.EditorState
import com.lumora.gallery.ui.editor.model.ExportFormat
import com.lumora.gallery.ui.editor.model.MarkupType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object ImageSaverUtil {

    /**
     * Saves the edited image either as a new copy or overwriting the original.
     * Returns the Uri of the saved image, or null on failure.
     */
    suspend fun saveEditedImage(
        context: Context,
        sourceUri: Uri,
        state: EditorState,
        format: ExportFormat = ExportFormat.JPEG,
        quality: Int = 90,
        saveAsCopy: Boolean = true,
        eraseExif: Boolean = false
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            // 1. Decode original bitmap (safely scaled if extremely large, e.g. max 4096px)
            val fullBitmap = ImageDecoderUtil.loadPreviewBitmap(
                context = context,
                uri = sourceUri,
                maxWidth = 4096,
                maxHeight = 4096
            ) ?: return@withContext null

            // 2. Apply transformations: Rotation, Flip, Crop
            val transformedBitmap = applyTransformsAndCrop(fullBitmap, state)

            // 3. Render Adjustments, Filters, Markup, and Text onto bitmap
            val outputBitmap = renderEffectsOnBitmap(transformedBitmap, state)

            // 4. Save to MediaStore
            val compressFormat = when (format) {
                ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
                ExportFormat.PNG -> Bitmap.CompressFormat.PNG
                ExportFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }

            val savedUri = if (saveAsCopy) {
                saveNewMediaItem(context, sourceUri, outputBitmap, compressFormat, quality, format)
            } else {
                overwriteMediaItem(context, sourceUri, outputBitmap, compressFormat, quality)
            }

            // 5. Copy or strip EXIF metadata if applicable
            if (savedUri != null && !eraseExif && format == ExportFormat.JPEG) {
                tryCopyExif(context, sourceUri, savedUri)
            }

            // Cleanup
            if (outputBitmap != fullBitmap && !outputBitmap.isRecycled) {
                outputBitmap.recycle()
            }
            if (!fullBitmap.isRecycled) {
                fullBitmap.recycle()
            }

            savedUri
        } catch (_: Exception) {
            null
        }
    }

    private fun applyTransformsAndCrop(source: Bitmap, state: EditorState): Bitmap {
        var result = source

        // Rotation & Flip
        if (state.rotationDegrees % 360 != 0 || state.flipHorizontal || state.flipVertical) {
            val matrix = android.graphics.Matrix().apply {
                if (state.rotationDegrees % 360 != 0) {
                    postRotate(state.rotationDegrees.toFloat())
                }
                val sx = if (state.flipHorizontal) -1f else 1f
                val sy = if (state.flipVertical) -1f else 1f
                if (sx != 1f || sy != 1f) {
                    postScale(sx, sy)
                }
            }
            val transformed = Bitmap.createBitmap(
                result, 0, 0, result.width, result.height, matrix, true
            )
            if (transformed != result && result != source) {
                result.recycle()
            }
            result = transformed
        }

        // Crop Ratio
        if (state.cropRatio != CropRatio.FREE && state.cropRatio.ratio != null && state.cropRatio.ratio!! > 0f) {
            val targetRatio = state.cropRatio.ratio!!
            val currentRatio = result.width.toFloat() / result.height.toFloat()

            var cropW = result.width
            var cropH = result.height

            if (currentRatio > targetRatio) {
                // Image is wider than desired -> crop sides
                cropW = (result.height * targetRatio).toInt().coerceIn(1, result.width)
            } else {
                // Image is taller than desired -> crop top and bottom
                cropH = (result.width / targetRatio).toInt().coerceIn(1, result.height)
            }

            val cropX = ((result.width - cropW) / 2).coerceAtLeast(0)
            val cropY = ((result.height - cropH) / 2).coerceAtLeast(0)

            val cropped = Bitmap.createBitmap(result, cropX, cropY, cropW, cropH)
            if (cropped != result && result != source) {
                result.recycle()
            }
            result = cropped
        }

        return result
    }

    private fun renderEffectsOnBitmap(source: Bitmap, state: EditorState): Bitmap {
        val resultBitmap = Bitmap.createBitmap(
            source.width,
            source.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(resultBitmap)

        // Draw source with ColorMatrix filter
        val composeMatrix = ColorMatrixGenerator.createComposeColorMatrix(
            adjustments = state.adjustments,
            preset = state.filterPreset,
            intensity = state.filterIntensity
        )
        val androidMatrix = ColorMatrixGenerator.toAndroidColorMatrix(composeMatrix)
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(androidMatrix)
        }
        canvas.drawBitmap(source, 0f, 0f, imagePaint)

        // Draw Vignette if applied
        if (state.adjustments.vignette > 0f) {
            drawVignette(canvas, source.width.toFloat(), source.height.toFloat(), state.adjustments.vignette)
        }

        // Draw Markup Strokes (scaled to bitmap coordinates)
        if (state.strokes.isNotEmpty()) {
            drawStrokes(canvas, state.strokes, source.width.toFloat(), source.height.toFloat())
        }

        // Draw Text Annotations
        if (state.textAnnotations.isNotEmpty()) {
            drawTextAnnotations(canvas, state)
        }

        return resultBitmap
    }

    private fun drawVignette(canvas: Canvas, width: Float, height: Float, intensity: Float) {
        val radius = (Math.hypot(width.toDouble(), height.toDouble()) / 2.0).toFloat()
        val alpha = (intensity / 100f * 200f).toInt().coerceIn(0, 255)
        val gradient = android.graphics.RadialGradient(
            width / 2f,
            height / 2f,
            radius,
            intArrayOf(android.graphics.Color.TRANSPARENT, android.graphics.Color.argb(alpha, 0, 0, 0)),
            floatArrayOf(0.4f, 1.0f),
            android.graphics.Shader.TileMode.CLAMP
        )
        val paint = Paint().apply {
            shader = gradient
        }
        canvas.drawRect(0f, 0f, width, height, paint)
    }

    private fun drawStrokes(
        canvas: Canvas,
        strokes: List<DrawingStroke>,
        bitmapWidth: Float,
        bitmapHeight: Float
    ) {
        for (stroke in strokes) {
            if (stroke.points.size < 2) continue

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                strokeWidth = stroke.strokeWidth * (bitmapWidth / 1000f).coerceAtLeast(1f)
            }

            when (stroke.markupType) {
                MarkupType.PEN -> {
                    paint.color = android.graphics.Color.argb(
                        (stroke.color.alpha * 255).toInt(),
                        (stroke.color.red * 255).toInt(),
                        (stroke.color.green * 255).toInt(),
                        (stroke.color.blue * 255).toInt()
                    )
                }
                MarkupType.HIGHLIGHTER -> {
                    paint.color = android.graphics.Color.argb(
                        100,
                        (stroke.color.red * 255).toInt(),
                        (stroke.color.green * 255).toInt(),
                        (stroke.color.blue * 255).toInt()
                    )
                }
                MarkupType.BLUR -> {
                    // Privacy blur / pixelate line
                    paint.color = android.graphics.Color.argb(220, 40, 40, 40)
                    paint.strokeWidth *= 2f
                }
                MarkupType.ERASER -> {
                    paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                }
            }

            val path = Path()
            val first = stroke.points.first()
            path.moveTo(first.x, first.y)
            for (i in 1 until stroke.points.size) {
                val pt = stroke.points[i]
                path.lineTo(pt.x, pt.y)
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun drawTextAnnotations(canvas: Canvas, state: EditorState) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 48f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (annotation in state.textAnnotations) {
            if (annotation.text.isBlank()) continue

            textPaint.color = android.graphics.Color.argb(
                (annotation.color.alpha * 255).toInt(),
                (annotation.color.red * 255).toInt(),
                (annotation.color.green * 255).toInt(),
                (annotation.color.blue * 255).toInt()
            )

            val bounds = Rect()
            textPaint.getTextBounds(annotation.text, 0, annotation.text.length, bounds)

            if (annotation.hasBackground) {
                bgPaint.color = android.graphics.Color.argb(
                    (annotation.backgroundColor.alpha * 255).toInt(),
                    (annotation.backgroundColor.red * 255).toInt(),
                    (annotation.backgroundColor.green * 255).toInt(),
                    (annotation.backgroundColor.blue * 255).toInt()
                )
                val pad = 16f
                val bgRect = RectF(
                    annotation.position.x - pad,
                    annotation.position.y - bounds.height() - pad,
                    annotation.position.x + bounds.width() + pad,
                    annotation.position.y + pad
                )
                canvas.drawRoundRect(bgRect, 12f, 12f, bgPaint)
            }

            canvas.drawText(
                annotation.text,
                annotation.position.x,
                annotation.position.y,
                textPaint
            )
        }
    }

    private fun saveNewMediaItem(
        context: Context,
        sourceUri: Uri,
        bitmap: Bitmap,
        compressFormat: Bitmap.CompressFormat,
        quality: Int,
        format: ExportFormat
    ): Uri? {
        val fileName = "Lumora_${System.currentTimeMillis()}.${format.extension}"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Lumora")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val uri = context.contentResolver.insert(collection, contentValues) ?: return null

        context.contentResolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(compressFormat, quality.coerceIn(10, 100), stream)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, contentValues, null, null)
        }

        return uri
    }

    private fun overwriteMediaItem(
        context: Context,
        sourceUri: Uri,
        bitmap: Bitmap,
        compressFormat: Bitmap.CompressFormat,
        quality: Int
    ): Uri? {
        return try {
            context.contentResolver.openOutputStream(sourceUri, "rwt")?.use { stream ->
                bitmap.compress(compressFormat, quality.coerceIn(10, 100), stream)
            }
            sourceUri
        } catch (_: Exception) {
            null
        }
    }

    private fun tryCopyExif(context: Context, srcUri: Uri, destUri: Uri) {
        try {
            // Read source EXIF
            val srcExif = context.contentResolver.openInputStream(srcUri)?.use {
                ExifInterface(it)
            } ?: return

            // Open destination file descriptor for writing EXIF tags
            context.contentResolver.openFileDescriptor(destUri, "rw")?.use { pfd ->
                val destExif = ExifInterface(pfd.fileDescriptor)
                val tags = arrayOf(
                    ExifInterface.TAG_DATETIME,
                    ExifInterface.TAG_DATETIME_DIGITIZED,
                    ExifInterface.TAG_MAKE,
                    ExifInterface.TAG_MODEL,
                    ExifInterface.TAG_F_NUMBER,
                    ExifInterface.TAG_EXPOSURE_TIME,
                    ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
                    ExifInterface.TAG_FOCAL_LENGTH
                )
                for (tag in tags) {
                    val value = srcExif.getAttribute(tag)
                    if (value != null) {
                        destExif.setAttribute(tag, value)
                    }
                }
                destExif.saveAttributes()
            }
        } catch (_: Exception) {}
    }
}
