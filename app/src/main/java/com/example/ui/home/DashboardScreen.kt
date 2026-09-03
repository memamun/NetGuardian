package com.example.ui.home

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.QuickMode
import com.example.firewall.NetworkType
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToApps: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onVpnPermissionNeeded: (Intent) -> Unit,
    onNavigateToSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isFirewallActive by viewModel.firewallActive.collectAsStateWithLifecycle()
    val snapshot by viewModel.firewallSnapshot.collectAsStateWithLifecycle()
    val networkType by viewModel.networkType.collectAsStateWithLifecycle()
    val isScreenOn by viewModel.isScreenOn.collectAsStateWithLifecycle()
    val quickMode by viewModel.activeQuickMode.collectAsStateWithLifecycle()
    val blockedAppsCount by viewModel.blockedAppsCount.collectAsStateWithLifecycle()
    val allowedAppsCount by viewModel.allowedAppsCount.collectAsStateWithLifecycle()
    val todayBlockedCount by viewModel.todayBlockedCount.collectAsStateWithLifecycle()
    val recentLogs by viewModel.recentLogs.collectAsStateWithLifecycle()
    val userPrefs by viewModel.userPreferences.collectAsStateWithLifecycle()

    var showStartupSafetyDialog by remember { mutableStateOf(false) }
    var startupErrorMessage by remember { mutableStateOf<String?>(null) }

    val handleToggleFirewall = {
        if (!isFirewallActive && !userPrefs.hasConfirmedFirewallStartup) {
            showStartupSafetyDialog = true
        } else {
            try {
                val prepareIntent = viewModel.toggleFirewall(context)
                if (prepareIntent != null) {
                    onVpnPermissionNeeded(prepareIntent)
                }
            } catch (e: Exception) {
                startupErrorMessage = e.message ?: "Failed to establish local VPN service"
            }
        }
    }

    if (showStartupSafetyDialog) {
        FirewallStartupSafetyDialog(
            onConfirm = {
                viewModel.setHasConfirmedFirewallStartup(true)
                showStartupSafetyDialog = false
                try {
                    val prepareIntent = viewModel.toggleFirewall(context)
                    if (prepareIntent != null) {
                        onVpnPermissionNeeded(prepareIntent)
                    }
                } catch (e: Exception) {
                    startupErrorMessage = e.message ?: "Failed to establish local VPN service"
                }
            },
            onDismiss = { showStartupSafetyDialog = false }
        )
    }

    if (startupErrorMessage != null) {
        FirewallStartupFailureDialog(
            errorMessage = startupErrorMessage ?: "Unknown error",
            onRetry = {
                startupErrorMessage = null
                handleToggleFirewall()
            },
            onDismiss = { startupErrorMessage = null }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Status Card
        item {
            HeroStatusCard(
                snapshot = snapshot,
                networkType = networkType,
                appCount = (blockedAppsCount + allowedAppsCount).coerceAtLeast(1),
                onToggle = handleToggleFirewall
            )
        }

        // Quick Settings Tiles Discovery Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToSettings?.invoke() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Quick Settings Tiles Available",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Control Firewall, Block All, and Wi-Fi Only directly from your notification pull-down shade.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (onNavigateToSettings != null) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Configure Tiles",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 2. Stat Tiles Grid
        item {
            StatTilesSection(
                blockedApps = blockedAppsCount,
                allowedApps = allowedAppsCount,
                todayBlocks = todayBlockedCount,
                networkType = networkType
            )
        }

        // 3. Quick Control Modes
        item {
            QuickControlsCard(
                isFirewallActive = isFirewallActive,
                onToggleFirewall = handleToggleFirewall,
                activeMode = quickMode,
                onSelectMode = { viewModel.setQuickMode(it) },
                onBlockAllNonSystem = { viewModel.blockAllNonSystem(true) },
                onAllowAll = { viewModel.allowAllApps() }
            )
        }

        // 4. Live Connection Activity Feed Preview
        item {
            RecentActivityPreviewCard(
                logs = recentLogs.take(4),
                onViewAllLogs = onNavigateToLogs
            )
        }

        // 5. Privacy & Architecture Transparency Card
        item {
            ArchitectureTransparencyCard()
        }
    }
}

@Composable
fun HeroStatusCard(
    isActive: Boolean,
    networkType: NetworkType,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    appCount: Int = 142
) {
    HeroStatusCard(
        snapshot = com.example.firewall.FirewallSnapshot(
            state = if (isActive) com.example.firewall.FirewallState.RUNNING else com.example.firewall.FirewallState.STOPPED
        ),
        networkType = networkType,
        onToggle = onToggle,
        modifier = modifier,
        appCount = appCount
    )
}

@Composable
fun HeroStatusCard(
    snapshot: com.example.firewall.FirewallSnapshot,
    networkType: NetworkType,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    appCount: Int = 142
) {
    val netGuardian = MaterialTheme.netGuardian
    val isActive = snapshot.state == com.example.firewall.FirewallState.RUNNING || snapshot.state == com.example.firewall.FirewallState.STARTING
    val isPaused = snapshot.state == com.example.firewall.FirewallState.PAUSED
    val isError = snapshot.state == com.example.firewall.FirewallState.ERROR

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val cardBg = when {
        isPaused -> netGuardian.warningContainer
        isActive -> MaterialTheme.colorScheme.primaryContainer
        isError -> netGuardian.blockedContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val iconBg = when {
        isPaused -> netGuardian.warning
        isActive -> MaterialTheme.colorScheme.primary
        isError -> netGuardian.blocked
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_status_card"),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isPaused -> Icons.Default.Block
                        isActive -> Icons.Default.Shield
                        else -> Icons.Default.Block
                    },
                    contentDescription = "Firewall Status Icon",
                    modifier = Modifier.size(32.dp),
                    tint = when {
                        isPaused -> netGuardian.onWarning
                        isActive -> MaterialTheme.colorScheme.onPrimary
                        isError -> netGuardian.onBlocked
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val titleText = when (snapshot.state) {
                com.example.firewall.FirewallState.RUNNING -> "Firewall is Active"
                com.example.firewall.FirewallState.STARTING -> "Starting Firewall..."
                com.example.firewall.FirewallState.PAUSED -> "Protection Paused"
                com.example.firewall.FirewallState.ERROR -> "Firewall Attention Required"
                com.example.firewall.FirewallState.VPN_PERMISSION_REQUIRED -> "VPN Permission Needed"
                com.example.firewall.FirewallState.STOPPED -> "Firewall is Stopped"
            }

            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                ),
                color = when {
                    isPaused -> netGuardian.onWarningContainer
                    isActive -> MaterialTheme.colorScheme.onPrimaryContainer
                    isError -> netGuardian.onBlockedContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            val subtitleText = when (snapshot.state) {
                com.example.firewall.FirewallState.RUNNING -> "Filtering $appCount apps locally on-device"
                com.example.firewall.FirewallState.STARTING -> "Establishing local VPN tunnel..."
                com.example.firewall.FirewallState.PAUSED -> "Traffic inspection suspended temporarily"
                com.example.firewall.FirewallState.ERROR -> snapshot.lastErrorMessage ?: "Unable to establish local VPN"
                com.example.firewall.FirewallState.VPN_PERMISSION_REQUIRED -> "Tap below to authorize the local firewall VPN profile"
                com.example.firewall.FirewallState.STOPPED -> "Tap below to activate on-device protection"
            }

            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            val buttonText = when {
                isPaused -> "Resume Protection"
                isActive -> "Stop Firewall"
                else -> "Start Firewall"
            }

            val (buttonBg, buttonFg) = when {
                isPaused -> Pair(netGuardian.warning, netGuardian.onWarning)
                isActive -> Pair(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                else -> Pair(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
            }

            Button(
                onClick = onToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("firewall_toggle_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonBg,
                    contentColor = buttonFg
                )
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun StatTilesSection(
    blockedApps: Int,
    allowedApps: Int,
    todayBlocks: Int,
    networkType: NetworkType,
    modifier: Modifier = Modifier
) {
    val netGuardian = MaterialTheme.netGuardian
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = "Blocked Today",
                value = todayBlocks.toString(),
                subtitle = "Connections stopped",
                valueColor = netGuardian.blocked,
                icon = Icons.Default.Security,
                iconTint = netGuardian.blocked,
                modifier = Modifier.weight(1f),
                tag = "stat_today_blocks"
            )
            StatCard(
                title = "Data Saved",
                value = "42.8 MB",
                subtitle = "Estimated savings",
                valueColor = MaterialTheme.colorScheme.primary,
                icon = Icons.Default.CheckCircle,
                iconTint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                tag = "stat_allowed_apps"
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = "Blocked Apps",
                value = blockedApps.toString(),
                subtitle = "Traffic restricted",
                valueColor = netGuardian.blocked,
                icon = Icons.Default.Block,
                iconTint = netGuardian.blocked,
                modifier = Modifier.weight(1f),
                tag = "stat_blocked_apps"
            )
            StatCard(
                title = "Active Network",
                value = when (networkType) {
                    NetworkType.WIFI -> "Wi-Fi"
                    NetworkType.MOBILE -> "Cellular"
                    NetworkType.ETHERNET -> "Ethernet"
                    NetworkType.NONE -> "Offline"
                },
                subtitle = "Local interface",
                valueColor = MaterialTheme.colorScheme.onSurface,
                icon = when (networkType) {
                    NetworkType.WIFI -> Icons.Default.Wifi
                    NetworkType.MOBILE -> Icons.Default.SignalCellularAlt
                    else -> Icons.Default.VpnKey
                },
                iconTint = if (networkType != NetworkType.NONE) MaterialTheme.colorScheme.primary else netGuardian.warning,
                modifier = Modifier.weight(1f),
                tag = "stat_network_type"
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    tag: String,
    modifier: Modifier = Modifier,
    subtitle: String = "Rule enforcement",
    valueColor: Color = Color.Unspecified
) {
    Card(
        modifier = modifier.testTag(tag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
                color = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface,
                fontSize = 24.sp
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun QuickControlsCard(
    isFirewallActive: Boolean,
    onToggleFirewall: () -> Unit,
    activeMode: QuickMode,
    onSelectMode: (QuickMode) -> Unit,
    onBlockAllNonSystem: () -> Unit,
    onAllowAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val netGuardian = MaterialTheme.netGuardian
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quick_controls_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Quick Controls",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Instant profiles & hardware network locks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "TILE & NOTIFICATION",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // 1. Master Firewall ON/OFF Toggle
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isFirewallActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isFirewallActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isFirewallActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Firewall ON / OFF",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isFirewallActive) "Active • Enforcing packet rules" else "Paused • All traffic bypasses",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isFirewallActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = isFirewallActive,
                        onCheckedChange = { onToggleFirewall() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            // 2. Quick Mode Profiles (Normal, Wi-Fi Only, Mobile Data Only, Block Non-System, Allow All)
            Text(
                text = "NETWORK PROFILE MODES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = activeMode == QuickMode.NORMAL,
                    onClick = { onSelectMode(QuickMode.NORMAL) },
                    label = { Text("Normal", fontSize = 11.sp, maxLines = 1) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                FilterChip(
                    selected = activeMode == QuickMode.WIFI_ONLY,
                    onClick = { onSelectMode(QuickMode.WIFI_ONLY) },
                    label = { Text("Wi-Fi Only", fontSize = 11.sp, maxLines = 1) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                FilterChip(
                    selected = activeMode == QuickMode.MOBILE_ONLY,
                    onClick = { onSelectMode(QuickMode.MOBILE_ONLY) },
                    label = { Text("Mobile Only", fontSize = 11.sp, maxLines = 1) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // 3. One-Tap Batch Action Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onSelectMode(QuickMode.BLOCK_NON_SYSTEM)
                        onBlockAllNonSystem()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = if (activeMode == QuickMode.BLOCK_NON_SYSTEM) {
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = netGuardian.blockedContainer,
                            contentColor = netGuardian.blocked
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (activeMode == QuickMode.BLOCK_NON_SYSTEM) netGuardian.blocked else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Text(
                        text = "Block Non-System",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = {
                        onSelectMode(QuickMode.NORMAL)
                        onAllowAll()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = "Allow All Apps",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 4. Quick Tile / Notification Explanation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Control modes live in your notification drawer actions and Android Quick Settings tile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun RecentActivityPreviewCard(
    logs: List<com.example.database.ConnectionLogEntity>,
    onViewAllLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val netGuardian = MaterialTheme.netGuardian
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENT ACTIVITY",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "View Logs",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onViewAllLogs() }
                    .padding(4.dp)
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("recent_activity_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No connection events logged yet.\nStart firewall to monitor traffic.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                Column {
                    logs.forEachIndexed { index, log ->
                        if (index > 0) {
                            HorizontalDivider(
                                thickness = 0.8.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (log.isBlocked) Icons.Default.Block else Icons.Default.Security,
                                        contentDescription = null,
                                        tint = if (log.isBlocked) netGuardian.blocked else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = log.appName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${log.destinationHost} • ${timeFormat.format(Date(log.timestamp))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (log.isBlocked) netGuardian.blockedContainer else netGuardian.allowedContainer
                            ) {
                                Text(
                                    text = if (log.isBlocked) "BLOCKED" else "ALLOWED",
                                    color = if (log.isBlocked) netGuardian.onBlockedContainer else netGuardian.onAllowedContainer,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArchitectureTransparencyCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transparency_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "100% On-Device Protection",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "NetGuardian uses Android's local VpnService loopback interface. Traffic is analyzed and filtered directly on your processor. Zero bytes are uploaded to remote servers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
