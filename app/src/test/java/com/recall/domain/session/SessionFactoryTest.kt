package com.recall.domain.session

import com.recall.data.db.Card
import com.recall.data.db.CardType
import com.recall.data.db.Grade
import com.recall.data.db.Phase
import com.recall.data.db.ReviewLog
import com.recall.data.db.ReviewState
import com.recall.data.db.StudyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import kotlin.random.Random

class SessionFactoryTest {
    private val now = Instant.parse("2026-10-23T09:00:00Z")
    private val zone = ZoneOffset.UTC

    private fun card(id: Long, type: CardType = CardType.MC, deckId: Long = 1, position: Int = id.toInt()) = Card(
        id = id, deckId = deckId, type = type, prompt = "Q$id",
        answer = if (type == CardType.TF) "false" else "a$id",
        wrongAnswers = if (type == CardType.MC) listOf("w1", "w2", "w3") else emptyList(),
        position = position, createdAt = 0, updatedAt = 0,
    )

    private fun log(cardId: Long, correct: Boolean, at: Long) = ReviewLog(
        cardId = cardId, deckId = 1, reviewedAt = at, grade = if (correct) Grade.GOOD else Grade.AGAIN,
        correct = correct, mode = StudyMode.QUIZ, firstTry = true,
    )

    private fun state(cardId: Long, phase: Phase, dueAt: Long = 0) =
        ReviewState(cardId, phase, dueAt, intervalDays = 1, ease = 2.5, reps = 1, lapses = 0)

    private val deck = (1L..12L).map { card(it) }

    private fun ids(items: List<SessionItem>) = items.map { it.card.id }

    @Test
    fun roundIsCutToItsSize() {
        val items = SessionFactory.practice(deck, emptyList(), StudyMode.QUIZ, 5, true, false, Random(1))
        assertEquals(5, items.size)
        assertEquals(5, ids(items).toSet().size)
    }

    @Test
    fun noSizeTakesTheWholeDeck() {
        val items = SessionFactory.practice(deck, emptyList(), StudyMode.QUIZ, null, true, false, Random(1))
        assertEquals(deck.map { it.id }.toSet(), ids(items).toSet())
    }

    @Test
    fun missedCardsComeFirstMostRecentFirst() {
        val logs = listOf(log(3, false, 100), log(7, false, 300), log(9, true, 400), log(11, false, 200))
        val items = SessionFactory.practice(deck, logs, StudyMode.QUIZ, 5, true, true, Random(1))
        assertEquals(listOf(7L, 11L, 3L), ids(items).take(3))
        assertTrue(ids(items).drop(3).none { it in setOf(3L, 7L, 11L) })
    }

    @Test
    fun dueRoundsOrderLearningThenReviewThenNew() {
        val today = now.toEpochMilli()
        val cards = listOf(
            card(1) to state(1, Phase.NEW),
            card(2) to state(2, Phase.REVIEW, today - 5_000),
            card(3) to state(3, Phase.LEARNING, today + 60_000),
            card(4) to state(4, Phase.REVIEW, today - 9_000),
            card(5) to state(5, Phase.LEARNING, today - 1_000),
            card(6) to state(6, Phase.REVIEW, today + 3 * 86_400_000L),
            card(7, position = 0) to state(7, Phase.NEW),
        )
        val items = SessionFactory.due(cards, mapOf(1L to 20), now, zone, true, Random(1))
        assertEquals(listOf(5L, 3L, 4L, 2L, 7L, 1L), ids(items))
    }

    @Test
    fun dueRoundsTakeOnlyTheAllowedNewCardsPerDeck() {
        val cards = listOf(
            card(1, deckId = 1, position = 0) to state(1, Phase.NEW),
            card(2, deckId = 1, position = 1) to state(2, Phase.NEW),
            card(3, deckId = 2, position = 0) to state(3, Phase.NEW),
            card(4, deckId = 2, position = 1) to state(4, Phase.NEW),
        )
        val items = SessionFactory.due(cards, mapOf(1L to 1, 2L to 2), now, zone, true, Random(1))
        assertEquals(listOf(1L, 3L, 4L), ids(items))
    }

    @Test
    fun withoutShuffleTheAnswerComesFirst() {
        val item = SessionFactory.item(card(1), StudyMode.QUIZ, shuffle = false, random = Random(1))
        assertEquals(listOf("a1", "w1", "w2", "w3"), item.options)
        assertEquals(setOf(0), item.correct)
    }

    @Test
    fun shuffledOptionsStillMarkTheRightOnes() {
        val multi = card(1).copy(alsoCorrect = listOf("b1"))
        val item = SessionFactory.item(multi, StudyMode.QUIZ, shuffle = true, random = Random(4))
        assertEquals(setOf("a1", "b1"), item.correct.map { item.options[it] }.toSet())
    }

    @Test
    fun trueFalseOptionsNeverMove() {
        repeat(5) { seed ->
            val item = SessionFactory.item(card(1, CardType.TF), StudyMode.QUIZ, shuffle = true, random = Random(seed))
            assertEquals(listOf(TRUE_OPTION, FALSE_OPTION), item.options)
            assertEquals(setOf(1), item.correct)
        }
    }

    @Test
    fun flashcardRoundsShowEveryCardAsAFlashcard() {
        val cards = listOf(card(1), card(2, CardType.TF), card(3, CardType.FLASH))
        val items = SessionFactory.practice(cards, emptyList(), StudyMode.FLASHCARDS, null, true, false, Random(1))
        assertTrue(items.all { it.presentation == Presentation.FLASH })

        val quiz = SessionFactory.practice(cards, emptyList(), StudyMode.QUIZ, null, true, false, Random(1))
        assertEquals(mapOf(1L to Presentation.QUIZ, 2L to Presentation.QUIZ, 3L to Presentation.FLASH), quiz.associate { it.card.id to it.presentation })
    }

    @Test
    fun retryKeepsExactlyTheMissedCardsInOrder() {
        val items = SessionFactory.retry(listOf(card(4), card(2)), shuffle = false, random = Random(1))
        assertEquals(listOf(4L, 2L), ids(items))
    }
}
