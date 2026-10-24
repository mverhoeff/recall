package com.recall.io

import com.recall.data.db.CardType

object ImportValidator {
    const val MISSING_HEADER = "This file needs a header row with type, prompt and answer."

    private val trueWords = setOf("true", "t", "yes", "1")
    private val falseWords = setOf("false", "f", "no", "0")

    fun fromCsv(rows: List<CsvRow>): ParsedFile {
        val headerIndex = rows.indexOfFirst { !it.isBlank() }
        if (headerIndex < 0) return ParsedFile.Failure(MISSING_HEADER)
        val columns = rows[headerIndex].fields.map { it.trim().lowercase() }
        fun column(name: String) = columns.indexOf(name)
        val type = column("type")
        val prompt = column("prompt")
        val answer = column("answer")
        if (type < 0 || prompt < 0 || answer < 0) return ParsedFile.Failure(MISSING_HEADER)
        val wrong = (1..5).map { column("wrong$it") }
        val explanation = column("explanation")

        val cards = mutableListOf<ImportedCard>()
        val issues = mutableListOf<ImportIssue>()
        for (row in rows.drop(headerIndex + 1)) {
            if (row.isBlank()) continue
            fun at(index: Int) = row.fields.getOrNull(index).orEmpty()
            val raw = RawCard(at(type), at(prompt), at(answer), wrong.map(::at), at(explanation))
            check(raw, "Row ${row.number}").fold(cards::add, issues::add)
        }
        return ParsedFile.Deck(name = null, cards, issues, FileKind.CSV)
    }

    fun fromJson(name: String?, raw: List<RawCard>): ParsedFile.Deck {
        val cards = mutableListOf<ImportedCard>()
        val issues = mutableListOf<ImportIssue>()
        raw.forEachIndexed { index, card -> check(card, "Card ${index + 1}").fold(cards::add, issues::add) }
        return ParsedFile.Deck(name?.trim()?.ifEmpty { null }, cards, issues, FileKind.JSON)
    }

    private fun check(raw: RawCard, where: String): Checked {
        val typeName = raw.type.trim()
        val prompt = raw.prompt.trim()
        val answer = raw.answer.trim()
        val explanation = raw.explanation.trim()
        val type = when (typeName.lowercase()) {
            "mc" -> CardType.MC
            "tf" -> CardType.TF
            "card" -> CardType.FLASH
            else -> return Checked.Bad("$where: type \"$typeName\" isn't known. Use mc, tf or card.")
        }
        if (prompt.isEmpty()) return Checked.Bad("$where: the prompt is empty.")
        return when (type) {
            CardType.MC -> {
                val options = (listOf(answer) + raw.wrong.map { it.trim() })
                    .filter { it.isNotEmpty() }
                    .distinctBy { it.lowercase() }
                if (answer.isEmpty() || options.size < 2) {
                    Checked.Bad("$where: a choice card needs at least 2 answers.")
                } else {
                    Checked.Good(ImportedCard(type, prompt, answer, options.drop(1), explanation))
                }
            }
            CardType.TF -> when (answer.lowercase()) {
                in trueWords -> Checked.Good(ImportedCard(type, prompt, "true", emptyList(), explanation))
                in falseWords -> Checked.Good(ImportedCard(type, prompt, "false", emptyList(), explanation))
                else -> Checked.Bad("$where: true/false cards need true or false as the answer.")
            }
            CardType.FLASH ->
                if (answer.isEmpty()) {
                    Checked.Bad("$where: the back of the card is empty.")
                } else {
                    Checked.Good(ImportedCard(type, prompt, answer, emptyList(), explanation))
                }
        }
    }

    private sealed interface Checked {
        data class Good(val card: ImportedCard) : Checked
        data class Bad(val message: String) : Checked

        fun fold(onGood: (ImportedCard) -> Unit, onBad: (ImportIssue) -> Unit) = when (this) {
            is Good -> onGood(card)
            is Bad -> onBad(ImportIssue(message))
        }
    }

    private fun CsvRow.isBlank() = fields.all { it.isBlank() }
}
