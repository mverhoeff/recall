package com.recall.ui.kiln

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.DpSize
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln
import com.recall.ui.theme.kilnDepth

enum class BlobShape { Pebble, Coil }

enum class BlobTone { Accent, Soft, Pop, Raised }

private val PebbleShape = RoundedCornerShape(
    topStartPercent = 58,
    topEndPercent = 42,
    bottomEndPercent = 55,
    bottomStartPercent = 45,
)

/** Decorative clay piece for illustrations. Hidden from accessibility services. */
@Composable
fun KilnBlob(shape: BlobShape, tone: BlobTone, size: DpSize, modifier: Modifier = Modifier) {
    val colors = Kiln.colors
    val fill = when (tone) {
        BlobTone.Accent -> colors.accent
        BlobTone.Soft -> colors.accentSoft
        BlobTone.Pop -> colors.pop
        BlobTone.Raised -> colors.surface2
    }
    val outline = when (shape) {
        BlobShape.Pebble -> PebbleShape
        BlobShape.Coil -> CircleShape
    }
    Box(
        modifier
            .clearAndSetSemantics {}
            .size(size)
            .kilnDepth(Depth.Float3, outline, fill, colors),
    )
}
