package com.recall.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.recall.ui.kiln.ButtonSize
import com.recall.ui.kiln.ButtonVariant
import com.recall.ui.kiln.KilnButton
import com.recall.ui.kiln.KilnIcon
import com.recall.ui.kiln.KilnIconButton
import com.recall.ui.kiln.KilnIcons
import com.recall.ui.kiln.KilnSurface
import com.recall.ui.kiln.KilnText
import com.recall.ui.kiln.SurfaceTone
import com.recall.ui.theme.ClayBody
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln
import com.recall.ui.theme.KilnPalettes
import com.recall.ui.theme.KilnTheme
import com.recall.ui.theme.TypePairing
import com.recall.ui.theme.kilnDepth
import com.recall.ui.theme.kilnGround
import com.recall.ui.theme.kilnTypography
import com.recall.ui.theme.resolve

@Composable
fun KilnGallery() {
    var body by rememberSaveable { mutableStateOf(ClayBody.SLATE) }
    var darkOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val dark = darkOverride ?: isSystemInDarkTheme()

    KilnTheme(body, dark, TypePairing.B, reducedMotion = false) {
        Column(
            Modifier
                .fillMaxSize()
                .kilnGround(Kiln.colors)
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            PaletteSwitcher(body, dark, onBody = { body = it }, onDark = { darkOverride = !dark })
            DepthGrid()
            NestedDemo()
            ButtonDemo()
            SurfaceDemo()
            IconGrid()
            TypeSamples()
        }
    }
}

@Composable
private fun PaletteSwitcher(body: ClayBody, dark: Boolean, onBody: (ClayBody) -> Unit, onDark: () -> Unit) {
    val colors = Kiln.colors
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        for (option in ClayBody.entries) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(KilnPalettes.colors(option, dark = false).accent, CircleShape)
                    .then(if (option == body) Modifier.border(3.dp, colors.ink, CircleShape) else Modifier)
                    .clickable { onBody(option) }
            )
        }
        Spacer(Modifier.size(8.dp))
        Box(
            Modifier
                .height(48.dp)
                .background(colors.ink, RoundedCornerShape(24.dp))
                .clickable(onClick = onDark)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(if (dark) "Dark" else "Light", style = Kiln.type.button.copy(color = colors.bg))
        }
    }
}

@Composable
private fun DepthGrid() {
    val colors = Kiln.colors
    Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Label("Depths on the ground")
        for (row in Depth.entries.chunked(3)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (depth in row) {
                    val fill = when (depth) {
                        Depth.Float1, Depth.Float2, Depth.Float3 -> colors.surface2
                        Depth.DipSm, Depth.Dip -> colors.well
                        else -> colors.surface
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(96.dp, 72.dp).kilnDepth(depth, RoundedCornerShape(18.dp), fill, colors))
                        Spacer(Modifier.height(10.dp))
                        BasicText(depth.name, style = Kiln.type.caption.copy(color = colors.ink2))
                    }
                }
            }
        }
    }
}

@Composable
private fun NestedDemo() {
    val colors = Kiln.colors
    val mold = Depth.Mold1.resolve(onGround = false)
    val dip = Depth.DipSm.resolve(onGround = false)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Label("Inside a Mold2 card")
        Column(
            Modifier
                .fillMaxWidth()
                .kilnDepth(Depth.Mold2, RoundedCornerShape(26.dp), colors.surface, colors)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(Modifier.size(120.dp, 72.dp).kilnDepth(mold, RoundedCornerShape(18.dp), colors.surface2, colors))
                Box(Modifier.size(120.dp, 72.dp).kilnDepth(dip, RoundedCornerShape(18.dp), colors.line, colors))
            }
            BasicText("Mold1 → $mold, DipSm → $dip", style = Kiln.type.caption.copy(color = colors.ink2))
        }
    }
}

@Composable
private fun SurfaceDemo() {
    var taps by rememberSaveable { mutableStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Label("Surfaces")
        KilnSurface(Modifier.fillMaxWidth(), depth = Depth.Mold1, onClick = { taps++ }) {
            KilnText("Clickable Mold1 surface, tapped $taps times", Modifier.padding(24.dp))
        }
        KilnSurface(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                KilnText("Mold2 card with nested surfaces", style = Kiln.type.bodyStrong)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KilnSurface(Modifier.size(96.dp, 64.dp), depth = Depth.Mold1, shape = RoundedCornerShape(18.dp)) {}
                    KilnSurface(Modifier.size(96.dp, 64.dp), depth = Depth.DipSm, shape = RoundedCornerShape(18.dp)) {}
                    KilnSurface(Modifier.size(96.dp, 64.dp), depth = Depth.Float1, tone = SurfaceTone.Accent, shape = RoundedCornerShape(18.dp)) {
                        KilnText("Accent", Modifier.align(Alignment.Center), style = Kiln.type.caption)
                    }
                }
                KilnText("Mold1 floats, DipSm goes flat", style = Kiln.type.caption, color = Kiln.colors.ink2)
            }
        }
    }
}

@Composable
private fun ButtonDemo() {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Label("Buttons")
        for (variant in ButtonVariant.entries) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                for (size in ButtonSize.entries) {
                    val label = if (size == ButtonSize.Sm) variant.name else size.name
                    KilnButton(label, {}, variant = variant, size = size)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KilnButton("Disabled", {}, enabled = false)
            KilnButton("Disabled", {}, variant = ButtonVariant.Secondary, enabled = false)
        }
        Label("Pressed on the ground")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KilnButton("Primary", {}, pressed = true)
            KilnButton("Secondary", {}, variant = ButtonVariant.Secondary, pressed = true)
            KilnButton("Ghost", {}, variant = ButtonVariant.Ghost, pressed = true)
        }
        Label("Pressed inside a card")
        KilnSurface(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KilnButton("Rest", {}, variant = ButtonVariant.Secondary)
                KilnButton("Pressed", {}, variant = ButtonVariant.Secondary, pressed = true)
            }
        }
        Label("Icon buttons")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            KilnIconButton(KilnIcons.plus, "Add", {})
            KilnIconButton(KilnIcons.check, "Confirm", {}, variant = ButtonVariant.Primary)
            KilnIconButton(KilnIcons.trash, "Delete", {}, variant = ButtonVariant.Ghost)
            KilnIconButton(KilnIcons.more, "More", {}, size = ButtonSize.Lg)
            KilnIconButton(KilnIcons.edit, "Edit", {}, pressed = true)
        }
        KilnButton("Start review", {}, size = ButtonSize.Lg, trailingIcon = KilnIcons.forward)
    }
}

@Composable
private fun IconGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Label("Icons")
        for (row in KilnIcons.all.chunked(5)) {
            Row(Modifier.fillMaxWidth()) {
                for ((name, icon) in row) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        KilnIcon(icon, contentDescription = name)
                        Spacer(Modifier.height(6.dp))
                        KilnText(name, style = Kiln.type.caption, color = Kiln.colors.ink2, maxLines = 1)
                    }
                }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun TypeSamples() {
    val colors = Kiln.colors
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        for (pairing in TypePairing.entries) {
            val type = kilnTypography(pairing)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BasicText("PAIRING ${pairing.name}", style = type.label.copy(color = colors.ink2))
                BasicText("Study the capital cities", style = type.h1.copy(color = colors.ink))
                BasicText(
                    "Cards due today: 12. Tap a deck to start a review session.",
                    style = type.body.copy(color = colors.ink),
                )
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    BasicText(text.uppercase(), style = Kiln.type.label.copy(color = Kiln.colors.ink2))
}
