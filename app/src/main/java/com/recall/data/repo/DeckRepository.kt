package com.recall.data.repo

import com.recall.data.db.Card
import com.recall.data.db.Deck
import com.recall.data.db.Phase
import com.recall.data.db.RecallDatabase
import com.recall.data.settings.SettingsRepository
import com.recall.domain.srs.Scheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock

data class DeckSummary(
    val deck: Deck,
    val cardCount: Int,
    val knownCount: Int,
    val learningCount: Int,
    val newCount: Int,
    val dueCount: Int,
)

class DeckRepository(
    private val database: RecallDatabase,
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    private val decks = database.deckDao()
    private val cards = database.cardDao()
    private val reviews = database.reviewDao()

    // Counts come from cards, states and logs, so any write to those tables recomputes every deck.
    val summaries: Flow<List<DeckSummary>> = combine(
        decks.observeAll(),
        database.invalidationTracker.createFlow("cards", "review_state", "review_logs"),
        settings.settings.map { it.newPerDay }.distinctUntilChanged(),
    ) { all, _, newPerDay -> all to newPerDay }
        .map { (all, newPerDay) -> all.map { summarize(it, newPerDay) } }

    fun cardsForDeck(deckId: Long): Flow<List<Card>> = cards.observeByDeck(deckId)

    suspend fun createDeck(name: String, description: String = ""): Long {
        val now = clock.millis()
        return decks.insert(Deck(name = name.trim(), description = description.trim(), createdAt = now, updatedAt = now))
    }

    suspend fun renameDeck(deckId: Long, name: String) {
        val deck = decks.get(deckId) ?: return
        decks.update(deck.copy(name = name.trim(), updatedAt = clock.millis()))
    }

    suspend fun deleteDeck(deckId: Long) = decks.delete(deckId)

    /** Appends [card] to the end of its deck; its id, position and timestamps are filled in here. */
    suspend fun addCard(card: Card): Long {
        val now = clock.millis()
        val position = (cards.maxPosition(card.deckId) ?: -1) + 1
        return cards.insertWithState(card.copy(id = 0, position = position, createdAt = now, updatedAt = now))
    }

    suspend fun updateCard(card: Card) = cards.update(card.copy(updatedAt = clock.millis()))

    suspend fun deleteCard(cardId: Long) = cards.delete(cardId)

    private suspend fun summarize(deck: Deck, newPerDay: Int): DeckSummary {
        val now = clock.instant()
        val zone = clock.zone
        val states = reviews.statesByDeck(deck.id)
        val startOfToday = now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        val startedToday = reviews.firstLogTimes(deck.id).count { it.firstAt >= startOfToday }
        val newCount = states.count { it.phase == Phase.NEW }
        val newDue = minOf(newPerDay - startedToday, newCount).coerceAtLeast(0)
        return DeckSummary(
            deck = deck,
            cardCount = states.size,
            knownCount = states.count { it.phase == Phase.REVIEW },
            learningCount = states.count { it.phase == Phase.LEARNING },
            newCount = newCount,
            dueCount = states.count { Scheduler.isDue(it, now, zone) } + newDue,
        )
    }
}
