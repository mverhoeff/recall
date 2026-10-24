package com.recall.io

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvParserTest {
    private fun fields(text: String) = CsvParser.parse(text).map { it.fields }

    @Test
    fun plainRowsSplitOnCommas() {
        assertEquals(
            listOf(listOf("type", "prompt", "answer"), listOf("card", "hola", "hello")),
            fields("type,prompt,answer\ncard,hola,hello\n"),
        )
    }

    @Test
    fun quotedFieldsKeepCommas() {
        assertEquals(listOf(listOf("a", "b, c", "d")), fields("a,\"b, c\",d"))
    }

    @Test
    fun quotedFieldsKeepNewlines() {
        assertEquals(listOf(listOf("one\ntwo", "x"), listOf("y", "z")), fields("\"one\ntwo\",x\ny,z\n"))
    }

    @Test
    fun doubledQuotesAreLiteral() {
        assertEquals(listOf(listOf("say \"hi\"", "\"")), fields("\"say \"\"hi\"\"\",\"\"\"\""))
    }

    @Test
    fun semicolonsAreDetected() {
        assertEquals(';', CsvParser.detectDelimiter("type;prompt;answer\n"))
        assertEquals(listOf(listOf("type", "prompt", "answer"), listOf("tf", "1,5 > 1", "ja")),
            fields("type;prompt;answer\ntf;1,5 > 1;ja\n"))
    }

    @Test
    fun tabsAreDetected() {
        assertEquals(listOf(listOf("type", "prompt", "answer"), listOf("card", "a, b", "c")),
            fields("type\tprompt\tanswer\ncard\ta, b\tc"))
    }

    @Test
    fun byteOrderMarkIsStripped() {
        assertEquals(listOf("type", "prompt"), fields("﻿type,prompt\n").single())
    }

    @Test
    fun windowsLineEndingsAreAccepted() {
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), fields("a,b\r\nc,d\r\n"))
    }

    @Test
    fun recordsAreNumberedNotLines() {
        val rows = CsvParser.parse("h1,h2\n\"multi\nline\",x\nlast,y\n")
        assertEquals(listOf(1, 2, 3), rows.map { it.number })
        assertEquals(listOf("last", "y"), rows[2].fields)
    }

    @Test
    fun blankLinesAreKeptAsRows() {
        val rows = CsvParser.parse("h\n\nx\n")
        assertEquals(listOf(listOf("h"), listOf(""), listOf("x")), rows.map { it.fields })
    }

    @Test
    fun emptyTrailingFieldIsKept() {
        assertEquals(listOf(listOf("a", "", "")), fields("a,,\n"))
    }

    @Test
    fun spaceBeforeQuoteStillOpensQuotedField() {
        assertEquals(listOf(listOf("a", "b, c")), fields("a, \"b, c\""))
    }
}
