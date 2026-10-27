package com.recall.ui.kiln

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Equal-width columns, using the first count in [columns] at which every child fits its column at
 * its natural width. At large font sizes items move onto more rows instead of having labels cut short.
 */
@Composable
fun FitColumns(columns: List<Int>, modifier: Modifier = Modifier, spacing: Dp = 12.dp, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val width = constraints.maxWidth
        fun cellWidth(count: Int) = ((width - gap * (count - 1)) / count).coerceAtLeast(0)
        val wanted = measurables.map { it.maxIntrinsicWidth(constraints.maxHeight) }
        val count = columns.firstOrNull { count -> wanted.all { it <= cellWidth(count) } } ?: columns.last()
        val cell = cellWidth(count)
        val rows = measurables.map { it.measure(Constraints.fixedWidth(cell)) }.chunked(count)
        val heights = rows.map { row -> row.maxOf { it.height } }
        layout(width, heights.sum() + gap * (rows.size - 1).coerceAtLeast(0)) {
            var y = 0
            rows.forEachIndexed { index, row ->
                row.forEachIndexed { column, placeable -> placeable.placeRelative(column * (cell + gap), y) }
                y += heights[index] + gap
            }
        }
    }
}
