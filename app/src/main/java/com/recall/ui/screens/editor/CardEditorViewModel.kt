package com.recall.ui.screens.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recall.data.db.Card
import com.recall.data.db.CardType
import com.recall.data.repo.DeckRepository
import com.recall.io.ImportValidator
import com.recall.io.ImportedCard
import com.recall.io.ParsedFile
import com.recall.io.RawCard
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val MIN_OPTIONS = 2
const val MAX_OPTIONS = 6
private const val NEW_OPTIONS = 4
private const val SAVED_SHOWN_MS = 1_500L

/** What the editor reads and writes; the app's is [DeckCardStore]. */
interface CardStore {
    suspend fun deckName(deckId: Long): String?
    suspend fun card(deckId: Long, cardId: Long): Card?
    suspend fun add(card: Card): Long
    suspend fun update(card: Card)
    suspend fun delete(cardId: Long)
}

class DeckCardStore(private val repository: DeckRepository) : CardStore {
    // A deck page that was just open has these in memory, so the editor can draw them on its first frame.
    override suspend fun deckName(deckId: Long): String? =
        repository.cachedDetails(deckId)?.summary?.deck?.name ?: repository.summary(deckId).first()?.deck?.name

    override suspend fun card(deckId: Long, cardId: Long): Card? =
        repository.cachedDetails(deckId)?.cards?.firstOrNull { it.first.id == cardId }?.first
            ?: repository.cardsForDeck(deckId).first().firstOrNull { it.id == cardId }

    override suspend fun add(card: Card): Long = repository.addCard(card)

    override suspend fun update(card: Card) = repository.updateCard(card)

    override suspend fun delete(cardId: Long) = repository.deleteCard(cardId)
}

/** Every type's fields live side by side, so switching type and back loses nothing. */
data class CardDraft(
    val type: CardType = CardType.MC,
    val prompt: String = "",
    val options: List<String> = List(NEW_OPTIONS) { "" },
    val correct: Int = 0,
    val truth: Boolean = true,
    val back: String = "",
    val explanation: String = "",
) {
    val answer: String
        get() = when (type) {
            CardType.MC -> options.getOrElse(correct) { "" }
            CardType.TF -> truth.toString()
            CardType.FLASH -> back
        }

    val wrong: List<String>
        get() = if (type == CardType.MC) options.filterIndexed { index, _ -> index != correct } else emptyList()
}

enum class DraftProblem { PROMPT, ANSWERS, BACK }

data class CardEditorUiState(
    val deckName: String,
    /** The card being edited, or null for a new one. */
    val original: Card?,
    val draft: CardDraft,
    /** Only set once a save has been tried, and cleared as soon as the draft is fixed. */
    val problem: DraftProblem? = null,
    val saved: Boolean = false,
) {
    val edited: Boolean
        get() = draft != (original?.let(::draftOf) ?: CardDraft(draft.type))
}

class CardEditorViewModel(
    private val deckId: Long,
    cardId: Long?,
    private val store: CardStore,
) : ViewModel() {
    private val _state = MutableStateFlow<CardEditorUiState?>(null)
    val state: StateFlow<CardEditorUiState?> = _state

    private var checked = false
    private var saving = false
    private var savedJob: Job? = null

    init {
        viewModelScope.launch {
            val name = store.deckName(deckId) ?: return@launch
            val original = cardId?.let { store.card(deckId, it) }
            _state.value = CardEditorUiState(name, original, original?.let(::draftOf) ?: CardDraft())
        }
    }

    fun selectType(type: CardType) = edit { it.copy(type = type) }

    fun setPrompt(text: String) = edit { it.copy(prompt = text) }

    fun setOption(index: Int, text: String) = edit { draft ->
        draft.copy(options = draft.options.mapIndexed { i, old -> if (i == index) text else old })
    }

    fun selectCorrect(index: Int) = edit { it.copy(correct = index) }

    fun addOption() = edit { draft ->
        if (draft.options.size >= MAX_OPTIONS) draft else draft.copy(options = draft.options + "")
    }

    fun removeOption(index: Int) = edit { draft ->
        if (draft.options.size <= MIN_OPTIONS) return@edit draft
        val correct = when {
            index < draft.correct -> draft.correct - 1
            index == draft.correct -> 0
            else -> draft.correct
        }
        draft.copy(options = draft.options.filterIndexed { i, _ -> i != index }, correct = correct)
    }

    fun setTruth(truth: Boolean) = edit { it.copy(truth = truth) }

    fun setBack(text: String) = edit { it.copy(back = text) }

    fun setExplanation(text: String) = edit { it.copy(explanation = text) }

    /** Saves the draft; [onSaved] runs once it is written. Does nothing while the draft has a problem. */
    fun save(onSaved: () -> Unit) {
        val current = _state.value ?: return
        val card = when (val result = validate(current.draft)) {
            is Validated.Bad -> {
                checked = true
                _state.value = current.copy(problem = result.problem)
                return
            }
            is Validated.Good -> result.card
        }
        if (saving) return
        saving = true
        viewModelScope.launch {
            val original = current.original
            if (original == null) {
                store.add(
                    Card(
                        deckId = deckId,
                        type = card.type,
                        prompt = card.prompt,
                        answer = card.answer,
                        wrongAnswers = card.wrong,
                        explanation = card.explanation,
                        position = 0,
                        createdAt = 0,
                        updatedAt = 0,
                    ),
                )
            } else {
                store.update(
                    original.copy(
                        type = card.type,
                        prompt = card.prompt,
                        answer = card.answer,
                        wrongAnswers = card.wrong,
                        explanation = card.explanation,
                    ),
                )
            }
            saving = false
            onSaved()
        }
    }

    /** Saves, then starts a fresh card of the same type and briefly says so. */
    fun saveAndAddNext() = save {
        checked = false
        _state.update { it?.copy(original = null, draft = CardDraft(it.draft.type), problem = null, saved = true) }
        savedJob?.cancel()
        savedJob = viewModelScope.launch {
            delay(SAVED_SHOWN_MS)
            _state.update { it?.copy(saved = false) }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val original = _state.value?.original ?: return
        viewModelScope.launch {
            store.delete(original.id)
            onDeleted()
        }
    }

    private fun edit(change: (CardDraft) -> CardDraft) = _state.update { current ->
        current ?: return@update null
        val draft = change(current.draft)
        val problem = if (checked) (validate(draft) as? Validated.Bad)?.problem else null
        current.copy(draft = draft, problem = problem)
    }
}

/** An existing card as a draft. Its correct answer goes first, then the wrong ones in their order. */
internal fun draftOf(card: Card): CardDraft = when (card.type) {
    CardType.MC -> CardDraft(
        type = card.type,
        prompt = card.prompt,
        options = (listOf(card.answer) + card.wrongAnswers).let { it + List(maxOf(0, MIN_OPTIONS - it.size)) { "" } },
        explanation = card.explanation,
    )
    CardType.TF -> CardDraft(card.type, card.prompt, truth = card.answer == "true", explanation = card.explanation)
    CardType.FLASH -> CardDraft(card.type, card.prompt, back = card.answer, explanation = card.explanation)
}

private sealed interface Validated {
    data class Good(val card: ImportedCard) : Validated
    data class Bad(val problem: DraftProblem) : Validated
}

// The same rules as a file import, so a card typed here can never be one an import would refuse.
private fun validate(draft: CardDraft): Validated {
    val type = when (draft.type) {
        CardType.MC -> "mc"
        CardType.TF -> "tf"
        CardType.FLASH -> "card"
    }
    val raw = RawCard(type, draft.prompt, draft.answer, draft.wrong, draft.explanation)
    val parsed: ParsedFile.Deck = ImportValidator.fromJson(null, listOf(raw))
    parsed.cards.firstOrNull()?.let { return Validated.Good(it) }
    return Validated.Bad(problemFor(withoutPlace(parsed.issues.first().message)))
}

/** Drops the "Card 1: " an import puts in front of each message. */
internal fun withoutPlace(message: String): String = message.substringAfter(": ")

private fun problemFor(message: String): DraftProblem = when (message) {
    "the prompt is empty." -> DraftProblem.PROMPT
    "a choice card needs at least 2 answers." -> DraftProblem.ANSWERS
    "the back of the card is empty." -> DraftProblem.BACK
    else -> error("No field for \"$message\"")
}
