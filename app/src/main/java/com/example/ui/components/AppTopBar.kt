package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.example.R
import com.example.firewall.FirewallState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String, state: FirewallState, modifier: Modifier = Modifier) {
    val status = stringResource(when (state) {
        FirewallState.RUNNING -> R.string.protection_running
        FirewallState.STARTING -> R.string.protection_starting
        FirewallState.PAUSED -> R.string.protection_paused
        FirewallState.ERROR -> R.string.protection_error
        FirewallState.VPN_PERMISSION_REQUIRED -> R.string.protection_setup
        FirewallState.STOPPED -> R.string.protection_stopped
    })
    TopAppBar(
        title = {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(status, style = MaterialTheme.typography.labelMedium,
                    color = if (state == FirewallState.ERROR) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    )
}
