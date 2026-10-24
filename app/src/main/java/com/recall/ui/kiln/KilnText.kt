package com.recall.ui.kiln

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.recall.ui.theme.LocalContentColor
import com.recall.ui.theme.LocalKilnTextStyle

@Composable
fun KilnText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalKilnTextStyle.current,
    color: Color = LocalContentColor.current,
    uppercase: Boolean = false,
    textAlign: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    BasicText(
        text = if (uppercase) text.uppercase() else text,
        modifier = modifier,
        style = style.merge(color = color, textAlign = textAlign),
        maxLines = maxLines,
        overflow = overflow,
    )
}
