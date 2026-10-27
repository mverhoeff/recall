package com.recall

import android.app.Application
import com.recall.di.AppContainer
import com.recall.notify.ReminderScheduler
import com.recall.notify.createReminderChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RecallApp : Application() {
    lateinit var container: AppContainer

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createReminderChannel(this)
        // Puts the reminder back on its wall-clock time, in case a clock or time zone change moved it.
        scope.launch {
            val settings = container.settings.settings.first()
            if (settings.reminderEnabled) ReminderScheduler.schedule(this@RecallApp, settings.reminderHour, settings.reminderMinute)
        }
    }
}
