package com.recall.domain.stats

import com.recall.data.db.ReviewLog
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Monday of the week that contains [today]. */
fun weekStart(today: LocalDate): LocalDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

private fun ReviewLog.day(zone: ZoneId): LocalDate = Instant.ofEpochMilli(reviewedAt).atZone(zone).toLocalDate()

private fun List<ReviewLog>.inWeekOf(today: LocalDate, zone: ZoneId): List<ReviewLog> {
    val monday = weekStart(today)
    val sunday = monday.plusDays(6)
    return filter { log -> log.day(zone).let { it >= monday && it <= sunday } }
}

/** Answers given on each day of the week that contains [today], Monday first. */
fun dailyCounts(logs: List<ReviewLog>, today: LocalDate, zone: ZoneId): List<Int> {
    val monday = weekStart(today)
    val counts = IntArray(7)
    for (log in logs.inWeekOf(today, zone)) {
        counts[(log.day(zone).toEpochDay() - monday.toEpochDay()).toInt()]++
    }
    return counts.toList()
}

/** Share of this week's first tries that were right, in percent, or null without any. */
fun firstTryAccuracy(logs: List<ReviewLog>, today: LocalDate, zone: ZoneId): Int? {
    val firstTries = logs.inWeekOf(today, zone).filter { it.firstTry }
    if (firstTries.isEmpty()) return null
    return firstTries.count { it.correct } * 100 / firstTries.size
}

/**
 * Share of right answers per deck over the 30 days before [now], in percent. Decks without answers
 * in that time are left out. Rounded down, like the deck page's accuracy, so the two always agree.
 */
fun deckAccuracy(logs: List<ReviewLog>, now: Instant): Map<Long, Int> {
    val since = now.minus(Duration.ofDays(30)).toEpochMilli()
    return logs
        .filter { it.reviewedAt >= since }
        .groupBy { it.deckId }
        .mapValues { (_, answers) -> answers.count { it.correct } * 100 / answers.size }
}
