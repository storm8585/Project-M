package io.nekohasekai.sfa.compose.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import io.nekohasekai.sfa.compose.theme.IosGreen

@Composable
fun IosSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = IosGreen,
    inactiveColor: Color = Color(0xFF39393D),
) {
    val haptic = LocalHapticFeedback.current

    val trackColor by animateColorAsState(
        targetValue = if (checked) activeColor else inactiveColor,
        animationSpec = spring(stiffness = 500f),
        label = "iosSwitchTrackColor",
    )

    val thumbProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 600f),
        label = "iosSwitchThumbProgress",
    )

    Canvas(
        modifier = modifier
            .requiredSize(width = 51.dp, height = 31.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(!checked)
            },
    ) {
        val width = size.width
        val height = size.height
        val cornerRadius = height / 2f

        drawRoundRect(
            color = trackColor,
            topLeft = Offset.Zero,
            size = Size(width, height),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Fill,
        )

        val padding = 2.dp.toPx()
        val thumbRadius = (height - (padding * 2f)) / 2f
        val startX = padding + thumbRadius
        val endX = width - padding - thumbRadius
        val currentX = startX + (endX - startX) * thumbProgress
        val centerY = height / 2f

        drawCircle(
            color = Color(0x33000000),
            radius = thumbRadius + 1.dp.toPx(),
            center = Offset(currentX, centerY + 1.dp.toPx()),
        )

        drawCircle(
            color = Color.White,
            radius = thumbRadius,
            center = Offset(currentX, centerY),
        )
    }
}
