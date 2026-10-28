package com.recall.data.settings

import com.recall.ui.theme.ClayBody
import com.recall.ui.theme.TypePairing

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val palette: ClayBody = ClayBody.SLATE,
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val typePairing: TypePairing = TypePairing.B,
    val newPerDay: Int = 20,
    val dailyGoal: Int = 30,
    val haptics: Boolean = true,
    val reduceMotion: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
) {
    /** Pulls out-of-range numbers back into what the settings screen can offer. */
    fun sanitized(): AppSettings {
        val defaults = AppSettings()
        return copy(
            newPerDay = newPerDay.coerceIn(0, 100),
            dailyGoal = dailyGoal.coerceIn(DAILY_GOAL_STEP, DAILY_GOAL_MAX),
            reminderHour = if (reminderHour in 0..23) reminderHour else defaults.reminderHour,
            reminderMinute = if (reminderMinute in ReminderMinutes) reminderMinute else defaults.reminderMinute,
        )
    }
}

val ReminderMinutes = listOf(0, 15, 30, 45)

const val DAILY_GOAL_STEP = 10
const val DAILY_GOAL_MAX = 200

fun AppSettings.resolveDark(systemDark: Boolean): Boolean = when (mode) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/** The lowercase form stored on disk and in backups, e.g. `terracotta`. */
val Enum<*>.key: String get() = name.lowercase()

/** Reads a stored key back, falling back when it is missing or no longer known. */
inline fun <reified E : Enum<E>> enumFromKey(value: String?, fallback: E): E =
    enumValues<E>().firstOrNull { it.key == value } ?: fallback
