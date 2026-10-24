package com.recall.ui.kiln

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
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

@Composable
fun KilnRadio(selected: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = Kiln.colors
    val onGround = LocalOnGround.current
    val depth = if (selected) Depth.Float1 else Depth.DipSm.resolve(onGround)
    val fill = when {
        selected -> colors.accent
        onGround -> colors.well
        else -> colors.line
    }
    val selection = if (onClick != null) {
        Modifier.selectable(
            selected = selected,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            role = Role.RadioButton,
            onClick = onClick,
        )
    } else {
        Modifier
    }
    Box(modifier.then(selection).size(48.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(28.dp)
                .kilnDepth(depth, CircleShape, fill, colors, animationSpec = Kiln.motion.base()),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(10.dp).background(colors.accentInk, CircleShape))
        }
    }
}
