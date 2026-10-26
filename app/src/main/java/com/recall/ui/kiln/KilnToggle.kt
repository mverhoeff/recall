package com.recall.ui.kiln

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recall.ui.theme.ClayEasing
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln
import com.recall.ui.theme.LocalOnGround
import com.recall.ui.theme.kilnDepth

@Composable
fun KilnToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    sublabel: String? = null,
    onSublabelClick: (() -> Unit)? = null,
) {
    val colors = Kiln.colors
    val onGround = LocalOnGround.current
    val motion = Kiln.motion
    val knob by animateFloatAsState(if (checked) 1f else 0f, motion.slow(ClayEasing), label = "knob")
    val fill by animateFloatAsState(if (checked) 1f else 0f, motion.base(), label = "track")
    val trackRest = if (onGround) colors.well else colors.line

    Row(
        modifier
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .defaultMinSize(minHeight = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            KilnText(
                label,
                style = Kiln.type.body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
            )
            if (sublabel != null && onSublabelClick != null) {
                KilnText(
                    sublabel,
                    Modifier
                        .touchReach(above = SublabelReachUp, below = SublabelReachDown)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = onSublabelClick,
                        )
                        .padding(top = SublabelReachUp, bottom = SublabelReachDown),
                    style = Kiln.type.caption,
                    color = colors.accent,
                )
            } else if (sublabel != null) {
                KilnText(sublabel, style = Kiln.type.caption, color = colors.ink2)
            }
        }
        Box(
            Modifier
                .size(62.dp, 34.dp)
                .kilnDepth(
                    if (onGround) Depth.DipSm else Depth.Flat,
                    CircleShape,
                    lerp(trackRest, colors.accent, fill),
                    colors,
                    onColour = checked,
                ),
        ) {
            Box(
                Modifier
                    .offset(x = 4.dp + 28.dp * knob, y = 4.dp)
                    .size(26.dp)
                    .kilnDepth(Depth.Float1, CircleShape, colors.surface2, colors),
            )
        }
    }
}

// A caption line is about 18dp tall. Reaching mostly downwards takes a full 48dp of touches without
// covering the label above, which still toggles the switch.
private val SublabelReachUp = 6.dp
private val SublabelReachDown = 24.dp

/** Lays the content out smaller than it measures, so its padding overlaps its neighbours. */
private fun Modifier.touchReach(above: Dp, below: Dp) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val top = above.roundToPx()
    layout(placeable.width, (placeable.height - top - below.roundToPx()).coerceAtLeast(0)) { placeable.place(0, -top) }
}
