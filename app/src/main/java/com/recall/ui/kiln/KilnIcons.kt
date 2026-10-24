package com.recall.ui.kiln

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.recall.ui.theme.LocalContentColor

object KilnIcons {
    val library by icon("library", "M4 4h7v7H4ZM13 4h7v7h-7ZM4 13h7v7H4ZM13 13h7v7h-7Z")
    val progress by icon("progress", "M4 20h16M7 16v-5M12 16V7M17 16v-8")
    val settings by icon("settings", "M4 7h10M18 7h2M4 17h4M12 17h8M16 4.5v5M10 14.5v5")
    val search by icon("search", "M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14ZM20 20l-4-4")
    val plus by icon("plus", "M12 5v14M5 12h14")
    val minus by icon("minus", "M5 12h14")
    val check by icon("check", CHECK)
    val close by icon("close", CLOSE)
    val back by icon("back", "M15 5l-7 7 7 7")
    val forward by icon("forward", "M5 12h14M13 6l6 6-6 6")
    val chevronDown by icon("chevronDown", "M6 9l6 6 6-6")
    val edit by icon("edit", "M4 20h4L19 9l-4-4L4 16ZM13.5 6.5l4 4")
    val trash by icon("trash", "M5 7h14M10 7V4.5h4V7M7 7l1 13h8l1-13")
    val folder by icon(
        "folder",
        "M3.5 7.5a2 2 0 0 1 2-2H10l2 2.5h6.5a2 2 0 0 1 2 2V17a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2Z",
    )
    val importIcon by icon("import", "M12 4v11M7.5 10.5 12 15l4.5-4.5M5 15v4h14v-4")
    val exportIcon by icon("export", "M12 15V4M7.5 8.5 12 4l4.5 4.5M5 15v4h14v-4")
    val star by icon("star", "M12 3.5l2.6 5.4 5.9.8-4.3 4.1 1 5.8L12 16.8l-5.2 2.8 1-5.8L3.5 9.7l5.9-.8Z")
    val bell by icon("bell", "M6 16v-5a6 6 0 0 1 12 0v5l1.5 2h-15ZM10 20.5a2 2 0 0 0 4 0")

    // A near-zero segment with a heavy round cap draws a dot.
    val more by icon("more", "M12 5.5h.01M12 12h.01M12 18.5h.01", strokeWidth = 3.6f)

    // Heavier strokes for the check and cross on filled feedback tiles.
    val checkStrong by icon("checkStrong", CHECK, strokeWidth = 3f)
    val closeStrong by icon("closeStrong", CLOSE, strokeWidth = 3f)

    val all: List<Pair<String, ImageVector>>
        get() = listOf(
            "library" to library, "progress" to progress, "settings" to settings, "search" to search,
            "plus" to plus, "minus" to minus, "check" to check, "close" to close,
            "back" to back, "forward" to forward, "chevronDown" to chevronDown, "edit" to edit,
            "trash" to trash, "folder" to folder, "import" to importIcon, "export" to exportIcon,
            "star" to star, "bell" to bell, "more" to more,
            "checkStrong" to checkStrong, "closeStrong" to closeStrong,
        )
}

private const val CHECK = "M5 12.5 10 17l9-10"
private const val CLOSE = "M6 6l12 12M18 6 6 18"

private fun icon(name: String, path: String, strokeWidth: Float = 2f) = lazy {
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(path),
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = strokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ).build()
}

@Composable
fun KilnIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
) {
    Image(
        painter = rememberVectorPainter(icon),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}
