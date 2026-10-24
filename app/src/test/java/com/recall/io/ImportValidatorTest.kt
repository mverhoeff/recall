package com.recall.io

import com.recall.data.db.CardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportValidatorTest {
    private fun csv(text: String) = ImportValidator.fromCsv(CsvParser.parse(text))

    private fun deck(text: String) = csv(text) as ParsedFile.Deck

    private fun messages(text: String) = deck(text).issues.map { it.message }

    @Test
    fun readsEachCardType() {
        val parsed = deck(
            """
            type,prompt,answer,wrong1,wrong2,explanation
            mc,Yo ___ hambre,tengo,teno,tiene,From tener
            tf,Madrid is in Spain,yes,,,
            card,poder · yo,pude,,,
            """.trimIndent(),
        )
        assertEquals(
            listOf(
                ImportedCard(CardType.MC, "Yo ___ hambre", "tengo", listOf("teno", "tiene"), "From tener"),
                ImportedCard(CardType.TF, "Madrid is in Spain", "true", emptyList(), ""),
                ImportedCard(CardType.FLASH, "poder · yo", "pude", emptyList(), ""),
            ),
            parsed.cards,
        )
        assertEquals(ImportCounts(choice = 1, trueFalse = 1, flashcards = 1), parsed.counts)
        assertTrue(parsed.issues.isEmpty())
        assertEquals(FileKind.CSV, parsed.kind)
    }

    @Test
    fun headerMayBeInAnyOrderAndCase() {
        val parsed = deck("Answer; EXPLANATION ;Prompt;Type;notes\n  hello ;;hola; CARD ;ignored\n")
        assertEquals(listOf(ImportedCard(CardType.FLASH, "hola", "hello", emptyList(), "")), parsed.cards)
    }

    @Test
    fun missingColumnsFailTheWholeFile() {
        assertEquals(ParsedFile.Failure(ImportValidator.MISSING_HEADER), csv("type,prompt\nmc,a\n"))
        assertEquals(ParsedFile.Failure(ImportValidator.MISSING_HEADER), csv(""))
        assertEquals(ParsedFile.Failure(ImportValidator.MISSING_HEADER), csv("card,hola,hello\n"))
    }

    @Test
    fun everyRowErrorHasItsMessage() {
        val text = """
            type,prompt,answer,wrong1
            quiz,What,x,
            card,  ,x,
            mc,Pick one,yes,
            tf,Is it,maybe,
            card,Front,,
            """.trimIndent()
        assertEquals(
            listOf(
                "Row 2: type \"quiz\" isn't known. Use mc, tf or card.",
                "Row 3: the prompt is empty.",
                "Row 4: a choice card needs at least 2 answers.",
                "Row 5: true/false cards need true or false as the answer.",
                "Row 6: the back of the card is empty.",
            ),
            messages(text),
        )
        assertTrue(deck(text).cards.isEmpty())
    }

    @Test
    fun rowNumbersCountAMultiLineFieldOnce() {
        val text = "type,prompt,answer\ncard,\"two\nlines\",back\ncard,,back\n"
        assertEquals(listOf("Row 3: the prompt is empty."), messages(text))
        assertEquals("two\nlines", deck(text).cards.single().prompt)
    }

    @Test
    fun blankRowsAreSkippedButStillCounted() {
        val text = "type,prompt,answer\n\n , , \ncard,a,b\ntf,b,nope\n"
        assertEquals(listOf("Row 5: true/false cards need true or false as the answer."), messages(text))
        assertEquals(1, deck(text).cards.size)
    }

    @Test
    fun duplicateChoicesAreDropped() {
        val parsed = deck("type,prompt,answer,wrong1,wrong2,wrong3\nmc,Capital,Paris,paris,Lyon,LYON\n")
        assertEquals(listOf("Lyon"), parsed.cards.single().wrong)
    }

    @Test
    fun choicesThatAreAllDuplicatesStillNeedTwoAnswers() {
        assertEquals(
            listOf("Row 2: a choice card needs at least 2 answers."),
            messages("type,prompt,answer,wrong1,wrong2\nmc,Capital,Paris,PARIS, paris \n"),
        )
    }

    @Test
    fun trueFalseAnswersAreNormalised() {
        val answers = deck("type,prompt,answer\ntf,a,T\ntf,b,No\ntf,c,1\ntf,d,FALSE\n").cards.map { it.answer }
        assertEquals(listOf("true", "false", "true", "false"), answers)
    }

    @Test
    fun jsonCardsAreNumberedByIndex() {
        val parsed = ImportValidator.fromJson(
            " Verbs ",
            listOf(RawCard("card", "a", "b"), RawCard("mc", "c", "d"), RawCard("TF", "e", "true")),
        )
        assertEquals("Verbs", parsed.name)
        assertEquals(listOf("Card 2: a choice card needs at least 2 answers."), parsed.issues.map { it.message })
        assertEquals(2, parsed.cards.size)
        assertEquals(FileKind.JSON, parsed.kind)
    }
}
