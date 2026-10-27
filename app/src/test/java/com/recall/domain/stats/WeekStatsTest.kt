package com.recall.domain.stats

import com.recall.data.db.Grade
import com.recall.data.db.ReviewLog
import com.recall.data.db.StudyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class WeekStatsTest {
    private val zone = ZoneId.of("Europe/Berlin")

    // Thursday.
    private val today = LocalDate.of(2026, 10, 22)

    private fun log(
        at: LocalDateTime,
        correct: Boolean = true,
        firstTry: Boolean = true,
        deckId: Long = 1,
    ) = ReviewLog(
        cardId = 1,
        deckId = deckId,
        reviewedAt = at.atZone(zone).toInstant().toEpochMilli(),
        grade = if (correct) Grade.GOOD else Grade.AGAIN,
        correct = correct,
        mode = StudyMode.QUIZ,
        firstTry = firstTry,
    )

    private fun on(day: LocalDate, hour: Int = 12) = day.atTime(hour, 0)

    @Test
    fun weekStartsOnMonday() {
        assertEquals(LocalDate.of(2026, 10, 19), weekStart(today))
        assertEquals(LocalDate.of(2026, 10, 19), weekStart(LocalDate.of(2026, 10, 19)))
        assertEquals(LocalDate.of(2026, 10, 19), weekStart(LocalDate.of(2026, 10, 25)))
    }

    @Test
    fun countsEachDayOfTheWeekMondayFirst() {
        val logs = listOf(
            log(on(LocalDate.of(2026, 10, 19))),
            log(on(LocalDate.of(2026, 10, 19), hour = 23)),
            log(on(LocalDate.of(2026, 10, 21))),
            log(on(today, hour = 0)),
        )
        assertEquals(listOf(2, 0, 1, 1, 0, 0, 0), dailyCounts(logs, today, zone))
    }

    @Test
    fun leavesOutTheWeeksAroundIt() {
        val logs = listOf(
            log(LocalDateTime.of(2026, 10, 18, 23, 59)),
            log(LocalDateTime.of(2026, 10, 19, 0, 0)),
            log(LocalDateTime.of(2026, 10, 25, 23, 59)),
            log(LocalDateTime.of(2026, 10, 26, 0, 0)),
        )
        assertEquals(listOf(1, 0, 0, 0, 0, 0, 1), dailyCounts(logs, today, zone))
    }

    @Test
    fun weekCanSpanTwoMonths() {
        val sunday = LocalDate.of(2026, 11, 1)
        val logs = listOf(log(on(LocalDate.of(2026, 10, 26))), log(on(sunday)), log(on(LocalDate.of(2026, 11, 2))))
        assertEquals(listOf(1, 0, 0, 0, 0, 0, 1), dailyCounts(logs, sunday, zone))
    }

    @Test
    fun firstTryAccuracyIgnoresRetriesAndOtherWeeks() {
        val logs = listOf(
            log(on(today), correct = true),
            log(on(today), correct = true),
            log(on(today), correct = false),
            log(on(today), correct = true, firstTry = false),
            log(on(LocalDate.of(2026, 10, 12)), correct = false),
        )
        assertEquals(66, firstTryAccuracy(logs, today, zone))
    }

    @Test
    fun noFirstTriesThisWeekHasNoAccuracy() {
        assertNull(firstTryAccuracy(emptyList(), today, zone))
        assertNull(firstTryAccuracy(listOf(log(on(today), firstTry = false)), today, zone))
        assertNull(firstTryAccuracy(listOf(log(on(LocalDate.of(2026, 10, 18)))), today, zone))
    }

    @Test
    fun deckAccuracyCoversTheLast30DaysPerDeck() {
        val now = on(today).atZone(zone).toInstant()
        val logs = listOf(
            log(on(today), correct = true, deckId = 1),
            log(on(today), correct = false, deckId = 1),
            log(on(today), correct = false, deckId = 1),
            log(on(today.minusDays(29)), correct = true, deckId = 2),
            log(on(today.minusDays(31)), correct = false, deckId = 2),
            log(on(today.minusDays(40)), correct = true, deckId = 3),
        )
        assertEquals(mapOf(1L to 33, 2L to 100), deckAccuracy(logs, now))
    }
}
