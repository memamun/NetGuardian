package com.example.data

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

enum class DiagnosticStatus {
    READY,
    NEEDS_ATTENTION,
    NOT_REQUIRED
}

enum class SetupErrorCode(
    val title: String,
    val description: String,
    val technicalDetail: String,
    val recoveryActionTitle: String?
) {
    VPN_NOT_AUTHORIZED(
        title = "VPN Permission Required",
        description = "Android requires VPN authorization so NetGuardian can create a local loopback filter on this device.",
        technicalDetail = "VpnService.prepare(context) returned non-null intent.",
        recoveryActionTitle = "Authorize VPN"
    ),
    VPN_AUTHORIZED(
        title = "VPN Access Ready",
        description = "NetGuardian is authorized to filter network traffic locally on this device.",
        technicalDetail = "VpnService.prepare(context) is null (authorized).",
        recoveryActionTitle = null
    ),
    VPN_START_FAILED(
        title = "Firewall Start Failed",
        description = "Could not establish the local VPN interface. Another VPN app may be in lockdown mode.",
        technicalDetail = "VpnService.Builder.establish() returned null or threw SecurityException.",
        recoveryActionTitle = "Retry Startup"
    ),
    VPN_RUNNING(
        title = "Firewall Running",
        description = "Local VPN interface is active and inspecting network packets according to your rules.",
        technicalDetail = "FirewallVpnService foreground service active with valid parcel file descriptor.",
        recoveryActionTitle = null
    ),
    NOTIFICATION_NOT_GRANTED(
        title = "Notifications Disabled",
        description = "Enable notifications to see protection status and alerts in the notification drawer. Protection can run without this permission.",
        technicalDetail = "POST_NOTIFICATIONS permission not granted (API >= 33).",
        recoveryActionTitle = "Enable Notifications"
    ),
    BATTERY_RESTRICTED(
        title = "Battery Optimization Active",
        description = "Your device manufacturer may stop background firewall protection to conserve battery.",
        technicalDetail = "PowerManager.isIgnoringBatteryOptimizations is false.",
        recoveryActionTitle = "Configure Battery"
    ),
    APP_LIST_UNAVAILABLE(
        title = "App Visibility Restricted",
        description = "NetGuardian cannot query installed applications to apply per-app rules.",
        technicalDetail = "PackageManager.getInstalledPackages returned empty list.",
        recoveryActionTitle = "Review Permissions"
    ),
    DATABASE_ERROR(
        title = "Database Inaccessible",
        description = "Local SQLite database could not be opened to persist firewall rules.",
        technicalDetail = "Room database instantiation or query failure.",
        recoveryActionTitle = "Reset Database"
    ),
    NETWORK_UNAVAILABLE(
        title = "No Network Connection",
        description = "Device is currently offline. Firewall rules will apply immediately once connection is established.",
        technicalDetail = "ConnectivityManager.activeNetwork is null or lacking internet capability.",
        recoveryActionTitle = "Check Network"
    )
}

data class DiagnosticCheck(
    val id: String,
    val title: String,
    val description: String,
    val status: DiagnosticStatus,
    val statusLabel: String,
    val technicalDetail: String,
    val isRequired: Boolean,
    val errorCode: SetupErrorCode? = null
)

object DiagnosticsHelper {

    fun isVpnAuthorized(context: Context): Boolean {
        return VpnService.prepare(context) == null
    }

    fun isNotificationGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            true // Notification runtime permission not required prior to Android 13
        }
    }

    fun isNotificationRequired(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNet = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNet) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun canQueryInstalledApps(context: Context): Boolean {
        return try {
            context.packageManager.getInstalledPackages(0).isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    fun runFullDiagnostics(context: Context, isFirewallRunning: Boolean, dbAccessible: Boolean): List<DiagnosticCheck> {
        val checks = mutableListOf<DiagnosticCheck>()

        // 1. VPN Authorization
        val vpnAuth = isVpnAuthorized(context)
        checks.add(
            DiagnosticCheck(
                id = "vpn_auth",
                title = "Firewall VPN Access",
                description = if (vpnAuth) "Authorized for local-only traffic inspection" else "Required to create local packet filter",
                status = if (vpnAuth) DiagnosticStatus.READY else DiagnosticStatus.NEEDS_ATTENTION,
                statusLabel = if (vpnAuth) "Ready" else "Needs attention",
                technicalDetail = if (vpnAuth) "VpnService.prepare() == null" else "Requires user authorization via system prompt",
                isRequired = true,
                errorCode = if (!vpnAuth) SetupErrorCode.VPN_NOT_AUTHORIZED else SetupErrorCode.VPN_AUTHORIZED
            )
        )

        // 2. Firewall Service
        val serviceReady = vpnAuth
        checks.add(
            DiagnosticCheck(
                id = "firewall_service",
                title = "Firewall Engine",
                description = if (isFirewallRunning) "Active — inspecting local packets" else if (serviceReady) "Ready to start on demand" else "Awaiting VPN authorization",
                status = if (isFirewallRunning || serviceReady) DiagnosticStatus.READY else DiagnosticStatus.NEEDS_ATTENTION,
                statusLabel = if (isFirewallRunning) "Active" else if (serviceReady) "Ready" else "Needs attention",
                technicalDetail = "FirewallVpnService local TUN interface",
                isRequired = true,
                errorCode = if (isFirewallRunning) SetupErrorCode.VPN_RUNNING else if (!serviceReady) SetupErrorCode.VPN_NOT_AUTHORIZED else null
            )
        )

        // 3. Notifications
        val notifRequired = isNotificationRequired()
        val notifGranted = isNotificationGranted(context)
        checks.add(
            DiagnosticCheck(
                id = "notifications",
                title = "Foreground Notifications",
                description = if (!notifRequired) "Not required on Android ${Build.VERSION.RELEASE}" else if (notifGranted) "Allowed for persistent status indicator" else "Optional: enables status and alerts in the notification drawer",
                status = if (!notifRequired) DiagnosticStatus.NOT_REQUIRED else if (notifGranted) DiagnosticStatus.READY else DiagnosticStatus.NEEDS_ATTENTION,
                statusLabel = if (!notifRequired) "Not required" else if (notifGranted) "Ready" else "Needs attention",
                technicalDetail = "POST_NOTIFICATIONS permission status",
                isRequired = false,
                errorCode = if (notifRequired && !notifGranted) SetupErrorCode.NOTIFICATION_NOT_GRANTED else null
            )
        )

        // 4. Background Operation (Battery Optimization)
        val batteryIgnored = isBatteryOptimizationIgnored(context)
        checks.add(
            DiagnosticCheck(
                id = "battery",
                title = "Background Operation",
                description = if (batteryIgnored) "Unrestricted — protection protected from OEM killers" else "Battery optimization active (OEM may restrict in sleep)",
                status = if (batteryIgnored) DiagnosticStatus.READY else DiagnosticStatus.NEEDS_ATTENTION,
                statusLabel = if (batteryIgnored) "Ready" else "Recommended",
                technicalDetail = "PowerManager.isIgnoringBatteryOptimizations",
                isRequired = false,
                errorCode = if (!batteryIgnored) SetupErrorCode.BATTERY_RESTRICTED else null
            )
        )

        // 5. App Visibility
        val appQueryOk = canQueryInstalledApps(context)
        checks.add(
            DiagnosticCheck(
                id = "app_visibility",
                title = "Installed App Access",
                description = if (appQueryOk) "Can inspect and apply rules to installed packages" else "Cannot query installed applications",
                status = if (appQueryOk) DiagnosticStatus.READY else DiagnosticStatus.NEEDS_ATTENTION,
                statusLabel = if (appQueryOk) "Ready" else "Needs attention",
                technicalDetail = "PackageManager.getInstalledPackages",
                isRequired = true,
                errorCode = if (!appQueryOk) SetupErrorCode.APP_LIST_UNAVAILABLE else null
            )
        )

        // 6. Local Storage / Database
        checks.add(
            DiagnosticCheck(
                id = "storage",
                title = "Offline Database",
                description = if (dbAccessible) "Local SQLite database accessible" else "Database access failed",
                status = if (dbAccessible) DiagnosticStatus.READY else DiagnosticStatus.NEEDS_ATTENTION,
                statusLabel = if (dbAccessible) "Ready" else "Needs attention",
                technicalDetail = "Room database local file access",
                isRequired = true,
                errorCode = if (!dbAccessible) SetupErrorCode.DATABASE_ERROR else null
            )
        )

        // 7. Network Connectivity
        val netAvailable = isNetworkAvailable(context)
        checks.add(
            DiagnosticCheck(
                id = "network",
                title = "Network Connectivity",
                description = if (netAvailable) "Active network detected" else "Offline — rules will apply once connected",
                status = if (netAvailable) DiagnosticStatus.READY else DiagnosticStatus.NEEDS_ATTENTION,
                statusLabel = if (netAvailable) "Ready" else "Offline",
                technicalDetail = "ConnectivityManager.activeNetwork state",
                isRequired = false,
                errorCode = if (!netAvailable) SetupErrorCode.NETWORK_UNAVAILABLE else null
            )
        )

        return checks
    }

    fun openAppNotificationSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            openApplicationDetailsSettings(context)
            return
        }
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openApplicationDetailsSettings(context)
        }
    }

    fun openBatterySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(intent)
        } catch (_: Exception) {
            openApplicationDetailsSettings(context)
        }
    }

    fun openApplicationDetailsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
