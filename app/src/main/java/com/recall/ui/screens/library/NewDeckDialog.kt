package com.recall.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.recall.R
import com.recall.ui.kiln.ButtonVariant
import com.recall.ui.kiln.KilnButton
import com.recall.ui.kiln.KilnSurface
import com.recall.ui.kiln.KilnText
import com.recall.ui.kiln.KilnTextField
import com.recall.ui.kiln.SurfaceTone
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln

@Composable
fun NewDeckDialog(onDismiss: () -> Unit, onCreate: (name: String, description: String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val sentences = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)

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
                KilnText(stringResource(R.string.library_new_deck), style = Kiln.type.h3, heading = true)
                KilnTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.new_deck_name),
                    modifier = Modifier.focusRequester(focus),
                    keyboardOptions = sentences,
                )
                KilnTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = stringResource(R.string.new_deck_description),
                    singleLine = false,
                    minLines = 2,
                    keyboardOptions = sentences,
                )
                Row(
                    Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    KilnButton(stringResource(R.string.action_cancel), onDismiss, variant = ButtonVariant.Ghost)
                    KilnButton(
                        stringResource(R.string.action_create),
                        onClick = { onCreate(name.trim(), description.trim()) },
                        enabled = name.isNotBlank(),
                    )
                }
            }
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}
