package com.recall.domain.stats

import java.time.LocalDate

/**
 * Consecutive days with at least one review, ending today. A streak that ended yesterday still
 * counts, so it doesn't read as lost in the morning before the first review of the day.
 */
fun currentStreak(reviewDays: Set<LocalDate>, today: LocalDate): Int {
    var day = if (today in reviewDays) today else today.minusDays(1)
    var count = 0
    while (day in reviewDays) {
        count++
        day = day.minusDays(1)
    }
    return count
}

fun bestStreak(reviewDays: Set<LocalDate>): Int {
    var best = 0
    for (day in reviewDays) {
        // Only count from the first day of each run, so every run is walked once.
        if (day.minusDays(1) in reviewDays) continue
        var length = 1
        while (day.plusDays(length.toLong()) in reviewDays) length++
        best = maxOf(best, length)
    }
    return best
}
