package com.example.firewall

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.example.data.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            val prefsRepo = PreferencesRepository(context)
            val firewallManager = FirewallManager.getInstance(context)
            val notificationManager = FirewallNotificationManager(context)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = prefsRepo.userPreferencesFlow.first()
                    if (prefs.startOnBoot || prefs.firewallEnabled) {
                        val prepareIntent = VpnService.prepare(context)
                        if (prepareIntent == null) {
                            // VPN is authorized, start protection safely
                            firewallManager.startFirewall(context)
                        } else {
                            // Android requires user interaction to authorize VPN after reboot
                            FirewallStateRepository.getInstance(context).setVpnPermissionRequired()
                            notificationManager.showBootRestoreNeededAlert()
                        }
                    }
                } catch (e: Exception) {
                    notificationManager.showBootRestoreNeededAlert()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
