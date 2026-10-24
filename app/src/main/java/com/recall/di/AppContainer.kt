package com.recall.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.recall.data.db.RecallDatabase
import com.recall.data.settings.SettingsRepository
import java.time.Clock

private val Context.settingsStore by preferencesDataStore(name = "settings")

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val clock: Clock = Clock.systemDefaultZone()

    val database: RecallDatabase by lazy { RecallDatabase.create(appContext) }

    val settings: SettingsRepository by lazy { SettingsRepository(appContext.settingsStore) }
}
