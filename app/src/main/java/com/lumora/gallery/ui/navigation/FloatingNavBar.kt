package com.lumora.gallery.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarViewMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumora.gallery.ui.theme.DarkBorder
import com.lumora.gallery.ui.theme.DarkSurfaceElevated
import com.lumora.gallery.ui.theme.DarkSurfaceHighlight
import com.lumora.gallery.ui.theme.FrostedPillBackground
import com.lumora.gallery.ui.theme.SoothingBlue
import com.lumora.gallery.ui.theme.TextPrimary
import com.lumora.gallery.ui.theme.TextSecondary

enum class NavTab {
    PHOTOS,
    ALBUMS
}

enum class TimeScale {
    YEARS,
    MONTHS,
    DAYS
}

enum class NavMode {
    TABS,
    TIME_SCALE
}

/**
 * HyperOS / iOS style floating pill navigation bar.
 *
 * In TABS mode:
 * - Left circle button: toggles Years | Months | Days view
 * - Center pill: Photos | Albums
 * - Right circle button: Sort & Filter (in Photos) or Add (in Albums)
 *
 * In TIME_SCALE mode (active when user taps bottom-left button):
 * - Left circle button: Close (X) to return to normal Photos grid
 * - Center pill: Years | Months | Days
 * - Right circle button: Sort & Filter
 */
@Composable
fun FloatingNavBar(
    navMode: NavMode = NavMode.TABS,
    currentTab: NavTab,
    currentTimeScale: TimeScale = TimeScale.DAYS,
    onTabSelected: (NavTab) -> Unit,
    onTimeScaleSelected: (TimeScale) -> Unit = {},
    onToggleTimeScaleMode: () -> Unit = {},
    onRightActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ==========================================
        // Left Action Button
        // In TABS mode -> Layout / Time scale icon
        // In TIME_SCALE mode -> Close (X) button
        // ==========================================
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(DarkSurfaceElevated)
                .border(1.dp, DarkBorder, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleTimeScaleMode
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = navMode == NavMode.TIME_SCALE,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                },
                label = "nav_left_icon"
            ) { isTimeScaleMode ->
                Icon(
                    imageVector = if (isTimeScaleMode) Icons.Rounded.Close else Icons.Rounded.CalendarViewMonth,
                    contentDescription = if (isTimeScaleMode) "Exit time view" else "Years, Months, Days view",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // ==========================================
        // Center Floating Pill
        // Morphs between [Photos | Albums] and [Years | Months | Days]
        // ==========================================
        Box(
            modifier = Modifier
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(FrostedPillBackground)
                .border(1.dp, DarkBorder, RoundedCornerShape(26.dp))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = navMode,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                },
                label = "center_pill_mode"
            ) { mode ->
                if (mode == NavMode.TIME_SCALE) {
                    // --- Years | Months | Days Pill ---
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TimeScalePillItem(
                            title = "Years",
                            selected = currentTimeScale == TimeScale.YEARS,
                            onClick = { onTimeScaleSelected(TimeScale.YEARS) }
                        )
                        TimeScalePillItem(
                            title = "Months",
                            selected = currentTimeScale == TimeScale.MONTHS,
                            onClick = { onTimeScaleSelected(TimeScale.MONTHS) }
                        )
                        TimeScalePillItem(
                            title = "Days",
                            selected = currentTimeScale == TimeScale.DAYS,
                            onClick = { onTimeScaleSelected(TimeScale.DAYS) }
                        )
                    }
                } else {
                    // --- Photos | Albums Pill ---
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PillTabItem(
                            title = "Photos",
                            icon = Icons.Rounded.Photo,
                            selected = currentTab == NavTab.PHOTOS,
                            onClick = { onTabSelected(NavTab.PHOTOS) }
                        )
                        PillTabItem(
                            title = "Albums",
                            icon = Icons.Rounded.PhotoAlbum,
                            selected = currentTab == NavTab.ALBUMS,
                            onClick = { onTabSelected(NavTab.ALBUMS) }
                        )
                    }
                }
            }
        }

        // ==========================================
        // Right Action Button
        // Photos tab -> Filter & sort
        // Albums tab -> Add album (+)
        // ==========================================
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(DarkSurfaceElevated)
                .border(1.dp, DarkBorder, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onRightActionClick
                ),
            contentAlignment = Alignment.Center
        ) {
            val rightIcon = if (currentTab == NavTab.ALBUMS && navMode == NavMode.TABS) {
                Icons.Rounded.Add
            } else {
                Icons.Rounded.FilterList
            }
            Icon(
                imageVector = rightIcon,
                contentDescription = "Action",
                tint = TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun PillTabItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) DarkSurfaceHighlight else Color.Transparent,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "pill_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) TextPrimary else TextSecondary,
        label = "pill_content"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) SoothingBlue else contentColor,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun TimeScalePillItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) DarkSurfaceHighlight else Color.Transparent,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "time_pill_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) TextPrimary else TextSecondary,
        label = "time_pill_content"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected) SoothingBlue else contentColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
