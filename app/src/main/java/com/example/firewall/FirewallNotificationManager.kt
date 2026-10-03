package com.example.firewall

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class FirewallNotificationManager(private val context: Context) {

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val protectionChannel = NotificationChannel(
                CHANNEL_PROTECTION,
                "NetGuardian Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active firewall packet filtering and ongoing protection status"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }

            val statusChannel = NotificationChannel(
                CHANNEL_STATUS,
                "NetGuardian Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for temporary pauses and policy mode updates"
                setShowBadge(false)
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "NetGuardian Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority alerts for firewall errors and unexpected disconnects"
                setShowBadge(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(listOf(protectionChannel, statusChannel, alertsChannel))
        }
    }

    fun buildForegroundNotification(snapshot: FirewallSnapshot): Notification {
        val openAppPendingIntent = createOpenAppPendingIntent(destinationRoute = "dashboard")
        val stopPendingIntent = createServicePendingIntent(FirewallVpnService.ACTION_STOP, 1)

        val isPaused = snapshot.state == FirewallState.PAUSED
        val remainingMinutes = snapshot.pauseUntilTimestamp?.let {
            val diffMs = it - System.currentTimeMillis()
            if (diffMs > 0) ((diffMs + 59000) / 60000).toInt() else 0
        } ?: 0

        val title = if (isPaused) {
            "Firewall protection paused"
        } else {
            "NetGuardian is protecting your device"
        }

        val contentText = if (isPaused) {
            "Filtering suspended • Resumes automatically in ${remainingMinutes}m"
        } else {
            "Firewall active • ${snapshot.blockedAppsCount} apps restricted • ${snapshot.blockedConnectionsCount} connections"
        }

        val bigText = if (isPaused) {
            "Firewall filtering is temporarily paused.\nNetwork traffic is passing through without inspection.\nProtection will resume automatically in ${remainingMinutes} minutes."
        } else {
            val modeText = when {
                snapshot.isBlockAllActive -> "Strict: All Non-System Blocked"
                snapshot.isWifiOnlyActive -> "Wi-Fi Only Mode Active"
                snapshot.isMobileDataBlocked -> "Mobile Data Blocked"
                else -> "Standard Protection"
            }
            "NetGuardian is actively filtering on-device network traffic.\n" +
                    "Mode: $modeText\n" +
                    "Restricted Apps: ${snapshot.blockedAppsCount} • Monitored Connections: ${snapshot.blockedConnectionsCount}\n" +
                    "Network: ${snapshot.activeNetwork.name}"
        }

        // Create Public Notification for Lock Screen Privacy
        val publicNotification = NotificationCompat.Builder(context, CHANNEL_PROTECTION)
            .setContentTitle("NetGuardian is active")
            .setContentText("Device network protection enabled")
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val builder = NotificationCompat.Builder(context, CHANNEL_PROTECTION)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))

        if (isPaused) {
            val resumePendingIntent = createServicePendingIntent(FirewallVpnService.ACTION_RESUME, 2)
            builder.addAction(R.drawable.ic_shield, "Resume Now", resumePendingIntent)
            builder.addAction(R.drawable.ic_shield, "Stop Firewall", stopPendingIntent)
        } else {
            val pausePendingIntent = createServicePendingIntent(FirewallVpnService.ACTION_PAUSE_10_MIN, 3)
            builder.addAction(R.drawable.ic_shield, "Stop Firewall", stopPendingIntent)
            builder.addAction(R.drawable.ic_shield, "Pause 10 min", pausePendingIntent)
            builder.addAction(R.drawable.ic_shield, "Open App", openAppPendingIntent)
        }

        return builder.build()
    }

    fun showUnexpectedDisconnectAlert() {
        val openAppPendingIntent = createOpenAppPendingIntent(destinationRoute = "dashboard")
        val restorePendingIntent = createServicePendingIntent(FirewallVpnService.ACTION_START, 4)

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setContentTitle("Firewall protection stopped")
            .setContentText("VPN connection was disconnected unexpectedly")
            .setSmallIcon(R.drawable.ic_shield)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(R.drawable.ic_shield, "Restore Protection", restorePendingIntent)
            .addAction(R.drawable.ic_shield, "Open App", openAppPendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_UNEXPECTED_DISCONNECT, notification)
    }

    fun showFirewallErrorAlert(errorMessage: String) {
        val openAppPendingIntent = createOpenAppPendingIntent(destinationRoute = "settings")

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setContentTitle("Firewall needs attention")
            .setContentText(errorMessage)
            .setSmallIcon(R.drawable.ic_shield)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(R.drawable.ic_shield, "Open NetGuardian", openAppPendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_ERROR, notification)
    }

    fun showBootRestoreNeededAlert() {
        val openAppPendingIntent = createOpenAppPendingIntent(destinationRoute = "settings")

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setContentTitle("Protection needs to be restored")
            .setContentText("NetGuardian was scheduled to run on boot. Tap to re-authorize protection.")
            .setSmallIcon(R.drawable.ic_shield)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(R.drawable.ic_shield, "Authorize Firewall", openAppPendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_BOOT_RESTORE, notification)
    }

    fun dismissAlertNotifications() {
        notificationManager.cancel(NOTIFICATION_ID_UNEXPECTED_DISCONNECT)
        notificationManager.cancel(NOTIFICATION_ID_ERROR)
        notificationManager.cancel(NOTIFICATION_ID_BOOT_RESTORE)
    }

    private fun createOpenAppPendingIntent(destinationRoute: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ROUTE, destinationRoute)
        }
        return PendingIntent.getActivity(
            context,
            100 + destinationRoute.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createServicePendingIntent(actionStr: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, FirewallVpnService::class.java).apply {
            action = actionStr
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && actionStr == FirewallVpnService.ACTION_START) {
            PendingIntent.getForegroundService(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getService(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    companion object {
        const val CHANNEL_PROTECTION = "netguardian_protection"
        const val CHANNEL_STATUS = "netguardian_status"
        const val CHANNEL_ALERTS = "netguardian_alerts"

        const val NOTIFICATION_ID_FOREGROUND = 1001
        const val NOTIFICATION_ID_UNEXPECTED_DISCONNECT = 2001
        const val NOTIFICATION_ID_ERROR = 2002
        const val NOTIFICATION_ID_BOOT_RESTORE = 2003

        const val EXTRA_ROUTE = "EXTRA_ROUTE"
    }
}
