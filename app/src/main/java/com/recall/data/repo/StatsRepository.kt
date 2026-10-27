package com.recall.data.repo

import com.recall.data.db.RecallDatabase
import com.recall.domain.stats.bestStreak
import com.recall.domain.stats.currentStreak
import com.recall.domain.stats.dailyCounts
import com.recall.domain.stats.deckAccuracy
import com.recall.domain.stats.firstTryAccuracy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

data class DeckAccuracy(val deckId: Long, val name: String, val percent: Int)

data class ProgressStats(
    /** False until the first answer, which shows the empty state instead. */
    val hasReviews: Boolean,
    val today: LocalDate,
    val streak: Int,
    val bestStreak: Int,
    /** Answers per day of this week, Monday first. */
    val week: List<Int>,
    val firstTryPercent: Int?,
    val decks: List<DeckAccuracy>,
) {
    val weekTotal: Int get() = week.sum()
}

class StatsRepository(database: RecallDatabase, private val clock: Clock) {
    private val decks = database.deckDao()
    private val reviews = database.reviewDao()

    // The same 400 days the Library reads its streak from, so the two never disagree.
    val progress: Flow<ProgressStats> = combine(
        decks.observeAll(),
        reviews.observeLogsSince(LocalDate.now(clock).minusDays(400).atStartOfDay(clock.zone).toInstant().toEpochMilli()),
    ) { allDecks, logs ->
        val today = LocalDate.now(clock)
        val days = logs.map { Instant.ofEpochMilli(it.reviewedAt).atZone(clock.zone).toLocalDate() }.toSet()
        val accuracy = deckAccuracy(logs, clock.instant())
        ProgressStats(
            hasReviews = logs.isNotEmpty(),
            today = today,
            streak = currentStreak(days, today),
            bestStreak = bestStreak(days),
            week = dailyCounts(logs, today, clock.zone),
            firstTryPercent = firstTryAccuracy(logs, today, clock.zone),
            decks = allDecks
                .mapNotNull { deck -> accuracy[deck.id]?.let { DeckAccuracy(deck.id, deck.name, it) } }
                .sortedBy { it.name.lowercase() },
        )
    }.flowOn(Dispatchers.Default)
}
