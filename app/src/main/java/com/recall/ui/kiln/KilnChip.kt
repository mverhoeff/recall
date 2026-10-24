package com.recall.ui.kiln

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln
import com.recall.ui.theme.LocalOnGround
import com.recall.ui.theme.kilnDepth
import com.recall.ui.theme.resolve

@Composable
fun KilnChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Kiln.colors
    val onGround = LocalOnGround.current
    val interactionSource = remember { MutableInteractionSource() }
    val press = rememberPressState(Depth.Float1, interactionSource, forcePressed = selected)
    val resolved = press.depth.resolve(onGround)
    val dipped = press.depth == Depth.DipSm
    val fill = when {
        !dipped -> colors.surface2
        onGround -> colors.well
        selected -> colors.accentSoft
        else -> colors.line
    }
    val textColor = if (selected && onGround) colors.accent else colors.ink

    Box(
        modifier
            .semantics { this.selected = selected }
            .kilnPressable(interactionSource, onClick = onClick)
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .kilnFocusRing(interactionSource, CircleShape)
                .graphicsLayer {
                    scaleX = press.scale
                    scaleY = press.scale
                }
                .kilnDepth(resolved, CircleShape, fill, colors, animationSpec = press.depthSpec)
                .height(40.dp)
                .widthIn(min = 40.dp)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            KilnText(
                text,
                style = Kiln.type.body.copy(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp),
                color = textColor,
                maxLines = 1,
            )
        }
    }
}
