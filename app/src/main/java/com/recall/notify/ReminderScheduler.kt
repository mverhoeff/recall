package com.recall.notify

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

private const val WORK_NAME = "daily-reminder"

object ReminderScheduler {
    /** Time until the next [hour]:[minute] on the wall clock, never zero, so a reminder set for now waits a day. */
    fun initialDelay(now: Instant, zone: ZoneId, hour: Int, minute: Int): Duration {
        val local = now.atZone(zone)
        var next = local.toLocalDate().atTime(hour, minute).atZone(zone)
        if (!next.isAfter(local)) next = local.toLocalDate().plusDays(1).atTime(hour, minute).atZone(zone)
        return Duration.between(now, next.toInstant())
    }

    /**
     * Replaces any earlier schedule, so calling it again with the same time is harmless. UPDATE would
     * keep the first schedule's start and count the new delay from there, firing early after a change.
     */
    fun schedule(context: Context, hour: Int, minute: Int, clock: Clock = Clock.systemDefaultZone()) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay(clock.instant(), clock.zone, hour, minute))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
