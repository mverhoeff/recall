package com.recall.ui.screens.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.recall.data.repo.DeckSummary
import com.recall.ui.kiln.BadgeTone
import com.recall.ui.kiln.BlobShape
import com.recall.ui.kiln.BlobTone
import com.recall.ui.kiln.ButtonSize
import com.recall.ui.kiln.ButtonVariant
import com.recall.ui.kiln.KilnBadge
import com.recall.ui.kiln.KilnBlob
import com.recall.ui.kiln.KilnButton
import com.recall.ui.kiln.KilnChip
import com.recall.ui.kiln.KilnIcon
import com.recall.ui.kiln.KilnIconButton
import com.recall.ui.kiln.KilnIcons
import com.recall.ui.kiln.KilnSurface
import com.recall.ui.kiln.KilnText
import com.recall.ui.kiln.KilnTextField
import com.recall.ui.kiln.KilnTile
import com.recall.ui.kiln.SurfaceTone
import com.recall.ui.kiln.TileSize
import com.recall.ui.kiln.TileTone
import com.recall.ui.kiln.arrival
import com.recall.ui.kiln.rise
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// The floating bottom bar: 52dp items, 6dp padding and its 20dp margin.
private val BottomBarSpace = 84.dp

// The UI is English, so the date is too, whatever the phone's language.
private val HeaderDate = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)

@Composable
fun LibraryScreen(onOpenDeck: (Long) -> Unit, onImport: () -> Unit, onStartReview: () -> Unit) {
    val container = (LocalContext.current.applicationContext as RecallApp).container
    val viewModel = viewModel { LibraryViewModel(container.decks, container.database.reviewDao(), container.clock) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val arrival = Modifier.arrival(state != null)
    val current = state ?: return
    var creating by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = current.query != null) { viewModel.closeSearch() }

    val safe = WindowInsets.safeDrawing.asPaddingValues()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(Modifier.fillMaxSize().then(arrival)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = safe.calculateTopPadding() + 16.dp,
                bottom = navBottom + BottomBarSpace + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                Header(
                    today = current.today,
                    query = current.query,
                    showSearch = current.hasDecks,
                    onOpenSearch = viewModel::openSearch,
                    onCloseSearch = viewModel::closeSearch,
                    onQueryChange = viewModel::search,
                    onImport = onImport,
                )
            }
            if (current.hasDecks) {
                if (current.query == null) {
                    item(key = "hero") { Hero(current, onStartReview, Modifier.padding(top = 12.dp)) }
                }
                item(key = "decks-title") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        KilnText(stringResource(R.string.library_your_decks), Modifier.weight(1f), style = Kiln.type.h3)
                        KilnChip(
                            text = stringResource(if (current.sort == DeckSort.DUE_FIRST) R.string.library_sort_due else R.string.library_sort_az),
                            selected = false,
                            onClick = viewModel::toggleSort,
                        )
                    }
                }
                itemsIndexed(current.decks, key = { _, it -> it.deck.id }) { index, summary ->
                    DeckRow(summary, onClick = { viewModel.openDeck(summary.deck.id, onOpenDeck) }, Modifier.rise(index))
                }
            } else {
                item(key = "empty") {
                    EmptyLibrary(onImport = onImport, onNewDeck = { creating = true }, Modifier.fillParentMaxHeight(0.7f))
                }
            }
        }
        if (current.hasDecks && current.query == null) {
            KilnButton(
                stringResource(R.string.library_new_deck),
                onClick = { creating = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = navBottom + BottomBarSpace + 16.dp),
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Lg,
                leadingIcon = KilnIcons.plus,
                restDepth = Depth.Float3,
            )
        }
    }

    if (creating) {
        NewDeckDialog(
            onDismiss = { creating = false },
            onCreate = { name, description ->
                creating = false
                viewModel.createDeck(name, description, onOpenDeck)
            },
        )
    }
}

@Composable
private fun EmptyLibrary(onImport: () -> Unit, onNewDeck: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(220.dp, 150.dp)) {
            KilnBlob(BlobShape.Coil, BlobTone.Soft, DpSize(170.dp, 60.dp), Modifier.align(Alignment.BottomStart))
            KilnBlob(BlobShape.Pebble, BlobTone.Pop, DpSize(110.dp, 100.dp), Modifier.align(Alignment.TopEnd))
        }
        KilnText(stringResource(R.string.library_empty_title), Modifier.padding(top = 32.dp), style = Kiln.type.h2)
        KilnText(
            stringResource(R.string.library_empty_body),
            Modifier.padding(top = 8.dp),
            style = Kiln.type.small,
            color = Kiln.colors.ink2,
            textAlign = TextAlign.Center,
        )
        Row(Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KilnButton(stringResource(R.string.library_import_file), onImport, leadingIcon = KilnIcons.importIcon)
            KilnButton(stringResource(R.string.library_new_deck), onNewDeck, variant = ButtonVariant.Secondary, leadingIcon = KilnIcons.plus)
        }
    }
}

@Composable
private fun Header(
    today: LocalDate,
    query: String?,
    showSearch: Boolean,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onImport: () -> Unit,
) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (query != null) {
            val focus = remember { FocusRequester() }
            KilnTextField(
                value = query,
                onValueChange = onQueryChange,
                label = stringResource(R.string.library_search_label),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus),
            )
            LaunchedEffect(Unit) { focus.requestFocus() }
        } else {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                KilnText(today.format(HeaderDate), style = Kiln.type.label, color = Kiln.colors.ink2, uppercase = true)
                KilnText(stringResource(R.string.nav_library), style = Kiln.type.h1)
            }
        }
        if (query != null) {
            KilnIconButton(KilnIcons.close, stringResource(R.string.library_close_search), onCloseSearch)
        } else if (showSearch) {
            KilnIconButton(KilnIcons.search, stringResource(R.string.library_search), onOpenSearch)
        }
        KilnIconButton(KilnIcons.importIcon, stringResource(R.string.library_import), onImport)
    }
}

@Composable
private fun Hero(state: LibraryUiState, onStartReview: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(38.dp)
    KilnSurface(modifier.fillMaxWidth(), depth = Depth.Mold3, tone = SurfaceTone.Accent, shape = shape) {
        Box(Modifier.matchParentSize().clip(shape)) {
            KilnBlob(
                BlobShape.Pebble,
                BlobTone.Pop,
                DpSize(132.dp, 120.dp),
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 44.dp, y = (-48).dp),
            )
        }
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            KilnText(stringResource(R.string.library_due_today), style = Kiln.type.label, uppercase = true)
            if (state.dueTotal > 0) {
                KilnText(pluralStringResource(R.plurals.card_count, state.dueTotal, state.dueTotal), style = Kiln.type.display)
                KilnText(
                    stringResource(
                        R.string.library_hero_detail,
                        pluralStringResource(R.plurals.library_from_decks, state.dueDeckCount, state.dueDeckCount),
                        pluralStringResource(R.plurals.library_minutes, state.dueMinutes, state.dueMinutes),
                    ),
                    style = Kiln.type.small,
                )
            } else {
                KilnText(stringResource(R.string.library_all_done), style = Kiln.type.display)
                KilnText(stringResource(R.string.library_nothing_due), style = Kiln.type.small)
            }
            if (state.dueTotal > 0 || state.streak > 0) {
                Row(
                    Modifier.padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (state.dueTotal > 0) {
                        KilnButton(
                            stringResource(R.string.library_start_review),
                            onStartReview,
                            variant = ButtonVariant.Secondary,
                            trailingIcon = KilnIcons.forward,
                        )
                    }
                    if (state.streak > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            KilnIcon(KilnIcons.star, contentDescription = null, size = 20.dp)
                            KilnText(stringResource(R.string.library_streak, state.streak), style = Kiln.type.bodyStrong)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckRow(summary: DeckSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    KilnSurface(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp),
        depth = Depth.Mold1,
        shape = RoundedCornerShape(18.dp),
        onClick = onClick,
    ) {
        Row(
            Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KilnTile(tone = TileTone.Soft, size = TileSize.Sm, initials = initials(summary.deck.name))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                KilnText(summary.deck.name, style = Kiln.type.bodyStrong, maxLines = 2)
                KilnText(deckCaption(summary), style = Kiln.type.caption, color = Kiln.colors.ink2)
            }
            Spacer(Modifier.width(4.dp))
            if (summary.dueCount > 0) {
                KilnBadge(stringResource(R.string.library_badge_due, summary.dueCount), tone = BadgeTone.Accent)
            } else {
                KilnBadge(stringResource(R.string.library_up_to_date), tone = BadgeTone.Ok)
            }
        }
    }
}

@Composable
private fun deckCaption(summary: DeckSummary): String {
    val cards = pluralStringResource(R.plurals.card_count, summary.cardCount, summary.cardCount)
    return when {
        summary.cardCount == 0 -> cards
        summary.knownCount == summary.cardCount -> stringResource(R.string.library_deck_all_known, cards)
        else -> stringResource(R.string.library_deck_known, cards, summary.knownCount * 100 / summary.cardCount)
    }
}

/** First letters of the first two words, e.g. "Spanish verbs — A2" → "SV". */
internal fun initials(name: String): String = name
    .split(Regex("\\s+"))
    .filter { word -> word.firstOrNull()?.isLetterOrDigit() == true }
    .take(2)
    .joinToString("") { it.first().uppercase() }
