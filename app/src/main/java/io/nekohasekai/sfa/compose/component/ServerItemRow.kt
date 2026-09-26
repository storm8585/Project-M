package io.nekohasekai.sfa.compose.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.nekohasekai.sfa.compose.theme.IosDivider
import io.nekohasekai.sfa.compose.theme.IosTextPrimary
import io.nekohasekai.sfa.compose.theme.IosTextSecondary
import io.nekohasekai.sfa.compose.theme.TelegramBlue

@Composable
fun ServerItemRow(
    name: String,
    isSelected: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(modifier = Modifier.size(32.dp)) {
                val corner = 8.dp.toPx()
                drawRoundRect(
                    color = if (isSelected) Color(0x262AABEE) else Color(0x14FFFFFF),
                    size = size,
                    cornerRadius = CornerRadius(corner, corner),
                )

                val center = Offset(size.width / 2f, size.height / 2f)
                val iconColor = if (isSelected) TelegramBlue else Color(0xFF9E9EA4)
                drawCircle(
                    color = iconColor,
                    radius = 8.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
                drawOval(
                    color = iconColor,
                    topLeft = Offset(center.x - 4.dp.toPx(), center.y - 8.dp.toPx()),
                    size = Size(8.dp.toPx(), 16.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx()),
                )
                drawLine(
                    color = iconColor,
                    start = Offset(center.x - 8.dp.toPx(), center.y),
                    end = Offset(center.x + 8.dp.toPx(), center.y),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = IosTextPrimary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(RoundedCornerShape(2.5.dp))
                            .clickable(enabled = false) {}
                            .padding(0.dp),
                    ) {
                        Canvas(modifier = Modifier.size(5.dp)) {
                            drawCircle(color = if (isSelected) Color(0xFF30D158) else Color(0xFF8E8E93))
                        }
                    }
                    Text(
                        text = "Optimal routing",
                        fontSize = 13.sp,
                        color = IosTextSecondary,
                    )
                }
            }

            if (isSelected) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val path = Path().apply {
                        moveTo(size.width * 0.2f, size.height * 0.52f)
                        lineTo(size.width * 0.44f, size.height * 0.74f)
                        lineTo(size.width * 0.82f, size.height * 0.28f)
                    }
                    drawPath(
                        path = path,
                        color = TelegramBlue,
                        style = Stroke(
                            width = 2.2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 62.dp),
                thickness = 0.5.dp,
                color = IosDivider,
            )
        }
    }
}
