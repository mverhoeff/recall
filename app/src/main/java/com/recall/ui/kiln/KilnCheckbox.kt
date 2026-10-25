package com.recall.ui.kiln

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln
import com.recall.ui.theme.LocalOnGround
import com.recall.ui.theme.kilnDepth
import com.recall.ui.theme.resolve

private val BoxShape = RoundedCornerShape(9.dp)

/** The several-answers sibling of [KilnRadio]: same size and depths, a rounded square with a check. */
@Composable
fun KilnCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = Kiln.colors
    val onGround = LocalOnGround.current
    val depth = if (checked) Depth.Float1 else Depth.DipSm.resolve(onGround)
    val fill = when {
        checked -> colors.accent
        onGround -> colors.well
        else -> colors.line
    }
    Box(
        modifier
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .size(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .kilnDepth(depth, BoxShape, fill, colors, animationSpec = Kiln.motion.base()),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) KilnIcon(KilnIcons.checkStrong, contentDescription = null, size = 18.dp, tint = colors.accentInk)
        }
    }
}
