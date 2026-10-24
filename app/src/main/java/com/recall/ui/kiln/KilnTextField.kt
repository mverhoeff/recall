package com.recall.ui.kiln

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln
import com.recall.ui.theme.LocalOnGround
import com.recall.ui.theme.kilnDepth
import com.recall.ui.theme.resolve

private val FieldShape = RoundedCornerShape(18.dp)

@Composable
fun KilnTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    hint: String? = null,
    error: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = Kiln.colors
    val onGround = LocalOnGround.current
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val depth = (if (focused) Depth.Dip else Depth.DipSm).resolve(onGround)
    val textStyle = Kiln.type.body.copy(fontWeight = FontWeight.Medium, color = colors.ink)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        KilnText(
            label,
            style = Kiln.type.body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp),
            color = colors.ink2,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (error != null) Modifier.outline(colors.err) else Modifier)
                .kilnDepth(depth, FieldShape, if (onGround) colors.well else colors.line, colors)
                .defaultMinSize(minHeight = 56.dp),
            textStyle = textStyle,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else minLines,
            keyboardOptions = keyboardOptions,
            cursorBrush = SolidColor(colors.accent),
            interactionSource = interactionSource,
            decorationBox = { inner ->
                Box(
                    Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        KilnText(placeholder, style = textStyle, color = colors.ink2)
                    }
                    inner()
                }
            },
        )
        if (error != null) {
            KilnText(
                error,
                style = Kiln.type.body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp),
                color = colors.err,
            )
        } else if (hint != null) {
            KilnText(hint, style = Kiln.type.caption, color = colors.ink2)
        }
    }
}

/** A 2dp ring sitting just outside the field, so it doesn't eat into the well. */
private fun Modifier.outline(color: Color) = drawBehind {
    val stroke = 2.dp.toPx()
    val inset = stroke / 2
    val ring = Size(size.width + stroke, size.height + stroke)
    translate(-inset, -inset) {
        drawOutline(FieldShape.createOutline(ring, layoutDirection, this), color, style = Stroke(stroke))
    }
}
