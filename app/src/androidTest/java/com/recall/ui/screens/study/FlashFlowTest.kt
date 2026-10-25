package com.recall.ui.screens.study

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.recall.RecallApp
import com.recall.data.db.Card
import com.recall.data.db.CardType
import com.recall.data.db.Deck
import com.recall.data.db.Phase
import com.recall.data.db.RecallDatabase
import com.recall.data.db.StudyMode
import com.recall.di.AppContainer
import com.recall.ui.nav.Study
import com.recall.ui.theme.ClayBody
import com.recall.ui.theme.KilnTheme
import com.recall.ui.theme.TypePairing
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlashFlowTest {

    @get:Rule
    val rule = createComposeRule()

    private val app = ApplicationProvider.getApplicationContext<RecallApp>()
    private lateinit var original: AppContainer
    private lateinit var database: RecallDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(app, RecallDatabase::class.java).build()
        original = app.container
        app.container = AppContainer(app, database)
    }

    @After
    fun tearDown() {
        app.container = original
        database.close()
    }

    @Test
    fun gradingGoodMovesNewCardsIntoReview() {
        val deckId = runBlocking {
            val deckId = database.deckDao().insert(Deck(name = "Spanish verbs", createdAt = 1, updatedAt = 1))
            listOf("poder · preterite · yo" to "pude", "tener · present · yo" to "tengo").forEachIndexed { i, (front, back) ->
                database.cardDao().insertWithState(
                    Card(deckId = deckId, type = CardType.FLASH, prompt = front, answer = back, position = i, createdAt = 1, updatedAt = 1),
                )
            }
            deckId
        }
        var finished: String? = null
        rule.setContent {
            KilnTheme(ClayBody.SLATE, dark = false, pairing = TypePairing.B, reducedMotion = true) {
                StudyScreen(Study(StudyMode.DUE.name, deckId), onClose = {}, onFinished = { finished = it })
            }
        }

        repeat(2) {
            rule.waitUntil(5_000) { rule.onAllNodes(hasText("Show answer")).fetchSemanticsNodes().size == 1 }
            rule.onNodeWithText("Show answer").performClick()
            rule.onNodeWithText("Good").performClick()
        }

        rule.waitUntil(5_000) { finished != null }
        assertNotNull(finished)
        // Answers are saved off the main thread, so wait for both to land.
        rule.waitUntil(5_000) {
            runBlocking { database.reviewDao().statesByDeck(deckId).all { it.phase == Phase.REVIEW } }
        }
        val phases = runBlocking { database.reviewDao().statesByDeck(deckId).map { it.phase } }
        assertEquals(listOf(Phase.REVIEW, Phase.REVIEW), phases)
    }
}
