package com.recall.notify

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderSchedulerTest {
    private val zone = ZoneId.of("Europe/Berlin")

    private fun at(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant()

    @Test
    fun laterTodayWaitsUntilThen() {
        assertEquals(Duration.ofHours(3), ReminderScheduler.initialDelay(at("2026-10-20T16:00"), zone, 19, 0))
        assertEquals(Duration.ofMinutes(15), ReminderScheduler.initialDelay(at("2026-10-20T19:00"), zone, 19, 15))
    }

    @Test
    fun timeAlreadyPassedMovesToTomorrow() {
        assertEquals(Duration.ofHours(23), ReminderScheduler.initialDelay(at("2026-10-20T20:00"), zone, 19, 0))
    }

    @Test
    fun exactlyNowWaitsAFullDay() {
        assertEquals(Duration.ofHours(24), ReminderScheduler.initialDelay(at("2026-10-20T19:00"), zone, 19, 0))
    }

    @Test
    fun midnightCrossesIntoTheNextDay() {
        assertEquals(Duration.ofMinutes(30), ReminderScheduler.initialDelay(at("2026-10-20T23:30"), zone, 0, 0))
    }

    @Test
    fun keepsTheWallClockTimeOverDaylightSavingChanges() {
        // Clocks go back an hour on 25 October 2026 and forward on 29 March 2026.
        assertEquals(Duration.ofHours(25), ReminderScheduler.initialDelay(at("2026-10-24T19:00"), zone, 19, 0))
        assertEquals(Duration.ofHours(23), ReminderScheduler.initialDelay(at("2026-03-28T19:00"), zone, 19, 0))
    }
}
