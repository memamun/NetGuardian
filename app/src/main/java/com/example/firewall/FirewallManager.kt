package com.example.firewall

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import com.example.data.PreferencesRepository
import com.example.data.QuickMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FirewallManager private constructor(private val appContext: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val prefsRepo = PreferencesRepository(appContext)
    private val stateRepo = FirewallStateRepository.getInstance(appContext)

    private val _isFirewallActive = MutableStateFlow(false)
    val isFirewallActive: StateFlow<Boolean> = _isFirewallActive.asStateFlow()

    private val _networkType = MutableStateFlow(NetworkType.NONE)
    val networkType: StateFlow<NetworkType> = _networkType.asStateFlow()

    private val _isScreenOn = MutableStateFlow(true)
    val isScreenOn: StateFlow<Boolean> = _isScreenOn.asStateFlow()

    private val _isDeviceIdle = MutableStateFlow(false)
    val isDeviceIdle: StateFlow<Boolean> = _isDeviceIdle.asStateFlow()

    private val _activeQuickMode = MutableStateFlow(QuickMode.NORMAL)
    val activeQuickMode: StateFlow<QuickMode> = _activeQuickMode.asStateFlow()

    private val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    init {
        monitorNetworkState()

        scope.launch {
            prefsRepo.userPreferencesFlow.collect { prefs ->
                _activeQuickMode.value = prefs.activeQuickMode
                stateRepo.setBlockAllMode(prefs.activeQuickMode == QuickMode.BLOCK_NON_SYSTEM)
                stateRepo.setWifiOnlyMode(prefs.activeQuickMode == QuickMode.WIFI_ONLY)
                stateRepo.setMobileDataBlocked(prefs.activeQuickMode == QuickMode.MOBILE_ONLY)
            }
        }

        scope.launch {
            stateRepo.state.collect { snapshot ->
                _isFirewallActive.value = snapshot.state == FirewallState.RUNNING || snapshot.state == FirewallState.STARTING
            }
        }
    }

    private fun monitorNetworkState() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val newType = when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                    else -> NetworkType.NONE
                }
                _networkType.value = newType
                stateRepo.updateNetwork(newType)
            }

            override fun onLost(network: Network) {
                _networkType.value = NetworkType.NONE
                stateRepo.updateNetwork(NetworkType.NONE)
            }
        })

        // Initial check
        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        if (caps != null) {
            val initType = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                else -> NetworkType.NONE
            }
            _networkType.value = initType
            stateRepo.updateNetwork(initType)
        }
    }

    fun setFirewallActive(active: Boolean) {
        _isFirewallActive.value = active
        if (active) {
            stateRepo.setRunning()
        } else {
            stateRepo.setStopped()
        }
    }

    fun setScreenState(screenOn: Boolean) {
        _isScreenOn.value = screenOn
    }

    fun setDeviceIdleState(idle: Boolean) {
        _isDeviceIdle.value = idle
    }

    fun setNetworkType(type: NetworkType) {
        _networkType.value = type
        stateRepo.updateNetwork(type)
    }

    fun setQuickMode(mode: QuickMode) {
        _activeQuickMode.value = mode
        stateRepo.setBlockAllMode(mode == QuickMode.BLOCK_NON_SYSTEM)
        stateRepo.setWifiOnlyMode(mode == QuickMode.WIFI_ONLY)
        stateRepo.setMobileDataBlocked(mode == QuickMode.MOBILE_ONLY)
        scope.launch {
            prefsRepo.setQuickMode(mode)
        }
        notifyRulesChanged()
    }

    fun checkVpnPermission(context: Context): Intent? {
        return VpnService.prepare(context)
    }

    fun startFirewall(context: Context) {
        stateRepo.setStarting()
        val intent = Intent(context, FirewallVpnService::class.java).apply {
            action = FirewallVpnService.ACTION_START
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopFirewall(context: Context) {
        val intent = Intent(context, FirewallVpnService::class.java).apply {
            action = FirewallVpnService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun toggleFirewall(context: Context): Intent? {
        if (_isFirewallActive.value) {
            stopFirewall(context)
            return null
        } else {
            val prepareIntent = checkVpnPermission(context)
            if (prepareIntent != null) {
                stateRepo.setVpnPermissionRequired()
                return prepareIntent
            }
            startFirewall(context)
            return null
        }
    }

    fun notifyRulesChanged() {
        val intent = Intent(appContext, FirewallVpnService::class.java).apply {
            action = FirewallVpnService.ACTION_RELOAD_RULES
        }
        appContext.startService(intent)
    }

    companion object {
        @Volatile
        private var INSTANCE: FirewallManager? = null

        fun getInstance(context: Context): FirewallManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirewallManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
