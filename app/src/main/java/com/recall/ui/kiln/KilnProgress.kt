package com.recall.ui.kiln

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln
import com.recall.ui.theme.LocalOnGround
import com.recall.ui.theme.kilnDepth

@Composable
fun KilnProgress(fraction: Float, modifier: Modifier = Modifier) {
    val colors = Kiln.colors
    val onGround = LocalOnGround.current
    val clamped = fraction.coerceIn(0f, 1f)
    val animated by animateFloatAsState(clamped, Kiln.motion.slow(), label = "progress")
    BoxWithConstraints(
        modifier
            .progressSemantics(clamped)
            .fillMaxWidth()
            .height(14.dp)
            .kilnDepth(
                if (onGround) Depth.DipSm else Depth.Flat,
                CircleShape,
                if (onGround) colors.well else colors.line,
                colors,
            )
            .padding(3.dp),
    ) {
        if (animated > 0f) {
            // Never narrower than tall, so the rounded ends survive at small values.
            val width = (maxWidth * animated).coerceAtLeast(maxHeight)
            Box(Modifier.width(width).fillMaxHeight().background(colors.accent, CircleShape))
        }
    }
}
