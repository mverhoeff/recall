package com.recall.ui.screens.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recall.data.db.Card
import com.recall.data.db.CardType
import com.recall.data.repo.DeckRepository
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

data class CardEditorUiState(
    val deckName: String,
    /** The card being edited, or null for a new one. */
    val original: Card?,
    val draft: CardDraft,
    val saved: Boolean = false,
)

class CardEditorViewModel(
    private val deckId: Long,
    cardId: Long?,
    private val store: CardStore,
) : ViewModel() {
    private val _state = MutableStateFlow<CardEditorUiState?>(null)
    val state: StateFlow<CardEditorUiState?> = _state

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

    /** Saves the draft; [onSaved] runs once it is written. */
    fun save(onSaved: () -> Unit) {
        val current = _state.value ?: return
        val card = current.draft.trimmed()
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
        _state.update { it?.copy(original = null, draft = CardDraft(it.draft.type), saved = true) }
        savedJob?.cancel()
        savedJob = viewModelScope.launch {
            delay(SAVED_SHOWN_MS)
            _state.update { it?.copy(saved = false) }
        }
    }

    private fun edit(change: (CardDraft) -> CardDraft) = _state.update { current ->
        current ?: return@update null
        current.copy(draft = change(current.draft))
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

private fun CardDraft.trimmed() = copy(
    prompt = prompt.trim(),
    options = options.map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("") },
    correct = options.take(correct).count { it.isNotBlank() },
    back = back.trim(),
    explanation = explanation.trim(),
)
