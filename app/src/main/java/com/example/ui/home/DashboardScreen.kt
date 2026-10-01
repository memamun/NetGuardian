package com.example.ui.home

import androidx.compose.ui.res.stringResource
import com.example.R

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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
    val quickMode by viewModel.activeQuickMode.collectAsStateWithLifecycle()
    val blockedAppsCount by viewModel.blockedAppsCount.collectAsStateWithLifecycle()
    val allowedAppsCount by viewModel.allowedAppsCount.collectAsStateWithLifecycle()
    val todayBlockedCount by viewModel.todayBlockedCount.collectAsStateWithLifecycle()
    val recentLogs by viewModel.recentLogsPreview.collectAsStateWithLifecycle()
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
                startupErrorMessage = "Check VPN permission and disconnect any other active VPN, then try again."
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
                    startupErrorMessage = "Check VPN permission and disconnect any other active VPN, then try again."
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
        contentPadding = PaddingValues(AppSpacing.content),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.content)
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

        item {
            androidx.compose.material3.ListItem(
                headlineContent = { Text(stringResource(R.string.tiles_short_title)) },
                supportingContent = { Text(stringResource(R.string.tiles_short_detail)) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                modifier = Modifier.clickable(enabled = onNavigateToSettings != null) { onNavigateToSettings?.invoke() },
                colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }

        // 4. Live Connection Activity Feed Preview
        item {
            RecentActivityPreviewCard(
                logs = recentLogs,
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
    appCount: Int = 0
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
    appCount: Int = 0
) {
    val colors = MaterialTheme.netGuardian
    val state = snapshot.state
    val active = state == com.example.firewall.FirewallState.RUNNING || state == com.example.firewall.FirewallState.STARTING
    val paused = state == com.example.firewall.FirewallState.PAUSED
    val error = state == com.example.firewall.FirewallState.ERROR
    val container by animateColorAsState(
        targetValue = when {
            paused -> colors.warningContainer
            error -> colors.blockedContainer
            active -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = M3Motion.effectsMedium(), label = "protectionContainer"
    )
    val foreground = when {
        paused -> colors.onWarningContainer
        error -> colors.onBlockedContainer
        active -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val title = stringResource(when (state) {
        com.example.firewall.FirewallState.RUNNING -> R.string.protection_running
        com.example.firewall.FirewallState.STARTING -> R.string.protection_starting
        com.example.firewall.FirewallState.PAUSED -> R.string.protection_paused
        com.example.firewall.FirewallState.ERROR -> R.string.protection_error
        com.example.firewall.FirewallState.VPN_PERMISSION_REQUIRED -> R.string.protection_setup
        com.example.firewall.FirewallState.STOPPED -> R.string.protection_stopped
    })
    val description = when (state) {
        com.example.firewall.FirewallState.RUNNING -> stringResource(R.string.hero_running_detail, appCount)
        com.example.firewall.FirewallState.STARTING -> stringResource(R.string.hero_starting_detail)
        com.example.firewall.FirewallState.PAUSED -> stringResource(R.string.hero_paused_detail)
        com.example.firewall.FirewallState.ERROR -> stringResource(R.string.hero_error_detail)
        com.example.firewall.FirewallState.VPN_PERMISSION_REQUIRED -> stringResource(R.string.hero_permission_detail)
        com.example.firewall.FirewallState.STOPPED -> stringResource(R.string.hero_stopped_detail)
    }
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = modifier.fillMaxWidth().testTag("hero_status_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = foreground)
    ) {
        Column(Modifier.padding(AppSpacing.content), verticalArrangement = Arrangement.spacedBy(AppSpacing.content)) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium), verticalAlignment = Alignment.Top) {
                // My design decision (not in M3): compact status emblem alongside the message.
                Surface(shape = CircleShape, color = foreground.copy(alpha = 0.08f), modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(if (paused || error) Icons.Default.Block else Icons.Default.Shield,
                            contentDescription = null, tint = foreground, modifier = Modifier.size(24.dp))
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.tiny)) {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    Text(description, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Button(
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onToggle() },
                modifier = Modifier.fillMaxWidth().testTag("firewall_toggle_button")
            ) {
                Text(stringResource(when {
                    paused -> R.string.hero_resume
                    active -> R.string.hero_stop
                    else -> R.string.hero_start
                }))
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun StatTilesSection(
    blockedApps: Int,
    allowedApps: Int,
    todayBlocks: Int,
    networkType: NetworkType,
    modifier: Modifier = Modifier
) {
    val netGuardian = MaterialTheme.netGuardian
    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.fillMaxWidth()) {
        // My design decision (not in M3): use fewer statistic columns for enlarged text.
        val columns = when {
            fontScale > 1.3f || maxWidth < 320.dp -> 1
            maxWidth >= 720.dp -> 4
            else -> 2
        }
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = columns,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
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
                title = "Allowed Apps",
                value = allowedApps.toString(),
                subtitle = "Not fully blocked",
                valueColor = MaterialTheme.colorScheme.primary,
                icon = Icons.Default.CheckCircle,
                iconTint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                tag = "stat_allowed_apps"
            )
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
        shape = M3ShapesTokens.CornerLargeIncreased,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.content),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(iconTint.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.emphasizedTypography.headlineMedium,
                color = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        shape = M3ShapesTokens.CornerLargeIncreased,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        )
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
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = stringResource(R.string.ui_quick_controls),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.ui_instant_profiles_hardware_locks),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Surface(
                    shape = M3ShapesTokens.CornerSmall,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = stringResource(R.string.ui_quick_tiles),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 1. Master Firewall ON/OFF Toggle
            Surface(
                shape = opticalInnerShape(20.dp, 8.dp),
                color = if (isFirewallActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainerLow,
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
                                text = stringResource(R.string.ui_firewall_on_off),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isFirewallActive) "Active • Enforcing packet rules" else "Paused • All traffic bypasses",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isFirewallActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )
                }
            }

            // 2. M3 Expressive Connected Button Group for Network Profiles
            Text(
                text = stringResource(R.string.ui_network_profile_modes),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val modes = listOf(
                QuickMode.NORMAL to "Normal",
                QuickMode.WIFI_ONLY to "Wi-Fi Only",
                QuickMode.MOBILE_ONLY to "Mobile Only"
            )
            val selectedModeIndex = modes.indexOfFirst { it.first == activeMode }
            val controlHaptic = LocalHapticFeedback.current

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                modes.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                        onClick = {
                            controlHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectMode(mode)
                        },
                        selected = index == selectedModeIndex,
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            // 3. One-Tap Batch Action Tonal Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
            ) {
                FilledTonalButton(
                    onClick = {
                        controlHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelectMode(QuickMode.BLOCK_NON_SYSTEM)
                        onBlockAllNonSystem()
                    },
                    modifier = Modifier.weight(1f),
                    shape = M3ShapesTokens.CornerFull,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = netGuardian.blockedContainer,
                        contentColor = netGuardian.blocked
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.ui_block_user_apps),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                FilledTonalButton(
                    onClick = {
                        controlHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectMode(QuickMode.NORMAL)
                        onAllowAll()
                    },
                    modifier = Modifier.weight(1f),
                    shape = M3ShapesTokens.CornerFull,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = netGuardian.allowedContainer,
                        contentColor = netGuardian.onAllowedContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.ui_allow_all_apps),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 4. Quick Tile / Notification Explanation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.ui_control_modes_live_in_your_notification_drawer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.ui_recent_activity),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.ui_view_logs),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onViewAllLogs() }
                    .padding(AppSpacing.tiny)
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("recent_activity_card"),
            shape = M3ShapesTokens.CornerLargeIncreased,
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
                        text = stringResource(R.string.ui_no_connection_events_logged_yet_nstart_firewall),
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
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.surfaceContainerLow, opticalInnerShape(20.dp, 10.dp)),
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
                                    )
                                }
                            }
                            Surface(
                                shape = M3ShapesTokens.CornerExtraSmall,
                                color = if (log.isBlocked) netGuardian.blockedContainer else netGuardian.allowedContainer
                            ) {
                                Text(
                                    text = if (log.isBlocked) "BLOCKED" else "ALLOWED",
                                    color = if (log.isBlocked) netGuardian.onBlockedContainer else netGuardian.onAllowedContainer,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
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
        shape = M3ShapesTokens.CornerLargeIncreased,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(AppSpacing.content),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.tiny)) {
                Text(
                    text = stringResource(R.string.ui_100_on_device_protection),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.ui_netguardian_uses_android_s_local_vpnservice_loopback),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
