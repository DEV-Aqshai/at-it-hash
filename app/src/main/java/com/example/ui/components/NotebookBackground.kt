package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NotebookGridLine
import com.example.ui.theme.NotebookMarginRed
import com.example.ui.theme.NotebookPaper
import com.example.ui.theme.NotebookRuleBlue

@Composable
fun NotebookScaffold(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NotebookPaper)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Light background grid (subtle notebook graph lines)
            val gridSize = 24.dp.toPx()
            var x = 0f
            while (x < width) {
                drawLine(
                    color = NotebookGridLine.copy(alpha = 0.45f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
                x += gridSize
            }

            // 2. Horizontal blue notebook rule lines
            val lineSpacing = 32.dp.toPx()
            var y = 60.dp.toPx()
            while (y < height) {
                drawLine(
                    color = NotebookRuleBlue.copy(alpha = 0.75f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.2f
                )
                y += lineSpacing
            }

            // 3. Left margin red vertical lines (classic dual margin rule)
            val marginX = 40.dp.toPx()
            drawLine(
                color = NotebookMarginRed.copy(alpha = 0.65f),
                start = Offset(marginX, 0f),
                end = Offset(marginX, height),
                strokeWidth = 1.8f
            )

            // 4. Binder punch holes on the far left edge
            val holeRadius = 6.dp.toPx()
            val holeX = 18.dp.toPx()
            val holeYs = listOf(height * 0.15f, height * 0.5f, height * 0.85f)
            for (hy in holeYs) {
                // outer shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.12f),
                    radius = holeRadius + 1f,
                    center = Offset(holeX, hy)
                )
                // hole opening
                drawCircle(
                    color = Color(0xFFE2E0D5),
                    radius = holeRadius,
                    center = Offset(holeX, hy)
                )
            }

            // 5. Subtle hand-drawn doodle accents in the margin
            val starPath = Path().apply {
                val cx = marginX * 0.5f
                val cy = 45.dp.toPx()
                val r = 5.dp.toPx()
                moveTo(cx, cy - r)
                lineTo(cx + r * 0.4f, cy - r * 0.3f)
                lineTo(cx + r, cy - r * 0.3f)
                lineTo(cx + r * 0.5f, cy + r * 0.1f)
                lineTo(cx + r * 0.7f, cy + r * 0.8f)
                lineTo(cx, cy + r * 0.4f)
                lineTo(cx - r * 0.7f, cy + r * 0.8f)
                lineTo(cx - r * 0.5f, cy + r * 0.1f)
                lineTo(cx - r, cy - r * 0.3f)
                lineTo(cx - r * 0.4f, cy - r * 0.3f)
                close()
            }
            drawPath(
                path = starPath,
                color = NotebookMarginRed.copy(alpha = 0.4f),
                style = Stroke(width = 1.5f)
            )
        }

        content()
    }
}
