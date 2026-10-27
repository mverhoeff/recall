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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.recall.RecallApp
import com.recall.notify.ReminderWorker
import com.recall.ui.kiln.BadgeTone
import com.recall.ui.kiln.BlobShape
import com.recall.ui.kiln.BlobTone
import com.recall.ui.kiln.ButtonSize
import com.recall.ui.kiln.ButtonVariant
import com.recall.ui.kiln.KilnBadge
import com.recall.ui.kiln.KilnBlob
import com.recall.ui.kiln.KilnButton
import com.recall.ui.kiln.KilnChip
import com.recall.ui.kiln.KilnDialog
import com.recall.ui.kiln.KilnIcon
import com.recall.ui.kiln.KilnIconButton
import com.recall.ui.kiln.KilnIcons
import com.recall.ui.kiln.KilnProgress
import com.recall.ui.kiln.KilnRadio
import com.recall.ui.kiln.KilnSheet
import com.recall.ui.kiln.KilnSurface
import com.recall.ui.kiln.KilnTabs
import com.recall.ui.kiln.KilnText
import com.recall.ui.kiln.KilnTextField
import com.recall.ui.kiln.KilnTile
import com.recall.ui.kiln.KilnToggle
import com.recall.ui.kiln.SurfaceTone
import com.recall.ui.kiln.TileSize
import com.recall.ui.kiln.TileTone
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
import kotlinx.coroutines.launch

@Composable
fun KilnGallery() {
    var body by rememberSaveable { mutableStateOf(ClayBody.SLATE) }
    var darkOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val dark = darkOverride ?: isSystemInDarkTheme()
    var sheet by rememberSaveable { mutableStateOf(false) }
    var dialog by rememberSaveable { mutableStateOf(false) }

    KilnTheme(body, dark, TypePairing.B, reducedMotion = false) {
        Box(Modifier.fillMaxSize().kilnGround(Kiln.colors)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .systemBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                PaletteSwitcher(body, dark, onBody = { body = it }, onDark = { darkOverride = !dark })
                SeedButton()
                ReminderButton()
                ChipsAndBadges()
                TilesAndProgress()
                Controls()
                Fields()
                Overlays(onSheet = { sheet = true }, onDialog = { dialog = true })
                DepthGrid()
                NestedDemo()
                ButtonDemo()
                SurfaceDemo()
                IconGrid()
                TypeSamples()
            }
            KilnSheet(sheet, onDismiss = { sheet = false }, Modifier.align(Alignment.BottomCenter)) {
                KilnText("Reminder time", style = Kiln.type.h3)
                KilnTabs(listOf("00", "15", "30", "45"), 0, onSelect = {})
                KilnButton("Done", { sheet = false }, Modifier.fillMaxWidth())
            }
        }
        if (dialog) {
            KilnDialog(
                title = "Delete everything?",
                body = "All decks, cards, progress and settings on this phone will be removed. This can't be undone.",
                confirmText = "Delete",
                onConfirm = { dialog = false },
                dismissText = "Cancel",
                onDismiss = { dialog = false },
                destructive = true,
            )
        }
    }
}

@Composable
private fun SeedButton() {
    val container = (LocalContext.current.applicationContext as RecallApp).container
    val scope = rememberCoroutineScope()
    var seeded by rememberSaveable { mutableStateOf(false) }
    KilnButton(
        if (seeded) "Sample decks added" else "Seed sample decks",
        onClick = { scope.launch { SampleData.seed(container); seeded = true } },
        variant = ButtonVariant.Secondary,
        enabled = !seeded,
    )
}

@Composable
private fun ReminderButton() {
    val context = LocalContext.current
    KilnButton(
        "Send reminder now",
        onClick = { WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<ReminderWorker>().build()) },
        variant = ButtonVariant.Secondary,
    )
}

/** Shows [content] on the ground and again inside a Mold2 panel, to compare both contexts. */
@Composable
private fun BothContexts(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Label(title)
        content()
        KilnSurface(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun ChipsAndBadges() {
    BothContexts("Chips") {
        var selected by rememberSaveable { mutableStateOf(1) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("5", "History", "Geography").forEachIndexed { i, text ->
                KilnChip(text, selected = i == selected, onClick = { selected = i })
            }
        }
    }
    BothContexts("Badges") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KilnBadge("New")
            KilnBadge("Learned", tone = BadgeTone.Ok)
            KilnBadge("Due", tone = BadgeTone.Warn)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KilnBadge("Missed", tone = BadgeTone.Err)
            KilnBadge("3", tone = BadgeTone.Accent)
        }
    }
}

@Composable
private fun TilesAndProgress() {
    BothContexts("Tiles") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            KilnTile(size = TileSize.Sm, icon = KilnIcons.folder)
            KilnTile(tone = TileTone.Soft, initials = "Ge")
            KilnTile(tone = TileTone.Accent, size = TileSize.Lg, icon = KilnIcons.star)
            KilnTile(tone = TileTone.Dip, icon = KilnIcons.library)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KilnTile(tone = TileTone.Ok, icon = KilnIcons.checkStrong)
            KilnTile(tone = TileTone.Err, icon = KilnIcons.closeStrong)
            KilnTile(tone = TileTone.Pop, initials = "Hi")
        }
    }
    BothContexts("Progress") {
        var fraction by rememberSaveable { mutableStateOf(0.35f) }
        KilnProgress(0f)
        KilnProgress(fraction)
        KilnButton(
            "Change progress",
            { fraction = if (fraction < 0.5f) 0.85f else 0.35f },
            variant = ButtonVariant.Secondary,
            size = ButtonSize.Sm,
        )
    }
}

@Composable
private fun Controls() {
    BothContexts("Tabs") {
        var tab by rememberSaveable { mutableStateOf(0) }
        KilnTabs(listOf("System", "Light", "Dark"), tab, onSelect = { tab = it })
    }
    BothContexts("Toggles") {
        var on by rememberSaveable { mutableStateOf(true) }
        var off by rememberSaveable { mutableStateOf(false) }
        KilnToggle(on, { on = it }, "Daily reminder", sublabel = "19:00, only when cards are due")
        KilnToggle(off, { off = it }, "Haptic feedback")
    }
    BothContexts("Radios") {
        var choice by rememberSaveable { mutableStateOf(0) }
        Row {
            repeat(3) { i -> KilnRadio(selected = i == choice, onClick = { choice = i }) }
        }
    }
}

@Composable
private fun Fields() {
    BothContexts("Text fields") {
        var name by rememberSaveable { mutableStateOf("") }
        var answer by rememberSaveable { mutableStateOf("Jupiter") }
        KilnTextField(name, { name = it }, "Deck name", placeholder = "World capitals", hint = "Shown in your library")
        KilnTextField(answer, { answer = it }, "Answer", error = "Answers need at least two options")
    }
    BothContexts("Blobs") {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            KilnBlob(BlobShape.Pebble, BlobTone.Accent, DpSize(72.dp, 64.dp))
            KilnBlob(BlobShape.Coil, BlobTone.Soft, DpSize(96.dp, 40.dp))
            KilnBlob(BlobShape.Pebble, BlobTone.Pop, DpSize(48.dp, 48.dp))
            KilnBlob(BlobShape.Coil, BlobTone.Raised, DpSize(40.dp, 56.dp))
        }
    }
}

@Composable
private fun Overlays(onSheet: () -> Unit, onDialog: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Label("Overlays")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KilnButton("Show sheet", onSheet, variant = ButtonVariant.Secondary)
            KilnButton("Show dialog", onDialog, variant = ButtonVariant.Secondary)
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
