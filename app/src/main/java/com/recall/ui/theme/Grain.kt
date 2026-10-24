package com.recall.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.recall.R

private val GrainTile = 180.dp

@Composable
fun Modifier.kilnGround(colors: KilnColors): Modifier {
    val resources = LocalContext.current.resources
    val tilePx = with(LocalDensity.current) { GrainTile.roundToPx() }
    val brush = remember(resources, tilePx) {
        val source = BitmapFactory.decodeResource(resources, R.drawable.grain)
        val tile = Bitmap.createScaledBitmap(source, tilePx, tilePx, true)
        ShaderBrush(ImageShader(tile.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
    }
    // Soft light only blends with what is already in the same offscreen layer, so the ground fill
    // has to be drawn inside it too, or the bare ground would show the raw grain.
    return this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            drawRect(brush = brush, alpha = colors.grainAlpha, blendMode = BlendMode.Softlight)
        }
        .background(colors.bg)
}
