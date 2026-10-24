package com.recall.ui.kiln

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.recall.ui.theme.ClayEasing
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln
import com.recall.ui.theme.LocalOnGround
import com.recall.ui.theme.kilnDepth
import kotlin.math.roundToInt

private val SheetShape = RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)

/**
 * A bottom sheet drawn in place. Put it last in a Box, aligned to the bottom. It closes with Back,
 * or when dragged down past a third of its height or flung downwards.
 */
@Composable
fun KilnSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!visible) return
    val colors = Kiln.colors
    val motion = Kiln.motion
    var risen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { risen = true }
    val alpha by animateFloatAsState(if (risen) 1f else 0f, motion.slow(), label = "sheet")

    var dragged by remember { mutableFloatStateOf(0f) }
    var height by remember { mutableIntStateOf(0) }
    val flingSpeed = with(LocalDensity.current) { 1000.dp.toPx() }
    val dragState = rememberDraggableState { delta -> dragged = (dragged + delta).coerceAtLeast(0f) }

    BackHandler(onBack = onDismiss)

    Column(
        modifier
            .fillMaxWidth()
            .onSizeChanged { height = it.height }
            .offset { IntOffset(0, dragged.roundToInt()) }
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
            .draggable(
                dragState,
                Orientation.Vertical,
                onDragStopped = { velocity ->
                    if (dragged > height / 3f || velocity > flingSpeed) {
                        animate(dragged, height.toFloat(), velocity, motion.base()) { value, _ -> dragged = value }
                        onDismiss()
                    } else {
                        animate(dragged, 0f, velocity, motion.base(ClayEasing)) { value, _ -> dragged = value }
                    }
                },
            )
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
