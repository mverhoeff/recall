package com.recall.ui.kiln

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln

@Composable
fun KilnDialog(
    title: String,
    body: String,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
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
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                KilnText(title, style = Kiln.type.h3, heading = true)
                KilnText(body, style = Kiln.type.small, color = Kiln.colors.ink2)
                Row(
                    Modifier
                        .align(Alignment.End)
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    KilnButton(dismissText, onDismiss, variant = ButtonVariant.Ghost)
                    KilnButton(
                        confirmText,
                        onConfirm,
                        variant = if (destructive) ButtonVariant.Pop else ButtonVariant.Primary,
                    )
                }
            }
        }
    }
}
