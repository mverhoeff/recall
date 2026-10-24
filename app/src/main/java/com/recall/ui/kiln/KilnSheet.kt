package com.recall.ui.kiln

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln
import com.recall.ui.theme.LocalOnGround
import com.recall.ui.theme.kilnDepth

private val SheetShape = RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)

/** A bottom sheet drawn in place. Put it last in a Box, aligned to the bottom. */
@Composable
fun KilnSheet(visible: Boolean, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    if (!visible) return
    val colors = Kiln.colors
    val motion = Kiln.motion
    var risen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { risen = true }
    val alpha by animateFloatAsState(if (risen) 1f else 0f, motion.slow(), label = "sheet")

    Column(
        modifier
            .fillMaxWidth()
            .alpha(alpha)
            .kilnDepth(
                if (risen) Depth.Float3 else Depth.Flat,
                SheetShape,
                colors.surface2,
                colors,
                animationSpec = motion.slow(),
            )
            // Keeps taps on the sheet from reaching whatever lies underneath.
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(40.dp, 6.dp).background(colors.line, CircleShape))
        CompositionLocalProvider(LocalOnGround provides false) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        }
    }
}
