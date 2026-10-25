package com.recall.ui.kiln

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.recall.ui.theme.Kiln

private const val RISE_STAGGER_MS = 80
private const val RISE_MAX_STEPS = 5

// Rows composed this long after their screen appeared came from scrolling, not from the entrance.
private const val ENTRANCE_WINDOW_MS = 400L

/** Uptime at which the current screen first appeared, or null outside a screen. */
val LocalEntranceStart = staticCompositionLocalOf<Long?> { null }

/** Marks the start of a screen's entrance for the [rise]s inside it. Kept across returns to the screen. */
@Composable
fun RiseEntrance(content: @Composable () -> Unit) {
    val start = rememberSaveable { SystemClock.uptimeMillis() }
    CompositionLocalProvider(LocalEntranceStart provides start, content = content)
}

/**
 * Entrance for list items: fades in and grows from 0.985 while the shadows settle, one item after
 * another. Only items there as the screen opens rise; an item scrolled into view later, or a screen
 * returned to, stays put.
 */
@Composable
fun Modifier.rise(index: Int): Modifier {
    val motion = Kiln.motion
    val start = LocalEntranceStart.current
    val elapsed = remember { start?.let { SystemClock.uptimeMillis() - it } ?: 0L }
    var risen by rememberSaveable { mutableStateOf(false) }
    val skip = risen || motion.reduced || elapsed > ENTRANCE_WINDOW_MS
    val progress = remember { Animatable(if (skip) 1f else 0f) }
    LaunchedEffect(Unit) {
        // Counted from the screen's start, so rows that arrive with late data don't trail further behind.
        val delay = (minOf(index, RISE_MAX_STEPS) * RISE_STAGGER_MS - elapsed).coerceAtLeast(0L).toInt()
        progress.animateTo(1f, motion.slow(delay = delay))
        risen = true
    }
    return graphicsLayer {
        alpha = progress.value
        val scale = 0.985f + 0.015f * progress.value
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Fades a screen's content in when its data arrives after the screen itself. Content that is there
 * from the first frame is left to the page transition, so it isn't faded twice.
 */
@Composable
fun Modifier.arrival(loaded: Boolean): Modifier {
    val motion = Kiln.motion
    val progress = remember { Animatable(if (loaded) 1f else 0f) }
    LaunchedEffect(loaded) {
        if (loaded) progress.animateTo(1f, motion.base())
    }
    return graphicsLayer { alpha = progress.value }
}
