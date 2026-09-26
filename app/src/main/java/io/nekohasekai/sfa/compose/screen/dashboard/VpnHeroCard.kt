package io.nekohasekai.sfa.compose.screen.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.compose.component.UptimeText
import io.nekohasekai.sfa.compose.theme.AccentCyan
import io.nekohasekai.sfa.compose.theme.AccentEmerald
import io.nekohasekai.sfa.compose.theme.ServiceRunning
import io.nekohasekai.sfa.constant.Status

@Composable
fun VpnHeroCard(
    status: Status,
    startTime: Long?,
    hasProfile: Boolean,
    selectedProfileName: String?,
    onToggle: () -> Unit,
    onSelectProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRunning = status == Status.Started
    val isStarting = status == Status.Starting
    val isStopping = status == Status.Stopping

    val ringBorderColor by animateColorAsState(
        targetValue = when {
            isRunning -> AccentEmerald
            isStarting -> AccentCyan
            else -> MaterialTheme.colorScheme.outline
        },
        label = "heroBorderColor",
    )

    val iconColor by animateColorAsState(
        targetValue = when {
            isRunning -> AccentEmerald
            isStarting -> AccentCyan
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "heroIconColor",
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                onClick = onSelectProfile,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasProfile) AccentEmerald.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Public,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (hasProfile) AccentEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedProfileName ?: stringResource(R.string.not_selected),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = if (isRunning) "Connected Endpoint" else "Tap to change server",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.expand),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp),
            ) {
                if (isRunning) {
                    Box(
                        modifier = Modifier
                            .size(158.dp)
                            .clip(CircleShape)
                            .background(AccentEmerald.copy(alpha = 0.06f)),
                    )
                }

                Surface(
                    onClick = onToggle,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(3.dp, ringBorderColor),
                    tonalElevation = 0.dp,
                    modifier = Modifier.size(136.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(136.dp),
                    ) {
                        if (isStarting || isStopping) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(56.dp),
                                color = ringBorderColor,
                                strokeWidth = 3.5.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = if (isRunning) stringResource(R.string.stop) else stringResource(R.string.action_start),
                                modifier = Modifier.size(54.dp),
                                tint = iconColor,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = when {
                    isRunning -> "CONNECTED"
                    isStarting -> "CONNECTING..."
                    isStopping -> "DISCONNECTING..."
                    else -> "TAP TO CONNECT"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = when {
                    isRunning -> AccentEmerald
                    isStarting -> AccentCyan
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isRunning && startTime != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ServiceRunning),
                        )
                        UptimeText(startTime = startTime)
                    }
                }
            } else {
                Text(
                    text = if (hasProfile) "Fast & Secure Connection" else "Select location to start",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 0.5.sp,
                )
            }
        }
    }
}
