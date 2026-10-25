package com.recall.data.repo

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.recall.data.db.Card
import com.recall.data.db.CardType
import com.recall.data.db.Deck
import com.recall.data.db.Grade
import com.recall.data.db.Phase
import com.recall.data.db.RecallDatabase
import com.recall.data.db.ReviewLog
import com.recall.data.db.ReviewState
import com.recall.data.db.StudyMode
import com.recall.data.settings.AppSettings
import com.recall.data.settings.SettingsRepository
import com.recall.data.settings.ThemeMode
import com.recall.io.BackupCodec
import com.recall.io.ParsedFile
import com.recall.ui.theme.ClayBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock = Clock.fixed(Instant.parse("2026-10-23T21:00:00.250Z"), ZoneOffset.UTC)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var settingsFile: File
    private lateinit var db: RecallDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var repo: BackupRepository

    @Before
    fun open() {
        db = Room.inMemoryDatabaseBuilder(context, RecallDatabase::class.java).build()
        settingsFile = context.preferencesDataStoreFile("backup-test")
        settingsFile.delete()
        settings = SettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { settingsFile })
        repo = BackupRepository(db, settings, clock)
    }

    @After
    fun close() {
        db.close()
        scope.cancel()
        settingsFile.delete()
    }

    private suspend fun seedDeck(name: String, cardCount: Int, createdAt: Long): List<Long> {
        val deckId = db.deckDao().insert(Deck(name = name, description = "About $name", createdAt = createdAt, updatedAt = createdAt))
        return (0 until cardCount).map { i ->
            val id = db.cardDao().insertWithState(
                Card(
                    deckId = deckId, type = if (i == 0) CardType.MC else CardType.FLASH,
                    prompt = "$name $i", answer = "A$i", wrongAnswers = if (i == 0) listOf("B", "C") else emptyList(),
                    position = i, createdAt = createdAt, updatedAt = createdAt,
                ),
            )
            db.reviewDao().insertLog(
                ReviewLog(
                    cardId = id, deckId = deckId, reviewedAt = 1000L + i, grade = Grade.GOOD,
                    correct = true, mode = StudyMode.QUIZ, firstTry = true,
                ),
            )
            id
        }
    }

    @Test
    fun restoreBringsBackEverythingThatWasExported() = runBlocking {
        val capitals = seedDeck("Capitals", cardCount = 3, createdAt = 10)
        seedDeck("Verbs", cardCount = 2, createdAt = 20)
        val studied = ReviewState(capitals[0], Phase.REVIEW, dueAt = 9000, intervalDays = 3, ease = 2.35, reps = 4, lapses = 1)
        db.reviewDao().upsertState(studied)
        settings.setPalette(ClayBody.SAGE)
        settings.setMode(ThemeMode.DARK)
        settings.setNewPerDay(35)
        val before = settings.settings.first()

        val text = repo.export()
        assertTrue(text.contains("\"exportedAt\": \"2026-10-23T21:00:00Z\""))

        db.deckDao().deleteAll()
        settings.clear()
        assertEquals(AppSettings(), settings.settings.first())

        val parsed = BackupCodec.decode(text) as ParsedFile.Backup
        repo.restore(parsed.backup)

        val decks = db.deckDao().observeAll().first().sortedBy { it.createdAt }
        assertEquals(listOf("Capitals", "Verbs"), decks.map { it.name })
        assertEquals("About Capitals", decks[0].description)
        val restoredCards = db.cardDao().byDeck(decks[0].id)
        assertEquals(3, restoredCards.size)
        assertEquals(2, db.cardDao().byDeck(decks[1].id).size)
        assertEquals(listOf("B", "C"), restoredCards[0].wrongAnswers)
        assertEquals(5, db.reviewDao().logsSince(0).size)
        assertTrue(db.reviewDao().logsSince(0).all { log -> log.deckId in decks.map { it.id } })

        val state = db.reviewDao().state(restoredCards[0].id)
        assertEquals(studied.copy(cardId = restoredCards[0].id), state)
        assertEquals(ReviewState.new(restoredCards[1].id), db.reviewDao().state(restoredCards[1].id))

        assertEquals(before, settings.settings.first())
    }

    @Test
    fun restoreReplacesWhatWasThere() = runBlocking {
        seedDeck("Capitals", cardCount = 2, createdAt = 10)
        val text = repo.export()
        seedDeck("Leftover", cardCount = 4, createdAt = 30)

        repo.restore((BackupCodec.decode(text) as ParsedFile.Backup).backup)

        assertEquals(listOf("Capitals"), db.deckDao().observeAll().first().map { it.name })
        assertEquals(2, db.reviewDao().logsSince(0).size)
    }
}
