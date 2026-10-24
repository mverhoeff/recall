package com.recall.domain.srs

import com.recall.data.db.Grade
import com.recall.data.db.Phase
import com.recall.data.db.ReviewState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class SchedulerTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val now = at(2026, 10, 23, 21, 30)

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Instant =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant()

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int) = at(year, month, day, hour, minute).toEpochMilli()

    private fun minutesFromNow(minutes: Long) = now.plus(Duration.ofMinutes(minutes)).toEpochMilli()

    private val new = ReviewState.new(cardId = 1)
    private val learning = new.copy(phase = Phase.LEARNING, dueAt = now.toEpochMilli(), reps = 1)
    private val review = ReviewState(1, Phase.REVIEW, dueAt = 0, intervalDays = 10, ease = 2.5, reps = 4, lapses = 1)

    private fun schedule(state: ReviewState, grade: Grade, instant: Instant = now) =
        Scheduler.schedule(state, grade, instant, zone)

    @Test
    fun againOnNewOrLearningComesBackInAMinute() {
        for (state in listOf(new, learning)) {
            val next = schedule(state, Grade.AGAIN)
            assertEquals(Phase.LEARNING, next.phase)
            assertEquals(minutesFromNow(1), next.dueAt)
            assertEquals(state.ease, next.ease, 0.0)
            assertEquals(state.reps + 1, next.reps)
        }
    }

    @Test
    fun hardOnNewOrLearningComesBackInTenMinutes() {
        for (state in listOf(new, learning)) {
            val next = schedule(state, Grade.HARD)
            assertEquals(Phase.LEARNING, next.phase)
            assertEquals(minutesFromNow(10), next.dueAt)
            assertEquals(state.reps + 1, next.reps)
        }
    }

    @Test
    fun goodOnNewOrLearningGraduatesForOneDay() {
        for (state in listOf(new, learning)) {
            val next = schedule(state, Grade.GOOD)
            assertEquals(Phase.REVIEW, next.phase)
            assertEquals(1, next.intervalDays)
            assertEquals(millis(2026, 10, 24, 4, 0), next.dueAt)
            assertEquals(2.5, next.ease, 1e-9)
        }
    }

    @Test
    fun easyOnNewOrLearningGraduatesForFourDays() {
        for (state in listOf(new, learning)) {
            val next = schedule(state, Grade.EASY)
            assertEquals(Phase.REVIEW, next.phase)
            assertEquals(4, next.intervalDays)
            assertEquals(millis(2026, 10, 27, 4, 0), next.dueAt)
            assertEquals(2.65, next.ease, 1e-9)
        }
    }

    @Test
    fun againOnReviewLapsesIntoLearning() {
        val next = schedule(review, Grade.AGAIN)
        assertEquals(Phase.LEARNING, next.phase)
        assertEquals(minutesFromNow(10), next.dueAt)
        assertEquals(1, next.intervalDays)
        assertEquals(2.3, next.ease, 1e-9)
        assertEquals(2, next.lapses)
        assertEquals(5, next.reps)
    }

    @Test
    fun hardOnReviewGrowsSlowlyAndLowersEase() {
        val next = schedule(review, Grade.HARD)
        assertEquals(Phase.REVIEW, next.phase)
        assertEquals(12, next.intervalDays)
        assertEquals(millis(2026, 11, 4, 4, 0), next.dueAt)
        assertEquals(2.35, next.ease, 1e-9)
        assertEquals(1, next.lapses)
    }

    @Test
    fun goodOnReviewMultipliesByEase() {
        val next = schedule(review, Grade.GOOD)
        assertEquals(Phase.REVIEW, next.phase)
        assertEquals(25, next.intervalDays)
        assertEquals(millis(2026, 11, 17, 4, 0), next.dueAt)
        assertEquals(2.5, next.ease, 1e-9)
    }

    @Test
    fun easyOnReviewAddsABonusAndRaisesEase() {
        val next = schedule(review, Grade.EASY)
        assertEquals(Phase.REVIEW, next.phase)
        // 10 × 2.5 × 1.3 = 32.5, and halves round up.
        assertEquals(33, next.intervalDays)
        assertEquals(2.65, next.ease, 1e-9)
    }

    @Test
    fun shortIntervalsStillGrowByAtLeastOneDay() {
        val next = schedule(review.copy(intervalDays = 1), Grade.GOOD)
        assertEquals(3, next.intervalDays)
        assertEquals(2, schedule(review.copy(intervalDays = 1), Grade.HARD).intervalDays)
    }

    @Test
    fun easeNeverDropsBelowMinimum() {
        var state = review
        repeat(10) {
            state = schedule(state, Grade.AGAIN)
            state = schedule(state, Grade.GOOD)
        }
        assertEquals(Scheduler.EASE_MIN, state.ease, 1e-9)
    }

    @Test
    fun easeNeverRisesAboveMaximum() {
        var state = review
        repeat(10) { state = schedule(state, Grade.EASY) }
        assertEquals(Scheduler.EASE_MAX, state.ease, 1e-9)
    }

    @Test
    fun lateNightAnswerIsDueNextMorning() {
        val next = schedule(new, Grade.GOOD, at(2026, 10, 23, 23, 30))
        assertEquals(millis(2026, 10, 24, 4, 0), next.dueAt)
    }

    @Test
    fun dayIntervalsKeepFourOClockAcrossDaylightSavingEnd() {
        // Clocks in Berlin go back an hour early on 25 October 2026.
        val next = schedule(new, Grade.GOOD, at(2026, 10, 24, 21, 30))
        val local = Instant.ofEpochMilli(next.dueAt).atZone(zone).toLocalDateTime()
        assertEquals(LocalDateTime.of(2026, 10, 25, 4, 0), local)
        assertEquals(Instant.parse("2026-10-25T03:00:00Z").toEpochMilli(), next.dueAt)
    }

    private fun preview(state: ReviewState, grade: Grade) = Scheduler.previewLabel(state, grade, now, zone)

    @Test
    fun previewsForANewCard() {
        assertEquals("< 1 min", preview(new, Grade.AGAIN))
        assertEquals("10 min", preview(new, Grade.HARD))
        assertEquals("1 day", preview(new, Grade.GOOD))
        assertEquals("4 days", preview(new, Grade.EASY))
    }

    @Test
    fun previewsForAReviewCard() {
        assertEquals("10 min", preview(review, Grade.AGAIN))
        assertEquals("12 days", preview(review, Grade.HARD))
        assertEquals("25 days", preview(review, Grade.GOOD))
        assertEquals("33 days", preview(review, Grade.EASY))
    }

    @Test
    fun dueMeansBeforeTodayIsOver() {
        val endOfToday = millis(2026, 10, 24, 0, 0) - 1
        assertTrue(Scheduler.isDue(review.copy(dueAt = endOfToday), now, zone))
        assertFalse(Scheduler.isDue(review.copy(dueAt = endOfToday + 1), now, zone))
        assertTrue(Scheduler.isDue(learning.copy(dueAt = 0), now, zone))
        assertFalse(Scheduler.isDue(new, now, zone))
    }
}
