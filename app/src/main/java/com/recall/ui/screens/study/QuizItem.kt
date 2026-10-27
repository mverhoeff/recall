package com.recall.ui.screens.study

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recall.R
import com.recall.data.db.CardType
import com.recall.domain.session.SessionItem
import com.recall.domain.session.SessionState
import com.recall.ui.kiln.BadgeTone
import com.recall.ui.kiln.BlobShape
import com.recall.ui.kiln.BlobTone
import com.recall.ui.kiln.ButtonSize
import com.recall.ui.kiln.KilnBadge
import com.recall.ui.kiln.KilnBlob
import com.recall.ui.kiln.KilnButton
import com.recall.ui.kiln.KilnIcon
import com.recall.ui.kiln.KilnIcons
import com.recall.ui.kiln.KilnSurface
import com.recall.ui.kiln.KilnText
import com.recall.ui.kiln.KilnTile
import com.recall.ui.kiln.TileSize
import com.recall.ui.kiln.TileTone
import com.recall.ui.kiln.kilnFocusRing
import com.recall.ui.kiln.kilnPressable
import com.recall.ui.kiln.neutralFill
import com.recall.ui.kiln.rememberPressState
import com.recall.ui.screens.deck.cardTypeName
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln
import com.recall.ui.theme.LocalOnGround
import com.recall.ui.theme.kilnDepth
import com.recall.ui.theme.resolve

private val CardShape = RoundedCornerShape(38.dp)
private val ChoiceShape = RoundedCornerShape(18.dp)
private val TruthShape = RoundedCornerShape(26.dp)
private val LetterShape = RoundedCornerShape(13.dp)

/** How one option looks: picked or not before the check, and what it turned out to be after. */
private enum class Look { Idle, Picked, Right, Wrong, Dimmed }

private fun look(index: Int, item: SessionItem, state: SessionState): Look {
    val picked = index in state.selected
    return when {
        !state.checked -> if (picked) Look.Picked else Look.Idle
        index in item.correct -> Look.Right
        picked -> Look.Wrong
        else -> Look.Dimmed
    }
}

@Composable
fun QuizItem(
    item: SessionItem,
    state: SessionState,
    deckName: String?,
    onSelect: (Int) -> Unit,
    onCheck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuestionCard(item, deckName)
            Column(
                Modifier
                    .padding(top = 8.dp)
                    .then(if (item.multiple) Modifier else Modifier.selectableGroup()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (item.card.type == CardType.TF) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(true, false).forEachIndexed { index, truth ->
                            TruthOption(
                                truth,
                                look(index, item, state),
                                picked = index in state.selected,
                                onClick = { onSelect(index) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                } else {
                    item.options.forEachIndexed { index, option ->
                        ChoiceOption(
                            letter = ('A' + index).toString(),
                            text = option,
                            look = look(index, item, state),
                            picked = index in state.selected,
                            multiple = item.multiple,
                            onClick = { onSelect(index) },
                        )
                    }
                }
            }
        }
        KilnButton(
            stringResource(R.string.quiz_check),
            onCheck,
            Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxWidth(),
            size = ButtonSize.Lg,
            enabled = state.selected.isNotEmpty() && !state.checked,
        )
    }
}

@Composable
private fun QuestionCard(item: SessionItem, deckName: String?) {
    KilnSurface(Modifier.fillMaxWidth(), depth = Depth.Mold2, shape = CardShape) {
        Box(Modifier.matchParentSize().clip(CardShape)) {
            KilnBlob(
                BlobShape.Pebble,
                BlobTone.Soft,
                DpSize(120.dp, 108.dp),
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = (-44).dp),
            )
        }
        Column(
            Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Both lines leave room for the pebble in the corner.
            // A long deck name moves to a line of its own instead of being cut short.
            FlowRow(
                Modifier.padding(end = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                KilnBadge(cardTypeName(item.card.type), tone = BadgeTone.Accent)
                if (deckName != null) KilnBadge(deckName)
            }
            KilnText(item.card.prompt, Modifier.padding(end = 28.dp), style = Kiln.type.h2)
            if (item.multiple) {
                KilnText(stringResource(R.string.quiz_pick_all), style = Kiln.type.small, color = Kiln.colors.ink2)
            }
        }
    }
}

@Composable
private fun ChoiceOption(letter: String, text: String, look: Look, picked: Boolean, multiple: Boolean, onClick: () -> Unit) {
    val colors = Kiln.colors
    val textColor = when (look) {
        Look.Picked -> colors.accent
        Look.Right -> colors.ok
        Look.Wrong -> colors.err
        Look.Idle, Look.Dimmed -> colors.ink
    }
    OptionFrame(look, picked, multiple, onClick, ChoiceShape, Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp)) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LetterTile(letter, look)
            KilnText(
                text,
                Modifier.weight(1f),
                style = Kiln.type.body.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                color = animateColorAsState(textColor, Kiln.motion.base(), label = "option").value,
            )
        }
    }
}

@Composable
private fun LetterTile(letter: String, look: Look) {
    val colors = Kiln.colors
    when (look) {
        Look.Right -> KilnTile(Modifier.size(40.dp), TileTone.Ok, TileSize.Sm, icon = KilnIcons.checkStrong)
        Look.Wrong -> KilnTile(Modifier.size(40.dp), TileTone.Err, TileSize.Sm, icon = KilnIcons.closeStrong)
        else -> {
            val picked = look == Look.Picked
            Box(
                Modifier
                    .size(40.dp)
                    .kilnDepth(
                        if (picked) Depth.Float1 else Depth.Flat,
                        LetterShape,
                        if (picked) colors.accent else colors.accentSoft,
                        colors,
                        animationSpec = Kiln.motion.base(),
                        onColour = picked,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                KilnText(
                    letter,
                    style = Kiln.type.h3.copy(fontSize = 16.sp, lineHeight = 16.sp),
                    color = if (picked) colors.accentInk else colors.ink,
                )
            }
        }
    }
}

@Composable
private fun TruthOption(truth: Boolean, look: Look, picked: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = Kiln.colors
    val tone = when (look) {
        Look.Picked -> TileTone.Accent
        Look.Right -> TileTone.Ok
        Look.Wrong -> TileTone.Err
        Look.Idle, Look.Dimmed -> TileTone.Soft
    }
    val textColor = when (look) {
        Look.Picked -> colors.accent
        Look.Right -> colors.ok
        Look.Wrong -> colors.err
        Look.Idle, Look.Dimmed -> colors.ink
    }
    OptionFrame(look, picked, multiple = false, onClick, TruthShape, modifier.height(140.dp)) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KilnTile(tone = tone, size = TileSize.Md, icon = if (truth) KilnIcons.checkStrong else KilnIcons.closeStrong)
            KilnText(
                stringResource(if (truth) R.string.answer_true else R.string.answer_false),
                style = Kiln.type.body.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                color = textColor,
            )
        }
    }
}

/**
 * A Secondary-button body in any shape: it floats until picked, then stays pressed in. Single
 * answers read as radio buttons, several as checkboxes.
 */
@Composable
private fun OptionFrame(
    look: Look,
    picked: Boolean,
    multiple: Boolean,
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = Kiln.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressedIn = look == Look.Picked || look == Look.Right || look == Look.Wrong
    val press = rememberPressState(Depth.Float1, interactionSource, forcePressed = pressedIn)
    val resolved = press.depth.resolve(LocalOnGround.current)
    val alpha by animateFloatAsState(if (look == Look.Dimmed) 0.55f else 1f, Kiln.motion.base(), label = "dim")
    val checked = look != Look.Idle && look != Look.Picked
    Box(
        modifier
            .alpha(alpha)
            .kilnFocusRing(interactionSource, shape)
            .kilnDepth(resolved, shape, neutralFill(press.depth, resolved, colors.surface2, colors), colors, animationSpec = press.depthSpec)
            .semantics {
                if (multiple) toggleableState = ToggleableState(picked) else selected = picked
            }
            .kilnPressable(
                interactionSource,
                enabled = !checked,
                role = if (multiple) Role.Checkbox else Role.RadioButton,
                onClick = onClick,
            ),
    ) {
        content()
    }
}
