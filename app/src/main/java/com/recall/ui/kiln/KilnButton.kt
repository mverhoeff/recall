package com.recall.ui.kiln

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln
import com.recall.ui.theme.LocalContentColor
import com.recall.ui.theme.LocalOnGround
import com.recall.ui.theme.kilnDepth
import com.recall.ui.theme.resolve

enum class ButtonVariant { Primary, Secondary, Pop, Ghost }

enum class ButtonSize(val height: Dp, val padding: Dp) {
    Sm(48.dp, 16.dp),
    Md(48.dp, 22.dp),
    Lg(58.dp, 30.dp),
}

private val Pill = RoundedCornerShape(50)

@Composable
fun KilnButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    size: ButtonSize = ButtonSize.Md,
    enabled: Boolean = true,
    pressed: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    restDepth: Depth? = null,
    horizontalPadding: Dp = size.padding,
    /** Overrides the variant's text and icon colour, e.g. for a Ghost button that deletes. */
    contentColor: Color? = null,
) {
    val style = when (size) {
        ButtonSize.Sm -> Kiln.type.buttonSm
        ButtonSize.Md -> Kiln.type.button
        ButtonSize.Lg -> Kiln.type.buttonLg
    }
    ButtonFrame(onClick, modifier, variant, size, enabled, pressed, horizontalPadding, restDepth) {
        CompositionLocalProvider(LocalContentColor provides (contentColor ?: LocalContentColor.current)) {
            if (leadingIcon != null) KilnIcon(leadingIcon, contentDescription = null, size = 22.dp)
            KilnText(text, style = style, maxLines = 1)
            if (trailingIcon != null) KilnIcon(trailingIcon, contentDescription = null, size = 22.dp)
        }
    }
}

@Composable
fun KilnIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Secondary,
    size: ButtonSize = ButtonSize.Md,
    enabled: Boolean = true,
    pressed: Boolean = false,
) {
    ButtonFrame(
        onClick,
        modifier.semantics { this.contentDescription = contentDescription },
        variant, size, enabled, pressed, horizontalPadding = 0.dp,
    ) {
        KilnIcon(icon, contentDescription = null)
    }
}

@Composable
private fun ButtonFrame(
    onClick: () -> Unit,
    modifier: Modifier,
    variant: ButtonVariant,
    size: ButtonSize,
    enabled: Boolean,
    pressed: Boolean,
    horizontalPadding: Dp,
    restDepth: Depth? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = Kiln.colors
    val interactionSource = remember { MutableInteractionSource() }
    val rest = restDepth ?: if (variant == ButtonVariant.Ghost) Depth.Flat else Depth.Float1
    val press = rememberPressState(rest, interactionSource, forcePressed = pressed)
    val requested = if (enabled) press.depth else Depth.Flat
    val resolved = requested.resolve(LocalOnGround.current)

    val (base, contentColor) = when (variant) {
        ButtonVariant.Primary -> colors.accent to colors.accentInk
        ButtonVariant.Secondary -> colors.surface2 to colors.ink
        ButtonVariant.Pop -> colors.pop to colors.popInk
        ButtonVariant.Ghost -> Color.Transparent to colors.accent
    }
    val fill = when (variant) {
        ButtonVariant.Secondary, ButtonVariant.Ghost -> neutralFill(requested, resolved, base, colors)
        ButtonVariant.Primary, ButtonVariant.Pop -> base
    }

    Row(
        modifier
            .alpha(if (enabled) 1f else 0.45f)
            .kilnFocusRing(interactionSource, Pill)
            .graphicsLayer {
                scaleX = press.scale
                scaleY = press.scale
            }
            .kilnDepth(
                resolved,
                Pill,
                fill,
                colors,
                animationSpec = press.depthSpec,
                onColour = variant == ButtonVariant.Primary || variant == ButtonVariant.Pop,
            )
            .kilnPressable(interactionSource, enabled = enabled, onClick = onClick)
            .height(size.height)
            .widthIn(min = size.height)
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalOnGround provides false, LocalContentColor provides contentColor) {
            content()
        }
    }
}
