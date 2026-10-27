package com.recall.ui.nav

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.recall.R
import com.recall.data.db.StudyMode
import com.recall.ui.kiln.BottomBarItem
import com.recall.ui.kiln.KilnBottomBar
import com.recall.ui.kiln.KilnIcons
import com.recall.ui.kiln.RiseEntrance
import com.recall.ui.screens.library.LibraryScreen
import com.recall.ui.screens.progress.ProgressScreen
import com.recall.ui.screens.settings.SettingsScreen
import com.recall.ui.theme.Kiln
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** Room the floating bar takes above the navigation bar: 52dp items, 6dp padding and its 20dp margin. */
internal val BottomBarSpace = 84.dp

private enum class Tab(val icon: ImageVector, val label: Int) {
    LIBRARY(KilnIcons.library, R.string.nav_library),
    PROGRESS(KilnIcons.progress, R.string.nav_progress),
    SETTINGS(KilnIcons.settings, R.string.nav_settings),
}

/**
 * The three tabs and their bar, as one page of the app. Deeper pages slide over all of it, bar
 * included. The tabs sit side by side: swiping, tapping the bar and Back all slide between them.
 */
@Composable
fun TabsHost(app: NavHostController) {
    val motion = Kiln.motion
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState { Tab.entries.size }
    val aboveBar = remember { AboveBarSlot() }
    val showTab: (Int) -> Unit = { index -> scope.launch { pager.animateScrollToPage(index, animationSpec = motion.base()) } }

    // Back from another tab returns to the Library. The gesture drags the tabs along the same way a tap would slide them.
    PredictiveBackHandler(enabled = pager.currentPage != Tab.LIBRARY.ordinal) { progress ->
        val from = pager.currentPage
        try {
            progress.collect { event ->
                val position = from * (1f - event.progress)
                val page = position.roundToInt()
                pager.scrollToPage(page, position - page)
            }
            pager.animateScrollToPage(Tab.LIBRARY.ordinal, animationSpec = motion.base())
        } catch (e: CancellationException) {
            scope.launch { pager.animateScrollToPage(from, animationSpec = motion.base()) }
            throw e
        }
    }

    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalAboveBar provides aboveBar) {
            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxSize(),
                // The neighbours are ready before a swipe starts, so the first one doesn't stutter.
                beyondViewportPageCount = 1,
                key = { Tab.entries[it] },
            ) { index ->
                RiseEntrance {
                    when (Tab.entries[index]) {
                        Tab.LIBRARY -> LibraryScreen(
                            onOpenDeck = { app.navigate(DeckDetail(it)) },
                            onImport = { app.navigate(Import()) },
                            onStartReview = { app.navigate(Study(mode = StudyMode.DUE.name)) },
                        )
                        Tab.PROGRESS -> ProgressScreen()
                        Tab.SETTINGS -> SettingsScreen(
                            onImport = { app.navigate(Import()) },
                            onDeletedAll = { showTab(Tab.LIBRARY.ordinal) },
                        )
                    }
                }
            }
        }
        KilnBottomBar(
            items = Tab.entries.map { BottomBarItem(it.icon, stringResource(it.label)) },
            // The tab the pages are heading for, so the pill sets off as soon as a slide does.
            selectedIndex = pager.targetPage,
            onSelect = showTab,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(20.dp),
        )
        aboveBar.content?.let { Box(Modifier.fillMaxSize(), content = it) }
    }
}

/** Where tab screens put sheets, so they cover the bar instead of sliding up behind it. */
@Stable
class AboveBarSlot {
    var content: (@Composable BoxScope.() -> Unit)? by mutableStateOf(null)
}

val LocalAboveBar = staticCompositionLocalOf<AboveBarSlot?> { null }

/** Draws [content] over the bottom bar while this is in the composition. */
@Composable
fun AboveBar(content: @Composable BoxScope.() -> Unit) {
    val slot = LocalAboveBar.current
    if (slot == null) {
        Box(Modifier.fillMaxSize(), content = content)
        return
    }
    val latest by rememberUpdatedState(content)
    DisposableEffect(slot) {
        val shown: @Composable BoxScope.() -> Unit = { latest() }
        slot.content = shown
        onDispose { if (slot.content === shown) slot.content = null }
    }
}
