package com.recall.io

import com.recall.data.db.Card
import com.recall.data.db.CardType
import com.recall.data.db.Deck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckJsonCodecTest {
    private val deck = Deck(id = 7, name = "Spanish verbs — A2", description = "Present tense", createdAt = 1, updatedAt = 2)

    private fun card(position: Int, type: CardType, prompt: String, answer: String, wrong: List<String> = emptyList(), explanation: String = "") =
        Card(
            id = position + 1L, deckId = 7, type = type, prompt = prompt, answer = answer, wrongAnswers = wrong,
            explanation = explanation, position = position, createdAt = 1, updatedAt = 1,
        )

    private val cards = listOf(
        card(1, CardType.TF, "Tengo is first person", "true"),
        card(0, CardType.MC, "Yo ___ hambre", "tengo", listOf("teno", "tiene"), "From tener"),
        card(2, CardType.FLASH, "poder · preterite · yo", "pude"),
    )

    @Test
    fun encodedDeckDecodesBackToTheSameCards() {
        val text = DeckJsonCodec.encode(deck, cards)
        val parsed = DeckJsonCodec.decode(text) as ParsedFile.Deck

        assertEquals("Spanish verbs — A2", parsed.name)
        assertEquals(
            listOf(
                ImportedCard(CardType.MC, "Yo ___ hambre", "tengo", listOf("teno", "tiene"), "From tener"),
                ImportedCard(CardType.TF, "Tengo is first person", "true", emptyList(), ""),
                ImportedCard(CardType.FLASH, "poder · preterite · yo", "pude", emptyList(), ""),
            ),
            parsed.cards,
        )
        assertTrue(parsed.issues.isEmpty())
    }

    @Test
    fun severalCorrectAnswersSurviveARoundTrip() {
        val multi = card(0, CardType.MC, "Which builders start a coroutine?", "launch", listOf("delay"))
            .copy(alsoCorrect = listOf("async"))
        val text = DeckJsonCodec.encode(deck, listOf(multi))
        assertTrue(text.contains("\"alsoCorrect\""))

        val parsed = DeckJsonCodec.decode(text) as ParsedFile.Deck
        assertEquals(
            listOf(ImportedCard(CardType.MC, "Which builders start a coroutine?", "launch", listOf("delay"), "", listOf("async"))),
            parsed.cards,
        )
    }

    @Test
    fun encodingWritesTheFormatHeaderAndSkipsEmptyFields() {
        val text = DeckJsonCodec.encode(deck, cards)
        assertTrue(text.contains("\"format\": \"recall-deck\""))
        assertTrue(text.contains("\"version\": 1"))
        assertTrue(text.contains("\"type\": \"card\""))
        assertFalse(text.contains("\"wrong\": []"))
        assertFalse(text.contains("alsoCorrect"))
        assertTrue(text.lines().size > 10)
    }

    @Test
    fun unknownKeysAreIgnored() {
        val text = """
            {"format": "recall-deck", "version": 1, "author": "me",
             "deck": {"name": "Capitals", "colour": "blue"},
             "cards": [{"type": "card", "prompt": "France", "answer": "Paris", "difficulty": 3}]}
        """.trimIndent()
        val parsed = DeckJsonCodec.decode(text) as ParsedFile.Deck
        assertEquals("Capitals", parsed.name)
        assertEquals("Paris", parsed.cards.single().answer)
    }

    @Test
    fun invalidCardsAreReportedByIndex() {
        val text = """{"format": "recall-deck", "version": 1, "cards": [
            {"type": "card", "prompt": "a", "answer": "b"},
            {"type": "essay", "prompt": "c", "answer": "d"}]}"""
        val parsed = DeckJsonCodec.decode(text) as ParsedFile.Deck
        assertEquals(listOf("Card 2: type \"essay\" isn't known. Use mc, tf or card."), parsed.issues.map { it.message })
        assertEquals(null, parsed.name)
    }

    @Test
    fun newerVersionsAreRefused() {
        assertEquals(ParsedFile.Failure(NEWER_VERSION), DeckJsonCodec.decode("""{"format": "recall-deck", "version": 2}"""))
    }

    @Test
    fun otherJsonIsNotADeck() {
        assertEquals(ParsedFile.Failure(NOT_A_DECK), DeckJsonCodec.decode("""{"name": "x"}"""))
        assertEquals(ParsedFile.Failure(NOT_A_DECK), DeckJsonCodec.decode("""{"format": "recall-deck", "cards": 5}"""))
        assertEquals(ParsedFile.Failure(NOT_A_DECK), DeckJsonCodec.decode("{ not json"))
    }

    @Test
    fun readerPicksTheKindFromTheContent() {
        val json = DeckFileReader.read(DeckJsonCodec.encode(deck, cards).toByteArray(), "export.json")
        assertEquals(FileKind.JSON, (json as ParsedFile.Deck).kind)
        val csv = DeckFileReader.read("﻿type,prompt,answer\ncard,a,b\n".toByteArray(), "Verbs.csv")
        assertEquals(FileKind.CSV, (csv as ParsedFile.Deck).kind)
        assertEquals("Verbs", csv.name)
    }

    @Test
    fun readerRefusesLargeFiles() {
        val big = ByteArray(DeckFileReader.MAX_BYTES + 1) { 'a'.code.toByte() }
        assertEquals(ParsedFile.Failure(DeckFileReader.TOO_LARGE), DeckFileReader.read(big, "big.csv"))
    }

    @Test
    fun readerRefusesTextThatIsNotUtf8() {
        val latin1 = "type,prompt,answer\ncard,café,x\n".toByteArray(Charsets.ISO_8859_1)
        assertEquals(ParsedFile.Failure(DeckFileReader.NOT_UTF8), DeckFileReader.read(latin1, "old.csv"))
    }
}
