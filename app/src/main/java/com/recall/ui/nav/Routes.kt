package com.recall.ui.nav

import kotlinx.serialization.Serializable

/** The three tabs (Library, Progress and Settings) with their bar. */
@Serializable
data object Home

@Serializable
data class DeckDetail(val deckId: Long)

@Serializable
data class CardList(val deckId: Long)

@Serializable
data class CardEditor(val deckId: Long, val cardId: Long? = null)

/** [cardIds] is comma-joined, for retrying exactly the cards missed in a round. */
@Serializable
data class Study(
    val mode: String,
    val deckId: Long? = null,
    val cardIds: String? = null,
    val size: Int = 10,
    val shuffle: Boolean = true,
    val missedFirst: Boolean = false,
)

@Serializable
data class Results(val sessionKey: String)

@Serializable
data class Import(val targetDeckId: Long? = null)
