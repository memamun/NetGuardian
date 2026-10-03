package com.example.ui.privacy

import com.example.firewall.FirewallState


import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.Text
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import com.example.ui.components.DnsAddressField
import com.example.R
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.UpstreamDnsType
import com.example.database.BlocklistEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@Composable
fun PrivacyScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val blocklist by viewModel.blocklist.collectAsStateWithLifecycle()
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val snapshot by viewModel.firewallSnapshot.collectAsStateWithLifecycle()
    val dnsLabel = stringResource(R.string.ui_dns_filtering)
    val haptic = LocalHapticFeedback.current

    var newDomainText by rememberSaveable { mutableStateOf("") }
    val domainError by viewModel.domainValidationError.collectAsStateWithLifecycle()

    val adDomains = remember(blocklist) { blocklist.filter { it.category == "AD" } }
    val trackerDomains = remember(blocklist) { blocklist.filter { it.category == "TRACKER" } }
    val malwareDomains = remember(blocklist) { blocklist.filter { it.category == "MALWARE" } }
    val customDomains = remember(blocklist) { blocklist.filter { it.category == "CUSTOM" } }

    val areAdsEnabled = remember(adDomains) { adDomains.any { it.isEnabled } }
    val areTrackersEnabled = remember(trackerDomains) { trackerDomains.any { it.isEnabled } }
    val isMalwareEnabled = remember(malwareDomains) { malwareDomains.any { it.isEnabled } }

    // Derive truthful DNS status from both the preference AND the running firewall state
    val dnsStatusResId = remember(prefs.dnsFilteringEnabled, snapshot.state, snapshot.dnsProtectionEffective) {
        if (!prefs.dnsFilteringEnabled) {
            R.string.dns_status_disabled
        } else {
            when (snapshot.state) {
                FirewallState.RUNNING -> if (snapshot.dnsProtectionEffective) R.string.dns_status_active else R.string.dns_status_limited
                FirewallState.STARTING -> R.string.dns_status_starting
                FirewallState.PAUSED -> R.string.dns_status_paused
                FirewallState.ERROR -> R.string.dns_status_error
                FirewallState.STOPPED, FirewallState.VPN_PERMISSION_REQUIRED -> R.string.dns_status_saved_not_running
            }
        }
    }
    val isDnsEffective = prefs.dnsFilteringEnabled && snapshot.state == FirewallState.RUNNING && snapshot.dnsProtectionEffective

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("privacy_screen"),
        contentPadding = PaddingValues(AppSpacing.content),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.content)
    ) {
        // Master DNS Toggle Card
        item {
            Card(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.setDnsFiltering(!prefs.dnsFilteringEnabled)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("master_dns_card")
                    .semantics { role = Role.Switch },
                shape = M3ShapesTokens.CornerLargeIncreased,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = opticalInnerShape(20.dp, 18.dp),
                            color = if (prefs.dnsFilteringEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = null,
                                    tint = if (prefs.dnsFilteringEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Text(
                                text = stringResource(R.string.ui_dns_filtering),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(dnsStatusResId),
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    isDnsEffective -> MaterialTheme.colorScheme.primary
                                    prefs.dnsFilteringEnabled && snapshot.state == FirewallState.STARTING -> MaterialTheme.colorScheme.tertiary
                                    prefs.dnsFilteringEnabled && (snapshot.state == FirewallState.ERROR || snapshot.state == FirewallState.STOPPED || snapshot.state == FirewallState.VPN_PERMISSION_REQUIRED) -> MaterialTheme.colorScheme.error
                                    prefs.dnsFilteringEnabled && snapshot.state == FirewallState.PAUSED -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                    Switch(
                        checked = prefs.dnsFilteringEnabled,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setDnsFiltering(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier.testTag("dns_toggle_switch").semantics { contentDescription = dnsLabel }
                    )
                }
            }
        }

        // DNS Engine Limitation Notice
        if (prefs.dnsFilteringEnabled) {
            item {
                Card(
                    shape = M3ShapesTokens.CornerMedium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.dns_engine_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Protection Categories Section
        item {
            Text(
                text = stringResource(R.string.ui_built_in_protection_rules),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            CategoryCard(
                title = "Ad Networks",
                description = "Blocks mobile ad networks, banner requests, and video ads",
                domainCount = adDomains.size,
                icon = Icons.Default.AdsClick,
                enabled = areAdsEnabled,
                onToggle = { enable -> viewModel.toggleCategory("AD", enable) }
            )
        }

        item {
            CategoryCard(
                title = "Trackers & Analytics",
                description = "Stops background analytics, user SDK beacons, and telemetry",
                domainCount = trackerDomains.size,
                icon = Icons.Default.TrackChanges,
                enabled = areTrackersEnabled,
                onToggle = { enable -> viewModel.toggleCategory("TRACKER", enable) }
            )
        }

        item {
            CategoryCard(
                title = "Malware & Phishing",
                description = "Blocks known scam servers, phishing domains, and malicious hosts",
                domainCount = malwareDomains.size,
                icon = Icons.Default.BugReport,
                enabled = isMalwareEnabled,
                onToggle = { enable -> viewModel.toggleCategory("MALWARE", enable) }
            )
        }

        // Section Title: Custom Rules
        item {
            Text(
                text = stringResource(R.string.ui_custom_domain_blocklist),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = M3ShapesTokens.CornerLargeIncreased,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(AppSpacing.content),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
                ) {
                    Text(
                        text = stringResource(R.string.ui_add_custom_block_rule),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                        verticalAlignment = Alignment.Top
                    ) {
                        OutlinedTextField(
                            value = newDomainText,
                            onValueChange = {
                                newDomainText = it
                                viewModel.clearDomainValidationError()
                            },
                            label = { Text(stringResource(R.string.domain_label)) },
                            placeholder = { Text(stringResource(R.string.ui_tracking_example_com)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            singleLine = true,
                            isError = domainError != null,
                            supportingText = if (domainError != null) {
                                { Text(domainError.orEmpty(), color = MaterialTheme.colorScheme.error) }
                            } else null,
                            shape = M3ShapesTokens.CornerSmall,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                errorBorderColor = MaterialTheme.colorScheme.error
                            )
                        )
                        Button(
                            onClick = {
                                if (newDomainText.isNotBlank()) {
                                    viewModel.addCustomBlocklistDomain(newDomainText)
                                    // Only clear on success — validation error preserves input
                                    if (viewModel.domainValidationError.value == null) {
                                        newDomainText = ""
                                    }
                                }
                            },
                            enabled = newDomainText.isNotBlank(),
                            shape = M3ShapesTokens.CornerMedium,
                            modifier = Modifier.height(56.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_domain))
                        }
                    }
                }
            }
        }

        // Custom Domains List
        if (customDomains.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.ui_no_custom_block_rules_added_yet),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        } else {
            items(customDomains, key = { it.id }) { domainItem ->
                CustomDomainItem(
                    item = domainItem,
                    onToggle = { viewModel.toggleBlocklistItem(domainItem.id, it) },
                    onDelete = { viewModel.deleteBlocklistItem(domainItem.id) }
                )
            }
        }

        // Upstream DNS Resolver (Local First, Zero Mandatory Third-Party Servers)
        item {
            Text(
                text = stringResource(R.string.ui_upstream_dns_resolver),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upstream_dns_card"),
                shape = M3ShapesTokens.CornerLargeIncreased,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(AppSpacing.content),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = opticalInnerShape(20.dp, 16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Router,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.ui_upstream_dns_resolver_2),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.ui_choose_where_allowed_domain_queries_resolve_no),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                        for (dnsOption in UpstreamDnsType.values()) {
                            val isSelected = prefs.upstreamDnsType == dnsOption
                            Surface(
                                modifier = Modifier.fillMaxWidth().selectable(
                                    selected = isSelected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.setUpstreamDnsType(dnsOption)
                                    }
                                ),
                                shape = opticalInnerShape(20.dp, 8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(AppSpacing.medium),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MaterialTheme.colorScheme.primary,
                                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = dnsOption.label,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = dnsOption.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (prefs.upstreamDnsType == UpstreamDnsType.CUSTOM) {
                        DnsAddressField(
                            address = prefs.customDnsIp,
                            onSave = viewModel::setCustomDnsIp
                        )
                    }
                }
            }
        }

        // Resolver Limitation Notice
        item {
            Card(
                shape = M3ShapesTokens.CornerMedium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.dns_resolver_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    title: String,
    description: String,
    domainCount: Int,
    icon: ImageVector,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Card(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onToggle(!enabled)
        },
        modifier = modifier
            .fillMaxWidth()
            .semantics { role = Role.Switch },
        shape = M3ShapesTokens.CornerLargeIncreased,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.content),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = opticalInnerShape(20.dp, 16.dp),
                        color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.tiny),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = M3ShapesTokens.CornerExtraSmall,
                            color = if (enabled && domainCount > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Text(
                                text = if (domainCount > 0) "$domainCount patterns" else "Active (built-in)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = if (enabled && domainCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = enabled,
                    modifier = Modifier.semantics { contentDescription = title },
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggle(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun CustomDomainItem(
    item: BlocklistEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val netGuardian = MaterialTheme.netGuardian
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = M3ShapesTokens.CornerMedium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.domain,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                ),
                color = if (item.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = item.isEnabled,
                    modifier = Modifier.semantics { contentDescription = item.domain },
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.ui_delete),
                        tint = netGuardian.blocked,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
