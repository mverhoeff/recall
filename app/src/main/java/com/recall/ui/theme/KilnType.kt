package com.recall.ui.theme

import androidx.annotation.FontRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.recall.R

enum class TypePairing { A, B, C }

@Immutable
data class KilnTypography(
    val display: TextStyle,
    val h1: TextStyle,
    val h2: TextStyle,
    val h3: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val small: TextStyle,
    val caption: TextStyle,
    val label: TextStyle,
    val button: TextStyle,
    val buttonSm: TextStyle,
    val buttonLg: TextStyle,
)

private val bodyWeights = listOf(400, 500, 600, 700)

@OptIn(ExperimentalTextApi::class)
private fun variableFamily(@FontRes res: Int, weights: List<Int>) = FontFamily(
    weights.map { w ->
        Font(res, weight = FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    }
)

private val Fredoka = variableFamily(R.font.fredoka, listOf(600))
private val Nunito = variableFamily(R.font.nunito, bodyWeights)
private val Baloo2 = variableFamily(R.font.baloo2, listOf(700))
private val Figtree = variableFamily(R.font.figtree, bodyWeights)
private val YoungSerif = FontFamily(Font(R.font.young_serif, weight = FontWeight.Normal))
private val InstrumentSans = variableFamily(R.font.instrument_sans, bodyWeights)

private fun style(family: FontFamily, size: Int, lineHeight: Float, weight: Int, tracking: Float) = TextStyle(
    fontFamily = family,
    fontSize = size.sp,
    lineHeight = (size * lineHeight).sp,
    fontWeight = FontWeight(weight),
    letterSpacing = tracking.em,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

fun kilnTypography(pairing: TypePairing): KilnTypography {
    val (display, body, displayWeight) = when (pairing) {
        TypePairing.A -> Triple(Fredoka, Nunito, 600)
        TypePairing.B -> Triple(Baloo2, Figtree, 700)
        TypePairing.C -> Triple(YoungSerif, InstrumentSans, 400)
    }
    return KilnTypography(
        display = style(display, 56, 1.02f, displayWeight, -0.02f),
        h1 = style(display, 32, 1.10f, displayWeight, -0.015f),
        h2 = style(display, 24, 1.20f, displayWeight, -0.01f),
        h3 = style(display, 19, 1.25f, displayWeight, 0f),
        body = style(body, 16, 1.50f, 400, 0f),
        bodyStrong = style(body, 15, 1.40f, 700, 0f),
        small = style(body, 14, 1.43f, 400, 0f),
        caption = style(body, 13, 1.38f, 400, 0f),
        // Uppercasing and the ink2 colour are applied where the label is drawn.
        label = style(body, 12, 1.20f, 700, 0.08f),
        button = style(body, 15, 1.0f, 700, 0.01f),
        buttonSm = style(body, 14, 1.0f, 700, 0.01f),
        buttonLg = style(body, 17, 1.0f, 700, 0.01f),
    )
}
