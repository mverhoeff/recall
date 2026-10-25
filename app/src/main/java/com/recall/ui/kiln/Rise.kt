package com.recall.ui.kiln

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.recall.ui.theme.Kiln

private const val RISE_STAGGER_MS = 80
private const val RISE_MAX_STEPS = 5

/**
 * Entrance for list items: fades in and grows from 0.985 while the shadows settle, one item after
 * another. It plays once; an item scrolled away and back, or a screen returned to, stays put.
 */
@Composable
fun Modifier.rise(index: Int): Modifier {
    val motion = Kiln.motion
    var risen by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (risen || motion.reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, motion.slow(delay = minOf(index, RISE_MAX_STEPS) * RISE_STAGGER_MS))
        risen = true
    }
    return graphicsLayer {
        alpha = progress.value
        val scale = 0.985f + 0.015f * progress.value
        scaleX = scale
        scaleY = scale
    }
}
