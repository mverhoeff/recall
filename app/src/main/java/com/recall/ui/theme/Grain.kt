package com.recall.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
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
    val brush = remember(resources, tilePx) {
        val source = BitmapFactory.decodeResource(resources, R.drawable.grain)
        val tile = Bitmap.createScaledBitmap(source, tilePx, tilePx, true)
        ShaderBrush(ImageShader(tile.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
    }
    val scroll = remember { GrainScroll() }
    val strength = if (colors.isDark) GRAIN_STRENGTH_DARK else GRAIN_STRENGTH_LIGHT
    val alpha = (colors.grainAlpha * strength).coerceAtMost(1f)

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

// Accumulates what nested scrollables actually moved, so the grain travels with the content
// as if it were printed on it.
private class GrainScroll : NestedScrollConnection {
    var offset by mutableStateOf(Offset.Zero)

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        offset += consumed
        return Offset.Zero
    }
}
