package com.recall.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaoTest {
    private lateinit var db: RecallDatabase
    private lateinit var decks: DeckDao
    private lateinit var cards: CardDao
    private lateinit var reviews: ReviewDao

    @Before
    fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), RecallDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        decks = db.deckDao()
        cards = db.cardDao()
        reviews = db.reviewDao()
    }

    @After
    fun close() = db.close()

    private suspend fun deckWithCards(count: Int): Pair<Long, List<Long>> {
        val deckId = decks.insert(Deck(name = "Capitals", createdAt = 1, updatedAt = 1))
        val ids = (0 until count).map { i ->
            cards.insertWithState(
                Card(
                    deckId = deckId, type = CardType.FLASH, prompt = "Q$i", answer = "A$i",
                    position = i, createdAt = 1, updatedAt = 1,
                ),
            )
        }
        return deckId to ids
    }

    private fun log(cardId: Long, deckId: Long, at: Long, correct: Boolean = true) = ReviewLog(
        cardId = cardId, deckId = deckId, reviewedAt = at,
        grade = if (correct) Grade.GOOD else Grade.AGAIN, correct = correct, mode = StudyMode.QUIZ, firstTry = true,
    )

    @Test
    fun insertWithStateCreatesNewStates() = runBlocking {
        val (deckId, ids) = deckWithCards(3)

        val states = reviews.statesByDeck(deckId)
        assertEquals(ids.toSet(), states.map { it.cardId }.toSet())
        assertTrue(states.all { it == ReviewState.new(it.cardId) })
        assertEquals(3, cards.countByDeck(deckId).first())
        assertEquals(2, cards.maxPosition(deckId))
    }

    @Test
    fun deletingDeckCascades() = runBlocking {
        val (deckId, ids) = deckWithCards(3)
        reviews.insertLog(log(ids[0], deckId, at = 10))

        decks.delete(deckId)

        assertNull(decks.get(deckId))
        assertTrue(cards.byDeck(deckId).isEmpty())
        assertTrue(ids.all { reviews.state(it) == null })
        assertTrue(reviews.logsSince(0).isEmpty())
    }

    @Test
    fun latestLogPerCardReturnsNewest() = runBlocking {
        val (deckId, ids) = deckWithCards(2)
        reviews.insertLog(log(ids[0], deckId, at = 10, correct = true))
        reviews.insertLog(log(ids[0], deckId, at = 30, correct = false))
        reviews.insertLog(log(ids[0], deckId, at = 20, correct = true))
        reviews.insertLog(log(ids[1], deckId, at = 5, correct = true))

        val latest = reviews.latestLogPerCard(deckId).associateBy { it.cardId }

        assertEquals(2, latest.size)
        assertEquals(30L, latest.getValue(ids[0]).reviewedAt)
        assertEquals(false, latest.getValue(ids[0]).correct)
        assertEquals(5L, latest.getValue(ids[1]).reviewedAt)
        assertEquals(
            mapOf(ids[0] to 10L, ids[1] to 5L),
            reviews.firstLogTimes(deckId).associate { it.cardId to it.firstAt },
        )
    }

    @Test
    fun wrongAnswersRoundTrip() = runBlocking {
        val deckId = decks.insert(Deck(name = "Quiz", createdAt = 1, updatedAt = 1))
        val wrong = listOf("a", "b, c", "\"q\"")
        val id = cards.insertWithState(
            Card(
                deckId = deckId, type = CardType.MC, prompt = "Pick", answer = "d", wrongAnswers = wrong,
                position = 0, createdAt = 1, updatedAt = 1,
            ),
        )

        assertEquals(wrong, cards.byIds(listOf(id)).single().wrongAnswers)
        assertEquals(wrong, Converters().toList(Converters().fromList(wrong)))
    }
}
