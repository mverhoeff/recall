package com.recall.data.repo

import androidx.room.withTransaction
import com.recall.data.db.Card
import com.recall.data.db.CardType
import com.recall.data.db.Deck
import com.recall.data.db.RecallDatabase
import com.recall.data.db.ReviewLog
import com.recall.data.db.ReviewState
import com.recall.data.settings.SettingsRepository
import com.recall.io.BackupCardDto
import com.recall.io.BackupCodec
import com.recall.io.BackupCodec.toSettings
import com.recall.io.BackupDeckDto
import com.recall.io.BackupFileDto
import com.recall.io.DeckJsonCodec.key
import com.recall.io.LogDto
import com.recall.io.ReviewDto
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.temporal.ChronoUnit

class BackupRepository(
    private val database: RecallDatabase,
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    private val decks = database.deckDao()
    private val cards = database.cardDao()
    private val reviews = database.reviewDao()

    suspend fun export(): String {
        // One read transaction, so an answer saved mid-export can't leave a log without its card.
        val deckDtos = database.withTransaction {
            val logsByCard = reviews.logsSince(0).groupBy { it.cardId }
            decks.observeAll().first().sortedBy { it.createdAt }.map { deck ->
                val states = reviews.statesByDeck(deck.id).associateBy { it.cardId }
                val cardDtos = cards.byDeck(deck.id).map { card ->
                    card.toDto(states[card.id] ?: ReviewState.new(card.id), logsByCard[card.id].orEmpty())
                }
                BackupDeckDto(deck.name, deck.description, deck.createdAt, deck.updatedAt, cardDtos)
            }
        }
        val backup = BackupFileDto(
            exportedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS).toString(),
            settings = BackupCodec.settingsDto(settings.settings.first()),
            decks = deckDtos,
        )
        return BackupCodec.encode(backup)
    }

    /** Replaces every deck, card, review and setting with the contents of [backup]. Ids are reassigned. */
    suspend fun restore(backup: BackupFileDto) {
        database.withTransaction {
            decks.deleteAll()
            for (deck in backup.decks) {
                val deckId = decks.insert(
                    Deck(name = deck.name, description = deck.description, createdAt = deck.createdAt, updatedAt = deck.updatedAt),
                )
                for (card in deck.cards) {
                    val cardId = cards.insertWithState(card.toCard(deckId))
                    reviews.upsertState(card.review.toState(cardId))
                    for (log in card.logs) reviews.insertLog(log.toLog(cardId, deckId))
                }
            }
        }
        settings.replaceAll(backup.settings.toSettings())
    }

    private fun Card.toDto(state: ReviewState, logs: List<ReviewLog>) = BackupCardDto(
        type = type.key(),
        prompt = prompt,
        answer = answer,
        wrong = wrongAnswers,
        alsoCorrect = alsoCorrect,
        explanation = explanation,
        position = position,
        createdAt = createdAt,
        updatedAt = updatedAt,
        review = ReviewDto(state.phase, state.dueAt, state.intervalDays, state.ease, state.reps, state.lapses),
        logs = logs.map { LogDto(it.reviewedAt, it.grade, it.correct, it.mode, it.firstTry) },
    )

    private fun BackupCardDto.toCard(deckId: Long) = Card(
        deckId = deckId,
        type = when (type) {
            "mc" -> CardType.MC
            "tf" -> CardType.TF
            else -> CardType.FLASH
        },
        prompt = prompt,
        answer = answer,
        wrongAnswers = wrong,
        alsoCorrect = alsoCorrect,
        explanation = explanation,
        position = position,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun ReviewDto.toState(cardId: Long) = ReviewState(cardId, phase, dueAt, intervalDays, ease, reps, lapses)

    private fun LogDto.toLog(cardId: Long, deckId: Long) = ReviewLog(
        cardId = cardId,
        deckId = deckId,
        reviewedAt = reviewedAt,
        grade = grade,
        correct = correct,
        mode = mode,
        firstTry = firstTry,
    )
}
