package com.recall.ui.screens.importer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recall.R
import com.recall.io.DeckFileReader
import com.recall.io.FileKind
import com.recall.io.ImportValidator
import com.recall.io.NEWER_VERSION
import com.recall.io.NOT_A_DECK
import com.recall.io.ParsedFile
import com.recall.ui.kiln.ButtonSize
import com.recall.ui.kiln.ButtonVariant
import com.recall.ui.kiln.KilnButton
import com.recall.ui.kiln.KilnIconButton
import com.recall.ui.kiln.KilnIcons
import com.recall.ui.kiln.KilnSurface
import com.recall.ui.kiln.KilnText
import com.recall.ui.kiln.KilnTile
import com.recall.ui.kiln.TileSize
import com.recall.ui.kiln.TileTone
import com.recall.ui.kiln.rise
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln

private val PickerTypes = arrayOf("text/*", "application/json", "application/octet-stream")

@Composable
fun ImportScreen(onBack: () -> Unit) {
    val viewModel = viewModel { ImportViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.load(context.contentResolver, uri)
    }
    val choose = { picker.launch(PickerTypes) }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding(),
        ) {
            Row(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                KilnIconButton(KilnIcons.back, stringResource(R.string.action_back), onBack)
                KilnText(stringResource(R.string.import_title), Modifier.weight(1f), style = Kiln.type.h3)
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val file = state.file
                if (file == null) {
                    ChooseWell(choose)
                } else {
                    FileRow(file, onChange = choose, Modifier.rise(0))
                    when (val parsed = file.parsed) {
                        is ParsedFile.Deck -> Unit
                        is ParsedFile.Failure -> FailurePanel(parsed.message, Modifier.rise(1))
                        is ParsedFile.Backup -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun ChooseWell(onChoose: () -> Unit) {
    KilnSurface(Modifier.fillMaxWidth(), depth = Depth.Dip, shape = RoundedCornerShape(26.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KilnTile(tone = TileTone.Default, size = TileSize.Lg, icon = KilnIcons.folder)
            KilnText(stringResource(R.string.import_choose_prompt), style = Kiln.type.bodyStrong, textAlign = TextAlign.Center)
            KilnButton(stringResource(R.string.import_choose), onChoose)
        }
    }
}

@Composable
private fun FileRow(file: PickedFile, onChange: () -> Unit, modifier: Modifier = Modifier) {
    val size = fileSize(file.sizeBytes)
    val caption = when (val parsed = file.parsed) {
        is ParsedFile.Deck -> when (parsed.kind) {
            FileKind.CSV -> stringResource(R.string.import_caption_csv, size)
            FileKind.JSON -> stringResource(R.string.import_caption_json, size)
        }
        is ParsedFile.Backup -> stringResource(R.string.import_caption_json, size)
        is ParsedFile.Failure -> size
    }
    KilnSurface(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp),
        depth = Depth.Mold1,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KilnTile(tone = TileTone.Soft, size = TileSize.Sm, icon = KilnIcons.folder)
            Column(Modifier.weight(1f)) {
                KilnText(file.name, style = Kiln.type.bodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                KilnText(caption, style = Kiln.type.caption, color = Kiln.colors.ink2)
            }
            KilnButton(stringResource(R.string.import_change), onChange, variant = ButtonVariant.Secondary, size = ButtonSize.Sm)
        }
    }
}

@Composable
private fun FailurePanel(message: String, modifier: Modifier = Modifier) {
    KilnSurface(modifier.fillMaxWidth(), depth = Depth.Mold2, shape = RoundedCornerShape(26.dp)) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            KilnTile(tone = TileTone.Err, size = TileSize.Sm, icon = KilnIcons.close)
            Column(Modifier.weight(1f)) {
                KilnText(stringResource(R.string.import_nothing), style = Kiln.type.h3)
                KilnText(importMessage(message), style = Kiln.type.small, color = Kiln.colors.ink2)
            }
        }
    }
}

@Composable
private fun fileSize(bytes: Long): String = when {
    bytes < 1024 * 1024 -> stringResource(R.string.import_size_kb, maxOf(1L, (bytes + 512) / 1024))
    else -> stringResource(R.string.import_size_mb, bytes / (1024.0 * 1024.0))
}

private val PlacedMessage = Regex("""^(Row|Card) (\d+): (.*)$""")
private val UnknownType = Regex("""^type "(.*)" isn't known\. Use mc, tf or card\.$""")

/** The reader words its messages in English; this finds the same wording in the app's strings. */
@Composable
private fun importMessage(message: String): String {
    when (message) {
        ImportValidator.MISSING_HEADER -> return stringResource(R.string.import_error_header)
        DeckFileReader.TOO_LARGE -> return stringResource(R.string.import_error_size)
        DeckFileReader.NOT_UTF8 -> return stringResource(R.string.import_error_utf8)
        NOT_A_DECK -> return stringResource(R.string.import_error_not_deck)
        NEWER_VERSION -> return stringResource(R.string.import_error_newer)
        UNREADABLE -> return stringResource(R.string.import_error_unreadable)
    }
    val placed = PlacedMessage.matchEntire(message) ?: return message
    val (kind, number, rest) = placed.destructured
    val place = stringResource(if (kind == "Row") R.string.import_row else R.string.import_card, number.toInt())
    UnknownType.matchEntire(rest)?.let { return stringResource(R.string.import_error_type, place, it.groupValues[1]) }
    return when (rest) {
        "the prompt is empty." -> stringResource(R.string.import_error_prompt, place)
        "a choice card needs at least 2 answers." -> stringResource(R.string.import_error_answers, place)
        "true/false cards need true or false as the answer." -> stringResource(R.string.import_error_truth, place)
        "the back of the card is empty." -> stringResource(R.string.import_error_back, place)
        else -> message
    }
}
