package com.recall.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KilnColorsTest {

    private val palettes = ClayBody.entries.flatMap { body ->
        listOf(false, true).map { dark -> "$body${if (dark) "-dark" else ""}" to KilnPalettes.colors(body, dark) }
    }

    @Test
    fun textMeetsBodyContrast() {
        for ((name, c) in palettes) {
            assertContrast(name, "ink on bg", c.ink, c.bg, 4.5f)
            assertContrast(name, "ink on surface2", c.ink, c.surface2, 4.5f)
            assertContrast(name, "ink2 on bg", c.ink2, c.bg, 4.5f)
            assertContrast(name, "ink2 on surface2", c.ink2, c.surface2, 4.5f)
            assertContrast(name, "accentInk on accent", c.accentInk, c.accent, 4.5f)
            assertContrast(name, "popInk on pop", c.popInk, c.pop, 4.5f)
        }
    }

    @Test
    fun statusColoursMeetGraphicContrast() {
        for ((name, c) in palettes) {
            assertContrast(name, "ok on bg", c.ok, c.bg, 3.0f)
            assertContrast(name, "warn on bg", c.warn, c.bg, 3.0f)
            assertContrast(name, "err on bg", c.err, c.bg, 3.0f)
        }
    }

    @Test
    fun groundSurfaceAndWellAreOneColour() {
        for ((name, c) in palettes) {
            assertEquals("$name surface", c.bg, c.surface)
            assertEquals("$name well", c.bg, c.well)
        }
    }

    @Test
    fun darkFlagMatchesVariant() {
        for (body in ClayBody.entries) {
            assertEquals(false, KilnPalettes.colors(body, dark = false).isDark)
            assertEquals(true, KilnPalettes.colors(body, dark = true).isDark)
        }
    }

    private fun assertContrast(palette: String, pair: String, fg: Color, bg: Color, min: Float) {
        val ratio = contrast(fg, bg)
        assertTrue("$palette: $pair is $ratio, needs $min", ratio >= min)
    }

    private fun channel(c: Float) = if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)

    private fun luminance(col: Color) =
        0.2126f * channel(col.red) + 0.7152f * channel(col.green) + 0.0722f * channel(col.blue)

    private fun contrast(a: Color, b: Color): Float {
        val (hi, lo) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (hi + 0.05f) / (lo + 0.05f)
    }
}
