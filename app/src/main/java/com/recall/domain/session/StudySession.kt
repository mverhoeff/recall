package com.recall.domain.session

import com.recall.data.db.Card
import com.recall.data.db.CardType
import com.recall.data.db.Grade
import java.time.Clock

enum class Presentation { QUIZ, FLASH }

const val TRUE_OPTION = "True"
const val FALSE_OPTION = "False"

data class SessionItem(val card: Card, val presentation: Presentation, val options: List<String>) {
    /** Positions in [options] that are right. A choice card can have several. */
    val correct: Set<Int>
        get() = when (card.type) {
            CardType.MC -> {
                val right = (listOf(card.answer) + card.alsoCorrect).map { it.lowercase() }.toSet()
                options.indices.filter { options[it].lowercase() in right }.toSet()
            }
            CardType.TF -> setOf(if (card.answer == "true") 0 else 1)
            CardType.FLASH -> emptySet()
        }

    /** Whether more than one option has to be picked, so the options act as checkboxes. */
    val multiple: Boolean
        get() = correct.size > 1
}

data class ItemResult(val card: Card, val correct: Boolean)

data class SessionState(
    val items: List<SessionItem>,
    val index: Int = 0,
    val selected: Set<Int> = emptySet(),
    val checked: Boolean = false,
    val flipped: Boolean = false,
    val results: List<ItemResult> = emptyList(),
    val finished: Boolean = items.isEmpty(),
) {
    val current: SessionItem?
        get() = items.getOrNull(index)

    /** The latest answer, once [checked]. */
    val lastCorrect: Boolean?
        get() = if (checked) results.lastOrNull()?.correct else null

    val isLast: Boolean
        get() = index == items.lastIndex
}

data class SessionResult(val results: List<ItemResult>, val elapsedMillis: Long) {
    val total: Int get() = results.size
    val correct: Int get() = results.count { it.correct }
    val accuracy: Int get() = if (total == 0) 0 else correct * 100 / total
    val missed: List<Card> get() = results.filterNot { it.correct }.map { it.card }
}

/** What was just answered, for the caller to save. */
data class Answer(val card: Card, val correct: Boolean, val grade: Grade, val firstTry: Boolean)

/**
 * One round of study. Quiz items go select → check → next; flashcards go flip → grade, which also
 * moves on. Calls that don't fit the current step are ignored.
 */
class StudySession(items: List<SessionItem>, private val clock: Clock) {
    var state: SessionState = SessionState(items)
        private set

    private val startedAt = clock.millis()
    private var lastAnsweredAt = startedAt
    private val answered = mutableSetOf<Long>()

    fun select(index: Int) {
        val item = state.current ?: return
        if (item.presentation != Presentation.QUIZ || state.checked || index !in item.options.indices) return
        val selected = when {
            !item.multiple -> setOf(index)
            index in state.selected -> state.selected - index
            else -> state.selected + index
        }
        state = state.copy(selected = selected)
    }

    /** Right only when exactly the correct options are picked. */
    fun check(): Answer? {
        val item = state.current ?: return null
        if (item.presentation != Presentation.QUIZ || state.checked || state.selected.isEmpty()) return null
        val correct = state.selected == item.correct
        val answer = answer(item, correct, if (correct) Grade.GOOD else Grade.AGAIN)
        state = state.copy(checked = true, results = state.results + ItemResult(item.card, correct))
        return answer
    }

    fun next() {
        if (state.current?.presentation == Presentation.QUIZ && !state.checked) return
        advance()
    }

    fun flip() {
        if (state.current?.presentation != Presentation.FLASH) return
        state = state.copy(flipped = true)
    }

    /** Again counts as missed; every other grade as known. */
    fun grade(grade: Grade): Answer? {
        val item = state.current ?: return null
        if (item.presentation != Presentation.FLASH || !state.flipped) return null
        val correct = grade != Grade.AGAIN
        val answer = answer(item, correct, grade)
        state = state.copy(results = state.results + ItemResult(item.card, correct))
        advance()
        return answer
    }

    /** Moves past the current item without an answer. */
    fun skip() {
        if (state.current == null || state.checked) return
        advance()
    }

    fun result(): SessionResult = SessionResult(state.results, lastAnsweredAt - startedAt)

    private fun answer(item: SessionItem, correct: Boolean, grade: Grade): Answer {
        lastAnsweredAt = clock.millis()
        return Answer(item.card, correct, grade, firstTry = answered.add(item.card.id))
    }

    private fun advance() {
        val next = state.index + 1
        state = if (next >= state.items.size) {
            state.copy(finished = true)
        } else {
            state.copy(index = next, selected = emptySet(), checked = false, flipped = false)
        }
    }
}
