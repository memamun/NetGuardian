package com.example.firewall

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.TileService
import android.util.Log
import com.example.data.PreferencesRepository
import com.example.data.QuickMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FirewallState {
    STOPPED,
    STARTING,
    RUNNING,
    PAUSED,
    ERROR,
    VPN_PERMISSION_REQUIRED
}

data class FirewallSnapshot(
    val state: FirewallState = FirewallState.STOPPED,
    val isBlockAllActive: Boolean = false,
    val isWifiOnlyActive: Boolean = false,
    val isMobileDataBlocked: Boolean = false,
    val pauseUntilTimestamp: Long? = null,
    val blockedAppsCount: Int = 0,
    val blockedConnectionsCount: Int = 0,
    val lastErrorMessage: String? = null,
    val activeNetwork: NetworkType = NetworkType.NONE,
    val dnsProtectionEffective: Boolean = false,
    val dnsStatusMessage: String? = null
)

class FirewallStateRepository private constructor(private val appContext: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob() + CoroutineExceptionHandler { _, _ ->
        setError("Could not load firewall settings. Reopen NetGuardian to retry.")
    })
    private val prefsRepo = PreferencesRepository(appContext)

    private val _state = MutableStateFlow(FirewallSnapshot())
    val state: StateFlow<FirewallSnapshot> = _state.asStateFlow()

    init {
        // Observe persistent preferences to initialize modes
        scope.launch {
            prefsRepo.userPreferencesFlow.collect { prefs ->
                _state.update { current ->
                    current.copy(
                        isBlockAllActive = prefs.activeQuickMode == QuickMode.BLOCK_NON_SYSTEM,
                        isWifiOnlyActive = prefs.activeQuickMode == QuickMode.WIFI_ONLY,
                        isMobileDataBlocked = prefs.activeQuickMode == QuickMode.WIFI_ONLY
                    )
                }
                requestAllTilesRefresh()
            }
        }
    }

    fun setStarting() {
        _state.update {
            it.copy(
                state = FirewallState.STARTING,
                lastErrorMessage = null
            )
        }
        requestAllTilesRefresh()
    }

    fun setRunning(
        blockedApps: Int = _state.value.blockedAppsCount,
        blockedConnections: Int = _state.value.blockedConnectionsCount,
        dnsEffective: Boolean = _state.value.dnsProtectionEffective
    ) {
        _state.update {
            it.copy(
                state = FirewallState.RUNNING,
                blockedAppsCount = blockedApps,
                blockedConnectionsCount = blockedConnections,
                lastErrorMessage = null,
                pauseUntilTimestamp = null,
                dnsProtectionEffective = dnsEffective
            )
        }
        requestAllTilesRefresh()
    }

    fun setStopped(errorMessage: String? = null) {
        val nextState = if (errorMessage != null) FirewallState.ERROR else FirewallState.STOPPED
        _state.update {
            it.copy(
                state = nextState,
                lastErrorMessage = errorMessage,
                pauseUntilTimestamp = null,
                dnsProtectionEffective = false,
                dnsStatusMessage = null
            )
        }
        requestAllTilesRefresh()
    }

    fun setPaused(untilTimestamp: Long) {
        _state.update {
            it.copy(
                state = FirewallState.PAUSED,
                dnsProtectionEffective = false,
                pauseUntilTimestamp = untilTimestamp
            )
        }
        requestAllTilesRefresh()
    }

    fun resumeFromPause() {
        if (_state.value.state == FirewallState.PAUSED) {
            _state.update {
                it.copy(
                    state = FirewallState.RUNNING,
                    pauseUntilTimestamp = null
                )
            }
            requestAllTilesRefresh()
        }
    }

    fun setError(message: String) {
        _state.update {
            it.copy(
                state = FirewallState.ERROR,
                dnsProtectionEffective = false,
                lastErrorMessage = message,
                pauseUntilTimestamp = null
            )
        }
        requestAllTilesRefresh()
    }

    fun setVpnPermissionRequired() {
        _state.update {
            it.copy(
                state = FirewallState.VPN_PERMISSION_REQUIRED
            )
        }
        requestAllTilesRefresh()
    }

    fun updateStats(blockedApps: Int, blockedConnections: Int) {
        _state.update {
            it.copy(
                blockedAppsCount = blockedApps,
                blockedConnectionsCount = blockedConnections
            )
        }
    }

    fun updateNetwork(networkType: NetworkType) {
        _state.update {
            it.copy(activeNetwork = networkType)
        }
    }

    fun setBlockAllMode(active: Boolean) {
        _state.update {
            it.copy(isBlockAllActive = active)
        }
        requestAllTilesRefresh()
    }

    fun setWifiOnlyMode(active: Boolean) {
        _state.update {
            it.copy(isWifiOnlyActive = active)
        }
        requestAllTilesRefresh()
    }

    fun setMobileDataBlocked(blocked: Boolean) {
        _state.update {
            it.copy(isMobileDataBlocked = blocked)
        }
        requestAllTilesRefresh()
    }

    fun requestAllTilesRefresh() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                TileService.requestListeningState(appContext, ComponentName(appContext, FirewallTileService::class.java))
                TileService.requestListeningState(appContext, ComponentName(appContext, BlockAllTileService::class.java))
                TileService.requestListeningState(appContext, ComponentName(appContext, WifiOnlyTileService::class.java))
                TileService.requestListeningState(appContext, ComponentName(appContext, MobileDataTileService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "Error requesting Quick Settings tile updates", e)
            }
        }
    }

    companion object {
        private const val TAG = "FirewallStateRepo"

        @Volatile
        private var INSTANCE: FirewallStateRepository? = null

        fun getInstance(context: Context): FirewallStateRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirewallStateRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
