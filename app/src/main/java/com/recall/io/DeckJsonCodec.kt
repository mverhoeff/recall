package com.recall.io

import com.recall.data.db.Card
import com.recall.data.db.CardType
import com.recall.data.db.Deck
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

const val DECK_FORMAT = "recall-deck"
const val NEWER_VERSION = "This file was made by a newer version of Recall."
const val NOT_A_DECK = "This file isn't a Recall deck."

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DeckFileDto(
    @EncodeDefault val format: String = DECK_FORMAT,
    @EncodeDefault val version: Int = 1,
    val deck: DeckInfoDto = DeckInfoDto(),
    val cards: List<CardDto> = emptyList(),
)

@Serializable
data class DeckInfoDto(val name: String = "", val description: String = "")

@Serializable
data class CardDto(
    val type: String = "",
    val prompt: String = "",
    val answer: String = "",
    val wrong: List<String> = emptyList(),
    val explanation: String = "",
)

object DeckJsonCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun decode(text: String): ParsedFile {
        val root = try {
            json.parseToJsonElement(text) as? JsonObject
        } catch (e: SerializationException) {
            null
        } ?: return ParsedFile.Failure(NOT_A_DECK)

        val format = runCatching { root["format"]?.jsonPrimitive?.contentOrNull }.getOrNull()
        if (format != DECK_FORMAT) return ParsedFile.Failure(NOT_A_DECK)
        val version = runCatching { root["version"]?.jsonPrimitive?.intOrNull }.getOrNull() ?: 1
        if (version > 1) return ParsedFile.Failure(NEWER_VERSION)

        val file = try {
            json.decodeFromJsonElement(DeckFileDto.serializer(), root)
        } catch (e: IllegalArgumentException) {
            // Covers SerializationException too: a field holding the wrong kind of value.
            return ParsedFile.Failure(NOT_A_DECK)
        }
        return ImportValidator.fromJson(file.deck.name, file.cards.map { it.toRaw() })
    }

    fun encode(deck: Deck, cards: List<Card>): String = json.encodeToString(
        DeckFileDto.serializer(),
        DeckFileDto(
            deck = DeckInfoDto(deck.name, deck.description),
            cards = cards.sortedBy { it.position }.map { it.toDto() },
        ),
    )

    fun CardType.key(): String = when (this) {
        CardType.MC -> "mc"
        CardType.TF -> "tf"
        CardType.FLASH -> "card"
    }

    fun Card.toDto() = CardDto(
        type = type.key(),
        prompt = prompt,
        answer = answer,
        wrong = if (type == CardType.MC) wrongAnswers else emptyList(),
        explanation = explanation,
    )

    fun CardDto.toRaw() = RawCard(type, prompt, answer, wrong, explanation)
}
