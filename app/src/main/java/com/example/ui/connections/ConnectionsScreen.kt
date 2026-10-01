package com.example.ui.connections

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.database.ConnectionLogEntity
import com.example.ui.LogFilter
import com.example.ui.MainViewModel
import com.example.ui.components.AppSearchField
import com.example.ui.components.ScreenState
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val logs by viewModel.filteredLogs.collectAsStateWithLifecycle()
    val firewallActive by viewModel.firewallActive.collectAsStateWithLifecycle()
    val allLogs by viewModel.recentLogs.collectAsStateWithLifecycle()
    val currentFilter by viewModel.logFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.logSearchQuery.collectAsStateWithLifecycle()
    val netGuardian = MaterialTheme.netGuardian
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    var showClearDialog by remember { mutableStateOf(false) }
    var selectedLogForDetail by remember { mutableStateOf<ConnectionLogEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val blockedCount = remember(allLogs) { allLogs.count { it.isBlocked } }
    val blockRate = remember(allLogs, blockedCount) {
        if (allLogs.isEmpty()) 0 else ((blockedCount.toFloat() / allLogs.size) * 100).toInt()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("connections_screen")
    ) {
        // Search Input
        AppSearchField(
            query = searchQuery,
            onQueryChange = { viewModel.logSearchQuery.value = it },
            label = stringResource(R.string.search_logs),
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("logs_search_field")
        )

        // Filter Chips & Clear Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
            ) {
                LogFilter.entries.forEach { filter ->
                    val (label, count) = when (filter) {
                        LogFilter.ALL -> "All" to allLogs.size
                        LogFilter.BLOCKED -> "Blocked" to blockedCount
                        LogFilter.ALLOWED -> "Allowed" to (allLogs.size - blockedCount)
                    }
                    val isSelected = currentFilter == filter

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.logFilter.value = filter
                        },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Surface(
                                    shape = M3ShapesTokens.CornerExtraSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow
                                ) {
                                    Text(
                                        text = "$count",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        },
                        shape = M3ShapesTokens.CornerSmall,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showClearDialog = true
                    },
                    enabled = allLogs.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = stringResource(R.string.ui_clear_logs),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Summary Bar
        Surface(
            shape = M3ShapesTokens.CornerMedium,
            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Text(
                        text = stringResource(if (firewallActive) R.string.inspection_active else R.string.inspection_paused),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Block Rate: $blockRate%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (blockRate > 0) netGuardian.blocked else MaterialTheme.colorScheme.primary
                )
            }
        }

        // Log Items
        if (logs.isEmpty()) {
            ScreenState(
                title = stringResource(R.string.no_connections),
                description = stringResource(when {
                    allLogs.isNotEmpty() -> R.string.logs_no_matches
                    firewallActive -> R.string.logs_empty_active
                    else -> R.string.logs_empty_paused
                }),
                modifier = Modifier.fillMaxSize(),
                icon = Icons.Default.Search,
                action = {
                    if (searchQuery.isNotBlank() || currentFilter != LogFilter.ALL) {
                        androidx.compose.material3.FilledTonalButton(onClick = {
                            viewModel.logSearchQuery.value = ""
                            viewModel.logFilter.value = LogFilter.ALL
                        }) { Text(stringResource(R.string.clear_filters)) }
                    }
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
            ) {
                items(logs, key = { it.id }) { log ->
                    ConnectionLogItemCard(
                        log = log,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedLogForDetail = log
                        }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            shape = M3ShapesTokens.CornerExtraLarge,
            title = { Text(stringResource(R.string.ui_clear_connection_logs), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.ui_are_you_sure_you_want_to_delete)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearLogs()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = netGuardian.blocked)
                ) {
                    Text(stringResource(R.string.ui_clear_all), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.ui_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Detail Bottom Sheet
    selectedLogForDetail?.let { log ->
        ModalBottomSheet(
            onDismissRequest = { selectedLogForDetail = null },
            sheetState = sheetState,
            shape = M3ShapesTokens.CornerExtraLargeTop,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            ConnectionDetailSheetContent(
                log = log,
                onDismiss = { selectedLogForDetail = null },
                onBlockDomain = { domain ->
                    viewModel.addCustomBlocklistDomain(domain)
                }
            )
        }
    }
}

@Composable
fun ConnectionLogItemCard(
    log: ConnectionLogEntity,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }
    val netGuardian = MaterialTheme.netGuardian

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("connection_log_card_${log.id}"),
        shape = M3ShapesTokens.CornerLargeIncreased,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (log.isBlocked) netGuardian.blocked.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.appName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = M3ShapesTokens.CornerSmall,
                    color = if (log.isBlocked) netGuardian.blockedContainer else netGuardian.allowedContainer,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (log.isBlocked) netGuardian.blocked.copy(alpha = 0.3f) else netGuardian.allowed.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (log.isBlocked) "BLOCKED" else "ALLOWED",
                        color = if (log.isBlocked) netGuardian.onBlockedContainer else netGuardian.onAllowedContainer,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Destination and Protocol
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.destinationHost,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = M3ShapesTokens.CornerExtraSmall,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Text(
                        text = "${log.protocol} : ${log.port}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            // Reason and Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.blockReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (log.isBlocked) netGuardian.blocked else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = timeFormat.format(Date(log.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun ConnectionDetailSheetContent(
    log: ConnectionLogEntity,
    onDismiss: () -> Unit,
    onBlockDomain: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val netGuardian = MaterialTheme.netGuardian
    val fullTimeFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("connection_detail_sheet"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sheet Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.connection_details),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.ui_cancel),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // App & Status Card
        Card(
            shape = M3ShapesTokens.CornerLargeIncreased,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = log.appName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = log.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = M3ShapesTokens.CornerSmall,
                    color = if (log.isBlocked) netGuardian.blockedContainer else netGuardian.allowedContainer,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (log.isBlocked) netGuardian.blocked.copy(alpha = 0.3f) else netGuardian.allowed.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (log.isBlocked) "BLOCKED" else "ALLOWED",
                        color = if (log.isBlocked) netGuardian.onBlockedContainer else netGuardian.onAllowedContainer,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Detailed Metadata Card
        Card(
            shape = M3ShapesTokens.CornerLargeIncreased,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Host / IP
                DetailRow(
                    label = "Host / Destination",
                    value = log.destinationHost,
                    isMonospace = true,
                    actionIcon = Icons.Default.ContentCopy,
                    onAction = {
                        clipboardManager.setText(AnnotatedString(log.destinationHost))
                        Toast.makeText(context, context.getString(R.string.host_copied), Toast.LENGTH_SHORT).show()
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Protocol & Port
                DetailRow(
                    label = stringResource(R.string.protocol_port),
                    value = "${log.protocol} : ${log.port}"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Decision & Reason
                DetailRow(
                    label = stringResource(R.string.filter_decision),
                    value = log.blockReason,
                    valueColor = if (log.isBlocked) netGuardian.blocked else netGuardian.allowed
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Timestamp
                DetailRow(
                    label = stringResource(R.string.event_timestamp),
                    value = fullTimeFormat.format(Date(log.timestamp))
                )

                if (log.bytesTransferred > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    DetailRow(
                        label = stringResource(R.string.data_transferred),
                        value = android.text.format.Formatter.formatFileSize(context, log.bytesTransferred)
                    )
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(log.packageName))
                    Toast.makeText(context, context.getString(R.string.package_copied), Toast.LENGTH_SHORT).show()
                },
                shape = M3ShapesTokens.CornerMedium,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.copy_package), fontSize = 12.sp)
            }

            if (!log.isBlocked && log.destinationHost.isNotBlank()) {
                Button(
                    onClick = {
                        onBlockDomain(log.destinationHost)
                        Toast.makeText(context, context.getString(R.string.domain_blocked_notice), Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    shape = M3ShapesTokens.CornerMedium,
                    colors = ButtonDefaults.buttonColors(containerColor = netGuardian.blocked),
                    modifier = Modifier
                        .testTag("block_domain_button")
                        .weight(1f)
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.block_domain_action), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isMonospace: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
                ),
                color = valueColor
            )
            if (actionIcon != null && onAction != null) {
                IconButton(
                    onClick = onAction,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
