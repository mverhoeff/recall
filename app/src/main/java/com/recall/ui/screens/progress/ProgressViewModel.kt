package com.recall.ui.screens.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recall.data.repo.ProgressStats
import com.recall.data.repo.StatsRepository
import com.recall.data.settings.AppSettings
import com.recall.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ProgressViewModel(stats: StatsRepository, settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<ProgressStats?> = stats.progress.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val dailyGoal: StateFlow<Int> = settings.settings.map { it.dailyGoal }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings().dailyGoal)
}
