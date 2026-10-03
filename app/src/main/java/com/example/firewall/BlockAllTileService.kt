package com.example.firewall

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.MainActivity
import com.example.R
import com.example.data.PreferencesRepository
import com.example.data.QuickMode
import com.example.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

@RequiresApi(Build.VERSION_CODES.N)
class BlockAllTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, _ ->
        stateRepo.setError("Could not update firewall mode. Open NetGuardian to retry.")
    })
    private lateinit var stateRepo: FirewallStateRepository
    private lateinit var prefsRepo: PreferencesRepository
    private lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        stateRepo = FirewallStateRepository.getInstance(applicationContext)
        prefsRepo = PreferencesRepository(applicationContext)
        database = AppDatabase.getDatabase(applicationContext)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val snapshot = stateRepo.state.value

        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_block_all)
        tile.label = getString(R.string.quick_settings_tile_block_all)

        if (snapshot.isBlockAllActive) {
            tile.state = Tile.STATE_ACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Strict: Active"
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Inactive"
            }
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val snapshot = stateRepo.state.value
        val shouldEnable = !snapshot.isBlockAllActive

        scope.launch {
            if (shouldEnable) {
                // Apply strict emergency mode
                stateRepo.setBlockAllMode(true)
                prefsRepo.setQuickMode(QuickMode.BLOCK_NON_SYSTEM)
            } else {
                // Restore standard policy
                stateRepo.setBlockAllMode(false)
                prefsRepo.setQuickMode(QuickMode.NORMAL)
            }

            // Notify firewall service to re-evaluate rules immediately
            val intent = Intent(applicationContext, FirewallVpnService::class.java).apply {
                action = FirewallVpnService.ACTION_RELOAD_RULES
            }
            FirewallManager.getInstance(applicationContext).notifyRulesChanged()

            stateRepo.requestAllTilesRefresh()
        }
    }
}
