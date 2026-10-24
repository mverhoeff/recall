package com.recall.ui.kiln

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln
import com.recall.ui.theme.kilnDepth

/** Segmented tabs. The track is flat in every context, so it looks the same on the ground and in cards. */
@Composable
fun KilnTabs(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Kiln.colors
    Row(
        modifier
            .fillMaxWidth()
            .kilnDepth(Depth.Flat, CircleShape, colors.line, colors)
            .padding(5.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            // Colours are blended at use, so a theme switch recolours at once instead of fading.
            val amount by animateFloatAsState(if (selected) 1f else 0f, Kiln.motion.base(), label = "tab")
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .kilnDepth(
                        if (selected) Depth.Float1 else Depth.Flat,
                        CircleShape,
                        lerp(Color.Transparent, colors.surface2, amount),
                        colors,
                        animationSpec = Kiln.motion.base(),
                    )
                    .selectable(
                        selected = selected,
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
                    color = lerp(colors.ink2, colors.ink, amount),
                    maxLines = 1,
                )
            }
        }
    }
}
