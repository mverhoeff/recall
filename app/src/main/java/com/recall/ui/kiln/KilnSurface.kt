package com.recall.ui.kiln

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln
import com.recall.ui.theme.KilnColors
import com.recall.ui.theme.LocalContentColor
import com.recall.ui.theme.LocalOnGround
import com.recall.ui.theme.kilnDepth
import com.recall.ui.theme.resolve

enum class SurfaceTone { Surface, Raised, Accent, Pop }

@Composable
fun KilnSurface(
    modifier: Modifier = Modifier,
    depth: Depth = Depth.Mold2,
    tone: SurfaceTone = SurfaceTone.Surface,
    shape: Shape = RoundedCornerShape(26.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = Kiln.colors
    val interactionSource = remember { MutableInteractionSource() }
    val press = rememberPressState(depth, interactionSource)
    val requested = if (onClick != null) press.depth else depth
    val resolved = requested.resolve(LocalOnGround.current)

    val (base, contentColor) = when (tone) {
        SurfaceTone.Surface -> colors.surface to colors.ink
        SurfaceTone.Raised -> colors.surface2 to colors.ink
        SurfaceTone.Accent -> colors.accent to colors.accentInk
        SurfaceTone.Pop -> colors.pop to colors.popInk
    }
    val fill = when (tone) {
        SurfaceTone.Surface, SurfaceTone.Raised -> neutralFill(requested, resolved, base, colors)
        SurfaceTone.Accent, SurfaceTone.Pop -> base
    }

    val interaction = if (onClick != null) {
        Modifier
            .kilnFocusRing(interactionSource, shape)
            .graphicsLayer {
                scaleX = press.scale
                scaleY = press.scale
            }
    } else {
        Modifier
    }
    Box(
        modifier
            .then(interaction)
            .kilnDepth(resolved, shape, fill, colors, animationSpec = press.depthSpec)
            .then(if (onClick != null) Modifier.kilnPressable(interactionSource, onClick = onClick) else Modifier),
    ) {
        CompositionLocalProvider(LocalOnGround provides false, LocalContentColor provides contentColor) {
            content()
        }
    }
}

/**
 * Fill for the neutral clay tones, following the stacking rule: a dip is a well on the ground and a
 * flat `line` tint anywhere else, and a mold lifted off the ground floats as surface2.
 */
internal fun neutralFill(requested: Depth, resolved: Depth, rest: Color, colors: KilnColors): Color = when {
    requested.isDip -> if (resolved.isDip) colors.well else colors.line
    requested.isMold && resolved != requested -> colors.surface2
    else -> rest
}

private val Depth.isDip get() = this == Depth.DipSm || this == Depth.Dip
private val Depth.isMold get() = this == Depth.Mold1 || this == Depth.Mold2 || this == Depth.Mold3
