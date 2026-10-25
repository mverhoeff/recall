package com.recall.ui.screens.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recall.R
import com.recall.RecallApp
import com.recall.domain.session.SessionResult
import com.recall.ui.kiln.BlobShape
import com.recall.ui.kiln.BlobTone
import com.recall.ui.kiln.ButtonSize
import com.recall.ui.kiln.KilnBlob
import com.recall.ui.kiln.KilnButton
import com.recall.ui.kiln.KilnIconButton
import com.recall.ui.kiln.KilnIcons
import com.recall.ui.kiln.KilnText
import com.recall.ui.kiln.arrival
import com.recall.ui.kiln.squish
import com.recall.ui.screens.deck.StatTile
import com.recall.ui.theme.Kiln

private const val GOOD_ACCURACY = 70
private const val FAIR_ACCURACY = 40

// The pebble lands once the ring has started filling.
private const val SQUISH_DELAY_MS = 200L

@Composable
fun ResultsScreen(sessionKey: String, onBack: () -> Unit, onMissing: () -> Unit) {
    val container = (LocalContext.current.applicationContext as RecallApp).container
    val viewModel = viewModel { ResultsViewModel(sessionKey, container) }
    if (viewModel.result == null) {
        LaunchedEffect(Unit) { onMissing() }
        return
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val arrival = Modifier.arrival(state != null)
    val current = state ?: return
    val result = current.result

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .then(arrival),
    ) {
        Row(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            KilnIconButton(KilnIcons.close, stringResource(R.string.action_close), onBack)
            KilnText(stringResource(R.string.results_label), style = Kiln.type.label, color = Kiln.colors.ink2, uppercase = true)
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Score(result)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KilnText(title(result.accuracy), style = Kiln.type.h1, textAlign = TextAlign.Center)
                KilnText(body(result), style = Kiln.type.small, color = Kiln.colors.ink2, textAlign = TextAlign.Center)
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(stringResource(R.string.percent, result.accuracy), stringResource(R.string.stat_accuracy), Modifier.weight(1f))
                StatTile(elapsed(result.elapsedMillis), stringResource(R.string.results_time), Modifier.weight(1f))
                StatTile(
                    pluralStringResource(R.plurals.results_streak_days, current.streak, current.streak),
                    stringResource(R.string.results_streak),
                    Modifier.weight(1f),
                )
            }
        }
        KilnButton(
            stringResource(R.string.results_back),
            onBack,
            Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxWidth(),
            size = ButtonSize.Lg,
        )
    }
}

@Composable
private fun Score(result: SessionResult) {
    Box(Modifier.size(width = 290.dp, height = 220.dp), contentAlignment = Alignment.Center) {
        KilnBlob(
            BlobShape.Coil,
            BlobTone.Soft,
            DpSize(44.dp, 44.dp),
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = 12.dp, y = (-8).dp),
        )
        ScoreRing(result.correct, result.total)
        KilnBlob(
            BlobShape.Pebble,
            BlobTone.Pop,
            DpSize(64.dp, 56.dp),
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-4).dp, y = (-2).dp)
                .squish(SQUISH_DELAY_MS),
        )
    }
}

@Composable
private fun title(accuracy: Int): String = stringResource(
    when {
        accuracy == 100 -> R.string.results_perfect
        accuracy >= GOOD_ACCURACY -> R.string.results_nicely_done
        accuracy >= FAIR_ACCURACY -> R.string.results_getting_there
        else -> R.string.results_keep_going
    },
)

@Composable
private fun body(result: SessionResult): String {
    val missed = result.total - result.correct
    if (missed == 0) return stringResource(R.string.results_body_perfect)
    return pluralStringResource(R.plurals.results_body, missed, result.correct, result.total, missed)
}

@Composable
private fun elapsed(millis: Long): String {
    val seconds = millis / 1000
    return stringResource(R.string.results_elapsed, seconds / 60, seconds % 60)
}
