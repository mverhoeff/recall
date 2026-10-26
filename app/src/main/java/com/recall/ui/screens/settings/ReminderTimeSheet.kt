package com.recall.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.recall.R
import com.recall.data.settings.ReminderMinutes
import com.recall.ui.kiln.ButtonSize
import com.recall.ui.kiln.KilnButton
import com.recall.ui.kiln.KilnChip
import com.recall.ui.kiln.KilnIconButton
import com.recall.ui.kiln.KilnIcons
import com.recall.ui.kiln.KilnSheet
import com.recall.ui.kiln.KilnText
import com.recall.ui.theme.Kiln

@Composable
fun ReminderTimeSheet(
    visible: Boolean,
    hour: Int,
    minute: Int,
    onChangeHour: (Int) -> Unit,
    onSelectMinute: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KilnSheet(visible, onDismiss, modifier) {
        KilnText(stringResource(R.string.reminder_title), style = Kiln.type.h3)
        KilnText(
            stringResource(R.string.reminder_time, hour, minute),
            Modifier.fillMaxWidth(),
            style = Kiln.type.display,
            textAlign = TextAlign.Center,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            KilnText(stringResource(R.string.reminder_hour), style = Kiln.type.small, color = Kiln.colors.ink2)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KilnIconButton(KilnIcons.minus, stringResource(R.string.reminder_earlier), { onChangeHour(-1) })
                KilnText(
                    stringResource(R.string.reminder_hour_value, hour),
                    Modifier.widthIn(min = 48.dp),
                    style = Kiln.type.h2,
                    textAlign = TextAlign.Center,
                )
                KilnIconButton(KilnIcons.plus, stringResource(R.string.reminder_later), { onChangeHour(1) })
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            KilnText(stringResource(R.string.reminder_minute), style = Kiln.type.small, color = Kiln.colors.ink2)
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReminderMinutes.forEach { option ->
                    KilnChip(
                        stringResource(R.string.reminder_minute_value, option),
                        selected = option == minute,
                        onClick = { onSelectMinute(option) },
                    )
                }
            }
        }
        KilnButton(stringResource(R.string.reminder_done), onDismiss, Modifier.fillMaxWidth(), size = ButtonSize.Lg)
    }
}
