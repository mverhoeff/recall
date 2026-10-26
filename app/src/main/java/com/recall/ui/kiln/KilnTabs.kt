package com.recall.ui.kiln

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln
import com.recall.ui.theme.kilnDepth
import kotlinx.coroutines.launch

private val TabGap = 4.dp
private val PillHeight = 44.dp
private val TouchHeight = 48.dp

/**
 * Segmented tabs. The track is flat in every context, so it looks the same on the ground and in cards.
 * The selection is one clay pill that slides between tabs: its leading edge sets off first and the
 * trailing edge catches up, so it stretches towards the new tab and settles back to size.
 */
@Composable
fun KilnTabs(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Kiln.colors
    val motion = Kiln.motion
    // Edges of the pill, measured in tabs: the selected tab spans [index, index + 1].
    val start = remember { Animatable(selectedIndex.toFloat()) }
    val end = remember { Animatable(selectedIndex + 1f) }
    LaunchedEffect(selectedIndex) {
        val forward = selectedIndex >= start.value
        val (leading, trailing) = if (forward) end to start else start to end
        launch { leading.animateTo(if (forward) selectedIndex + 1f else selectedIndex.toFloat(), motion.base()) }
        launch { trailing.animateTo(if (forward) selectedIndex.toFloat() else selectedIndex + 1f, motion.slow()) }
    }

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .kilnDepth(Depth.Flat, CircleShape, colors.line, colors)
            // Tabs take touches over 48dp but draw a 44dp pill, so they reach 2dp into the track's padding.
            .padding(horizontal = 5.dp, vertical = 5.dp - (TouchHeight - PillHeight) / 2),
    ) {
        val step = (maxWidth - TabGap * (items.size - 1)) / items.size + TabGap
        val left = step * start.value
        val right = step * end.value - TabGap
        Box(
            Modifier
                .offset(x = left, y = (TouchHeight - PillHeight) / 2)
                .width(right - left)
                .height(PillHeight)
                .kilnDepth(Depth.Float1, CircleShape, colors.surface2, colors),
        )
        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(TabGap)) {
            items.forEachIndexed { index, item ->
                // How much of this tab the pill covers, so labels brighten as it passes over them.
                val cover = (minOf(end.value, index + 1f) - maxOf(start.value, index.toFloat())).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .weight(1f)
                        .height(TouchHeight)
                        .selectable(
                            selected = index == selectedIndex,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(index) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    KilnText(
                        item,
                        style = Kiln.type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp),
                        color = lerp(colors.ink2, colors.ink, cover),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
