package com.recall.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.recall.data.db.RecallDatabase
import com.recall.data.repo.BackupRepository
import com.recall.data.repo.DeckRepository
import com.recall.data.repo.StudyRepository
import com.recall.data.settings.SettingsRepository
import com.recall.domain.session.SessionStore
import java.time.Clock

private val Context.settingsStore by preferencesDataStore(name = "settings")

/** [testDatabase] lets tests swap in an in-memory database. */
class AppContainer(context: Context, private val testDatabase: RecallDatabase? = null) {
    private val appContext = context.applicationContext

    val clock: Clock = Clock.systemDefaultZone()

    val database: RecallDatabase by lazy { testDatabase ?: RecallDatabase.create(appContext) }

    val settings: SettingsRepository by lazy { SettingsRepository(appContext.settingsStore) }

    val decks: DeckRepository by lazy { DeckRepository(database, settings, clock) }

    val study: StudyRepository by lazy { StudyRepository(database, clock) }

    val backup: BackupRepository by lazy { BackupRepository(database, settings, clock) }

    val sessions = SessionStore()
}
