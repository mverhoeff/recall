package com.recall.ui.kiln

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recall.ui.theme.Kiln

enum class BadgeTone { Neutral, Ok, Warn, Err, Accent }

/** Status label. Flat and identical in every context, so it never reads as pressable. */
@Composable
fun KilnBadge(text: String, modifier: Modifier = Modifier, tone: BadgeTone = BadgeTone.Neutral) {
    val colors = Kiln.colors
    val color = when (tone) {
        BadgeTone.Neutral -> colors.ink2
        BadgeTone.Ok -> colors.ok
        BadgeTone.Warn -> colors.warn
        BadgeTone.Err -> colors.err
        BadgeTone.Accent -> colors.accent
    }
    Row(
        modifier
            .background(color.copy(alpha = 0.13f), CircleShape)
            .height(24.dp)
            .widthIn(min = 24.dp)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        KilnText(
            text,
            style = Kiln.type.body.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 14.sp),
            color = color,
            maxLines = 1,
        )
    }
}
