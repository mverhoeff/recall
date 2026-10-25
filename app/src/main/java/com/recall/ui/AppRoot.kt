package com.recall.ui

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.recall.R
import com.recall.data.settings.AppSettings
import com.recall.data.settings.resolveDark
import com.recall.di.AppContainer
import com.recall.ui.kiln.BottomBarItem
import com.recall.ui.kiln.KilnBottomBar
import com.recall.ui.kiln.KilnIcons
import com.recall.ui.nav.KilnNavHost
import com.recall.ui.nav.Library
import com.recall.ui.nav.Progress
import com.recall.ui.nav.Settings
import com.recall.ui.theme.Kiln
import com.recall.ui.theme.KilnTheme
import com.recall.ui.theme.kilnGround

private enum class TopLevel(val route: Any, val icon: ImageVector, val label: Int) {
    LIBRARY(Library, KilnIcons.library, R.string.nav_library),
    PROGRESS(Progress, KilnIcons.progress, R.string.nav_progress),
    SETTINGS(Settings, KilnIcons.settings, R.string.nav_settings),
}

@Composable
fun AppRoot(container: AppContainer) {
    // Waiting for the stored settings avoids flashing the default palette on every launch.
    val settings: AppSettings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val current = settings ?: return
    val dark = current.resolveDark(isSystemInDarkTheme())

    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
    }

    KilnTheme(current.palette, dark, current.typePairing, current.reduceMotion) {
        val navController = rememberNavController()
        Box(Modifier.fillMaxSize().kilnGround(Kiln.colors)) {
            KilnNavHost(navController, Modifier.fillMaxSize())
            TopLevelBar(navController, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun TopLevelBar(navController: NavHostController, modifier: Modifier) {
    val motion = Kiln.motion
    val destination = navController.currentBackStackEntryAsState().value?.destination
    val selected = TopLevel.entries.indexOfFirst { destination?.hasRoute(it.route::class) == true }
    // While the bar fades out it keeps showing the tab it was on.
    val last = remember { intArrayOf(0) }
    if (selected >= 0) last[0] = selected

    // Timed like the pages, so the bar leaves and returns together with the screen beneath it.
    AnimatedVisibility(
        visible = selected >= 0,
        modifier = modifier,
        enter = fadeIn(motion.base(delay = 90)) + scaleIn(motion.base(delay = 90), initialScale = 0.94f),
        exit = fadeOut(motion.fast()) + scaleOut(motion.fast(), targetScale = 0.94f),
    ) {
        KilnBottomBar(
            items = TopLevel.entries.map { BottomBarItem(it.icon, stringResource(it.label)) },
            selectedIndex = last[0],
            onSelect = { index ->
                navController.navigate(TopLevel.entries[index].route) {
                    // Tabs keep their own back stacks and scroll positions instead of piling up.
                    popUpTo(Library) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = Modifier
                .navigationBarsPadding()
                .padding(20.dp),
        )
    }
}
