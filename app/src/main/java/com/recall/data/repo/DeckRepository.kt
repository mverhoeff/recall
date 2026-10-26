package com.recall.data.repo

import androidx.room.withTransaction
import com.recall.data.db.Card
import com.recall.data.db.Deck
import com.recall.data.db.Phase
import com.recall.data.db.RecallDatabase
import com.recall.data.db.ReviewState
import com.recall.data.settings.SettingsRepository
import com.recall.domain.srs.Scheduler
import com.recall.io.ImportedCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

data class DeckSummary(
    val deck: Deck,
    val cardCount: Int,
    val knownCount: Int,
    val learningCount: Int,
    val newCount: Int,
    val dueCount: Int,
)

data class DeckDetails(
    val summary: DeckSummary,
    /** Share of the last 30 days' answers that were right, or null with no answers in that time. */
    val accuracy: Int?,
    /** Cards whose latest answer was wrong. */
    val missedCount: Int,
    val cards: List<Pair<Card, ReviewState?>>,
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

    /** One deck's counts, or null once it is deleted. Cheaper than picking it out of [summaries]. */
    fun summary(deckId: Long): Flow<DeckSummary?> = combine(
        database.invalidationTracker.createFlow("decks", "cards", "review_state", "review_logs"),
        settings.settings.map { it.newPerDay }.distinctUntilChanged(),
    ) { _, newPerDay -> newPerDay }
        .map { newPerDay -> decks.get(deckId)?.let { summarize(it, newPerDay) } }

    // The last details read per deck, so a deck page can draw its content on its very first frame.
    private val detailsCache = ConcurrentHashMap<Long, DeckDetails>()

    fun cachedDetails(deckId: Long): DeckDetails? = detailsCache[deckId]

    fun details(deckId: Long): Flow<DeckDetails?> = summary(deckId).map { summary ->
        summary?.let { loadDetails(it) }.also { remember(deckId, it) }
    }

    /** Reads a deck's details ahead of opening it; the deck page then starts from [cachedDetails]. */
    suspend fun prefetchDetails(deckId: Long) {
        remember(deckId, summary(deckId).first()?.let { loadDetails(it) })
    }

    private fun remember(deckId: Long, details: DeckDetails?) {
        if (details == null) detailsCache.remove(deckId) else detailsCache[deckId] = details
    }

    private suspend fun loadDetails(summary: DeckSummary): DeckDetails {
        val deckId = summary.deck.id
        val states = reviews.statesByDeck(deckId).associateBy { it.cardId }
        val recent = reviews.logsSince(clock.instant().minus(Duration.ofDays(30)).toEpochMilli()).filter { it.deckId == deckId }
        return DeckDetails(
            summary = summary,
            accuracy = if (recent.isEmpty()) null else recent.count { it.correct } * 100 / recent.size,
            missedCount = reviews.latestLogPerCard(deckId).count { !it.correct },
            cards = cards.byDeck(deckId).map { it to states[it.id] },
        )
    }

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

    /**
     * Appends [imported] to the deck [deckId], or to a new deck named [newDeckName] when [deckId] is null,
     * all in one transaction. Returns the deck's id.
     */
    suspend fun importCards(deckId: Long?, newDeckName: String?, imported: List<ImportedCard>): Long = database.withTransaction {
        val now = clock.millis()
        val target = deckId ?: decks.insert(Deck(name = newDeckName.orEmpty().trim(), createdAt = now, updatedAt = now))
        val start = (cards.maxPosition(target) ?: -1) + 1
        imported.forEachIndexed { index, card ->
            cards.insertWithState(
                Card(
                    deckId = target,
                    type = card.type,
                    prompt = card.prompt,
                    answer = card.answer,
                    wrongAnswers = card.wrong,
                    alsoCorrect = card.alsoCorrect,
                    explanation = card.explanation,
                    position = start + index,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
        target
    }

    suspend fun updateCard(card: Card) = cards.update(card.copy(updatedAt = clock.millis()))

    suspend fun deleteCard(cardId: Long) = cards.delete(cardId)

    /** How many of a deck's new cards today's review takes, the same number its due count includes. */
    suspend fun newCardsDue(deckId: Long): Int {
        val newCount = reviews.statesByDeck(deckId).count { it.phase == Phase.NEW }
        return newDue(deckId, newCount, settings.settings.first().newPerDay)
    }

    private suspend fun newDue(deckId: Long, newCount: Int, newPerDay: Int): Int {
        val now = clock.instant()
        val zone = clock.zone
        val startOfToday = now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        val startedToday = reviews.firstLogTimes(deckId).count { it.firstAt >= startOfToday }
        return minOf(newPerDay - startedToday, newCount).coerceAtLeast(0)
    }

    private suspend fun summarize(deck: Deck, newPerDay: Int): DeckSummary {
        val now = clock.instant()
        val zone = clock.zone
        val states = reviews.statesByDeck(deck.id)
        val newCount = states.count { it.phase == Phase.NEW }
        val newDue = newDue(deck.id, newCount, newPerDay)
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
