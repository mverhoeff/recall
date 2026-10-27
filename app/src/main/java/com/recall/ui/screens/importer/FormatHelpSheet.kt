package com.recall.ui.screens.importer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.recall.R
import com.recall.ui.kiln.KilnSheet
import com.recall.ui.kiln.KilnSurface
import com.recall.ui.kiln.KilnText
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln

// Leaves the top of the screen clear; a long sheet scrolls instead.
private const val MAX_SHARE = 0.8f

@Composable
fun FormatHelpSheet(visible: Boolean, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * MAX_SHARE
    KilnSheet(visible, onDismiss, modifier) {
        Column(
            Modifier
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HelpContent()
        }
    }
}

@Composable
private fun ColumnScope.HelpContent() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        KilnText(stringResource(R.string.help_title), style = Kiln.type.h3, heading = true)
        KilnText(stringResource(R.string.help_intro), style = Kiln.type.small, color = Kiln.colors.ink2)
    }

    ColumnGroup(
        stringResource(R.string.help_required),
        listOf(
            R.string.help_col_type to R.string.help_col_type_desc,
            R.string.help_col_prompt to R.string.help_col_prompt_desc,
            R.string.help_col_answer to R.string.help_col_answer_desc,
        ),
    )
    ColumnGroup(
        stringResource(R.string.help_optional),
        listOf(
            R.string.help_col_wrong to R.string.help_col_wrong_desc,
            R.string.help_col_more to R.string.help_col_more_desc,
            R.string.help_col_explanation to R.string.help_col_explanation_desc,
        ),
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GroupLabel(stringResource(R.string.help_examples))
        KilnSurface(Modifier.fillMaxWidth(), depth = Depth.Dip, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                KilnText(csvLine(stringResource(R.string.help_example_header)), style = Kiln.type.small.copy(fontWeight = FontWeight.Bold))
                KilnText(csvLine(stringResource(R.string.help_example_mc)), style = Kiln.type.small)
                KilnText(csvLine(stringResource(R.string.help_example_tf)), style = Kiln.type.small)
                KilnText(csvLine(stringResource(R.string.help_example_card)), style = Kiln.type.small)
            }
        }
    }

    KilnText(stringResource(R.string.help_json), style = Kiln.type.small, color = Kiln.colors.ink2)
}

@Composable
private fun ColumnGroup(label: String, columns: List<Pair<Int, Int>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GroupLabel(label)
        KilnSurface(Modifier.fillMaxWidth(), depth = Depth.Dip, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                columns.forEachIndexed { index, (name, description) ->
                    if (index > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Kiln.colors.line),
                        )
                    }
                    Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        KilnText(stringResource(name), style = Kiln.type.small.copy(fontWeight = FontWeight.Bold))
                        KilnText(stringResource(description), style = Kiln.type.small, color = Kiln.colors.ink2)
                    }
                }
            }
        }
    }
}

// A CSV row has few spaces, so a narrow screen would break it inside a word; this lets it break after commas.
private fun csvLine(row: String) = row.replace(",", ",\u200B")

@Composable
private fun GroupLabel(text: String) {
    KilnText(text, style = Kiln.type.label, color = Kiln.colors.ink2, uppercase = true, heading = true)
}
