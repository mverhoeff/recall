package com.recall.data.repo

import androidx.room.withTransaction
import com.recall.data.db.Card
import com.recall.data.db.Grade
import com.recall.data.db.Phase
import com.recall.data.db.RecallDatabase
import com.recall.data.db.ReviewLog
import com.recall.data.db.ReviewState
import com.recall.data.db.StudyMode
import com.recall.domain.srs.Scheduler
import java.time.Clock

class StudyRepository(
    private val database: RecallDatabase,
    private val clock: Clock,
) {
    private val reviews = database.reviewDao()

    /**
     * Saves one answer. Due reviews and new cards are rescheduled; practising a card ahead of time
     * leaves its schedule alone, so extra rounds can't push reviews further out. The answer is
     * always logged.
     */
    suspend fun recordAnswer(card: Card, grade: Grade, correct: Boolean, mode: StudyMode, firstTry: Boolean) {
        database.withTransaction {
            val now = clock.instant()
            val state = reviews.state(card.id) ?: ReviewState.new(card.id)
            val next = when {
                mode == StudyMode.DUE || state.phase == Phase.NEW || state.dueAt <= now.toEpochMilli() ->
                    Scheduler.schedule(state, grade, now, clock.zone)
                // Forgetting a known card in practice still brings it back into today's review.
                grade == Grade.AGAIN && state.phase == Phase.REVIEW -> state.copy(dueAt = now.toEpochMilli())
                else -> state
            }
            if (next != state) reviews.upsertState(next)
            reviews.insertLog(
                ReviewLog(
                    cardId = card.id,
                    deckId = card.deckId,
                    reviewedAt = now.toEpochMilli(),
                    grade = grade,
                    correct = correct,
                    mode = mode,
                    firstTry = firstTry,
                ),
            )
        }
    }
}
