package com.recall.ui.kiln

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln
import com.recall.ui.theme.LocalContentColor
import com.recall.ui.theme.LocalOnGround
import com.recall.ui.theme.kilnDepth
import com.recall.ui.theme.resolve

enum class TileTone { Default, Soft, Accent, Pop, Dip, Ok, Err }

enum class TileSize(val size: Dp, val radius: Dp, val icon: Dp) {
    Sm(44.dp, 14.dp, 22.dp),
    Md(56.dp, 18.dp, 26.dp),
    Lg(72.dp, 24.dp, 32.dp),
}

/** A square holding one icon or a pair of initials. Decorative, so it takes no focus. */
@Composable
fun KilnTile(
    modifier: Modifier = Modifier,
    tone: TileTone = TileTone.Default,
    size: TileSize = TileSize.Md,
    icon: ImageVector? = null,
    initials: String? = null,
) {
    val colors = Kiln.colors
    val onGround = LocalOnGround.current
    val (depth, fill, content) = when (tone) {
        TileTone.Default -> Triple(Depth.Float1, colors.surface2, colors.ink)
        TileTone.Soft -> Triple(Depth.Flat, colors.accentSoft, colors.ink)
        TileTone.Accent -> Triple(Depth.Float1, colors.accent, colors.accentInk)
        TileTone.Pop -> Triple(Depth.Float1, colors.pop, colors.popInk)
        TileTone.Dip -> Triple(Depth.DipSm, if (onGround) colors.well else colors.line, colors.ink2)
        TileTone.Ok -> Triple(Depth.Float1, colors.ok, colors.bg)
        TileTone.Err -> Triple(Depth.Float1, colors.err, colors.bg)
    }
    val shape = RoundedCornerShape(size.radius)
    Box(
        modifier
            .kilnDepth(depth.resolve(onGround), shape, fill, colors)
            .size(size.size),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalOnGround provides false, LocalContentColor provides content) {
            if (icon != null) KilnIcon(icon, contentDescription = null, size = size.icon)
            if (initials != null) {
                KilnText(
                    initials,
                    style = Kiln.type.h3.copy(fontSize = 15.sp, lineHeight = 15.sp),
                    maxLines = 1,
                )
            }
        }
    }
}
