package com.recall.domain.session

import com.recall.data.db.Card
import com.recall.data.db.CardType
import com.recall.data.db.Phase
import com.recall.data.db.ReviewLog
import com.recall.data.db.ReviewState
import com.recall.data.db.StudyMode
import com.recall.domain.srs.Scheduler
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random

object SessionFactory {
    /**
     * A quiz or flashcard round over one deck. With [missedFirst], cards whose latest answer was wrong
     * lead, most recently missed first; the rest follow in random order. [size] null means every card.
     */
    fun practice(
        cards: List<Card>,
        latestLogs: List<ReviewLog>,
        mode: StudyMode,
        size: Int?,
        shuffle: Boolean,
        missedFirst: Boolean,
        random: Random,
    ): List<SessionItem> {
        val byId = cards.associateBy { it.id }
        val missed = if (missedFirst) {
            latestLogs.filterNot { it.correct }.sortedByDescending { it.reviewedAt }.mapNotNull { byId[it.cardId] }
        } else {
            emptyList()
        }
        val missedIds = missed.map { it.id }.toSet()
        val order = missed + cards.filterNot { it.id in missedIds }.shuffled(random)
        return (if (size == null) order else order.take(size)).map { item(it, mode, shuffle, random) }
    }

    /**
     * Today's reviews: learning cards, then review cards, each soonest due first, then new cards in
     * deck order. [newAllowance] caps the new cards per deck, the same way the due counts do.
     */
    fun due(
        cards: List<Pair<Card, ReviewState>>,
        newAllowance: Map<Long, Int>,
        now: Instant,
        zone: ZoneId,
        shuffle: Boolean,
        random: Random,
    ): List<SessionItem> {
        fun dueIn(phase: Phase) = cards
            .filter { (_, state) -> state.phase == phase && Scheduler.isDue(state, now, zone) }
            .sortedBy { (_, state) -> state.dueAt }
            .map { it.first }
        val fresh = cards
            .filter { (_, state) -> state.phase == Phase.NEW }
            .map { it.first }
            .groupBy { it.deckId }
            .flatMap { (deckId, deckCards) -> deckCards.sortedBy { it.position }.take(newAllowance[deckId] ?: 0) }
            .sortedWith(compareBy({ it.position }, { it.deckId }))
        return (dueIn(Phase.LEARNING) + dueIn(Phase.REVIEW) + fresh).map { item(it, StudyMode.DUE, shuffle, random) }
    }

    /** Exactly the given cards, in their order, as a quiz. */
    fun retry(cards: List<Card>, shuffle: Boolean, random: Random): List<SessionItem> =
        cards.map { item(it, StudyMode.QUIZ, shuffle, random) }

    fun item(card: Card, mode: StudyMode, shuffle: Boolean, random: Random): SessionItem {
        val presentation = if (card.type == CardType.FLASH || mode == StudyMode.FLASHCARDS) Presentation.FLASH else Presentation.QUIZ
        val options = when {
            presentation == Presentation.FLASH -> emptyList()
            card.type == CardType.TF -> listOf(TRUE_OPTION, FALSE_OPTION)
            else -> (listOf(card.answer) + card.alsoCorrect + card.wrongAnswers).let { if (shuffle) it.shuffled(random) else it }
        }
        return SessionItem(card, presentation, options)
    }
}
