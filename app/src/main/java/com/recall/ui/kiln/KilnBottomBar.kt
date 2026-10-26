package com.recall.ui.kiln

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln
import com.recall.ui.theme.LocalOnGround
import com.recall.ui.theme.resolve
import kotlinx.coroutines.launch

private val ItemHeight = 52.dp

@Immutable
data class BottomBarItem(val icon: ImageVector, val label: String)

/**
 * The floating pill of top-level destinations. Placement above the system bars is up to the caller.
 * Like [KilnTabs], the active item is one accent pill that slides over, stretching towards the new
 * item and settling back to size.
 */
@Composable
fun KilnBottomBar(
    items: List<BottomBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Kiln.colors
    val motion = Kiln.motion
    // Edges of the pill, measured in items: the selected item spans [index, index + 1].
    val start = remember { Animatable(selectedIndex.toFloat()) }
    val end = remember { Animatable(selectedIndex + 1f) }
    LaunchedEffect(selectedIndex) {
        val forward = selectedIndex >= start.value
        val (leading, trailing) = if (forward) end to start else start to end
        launch { leading.animateTo(if (forward) selectedIndex + 1f else selectedIndex.toFloat(), motion.base()) }
        launch { trailing.animateTo(if (forward) selectedIndex.toFloat() else selectedIndex + 1f, motion.slow()) }
    }

    KilnSurface(
        modifier.fillMaxWidth(),
        depth = Depth.Float3,
        tone = SurfaceTone.Raised,
        shape = CircleShape,
    ) {
        BoxWithConstraints(Modifier.padding(6.dp)) {
            val step = maxWidth / items.size
            Box(
                Modifier
                    .offset(x = step * start.value)
                    .width(step * (end.value - start.value))
                    .height(ItemHeight)
                    .background(colors.accent, CircleShape),
            )
            Row(Modifier.selectableGroup()) {
                items.forEachIndexed { index, item ->
                    // How much of this item the pill covers, so its icon and label turn as the pill passes.
                    val cover = (minOf(end.value, index + 1f) - maxOf(start.value, index.toFloat())).coerceIn(0f, 1f)
                    BarItem(item, index == selectedIndex, cover, onClick = { onSelect(index) }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BarItem(item: BottomBarItem, selected: Boolean, cover: Float, onClick: () -> Unit, modifier: Modifier) {
    val colors = Kiln.colors
    val motion = Kiln.motion
    val interactionSource = remember { MutableInteractionSource() }
    val press = rememberPressState(Depth.Flat, interactionSource)
    val resolved = press.depth.resolve(LocalOnGround.current)

    // The accent pill is drawn behind the row, so only a pressed item that isn't selected gets a fill of its own.
    val fill by animateColorAsState(
        if (selected) Color.Transparent else neutralFill(press.depth, resolved, Color.Transparent, colors),
        motion.base(),
        label = "bar item fill",
    )
    val content = lerp(colors.ink2, colors.accentInk, cover)

    Column(
        modifier
            .kilnFocusRing(interactionSource, CircleShape)
            .graphicsLayer {
                scaleX = press.scale
                scaleY = press.scale
            }
            .background(fill, CircleShape)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .height(ItemHeight),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        KilnIcon(item.icon, contentDescription = null, size = 22.dp, tint = content)
        KilnText(
            item.label,
            style = Kiln.type.body.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 14.sp),
            color = content,
            maxLines = 1,
        )
    }
}
