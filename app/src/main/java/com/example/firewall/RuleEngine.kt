package com.example.firewall

import com.example.data.QuickMode
import com.example.database.AppRuleEntity

enum class NetworkType {
    WIFI,
    MOBILE,
    ETHERNET,
    NONE
}

data class FilterDecision(
    val isAllowed: Boolean,
    val reason: String
)

class RuleEngine {

    fun evaluate(
        rule: AppRuleEntity?,
        destinationHost: String?,
        port: Int,
        protocol: String,
        currentNetwork: NetworkType,
        isScreenOn: Boolean,
        isBackground: Boolean,
        isDeviceIdle: Boolean = false,
        quickMode: QuickMode,
        blockedDomains: Set<String>,
        dnsFilteringEnabled: Boolean
    ): FilterDecision {
        // 1. Quick Mode Overrides
        if (quickMode == QuickMode.BLOCK_NON_SYSTEM && rule != null && !rule.isSystemApp) {
            return FilterDecision(false, "Quick Mode: Non-System Blocked")
        }
        if (quickMode == QuickMode.WIFI_ONLY && currentNetwork == NetworkType.MOBILE) {
            return FilterDecision(false, "Quick Mode: Wi-Fi Only Active")
        }
        if (quickMode == QuickMode.MOBILE_ONLY && currentNetwork == NetworkType.WIFI) {
            return FilterDecision(false, "Quick Mode: Mobile Only Active")
        }

        // If no rule exists, default to allow
        if (rule == null) {
            return FilterDecision(true, "Allowed (Default)")
        }

        // 2. Direct App Block Rule
        if (rule.isBlocked) {
            return FilterDecision(false, "App Blocked by User")
        }

        // 3. Network Type Specific Rules
        if (currentNetwork == NetworkType.WIFI && rule.blockWifi) {
            return FilterDecision(false, "Blocked on Wi-Fi")
        }
        if (currentNetwork == NetworkType.MOBILE && rule.blockMobile) {
            return FilterDecision(false, "Blocked on Mobile Data")
        }

        // 4. Screen Off Policy
        if (!isScreenOn && rule.blockScreenOff) {
            return FilterDecision(false, "Blocked while Screen Off")
        }

        // 5. Device Idle Policy
        if (isDeviceIdle && rule.blockDeviceIdle) {
            return FilterDecision(false, "Blocked while Device Idle")
        }

        // 6. Background Activity Policy
        if (isBackground && rule.blockBackground) {
            return FilterDecision(false, "Background Access Blocked")
        }

        // 6. DNS Tracker / Ad / Malware Filtering
        if (dnsFilteringEnabled && rule.blockTrackers && destinationHost != null) {
            val normalizedHost = destinationHost.lowercase().trimEnd('.')
            for (blocked in blockedDomains) {
                if (normalizedHost == blocked || normalizedHost.endsWith(".$blocked")) {
                    return FilterDecision(false, "DNS Blocklist ($blocked)")
                }
            }
        }

        return FilterDecision(true, "Allowed")
    }
}
