package com.recall.io

/** One CSV record. [number] counts records from 1 (the header), so a quoted multi-line field is one row. */
data class CsvRow(val number: Int, val fields: List<String>)

object CsvParser {
    private val delimiters = listOf(',', ';', '\t')

    private enum class State { Outside, InQuotes, AfterQuote }

    fun parse(text: String): List<CsvRow> {
        val body = text.removePrefix("﻿")
        val delimiter = detectDelimiter(body)
        val rows = mutableListOf<CsvRow>()
        val fields = mutableListOf<String>()
        val field = StringBuilder()
        var state = State.Outside
        var rowHasContent = false

        fun endField() {
            fields += field.toString()
            field.clear()
        }

        fun endRow() {
            endField()
            rows += CsvRow(rows.size + 1, fields.toList())
            fields.clear()
            rowHasContent = false
        }

        var i = 0
        while (i < body.length) {
            val ch = body[i]
            if (ch != '\r') rowHasContent = true
            when (state) {
                State.Outside -> when {
                    // Whitespace before an opening quote is dropped, so `a, "b"` still reads as quoted.
                    ch == '"' && field.isBlank() -> {
                        field.clear()
                        state = State.InQuotes
                    }
                    ch == delimiter -> endField()
                    ch == '\n' -> endRow()
                    ch == '\r' -> Unit
                    else -> field.append(ch)
                }
                State.InQuotes -> when {
                    ch == '"' && body.getOrNull(i + 1) == '"' -> {
                        field.append('"')
                        i++
                    }
                    ch == '"' -> state = State.AfterQuote
                    else -> field.append(ch)
                }
                State.AfterQuote -> when (ch) {
                    delimiter -> {
                        endField()
                        state = State.Outside
                    }
                    '\n' -> {
                        endRow()
                        state = State.Outside
                    }
                    '\r' -> Unit
                    else -> field.append(ch)
                }
            }
            i++
        }
        if (rowHasContent) endRow()
        return rows
    }

    /** Picks whichever delimiter the header line uses most; spreadsheets set to German export with `;`. */
    fun detectDelimiter(text: String): Char {
        val header = text.removePrefix("﻿").lineSequence().firstOrNull { it.isNotBlank() } ?: return ','
        return delimiters.maxBy { d -> header.count { it == d } }
    }
}
