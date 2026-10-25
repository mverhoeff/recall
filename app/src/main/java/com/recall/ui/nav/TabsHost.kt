package com.recall.ui.nav

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.recall.R
import com.recall.data.db.StudyMode
import com.recall.ui.kiln.BottomBarItem
import com.recall.ui.kiln.KilnBottomBar
import com.recall.ui.kiln.KilnIcons
import com.recall.ui.screens.PlaceholderScreen
import com.recall.ui.screens.library.LibraryScreen
import com.recall.ui.theme.Kiln

// The old tab clears before the new one fades in, so the two never show through each other.
private const val THROUGH_DELAY_MS = 90

private enum class Tab(val route: Any, val icon: ImageVector, val label: Int) {
    LIBRARY(Library, KilnIcons.library, R.string.nav_library),
    PROGRESS(Progress, KilnIcons.progress, R.string.nav_progress),
    SETTINGS(Settings, KilnIcons.settings, R.string.nav_settings),
}

/**
 * The three tabs and their bar, as one page of the app. Deeper pages slide over all of it, bar
 * included; switching tabs only fades the content while the bar stays put.
 */
@Composable
fun TabsHost(app: NavHostController) {
    val motion = Kiln.motion
    val tabs = rememberNavController()
    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = tabs,
            startDestination = Library,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(motion.base(delay = THROUGH_DELAY_MS)) },
            exitTransition = { fadeOut(motion.fast()) },
            popEnterTransition = { fadeIn(motion.base(delay = THROUGH_DELAY_MS)) },
            popExitTransition = { fadeOut(motion.fast()) },
        ) {
            page<Library> {
                LibraryScreen(
                    onOpenDeck = { app.navigate(DeckDetail(it)) },
                    onImport = { app.navigate(Import()) },
                    onStartReview = { app.navigate(Study(mode = StudyMode.DUE.name)) },
                )
            }
            page<Progress> { PlaceholderScreen(stringResource(R.string.nav_progress)) }
            page<Settings> { PlaceholderScreen(stringResource(R.string.nav_settings)) }
        }
        TabBar(tabs, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun TabBar(tabs: NavHostController, modifier: Modifier) {
    val destination = tabs.currentBackStackEntryAsState().value?.destination
    val selected = Tab.entries.indexOfFirst { destination?.hasRoute(it.route::class) == true }.coerceAtLeast(0)
    KilnBottomBar(
        items = Tab.entries.map { BottomBarItem(it.icon, stringResource(it.label)) },
        selectedIndex = selected,
        onSelect = { index ->
            tabs.navigate(Tab.entries[index].route) {
                // Tabs keep their own back stacks and scroll positions instead of piling up.
                popUpTo(Library) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        modifier = modifier
            .navigationBarsPadding()
            .padding(20.dp),
    )
}
