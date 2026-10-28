package com.recall.io

import com.recall.data.db.Grade
import com.recall.data.db.Phase
import com.recall.data.db.StudyMode
import com.recall.data.settings.AppSettings
import com.recall.data.settings.enumFromKey
import com.recall.data.settings.key
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

const val BACKUP_FORMAT = "recall-backup"

@Serializable
data class BackupFileDto(
    val format: String = BACKUP_FORMAT,
    val version: Int = 1,
    val exportedAt: String = "",
    val settings: BackupSettingsDto = BackupSettingsDto(),
    val decks: List<BackupDeckDto> = emptyList(),
)

/** Settings travel as their stored keys, so a file stays readable if the enums are renamed. */
@Serializable
data class BackupSettingsDto(
    val palette: String = "slate",
    val mode: String = "system",
    val typePairing: String = "b",
    val newPerDay: Int = 20,
    val dailyGoal: Int = 30,
    val haptics: Boolean = true,
    val reduceMotion: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
)

@Serializable
data class BackupDeckDto(
    val name: String,
    val description: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val cards: List<BackupCardDto> = emptyList(),
)

@Serializable
data class BackupCardDto(
    val type: String,
    val prompt: String,
    val answer: String,
    val wrong: List<String> = emptyList(),
    val alsoCorrect: List<String> = emptyList(),
    val explanation: String = "",
    val position: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val review: ReviewDto = ReviewDto(),
    val logs: List<LogDto> = emptyList(),
)

@Serializable
data class ReviewDto(
    val phase: Phase = Phase.NEW,
    val dueAt: Long = 0,
    val intervalDays: Int = 0,
    val ease: Double = 2.5,
    val reps: Int = 0,
    val lapses: Int = 0,
)

@Serializable
data class LogDto(
    val reviewedAt: Long,
    val grade: Grade,
    val correct: Boolean,
    val mode: StudyMode,
    val firstTry: Boolean,
)

object BackupCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val cardTypes = setOf("mc", "tf", "card")

    fun encode(backup: BackupFileDto): String = json.encodeToString(BackupFileDto.serializer(), backup)

    /** Whether [text] claims to be a backup, so the reader can tell it apart from a single deck. */
    fun isBackup(text: String): Boolean = formatOf(parse(text)) == BACKUP_FORMAT

    fun decode(text: String): ParsedFile {
        val root = parse(text) ?: return ParsedFile.Failure(NOT_A_DECK)
        if (formatOf(root) != BACKUP_FORMAT) return ParsedFile.Failure(NOT_A_DECK)
        val version = runCatching { root["version"]?.jsonPrimitive?.intOrNull }.getOrNull() ?: 1
        if (version > 1) return ParsedFile.Failure(NEWER_VERSION)

        val backup = try {
            json.decodeFromJsonElement(BackupFileDto.serializer(), root)
        } catch (e: IllegalArgumentException) {
            return ParsedFile.Failure(NOT_A_DECK)
        }
        // Restoring wipes the library first, so a file with cards we can't store is refused whole.
        val cards = backup.decks.flatMap { it.cards }
        if (cards.any { it.type !in cardTypes }) return ParsedFile.Failure(NOT_A_DECK)
        return ParsedFile.Backup(backup)
    }

    fun settingsDto(s: AppSettings) = BackupSettingsDto(
        palette = s.palette.key,
        mode = s.mode.key,
        typePairing = s.typePairing.key,
        newPerDay = s.newPerDay,
        dailyGoal = s.dailyGoal,
        haptics = s.haptics,
        reduceMotion = s.reduceMotion,
        reminderEnabled = s.reminderEnabled,
        reminderHour = s.reminderHour,
        reminderMinute = s.reminderMinute,
    )

    fun BackupSettingsDto.toSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            palette = enumFromKey(palette, defaults.palette),
            mode = enumFromKey(mode, defaults.mode),
            typePairing = enumFromKey(typePairing, defaults.typePairing),
            newPerDay = newPerDay,
            dailyGoal = dailyGoal,
            haptics = haptics,
            reduceMotion = reduceMotion,
            reminderEnabled = reminderEnabled,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute,
        ).sanitized()
    }

    private fun parse(text: String): JsonObject? = try {
        json.parseToJsonElement(text) as? JsonObject
    } catch (e: SerializationException) {
        null
    }

    private fun formatOf(root: JsonObject?): String? =
        runCatching { root?.get("format")?.jsonPrimitive?.contentOrNull }.getOrNull()
}
