package com.recall.ui.theme

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle

val LocalKilnColors = staticCompositionLocalOf { KilnPalettes.colors(ClayBody.SLATE, dark = false) }
val LocalKilnType = staticCompositionLocalOf { kilnTypography(TypePairing.B) }
val LocalKilnMotion = staticCompositionLocalOf { KilnMotion(reduced = false) }
val LocalOnGround = compositionLocalOf { true }
val LocalContentColor = compositionLocalOf { Color.Unspecified }
val LocalKilnTextStyle = compositionLocalOf { TextStyle.Default }

object Kiln {
    val colors: KilnColors
        @Composable @ReadOnlyComposable get() = LocalKilnColors.current
    val type: KilnTypography
        @Composable @ReadOnlyComposable get() = LocalKilnType.current
    val motion: KilnMotion
        @Composable @ReadOnlyComposable get() = LocalKilnMotion.current
}

@Composable
fun KilnTheme(
    body: ClayBody,
    dark: Boolean,
    pairing: TypePairing,
    reducedMotion: Boolean,
    content: @Composable () -> Unit,
) {
    val colors = KilnPalettes.colors(body, dark)
    val type = remember(pairing) { kilnTypography(pairing) }
    val context = LocalContext.current
    val systemOff = remember(context) { systemAnimationsOff(context) }
    val motion = KilnMotion(reduced = reducedMotion || systemOff)
    val selection = remember(colors) { TextSelectionColors(colors.accent, colors.accent.copy(alpha = 0.3f)) }
    CompositionLocalProvider(
        LocalKilnColors provides colors,
        LocalKilnType provides type,
        LocalKilnMotion provides motion,
        LocalOnGround provides true,
        LocalContentColor provides colors.ink,
        LocalKilnTextStyle provides type.body,
        LocalTextSelectionColors provides selection,
        content = content,
    )
}
