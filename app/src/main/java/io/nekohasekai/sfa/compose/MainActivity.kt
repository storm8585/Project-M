package io.nekohasekai.sfa.compose

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import androidx.lifecycle.lifecycleScope
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.sfa.Application
import io.nekohasekai.sfa.bg.ServiceConnection
import io.nekohasekai.sfa.bg.ServiceNotification
import io.nekohasekai.sfa.compose.base.GlobalEventBus
import io.nekohasekai.sfa.compose.base.UiEvent
import io.nekohasekai.sfa.compose.screen.dashboard.DashboardScreen
import io.nekohasekai.sfa.compose.screen.dashboard.DashboardViewModel
import io.nekohasekai.sfa.compose.theme.Theme
import io.nekohasekai.sfa.constant.Alert
import io.nekohasekai.sfa.constant.ServiceMode
import io.nekohasekai.sfa.constant.Status
import io.nekohasekai.sfa.database.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity :
    AppCompatActivity(),
    ServiceConnection.Callback {

    private val connection = ServiceConnection(this, this)
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private var notificationPermissionRequested = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            startService()
        }

    private val prepareLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                startService0()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ConfigurationCompat.getLocales(resources.configuration)[0]?.let { locale ->
            runCatching {
                Libbox.setLocale(locale.toLanguageTag())
            }.onFailure {
                Log.d("MainActivity", "set locale: ${it.message}")
            }
        }

        enableEdgeToEdge()

        lifecycleScope.launch {
            Settings.dataStore.initialize()
            connection.reconnect()

            setContent {
                Theme {
                    val uiState by dashboardViewModel.uiState.collectAsState()
                    DashboardScreen(
                        serviceStatus = uiState.serviceStatus,
                        viewModel = dashboardViewModel,
                    )
                }
            }
        }

        lifecycleScope.launch {
            GlobalEventBus.events.collect { event ->
                when (event) {
                    is UiEvent.RequestStartService -> startService()
                    is UiEvent.RequestReconnectService -> connection.reconnect()
                    is UiEvent.ErrorMessage -> {
                        Toast.makeText(this@MainActivity, event.message, Toast.LENGTH_LONG).show()
                    }
                    else -> Unit
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        connection.disconnect()
    }

    override fun onServiceStatusChanged(status: Status) {
        dashboardViewModel.updateServiceStatus(status)
    }

    override fun onServiceAlert(type: Alert, message: String?) {
        when (type) {
            Alert.RequestVPNPermission -> {
                lifecycleScope.launch { prepare() }
            }
            Alert.RequestNotificationPermission -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            else -> Unit
        }
    }

    @SuppressLint("NewApi")
    fun startService() {
        lifecycleScope.launch {
            Settings.dataStore.initialize()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !ServiceNotification.checkPermission()) {
                if (!notificationPermissionRequested) {
                    notificationPermissionRequested = true
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    return@launch
                }
            }
            startService0()
        }
    }

    private fun startService0() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (Settings.rebuildServiceMode()) {
                withContext(Dispatchers.Main) {
                    connection.reconnect()
                }
            }
            if (Settings.serviceMode == ServiceMode.VPN) {
                if (prepare()) {
                    return@launch
                }
            }
            val intent = Intent(Application.application, Settings.serviceClass())
            withContext(Dispatchers.Main) {
                ContextCompat.startForegroundService(this@MainActivity, intent)
            }
            Settings.startedByUser = true
        }
    }

    private suspend fun prepare(): Boolean = withContext(Dispatchers.Main) {
        try {
            val intent = VpnService.prepare(this@MainActivity)
            if (intent != null) {
                prepareLauncher.launch(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            true
        }
    }
}
