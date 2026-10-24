package com.recall.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.ShadowScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

/** Multiplies every blur radius. CSS blur and Compose shadow radius need not soften alike. */
const val BLUR_SCALE = 1f

enum class Depth { Flat, Mold1, Mold2, Mold3, Float1, Float2, Float3, DipSm, Dip }

enum class ShadowRole { ShadowDark, ShadowLight, InnerLight, InnerShade }

@Immutable
data class ShadowLayer(val x: Float, val y: Float, val blur: Float, val spread: Float, val role: ShadowRole) {
    val isZero get() = x == 0f && y == 0f && blur == 0f && spread == 0f
}

@Immutable
data class DepthSpec(val s1: ShadowLayer, val s2: ShadowLayer, val s3: ShadowLayer, val s4: ShadowLayer) {
    val layers get() = listOf(s1, s2, s3, s4)
}

@Immutable
data class ResolvedLayer(val x: Float, val y: Float, val blur: Float, val spread: Float, val color: Color) {
    val isZero get() = x == 0f && y == 0f && blur == 0f && spread == 0f
}

private fun layer(x: Float, y: Float, blur: Float, spread: Float, role: ShadowRole) =
    ShadowLayer(x, y, blur, spread, role)

private fun none(role: ShadowRole) = ShadowLayer(0f, 0f, 0f, 0f, role)

private val noInnerLight = none(ShadowRole.InnerLight)
private val noInnerShade = none(ShadowRole.InnerShade)

private fun mold(x: Float, y: Float, blur: Float, lx: Float, ly: Float, lBlur: Float) = DepthSpec(
    layer(x, y, blur, 0f, ShadowRole.ShadowDark),
    layer(lx, ly, lBlur, 0f, ShadowRole.ShadowLight),
    noInnerLight, noInnerShade,
)

private fun floating(y: Float, blur: Float, spread: Float, cy: Float, cBlur: Float, cSpread: Float) = DepthSpec(
    layer(0f, y, blur, spread, ShadowRole.ShadowDark),
    layer(0f, cy, cBlur, cSpread, ShadowRole.InnerShade),
    noInnerLight, noInnerShade,
)

private fun dip(d: Float, blur: Float) = DepthSpec(
    none(ShadowRole.ShadowDark),
    none(ShadowRole.ShadowLight),
    layer(d, d, blur, 0f, ShadowRole.ShadowDark),
    layer(-d, -d, blur, 0f, ShadowRole.ShadowLight),
)

private val specs = mapOf(
    Depth.Flat to DepthSpec(none(ShadowRole.ShadowDark), none(ShadowRole.ShadowLight), noInnerLight, noInnerShade),
    Depth.Mold1 to mold(4f, 4f, 10f, -4f, -4f, 10f),
    Depth.Mold2 to mold(9f, 9f, 22f, -8f, -8f, 20f),
    Depth.Mold3 to mold(16f, 18f, 38f, -14f, -14f, 32f),
    Depth.Float1 to floating(4f, 10f, -3f, 1f, 2f, 0f),
    Depth.Float2 to floating(10f, 22f, -8f, 2f, 5f, 0f),
    Depth.Float3 to floating(24f, 46f, -16f, 5f, 12f, -2f),
    Depth.DipSm to dip(3f, 7f),
    Depth.Dip to dip(6f, 12f),
)

fun Depth.spec(): DepthSpec = specs.getValue(this)

/** Applies the stacking rule: molds and dips belong to the ground only. */
fun Depth.resolve(onGround: Boolean): Depth = when {
    onGround -> this
    this == Depth.Mold1 || this == Depth.Mold2 || this == Depth.Mold3 -> Depth.Float1
    this == Depth.DipSm || this == Depth.Dip -> Depth.Flat
    else -> this
}

fun lerp(a: ShadowLayer, b: ShadowLayer, t: Float) = ShadowLayer(
    lerp(a.x, b.x, t), lerp(a.y, b.y, t), lerp(a.blur, b.blur, t), lerp(a.spread, b.spread, t),
    if (t >= 0.5f) b.role else a.role,
)

fun lerp(a: DepthSpec, b: DepthSpec, t: Float) =
    DepthSpec(lerp(a.s1, b.s1, t), lerp(a.s2, b.s2, t), lerp(a.s3, b.s3, t), lerp(a.s4, b.s4, t))

fun lerp(a: ResolvedLayer, b: ResolvedLayer, t: Float) = ResolvedLayer(
    lerp(a.x, b.x, t), lerp(a.y, b.y, t), lerp(a.blur, b.blur, t), lerp(a.spread, b.spread, t),
    lerpColor(a.color, b.color, t),
)

fun ShadowRole.color(colors: KilnColors) = when (this) {
    ShadowRole.ShadowDark -> colors.shadowDark
    ShadowRole.ShadowLight -> colors.shadowLight
    ShadowRole.InnerLight -> colors.innerLight
    ShadowRole.InnerShade -> colors.innerShade
}

fun ShadowLayer.resolve(colors: KilnColors) = ResolvedLayer(x, y, blur, spread, role.color(colors))

private val settle = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

private val OnColourShade = Color.Black.copy(alpha = 0.3f)
private val OnColourLight = Color.White.copy(alpha = 0.3f)

@Composable
fun Modifier.kilnDepth(
    depth: Depth,
    shape: Shape,
    fill: Color,
    colors: KilnColors,
    animate: Boolean = true,
    animationSpec: AnimationSpec<Float> = tween(260, easing = settle),
    onColour: Boolean = false,
): Modifier {
    // Only the depth animates. Colours are looked up while drawing, so a theme switch recolours
    // the shadows in the same frame as the fill instead of fading over from the old palette.
    val target = depth.spec().layers
    var from by remember { mutableStateOf(target) }
    var to by remember { mutableStateOf(target) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(target) {
        if (target == to) return@LaunchedEffect
        // Start from wherever the previous animation got to, so interruptions don't jump.
        val t = progress.value
        from = from.indices.map { lerp(from[it], to[it], t) }
        to = target
        if (animate) {
            progress.snapTo(0f)
            progress.animateTo(1f, animationSpec)
        } else {
            progress.snapTo(1f)
        }
    }

    // The clay's own shade and highlight are tuned for the pale ground; on a strong fill the shade can be
    // lighter than the fill itself, so a dip on accent or pop is shaded with plain black and white.
    val inset = if (onColour) colors.copy(shadowDark = OnColourShade, shadowLight = OnColourLight) else colors
    fun current(slot: Int): ResolvedLayer {
        val palette = if (slot >= 2) inset else colors
        return lerp(from[slot].resolve(palette), to[slot].resolve(palette), progress.value)
    }
    // A slot that is empty at both ends of the animation is left out of the chain.
    fun used(slot: Int) = !from[slot].isZero || !to[slot].isZero

    var m = this
    for (slot in 0..1) if (used(slot)) m = m.outerShadow(shape) { current(slot) }
    m = m.background(fill, shape)
    for (slot in 2..3) if (used(slot)) m = m.innerShadow(shape) { set(current(slot)) }
    return m
}

/**
 * Drop shadow drawn with the platform blur. Compose's dropShadow renders into a bitmap only one blur
 * radius larger than the shape, which cuts the soft tail of big shadows off in a visible straight edge.
 */
private fun Modifier.outerShadow(shape: Shape, layer: () -> ResolvedLayer) = drawWithCache {
    val paint = Paint()
    var filterRadius = -1f
    onDrawBehind {
        val current = layer()
        if (current.isZero) return@onDrawBehind
        val radius = (current.blur * BLUR_SCALE).dp.toPx()
        if (radius != filterRadius) {
            paint.asFrameworkPaint().maskFilter = if (radius > 0f) BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL) else null
            filterRadius = radius
        }
        paint.color = current.color
        val spread = current.spread.dp.toPx()
        val grown = Size((size.width + spread * 2).coerceAtLeast(0f), (size.height + spread * 2).coerceAtLeast(0f))
        val outline = shape.createOutline(grown, layoutDirection, this)
        translate(current.x.dp.toPx() - spread, current.y.dp.toPx() - spread) {
            drawIntoCanvas { it.drawOutline(outline, paint) }
        }
    }
}

private fun ShadowScope.set(layer: ResolvedLayer) {
    radius = (layer.blur * BLUR_SCALE).dp.toPx()
    spread = layer.spread.dp.toPx()
    offset = Offset(layer.x.dp.toPx(), layer.y.dp.toPx())
    color = layer.color
    alpha = if (layer.isZero) 0f else 1f
}
