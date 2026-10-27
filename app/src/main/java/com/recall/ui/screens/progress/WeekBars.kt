package com.recall.ui.screens.progress

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.recall.R
import com.recall.ui.kiln.KilnSurface
import com.recall.ui.kiln.KilnText
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln
import com.recall.ui.theme.kilnDepth

private val BarArea = 140.dp
private val BarShape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
private val BarWidth = 24.dp
private val StubHeight = 6.dp

// Short bars still show their rounded top.
private val MinBar = 14.dp

/**
 * This week's answers per day, Monday first. [todayIndex] is today's place in the week; later days
 * have nothing yet and show a stub.
 */
@Composable
fun WeekBars(counts: List<Int>, todayIndex: Int, goal: Int, modifier: Modifier = Modifier) {
    val colors = Kiln.colors
    val fullDays = stringArrayResource(R.array.progress_days)
    val shortDays = stringArrayResource(R.array.progress_days_short)
    val dayFormat = stringResource(R.string.progress_bar_day)
    val description = stringResource(
        R.string.progress_bars_description,
        (0..todayIndex).joinToString(", ") { dayFormat.format(fullDays[it], counts[it]) },
    )
    val max = maxOf(goal, counts.max())
    // Marks inside a raised panel: the well there is a flat `line` tint, so `line` itself wouldn't show.
    val mark = colors.ink2.copy(alpha = 0.35f)

    Column(modifier.clearAndSetSemantics { contentDescription = description }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        KilnSurface(Modifier.fillMaxWidth(), depth = Depth.Dip, shape = RoundedCornerShape(18.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 14.dp)
                    .height(BarArea),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val y = size.height * (1f - goal.toFloat() / max)
                    drawLine(
                        mark,
                        Offset(0f, y),
                        Offset(size.width, y),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
                    )
                }
                Row(Modifier.fillMaxSize()) {
                    counts.forEachIndexed { index, count ->
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                            when {
                                index > todayIndex -> Box(
                                    Modifier
                                        .width(BarWidth)
                                        .height(StubHeight)
                                        .background(mark, BarShape),
                                )
                                count > 0 -> Bar(
                                    height = maxOf(BarArea * (count.toFloat() / max), MinBar),
                                    today = index == todayIndex,
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            shortDays.forEachIndexed { index, day ->
                val today = index == todayIndex
                KilnText(
                    if (today) stringResource(R.string.progress_today) else day,
                    Modifier.weight(1f),
                    style = Kiln.type.caption,
                    color = if (today) colors.accent else colors.ink2,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun Bar(height: Dp, today: Boolean) {
    val colors = Kiln.colors
    val animated by animateDpAsState(height, Kiln.motion.slow(), label = "bar")
    Box(
        Modifier
            .width(BarWidth)
            .height(animated)
            .kilnDepth(Depth.Float1, BarShape, if (today) colors.accent else colors.accentSoft, colors),
    )
}
