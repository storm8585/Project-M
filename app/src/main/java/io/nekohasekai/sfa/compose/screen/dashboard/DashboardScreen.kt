package io.nekohasekai.sfa.compose.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.nekohasekai.sfa.compose.component.HeroConnectionCard
import io.nekohasekai.sfa.compose.component.InsetGroup
import io.nekohasekai.sfa.compose.component.ServerItemRow
import io.nekohasekai.sfa.compose.theme.IosBackground
import io.nekohasekai.sfa.compose.theme.IosTextPrimary
import io.nekohasekai.sfa.compose.theme.IosTextSecondary
import io.nekohasekai.sfa.constant.Status

@Composable
fun DashboardScreen(
    serviceStatus: Status,
    viewModel: DashboardViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IosBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 4.dp),
        ) {
            Text(
                text = "Modex",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.36.sp,
                color = IosTextPrimary,
            )
            Text(
                text = "Network Tunnel",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = IosTextSecondary,
            )
        }

        HeroConnectionCard(
            status = serviceStatus,
            downlink = uiState.downlink,
            downlinkTotal = uiState.downlinkTotal,
            uplink = uiState.uplink,
            uplinkTotal = uiState.uplinkTotal,
            onToggle = { viewModel.toggleService() },
        )

        InsetGroup(
            header = "Servers",
            footer = "Select a server to route network connections.",
        ) {
            if (uiState.profiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No servers configured",
                        fontSize = 15.sp,
                        color = IosTextSecondary,
                    )
                }
            } else {
                uiState.profiles.forEachIndexed { index, profile ->
                    ServerItemRow(
                        name = profile.name,
                        isSelected = profile.id == uiState.selectedProfileId,
                        showDivider = index < uiState.profiles.lastIndex,
                        onClick = { viewModel.selectProfile(profile.id) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
