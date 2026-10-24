package com.recall.data.settings

import com.recall.ui.theme.ClayBody
import com.recall.ui.theme.TypePairing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {
    @Test
    fun systemModeFollowsTheSystem() {
        val s = AppSettings(mode = ThemeMode.SYSTEM)
        assertTrue(s.resolveDark(systemDark = true))
        assertFalse(s.resolveDark(systemDark = false))
    }

    @Test
    fun lightAndDarkIgnoreTheSystem() {
        assertFalse(AppSettings(mode = ThemeMode.LIGHT).resolveDark(systemDark = true))
        assertTrue(AppSettings(mode = ThemeMode.DARK).resolveDark(systemDark = false))
    }

    @Test
    fun keysReadBackToTheirValues() {
        assertEquals(ClayBody.TERRACOTTA, enumFromKey("terracotta", ClayBody.SLATE))
        assertEquals(ThemeMode.DARK, enumFromKey(ThemeMode.DARK.key, ThemeMode.SYSTEM))
        assertEquals(TypePairing.C, enumFromKey("c", TypePairing.B))
    }

    @Test
    fun unknownKeysFallBackToDefaults() {
        assertEquals(ClayBody.SLATE, enumFromKey("porcelain", ClayBody.SLATE))
        assertEquals(ClayBody.SLATE, enumFromKey("TERRACOTTA", ClayBody.SLATE))
        assertEquals(ThemeMode.SYSTEM, enumFromKey("", ThemeMode.SYSTEM))
        assertEquals(TypePairing.B, enumFromKey(null, TypePairing.B))
    }

    @Test
    fun numbersOutOfRangeAreSanitized() {
        val s = AppSettings(newPerDay = 250, reminderHour = 24, reminderMinute = 10).sanitized()
        assertEquals(100, s.newPerDay)
        assertEquals(19, s.reminderHour)
        assertEquals(0, s.reminderMinute)
        assertEquals(0, AppSettings(newPerDay = -5).sanitized().newPerDay)
    }

    @Test
    fun defaultsMatchAFreshInstall() {
        val s = AppSettings()
        assertEquals(ClayBody.SLATE, s.palette)
        assertEquals(ThemeMode.SYSTEM, s.mode)
        assertEquals(TypePairing.B, s.typePairing)
        assertEquals(20, s.newPerDay)
        assertTrue(s.haptics)
        assertFalse(s.reduceMotion)
        assertFalse(s.reminderEnabled)
        assertEquals(19 to 0, s.reminderHour to s.reminderMinute)
        assertEquals(s, s.sanitized())
    }
}
