package com.recall.ui.kiln

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.recall.ui.theme.Kiln
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SQUISH_MS = 900

// Each step eases in and out, so the blob wobbles instead of ticking between poses.
private val Wobble = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)

/** Squashes and stretches once, like soft clay landing. Stays still when motion is reduced. */
@Composable
fun Modifier.squish(delayMillis: Long = 0): Modifier {
    val reduced = Kiln.motion.reduced
    val x = remember { Animatable(1f) }
    val y = remember { Animatable(1f) }
    LaunchedEffect(reduced) {
        if (reduced) return@LaunchedEffect
        delay(delayMillis)
        launch { x.animateTo(1f, pose(1.05f, 0.97f, 1.01f)) }
        launch { y.animateTo(1f, pose(0.92f, 1.04f, 0.99f)) }
    }
    return graphicsLayer {
        scaleX = x.value
        scaleY = y.value
    }
}

private fun pose(at20: Float, at45: Float, at70: Float) = keyframes {
    durationMillis = SQUISH_MS
    1f at 0 using Wobble
    at20 at SQUISH_MS * 20 / 100 using Wobble
    at45 at SQUISH_MS * 45 / 100 using Wobble
    at70 at SQUISH_MS * 70 / 100 using Wobble
}
