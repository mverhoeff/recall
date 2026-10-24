package com.recall.ui.kiln

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.recall.ui.theme.ClayEasing
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln

fun Modifier.kilnPressable(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    onClick: () -> Unit,
): Modifier = clickable(
    interactionSource = interactionSource,
    indication = null,
    enabled = enabled,
    onClickLabel = onClickLabel,
    role = role,
    onClick = onClick,
)

@Immutable
data class PressState(val depth: Depth, val scale: Float, val depthSpec: AnimationSpec<Float>)

/**
 * Press motion: sink to DipSm and scale 0.98 quickly, then spring back to [rest].
 * [forcePressed] keeps the dipped look without the squeeze, for selected options.
 */
@Composable
fun rememberPressState(rest: Depth, interactionSource: InteractionSource, forcePressed: Boolean = false): PressState {
    val touching by interactionSource.collectIsPressedAsState()
    val down = touching || forcePressed
    val motion = Kiln.motion
    val scale by animateFloatAsState(
        targetValue = if (touching) 0.98f else 1f,
        animationSpec = if (touching) motion.fast() else motion.base(ClayEasing),
        label = "press scale",
    )
    // The clay overshoot is kept off the shadows, which would otherwise sink past their rest values.
    val depthSpec = if (down) motion.fast<Float>() else motion.base()
    return PressState(if (down) Depth.DipSm else rest, scale, depthSpec)
}

/** A 3dp accent ring 3dp outside [shape], shown while [interactionSource] has focus. */
@Composable
fun Modifier.kilnFocusRing(interactionSource: InteractionSource, shape: Shape): Modifier {
    val focused by interactionSource.collectIsFocusedAsState()
    val color = Kiln.colors.accent
    return drawBehind {
        if (!focused) return@drawBehind
        val stroke = 3.dp.toPx()
        val inset = 3.dp.toPx() + stroke / 2
        val ring = Size(size.width + inset * 2, size.height + inset * 2)
        translate(-inset, -inset) {
            drawOutline(shape.createOutline(ring, layoutDirection, this), color, style = Stroke(stroke))
        }
    }
}
