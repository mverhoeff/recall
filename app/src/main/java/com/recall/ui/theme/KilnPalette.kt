package com.recall.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

enum class ClayBody { SLATE, TERRACOTTA, SAGE, OCHRE }

object KilnPalettes {
    fun colors(body: ClayBody, dark: Boolean): KilnColors = when (body) {
        ClayBody.SLATE -> if (dark) SlateDark else Slate
        ClayBody.TERRACOTTA -> if (dark) TerracottaDark else Terracotta
        ClayBody.SAGE -> if (dark) SageDark else Sage
        ClayBody.OCHRE -> if (dark) OchreDark else Ochre
    }
}

private fun rgba(r: Int, g: Int, b: Int, a: Float) = Color(r, g, b, (a * 255).roundToInt())

private val Slate = KilnColors(
    bg = Color(0xFFDCE2E9),
    surface = Color(0xFFDCE2E9),
    surface2 = Color(0xFFE6EBF0),
    well = Color(0xFFDCE2E9),
    ink = Color(0xFF1C2531),
    ink2 = Color(0xFF47566A),
    line = rgba(40, 56, 78, 0.14f),
    accent = Color(0xFF4C6A8C),
    accentInk = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFC6D3E2),
    pop = Color(0xFFB85C38),
    popInk = Color(0xFFFFFFFF),
    ok = Color(0xFF3B7650),
    warn = Color(0xFF8E620E),
    err = Color(0xFFA63E2C),
    shadowDark = rgba(86, 106, 138, 0.42f),
    shadowLight = rgba(255, 255, 255, 0.92f),
    innerLight = rgba(255, 255, 255, 0.55f),
    innerShade = rgba(70, 92, 124, 0.14f),
    grainAlpha = 0.55f,
    isDark = false,
)

private val SlateDark = KilnColors(
    bg = Color(0xFF1D242D),
    surface = Color(0xFF1D242D),
    surface2 = Color(0xFF252E39),
    well = Color(0xFF1D242D),
    ink = Color(0xFFE6EBF1),
    ink2 = Color(0xFFA0AEBF),
    line = rgba(200, 215, 235, 0.10f),
    accent = Color(0xFF8EAACB),
    accentInk = Color(0xFF0F1B29),
    accentSoft = Color(0xFF33475E),
    pop = Color(0xFFE08A63),
    popInk = Color(0xFF2A140A),
    ok = Color(0xFF86C79A),
    warn = Color(0xFFE2B65C),
    err = Color(0xFFEF907D),
    shadowDark = rgba(5, 8, 12, 0.62f),
    shadowLight = rgba(120, 150, 190, 0.10f),
    innerLight = rgba(200, 220, 245, 0.06f),
    innerShade = rgba(0, 0, 0, 0.28f),
    grainAlpha = 0.45f,
    isDark = true,
)

private val Terracotta = KilnColors(
    bg = Color(0xFFE8D9CC),
    surface = Color(0xFFE8D9CC),
    surface2 = Color(0xFFF1E6DC),
    well = Color(0xFFE8D9CC),
    ink = Color(0xFF2D1D15),
    ink2 = Color(0xFF664A3C),
    line = rgba(100, 55, 30, 0.15f),
    accent = Color(0xFFAE4F2A),
    accentInk = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFEBC3AE),
    pop = Color(0xFF3F6283),
    popInk = Color(0xFFFFFFFF),
    ok = Color(0xFF3B7650),
    warn = Color(0xFF8A5C0B),
    err = Color(0xFFA1321F),
    shadowDark = rgba(140, 90, 62, 0.42f),
    shadowLight = rgba(255, 248, 240, 0.9f),
    innerLight = rgba(255, 246, 236, 0.55f),
    innerShade = rgba(130, 76, 48, 0.15f),
    grainAlpha = 0.85f,
    isDark = false,
)

private val TerracottaDark = KilnColors(
    bg = Color(0xFF271C16),
    surface = Color(0xFF271C16),
    surface2 = Color(0xFF33251E),
    well = Color(0xFF271C16),
    ink = Color(0xFFF4E6DB),
    ink2 = Color(0xFFC6A997),
    line = rgba(240, 200, 170, 0.10f),
    accent = Color(0xFFE38A61),
    accentInk = Color(0xFF2A1209),
    accentSoft = Color(0xFF5A3322),
    pop = Color(0xFF93B3D3),
    popInk = Color(0xFF102030),
    ok = Color(0xFF86C79A),
    warn = Color(0xFFE2B65C),
    err = Color(0xFFF09A86),
    shadowDark = rgba(8, 4, 2, 0.62f),
    shadowLight = rgba(210, 150, 115, 0.09f),
    innerLight = rgba(255, 220, 195, 0.06f),
    innerShade = rgba(0, 0, 0, 0.28f),
    grainAlpha = 0.70f,
    isDark = true,
)

private val Sage = KilnColors(
    bg = Color(0xFFDBE2D6),
    surface = Color(0xFFDBE2D6),
    surface2 = Color(0xFFE6EBE2),
    well = Color(0xFFDBE2D6),
    ink = Color(0xFF1D281E),
    ink2 = Color(0xFF465744),
    line = rgba(40, 70, 40, 0.14f),
    accent = Color(0xFF4B6E51),
    accentInk = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFC3D4C2),
    pop = Color(0xFFA9542F),
    popInk = Color(0xFFFFFFFF),
    ok = Color(0xFF2F6E44),
    warn = Color(0xFF8A5C0B),
    err = Color(0xFFA63E2C),
    shadowDark = rgba(88, 110, 86, 0.42f),
    shadowLight = rgba(255, 255, 250, 0.9f),
    innerLight = rgba(255, 255, 250, 0.55f),
    innerShade = rgba(62, 90, 62, 0.14f),
    grainAlpha = 0.55f,
    isDark = false,
)

private val SageDark = KilnColors(
    bg = Color(0xFF1C231D),
    surface = Color(0xFF1C231D),
    surface2 = Color(0xFF252D26),
    well = Color(0xFF1C231D),
    ink = Color(0xFFE3EBE1),
    ink2 = Color(0xFFA2B3A0),
    line = rgba(200, 230, 200, 0.10f),
    accent = Color(0xFF92B898),
    accentInk = Color(0xFF112015),
    accentSoft = Color(0xFF34493A),
    pop = Color(0xFFE08A63),
    popInk = Color(0xFF2A140A),
    ok = Color(0xFF86C79A),
    warn = Color(0xFFE2B65C),
    err = Color(0xFFEF907D),
    shadowDark = rgba(3, 6, 4, 0.62f),
    shadowLight = rgba(140, 180, 140, 0.09f),
    innerLight = rgba(220, 250, 220, 0.06f),
    innerShade = rgba(0, 0, 0, 0.28f),
    grainAlpha = 0.45f,
    isDark = true,
)

private val Ochre = KilnColors(
    bg = Color(0xFFE8DFCA),
    surface = Color(0xFFE8DFCA),
    surface2 = Color(0xFFF1EBDC),
    well = Color(0xFFE8DFCA),
    ink = Color(0xFF2A2314),
    ink2 = Color(0xFF5B503C),
    line = rgba(90, 70, 30, 0.15f),
    accent = Color(0xFF86651B),
    accentInk = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFE3D2A4),
    pop = Color(0xFF4C6A8C),
    popInk = Color(0xFFFFFFFF),
    ok = Color(0xFF3B7650),
    warn = Color(0xFF8A5C0B),
    err = Color(0xFFA63E2C),
    shadowDark = rgba(132, 110, 62, 0.42f),
    shadowLight = rgba(255, 252, 240, 0.9f),
    innerLight = rgba(255, 252, 240, 0.55f),
    innerShade = rgba(120, 96, 44, 0.14f),
    grainAlpha = 0.70f,
    isDark = false,
)

private val OchreDark = KilnColors(
    bg = Color(0xFF24201A),
    surface = Color(0xFF24201A),
    surface2 = Color(0xFF2E2920),
    well = Color(0xFF24201A),
    ink = Color(0xFFF1EAD8),
    ink2 = Color(0xFFBEB298),
    line = rgba(240, 220, 170, 0.10f),
    accent = Color(0xFFD8B25E),
    accentInk = Color(0xFF241B07),
    accentSoft = Color(0xFF4E4224),
    pop = Color(0xFF8EAACB),
    popInk = Color(0xFF0F1B29),
    ok = Color(0xFF86C79A),
    warn = Color(0xFFE2B65C),
    err = Color(0xFFEF907D),
    shadowDark = rgba(6, 5, 2, 0.62f),
    shadowLight = rgba(200, 175, 120, 0.09f),
    innerLight = rgba(255, 240, 200, 0.06f),
    innerShade = rgba(0, 0, 0, 0.28f),
    grainAlpha = 0.55f,
    isDark = true,
)
