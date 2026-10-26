package com.recall.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recall.data.settings.AppSettings
import com.recall.data.settings.SettingsRepository
import com.recall.data.settings.ThemeMode
import com.recall.ui.theme.ClayBody
import com.recall.ui.theme.TypePairing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<AppSettings?> = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setPalette(palette: ClayBody) = write { setPalette(palette) }

    fun setMode(mode: ThemeMode) = write { setMode(mode) }

    fun setTypePairing(pairing: TypePairing) = write { setTypePairing(pairing) }

    private fun write(change: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch { settings.change() }
    }
}
