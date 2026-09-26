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
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.UnfoldMore
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
import io.nekohasekai.sfa.compose.theme.ModexBlue
import io.nekohasekai.sfa.compose.theme.ModexBlueLight
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

    val borderColor by animateColorAsState(
        targetValue = when {
            isRunning -> ServiceRunning
            isStarting -> ModexBlueLight
            else -> MaterialTheme.colorScheme.primary
        },
        label = "heroBorderColor",
    )

    val iconColor by animateColorAsState(
        targetValue = when {
            isRunning -> ServiceRunning
            isStarting -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.primary
        },
        label = "heroIconColor",
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
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
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.InsertDriveFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (hasProfile) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = selectedProfileName ?: stringResource(R.string.not_selected),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (hasProfile) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Default.UnfoldMore,
                        contentDescription = stringResource(R.string.expand),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Surface(
                onClick = onToggle,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(2.5.dp, borderColor),
                tonalElevation = 0.dp,
                modifier = Modifier.size(132.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(132.dp),
                ) {
                    if (isStarting || isStopping) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(56.dp),
                            color = borderColor,
                            strokeWidth = 3.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = if (isRunning) stringResource(R.string.stop) else stringResource(R.string.action_start),
                            modifier = Modifier.size(52.dp),
                            tint = iconColor,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = when {
                    isRunning -> stringResource(R.string.status_started)
                    isStarting -> stringResource(R.string.status_starting)
                    isStopping -> stringResource(R.string.status_stopping)
                    else -> stringResource(R.string.status_stopped)
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = when {
                    isRunning -> ServiceRunning
                    isStarting -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (isRunning && startTime != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
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
                    text = if (hasProfile) stringResource(R.string.action_start) else stringResource(R.string.not_selected),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 0.5.sp,
                )
            }
        }
    }
}
