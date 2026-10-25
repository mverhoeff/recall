package com.recall.ui.screens.results

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recall.R
import com.recall.ui.kiln.KilnText
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln
import com.recall.ui.theme.kilnDepth

private val RingSize = 196.dp
private val DiscSize = 130.dp
private val ArcWidth = 15.dp
private val ArcInset = 10.dp

// Room for the score across the middle of the disc.
private val ScoreWidth = 108.dp
private const val SCORE_MAX_SP = 44
private const val SCORE_MIN_SP = 20

/** A well with an accent arc around it for the share of right answers, and the score on a disc in the middle. */
@Composable
fun ScoreRing(correct: Int, total: Int, modifier: Modifier = Modifier) {
    val colors = Kiln.colors
    val share = if (total == 0) 0f else correct / total.toFloat()
    val sweep = remember { Animatable(0f) }
    val spec = Kiln.motion.slow<Float>()
    LaunchedEffect(share) { sweep.animateTo(share * 360f, spec) }
    val description = stringResource(R.string.results_score_description, correct, total)
    Box(
        modifier
            .size(RingSize)
            .clearAndSetSemantics { contentDescription = description }
            .kilnDepth(Depth.Dip, CircleShape, colors.well, colors)
            .drawWithContent {
                drawContent()
                if (sweep.value <= 0f) return@drawWithContent
                val stroke = ArcWidth.toPx()
                val inset = ArcInset.toPx() + stroke / 2
                drawArc(
                    color = colors.accent,
                    startAngle = -90f,
                    sweepAngle = sweep.value,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .size(DiscSize)
                .kilnDepth(Depth.Float2, CircleShape, colors.surface2, colors),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Score(correct, total)
            KilnText(stringResource(R.string.results_correct), style = Kiln.type.small, color = colors.ink2)
        }
    }
}

/** "4/5", shrunk until it fits across the disc, so "999/999" still does. */
@Composable
private fun Score(correct: Int, total: Int) {
    val display = Kiln.type.display
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val text = stringResource(R.string.results_score, correct, total)
    val style = remember(text, display, density) {
        val sizes = (SCORE_MAX_SP downTo SCORE_MIN_SP step 2).map { size ->
            display.copy(fontSize = size.sp, lineHeight = (size + 2).sp)
        }
        val room = with(density) { ScoreWidth.roundToPx() }
        sizes.firstOrNull { candidate ->
            measurer.measure(text, candidate, softWrap = false, maxLines = 1).size.width <= room
        } ?: sizes.last()
    }
    KilnText(text, style = style, color = Kiln.colors.ink, maxLines = 1)
}
