package com.recall.io

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

object DeckFileReader {
    const val MAX_BYTES = 5 * 1024 * 1024
    const val TOO_LARGE = "This file is larger than 5 MB."
    const val NOT_UTF8 = "This file isn't UTF-8 text."

    fun read(bytes: ByteArray, fileName: String): ParsedFile {
        if (bytes.size > MAX_BYTES) return ParsedFile.Failure(TOO_LARGE)
        val text = try {
            // The default decoder would quietly swap bad bytes for U+FFFD and import garbled cards.
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
                .removePrefix("﻿")
        } catch (e: CharacterCodingException) {
            return ParsedFile.Failure(NOT_UTF8)
        }

        val parsed = if (text.trimStart().startsWith('{')) {
            DeckJsonCodec.decode(text)
        } else {
            ImportValidator.fromCsv(CsvParser.parse(text))
        }
        if (parsed !is ParsedFile.Deck || parsed.name != null) return parsed
        return parsed.copy(name = fileName.substringBeforeLast('.').trim().ifEmpty { null })
    }
}
