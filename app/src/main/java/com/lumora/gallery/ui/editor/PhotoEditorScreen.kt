package com.lumora.gallery.ui.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumora.gallery.data.model.MediaItem
import com.lumora.gallery.ui.editor.model.AdjustmentType
import com.lumora.gallery.ui.editor.model.Adjustments
import com.lumora.gallery.ui.editor.model.CropRatio
import com.lumora.gallery.ui.editor.model.DrawingStroke
import com.lumora.gallery.ui.editor.model.EditorState
import com.lumora.gallery.ui.editor.model.EditorTool
import com.lumora.gallery.ui.editor.model.ExportFormat
import com.lumora.gallery.ui.editor.model.FilterPreset
import com.lumora.gallery.ui.editor.model.MarkupType
import com.lumora.gallery.ui.editor.model.TextAnnotation
import com.lumora.gallery.ui.editor.util.ColorMatrixGenerator
import com.lumora.gallery.ui.editor.util.ImageDecoderUtil
import com.lumora.gallery.ui.editor.util.ImageSaverUtil
import com.lumora.gallery.ui.theme.AccentGold
import com.lumora.gallery.ui.theme.DarkSurface
import com.lumora.gallery.ui.theme.PureBlack
import com.lumora.gallery.ui.theme.TextPrimary
import com.lumora.gallery.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun PhotoEditorScreen(
    mediaItem: MediaItem,
    onDismiss: () -> Unit,
    onSaved: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Editor State and History
    var currentState by remember { mutableStateOf(EditorState()) }
    val undoStack = remember { mutableStateListOf<EditorState>() }
    val redoStack = remember { mutableStateListOf<EditorState>() }

    // UI Tools selection
    var selectedTool by remember { mutableStateOf(EditorTool.NONE) }
    var selectedAdjustment by remember { mutableStateOf(AdjustmentType.BRIGHTNESS) }
    var selectedMarkupType by remember { mutableStateOf(MarkupType.PEN) }
    var selectedMarkupColor by remember { mutableStateOf(Color.White) }
    var markupStrokeWidth by remember { mutableFloatStateOf(10f) }

    // Flags & Dialogs
    var isComparingOriginal by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showAddTextDialog by remember { mutableStateOf(false) }

    // Load preview bitmap on launch
    LaunchedEffect(mediaItem.uri) {
        isLoading = true
        previewBitmap = ImageDecoderUtil.loadPreviewBitmap(context, mediaItem.uri)
        isLoading = false
    }

    fun pushState(newState: EditorState) {
        undoStack.add(currentState)
        redoStack.clear()
        currentState = newState
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(currentState)
            currentState = prev
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(currentState)
            currentState = next
        }
    }

    BackHandler {
        if (currentState.hasModifications()) {
            showDiscardDialog = true
        } else {
            onDismiss()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = AccentGold
            )
            return@Box
        }

        val bitmap = previewBitmap
        if (bitmap == null) {
            Text(
                text = "Failed to load image for editing",
                color = TextSecondary,
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }

        // ==========================================
        // Main Editor Layout
        // ==========================================
        Column(modifier = Modifier.fillMaxSize()) {
            // --- Top App Bar ---
            EditorTopBar(
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                onClose = {
                    if (currentState.hasModifications()) {
                        showDiscardDialog = true
                    } else {
                        onDismiss()
                    }
                },
                onUndo = { undo() },
                onRedo = { redo() },
                onCompareStart = { isComparingOriginal = true },
                onCompareEnd = { isComparingOriginal = false },
                onSave = { showExportDialog = true }
            )

            // --- Center Preview Canvas ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                EditorCanvasView(
                    bitmap = bitmap,
                    editorState = if (isComparingOriginal) EditorState() else currentState,
                    selectedTool = selectedTool,
                    markupType = selectedMarkupType,
                    markupColor = selectedMarkupColor,
                    markupStrokeWidth = markupStrokeWidth,
                    onStrokeAdded = { newStroke ->
                        pushState(currentState.copy(strokes = currentState.strokes + newStroke))
                    }
                )

                // Comparing pill overlay
                if (isComparingOriginal) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .background(PureBlack.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("ORIGINAL", color = AccentGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- Tool Sub-panels ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
            ) {
                when (selectedTool) {
                    EditorTool.CROP_ROTATE -> {
                        CropRotatePanel(
                            rotation = currentState.rotationDegrees,
                            flipH = currentState.flipHorizontal,
                            flipV = currentState.flipVertical,
                            selectedRatio = currentState.cropRatio,
                            onRotate = {
                                pushState(currentState.copy(rotationDegrees = (currentState.rotationDegrees + 90) % 360))
                            },
                            onFlipHorizontal = {
                                pushState(currentState.copy(flipHorizontal = !currentState.flipHorizontal))
                            },
                            onFlipVertical = {
                                pushState(currentState.copy(flipVertical = !currentState.flipVertical))
                            },
                            onSelectRatio = { ratio ->
                                pushState(currentState.copy(cropRatio = ratio))
                            }
                        )
                    }
                    EditorTool.ADJUST -> {
                        AdjustPanel(
                            selectedType = selectedAdjustment,
                            adjustments = currentState.adjustments,
                            onSelectType = { selectedAdjustment = it },
                            onValueChange = { newVal ->
                                val adj = currentState.adjustments
                                val updated = when (selectedAdjustment) {
                                    AdjustmentType.BRIGHTNESS -> adj.copy(brightness = newVal)
                                    AdjustmentType.CONTRAST -> adj.copy(contrast = newVal)
                                    AdjustmentType.SATURATION -> adj.copy(saturation = newVal)
                                    AdjustmentType.WARMTH -> adj.copy(warmth = newVal)
                                    AdjustmentType.EXPOSURE -> adj.copy(exposure = newVal)
                                    AdjustmentType.VIGNETTE -> adj.copy(vignette = newVal)
                                }
                                currentState = currentState.copy(adjustments = updated)
                            },
                            onValueChangeFinished = {
                                pushState(currentState)
                            }
                        )
                    }
                    EditorTool.FILTERS -> {
                        FiltersPanel(
                            currentPreset = currentState.filterPreset,
                            currentIntensity = currentState.filterIntensity,
                            onSelectPreset = { preset ->
                                pushState(currentState.copy(filterPreset = preset, filterIntensity = 1f))
                            },
                            onIntensityChange = { intensity ->
                                currentState = currentState.copy(filterIntensity = intensity)
                            },
                            onIntensityFinished = {
                                pushState(currentState)
                            }
                        )
                    }
                    EditorTool.MARKUP -> {
                        MarkupPanel(
                            selectedMarkupType = selectedMarkupType,
                            selectedColor = selectedMarkupColor,
                            strokeWidth = markupStrokeWidth,
                            onSelectType = { selectedMarkupType = it },
                            onSelectColor = { selectedMarkupColor = it },
                            onStrokeWidthChange = { markupStrokeWidth = it }
                        )
                    }
                    EditorTool.TEXT -> {
                        TextToolPanel(
                            onAddTextClick = { showAddTextDialog = true },
                            hasAnnotations = currentState.textAnnotations.isNotEmpty(),
                            onClearAllText = {
                                pushState(currentState.copy(textAnnotations = emptyList()))
                            }
                        )
                    }
                    EditorTool.NONE -> {
                        // Empty placeholder
                    }
                }
            }

            // --- Bottom Navigation Categories Tray ---
            EditorBottomTray(
                selectedTool = selectedTool,
                onSelectTool = { tool ->
                    selectedTool = if (selectedTool == tool) EditorTool.NONE else tool
                }
            )
        }

        // --- Loading Spinner when saving ---
        if (isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PureBlack.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentGold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Saving high-resolution image...", color = TextPrimary, fontSize = 14.sp)
                }
            }
        }
    }

    // --- Discard Dialog ---
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Edits?", color = TextPrimary) },
            text = { Text("You have unsaved edits. Are you sure you want to discard them?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onDismiss()
                }) {
                    Text("Discard", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // --- Export / Save Dialog ---
    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onConfirmExport = { format, quality, saveAsCopy, eraseExif ->
                showExportDialog = false
                isSaving = true
                scope.launch {
                    val saved = ImageSaverUtil.saveEditedImage(
                        context = context,
                        sourceUri = mediaItem.uri,
                        state = currentState,
                        format = format,
                        quality = quality,
                        saveAsCopy = saveAsCopy,
                        eraseExif = eraseExif
                    )
                    isSaving = false
                    if (saved != null) {
                        onSaved(saved)
                    }
                }
            }
        )
    }

    // --- Add Text Dialog ---
    if (showAddTextDialog) {
        AddTextAnnotationDialog(
            onDismiss = { showAddTextDialog = false },
            onAddText = { newText, textColor, hasBg ->
                showAddTextDialog = false
                val annotation = TextAnnotation(
                    text = newText,
                    color = textColor,
                    hasBackground = hasBg
                )
                pushState(currentState.copy(textAnnotations = currentState.textAnnotations + annotation))
            }
        )
    }
}

@Composable
private fun EditorTopBar(
    canUndo: Boolean,
    canRedo: Boolean,
    onClose: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCompareStart: () -> Unit,
    onCompareEnd: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(
                    Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) Color.White else Color.White.copy(alpha = 0.3f)
                )
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(
                    Icons.AutoMirrored.Rounded.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) Color.White else Color.White.copy(alpha = 0.3f)
                )
            }
            // Press & Hold Compare Button
            Box(
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onCompareStart()
                                tryAwaitRelease()
                                onCompareEnd()
                            }
                        )
                    }
                    .padding(8.dp)
            ) {
                Icon(Icons.Rounded.Visibility, contentDescription = "Compare", tint = Color.White)
            }
        }

        Button(
            onClick = onSave,
            colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = PureBlack),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text("Save", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun EditorCanvasView(
    bitmap: Bitmap,
    editorState: EditorState,
    selectedTool: EditorTool,
    markupType: MarkupType,
    markupColor: Color,
    markupStrokeWidth: Float,
    onStrokeAdded: (DrawingStroke) -> Unit
) {
    val currentPoints = remember { mutableStateListOf<Offset>() }

    val composeColorMatrix = remember(editorState.adjustments, editorState.filterPreset, editorState.filterIntensity) {
        ColorMatrixGenerator.createComposeColorMatrix(
            adjustments = editorState.adjustments,
            preset = editorState.filterPreset,
            intensity = editorState.filterIntensity
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(selectedTool, markupType, markupColor, markupStrokeWidth) {
                if (selectedTool == EditorTool.MARKUP) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPoints.clear()
                            currentPoints.add(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentPoints.add(change.position)
                        },
                        onDragEnd = {
                            if (currentPoints.size > 1) {
                                onStrokeAdded(
                                    DrawingStroke(
                                        points = currentPoints.toList(),
                                        color = markupColor,
                                        strokeWidth = markupStrokeWidth,
                                        markupType = markupType
                                    )
                                )
                            }
                            currentPoints.clear()
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
        val rotation = editorState.rotationDegrees.toFloat()
        val scaleX = if (editorState.flipHorizontal) -1f else 1f
        val scaleY = if (editorState.flipVertical) -1f else 1f

        // Render Image with Color Filter & Transformations
        androidx.compose.foundation.Image(
            bitmap = imageBitmap,
            contentDescription = "Edited Preview",
            colorFilter = ColorFilter.colorMatrix(composeColorMatrix),
            modifier = Modifier
                .rotate(rotation)
                .scale(scaleX, scaleY)
        )

        // Overlay Canvas for Drawing Strokes and Text
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Render committed strokes
            for (stroke in editorState.strokes) {
                if (stroke.points.size < 2) continue
                val strokeColor = when (stroke.markupType) {
                    MarkupType.HIGHLIGHTER -> stroke.color.copy(alpha = 0.4f)
                    MarkupType.BLUR -> Color.Gray.copy(alpha = 0.8f)
                    else -> stroke.color
                }
                for (i in 0 until stroke.points.size - 1) {
                    drawLine(
                        color = strokeColor,
                        start = stroke.points[i],
                        end = stroke.points[i + 1],
                        strokeWidth = stroke.strokeWidth,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }

            // Render stroke currently being drawn
            if (currentPoints.size > 1) {
                val strokeColor = when (markupType) {
                    MarkupType.HIGHLIGHTER -> markupColor.copy(alpha = 0.4f)
                    MarkupType.BLUR -> Color.Gray.copy(alpha = 0.8f)
                    else -> markupColor
                }
                for (i in 0 until currentPoints.size - 1) {
                    drawLine(
                        color = strokeColor,
                        start = currentPoints[i],
                        end = currentPoints[i + 1],
                        strokeWidth = markupStrokeWidth,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorBottomTray(
    selectedTool: EditorTool,
    onSelectTool: (EditorTool) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PureBlack)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrayItem(
            icon = Icons.Rounded.Crop,
            label = "Crop/Rotate",
            isSelected = selectedTool == EditorTool.CROP_ROTATE,
            onClick = { onSelectTool(EditorTool.CROP_ROTATE) }
        )
        TrayItem(
            icon = Icons.Rounded.Tune,
            label = "Adjust",
            isSelected = selectedTool == EditorTool.ADJUST,
            onClick = { onSelectTool(EditorTool.ADJUST) }
        )
        TrayItem(
            icon = Icons.Rounded.AutoFixHigh,
            label = "Filters",
            isSelected = selectedTool == EditorTool.FILTERS,
            onClick = { onSelectTool(EditorTool.FILTERS) }
        )
        TrayItem(
            icon = Icons.Rounded.Edit,
            label = "Markup",
            isSelected = selectedTool == EditorTool.MARKUP,
            onClick = { onSelectTool(EditorTool.MARKUP) }
        )
        TrayItem(
            icon = Icons.Rounded.TextFields,
            label = "Text",
            isSelected = selectedTool == EditorTool.TEXT,
            onClick = { onSelectTool(EditorTool.TEXT) }
        )
    }
}

@Composable
private fun TrayItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) AccentGold else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isSelected) AccentGold else Color.White.copy(alpha = 0.7f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun CropRotatePanel(
    rotation: Int,
    flipH: Boolean,
    flipV: Boolean,
    selectedRatio: CropRatio,
    onRotate: () -> Unit,
    onFlipHorizontal: () -> Unit,
    onFlipVertical: () -> Unit,
    onSelectRatio: (CropRatio) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = onRotate) {
                Icon(Icons.Rounded.RotateRight, contentDescription = "Rotate 90", tint = Color.White)
            }
            IconButton(onClick = onFlipHorizontal) {
                Icon(Icons.Rounded.Flip, contentDescription = "Flip Horizontal", tint = if (flipH) AccentGold else Color.White)
            }
            IconButton(onClick = onFlipVertical) {
                Icon(Icons.Rounded.Flip, contentDescription = "Flip Vertical", tint = if (flipV) AccentGold else Color.White, modifier = Modifier.rotate(90f))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CropRatio.entries.forEach { ratio ->
                val isSelected = selectedRatio == ratio
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) AccentGold else DarkSurface)
                        .border(1.dp, if (isSelected) AccentGold else Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable { onSelectRatio(ratio) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = ratio.displayName,
                        fontSize = 12.sp,
                        color = if (isSelected) PureBlack else TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun AdjustPanel(
    selectedType: AdjustmentType,
    adjustments: Adjustments,
    onSelectType: (AdjustmentType) -> Unit,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    val currentValue = when (selectedType) {
        AdjustmentType.BRIGHTNESS -> adjustments.brightness
        AdjustmentType.CONTRAST -> adjustments.contrast
        AdjustmentType.SATURATION -> adjustments.saturation
        AdjustmentType.WARMTH -> adjustments.warmth
        AdjustmentType.EXPOSURE -> adjustments.exposure
        AdjustmentType.VIGNETTE -> adjustments.vignette
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(selectedType.displayName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("${currentValue.toInt()}", color = AccentGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Slider(
            value = currentValue,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = selectedType.min..selectedType.max,
            colors = SliderDefaults.colors(
                thumbColor = AccentGold,
                activeTrackColor = AccentGold,
                inactiveTrackColor = Color.DarkGray
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdjustmentType.entries.forEach { type ->
                val isSelected = selectedType == type
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) AccentGold else PureBlack)
                        .clickable { onSelectType(type) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = type.displayName,
                        fontSize = 12.sp,
                        color = if (isSelected) PureBlack else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun FiltersPanel(
    currentPreset: FilterPreset,
    currentIntensity: Float,
    onSelectPreset: (FilterPreset) -> Unit,
    onIntensityChange: (Float) -> Unit,
    onIntensityFinished: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        if (currentPreset != FilterPreset.ORIGINAL) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Intensity", color = TextSecondary, fontSize = 12.sp)
                Text("${(currentIntensity * 100).toInt()}%", color = AccentGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = currentIntensity,
                onValueChange = onIntensityChange,
                onValueChangeFinished = onIntensityFinished,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = AccentGold,
                    activeTrackColor = AccentGold,
                    inactiveTrackColor = Color.DarkGray
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterPreset.entries.forEach { preset ->
                val isSelected = currentPreset == preset
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectPreset(preset) }
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) AccentGold else Color(0xFF2A2A2A))
                            .border(2.dp, if (isSelected) AccentGold else Color.Transparent, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset.displayName.take(3).uppercase(),
                            color = if (isSelected) PureBlack else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = preset.displayName,
                        fontSize = 11.sp,
                        color = if (isSelected) AccentGold else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkupPanel(
    selectedMarkupType: MarkupType,
    selectedColor: Color,
    strokeWidth: Float,
    onSelectType: (MarkupType) -> Unit,
    onSelectColor: (Color) -> Unit,
    onStrokeWidthChange: (Float) -> Unit
) {
    val palette = listOf(
        Color.White,
        AccentGold,
        Color(0xFFFF5252), // Red
        Color(0xFF4CAF50), // Green
        Color(0xFF2196F3), // Blue
        Color.Black
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            MarkupType.entries.forEach { type ->
                val isSelected = selectedMarkupType == type
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) AccentGold else PureBlack)
                        .clickable { onSelectType(type) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = type.name,
                        fontSize = 11.sp,
                        color = if (isSelected) PureBlack else TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Color Palette
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            palette.forEach { color ->
                val isSelected = selectedColor == color
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            2.dp,
                            if (isSelected) AccentGold else Color.DarkGray,
                            CircleShape
                        )
                        .clickable { onSelectColor(color) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = if (color == Color.White || color == AccentGold) PureBlack else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TextToolPanel(
    onAddTextClick: () -> Unit,
    hasAnnotations: Boolean,
    onClearAllText: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = onAddTextClick,
            colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = PureBlack),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Add Text", fontWeight = FontWeight.Bold)
        }

        if (hasAnnotations) {
            TextButton(onClick = onClearAllText) {
                Text("Clear All Text", color = Color(0xFFFF5252))
            }
        }
    }
}

@Composable
private fun ExportDialog(
    onDismiss: () -> Unit,
    onConfirmExport: (format: ExportFormat, quality: Int, saveAsCopy: Boolean, eraseExif: Boolean) -> Unit
) {
    var format by remember { mutableStateOf(ExportFormat.JPEG) }
    var quality by remember { mutableFloatStateOf(90f) }
    var saveAsCopy by remember { mutableStateOf(true) }
    var eraseExif by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Image", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Format", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportFormat.entries.forEach { fmt ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { format = fmt }
                        ) {
                            RadioButton(
                                selected = format == fmt,
                                onClick = { format = fmt },
                                colors = RadioButtonDefaults.colors(selectedColor = AccentGold)
                            )
                            Text(fmt.name, color = TextPrimary, fontSize = 13.sp)
                        }
                    }
                }

                if (format != ExportFormat.PNG) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Quality", color = TextSecondary, fontSize = 12.sp)
                        Text("${quality.toInt()}%", color = AccentGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = quality,
                        onValueChange = { quality = it },
                        valueRange = 50f..100f,
                        colors = SliderDefaults.colors(thumbColor = AccentGold, activeTrackColor = AccentGold)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { saveAsCopy = !saveAsCopy },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = saveAsCopy,
                        onClick = { saveAsCopy = true },
                        colors = RadioButtonDefaults.colors(selectedColor = AccentGold)
                    )
                    Text("Save as new copy", color = TextPrimary, fontSize = 13.sp)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { saveAsCopy = !saveAsCopy },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !saveAsCopy,
                        onClick = { saveAsCopy = false },
                        colors = RadioButtonDefaults.colors(selectedColor = AccentGold)
                    )
                    Text("Overwrite original", color = TextPrimary, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmExport(format, quality.toInt(), saveAsCopy, eraseExif) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = PureBlack)
            ) {
                Text("Export", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}

@Composable
private fun AddTextAnnotationDialog(
    onDismiss: () -> Unit,
    onAddText: (text: String, color: Color, hasBg: Boolean) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color.White) }
    var hasBackground by remember { mutableStateOf(true) }

    val colors = listOf(Color.White, AccentGold, Color(0xFFFF5252), Color(0xFF4CAF50), Color(0xFF2196F3))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Text", color = TextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Type something...", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(2.dp, if (selectedColor == c) AccentGold else Color.DarkGray, CircleShape)
                                .clickable { selectedColor = c }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onAddText(text, selectedColor, hasBackground) },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = PureBlack)
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}
