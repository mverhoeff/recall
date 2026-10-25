package com.recall.ui.nav

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.recall.R
import com.recall.data.db.StudyMode
import com.recall.ui.kiln.RiseEntrance
import com.recall.ui.screens.PlaceholderScreen
import com.recall.ui.screens.deck.CardListScreen
import com.recall.ui.screens.deck.DeckDetailScreen
import com.recall.ui.screens.library.LibraryScreen
import com.recall.ui.theme.Kiln

// The new page waits until the old one has mostly faded, so the two never show through each other.
private const val THROUGH_DELAY_MS = 90

// Going deeper, pages come up towards the viewer; going back, they sink away.
private const val NEAR_SCALE = 1.03f
private const val FAR_SCALE = 0.96f

@Composable
fun KilnNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    val motion = Kiln.motion
    NavHost(
        navController = navController,
        startDestination = Library,
        modifier = modifier,
        enterTransition = {
            fadeIn(motion.base(delay = THROUGH_DELAY_MS)) + scaleIn(motion.base(delay = THROUGH_DELAY_MS), FAR_SCALE)
        },
        exitTransition = { fadeOut(motion.fast()) + scaleOut(motion.fast(), NEAR_SCALE) },
        popEnterTransition = {
            fadeIn(motion.base(delay = THROUGH_DELAY_MS)) + scaleIn(motion.base(delay = THROUGH_DELAY_MS), NEAR_SCALE)
        },
        popExitTransition = { fadeOut(motion.fast()) + scaleOut(motion.fast(), FAR_SCALE) },
    ) {
        screen<Library> {
            LibraryScreen(
                onOpenDeck = { navController.navigate(DeckDetail(it)) },
                onImport = { navController.navigate(Import()) },
                onStartReview = { navController.navigate(Study(mode = StudyMode.DUE.name)) },
            )
        }
        screen<Progress> { PlaceholderScreen(stringResource(R.string.nav_progress)) }
        screen<Settings> { PlaceholderScreen(stringResource(R.string.nav_settings)) }
        screen<DeckDetail> { entry ->
            val deckId = entry.toRoute<DeckDetail>().deckId
            DeckDetailScreen(
                deckId = deckId,
                onBack = { navController.back(entry) },
                onAddCard = { navController.navigate(CardEditor(deckId)) },
                onOpenCard = { navController.navigate(CardEditor(deckId, it)) },
                onSeeAll = { navController.navigate(CardList(deckId)) },
                onDeleted = { navController.back(entry) },
                onStartStudy = { mode, size, shuffle, missedFirst ->
                    navController.navigate(Study(mode.name, deckId, size = size, shuffle = shuffle, missedFirst = missedFirst))
                },
            )
        }
        screen<CardList> { entry ->
            val deckId = entry.toRoute<CardList>().deckId
            CardListScreen(
                deckId = deckId,
                onBack = { navController.back(entry) },
                onOpenCard = { navController.navigate(CardEditor(deckId, it)) },
            )
        }
        screen<CardEditor> { PlaceholderScreen("Card") }
        screen<Study> { PlaceholderScreen("Study") }
        screen<Results> { PlaceholderScreen("Results") }
        screen<Import> { PlaceholderScreen("Import") }
    }
}

private inline fun <reified T : Any> NavGraphBuilder.screen(noinline content: @Composable (NavBackStackEntry) -> Unit) {
    composable<T> { entry -> RiseEntrance { content(entry) } }
}

// A second tap on Back while the page is already leaving would otherwise pop the page beneath it too.
private fun NavHostController.back(entry: NavBackStackEntry) {
    if (currentBackStackEntry?.id == entry.id) popBackStack()
}
