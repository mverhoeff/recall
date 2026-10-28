package com.recall.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.recall.ui.theme.ClayBody
import com.recall.ui.theme.TypePairing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    val settings: Flow<AppSettings> = dataStore.data
        // An unreadable file shouldn't lock anyone out of the app; defaults are a safe start.
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map(::settingsOf)

    suspend fun setPalette(palette: ClayBody) = dataStore.edit { it[Palette] = palette.key }

    suspend fun setMode(mode: ThemeMode) = dataStore.edit { it[Mode] = mode.key }

    suspend fun setTypePairing(pairing: TypePairing) = dataStore.edit { it[Pairing] = pairing.key }

    suspend fun setNewPerDay(count: Int) = dataStore.edit { it[NewPerDay] = count.coerceIn(0, 100) }

    suspend fun setDailyGoal(goal: Int) = dataStore.edit { it[DailyGoal] = goal.coerceIn(DAILY_GOAL_STEP, DAILY_GOAL_MAX) }

    suspend fun setHaptics(on: Boolean) = dataStore.edit { it[Haptics] = on }

    suspend fun setReduceMotion(on: Boolean) = dataStore.edit { it[ReduceMotion] = on }

    suspend fun setReminderEnabled(on: Boolean) = dataStore.edit { it[ReminderEnabled] = on }

    suspend fun setReminderHour(hour: Int) = dataStore.edit { it[ReminderHour] = hour }

    suspend fun setReminderMinute(minute: Int) = dataStore.edit { it[ReminderMinute] = minute }

    suspend fun replaceAll(s: AppSettings) {
        val clean = s.sanitized()
        dataStore.edit {
            it.clear()
            it[Palette] = clean.palette.key
            it[Mode] = clean.mode.key
            it[Pairing] = clean.typePairing.key
            it[NewPerDay] = clean.newPerDay
            it[DailyGoal] = clean.dailyGoal
            it[Haptics] = clean.haptics
            it[ReduceMotion] = clean.reduceMotion
            it[ReminderEnabled] = clean.reminderEnabled
            it[ReminderHour] = clean.reminderHour
            it[ReminderMinute] = clean.reminderMinute
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val Palette = stringPreferencesKey("palette")
        val Mode = stringPreferencesKey("mode")
        val Pairing = stringPreferencesKey("typePairing")
        val NewPerDay = intPreferencesKey("newPerDay")
        val DailyGoal = intPreferencesKey("dailyGoal")
        val Haptics = booleanPreferencesKey("haptics")
        val ReduceMotion = booleanPreferencesKey("reduceMotion")
        val ReminderEnabled = booleanPreferencesKey("reminderEnabled")
        val ReminderHour = intPreferencesKey("reminderHour")
        val ReminderMinute = intPreferencesKey("reminderMinute")

        fun settingsOf(prefs: Preferences): AppSettings {
            val defaults = AppSettings()
            return AppSettings(
                palette = enumFromKey(prefs[Palette], defaults.palette),
                mode = enumFromKey(prefs[Mode], defaults.mode),
                typePairing = enumFromKey(prefs[Pairing], defaults.typePairing),
                newPerDay = prefs[NewPerDay] ?: defaults.newPerDay,
                dailyGoal = prefs[DailyGoal] ?: defaults.dailyGoal,
                haptics = prefs[Haptics] ?: defaults.haptics,
                reduceMotion = prefs[ReduceMotion] ?: defaults.reduceMotion,
                reminderEnabled = prefs[ReminderEnabled] ?: defaults.reminderEnabled,
                reminderHour = prefs[ReminderHour] ?: defaults.reminderHour,
                reminderMinute = prefs[ReminderMinute] ?: defaults.reminderMinute,
            ).sanitized()
        }
    }
}
