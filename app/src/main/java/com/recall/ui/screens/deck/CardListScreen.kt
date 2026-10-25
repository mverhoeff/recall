package com.recall.ui.screens.deck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.recall.R
import com.recall.ui.kiln.KilnIconButton
import com.recall.ui.kiln.KilnIcons
import com.recall.ui.kiln.KilnText
import com.recall.ui.kiln.arrival
import com.recall.ui.kiln.rise
import com.recall.ui.theme.Kiln

@Composable
fun CardListScreen(deckId: Long, onBack: () -> Unit, onOpenCard: (Long) -> Unit) {
    val viewModel = deckDetailViewModel(deckId)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val arrival = Modifier.arrival(state != null)
    val current = state ?: return

    val safe = WindowInsets.safeDrawing.asPaddingValues()
    LazyColumn(
        Modifier
            .fillMaxSize()
            .then(arrival),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = safe.calculateTopPadding() + 12.dp,
            bottom = safe.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "bar") {
            Row(
                Modifier.padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                KilnIconButton(KilnIcons.back, stringResource(R.string.action_back), onBack)
                Column(Modifier.weight(1f)) {
                    KilnText(stringResource(R.string.deck_cards), style = Kiln.type.h3)
                    KilnText(
                        stringResource(
                            R.string.card_list_caption,
                            pluralStringResource(R.plurals.card_count, current.cards.size, current.cards.size),
                            current.summary.deck.name,
                        ),
                        style = Kiln.type.caption,
                        color = Kiln.colors.ink2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        itemsIndexed(current.cards, key = { _, it -> it.card.id }) { index, row ->
            CardListRow(row, onClick = { onOpenCard(row.card.id) }, Modifier.rise(index))
        }
    }
}
