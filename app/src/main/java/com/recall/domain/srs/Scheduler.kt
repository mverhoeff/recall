package com.recall.domain.srs

import com.recall.data.db.Grade
import com.recall.data.db.Phase
import com.recall.data.db.ReviewState
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.roundToInt

object Scheduler {
    const val EASE_START = 2.5
    const val EASE_MIN = 1.3
    const val EASE_MAX = 3.0

    fun schedule(state: ReviewState, grade: Grade, now: Instant, zone: ZoneId): ReviewState {
        val reps = state.reps + 1
        val i = state.intervalDays
        val e = state.ease
        return when (state.phase) {
            Phase.NEW, Phase.LEARNING -> when (grade) {
                Grade.AGAIN -> state.copy(phase = Phase.LEARNING, dueAt = minutesLater(now, 1), reps = reps)
                Grade.HARD -> state.copy(phase = Phase.LEARNING, dueAt = minutesLater(now, 10), reps = reps)
                Grade.GOOD -> review(state, interval = 1, ease = e, now, zone, reps)
                Grade.EASY -> review(state, interval = 4, ease = e + 0.15, now, zone, reps)
            }
            Phase.REVIEW -> when (grade) {
                Grade.AGAIN -> state.copy(
                    phase = Phase.LEARNING,
                    dueAt = minutesLater(now, 10),
                    intervalDays = 1,
                    ease = clampEase(e - 0.20),
                    reps = reps,
                    lapses = state.lapses + 1,
                )
                Grade.HARD -> review(state, max(i + 1, (i * 1.2).roundToInt()), e - 0.15, now, zone, reps)
                Grade.GOOD -> review(state, max(i + 1, (i * e).roundToInt()), e, now, zone, reps)
                Grade.EASY -> review(state, max(i + 1, (i * e * 1.3).roundToInt()), e + 0.15, now, zone, reps)
            }
        }
    }

    /** When the card would come back after [grade], as shown under the flashcard grade buttons. */
    fun previewLabel(state: ReviewState, grade: Grade, now: Instant, zone: ZoneId): String {
        val next = schedule(state, grade, now, zone)
        if (next.phase == Phase.REVIEW) {
            return if (next.intervalDays == 1) "1 day" else "${next.intervalDays} days"
        }
        val minutes = Duration.ofMillis(next.dueAt - now.toEpochMilli()).toMinutes()
        return if (minutes <= 1) "< 1 min" else "$minutes min"
    }

    /** Due cards are the ones in learning or review whose time comes up before today is over. */
    fun isDue(state: ReviewState, now: Instant, zone: ZoneId): Boolean =
        state.phase != Phase.NEW && state.dueAt <= endOfDay(now, zone)

    fun endOfDay(now: Instant, zone: ZoneId): Long =
        now.atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

    /**
     * Day intervals end at 04:00 local time rather than at midnight, so a card answered late in the
     * evening comes back the next morning instead of a few minutes later.
     */
    fun daysLater(now: Instant, days: Int, zone: ZoneId): Long =
        now.atZone(zone).toLocalDate().plusDays(days.toLong()).atTime(4, 0).atZone(zone).toInstant().toEpochMilli()

    private fun minutesLater(now: Instant, minutes: Long): Long = now.plus(Duration.ofMinutes(minutes)).toEpochMilli()

    private fun clampEase(ease: Double) = ease.coerceIn(EASE_MIN, EASE_MAX)

    private fun review(state: ReviewState, interval: Int, ease: Double, now: Instant, zone: ZoneId, reps: Int) =
        state.copy(
            phase = Phase.REVIEW,
            dueAt = daysLater(now, interval, zone),
            intervalDays = interval,
            ease = clampEase(ease),
            reps = reps,
        )
}
