package com.recall.ui.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recall.BuildConfig
import com.recall.R
import com.recall.RecallApp
import com.recall.data.settings.ThemeMode
import com.recall.ui.kiln.KilnIcon
import com.recall.ui.kiln.KilnIcons
import com.recall.ui.kiln.KilnSurface
import com.recall.ui.kiln.KilnTabs
import com.recall.ui.kiln.KilnText
import com.recall.ui.kiln.arrival
import com.recall.ui.kiln.kilnFocusRing
import com.recall.ui.kiln.kilnPressable
import com.recall.ui.kiln.neutralFill
import com.recall.ui.kiln.rememberPressState
import com.recall.ui.nav.BottomBarSpace
import com.recall.ui.theme.ClayBody
import com.recall.ui.theme.Depth
import com.recall.ui.theme.Kiln
import com.recall.ui.theme.KilnPalettes
import com.recall.ui.theme.LocalOnGround
import com.recall.ui.theme.TypePairing
import com.recall.ui.theme.kilnDepth
import com.recall.ui.theme.kilnTypography
import com.recall.ui.theme.resolve

private val Modes = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
private val OptionShape = RoundedCornerShape(18.dp)

@Composable
fun SettingsScreen(onImport: () -> Unit) {
    val container = (LocalContext.current.applicationContext as RecallApp).container
    val viewModel = viewModel { SettingsViewModel(container.settings) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val arrival = Modifier.arrival(state != null)
    val settings = state ?: return

    val safe = WindowInsets.safeDrawing.asPaddingValues()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(Modifier.fillMaxSize().then(arrival)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = safe.calculateTopPadding() + 16.dp,
                    bottom = navBottom + BottomBarSpace + 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KilnText(stringResource(R.string.settings_title), style = Kiln.type.h1)

            SectionLabel(stringResource(R.string.settings_colour))
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ClayBody.entries.forEach { palette ->
                    PaletteOption(palette, settings.palette == palette, { viewModel.setPalette(palette) }, Modifier.weight(1f))
                }
            }
            KilnTabs(
                items = listOf(
                    stringResource(R.string.settings_mode_system),
                    stringResource(R.string.settings_mode_light),
                    stringResource(R.string.settings_mode_dark),
                ),
                selectedIndex = Modes.indexOf(settings.mode),
                onSelect = { viewModel.setMode(Modes[it]) },
            )

            SectionLabel(stringResource(R.string.settings_type))
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TypePairing.entries.forEach { pairing ->
                    PairingOption(pairing, settings.typePairing == pairing, { viewModel.setTypePairing(pairing) }, Modifier.weight(1f))
                }
            }

            SectionLabel(stringResource(R.string.settings_your_data))
            DataRow(
                stringResource(R.string.settings_import),
                stringResource(R.string.settings_import_sub),
                onClick = onImport,
            )

            KilnText(
                stringResource(R.string.settings_footer, BuildConfig.VERSION_NAME),
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                style = Kiln.type.caption,
                color = Kiln.colors.ink2,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PaletteOption(palette: ClayBody, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val swatch = remember(palette) { KilnPalettes.colors(palette, dark = false) }
    val ring = Kiln.colors.line
    OptionButton(selected, onClick, 96.dp, modifier) {
        // The light ground and the accent, split along the diagonal.
        Canvas(
            Modifier
                .size(40.dp)
                .clip(CircleShape),
        ) {
            drawRect(swatch.bg)
            val lower = Path().apply {
                moveTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(lower, swatch.accent)
            // A light ground half would vanish into a pressed-in button on a light theme without an edge.
            val stroke = 1.5.dp.toPx()
            drawCircle(ring, radius = (size.minDimension - stroke) / 2, style = Stroke(stroke))
        }
        KilnText(
            stringResource(paletteName(palette)),
            style = OptionLabel,
            color = if (selected) Kiln.colors.accent else Kiln.colors.ink,
            maxLines = 1,
        )
    }
}

@Composable
private fun PairingOption(pairing: TypePairing, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val face = remember(pairing) { kilnTypography(pairing).h2 }
    OptionButton(selected, onClick, 88.dp, modifier) {
        // The faces have different line heights, so a fixed box keeps the names below them level.
        Box(Modifier.height(36.dp), contentAlignment = Alignment.Center) {
            KilnText(stringResource(R.string.settings_type_sample), style = face, color = if (selected) Kiln.colors.accent else Kiln.colors.ink)
        }
        KilnText(
            stringResource(pairingName(pairing)),
            style = OptionLabel,
            color = if (selected) Kiln.colors.accent else Kiln.colors.ink,
            maxLines = 1,
        )
    }
}

/** A Secondary-button body in a rounded square: it floats until chosen, then stays pressed in. */
@Composable
private fun OptionButton(
    selected: Boolean,
    onClick: () -> Unit,
    height: Dp,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Kiln.colors
    val interactionSource = remember { MutableInteractionSource() }
    val press = rememberPressState(Depth.Float1, interactionSource, forcePressed = selected)
    val resolved = press.depth.resolve(LocalOnGround.current)
    Column(
        modifier
            .kilnFocusRing(interactionSource, OptionShape)
            .kilnDepth(resolved, OptionShape, neutralFill(press.depth, resolved, colors.surface2, colors), colors, animationSpec = press.depthSpec)
            .semantics { this.selected = selected }
            .kilnPressable(interactionSource, role = Role.RadioButton, onClick = onClick)
            .height(height),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        content = content,
    )
}

@Composable
private fun DataRow(title: String, caption: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    KilnSurface(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp),
        depth = Depth.Mold1,
        shape = RoundedCornerShape(18.dp),
        onClick = onClick,
    ) {
        Row(
            Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                KilnText(title, style = Kiln.type.bodyStrong)
                KilnText(caption, style = Kiln.type.caption, color = Kiln.colors.ink2)
            }
            KilnIcon(KilnIcons.forward, contentDescription = null, size = 22.dp, tint = Kiln.colors.ink2)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    KilnText(text, Modifier.padding(top = 8.dp), style = Kiln.type.label, color = Kiln.colors.ink2, uppercase = true)
}

private val OptionLabel
    @Composable get() = Kiln.type.body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)

private fun paletteName(palette: ClayBody): Int = when (palette) {
    ClayBody.SLATE -> R.string.palette_slate
    ClayBody.TERRACOTTA -> R.string.palette_terracotta
    ClayBody.SAGE -> R.string.palette_sage
    ClayBody.OCHRE -> R.string.palette_ochre
}

private fun pairingName(pairing: TypePairing): Int = when (pairing) {
    TypePairing.A -> R.string.pairing_friendly
    TypePairing.B -> R.string.pairing_chunky
    TypePairing.C -> R.string.pairing_classic
}
