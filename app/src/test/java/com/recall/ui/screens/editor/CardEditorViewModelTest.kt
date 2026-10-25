package com.recall.ui.screens.editor

import com.recall.data.db.Card
import com.recall.data.db.CardType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CardEditorViewModelTest {
    private class FakeStore(vararg cards: Card) : CardStore {
        val cards = cards.associateBy { it.id }.toMutableMap()
        val added = mutableListOf<Card>()
        val updated = mutableListOf<Card>()

        override suspend fun deckName(deckId: Long) = "Spanish verbs"
        override suspend fun card(deckId: Long, cardId: Long) = cards[cardId]
        override suspend fun add(card: Card): Long {
            added += card
            return added.size.toLong()
        }
        override suspend fun update(card: Card) {
            updated += card
        }
    }

    private val existing = Card(
        id = 42, deckId = 1, type = CardType.MC, prompt = "I have", answer = "tengo",
        wrongAnswers = listOf("tiene", "tienes"), explanation = "", position = 7, createdAt = 100, updatedAt = 100,
    )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun CardEditorViewModel.current() = state.value!!

    @Test
    fun changingTheCorrectRadioUpdatesTheAnswer() {
        val store = FakeStore()
        val viewModel = CardEditorViewModel(1, null, store)
        viewModel.setPrompt("I have")
        listOf("tiene", "tengo", "tienes").forEachIndexed(viewModel::setOption)
        viewModel.selectCorrect(1)
        assertEquals("tengo", viewModel.current().draft.answer)

        viewModel.save {}
        assertEquals("tengo", store.added.single().answer)
        assertEquals(listOf("tiene", "tienes"), store.added.single().wrongAnswers)
    }

    @Test
    fun removingARowAboveTheCorrectOneKeepsItSelected() {
        val viewModel = CardEditorViewModel(1, null, FakeStore())
        listOf("a", "b", "c", "d").forEachIndexed(viewModel::setOption)
        viewModel.selectCorrect(2)
        viewModel.removeOption(0)

        assertEquals("c", viewModel.current().draft.answer)
    }

    @Test
    fun editingKeepsIdAndPosition() {
        val store = FakeStore(existing)
        val viewModel = CardEditorViewModel(1, existing.id, store)
        assertEquals("tengo", viewModel.current().draft.answer)

        viewModel.setPrompt("I have (yo)")
        var saved = false
        viewModel.save { saved = true }

        assertTrue(saved)
        val written = store.updated.single()
        assertEquals(42L, written.id)
        assertEquals(7, written.position)
        assertEquals("I have (yo)", written.prompt)
        assertTrue(store.added.isEmpty())
    }

    @Test
    fun switchingTypeKeepsPromptAndExplanation() {
        val viewModel = CardEditorViewModel(1, null, FakeStore())
        viewModel.setPrompt("Madrid is the capital of Spain")
        viewModel.setExplanation("Since 1561.")
        viewModel.selectType(CardType.TF)

        val draft = viewModel.current().draft
        assertEquals("Madrid is the capital of Spain", draft.prompt)
        assertEquals("Since 1561.", draft.explanation)
    }

    @Test
    fun saveAndAddNextStartsAFreshCardOfTheSameType() {
        val store = FakeStore()
        val viewModel = CardEditorViewModel(1, null, store)
        viewModel.selectType(CardType.TF)
        viewModel.setPrompt("Madrid is the capital of Spain")
        viewModel.saveAndAddNext()

        val state = viewModel.current()
        assertEquals("true", store.added.single().answer)
        assertEquals(CardType.TF, state.draft.type)
        assertEquals("", state.draft.prompt)
        assertTrue(state.saved)
    }
}
