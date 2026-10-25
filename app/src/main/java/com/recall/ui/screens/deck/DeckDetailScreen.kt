package com.recall.ui.screens.deck

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recall.R
import com.recall.RecallApp
import com.recall.data.db.StudyMode
import com.recall.ui.kiln.ButtonSize
import com.recall.ui.kiln.ButtonVariant
import com.recall.ui.kiln.KilnButton
import com.recall.ui.kiln.KilnChip
import com.recall.ui.kiln.KilnDialog
import com.recall.ui.kiln.KilnIconButton
import com.recall.ui.kiln.KilnIcons
import com.recall.ui.kiln.KilnProgress
import com.recall.ui.kiln.KilnSurface
import com.recall.ui.kiln.KilnTabs
import com.recall.ui.kiln.KilnText
import com.recall.ui.kiln.KilnToggle
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln

private val RoundSizes = listOf(5, 10, 20)
private val StudyModes = listOf(StudyMode.QUIZ, StudyMode.FLASHCARDS, StudyMode.DUE)

@Composable
fun deckDetailViewModel(deckId: Long): DeckDetailViewModel {
    val container = (LocalContext.current.applicationContext as RecallApp).container
    return viewModel { DeckDetailViewModel(deckId, container.decks, container.database, container.clock) }
}

@Composable
fun DeckDetailScreen(
    deckId: Long,
    onBack: () -> Unit,
    onAddCard: () -> Unit,
    onDeleted: () -> Unit,
    onStartStudy: (mode: StudyMode, size: Int, shuffle: Boolean, missedFirst: Boolean) -> Unit,
) {
    val viewModel = deckDetailViewModel(deckId)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state ?: return
    val summary = current.summary
    val context = LocalContext.current
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.export(context.contentResolver, uri)
    }

    val safe = WindowInsets.safeDrawing.asPaddingValues()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = safe.calculateTopPadding() + 12.dp,
                bottom = safe.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "bar") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KilnIconButton(KilnIcons.back, stringResource(R.string.action_back), onBack)
                    Spacer(Modifier.weight(1f))
                    KilnIconButton(KilnIcons.plus, stringResource(R.string.deck_add_card), onAddCard)
                    KilnIconButton(KilnIcons.more, stringResource(R.string.deck_more), { menuOpen = true })
                }
            }
            item(key = "title") {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    KilnText(
                        stringResource(R.string.deck_label, pluralStringResource(R.plurals.card_count, summary.cardCount, summary.cardCount)),
                        style = Kiln.type.label,
                        color = Kiln.colors.ink2,
                        uppercase = true,
                    )
                    KilnText(summary.deck.name, style = Kiln.type.h1)
                    if (summary.deck.description.isNotBlank()) {
                        KilnText(summary.deck.description, style = Kiln.type.small, color = Kiln.colors.ink2)
                    }
                }
            }
            item(key = "stats") {
                val none = stringResource(R.string.stat_none)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(summary.dueCount.toString(), stringResource(R.string.stat_due), Modifier.weight(1f))
                    StatTile(
                        if (summary.cardCount == 0) none else stringResource(R.string.percent, summary.knownCount * 100 / summary.cardCount),
                        stringResource(R.string.stat_known),
                        Modifier.weight(1f),
                    )
                    StatTile(
                        current.accuracy?.let { stringResource(R.string.percent, it) } ?: none,
                        stringResource(R.string.stat_accuracy),
                        Modifier.weight(1f),
                    )
                }
            }
            item(key = "progress") {
                Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KilnProgress(if (summary.cardCount == 0) 0f else summary.knownCount / summary.cardCount.toFloat())
                    KilnText(
                        stringResource(R.string.deck_progress_caption, summary.knownCount, summary.learningCount, summary.newCount),
                        style = Kiln.type.caption,
                        color = Kiln.colors.ink2,
                    )
                }
            }
            item(key = "panel") {
                StudyPanelCard(current, viewModel, onStartStudy, Modifier.padding(top = 8.dp))
            }
        }
        // The sheet is dropped at once when an action opens a dialog or the file picker, which covers it anyway.
        DeckMenuSheet(
            visible = menuOpen,
            onDismiss = { menuOpen = false },
            onRename = { menuOpen = false; renaming = true },
            onExport = { menuOpen = false; exporter.launch(deckFileName(summary.deck.name)) },
            onDelete = { menuOpen = false; deleting = true },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (renaming) {
        RenameDeckDialog(
            name = summary.deck.name,
            onDismiss = { renaming = false },
            onRename = { renaming = false; viewModel.rename(it) },
        )
    }
    if (deleting) {
        KilnDialog(
            title = stringResource(R.string.deck_delete_title),
            body = if (summary.cardCount == 0) {
                stringResource(R.string.deck_delete_body_empty)
            } else {
                pluralStringResource(R.plurals.deck_delete_body, summary.cardCount, summary.cardCount)
            },
            confirmText = stringResource(R.string.action_delete),
            onConfirm = { deleting = false; viewModel.delete(onDeleted) },
            dismissText = stringResource(R.string.action_cancel),
            onDismiss = { deleting = false },
            destructive = true,
        )
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    KilnSurface(modifier, depth = Depth.Mold1, shape = RoundedCornerShape(18.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            KilnText(value, style = Kiln.type.h2, maxLines = 1)
            KilnText(label, style = Kiln.type.label, color = Kiln.colors.ink2, maxLines = 1)
        }
    }
}

@Composable
private fun StudyPanelCard(
    state: DeckDetailUiState,
    viewModel: DeckDetailViewModel,
    onStartStudy: (StudyMode, Int, Boolean, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val panel = state.panel
    val cardCount = state.summary.cardCount
    val dueCount = state.summary.dueCount
    KilnSurface(modifier.fillMaxWidth(), depth = Depth.Mold2, shape = RoundedCornerShape(26.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            KilnTabs(
                items = listOf(
                    stringResource(R.string.study_tab_quiz),
                    stringResource(R.string.study_tab_flashcards),
                    stringResource(R.string.study_tab_due),
                ),
                selectedIndex = StudyModes.indexOf(panel.mode),
                onSelect = { viewModel.selectMode(StudyModes[it]) },
            )
            if (panel.mode != StudyMode.DUE && cardCount > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KilnText(stringResource(R.string.study_round_size), style = Kiln.type.small, color = Kiln.colors.ink2)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RoundSizes.filter { it <= cardCount }.forEach { size ->
                            KilnChip(size.toString(), selected = state.roundSize == size, onClick = { viewModel.selectSize(size) })
                        }
                        KilnChip(
                            stringResource(R.string.study_size_all, cardCount),
                            selected = state.roundSize == null,
                            onClick = { viewModel.selectSize(null) },
                        )
                    }
                }
            }
            if (panel.mode == StudyMode.QUIZ) {
                KilnToggle(
                    checked = panel.shuffle,
                    onCheckedChange = viewModel::setShuffle,
                    label = stringResource(R.string.study_shuffle),
                    sublabel = stringResource(R.string.study_shuffle_sub),
                )
            }
            if (panel.mode != StudyMode.DUE && state.missedCount > 0) {
                KilnToggle(
                    checked = panel.missedFirst,
                    onCheckedChange = viewModel::setMissedFirst,
                    label = stringResource(R.string.study_missed_first),
                    sublabel = pluralStringResource(R.plurals.study_missed_first_sub, state.missedCount, state.missedCount),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KilnButton(
                    text = when (panel.mode) {
                        StudyMode.QUIZ -> stringResource(R.string.study_start_quiz)
                        StudyMode.FLASHCARDS -> stringResource(R.string.study_start_flashcards)
                        StudyMode.DUE -> stringResource(R.string.study_review_due, dueCount)
                    },
                    onClick = {
                        onStartStudy(panel.mode, state.roundSize ?: cardCount, panel.shuffle, panel.missedFirst && state.missedCount > 0)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    size = ButtonSize.Lg,
                    enabled = if (panel.mode == StudyMode.DUE) dueCount > 0 else cardCount > 0,
                )
                if (panel.mode == StudyMode.DUE && dueCount == 0) {
                    KilnText(
                        stringResource(R.string.study_nothing_due),
                        Modifier.fillMaxWidth(),
                        style = Kiln.type.caption,
                        color = Kiln.colors.ink2,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
