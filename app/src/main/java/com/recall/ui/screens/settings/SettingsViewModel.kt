package com.recall.ui.screens.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recall.data.db.RecallDatabase
import com.recall.data.repo.BackupRepository
import com.recall.data.settings.AppSettings
import com.recall.data.settings.SettingsRepository
import com.recall.data.settings.ThemeMode
import com.recall.ui.theme.ClayBody
import com.recall.ui.theme.TypePairing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

const val NEW_PER_DAY_STEP = 5
const val NEW_PER_DAY_MAX = 100

/**
 * [onReminderChanged] hears about every change to the reminder's switch or time, with the settings
 * as they are after the change, so the daily reminder can be scheduled to match.
 */
class SettingsViewModel(
    private val settings: SettingsRepository,
    private val backups: BackupRepository,
    private val database: RecallDatabase,
    private val onReminderChanged: (AppSettings) -> Unit = {},
) : ViewModel() {
    val state: StateFlow<AppSettings?> = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setPalette(palette: ClayBody) = write { setPalette(palette) }

    fun setMode(mode: ThemeMode) = write { setMode(mode) }

    fun setTypePairing(pairing: TypePairing) = write { setTypePairing(pairing) }

    fun changeNewPerDay(by: Int) {
        val current = state.value ?: return
        val next = (current.newPerDay + by).coerceIn(0, NEW_PER_DAY_MAX)
        write { setNewPerDay(next) }
    }

    fun setHaptics(on: Boolean) = write { setHaptics(on) }

    fun setReduceMotion(on: Boolean) = write { setReduceMotion(on) }

    fun setReminderEnabled(on: Boolean) = writeReminder { setReminderEnabled(on) }

    fun changeReminderHour(by: Int) {
        val current = state.value ?: return
        writeReminder { setReminderHour(Math.floorMod(current.reminderHour + by, 24)) }
    }

    fun setReminderMinute(minute: Int) = writeReminder { setReminderMinute(minute) }

    fun backUp(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val text = backups.export()
            withContext(Dispatchers.IO) {
                // The picker can hand back a location that refuses writes; there is nothing to recover then.
                try {
                    resolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
                } catch (_: IOException) {
                } catch (_: SecurityException) {
                }
            }
        }
    }

    /** Removes every deck, card, review and setting; [onDeleted] runs once they are gone. */
    fun deleteEverything(onDeleted: () -> Unit) {
        viewModelScope.launch {
            // Cards, their states and their logs go with the decks.
            database.deckDao().deleteAll()
            settings.clear()
            onReminderChanged(settings.settings.first())
            onDeleted()
        }
    }

    private fun write(change: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch { settings.change() }
    }

    private fun writeReminder(change: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch {
            settings.change()
            onReminderChanged(settings.settings.first())
        }
    }
}
