package io.nekohasekai.sfa.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun LineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    animate: Boolean = false,
) {
    val linePath = remember { Path() }
    val fillPath = remember { Path() }
    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
    ) {
        val width = size.width
        val height = size.height
        val maxValue = max(data.maxOrNull() ?: 1f, 1f) * 1.2f
        val pointCount = data.size

        val gridLineCount = 3
        for (i in 0..gridLineCount) {
            val y = height * i / gridLineCount
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect,
            )
        }

        if (pointCount > 1) {
            val spacing = width / (pointCount - 1).toFloat()

            linePath.reset()
            fillPath.reset()

            val firstNormalized = (data[0] / maxValue).coerceIn(0f, 1f)
            val firstY = height * (1 - firstNormalized)
            linePath.moveTo(0f, firstY)

            for (i in 1 until pointCount) {
                val x = i * spacing
                val normalizedValue = (data[i] / maxValue).coerceIn(0f, 1f)
                val y = height * (1 - normalizedValue)
                linePath.lineTo(x, y)
            }

            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )

            fillPath.addPath(linePath)
            val lastNormalized = (data[pointCount - 1] / maxValue).coerceIn(0f, 1f)
            val lastY = height * (1 - lastNormalized)
            val lastX = (pointCount - 1) * spacing
            fillPath.lineTo(lastX, height)
            fillPath.lineTo(0f, height)
            fillPath.close()

            drawPath(
                path = fillPath,
                color = lineColor.copy(alpha = 0.1f),
            )
        }
    }
}
