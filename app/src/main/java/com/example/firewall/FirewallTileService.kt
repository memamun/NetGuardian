package com.example.firewall

import android.content.Intent
import android.graphics.drawable.Icon
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.MainActivity
import com.example.R

@RequiresApi(Build.VERSION_CODES.N)
class FirewallTileService : TileService() {

    private lateinit var stateRepo: FirewallStateRepository
    private lateinit var firewallManager: FirewallManager

    override fun onCreate() {
        super.onCreate()
        stateRepo = FirewallStateRepository.getInstance(applicationContext)
        firewallManager = FirewallManager.getInstance(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val snapshot = stateRepo.state.value

        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_firewall)
        tile.label = getString(R.string.quick_settings_tile_firewall)

        when (snapshot.state) {
            FirewallState.RUNNING -> {
                tile.state = Tile.STATE_ACTIVE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = if (snapshot.blockedAppsCount > 0) {
                        "${snapshot.blockedAppsCount} restricted"
                    } else {
                        "Active"
                    }
                }
            }
            FirewallState.STARTING -> {
                tile.state = Tile.STATE_ACTIVE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Starting..."
                }
            }
            FirewallState.PAUSED -> {
                tile.state = Tile.STATE_INACTIVE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Paused"
                }
            }
            FirewallState.ERROR -> {
                tile.state = Tile.STATE_INACTIVE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Error"
                }
            }
            FirewallState.VPN_PERMISSION_REQUIRED -> {
                tile.state = Tile.STATE_INACTIVE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Permission needed"
                }
            }
            FirewallState.STOPPED -> {
                tile.state = Tile.STATE_INACTIVE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Off"
                }
            }
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val snapshot = stateRepo.state.value

        when (snapshot.state) {
            FirewallState.RUNNING, FirewallState.STARTING -> {
                // User requests stop
                firewallManager.stopFirewall(this)
                updateTileState()
            }
            FirewallState.PAUSED -> {
                // Resume firewall
                val resumeIntent = Intent(this, FirewallVpnService::class.java).apply {
                    action = FirewallVpnService.ACTION_RESUME
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(resumeIntent)
                } else {
                    startService(resumeIntent)
                }
                updateTileState()
            }
            else -> {
                // OFF / ERROR / STOPPED -> Check VPN Authorization first
                val prepareIntent = VpnService.prepare(this)
                if (prepareIntent == null) {
                    // Authorized! Start firewall
                    firewallManager.startFirewall(this)
                    qsTile?.let {
                        it.state = Tile.STATE_ACTIVE
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            it.subtitle = "Starting..."
                        }
                        it.updateTile()
                    }
                } else {
                    // Not authorized: open MainActivity onboarding/setup
                    stateRepo.setVpnPermissionRequired()
                    updateTileState()

                    val launchIntent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra(FirewallNotificationManager.EXTRA_ROUTE, "dashboard")
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startActivityAndCollapse(
                            android.app.PendingIntent.getActivity(
                                this,
                                10,
                                launchIntent,
                                android.app.PendingIntent.FLAG_IMMUTABLE
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        startActivityAndCollapse(launchIntent)
                    }
                }
            }
        }
    }
}
