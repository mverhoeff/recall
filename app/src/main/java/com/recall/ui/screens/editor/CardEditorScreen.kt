package com.recall.ui.screens.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recall.R
import com.recall.RecallApp
import com.recall.data.db.CardType
import com.recall.ui.kiln.ButtonSize
import com.recall.ui.kiln.ButtonVariant
import com.recall.ui.kiln.KilnCheckbox
import com.recall.ui.kiln.KilnButton
import com.recall.ui.kiln.KilnDialog
import com.recall.ui.kiln.KilnIconButton
import com.recall.ui.kiln.KilnIcons
import com.recall.ui.kiln.KilnTabs
import com.recall.ui.kiln.KilnText
import com.recall.ui.kiln.KilnTextField
import com.recall.ui.kiln.arrival
import com.recall.ui.screens.deck.cardTypeName
import com.recall.ui.theme.Kiln

private val CardTypes = listOf(CardType.MC, CardType.TF, CardType.FLASH)

// Two large buttons share a phone's width, so the usual large padding would cut "Save, add next" short.
private val HalfWidthPadding = 12.dp

@Composable
fun CardEditorScreen(deckId: Long, cardId: Long?, onClose: () -> Unit) {
    val container = (LocalContext.current.applicationContext as RecallApp).container
    val viewModel = viewModel { CardEditorViewModel(deckId, cardId, DeckCardStore(container.decks)) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val arrival = Modifier.arrival(state != null)
    val current = state ?: return
    val draft = current.draft
    var discarding by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    val close: () -> Unit = { if (current.edited) discarding = true else onClose() }
    BackHandler(enabled = current.edited, onBack = { discarding = true })
    val scroll = rememberScrollState()
    val motion = Kiln.motion
    // A fresh card starts at the top again, where its type is picked.
    LaunchedEffect(current.saved) {
        if (current.saved) scroll.animateScrollTo(0, motion.base())
    }

    Column(
        Modifier
            .fillMaxSize()
            .then(arrival)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
    ) {
        Row(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            KilnIconButton(KilnIcons.close, stringResource(R.string.action_close), close)
            Column(Modifier.weight(1f)) {
                KilnText(
                    stringResource(if (current.original == null) R.string.editor_new else R.string.editor_edit),
                    style = Kiln.type.h3,
                )
                KilnText(
                    current.deckName,
                    style = Kiln.type.caption,
                    color = Kiln.colors.ink2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (current.original != null) {
                KilnIconButton(KilnIcons.trash, stringResource(R.string.editor_delete), { deleting = true })
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            KilnTabs(
                items = CardTypes.map { cardTypeName(it) },
                selectedIndex = CardTypes.indexOf(draft.type),
                onSelect = { viewModel.selectType(CardTypes[it]) },
            )
            KilnTextField(
                value = draft.prompt,
                onValueChange = viewModel::setPrompt,
                label = stringResource(
                    when (draft.type) {
                        CardType.MC -> R.string.editor_question
                        CardType.TF -> R.string.editor_statement
                        CardType.FLASH -> R.string.editor_front
                    },
                ),
                hint = if (draft.type == CardType.FLASH) stringResource(R.string.editor_front_hint) else null,
                error = if (current.problem == DraftProblem.PROMPT) stringResource(R.string.editor_error_prompt) else null,
                singleLine = false,
                minLines = 2,
                keyboardOptions = Sentences,
            )
            when (draft.type) {
                CardType.MC -> Answers(draft, current.problem == DraftProblem.ANSWERS, viewModel)
                CardType.TF -> Truth(draft.truth, viewModel::setTruth)
                CardType.FLASH -> KilnTextField(
                    value = draft.back,
                    onValueChange = viewModel::setBack,
                    label = stringResource(R.string.editor_back),
                    error = if (current.problem == DraftProblem.BACK) stringResource(R.string.editor_error_back) else null,
                    singleLine = false,
                    minLines = 2,
                    keyboardOptions = Sentences,
                )
            }
            KilnTextField(
                value = draft.explanation,
                onValueChange = viewModel::setExplanation,
                label = stringResource(R.string.editor_explanation),
                hint = stringResource(R.string.editor_explanation_hint),
                singleLine = false,
                minLines = 2,
                keyboardOptions = Sentences,
            )
        }

        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KilnButton(
                    stringResource(R.string.editor_save_next),
                    viewModel::saveAndAddNext,
                    Modifier.weight(1f),
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Lg,
                    horizontalPadding = HalfWidthPadding,
                )
                KilnButton(
                    stringResource(R.string.editor_save),
                    { viewModel.save(onClose) },
                    Modifier.weight(1f),
                    size = ButtonSize.Lg,
                    horizontalPadding = HalfWidthPadding,
                )
            }
            val saved by animateFloatAsState(if (current.saved) 1f else 0f, Kiln.motion.base(), label = "saved")
            KilnText(
                stringResource(R.string.editor_saved),
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = saved },
                style = Kiln.type.caption,
                color = Kiln.colors.ok,
                textAlign = TextAlign.Center,
            )
        }
    }

    if (discarding) {
        KilnDialog(
            title = stringResource(R.string.editor_discard_title),
            body = stringResource(R.string.editor_discard_body),
            confirmText = stringResource(R.string.editor_discard),
            onConfirm = {
                discarding = false
                onClose()
            },
            dismissText = stringResource(R.string.editor_keep),
            onDismiss = { discarding = false },
        )
    }
    if (deleting) {
        KilnDialog(
            title = stringResource(R.string.editor_delete_title),
            body = stringResource(R.string.editor_delete_body),
            confirmText = stringResource(R.string.action_delete),
            onConfirm = {
                deleting = false
                viewModel.delete(onClose)
            },
            dismissText = stringResource(R.string.action_cancel),
            onDismiss = { deleting = false },
            destructive = true,
        )
    }
}

private val Sentences = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)

@Composable
private fun Answers(draft: CardDraft, problem: Boolean, viewModel: CardEditorViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(stringResource(R.string.editor_answers))
        draft.options.forEachIndexed { index, option ->
            val correct = stringResource(R.string.editor_correct_answer, index + 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                KilnCheckbox(
                    checked = index in draft.correct,
                    onCheckedChange = { viewModel.toggleCorrect(index) },
                    modifier = Modifier.semantics { contentDescription = correct },
                )
                KilnTextField(
                    value = option,
                    onValueChange = { viewModel.setOption(index, it) },
                    label = null,
                    modifier = Modifier.weight(1f),
                    placeholder = stringResource(R.string.editor_answer, index + 1),
                    keyboardOptions = Sentences,
                )
                KilnIconButton(
                    KilnIcons.trash,
                    stringResource(R.string.editor_remove_answer, index + 1),
                    { viewModel.removeOption(index) },
                    variant = ButtonVariant.Ghost,
                    enabled = draft.options.size > MIN_OPTIONS,
                )
            }
        }
        if (problem) {
            KilnText(
                stringResource(R.string.editor_error_answers),
                style = Kiln.type.body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp),
                color = Kiln.colors.err,
            )
        }
        KilnButton(
            stringResource(R.string.editor_add_answer, MAX_OPTIONS),
            viewModel::addOption,
            variant = ButtonVariant.Ghost,
            size = ButtonSize.Sm,
            enabled = draft.options.size < MAX_OPTIONS,
            leadingIcon = KilnIcons.plus,
        )
    }
}

@Composable
private fun Truth(truth: Boolean, onSelect: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(stringResource(R.string.editor_statement_is))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KilnButton(
                stringResource(R.string.answer_true),
                { onSelect(true) },
                Modifier.weight(1f),
                variant = ButtonVariant.Secondary,
                pressed = truth,
            )
            KilnButton(
                stringResource(R.string.answer_false),
                { onSelect(false) },
                Modifier.weight(1f),
                variant = ButtonVariant.Secondary,
                pressed = !truth,
            )
        }
    }
}

// Matches the labels KilnTextField puts above its fields.
@Composable
private fun SectionLabel(text: String) {
    KilnText(
        text,
        style = Kiln.type.body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp),
        color = Kiln.colors.ink2,
    )
}
