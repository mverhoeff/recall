package com.recall.ui.screens.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recall.R
import com.recall.RecallApp
import com.recall.data.repo.DeckAccuracy
import com.recall.data.repo.ProgressStats
import com.recall.domain.stats.weekStart
import com.recall.ui.kiln.BlobShape
import com.recall.ui.kiln.BlobTone
import com.recall.ui.kiln.KilnBlob
import com.recall.ui.kiln.KilnProgress
import com.recall.ui.kiln.KilnSurface
import com.recall.ui.kiln.KilnText
import com.recall.ui.kiln.arrival
import com.recall.ui.kiln.rise
import com.recall.ui.nav.BottomBarSpace
import com.recall.ui.theme.Kiln

private const val DailyGoal = 30

@Composable
fun ProgressScreen() {
    val container = (LocalContext.current.applicationContext as RecallApp).container
    val viewModel = viewModel { ProgressViewModel(container.stats) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val arrival = Modifier.arrival(state != null)
    val current = state ?: return

    val safe = WindowInsets.safeDrawing.asPaddingValues()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        Modifier.fillMaxSize().then(arrival),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = safe.calculateTopPadding() + 16.dp,
            bottom = navBottom + BottomBarSpace + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                KilnText(stringResource(R.string.progress_this_week), style = Kiln.type.label, color = Kiln.colors.ink2, uppercase = true)
                KilnText(stringResource(R.string.nav_progress), style = Kiln.type.h1, heading = true)
            }
        }
        if (!current.hasReviews) {
            item(key = "empty") {
                KilnText(
                    stringResource(R.string.progress_empty),
                    Modifier.padding(top = 8.dp),
                    style = Kiln.type.small,
                    color = Kiln.colors.ink2,
                )
            }
            return@LazyColumn
        }
        item(key = "tiles") { Tiles(current, Modifier.padding(top = 12.dp).rise(0)) }
        item(key = "week") { WeekPanel(current, Modifier.padding(top = 12.dp).rise(1)) }
        if (current.decks.isNotEmpty()) {
            item(key = "accuracy-title") {
                KilnText(
                    stringResource(R.string.progress_accuracy_title),
                    Modifier.padding(top = 16.dp).rise(2),
                    style = Kiln.type.h3,
                    heading = true,
                )
            }
            itemsIndexed(current.decks, key = { _, it -> it.deckId }) { index, deck ->
                DeckAccuracyRow(deck, Modifier.rise(index + 3))
            }
        }
    }
}

@Composable
private fun Tiles(state: ProgressStats, modifier: Modifier = Modifier) {
    Row(modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatPanel(
            value = state.streak.toString(),
            title = stringResource(R.string.progress_streak),
            caption = pluralStringResource(R.plurals.progress_best, state.bestStreak, state.bestStreak),
            withPebble = true,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        StatPanel(
            value = state.weekTotal.toString(),
            title = pluralStringResource(R.plurals.progress_reviewed, state.weekTotal),
            caption = state.firstTryPercent?.let { stringResource(R.string.progress_first_try, it) },
            withPebble = false,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

@Composable
private fun StatPanel(value: String, title: String, caption: String?, withPebble: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(26.dp)
    KilnSurface(modifier, shape = shape) {
        if (withPebble) {
            Box(Modifier.matchParentSize().clip(shape)) {
                KilnBlob(
                    BlobShape.Pebble,
                    BlobTone.Pop,
                    DpSize(72.dp, 66.dp),
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 26.dp, y = (-28).dp),
                )
            }
        }
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            KilnText(value, style = Kiln.type.display, maxLines = 1)
            KilnText(title, style = Kiln.type.bodyStrong)
            if (caption != null) {
                KilnText(caption, Modifier.padding(top = 4.dp), style = Kiln.type.caption, color = Kiln.colors.ink2)
            }
        }
    }
}

@Composable
private fun WeekPanel(state: ProgressStats, modifier: Modifier = Modifier) {
    val todayIndex = (state.today.toEpochDay() - weekStart(state.today).toEpochDay()).toInt()
    KilnSurface(modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KilnText(stringResource(R.string.progress_daily_reviews), Modifier.weight(1f), style = Kiln.type.h3, heading = true)
                KilnText(stringResource(R.string.progress_goal, DailyGoal), style = Kiln.type.caption, color = Kiln.colors.ink2)
            }
            WeekBars(state.week, todayIndex, DailyGoal)
        }
    }
}

@Composable
private fun DeckAccuracyRow(deck: DeckAccuracy, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KilnText(
                deck.name,
                Modifier.weight(1f).padding(end = 12.dp),
                style = Kiln.type.bodyStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            KilnText(stringResource(R.string.percent, deck.percent), style = Kiln.type.bodyStrong)
        }
        KilnProgress(deck.percent / 100f)
    }
}
