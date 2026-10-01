package com.example.ui.settings

import androidx.compose.ui.res.stringResource
import com.example.R

import android.content.Context
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DiagnosticStatus
import com.example.data.DiagnosticsHelper
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@Composable
fun ProtectionSetupSection(
    viewModel: MainViewModel,
    onRequestVpn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val firewallActive by viewModel.firewallActive.collectAsStateWithLifecycle()
    val diagnostics by viewModel.systemDiagnostics.collectAsStateWithLifecycle()

    var isVpnAuth by remember { mutableStateOf(DiagnosticsHelper.isVpnAuthorized(context)) }
    var isNotifGranted by remember { mutableStateOf(DiagnosticsHelper.isNotificationGranted(context)) }
    var isBatteryIgnored by remember { mutableStateOf(DiagnosticsHelper.isBatteryOptimizationIgnored(context)) }
    var isAppQueryOk by remember { mutableStateOf(DiagnosticsHelper.canQueryInstalledApps(context)) }

    fun refreshStates() {
        isVpnAuth = DiagnosticsHelper.isVpnAuthorized(context)
        isNotifGranted = DiagnosticsHelper.isNotificationGranted(context)
        isBatteryIgnored = DiagnosticsHelper.isBatteryOptimizationIgnored(context)
        isAppQueryOk = DiagnosticsHelper.canQueryInstalledApps(context)
        viewModel.runDiagnostics(context)
    }

    LaunchedEffect(Unit) {
        refreshStates()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("protection_setup_card"),
        shape = M3ShapesTokens.CornerLargeIncreased,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.content),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(
                            text = stringResource(R.string.ui_protection_setup_permissions),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.ui_live_android_security_model_verification),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        refreshStates()
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.ui_check), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 1. VPN Firewall
            PermissionItemRow(
                icon = Icons.Default.VpnKey,
                title = "VPN Firewall",
                description = if (firewallActive) "Active — local traffic filtered" else if (isVpnAuth) "Ready — authorized on device" else "Android requires VPN authorization for local filtering",
                status = if (firewallActive || isVpnAuth) "✓ Enabled" else "! Requires Auth",
                isOk = isVpnAuth,
                actionLabel = if (!isVpnAuth) "Authorize" else null,
                onAction = { onRequestVpn() }
            )

            // 2. Notifications
            val notifRequired = DiagnosticsHelper.isNotificationRequired()
            val notifOk = !notifRequired || isNotifGranted
            PermissionItemRow(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                description = if (!notifRequired) "Automatic on Android ${Build.VERSION.RELEASE}" else if (isNotifGranted) "Persistent status shade indicator permitted" else "Required on Android 13+ for foreground service notification",
                status = if (notifOk) "✓ Enabled" else "! Restricted",
                isOk = notifOk,
                actionLabel = if (!notifOk) "Fix" else null,
                onAction = { DiagnosticsHelper.openAppNotificationSettings(context) }
            )

            // 3. Battery Optimization
            PermissionItemRow(
                icon = Icons.Default.BatterySaver,
                title = "Battery Optimization",
                description = if (isBatteryIgnored) "Unrestricted — protection won't be killed in standby" else "Your device may restrict background operation.",
                status = if (isBatteryIgnored) "✓ Unrestricted" else "! Restricted",
                isOk = isBatteryIgnored,
                actionLabel = if (!isBatteryIgnored) "Fix" else null,
                onAction = { DiagnosticsHelper.openBatterySettings(context) }
            )

            // 4. Installed Apps Access
            PermissionItemRow(
                icon = Icons.Default.Apps,
                title = "Installed Apps Access",
                description = if (isAppQueryOk) "Can query installed apps for individual rule assignment" else "Cannot inspect installed packages",
                status = if (isAppQueryOk) "✓ Available" else "! Restricted",
                isOk = isAppQueryOk,
                actionLabel = if (!isAppQueryOk) "Settings" else null,
                onAction = { DiagnosticsHelper.openApplicationDetailsSettings(context) }
            )

            // 5. Privacy Filters
            PermissionItemRow(
                icon = Icons.Default.Dns,
                title = "Privacy Filters",
                description = "On-device DNS sinkhole for trackers and ad networks",
                status = "○ Optional",
                isOk = true,
                actionLabel = null,
                onAction = null
            )
        }
    }
}

@Composable
private fun PermissionItemRow(
    icon: ImageVector,
    title: String,
    description: String,
    status: String,
    isOk: Boolean,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
    val netGuardian = MaterialTheme.netGuardian
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isOk) MaterialTheme.colorScheme.primaryContainer else netGuardian.warningContainer,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isOk) MaterialTheme.colorScheme.primary else netGuardian.warning,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = CircleShape,
                    color = if (isOk) netGuardian.allowedContainer else netGuardian.warningContainer
                ) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isOk) netGuardian.onAllowedContainer else netGuardian.onWarningContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )
        }

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAction()
                },
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = netGuardian.warningContainer,
                    contentColor = netGuardian.onWarningContainer
                )
            ) {
                Text(actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
