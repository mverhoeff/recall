package com.recall.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class CardType { MC, TF, FLASH }

enum class Phase { NEW, LEARNING, REVIEW }

enum class Grade { AGAIN, HARD, GOOD, EASY }

enum class StudyMode { QUIZ, FLASHCARDS, DUE }

@Entity(tableName = "decks")
data class Deck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * [answer] is the correct option for MC, "true" or "false" for TF, and the back side for FLASH.
 * [wrongAnswers] is only filled for MC.
 */
@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(entity = Deck::class, parentColumns = ["id"], childColumns = ["deckId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("deckId")],
)
data class Card(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val type: CardType,
    val prompt: String,
    val answer: String,
    val wrongAnswers: List<String> = emptyList(),
    val explanation: String = "",
    val position: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "review_state",
    foreignKeys = [
        ForeignKey(entity = Card::class, parentColumns = ["id"], childColumns = ["cardId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class ReviewState(
    @PrimaryKey val cardId: Long,
    val phase: Phase,
    val dueAt: Long,
    val intervalDays: Int,
    val ease: Double,
    val reps: Int,
    val lapses: Int,
) {
    companion object {
        fun new(cardId: Long) = ReviewState(cardId, Phase.NEW, dueAt = 0, intervalDays = 0, ease = 2.5, reps = 0, lapses = 0)
    }
}

@Entity(
    tableName = "review_logs",
    foreignKeys = [
        ForeignKey(entity = Card::class, parentColumns = ["id"], childColumns = ["cardId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("cardId"), Index("deckId"), Index("reviewedAt")],
)
data class ReviewLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val deckId: Long,
    val reviewedAt: Long,
    val grade: Grade,
    val correct: Boolean,
    val mode: StudyMode,
    val firstTry: Boolean,
)

/** The time of a card's first ever review. */
data class CardFirstLog(val cardId: Long, val firstAt: Long)
