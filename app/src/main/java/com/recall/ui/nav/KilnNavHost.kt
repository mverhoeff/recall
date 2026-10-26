package com.recall.ui.nav

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.recall.data.db.StudyMode
import com.recall.ui.kiln.RiseEntrance
import com.recall.ui.screens.deck.CardListScreen
import com.recall.ui.screens.deck.DeckDetailScreen
import com.recall.ui.screens.editor.CardEditorScreen
import com.recall.ui.screens.importer.ImportScreen
import com.recall.ui.screens.results.ResultsScreen
import com.recall.ui.screens.study.StudyScreen
import com.recall.ui.theme.Kiln

// The page underneath moves a quarter of the way, so the two pages don't travel as one sheet.
private const val UNDER_SHIFT = 4

@Composable
fun KilnNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    val motion = Kiln.motion
    NavHost(
        navController = navController,
        startDestination = Home,
        modifier = modifier,
        // Deeper pages come in from the right and leave to the right, the way the back gesture pulls them.
        enterTransition = { slideInHorizontally(motion.base()) { it } },
        exitTransition = { slideOutHorizontally(motion.base()) { -it / UNDER_SHIFT } },
        popEnterTransition = { slideInHorizontally(motion.base()) { -it / UNDER_SHIFT } },
        popExitTransition = { slideOutHorizontally(motion.base()) { it } },
        // The back gesture drags the same slide instead of Navigation's default shrink and fade.
        predictivePopEnterTransition = { slideInHorizontally(motion.base()) { -it / UNDER_SHIFT } },
        predictivePopExitTransition = { slideOutHorizontally(motion.base()) { it } },
    ) {
        page<Home> { TabsHost(navController) }
        page<DeckDetail> { entry ->
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
        page<CardList> { entry ->
            val deckId = entry.toRoute<CardList>().deckId
            CardListScreen(
                deckId = deckId,
                onBack = { navController.back(entry) },
                onOpenCard = { navController.navigate(CardEditor(deckId, it)) },
            )
        }
        page<CardEditor> { entry ->
            val route = entry.toRoute<CardEditor>()
            CardEditorScreen(route.deckId, route.cardId, onClose = { navController.back(entry) })
        }
        page<Study> { entry ->
            StudyScreen(
                route = entry.toRoute<Study>(),
                onClose = { navController.back(entry) },
                onFinished = { key ->
                    navController.navigate(Results(key)) { popUpTo<Study> { inclusive = true } }
                },
            )
        }
        page<Results> { entry ->
            ResultsScreen(
                sessionKey = entry.toRoute<Results>().sessionKey,
                // The retry round replaces these results, so finishing it lands on fresh ones.
                onRetry = { ids ->
                    navController.navigate(Study(StudyMode.QUIZ.name, cardIds = ids.joinToString(","))) {
                        popUpTo<Results> { inclusive = true }
                    }
                },
                onBack = { navController.back(entry) },
                onMissing = { navController.popBackStack<Home>(inclusive = false) },
            )
        }
        page<Import> { entry ->
            ImportScreen(
                targetDeckId = entry.toRoute<Import>().targetDeckId,
                onBack = { navController.back(entry) },
                onImported = { deckId ->
                    navController.navigate(DeckDetail(deckId)) { popUpTo<Import> { inclusive = true } }
                },
                onRestored = { navController.popBackStack<Home>(inclusive = false) },
            )
        }
    }
}

internal inline fun <reified T : Any> NavGraphBuilder.page(noinline content: @Composable (NavBackStackEntry) -> Unit) {
    composable<T> { entry ->
        // Pages are opaque, so one sliding over another doesn't show it through.
        Box(
            Modifier
                .fillMaxSize()
                .background(Kiln.colors.bg),
        ) {
            RiseEntrance { content(entry) }
        }
    }
}

// A second tap on Back while the page is already leaving would otherwise pop the page beneath it too.
private fun NavHostController.back(entry: NavBackStackEntry) {
    if (currentBackStackEntry?.id == entry.id) popBackStack()
}
