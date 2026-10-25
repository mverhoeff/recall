package com.recall.ui.screens.deck

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recall.data.db.Card
import com.recall.data.db.Phase
import com.recall.data.db.RecallDatabase
import com.recall.data.db.ReviewState
import com.recall.data.db.StudyMode
import com.recall.data.repo.DeckRepository
import com.recall.data.repo.DeckSummary
import com.recall.domain.srs.Scheduler
import com.recall.io.DeckJsonCodec
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface NextDue {
    data object New : NextDue
    data object Due : NextDue
    data class InDays(val days: Long) : NextDue
}

data class CardRow(val card: Card, val next: NextDue)

/** Remembered per deck only while the app runs. */
data class StudyPanel(
    val mode: StudyMode = StudyMode.QUIZ,
    /** Null means every card in the deck. */
    val size: Int? = 10,
    val shuffle: Boolean = true,
    val missedFirst: Boolean = false,
)

data class DeckDetailUiState(
    val summary: DeckSummary,
    /** Share of the last 30 days' answers that were right, or null with no answers in that time. */
    val accuracy: Int?,
    val missedCount: Int,
    val cards: List<CardRow>,
    val panel: StudyPanel,
) {
    /** A size larger than the deck falls back to all of it. */
    val roundSize: Int?
        get() = panel.size?.takeIf { it <= summary.cardCount }
}

class DeckDetailViewModel(
    private val deckId: Long,
    private val repository: DeckRepository,
    private val database: RecallDatabase,
    private val clock: Clock,
) : ViewModel() {
    private val panel = MutableStateFlow(StudyPanel())

    private val details = repository.summary(deckId).map { summary -> summary?.let { load(it) } }

    val state: StateFlow<DeckDetailUiState?> = combine(details, panel) { loaded, panel ->
        loaded?.copy(panel = panel)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectMode(mode: StudyMode) = panel.update { it.copy(mode = mode) }

    fun selectSize(size: Int?) = panel.update { it.copy(size = size) }

    fun setShuffle(on: Boolean) = panel.update { it.copy(shuffle = on) }

    fun setMissedFirst(on: Boolean) = panel.update { it.copy(missedFirst = on) }

    fun rename(name: String) {
        viewModelScope.launch { repository.renameDeck(deckId, name) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteDeck(deckId)
            onDeleted()
        }
    }

    fun export(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val deck = database.deckDao().get(deckId) ?: return@launch
            val text = DeckJsonCodec.encode(deck, database.cardDao().byDeck(deckId))
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

    private suspend fun load(summary: DeckSummary): DeckDetailUiState {
        val now = clock.instant()
        val reviews = database.reviewDao()
        val states = reviews.statesByDeck(deckId).associateBy { it.cardId }
        val recent = reviews.logsSince(now.minus(Duration.ofDays(30)).toEpochMilli()).filter { it.deckId == deckId }
        val cards = database.cardDao().byDeck(deckId).map { CardRow(it, nextDue(states[it.id], now)) }
        return DeckDetailUiState(
            summary = summary,
            accuracy = if (recent.isEmpty()) null else recent.count { it.correct } * 100 / recent.size,
            missedCount = reviews.latestLogPerCard(deckId).count { !it.correct },
            cards = cards,
            panel = panel.value,
        )
    }

    private fun nextDue(state: ReviewState?, now: Instant): NextDue {
        if (state == null || state.phase == Phase.NEW) return NextDue.New
        if (Scheduler.isDue(state, now, clock.zone)) return NextDue.Due
        val today = now.atZone(clock.zone).toLocalDate()
        val dueDay = Instant.ofEpochMilli(state.dueAt).atZone(clock.zone).toLocalDate()
        return NextDue.InDays(ChronoUnit.DAYS.between(today, dueDay))
    }
}
