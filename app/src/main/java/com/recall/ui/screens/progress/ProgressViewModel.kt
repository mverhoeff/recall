package com.recall.ui.screens.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recall.data.repo.ProgressStats
import com.recall.data.repo.StatsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ProgressViewModel(stats: StatsRepository) : ViewModel() {
    val state: StateFlow<ProgressStats?> = stats.progress.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
