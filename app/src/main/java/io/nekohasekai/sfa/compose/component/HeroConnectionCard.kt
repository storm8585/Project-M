package io.nekohasekai.sfa.compose.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.nekohasekai.sfa.compose.theme.IosDivider
import io.nekohasekai.sfa.compose.theme.IosGreen
import io.nekohasekai.sfa.compose.theme.IosOrange
import io.nekohasekai.sfa.compose.theme.IosTextPrimary
import io.nekohasekai.sfa.compose.theme.IosTextSecondary
import io.nekohasekai.sfa.compose.theme.TelegramBlue
import io.nekohasekai.sfa.constant.Status

@Composable
fun HeroConnectionCard(
    status: Status,
    downlink: String,
    downlinkTotal: String,
    uplink: String,
    uplinkTotal: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRunning = status == Status.Started
    val isStarting = status == Status.Starting
    val isStopping = status == Status.Stopping

    val statusDotColor = when {
        isRunning -> IosGreen
        isStarting -> TelegramBlue
        isStopping -> IosOrange
        else -> Color(0xFF48484A)
    }

    val statusText = when {
        isRunning -> "Connected"
        isStarting -> "Connecting..."
        isStopping -> "Disconnecting..."
        else -> "Not Connected"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    InsetGroup(
        header = "Status",
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Canvas(modifier = Modifier.size(14.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    if (isRunning) {
                        drawCircle(
                            color = statusDotColor.copy(alpha = pulseAlpha),
                            radius = size.width / 2f,
                            center = center,
                        )
                    }
                    drawCircle(
                        color = statusDotColor,
                        radius = 4.5.dp.toPx(),
                        center = center,
                    )
                }

                Column {
                    Text(
                        text = statusText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IosTextPrimary,
                    )
                    Text(
                        text = if (isRunning) "Active secure session" else "Tap switch to enable",
                        fontSize = 13.sp,
                        color = IosTextSecondary,
                    )
                }
            }

            IosSwitch(
                checked = isRunning || isStarting,
                onCheckedChange = { onToggle() },
                enabled = !isStarting && !isStopping,
                activeColor = IosGreen,
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp),
            thickness = 0.5.dp,
            color = IosDivider,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TelemetryItem(
                label = "Download",
                rate = downlink,
                total = downlinkTotal,
                accentColor = IosGreen,
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.width(16.dp))

            TelemetryItem(
                label = "Upload",
                rate = uplink,
                total = uplinkTotal,
                accentColor = TelegramBlue,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TelemetryItem(
    label: String,
    rate: String,
    total: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0AFFFFFF))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accentColor),
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = IosTextSecondary,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = rate,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = IosTextPrimary,
        )

        Text(
            text = "$total total",
            fontSize = 12.sp,
            color = IosTextSecondary,
        )
    }
}
