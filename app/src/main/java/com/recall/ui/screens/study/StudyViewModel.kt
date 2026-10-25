package com.recall.ui.screens.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recall.data.db.Card
import com.recall.data.db.Grade
import com.recall.data.db.Phase
import com.recall.data.db.ReviewState
import com.recall.data.db.StudyMode
import com.recall.di.AppContainer
import com.recall.domain.session.Answer
import com.recall.domain.session.SessionFactory
import com.recall.domain.session.SessionItem
import com.recall.domain.session.SessionState
import com.recall.domain.session.StudySession
import com.recall.domain.srs.Scheduler
import com.recall.ui.nav.Study
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import kotlin.random.Random

data class StudyUiState(
    val session: SessionState,
    val deckNames: Map<Long, String>,
    val haptics: Boolean,
    /** Set once the round is over, to open its results. */
    val resultKey: String? = null,
) {
    val answered: Int get() = session.results.size
    val total: Int get() = session.items.size
}

/** When a card would come back after a grade: in minutes while it's being learned, in days after. */
sealed interface Comeback {
    data class Minutes(val minutes: Long) : Comeback
    data class Days(val days: Int) : Comeback
}

data class GradeOption(val grade: Grade, val comeback: Comeback)

class StudyViewModel(private val route: Study, private val container: AppContainer) : ViewModel() {
    private val mode = StudyMode.valueOf(route.mode)
    private var session: StudySession? = null
    private var reviewStates: Map<Long, ReviewState> = emptyMap()

    private val _state = MutableStateFlow<StudyUiState?>(null)
    val state: StateFlow<StudyUiState?> = _state

    /** True when there was nothing to study, so the screen can close straight away. */
    val empty = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val items = buildRound()
            if (items.isEmpty()) {
                empty.value = true
                return@launch
            }
            reviewStates = items.map { it.card.deckId }.distinct()
                .flatMap { container.database.reviewDao().statesByDeck(it) }
                .associateBy { it.cardId }
            val names = container.database.deckDao().observeAll().first().associate { it.id to it.name }
            val started = StudySession(items, container.clock)
            session = started
            _state.value = StudyUiState(started.state, names, container.settings.settings.first().haptics)
        }
    }

    fun select(index: Int) = act { it.select(index) }

    /** Checks the picked answer; returns whether it was right, or null if nothing could be checked. */
    fun check(): Boolean? {
        val answer = session?.check() ?: return null
        record(answer)
        publish()
        return answer.correct
    }

    fun next() = act { it.next() }

    fun flip() = act { it.flip() }

    fun grade(grade: Grade) = act { session -> session.grade(grade)?.let(::record) }

    /** The four grades with when each would bring [card] back, from its schedule as the round started. */
    fun previews(card: Card): List<GradeOption> {
        val state = reviewStates[card.id] ?: ReviewState.new(card.id)
        val now = container.clock.instant()
        return Grade.entries.map { grade ->
            val next = Scheduler.schedule(state, grade, now, container.clock.zone)
            val comeback = if (next.phase == Phase.REVIEW) {
                Comeback.Days(next.intervalDays)
            } else {
                Comeback.Minutes(Duration.ofMillis(next.dueAt - now.toEpochMilli()).toMinutes())
            }
            GradeOption(grade, comeback)
        }
    }

    private fun act(step: (StudySession) -> Unit) {
        session?.let(step) ?: return
        publish()
    }

    private fun publish() {
        val current = session ?: return
        val previous = _state.value ?: return
        val key = if (current.state.finished && previous.resultKey == null) {
            container.sessions.put(current.result())
        } else {
            previous.resultKey
        }
        _state.value = previous.copy(session = current.state, resultKey = key)
    }

    private fun record(answer: Answer) {
        viewModelScope.launch {
            // The round's page may close right after its last answer; the answer is still saved.
            withContext(NonCancellable) {
                container.study.recordAnswer(answer.card, answer.grade, answer.correct, mode, answer.firstTry)
            }
        }
    }

    private suspend fun buildRound(): List<SessionItem> {
        val database = container.database
        val cards = database.cardDao()
        val reviews = database.reviewDao()
        val random = Random.Default
        val retryIds = route.cardIds?.split(',')?.mapNotNull { it.toLongOrNull() }
        return when {
            retryIds != null -> {
                val byId = cards.byIds(retryIds).associateBy { it.id }
                SessionFactory.retry(retryIds.mapNotNull { byId[it] }, route.shuffle, random)
            }
            mode == StudyMode.DUE -> {
                val deckIds = route.deckId?.let(::listOf) ?: database.deckDao().observeAll().first().map { it.id }
                val due = deckIds.flatMap { deckId ->
                    val states = reviews.statesByDeck(deckId).associateBy { it.cardId }
                    cards.byDeck(deckId).mapNotNull { card -> states[card.id]?.let { card to it } }
                }
                val allowance = deckIds.associateWith { container.decks.newCardsDue(it) }
                val now = container.clock.instant()
                SessionFactory.due(due, allowance, now, container.clock.zone, route.shuffle, random)
            }
            else -> {
                val deckId = route.deckId ?: return emptyList()
                SessionFactory.practice(
                    cards.byDeck(deckId),
                    reviews.latestLogPerCard(deckId),
                    mode,
                    route.size.takeIf { it > 0 },
                    route.shuffle,
                    route.missedFirst,
                    random,
                )
            }
        }
    }
}
