package com.recall.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.recall.MainActivity
import com.recall.R
import com.recall.RecallApp
import kotlinx.coroutines.flow.first
import kotlin.math.ceil

const val REMINDER_CHANNEL = "reminders"
private const val REMINDER_ID = 1

/** Posts the daily reminder, but only when something is due. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as RecallApp).container
        if (!container.settings.settings.first().reminderEnabled) return Result.success()
        val due = container.decks.summaries.first().sumOf { it.dueCount }
        if (due > 0 && notificationsAllowed(applicationContext)) post(due)
        return Result.success()
    }

    private fun post(due: Int) {
        val context = applicationContext
        val resources = context.resources
        // About 20 seconds a card, as on the Library.
        val minutes = ceil(due * 20 / 60.0).toInt()
        val text = context.getString(
            R.string.reminder_notification_text,
            resources.getQuantityString(R.plurals.reminder_notification_due, due, due),
            resources.getQuantityString(R.plurals.library_minutes, minutes, minutes),
        )
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(REMINDER_ID, notification)
        } catch (_: SecurityException) {
            // The permission was taken away between the check and the post.
        }
    }
}

fun notificationsAllowed(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

fun createReminderChannel(context: Context) {
    val channel = NotificationChannel(
        REMINDER_CHANNEL,
        context.getString(R.string.reminder_channel),
        NotificationManager.IMPORTANCE_DEFAULT,
    )
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}
