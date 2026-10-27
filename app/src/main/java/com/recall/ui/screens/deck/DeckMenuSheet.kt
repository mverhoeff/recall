package com.recall.ui.screens.deck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.recall.R
import com.recall.ui.kiln.ButtonVariant
import com.recall.ui.kiln.KilnButton
import com.recall.ui.kiln.KilnIcon
import com.recall.ui.kiln.KilnIcons
import com.recall.ui.kiln.KilnSheet
import com.recall.ui.kiln.KilnSurface
import com.recall.ui.kiln.KilnText
import com.recall.ui.kiln.KilnTextField
import com.recall.ui.kiln.SurfaceTone
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln
import com.recall.ui.theme.LocalContentColor
import java.text.Normalizer

@Composable
fun DeckMenuSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KilnSheet(visible, onDismiss, modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MenuRow(KilnIcons.edit, stringResource(R.string.deck_menu_rename), onRename)
            MenuRow(KilnIcons.exportIcon, stringResource(R.string.deck_menu_export), onExport)
            MenuRow(KilnIcons.trash, stringResource(R.string.deck_menu_delete), onDelete, color = Kiln.colors.err)
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit, color: Color = LocalContentColor.current) {
    KilnSurface(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp),
        depth = Depth.Float1,
        tone = SurfaceTone.Raised,
        shape = RoundedCornerShape(18.dp),
        onClick = onClick,
    ) {
        Row(
            Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            KilnIcon(icon, contentDescription = null, size = 22.dp, tint = color)
            KilnText(label, style = Kiln.type.bodyStrong, color = color)
        }
    }
}

@Composable
fun RenameDeckDialog(name: String, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var value by rememberSaveable { mutableStateOf(name) }
    val focus = remember { FocusRequester() }

    Dialog(onDismissRequest = onDismiss) {
        KilnSurface(
            // The window ends at the content's bounds, so leave room for the float shadow to fall into.
            Modifier
                .padding(top = 8.dp, bottom = 40.dp)
                .fillMaxWidth(),
            depth = Depth.Float3,
            tone = SurfaceTone.Raised,
            shape = RoundedCornerShape(38.dp),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                KilnText(stringResource(R.string.deck_rename_title), style = Kiln.type.h3, heading = true)
                KilnTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = stringResource(R.string.new_deck_name),
                    modifier = Modifier.focusRequester(focus),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Row(
                    Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    KilnButton(stringResource(R.string.action_cancel), onDismiss, variant = ButtonVariant.Ghost)
                    KilnButton(
                        stringResource(R.string.action_save),
                        onClick = { onRename(value.trim()) },
                        enabled = value.isNotBlank(),
                    )
                }
            }
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}

/** "Spanish verbs — A2" → "spanish-verbs-a2.json". */
internal fun deckFileName(name: String): String {
    val slug = Normalizer.normalize(name, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
    return "${slug.ifEmpty { "deck" }}.json"
}
