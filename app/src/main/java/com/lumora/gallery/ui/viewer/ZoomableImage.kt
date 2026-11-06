package com.lumora.gallery.ui.viewer

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.math.abs

/**
 * Butter-smooth zoomable & swipeable image container inspired by HyperOS Gallery.
 *
 * Capabilities:
 * - Fluid pinch-to-zoom with spring boundaries and zero choppiness.
 * - Double-tap with animated spring zoom (1x <-> 2.5x).
 * - Smooth pass-through of horizontal swipes when unzoomed (powering HorizontalPager).
 * - Swipe-up gesture to reveal media details sheet.
 * - Swipe-down gesture to dismiss viewer.
 * - Single tap to toggle chrome overlays.
 */
@Composable
fun ZoomableImage(
    uri: Uri,
    onSingleTap: () -> Unit,
    onSwipeUp: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scaleAnim = remember { Animatable(1f) }
    val offsetXAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(uri) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var zoom = 1f
                    var pan = Offset.Zero
                    var pastTouchSlop = false
                    val touchSlop = viewConfiguration.touchSlop
                    var totalDragY = 0f
                    var totalDragX = 0f
                    var isTap = true
                    var lastUpTime = 0L

                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.any { it.isConsumed }
                        if (canceled) break

                        if (event.changes.size > 1) {
                            // Multi-touch pinch gesture
                            isTap = false
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            zoom *= zoomChange
                            pan += panChange

                            scope.launch {
                                val currentScale = scaleAnim.value
                                val targetScale = (currentScale * zoomChange).coerceIn(0.85f, 6f)
                                scaleAnim.snapTo(targetScale)

                                if (targetScale > 1f) {
                                    val maxOffsetX = (size.width * (targetScale - 1f)) / 2f
                                    val maxOffsetY = (size.height * (targetScale - 1f)) / 2f
                                    val newX = (offsetXAnim.value + panChange.x).coerceIn(-maxOffsetX, maxOffsetX)
                                    val newY = (offsetYAnim.value + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)
                                    offsetXAnim.snapTo(newX)
                                    offsetYAnim.snapTo(newY)
                                }
                            }

                            event.changes.forEach { it.consume() }
                        } else {
                            // Single pointer
                            val change = event.changes.firstOrNull() ?: break
                            val positionChange = change.positionChange()

                            if (scaleAnim.value > 1.05f) {
                                // Pan around zoomed photo
                                isTap = false
                                scope.launch {
                                    val maxOffsetX = (size.width * (scaleAnim.value - 1f)) / 2f
                                    val maxOffsetY = (size.height * (scaleAnim.value - 1f)) / 2f
                                    val newX = (offsetXAnim.value + positionChange.x).coerceIn(-maxOffsetX, maxOffsetX)
                                    val newY = (offsetYAnim.value + positionChange.y).coerceIn(-maxOffsetY, maxOffsetY)
                                    offsetXAnim.snapTo(newX)
                                    offsetYAnim.snapTo(newY)
                                }
                                change.consume()
                            } else {
                                // Scale is 1x (unzoomed)
                                totalDragX += positionChange.x
                                totalDragY += positionChange.y

                                if (!pastTouchSlop) {
                                    val dragMagnitude = abs(totalDragX) + abs(totalDragY)
                                    if (dragMagnitude > touchSlop) {
                                        pastTouchSlop = true
                                        isTap = false
                                    }
                                }

                                if (pastTouchSlop) {
                                    // If vertical motion is dominant, handle swipe up / down
                                    if (abs(totalDragY) > abs(totalDragX) * 1.5f) {
                                        change.consume()
                                    }
                                    // If horizontal is dominant, DO NOT consume so HorizontalPager handles page flips!
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    // Gesture ended
                    if (scaleAnim.value < 1f) {
                        // Spring back to 1x
                        scope.launch {
                            scaleAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            offsetXAnim.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy))
                            offsetYAnim.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy))
                        }
                    }

                    if (isTap && !pastTouchSlop) {
                        onSingleTap()
                    } else if (scaleAnim.value <= 1.05f) {
                        if (totalDragY < -120f) {
                            // Swiped UP -> Show Media Details Sheet!
                            onSwipeUp()
                        } else if (totalDragY > 160f) {
                            // Swiped DOWN -> Dismiss Viewer!
                            onDismissRequest()
                        }
                    }
                }
            }
            // Double-tap detector
            .pointerInput(uri) {
                awaitEachGesture {
                    var down = awaitFirstDown(requireUnconsumed = false)
                    val firstDownTime = System.currentTimeMillis()
                    var isDouble = false

                    // Wait for release
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })

                    val upTime = System.currentTimeMillis()
                    if (upTime - firstDownTime < 300) {
                        // Look for second tap within 300ms
                        try {
                            val secondDown = withTimeout(300) {
                                awaitFirstDown(requireUnconsumed = false)
                            }
                            // Double tap detected!
                            isDouble = true
                            secondDown.consume()

                            scope.launch {
                                if (scaleAnim.value > 1.2f) {
                                    scaleAnim.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow))
                                    offsetXAnim.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                    offsetYAnim.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                } else {
                                    scaleAnim.animateTo(2.5f, spring(stiffness = Spring.StiffnessMediumLow))
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(uri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                    translationX = offsetXAnim.value
                    translationY = offsetYAnim.value
                }
        )
    }
}
