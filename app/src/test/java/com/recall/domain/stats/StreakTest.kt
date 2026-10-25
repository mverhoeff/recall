package com.recall.domain.stats

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakTest {
    private val today = LocalDate.of(2026, 10, 23)

    private fun daysAgo(vararg offsets: Long) = offsets.map { today.minusDays(it) }.toSet()

    @Test
    fun emptyHistoryHasNoStreak() {
        assertEquals(0, currentStreak(emptySet(), today))
        assertEquals(0, bestStreak(emptySet()))
    }

    @Test
    fun countsBackFromToday() {
        assertEquals(3, currentStreak(daysAgo(0, 1, 2), today))
    }

    @Test
    fun streakEndingYesterdayStillCountsBeforeTodaysReview() {
        assertEquals(2, currentStreak(daysAgo(1, 2), today))
    }

    @Test
    fun gapEndsTheStreak() {
        assertEquals(2, currentStreak(daysAgo(0, 1, 3, 4, 5), today))
        assertEquals(0, currentStreak(daysAgo(2, 3), today))
    }

    @Test
    fun bestStreakIsTheLongestRun() {
        assertEquals(4, bestStreak(daysAgo(0, 1, 10, 11, 12, 13, 20)))
        assertEquals(1, bestStreak(daysAgo(5)))
    }

    @Test
    fun runsCrossMonthBoundaries() {
        val days = setOf(LocalDate.of(2026, 2, 27), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 1))
        assertEquals(3, currentStreak(days, LocalDate.of(2026, 3, 1)))
        assertEquals(3, bestStreak(days))
    }
}
