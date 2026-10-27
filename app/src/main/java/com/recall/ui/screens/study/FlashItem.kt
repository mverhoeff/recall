package com.recall.ui.screens.study

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recall.R
import com.recall.data.db.Card
import com.recall.data.db.CardType
import com.recall.data.db.Grade
import com.recall.domain.session.SessionItem
import com.recall.domain.session.SessionState
import com.recall.ui.kiln.BadgeTone
import com.recall.ui.kiln.ButtonSize
import com.recall.ui.kiln.KilnBadge
import com.recall.ui.kiln.KilnButton
import com.recall.ui.kiln.KilnText
import com.recall.ui.kiln.kilnFocusRing
import com.recall.ui.kiln.kilnPressable
import com.recall.ui.kiln.neutralFill
import com.recall.ui.kiln.rememberPressState
import com.recall.ui.screens.deck.cardTypeName
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln
import com.recall.ui.theme.LocalContentColor
import com.recall.ui.theme.LocalOnGround
import com.recall.ui.theme.kilnDepth
import com.recall.ui.theme.resolve
import kotlinx.coroutines.delay

private val CardShape = RoundedCornerShape(38.dp)
private val GradeShape = RoundedCornerShape(18.dp)
private const val HINT_SEPARATOR = " · "
private const val LONG_FRONT = 60
private const val FRONT_MAX_SP = 56
private const val FRONT_MIN_SP = 32
private const val FRONT_MAX_LINES = 3
private const val FLIP_DIP_MS = 130L

// Tall enough for the grade row, so swapping in the grades doesn't resize the card above.
private val ActionsHeight = 100.dp

@Composable
fun FlashItem(
    item: SessionItem,
    state: SessionState,
    deckName: String?,
    grades: List<GradeOption>,
    onFlip: () -> Unit,
    onGrade: (Grade) -> Unit,
    modifier: Modifier = Modifier,
) {
    val flipped = state.flipped
    Column(
        modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            KilnBadge(cardTypeName(item.card.type), tone = BadgeTone.Accent)
            if (deckName != null) KilnBadge(deckName)
        }
        FlashCard(item.card, flipped, onFlip, Modifier.weight(1f).fillMaxWidth())
        Crossfade(flipped, Modifier.fillMaxWidth().height(ActionsHeight), Kiln.motion.base(), label = "actions") { back ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                if (back) {
                    GradeRow(grades, onGrade)
                } else {
                    KilnButton(stringResource(R.string.flash_show), onFlip, Modifier.fillMaxWidth(), size = ButtonSize.Lg)
                }
            }
        }
    }
}

@Composable
private fun FlashCard(card: Card, flipped: Boolean, onFlip: () -> Unit, modifier: Modifier) {
    val colors = Kiln.colors
    val motion = Kiln.motion
    var depth by remember { mutableStateOf(Depth.Mold3) }
    // The card sinks a little and comes back up as it turns over, instead of rotating.
    LaunchedEffect(flipped) {
        if (!flipped || motion.reduced) return@LaunchedEffect
        depth = Depth.Mold2
        delay(FLIP_DIP_MS)
        depth = Depth.Mold3
    }
    val resolved = depth.resolve(LocalOnGround.current)
    val (front, label) = frontAndLabel(card)
    Box(
        modifier
            .kilnDepth(resolved, CardShape, neutralFill(depth, resolved, colors.surface, colors), colors, animationSpec = motion.fast())
            .kilnPressable(
                remember { MutableInteractionSource() },
                enabled = !flipped,
                onClickLabel = stringResource(R.string.flash_show),
                onClick = onFlip,
            )
            .padding(24.dp),
    ) {
        CompositionLocalProvider(LocalOnGround provides false, LocalContentColor provides colors.ink) {
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier.weight(1.2f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                ) {
                    if (label != null) KilnText(label, style = Kiln.type.label, color = colors.ink2, uppercase = true)
                    FrontText(front)
                }
                Crossfade(flipped, Modifier.weight(1f).fillMaxWidth(), motion.base(), label = "back") { back ->
                    if (back) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                            Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                KilnText(backText(card), style = Kiln.type.h1, color = colors.accent)
                                if (card.explanation.isNotBlank()) {
                                    KilnText(card.explanation, style = Kiln.type.small, color = colors.ink2)
                                }
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                            KilnText(
                                stringResource(R.string.flash_hint),
                                style = Kiln.type.small,
                                color = colors.ink2,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The big front text, as large as fits: whole words on at most three lines, down to 32sp. */
@Composable
private fun FrontText(text: String) {
    if (text.length > LONG_FRONT) {
        KilnText(text, style = Kiln.type.h2)
        return
    }
    val display = Kiln.type.display
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = constraints.maxWidth
        val style = remember(text, width, display) {
            val fits = (FRONT_MAX_SP downTo FRONT_MIN_SP step 4).map { size ->
                display.copy(fontSize = size.sp, lineHeight = display.lineHeight * (size / FRONT_MAX_SP.toFloat()))
            }
            fits.firstOrNull { candidate ->
                text.split(' ').all { word ->
                    measurer.measure(word, candidate, softWrap = false, maxLines = 1).size.width <= width
                } &&
                    measurer.measure(text, candidate, constraints = Constraints(maxWidth = width)).lineCount <= FRONT_MAX_LINES
            } ?: fits.last()
        }
        KilnText(text, style = style)
    }
}

@Composable
private fun GradeRow(grades: List<GradeOption>, onGrade: (Grade) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        KilnText(stringResource(R.string.flash_how), style = Kiln.type.small, color = Kiln.colors.ink2)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            grades.forEach { option ->
                GradeButton(option, { onGrade(option.grade) }, Modifier.weight(1f))
            }
        }
    }
}

/** A two-line button: the grade, and when the card would come back after it. Good is the primary one. */
@Composable
private fun GradeButton(option: GradeOption, onClick: () -> Unit, modifier: Modifier) {
    val colors = Kiln.colors
    val primary = option.grade == Grade.GOOD
    val interactionSource = remember { MutableInteractionSource() }
    val press = rememberPressState(Depth.Float1, interactionSource)
    val resolved = press.depth.resolve(LocalOnGround.current)
    val fill = if (primary) colors.accent else neutralFill(press.depth, resolved, colors.surface2, colors)
    Column(
        modifier
            .kilnFocusRing(interactionSource, GradeShape)
            .graphicsLayer {
                scaleX = press.scale
                scaleY = press.scale
            }
            .kilnDepth(resolved, GradeShape, fill, colors, animationSpec = press.depthSpec, onColour = primary)
            .kilnPressable(interactionSource, onClick = onClick)
            .height(68.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        KilnText(gradeName(option.grade), style = Kiln.type.button, color = if (primary) colors.accentInk else colors.ink, maxLines = 1)
        KilnText(
            comebackText(option.comeback),
            style = Kiln.type.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            color = if (primary) colors.accentInk else colors.ink2,
            maxLines = 1,
        )
    }
}

/** "poder · preterite · yo" shows "poder" big with the rest as its label; quiz cards are labelled as questions. */
@Composable
private fun frontAndLabel(card: Card): Pair<String, String?> {
    val split = card.prompt.indexOf(HINT_SEPARATOR)
    if (split >= 0) return card.prompt.substring(0, split) to card.prompt.substring(split + HINT_SEPARATOR.length)
    val label = if (card.type == CardType.FLASH) null else stringResource(R.string.flash_question)
    return card.prompt to label
}

/** Every right answer, so a choice card with several shows them all. */
@Composable
private fun backText(card: Card): String = when (card.type) {
    CardType.TF -> stringResource(if (card.answer == "true") R.string.answer_true else R.string.answer_false)
    CardType.MC -> joinedList(listOf(card.answer) + card.alsoCorrect)
    CardType.FLASH -> card.answer
}

@Composable
private fun gradeName(grade: Grade): String = stringResource(
    when (grade) {
        Grade.AGAIN -> R.string.grade_again
        Grade.HARD -> R.string.grade_hard
        Grade.GOOD -> R.string.grade_good
        Grade.EASY -> R.string.grade_easy
    },
)

@Composable
private fun comebackText(comeback: Comeback): String = when (comeback) {
    is Comeback.Minutes ->
        if (comeback.minutes <= 1) stringResource(R.string.comeback_under_minute)
        else stringResource(R.string.comeback_minutes, comeback.minutes)
    is Comeback.Days -> pluralStringResource(R.plurals.comeback_days, comeback.days, comeback.days)
}
