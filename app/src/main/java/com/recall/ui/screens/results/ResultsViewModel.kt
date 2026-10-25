package com.recall.ui.screens.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recall.di.AppContainer
import com.recall.domain.session.SessionResult
import com.recall.domain.stats.currentStreak
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

data class ResultsUiState(val result: SessionResult, val streak: Int)

class ResultsViewModel(sessionKey: String, private val container: AppContainer) : ViewModel() {
    /** The round itself, or null when the app was restarted and it's gone. */
    val result: SessionResult? = container.sessions.get(sessionKey)

    private val _state = MutableStateFlow<ResultsUiState?>(null)
    val state: StateFlow<ResultsUiState?> = _state

    init {
        if (result != null) {
            viewModelScope.launch { _state.value = ResultsUiState(result, streak()) }
        }
    }

    // Worked out the same way as on the Library, so the two never disagree.
    private suspend fun streak(): Int {
        val clock = container.clock
        val today = LocalDate.now(clock)
        val since = today.minusDays(STREAK_WINDOW_DAYS).atStartOfDay(clock.zone).toInstant().toEpochMilli()
        val days = container.database.reviewDao().logsSince(since)
            .map { Instant.ofEpochMilli(it.reviewedAt).atZone(clock.zone).toLocalDate() }
            .toSet()
        return currentStreak(days, today)
    }

    private companion object {
        const val STREAK_WINDOW_DAYS = 400L
    }
}
