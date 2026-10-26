package com.recall.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.recall.R

private val GrainTile = 96.dp

// Scale every palette's grain opacity, so the themes keep their relative roughness. Soft light
// barely shifts pale colours, so the light palettes need more of it than the dark ones.
private const val GRAIN_STRENGTH_LIGHT = 1.3f
private const val GRAIN_STRENGTH_DARK = 0.6f

@Composable
fun Modifier.kilnGround(colors: KilnColors): Modifier {
    val resources = LocalContext.current.resources
    val tilePx = with(LocalDensity.current) { GrainTile.roundToPx() }
    val tile = remember(resources, tilePx) {
        val source = BitmapFactory.decodeResource(resources, R.drawable.grain)
        Bitmap.createScaledBitmap(source, tilePx, tilePx, true)
    }
    val brush = remember(tile) { ShaderBrush(ImageShader(tile.asImageBitmap(), TileMode.Repeated, TileMode.Repeated)) }
    val scroll = remember { GrainScroll() }
    val strength = if (colors.isDark) GRAIN_STRENGTH_DARK else GRAIN_STRENGTH_LIGHT
    val alpha = (colors.grainAlpha * strength).coerceAtMost(1f)

    if (!colors.isDark && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val grain = remember(tile) { CappedGrain(tile) }
        return this
            .nestedScroll(scroll)
            .graphicsLayer {
                val shift = Offset(scroll.offset.x % tilePx, scroll.offset.y % tilePx)
                renderEffect = grain.effect(shift, alpha, colors.bg)
            }
            .background(colors.bg)
    }

    // Soft light only blends with what is already in the same offscreen layer, so the ground fill
    // has to be drawn inside it too, or the bare ground would show the raw grain.
    return this
        .nestedScroll(scroll)
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val shift = Offset(scroll.offset.x % tilePx, scroll.offset.y % tilePx)
            translate(shift.x, shift.y) {
                drawRect(
                    brush = brush,
                    topLeft = -shift,
                    size = size,
                    alpha = alpha,
                    blendMode = BlendMode.Softlight,
                )
            }
        }
        .background(colors.bg)
}

/**
 * Soft light changes mid tones far more than pale ones, so on a light theme the strength that shows
 * on the ground turns darker fills (accent buttons, the hero card) coarse. This applies the same
 * soft-light grain, but caps every pixel's change at what the ground itself gets from that grain.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class CappedGrain(tile: Bitmap) {
    private val tileShader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    private val shader = RuntimeShader(CAPPED_GRAIN)
    private val matrix = Matrix()

    fun effect(shift: Offset, alpha: Float, ground: Color): androidx.compose.ui.graphics.RenderEffect {
        matrix.setTranslate(shift.x, shift.y)
        tileShader.setLocalMatrix(matrix)
        shader.setInputShader("grain", tileShader)
        shader.setFloatUniform("strength", alpha)
        shader.setColorUniform("ground", ground.toArgb())
        return RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
    }
}

private const val CAPPED_GRAIN = """
uniform shader content;
uniform shader grain;
uniform half strength;
layout(color) uniform half4 ground;

// The W3C soft light, the same one BlendMode.Softlight uses.
half3 softLight(half3 d, half3 s) {
    half3 dark = d - (1.0 - 2.0 * s) * d * (1.0 - d);
    half3 lift = mix(((16.0 * d - 12.0) * d + 4.0) * d, sqrt(d), step(0.25, d));
    half3 light = d + (2.0 * s - 1.0) * (lift - d);
    return mix(dark, light, step(0.5, s));
}

half4 main(float2 p) {
    half4 c = content.eval(p);
    half4 g = grain.eval(p);
    half amount = g.a * strength;
    if (amount <= 0.0 || c.a <= 0.0) return c;
    half3 s = g.rgb / g.a;
    half3 d = c.rgb / c.a;
    half3 change = (softLight(d, s) - d) * amount;
    half3 cap = abs(softLight(ground.rgb, s) - ground.rgb) * amount;
    return half4(clamp(d + clamp(change, -cap, cap), 0.0, 1.0) * c.a, c.a);
}
"""

// Accumulates what nested scrollables actually moved, so the grain travels with the content
// as if it were printed on it. Only vertical scrolling counts: pages and tabs slide sideways
// without moving it, whether by a swipe or a tap.
private class GrainScroll : NestedScrollConnection {
    var offset by mutableStateOf(Offset.Zero)

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        offset += Offset(0f, consumed.y)
        return Offset.Zero
    }
}
