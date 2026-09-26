package io.nekohasekai.sfa.compose.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.compose.base.UiEvent
import io.nekohasekai.sfa.compose.navigation.NewProfileArgs
import io.nekohasekai.sfa.compose.navigation.Screen
import io.nekohasekai.sfa.compose.theme.AccentEmerald
import io.nekohasekai.sfa.compose.topbar.LocalScaffoldPadding
import io.nekohasekai.sfa.compose.topbar.OverrideTopBar
import io.nekohasekai.sfa.constant.Status
import io.nekohasekai.sfa.utils.RemoteControlManager

data class CardRenderItem(val cards: List<CardGroup>, val isRow: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    serviceStatus: Status,
    showStartFab: Boolean,
    showStatusBar: Boolean,
    onOpenNewProfile: (NewProfileArgs) -> Unit,
    viewModel: DashboardViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val remoteServer by RemoteControlManager.remoteServer.collectAsState()
    val remoteConnected by RemoteControlManager.isConnected.collectAsState()
    val isRemote = remoteServer != null
    val isConnected = serviceStatus == Status.Started

    OverrideTopBar {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "MODEX",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) AccentEmerald else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
            actions = {
                IconButton(onClick = { viewModel.sendGlobalEvent(UiEvent.Navigate(Screen.Settings.route)) }) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.title_settings),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
    }

    if (uiState.showProfilePickerSheet) {
        LocationSelectorSheet(
            profiles = uiState.profiles,
            selectedProfileId = uiState.selectedProfileId,
            onSelectProfile = { profile ->
                viewModel.selectProfile(profile.id)
            },
            onDismiss = {
                viewModel.hideProfilePickerSheet()
            },
        )
    }

    if (isRemote && !remoteConnected) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val scaffoldPadding = LocalScaffoldPadding.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(scaffoldPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
    ) {
        item(key = "vpn_hero") {
            VpnHeroCard(
                status = serviceStatus,
                startTime = uiState.serviceStartTime,
                hasProfile = uiState.selectedProfileId != -1L,
                selectedProfileName = uiState.selectedProfileName,
                onToggle = { viewModel.toggleService() },
                onSelectProfile = { viewModel.showProfilePickerSheet() },
            )
        }

        item(key = "vpn_stats") {
            VpnStatsCard(
                downlink = uiState.downlink,
                downlinkTotal = uiState.downlinkTotal,
                uplink = uiState.uplink,
                uplinkTotal = uiState.uplinkTotal,
                isConnected = isConnected,
            )
        }
    }
}
