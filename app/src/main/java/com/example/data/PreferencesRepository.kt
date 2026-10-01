package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "netguardian_prefs")

enum class QuickMode(val label: String) {
    NORMAL("Normal"),
    BLOCK_NON_SYSTEM("Block Non-System"),
    WIFI_ONLY("Wi-Fi Only"),
    MOBILE_ONLY("Mobile Data Only")
}

enum class UpstreamDnsType(val label: String, val primaryIp: String, val description: String) {
    SYSTEM_DEFAULT("System / Local Gateway", "10.1.10.1", "Uses your local Wi-Fi router or cellular ISP DNS"),
    ADGUARD("AdGuard DNS (Ad-Free)", "94.140.14.14", "Blocks ads, tracking, and telemetry across all queries"),
    CONTROLD("Control D (Ad & Tracker Blocking)", "76.76.2.2", "High-performance privacy resolver that filters ads"),
    CLOUDFLARE("Cloudflare DNS (1.1.1.1)", "1.1.1.1", "Optional public privacy resolver"),
    QUAD9("Quad9 (9.9.9.9)", "9.9.9.9", "Optional public malware-blocking resolver"),
    GOOGLE("Google DNS (8.8.8.8)", "8.8.8.8", "Optional public DNS resolver"),
    CUSTOM("Custom Upstream Resolver", "", "User-defined DNS server IP (e.g. your local Pi-hole)"),
    LOCAL_SINKHOLE("Local Loopback (Offline Only)", "127.0.0.1", "100% on-device offline sinkhole, no third-party server contacted")
}

data class UserPreferences(
    val firewallEnabled: Boolean = false,
    val startOnBoot: Boolean = false,
    val persistentNotification: Boolean = true,
    val blockNewAppsByDefault: Boolean = false,
    val dnsFilteringEnabled: Boolean = true,
    val blockTrackersGlobal: Boolean = true,
    val blockAdsGlobal: Boolean = true,
    val blockMalwareGlobal: Boolean = true,
    val upstreamDnsType: UpstreamDnsType = UpstreamDnsType.SYSTEM_DEFAULT,
    val customDnsIp: String = "192.168.1.1",
    val themeMode: String = "LIGHT", // "SYSTEM", "LIGHT", "DARK"
    val activeQuickMode: QuickMode = QuickMode.NORMAL,
    val onboardingCompleted: Boolean = false,
    val defaultRulePolicy: String = "ALLOW_ALL", // "ALLOW_ALL", "ASK_PER_APP", "BLOCK_NEW"
    val hasSeenAppManagerExplanation: Boolean = false,
    val hasConfirmedFirewallStartup: Boolean = false
)

class PreferencesRepository(private val context: Context) {

    private val syncPrefs = context.getSharedPreferences("netguardian_sync_prefs", Context.MODE_PRIVATE)

    fun isOnboardingCompletedSync(): Boolean {
        return syncPrefs.getBoolean("onboarding_completed", false)
    }

    fun hasSyncOnboardingState(): Boolean {
        return syncPrefs.contains("onboarding_completed")
    }

    private object Keys {
        val FIREWALL_ENABLED = booleanPreferencesKey("firewall_enabled")
        val START_ON_BOOT = booleanPreferencesKey("start_on_boot")
        val PERSISTENT_NOTIFICATION = booleanPreferencesKey("persistent_notification")
        val BLOCK_NEW_APPS = booleanPreferencesKey("block_new_apps_by_default")
        val DNS_FILTERING_ENABLED = booleanPreferencesKey("dns_filtering_enabled")
        val BLOCK_TRACKERS = booleanPreferencesKey("block_trackers_global")
        val BLOCK_ADS = booleanPreferencesKey("block_ads_global")
        val BLOCK_MALWARE = booleanPreferencesKey("block_malware_global")
        val UPSTREAM_DNS_TYPE = stringPreferencesKey("upstream_dns_type")
        val CUSTOM_DNS_IP = stringPreferencesKey("custom_dns_ip")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val QUICK_MODE = stringPreferencesKey("quick_mode")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val DEFAULT_RULE_POLICY = stringPreferencesKey("default_rule_policy")
        val HAS_SEEN_APP_MANAGER_EXPLANATION = booleanPreferencesKey("has_seen_app_manager_explanation")
        val HAS_CONFIRMED_FIREWALL_STARTUP = booleanPreferencesKey("has_confirmed_firewall_startup")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val completed = prefs[Keys.ONBOARDING_COMPLETED] ?: false
        if (syncPrefs.getBoolean("onboarding_completed", false) != completed) {
            syncPrefs.edit().putBoolean("onboarding_completed", completed).commit()
        }
        UserPreferences(
            firewallEnabled = prefs[Keys.FIREWALL_ENABLED] ?: false,
            startOnBoot = prefs[Keys.START_ON_BOOT] ?: false,
            persistentNotification = prefs[Keys.PERSISTENT_NOTIFICATION] ?: true,
            blockNewAppsByDefault = prefs[Keys.BLOCK_NEW_APPS] ?: false,
            dnsFilteringEnabled = prefs[Keys.DNS_FILTERING_ENABLED] ?: true,
            blockTrackersGlobal = prefs[Keys.BLOCK_TRACKERS] ?: true,
            blockAdsGlobal = prefs[Keys.BLOCK_ADS] ?: true,
            blockMalwareGlobal = prefs[Keys.BLOCK_MALWARE] ?: true,
            upstreamDnsType = try {
                UpstreamDnsType.valueOf(prefs[Keys.UPSTREAM_DNS_TYPE] ?: UpstreamDnsType.SYSTEM_DEFAULT.name)
            } catch (_: Exception) {
                UpstreamDnsType.SYSTEM_DEFAULT
            },
            customDnsIp = prefs[Keys.CUSTOM_DNS_IP] ?: "192.168.1.1",
            themeMode = prefs[Keys.THEME_MODE] ?: "LIGHT",
            activeQuickMode = try {
                QuickMode.valueOf(prefs[Keys.QUICK_MODE] ?: QuickMode.NORMAL.name)
            } catch (_: Exception) {
                QuickMode.NORMAL
            },
            onboardingCompleted = completed,
            defaultRulePolicy = prefs[Keys.DEFAULT_RULE_POLICY] ?: "ALLOW_ALL",
            hasSeenAppManagerExplanation = prefs[Keys.HAS_SEEN_APP_MANAGER_EXPLANATION] ?: false,
            hasConfirmedFirewallStartup = prefs[Keys.HAS_CONFIRMED_FIREWALL_STARTUP] ?: false
        )
    }

    suspend fun setFirewallEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FIREWALL_ENABLED] = enabled }
    }

    suspend fun setStartOnBoot(enabled: Boolean) {
        context.dataStore.edit { it[Keys.START_ON_BOOT] = enabled }
    }

    suspend fun setPersistentNotification(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PERSISTENT_NOTIFICATION] = enabled }
    }

    suspend fun setBlockNewAppsByDefault(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BLOCK_NEW_APPS] = enabled }
    }

    suspend fun setDnsFilteringEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DNS_FILTERING_ENABLED] = enabled }
    }

    suspend fun setBlockTrackersGlobal(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BLOCK_TRACKERS] = enabled }
    }

    suspend fun setBlockAdsGlobal(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BLOCK_ADS] = enabled }
    }

    suspend fun setBlockMalwareGlobal(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BLOCK_MALWARE] = enabled }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setQuickMode(mode: QuickMode) {
        context.dataStore.edit { it[Keys.QUICK_MODE] = mode.name }
    }

    suspend fun setUpstreamDnsType(type: UpstreamDnsType) {
        context.dataStore.edit { it[Keys.UPSTREAM_DNS_TYPE] = type.name }
    }

    suspend fun setCustomDnsIp(ip: String) {
        context.dataStore.edit { it[Keys.CUSTOM_DNS_IP] = ip.trim() }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        syncPrefs.edit().putBoolean("onboarding_completed", completed).commit()
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setDefaultRulePolicy(policy: String) {
        context.dataStore.edit { it[Keys.DEFAULT_RULE_POLICY] = policy }
    }

    suspend fun setHasSeenAppManagerExplanation(seen: Boolean) {
        context.dataStore.edit { it[Keys.HAS_SEEN_APP_MANAGER_EXPLANATION] = seen }
    }

    suspend fun setHasConfirmedFirewallStartup(confirmed: Boolean) {
        context.dataStore.edit { it[Keys.HAS_CONFIRMED_FIREWALL_STARTUP] = confirmed }
    }
}
