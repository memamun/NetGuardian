package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppItem
import com.example.data.AppRepository
import com.example.data.PreferencesRepository
import com.example.data.QuickMode
import com.example.data.UpstreamDnsType
import com.example.data.UserPreferences
import com.example.database.AppBlockedStat
import com.example.database.AppDatabase
import com.example.database.AppRuleEntity
import com.example.database.BlocklistEntity
import com.example.database.ConnectionLogEntity
import com.example.firewall.FirewallManager
import com.example.firewall.NetworkType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CancellationException
import androidx.room.withTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

enum class AppFilter {
    ALL,
    USER,
    SYSTEM,
    BLOCKED,
    RESTRICTED
}

enum class LogFilter {
    ALL,
    BLOCKED,
    ALLOWED
}

sealed interface RootNavState {
    data object Loading : RootNavState
    data object Onboarding : RootNavState
    data object MainApp : RootNavState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _operationError = MutableStateFlow<String?>(null)
    val operationError: StateFlow<String?> = _operationError.asStateFlow()
    fun dismissOperationError() { _operationError.value = null }
    private val storageErrors = CoroutineExceptionHandler { _, _ ->
        isAppsLoading.value = false
        _operationError.value = "Could not read or save your settings. Free some device storage and reopen NetGuardian to retry."
        if (_rootNavState.value == RootNavState.Loading) _rootNavState.value = RootNavState.Onboarding
    }

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.firewallDao()
    private val appRepo = AppRepository(application)
    private val prefsRepo = PreferencesRepository(application)
    private val firewallManager = FirewallManager.getInstance(application)
    private val stateRepo = com.example.firewall.FirewallStateRepository.getInstance(application)

    val firewallActive: StateFlow<Boolean> = firewallManager.isFirewallActive
    val firewallSnapshot: StateFlow<com.example.firewall.FirewallSnapshot> = stateRepo.state
    val networkType: StateFlow<NetworkType> = firewallManager.networkType
    val isScreenOn: StateFlow<Boolean> = firewallManager.isScreenOn
    val isDeviceIdle: StateFlow<Boolean> = firewallManager.isDeviceIdle
    val activeQuickMode: StateFlow<QuickMode> = firewallManager.activeQuickMode

    private val _rootNavState = MutableStateFlow<RootNavState>(
        if (prefsRepo.isOnboardingCompletedSync()) {
            RootNavState.MainApp
        } else {
            RootNavState.Loading
        }
    )
    val rootNavState: StateFlow<RootNavState> = _rootNavState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            try {
                if (dao.getAllBlocklistSync().isEmpty()) {
                    AppDatabase.populateDefaultBlocklists(dao)
                }
            } catch (_: Exception) {}
        }
        viewModelScope.launch(storageErrors) {
            prefsRepo.userPreferencesFlow.collect { prefs ->
                if (_rootNavState.value != RootNavState.MainApp) {
                    _rootNavState.value = if (prefs.onboardingCompleted) {
                        RootNavState.MainApp
                    } else {
                        RootNavState.Onboarding
                    }
                }
            }
        }
    }

    val userPreferences: StateFlow<UserPreferences> = prefsRepo.userPreferencesFlow
        .stateIn(
            scope = kotlinx.coroutines.CoroutineScope(viewModelScope.coroutineContext + storageErrors),
            started = SharingStarted.Eagerly,
            initialValue = UserPreferences(
                onboardingCompleted = prefsRepo.isOnboardingCompletedSync()
            )
        )

    val isAppsLoading = MutableStateFlow(true)

    // Installed Apps
    val installedApps: StateFlow<List<AppItem>> = appRepo.appsFlow
        .onEach { isAppsLoading.value = false }
        .stateIn(
            scope = kotlinx.coroutines.CoroutineScope(viewModelScope.coroutineContext + storageErrors),
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val searchQuery = MutableStateFlow("")
    val selectedFilter = MutableStateFlow(AppFilter.ALL)

    val filteredApps: StateFlow<List<AppItem>> = combine(
        installedApps,
        searchQuery,
        selectedFilter
    ) { apps, query, filter ->
        apps.filter { app ->
            val matchesQuery = query.isBlank() ||
                    app.appName.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                AppFilter.ALL -> true
                AppFilter.USER -> !app.isSystemApp
                AppFilter.SYSTEM -> app.isSystemApp
                AppFilter.BLOCKED -> app.rule.isBlocked
                AppFilter.RESTRICTED -> app.rule.isBlocked || app.rule.blockWifi || app.rule.blockMobile || app.rule.blockBackground || app.rule.blockScreenOff
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(
        scope = kotlinx.coroutines.CoroutineScope(viewModelScope.coroutineContext + storageErrors),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val blockedAppsCount: StateFlow<Int> = installedApps.combine(MutableStateFlow(0)) { apps, _ ->
        apps.count { it.rule.isBlocked || it.rule.blockWifi || it.rule.blockMobile }
    }.stateIn(kotlinx.coroutines.CoroutineScope(viewModelScope.coroutineContext + storageErrors), SharingStarted.WhileSubscribed(5000), 0)

    val allowedAppsCount: StateFlow<Int> = installedApps.combine(MutableStateFlow(0)) { apps, _ ->
        apps.count { !it.rule.isBlocked && !it.rule.blockWifi && !it.rule.blockMobile }
    }.stateIn(kotlinx.coroutines.CoroutineScope(viewModelScope.coroutineContext + storageErrors), SharingStarted.WhileSubscribed(5000), 0)

    // Connection Logs
    val logFilter = MutableStateFlow(LogFilter.ALL)
    val logSearchQuery = MutableStateFlow("")

    val recentLogs: StateFlow<List<ConnectionLogEntity>> = dao.getRecentLogs(200).stateIn(
        scope = kotlinx.coroutines.CoroutineScope(viewModelScope.coroutineContext + storageErrors),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentLogsPreview: StateFlow<List<ConnectionLogEntity>> = dao.getRecentLogs(4).stateIn(
        scope = kotlinx.coroutines.CoroutineScope(viewModelScope.coroutineContext + storageErrors),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredLogs: StateFlow<List<ConnectionLogEntity>> = combine(
        recentLogs,
        logFilter,
        logSearchQuery
    ) { logs, filter, query ->
        logs.filter { log ->
            val matchesFilter = when (filter) {
                LogFilter.ALL -> true
                LogFilter.BLOCKED -> log.isBlocked
                LogFilter.ALLOWED -> !log.isBlocked
            }
            val matchesQuery = query.isBlank() ||
                    log.destinationHost.contains(query, ignoreCase = true) ||
                    log.appName.contains(query, ignoreCase = true) ||
                    log.packageName.contains(query, ignoreCase = true)

            matchesFilter && matchesQuery
        }
    }.stateIn(
        scope = kotlinx.coroutines.CoroutineScope(viewModelScope.coroutineContext + storageErrors),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Privacy Blocklists
    val blocklist: StateFlow<List<BlocklistEntity>> = dao.getAllBlocklist().stateIn(
        scope = kotlinx.coroutines.CoroutineScope(viewModelScope.coroutineContext + storageErrors),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Today's Stats
    private val startOfDayMillis: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    val todayBlockedCount: StateFlow<Int> = dao.getTodayBlockedCount(startOfDayMillis).stateIn(
        scope = kotlinx.coroutines.CoroutineScope(viewModelScope.coroutineContext + storageErrors),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val todayAllowedCount: StateFlow<Int> = dao.getTodayAllowedCount(startOfDayMillis).stateIn(
        scope = kotlinx.coroutines.CoroutineScope(viewModelScope.coroutineContext + storageErrors),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val topBlockedApps: StateFlow<List<AppBlockedStat>> = dao.getTopBlockedApps(5).stateIn(
        scope = kotlinx.coroutines.CoroutineScope(viewModelScope.coroutineContext + storageErrors),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Actions
    fun toggleFirewall(context: Context): Intent? {
        return firewallManager.toggleFirewall(context)
    }

    fun startFirewall(context: Context) {
        firewallManager.startFirewall(context)
    }

    fun stopFirewall(context: Context) {
        firewallManager.stopFirewall(context)
    }

    fun setQuickMode(mode: QuickMode) {
        firewallManager.setQuickMode(mode)
    }

    fun updateAppRule(rule: AppRuleEntity) {
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            appRepo.updateRule(rule)
            firewallManager.notifyRulesChanged()
        }
    }

    fun toggleAppBlock(rule: AppRuleEntity) {
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            appRepo.toggleBlock(rule)
            firewallManager.notifyRulesChanged()
        }
    }

    fun toggleAppWifi(rule: AppRuleEntity) {
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            appRepo.toggleWifi(rule)
            firewallManager.notifyRulesChanged()
        }
    }

    fun toggleAppMobile(rule: AppRuleEntity) {
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            appRepo.toggleMobile(rule)
            firewallManager.notifyRulesChanged()
        }
    }

    fun toggleAppBackground(rule: AppRuleEntity) {
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            appRepo.toggleBackground(rule)
            firewallManager.notifyRulesChanged()
        }
    }

    fun toggleAppScreenOff(rule: AppRuleEntity) {
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            appRepo.toggleScreenOff(rule)
            firewallManager.notifyRulesChanged()
        }
    }

    fun toggleAppDeviceIdle(rule: AppRuleEntity) {
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            appRepo.toggleDeviceIdle(rule)
            firewallManager.notifyRulesChanged()
        }
    }

    fun toggleAppTrackers(rule: AppRuleEntity) {
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            appRepo.toggleTrackers(rule)
            firewallManager.notifyRulesChanged()
        }
    }

    fun blockAllNonSystem(block: Boolean) {
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            appRepo.blockAllNonSystem(block)
            firewallManager.notifyRulesChanged()
        }
    }

    fun allowAllApps() {
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            appRepo.allowAllApps()
            firewallManager.notifyRulesChanged()
        }
    }

    fun setBlockAllBackground(block: Boolean) {
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            appRepo.setBlockAllBackground(block)
            firewallManager.notifyRulesChanged()
        }
    }

    fun setBlockAllScreenOff(block: Boolean) {
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            appRepo.setBlockAllScreenOff(block)
            firewallManager.notifyRulesChanged()
        }
    }

    fun setBlockAllDeviceIdle(block: Boolean) {
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            appRepo.setBlockAllDeviceIdle(block)
            firewallManager.notifyRulesChanged()
        }
    }

    fun setUpstreamDnsType(type: UpstreamDnsType) {
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            prefsRepo.setUpstreamDnsType(type)
            firewallManager.notifyRulesChanged()
        }
    }

    fun setCustomDnsIp(ip: String) {
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            prefsRepo.setCustomDnsIp(ip)
            firewallManager.notifyRulesChanged()
        }
    }

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            dao.clearAllLogs()
        }
    }

    // Domain validation feedback for the UI
    private val _domainValidationError = MutableStateFlow<String?>(null)
    val domainValidationError: StateFlow<String?> = _domainValidationError.asStateFlow()

    fun clearDomainValidationError() {
        _domainValidationError.value = null
    }

    fun addCustomBlocklistDomain(domain: String) {
        val result = com.example.dns.DomainValidator.validate(domain)
        when (result) {
            is com.example.dns.DomainValidator.ValidationResult.Invalid -> {
                _domainValidationError.value = result.errorMessage
            }
            is com.example.dns.DomainValidator.ValidationResult.Valid -> {
                _domainValidationError.value = null
                viewModelScope.launch(Dispatchers.IO + storageErrors) {
                    dao.insertBlocklist(
                        BlocklistEntity(
                            domain = result.normalizedDomain,
                            category = "CUSTOM",
                            isEnabled = true
                        )
                    )
                    firewallManager.notifyRulesChanged()
                }
            }
        }
    }

    fun toggleBlocklistItem(id: Long, isEnabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            dao.toggleBlocklistItem(id, isEnabled)
            firewallManager.notifyRulesChanged()
        }
    }

    fun deleteBlocklistItem(id: Long) {
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            dao.deleteBlocklistItem(id)
            firewallManager.notifyRulesChanged()
        }
    }

    fun toggleCategory(category: String, isEnabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            dao.toggleCategory(category, isEnabled)
            firewallManager.notifyRulesChanged()
        }
    }

    fun setStartOnBoot(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO + storageErrors) { prefsRepo.setStartOnBoot(enabled) }
    }

    fun setPersistentNotification(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO + storageErrors) { prefsRepo.setPersistentNotification(enabled) }
    }

    fun setBlockNewApps(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO + storageErrors) { prefsRepo.setBlockNewAppsByDefault(enabled) }
    }

    fun setDnsFiltering(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            prefsRepo.setDnsFilteringEnabled(enabled)
            firewallManager.notifyRulesChanged()
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch(Dispatchers.IO + storageErrors) { prefsRepo.setThemeMode(mode) }
    }

    fun resetRulesToDefault() {
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            db.withTransaction {
                dao.resetAllRulesToAllow()
                dao.clearCustomBlocklist()
            }
            prefsRepo.setQuickMode(QuickMode.NORMAL)
            firewallManager.notifyRulesChanged()
        }
    }

    suspend fun exportRulesJson(): String = withContext(Dispatchers.IO) {
        val rules = dao.getAllRulesSync()
        val array = JSONArray()
        for (r in rules) {
            val obj = JSONObject().apply {
                put("packageName", r.packageName)
                put("appName", r.appName)
                put("isBlocked", r.isBlocked)
                put("blockWifi", r.blockWifi)
                put("blockMobile", r.blockMobile)
                put("blockBackground", r.blockBackground)
                put("blockScreenOff", r.blockScreenOff)
                put("blockDeviceIdle", r.blockDeviceIdle)
                put("blockTrackers", r.blockTrackers)
            }
            array.put(obj)
        }
        val root = JSONObject().apply {
            put("app", "NetGuardian")
            put("version", 2)
            put("timestamp", System.currentTimeMillis())
            put("rules", array)
        }
        root.toString(2)
    }

    suspend fun importRulesJson(jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (jsonStr.length > 1_000_000) return@withContext false
            val root = JSONObject(jsonStr)
            if (root.optString("app") != "NetGuardian" || root.optInt("version") !in 1..2) return@withContext false
            val array = root.getJSONArray("rules")
            if (array.length() > 10000) return@withContext false
            val list = mutableListOf<AppRuleEntity>()
            val seen = mutableSetOf<String>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val packageName = obj.getString("packageName")
                if (packageName.length > 255 || !packageName.matches(Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)*")) || !seen.add(packageName)) return@withContext false
                val existing = dao.getRuleSync(packageName) ?: continue
                list.add(
                    existing.copy(
                        isBlocked = obj.optBoolean("isBlocked", false),
                        blockWifi = obj.optBoolean("blockWifi", false),
                        blockMobile = obj.optBoolean("blockMobile", false),
                        blockBackground = obj.optBoolean("blockBackground", false),
                        blockScreenOff = obj.optBoolean("blockScreenOff", false),
                        blockDeviceIdle = obj.optBoolean("blockDeviceIdle", false),
                        blockTrackers = obj.optBoolean("blockTrackers", true)
                    )
                )
            }
            if (list.isNotEmpty()) {
                dao.insertRules(list)
                firewallManager.notifyRulesChanged()
                true
            } else false
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    // Onboarding & Diagnostics
    private val _systemDiagnostics = MutableStateFlow<List<com.example.data.DiagnosticCheck>>(emptyList())
    val systemDiagnostics: StateFlow<List<com.example.data.DiagnosticCheck>> = _systemDiagnostics.asStateFlow()

    fun runDiagnostics(context: Context) {
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            val isDbAccessible = try {
                dao.getAllBlocklistSync()
                true
            } catch (_: Exception) {
                false
            }
            val checks = com.example.data.DiagnosticsHelper.runFullDiagnostics(
                context = context,
                isFirewallRunning = firewallActive.value,
                dbAccessible = isDbAccessible
            )
            _systemDiagnostics.value = checks
        }
    }

    fun completeOnboarding(policy: String = "ALLOW_ALL") {
        _rootNavState.value = RootNavState.MainApp
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            prefsRepo.setOnboardingCompleted(true)
            prefsRepo.setDefaultRulePolicy(policy)
            if (policy == "BLOCK_NEW") {
                prefsRepo.setBlockNewAppsByDefault(true)
            } else if (policy == "ALLOW_ALL") {
                appRepo.allowAllApps()
                firewallManager.notifyRulesChanged()
            }
        }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        if (completed) {
            _rootNavState.value = RootNavState.MainApp
        }
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            prefsRepo.setOnboardingCompleted(completed)
        }
    }

    fun setHasSeenAppManagerExplanation(seen: Boolean) {
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            prefsRepo.setHasSeenAppManagerExplanation(seen)
        }
    }

    fun setHasConfirmedFirewallStartup(confirmed: Boolean) {
        viewModelScope.launch(Dispatchers.IO + storageErrors) {
            prefsRepo.setHasConfirmedFirewallStartup(confirmed)
        }
    }
}
