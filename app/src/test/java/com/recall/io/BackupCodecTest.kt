package com.recall.io

import com.recall.data.db.Grade
import com.recall.data.db.Phase
import com.recall.data.db.StudyMode
import com.recall.data.settings.AppSettings
import com.recall.data.settings.ThemeMode
import com.recall.io.BackupCodec.toSettings
import com.recall.ui.theme.ClayBody
import com.recall.ui.theme.TypePairing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    private val settings = AppSettings(
        palette = ClayBody.TERRACOTTA,
        mode = ThemeMode.DARK,
        typePairing = TypePairing.C,
        newPerDay = 35,
        dailyGoal = 50,
        haptics = false,
        reduceMotion = true,
        reminderEnabled = true,
        reminderHour = 7,
        reminderMinute = 45,
    )

    private val backup = BackupFileDto(
        exportedAt = "2026-10-23T21:00:00Z",
        settings = BackupCodec.settingsDto(settings),
        decks = listOf(
            BackupDeckDto(
                name = "Spanish verbs — A2",
                description = "Present tense",
                createdAt = 100,
                updatedAt = 200,
                cards = listOf(
                    BackupCardDto(
                        type = "mc", prompt = "Yo ___ hambre", answer = "tengo", wrong = listOf("teno", "tiene"),
                        explanation = "From tener", position = 0, createdAt = 100, updatedAt = 150,
                        review = ReviewDto(Phase.REVIEW, dueAt = 9000, intervalDays = 3, ease = 2.35, reps = 4, lapses = 1),
                        logs = listOf(
                            LogDto(reviewedAt = 500, grade = Grade.AGAIN, correct = false, mode = StudyMode.QUIZ, firstTry = true),
                            LogDto(reviewedAt = 800, grade = Grade.GOOD, correct = true, mode = StudyMode.DUE, firstTry = true),
                        ),
                    ),
                    BackupCardDto(type = "card", prompt = "poder · preterite · yo", answer = "pude", position = 1),
                ),
            ),
            BackupDeckDto(name = "Empty"),
        ),
    )

    @Test
    fun encodedBackupDecodesToTheSameModel() {
        val parsed = BackupCodec.decode(BackupCodec.encode(backup)) as ParsedFile.Backup

        assertEquals(backup, parsed.backup)
        assertEquals(2, parsed.deckCount)
        assertEquals(2, parsed.cardCount)
    }

    @Test
    fun settingsSurviveTheTripThroughKeys() {
        val text = BackupCodec.encode(backup)
        assertTrue(text.contains("\"palette\": \"terracotta\""))
        assertTrue(text.contains("\"phase\": \"REVIEW\""))

        val parsed = BackupCodec.decode(text) as ParsedFile.Backup
        assertEquals(settings, parsed.backup.settings.toSettings())
    }

    @Test
    fun unknownSettingValuesFallBackToDefaults() {
        val odd = BackupSettingsDto(palette = "plum", mode = "dusk", newPerDay = 500, reminderMinute = 7)
        val restored = odd.toSettings()

        assertEquals(ClayBody.SLATE, restored.palette)
        assertEquals(ThemeMode.SYSTEM, restored.mode)
        assertEquals(100, restored.newPerDay)
        assertEquals(0, restored.reminderMinute)
    }

    @Test
    fun newerVersionIsRefused() {
        val text = BackupCodec.encode(backup.copy(version = 2))
        assertEquals(ParsedFile.Failure(NEWER_VERSION), BackupCodec.decode(text))
    }

    @Test
    fun unknownCardTypeRefusesTheWholeFile() {
        val bad = backup.copy(decks = listOf(BackupDeckDto(name = "X", cards = listOf(BackupCardDto("essay", "Q", "A")))))
        assertEquals(ParsedFile.Failure(NOT_A_DECK), BackupCodec.decode(BackupCodec.encode(bad)))
    }

    @Test
    fun fileReaderRecognisesBackups() {
        val bytes = BackupCodec.encode(backup).toByteArray()
        assertTrue(DeckFileReader.read(bytes, "recall-backup-2026-10-23.json") is ParsedFile.Backup)
    }
}
