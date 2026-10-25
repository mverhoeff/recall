package com.recall.io

import com.recall.data.db.CardType

/** A card as written in a file, before any checks. */
data class RawCard(
    val type: String,
    val prompt: String,
    val answer: String,
    val wrong: List<String> = emptyList(),
    val explanation: String = "",
    /** Correct options beyond [answer]; only read for choice cards. */
    val alsoCorrect: List<String> = emptyList(),
)

data class ImportedCard(
    val type: CardType,
    val prompt: String,
    val answer: String,
    val wrong: List<String>,
    val explanation: String,
    val alsoCorrect: List<String> = emptyList(),
)

data class ImportIssue(val message: String)

enum class FileKind { CSV, JSON }

data class ImportCounts(val choice: Int, val trueFalse: Int, val flashcards: Int)

sealed interface ParsedFile {
    data class Deck(
        val name: String?,
        val cards: List<ImportedCard>,
        val issues: List<ImportIssue>,
        val kind: FileKind,
    ) : ParsedFile {
        val counts: ImportCounts
            get() = ImportCounts(
                choice = cards.count { it.type == CardType.MC },
                trueFalse = cards.count { it.type == CardType.TF },
                flashcards = cards.count { it.type == CardType.FLASH },
            )
    }

    data class Backup(val backup: BackupFileDto) : ParsedFile {
        val deckCount: Int get() = backup.decks.size
        val cardCount: Int get() = backup.decks.sumOf { it.cards.size }
    }

    data class Failure(val message: String) : ParsedFile
}
