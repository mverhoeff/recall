package com.recall.debug

import com.recall.data.db.Card
import com.recall.data.db.CardType
import com.recall.data.db.Deck
import com.recall.data.db.Grade
import com.recall.data.db.Phase
import com.recall.data.db.ReviewLog
import com.recall.data.db.ReviewState
import com.recall.data.db.StudyMode
import com.recall.di.AppContainer
import java.time.Duration
import java.time.LocalDate

/** Decks with a few days of history, for looking at screens without typing cards in by hand. */
object SampleData {
    private class SampleCard(val type: CardType, val prompt: String, val answer: String, val wrong: List<String>, val explanation: String)

    private fun mc(prompt: String, answer: String, vararg wrong: String, explanation: String = "") =
        SampleCard(CardType.MC, prompt, answer, wrong.toList(), explanation)

    private fun tf(prompt: String, answer: Boolean, explanation: String = "") =
        SampleCard(CardType.TF, prompt, answer.toString(), emptyList(), explanation)

    private fun fc(prompt: String, answer: String) = SampleCard(CardType.FLASH, prompt, answer, emptyList(), "")

    // One letter per card: R review due today, r review due later, L learning, N new.
    private val decks = listOf(
        Triple(
            "Spanish verbs — A2", "Present and preterite of common verbs",
            "RRRrrrLLLNNN" to listOf(
                mc("Yo ___ hambre.", "tengo", "tiene", "tienes", "tenemos", explanation = "Tener, first person singular."),
                mc("Nosotros ___ en Madrid.", "vivimos", "vivís", "viven", "vivo"),
                mc("Ellos ___ español.", "hablan", "hablamos", "hablas"),
                tf("\"Fui\" is the preterite yo form of both ir and ser.", true),
                tf("\"Hago\" is the present yo form of hacer.", true),
                tf("\"Sabo\" is the present yo form of saber.", false, "It's \"sé\"."),
                fc("poder · preterite · yo", "pude"),
                fc("tener · preterite · yo", "tuve"),
                fc("decir · preterite · él", "dijo"),
                fc("querer · present · tú", "quieres"),
                fc("venir · present · yo", "vengo"),
                fc("estar · preterite · nosotros", "estuvimos"),
            ),
        ),
        Triple(
            "Human anatomy: bones", "",
            "rrrrRRNNNNNN" to listOf(
                mc("Which is the longest bone in the human body?", "Femur", "Tibia", "Humerus", "Fibula"),
                mc("How many bones does an adult skeleton usually have?", "206", "186", "212", "226"),
                mc("Which bone is the collarbone?", "Clavicle", "Scapula", "Sternum"),
                mc("Where is the smallest bone in the body?", "Middle ear", "Nose", "Little toe", "Wrist", explanation = "The stapes, about 3 mm long."),
                tf("The patella is the kneecap.", true),
                tf("The radius is on the little-finger side of the forearm.", false, "That's the ulna. The radius is on the thumb side."),
                tf("Adults have 12 pairs of ribs.", true),
                tf("The mandible is the only bone of the skull that moves.", true),
                fc("Shoulder blade", "Scapula"),
                fc("Breastbone", "Sternum"),
                fc("Upper arm bone", "Humerus"),
                fc("Bones of the fingers and toes", "Phalanges"),
            ),
        ),
        Triple(
            "Kotlin coroutines", "Builders, scopes and flows",
            "NNNNNNNNNNNN" to listOf(
                mc("Which builder returns a Deferred?", "async", "launch", "runBlocking", "withContext"),
                mc("Which dispatcher is meant for blocking I/O?", "Dispatchers.IO", "Dispatchers.Main", "Dispatchers.Default", "Dispatchers.Unconfined"),
                mc("What does launch return?", "Job", "Deferred", "Unit", "Flow"),
                tf("delay() blocks the thread it runs on.", false, "It suspends the coroutine and frees the thread."),
                tf("A suspend function can only be called from a coroutine or another suspend function.", true),
                tf("Cancelling a parent job cancels its children.", true),
                tf("A cold flow runs its code again for every collector.", true),
                fc("Switches context and returns the block's result", "withContext"),
                fc("Scope that ends when a ViewModel is cleared", "viewModelScope"),
                fc("Hot flow that always holds a current value", "StateFlow"),
                fc("Scope where one failing child doesn't cancel the others", "supervisorScope"),
                fc("Blocks the thread until its coroutines finish, mostly for main() and tests", "runBlocking"),
            ),
        ),
        Triple(
            "EU capitals", "",
            "rrrrrrrrrrrr" to listOf(
                mc("Capital of Portugal?", "Lisbon", "Porto", "Madrid"),
                mc("Capital of Slovenia?", "Ljubljana", "Bratislava", "Zagreb"),
                mc("Capital of Estonia?", "Tallinn", "Riga", "Vilnius", "Helsinki"),
                mc("Capital of Malta?", "Valletta", "Mdina", "Sliema"),
                tf("Brussels is the capital of Belgium.", true),
                tf("Kraków is the capital of Poland.", false, "It's Warsaw."),
                tf("Sofia is the capital of Bulgaria.", true),
                tf("Turku is the capital of Finland.", false, "It's Helsinki."),
                fc("Latvia", "Riga"),
                fc("Croatia", "Zagreb"),
                fc("Slovakia", "Bratislava"),
                fc("Cyprus", "Nicosia"),
            ),
        ),
    )

    suspend fun seed(container: AppContainer) {
        val db = container.database
        val clock = container.clock
        val now = clock.instant()
        val today = LocalDate.now(clock)
        fun daysAgo(days: Long) = today.minusDays(days).atTime(10, 0).atZone(clock.zone).toInstant().toEpochMilli()

        decks.forEachIndexed { d, (name, description, content) ->
            val (plan, cards) = content
            val deckId = db.deckDao().insert(Deck(name = name, description = description, createdAt = daysAgo(10L - d), updatedAt = daysAgo(4L - d)))
            cards.forEachIndexed { i, sample ->
                val cardId = db.cardDao().insertWithState(
                    Card(
                        deckId = deckId, type = sample.type, prompt = sample.prompt, answer = sample.answer,
                        wrongAnswers = sample.wrong, explanation = sample.explanation, position = i,
                        createdAt = daysAgo(10L - d), updatedAt = daysAgo(10L - d),
                    ),
                )
                val state = when (plan[i]) {
                    'R' -> ReviewState(cardId, Phase.REVIEW, now.minus(Duration.ofHours(1)).toEpochMilli(), 3, 2.5, 3, 0)
                    'r' -> ReviewState(cardId, Phase.REVIEW, daysAgo(-3L - i % 4), 6, 2.6, 4, 0)
                    'L' -> ReviewState(cardId, Phase.LEARNING, now.minus(Duration.ofMinutes(10)).toEpochMilli(), 0, 2.3, 2, 1)
                    else -> return@forEachIndexed
                }
                db.reviewDao().upsertState(state)
                // Spread answers over the last four days, so the streak and weekly stats have something to show.
                for (back in listOf(1 + i % 4, 1 + (i + 2) % 4)) {
                    val correct = (i + back) % 5 != 0
                    db.reviewDao().insertLog(
                        ReviewLog(
                            cardId = cardId, deckId = deckId, reviewedAt = daysAgo(back.toLong()) + i * 60_000L,
                            grade = if (correct) Grade.GOOD else Grade.AGAIN, correct = correct,
                            mode = StudyMode.QUIZ, firstTry = true,
                        ),
                    )
                }
                if (d == 0 && i == 0) {
                    db.reviewDao().insertLog(
                        ReviewLog(
                            cardId = cardId, deckId = deckId, reviewedAt = now.minus(Duration.ofMinutes(30)).toEpochMilli(),
                            grade = Grade.GOOD, correct = true, mode = StudyMode.QUIZ, firstTry = true,
                        ),
                    )
                }
            }
        }
    }
}
