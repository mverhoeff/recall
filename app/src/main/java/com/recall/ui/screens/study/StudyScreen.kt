package com.recall.ui.screens.study

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recall.R
import com.recall.RecallApp
import com.recall.data.db.CardType
import com.recall.domain.session.Presentation
import com.recall.domain.session.SessionItem
import com.recall.ui.kiln.ButtonSize
import com.recall.ui.kiln.KilnButton
import com.recall.ui.kiln.KilnDialog
import com.recall.ui.kiln.KilnIconButton
import com.recall.ui.kiln.KilnIcons
import com.recall.ui.kiln.KilnProgress
import com.recall.ui.kiln.KilnSheet
import com.recall.ui.kiln.KilnText
import com.recall.ui.kiln.KilnTile
import com.recall.ui.kiln.TileSize
import com.recall.ui.kiln.TileTone
import com.recall.ui.kiln.arrival
import com.recall.ui.nav.Study
import com.recall.ui.theme.Kiln

private const val ENTER_SCALE = 0.985f

@Composable
fun StudyScreen(route: Study, onClose: () -> Unit, onFinished: (String) -> Unit) {
    val container = (LocalContext.current.applicationContext as RecallApp).container
    val viewModel = viewModel { StudyViewModel(route, container) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val empty by viewModel.empty.collectAsStateWithLifecycle()
    LaunchedEffect(empty) { if (empty) onClose() }
    val arrival = Modifier.arrival(state != null)
    val current = state ?: return
    LaunchedEffect(current.resultKey) { current.resultKey?.let(onFinished) }

    val session = current.session
    val view = LocalView.current
    val motion = Kiln.motion
    var ending by rememberSaveable { mutableStateOf(false) }
    val close: () -> Unit = { if (current.answered > 0) ending = true else onClose() }

    Box(Modifier.fillMaxSize().then(arrival)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                KilnIconButton(KilnIcons.close, stringResource(R.string.action_close), close)
                KilnProgress(current.answered.toFloat() / current.total, Modifier.weight(1f))
                KilnText(
                    stringResource(R.string.quiz_counter, session.index + 1, current.total),
                    style = Kiln.type.bodyStrong,
                )
            }
            // Each question rises in as the last one fades out.
            AnimatedContent(
                targetState = session,
                contentKey = { it.index },
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (fadeIn(motion.slow()) + scaleIn(motion.slow(), initialScale = ENTER_SCALE)) togetherWith
                        fadeOut(motion.fast())
                },
                label = "item",
            ) { shown ->
                // Each item draws from its own snapshot, so one fading out still shows how it was answered.
                val item = shown.current ?: return@AnimatedContent
                when (item.presentation) {
                    Presentation.QUIZ -> QuizItem(
                        item = item,
                        state = shown,
                        deckName = current.deckNames[item.card.deckId],
                        onSelect = viewModel::select,
                        onCheck = {
                            val correct = viewModel.check()
                            if (correct != null && current.haptics) view.answerHaptic(correct)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    Presentation.FLASH -> FlashItem(
                        item = item,
                        state = shown,
                        deckName = current.deckNames[item.card.deckId],
                        grades = remember(item.card.id) { viewModel.previews(item.card) },
                        onFlip = viewModel::flip,
                        onGrade = viewModel::grade,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        val item = session.current
        val correct = session.lastCorrect
        KilnSheet(
            visible = item != null && correct != null && !session.finished,
            onDismiss = viewModel::next,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            if (item != null && correct != null) Feedback(item, correct, session.isLast, viewModel::next)
        }
    }

    // Registered after the sheet's own Back handling, so Back always means closing the round.
    key(session.index, session.checked) {
        BackHandler(onBack = close)
    }
    if (ending) {
        KilnDialog(
            title = stringResource(R.string.quiz_end_title),
            body = stringResource(R.string.quiz_end_body),
            confirmText = stringResource(R.string.quiz_end),
            onConfirm = {
                ending = false
                onClose()
            },
            dismissText = stringResource(R.string.quiz_keep),
            onDismiss = { ending = false },
        )
    }
}

@Composable
private fun Feedback(item: SessionItem, correct: Boolean, last: Boolean, onNext: () -> Unit) {
    val colors = Kiln.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        KilnTile(
            tone = if (correct) TileTone.Ok else TileTone.Err,
            size = TileSize.Sm,
            icon = if (correct) KilnIcons.checkStrong else KilnIcons.closeStrong,
        )
        KilnText(
            if (correct) stringResource(R.string.quiz_right) else stringResource(R.string.quiz_wrong, rightAnswers(item)),
            Modifier.weight(1f),
            style = Kiln.type.h3,
            color = if (correct) colors.ok else colors.err,
        )
    }
    if (item.card.explanation.isNotBlank()) {
        KilnText(item.card.explanation, style = Kiln.type.small, color = colors.ink2)
    }
    KilnButton(
        stringResource(if (last) R.string.quiz_see_results else R.string.quiz_next),
        onNext,
        Modifier.fillMaxWidth(),
        size = ButtonSize.Lg,
        trailingIcon = if (last) null else KilnIcons.forward,
    )
}

/** "tengo", "async and launch", or "a, b and c"; true/false answers as plain words. */
@Composable
private fun rightAnswers(item: SessionItem): String {
    if (item.card.type == CardType.TF) {
        return stringResource(if (item.card.answer == "true") R.string.quiz_true_word else R.string.quiz_false_word)
    }
    return joinedList(item.correct.sorted().map { item.options[it] })
}

@Composable
internal fun joinedList(items: List<String>): String {
    if (items.size == 1) return items.single()
    val head = items.dropLast(1).reduce { joined, next -> stringResource(R.string.list_comma, joined, next) }
    return stringResource(R.string.list_and, head, items.last())
}
