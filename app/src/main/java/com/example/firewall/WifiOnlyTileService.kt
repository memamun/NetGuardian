package com.example.firewall

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.R
import com.example.data.PreferencesRepository
import com.example.data.QuickMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
class WifiOnlyTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var stateRepo: FirewallStateRepository
    private lateinit var prefsRepo: PreferencesRepository

    override fun onCreate() {
        super.onCreate()
        stateRepo = FirewallStateRepository.getInstance(applicationContext)
        prefsRepo = PreferencesRepository(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun hasCellularCapability(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val snapshot = stateRepo.state.value

        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_wifi_only)
        tile.label = getString(R.string.quick_settings_tile_wifi_only)

        if (!hasCellularCapability()) {
            tile.state = Tile.STATE_UNAVAILABLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Unsupported (No Cellular)"
            }
            tile.updateTile()
            return
        }

        if (snapshot.isWifiOnlyActive) {
            tile.state = Tile.STATE_ACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Mobile Data Blocked"
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "All Transports"
            }
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (!hasCellularCapability()) {
            return
        }

        val snapshot = stateRepo.state.value
        val shouldEnable = !snapshot.isWifiOnlyActive

        scope.launch {
            stateRepo.setWifiOnlyMode(shouldEnable)
            val newQuickMode = if (shouldEnable) QuickMode.WIFI_ONLY else QuickMode.NORMAL
            prefsRepo.setQuickMode(newQuickMode)

            val intent = Intent(applicationContext, FirewallVpnService::class.java).apply {
                action = FirewallVpnService.ACTION_RELOAD_RULES
            }
            applicationContext.startService(intent)

            stateRepo.requestAllTilesRefresh()
        }
    }
}
