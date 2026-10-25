package com.recall.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recall.data.db.ReviewDao
import com.recall.data.repo.DeckRepository
import com.recall.data.repo.DeckSummary
import com.recall.domain.stats.currentStreak
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlin.math.ceil

enum class DeckSort { DUE_FIRST, A_Z }

// Past this the deck opens anyway and its content fades in when it arrives.
private const val OPEN_WAIT_MS = 300L

data class LibraryUiState(
    val today: LocalDate,
    val decks: List<DeckSummary>,
    val hasDecks: Boolean,
    val dueTotal: Int,
    val dueDeckCount: Int,
    val streak: Int,
    val sort: DeckSort,
    /** Null while the search field is closed. */
    val query: String?,
) {
    /** About 20 seconds a card. */
    val dueMinutes: Int get() = ceil(dueTotal * 20 / 60.0).toInt()
}

class LibraryViewModel(
    private val repository: DeckRepository,
    reviews: ReviewDao,
    private val clock: Clock,
) : ViewModel() {
    private val sort = MutableStateFlow(DeckSort.DUE_FIRST)
    private val query = MutableStateFlow<String?>(null)

    // Only the last 400 days are read, which caps the streak there instead of loading every log.
    private val streak = reviews.observeLogsSince(startOfToday().minusSeconds(400L * 24 * 60 * 60).toEpochMilli())
        .map { logs ->
            val days = logs.map { Instant.ofEpochMilli(it.reviewedAt).atZone(clock.zone).toLocalDate() }.toSet()
            currentStreak(days, LocalDate.now(clock))
        }

    val state: StateFlow<LibraryUiState?> = combine(repository.summaries, streak, sort, query) { all, streak, sort, query ->
        val shown = all
            .filter { query.isNullOrBlank() || it.deck.name.contains(query.trim(), ignoreCase = true) }
            .sortedWith(
                when (sort) {
                    DeckSort.DUE_FIRST -> compareByDescending<DeckSummary> { it.dueCount }.thenBy { it.deck.name.lowercase() }
                    DeckSort.A_Z -> compareBy { it.deck.name.lowercase() }
                },
            )
        LibraryUiState(
            today = LocalDate.now(clock),
            decks = shown,
            hasDecks = all.isNotEmpty(),
            dueTotal = all.sumOf { it.dueCount },
            dueDeckCount = all.count { it.dueCount > 0 },
            streak = streak,
            sort = sort,
            query = query,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggleSort() {
        sort.value = if (sort.value == DeckSort.DUE_FIRST) DeckSort.A_Z else DeckSort.DUE_FIRST
    }

    fun openSearch() {
        query.value = ""
    }

    fun closeSearch() {
        query.value = null
    }

    fun search(text: String) {
        query.value = text
    }

    private var opening = false

    /** Opens a deck once its page has something to show, so it slides in full rather than filling up late. */
    fun openDeck(deckId: Long, onReady: (Long) -> Unit) {
        if (opening) return
        opening = true
        viewModelScope.launch {
            withTimeoutOrNull(OPEN_WAIT_MS) { repository.prefetchDetails(deckId) }
            opening = false
            onReady(deckId)
        }
    }

    fun createDeck(name: String, description: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val deckId = repository.createDeck(name, description)
            withTimeoutOrNull(OPEN_WAIT_MS) { repository.prefetchDetails(deckId) }
            onCreated(deckId)
        }
    }

    private fun startOfToday(): Instant = LocalDate.now(clock).atStartOfDay(clock.zone).toInstant()
}
