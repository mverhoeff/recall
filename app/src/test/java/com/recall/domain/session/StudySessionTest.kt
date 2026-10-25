package com.recall.domain.session

import com.recall.data.db.Card
import com.recall.data.db.CardType
import com.recall.data.db.Grade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class StudySessionTest {
    private class StepClock(var now: Instant = Instant.parse("2026-10-23T09:00:00Z")) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = now
        fun advance(seconds: Long) {
            now += Duration.ofSeconds(seconds)
        }
    }

    private fun card(id: Long, type: CardType, answer: String, alsoCorrect: List<String> = emptyList()) = Card(
        id = id, deckId = 1, type = type, prompt = "Q$id", answer = answer, alsoCorrect = alsoCorrect,
        position = id.toInt(), createdAt = 0, updatedAt = 0,
    )

    private val tengo = SessionItem(card(1, CardType.MC, "tengo"), Presentation.QUIZ, listOf("tiene", "tengo", "tienes"))
    private val madrid = SessionItem(card(2, CardType.TF, "true"), Presentation.QUIZ, listOf(TRUE_OPTION, FALSE_OPTION))
    private val builders = SessionItem(
        card(3, CardType.MC, "launch", alsoCorrect = listOf("async")),
        Presentation.QUIZ,
        listOf("delay", "async", "launch"),
    )
    private val pude = SessionItem(card(4, CardType.FLASH, "pude"), Presentation.FLASH, emptyList())

    @Test
    fun selectIsIgnoredAfterCheck() {
        val session = StudySession(listOf(tengo), StepClock())
        session.select(1)
        session.check()
        session.select(0)

        assertEquals(setOf(1), session.state.selected)
        assertTrue(session.state.lastCorrect!!)
    }

    @Test
    fun checkNeedsASelection() {
        val session = StudySession(listOf(tengo), StepClock())
        assertNull(session.check())
        assertFalse(session.state.checked)
    }

    @Test
    fun countsRightAndWrongAnswers() {
        val session = StudySession(listOf(tengo, madrid), StepClock())
        session.select(0)
        val wrong = session.check()!!
        session.next()
        session.select(0)
        val right = session.check()!!

        assertFalse(wrong.correct)
        assertEquals(Grade.AGAIN, wrong.grade)
        assertTrue(right.correct)
        assertEquals(Grade.GOOD, right.grade)
        assertEquals(1, session.result().correct)
        assertEquals(2, session.result().total)
        assertEquals(50, session.result().accuracy)
    }

    @Test
    fun nextIsIgnoredBeforeCheck() {
        val session = StudySession(listOf(tengo, madrid), StepClock())
        session.next()
        assertEquals(0, session.state.index)
    }

    @Test
    fun nextAfterTheLastItemFinishes() {
        val session = StudySession(listOf(tengo), StepClock())
        session.select(1)
        session.check()
        assertTrue(session.state.isLast)
        session.next()

        assertTrue(session.state.finished)
    }

    @Test
    fun severalCorrectAnswersMustAllBePicked() {
        val session = StudySession(listOf(builders, builders.copy(card = builders.card.copy(id = 5))), StepClock())
        assertTrue(builders.multiple)
        session.select(2)
        assertFalse(session.check()!!.correct)

        session.next()
        session.select(1)
        session.select(0)
        session.select(0)
        session.select(2)
        assertEquals(setOf(1, 2), session.state.selected)
        assertTrue(session.check()!!.correct)
    }

    @Test
    fun aSingleAnswerCardReplacesTheSelection() {
        val session = StudySession(listOf(tengo), StepClock())
        session.select(0)
        session.select(2)
        assertEquals(setOf(2), session.state.selected)
    }

    @Test
    fun flashcardsFlipThenGradeAndMoveOn() {
        val session = StudySession(listOf(pude, tengo), StepClock())
        assertNull(session.grade(Grade.GOOD))
        session.flip()
        val answer = session.grade(Grade.AGAIN)!!

        assertFalse(answer.correct)
        assertEquals(Grade.AGAIN, answer.grade)
        assertEquals(1, session.state.index)
        assertFalse(session.state.flipped)
    }

    @Test
    fun elapsedRunsFromTheStartToTheLastAnswer() {
        val clock = StepClock()
        val session = StudySession(listOf(tengo, madrid), clock)
        clock.advance(40)
        session.select(1)
        session.check()
        session.next()
        clock.advance(128)
        session.select(0)
        session.check()
        clock.advance(30)
        session.next()

        assertEquals(168_000L, session.result().elapsedMillis)
    }

    @Test
    fun onlyTheFirstAnswerToACardIsTheFirstTry() {
        val session = StudySession(listOf(tengo, tengo), StepClock())
        session.select(1)
        assertTrue(session.check()!!.firstTry)
        session.next()
        session.select(1)
        assertFalse(session.check()!!.firstTry)
    }

    @Test
    fun anEmptyRoundIsFinishedAtOnce() {
        assertTrue(StudySession(emptyList(), StepClock()).state.finished)
    }
}
