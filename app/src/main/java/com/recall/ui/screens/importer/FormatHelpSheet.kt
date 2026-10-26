package com.recall.ui.screens.importer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.recall.R
import com.recall.ui.kiln.KilnSheet
import com.recall.ui.kiln.KilnSurface
import com.recall.ui.kiln.KilnText
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln

@Composable
fun FormatHelpSheet(visible: Boolean, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    KilnSheet(visible, onDismiss, modifier) {
        KilnText(stringResource(R.string.help_title), style = Kiln.type.h3)
        KilnText(stringResource(R.string.help_body), style = Kiln.type.small, color = Kiln.colors.ink2)
        KilnSurface(Modifier.fillMaxWidth(), depth = Depth.Dip, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                KilnText(stringResource(R.string.help_header), style = Kiln.type.small.copy(fontWeight = FontWeight.Bold))
                KilnText(stringResource(R.string.help_example_mc), style = Kiln.type.small)
                KilnText(stringResource(R.string.help_example_tf), style = Kiln.type.small)
                KilnText(stringResource(R.string.help_example_card), style = Kiln.type.small)
            }
        }
        KilnText(stringResource(R.string.help_types), style = Kiln.type.small, color = Kiln.colors.ink2)
        KilnText(stringResource(R.string.help_more_answers), style = Kiln.type.small, color = Kiln.colors.ink2)
        KilnText(stringResource(R.string.help_json), style = Kiln.type.small, color = Kiln.colors.ink2)
    }
}
