package com.recall.ui.nav

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.recall.R
import com.recall.data.db.StudyMode
import com.recall.ui.screens.PlaceholderScreen
import com.recall.ui.screens.library.LibraryScreen
import com.recall.ui.theme.Kiln

@Composable
fun KilnNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    val motion = Kiln.motion
    NavHost(
        navController = navController,
        startDestination = Library,
        modifier = modifier,
        enterTransition = { fadeIn(motion.base()) },
        exitTransition = { fadeOut(motion.base()) },
        popEnterTransition = { fadeIn(motion.base()) },
        popExitTransition = { fadeOut(motion.base()) },
    ) {
        composable<Library> {
            LibraryScreen(
                onOpenDeck = { navController.navigate(DeckDetail(it)) },
                onImport = { navController.navigate(Import()) },
                onStartReview = { navController.navigate(Study(mode = StudyMode.DUE.name)) },
            )
        }
        composable<Progress> { PlaceholderScreen(stringResource(R.string.nav_progress)) }
        composable<Settings> { PlaceholderScreen(stringResource(R.string.nav_settings)) }
        composable<DeckDetail> { PlaceholderScreen("Deck") }
        composable<CardEditor> { PlaceholderScreen("Card") }
        composable<Study> { PlaceholderScreen("Study") }
        composable<Results> { PlaceholderScreen("Results") }
        composable<Import> { PlaceholderScreen("Import") }
    }
}
