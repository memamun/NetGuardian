package com.example.ui.settings

import com.example.ui.theme.AppSpacing
import androidx.compose.ui.res.stringResource

import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.firewall.BlockAllTileService
import com.example.firewall.FirewallTileService
import com.example.firewall.MobileDataTileService
import com.example.firewall.TileHelper
import com.example.firewall.WifiOnlyTileService
import com.example.ui.theme.netGuardian

@Composable
fun QuickSettingsTilesSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showManualInstructions by remember { mutableStateOf(false) }

    val hasCellular = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.ui_quick_settings_tiles),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            ) {
                Text(
                    text = stringResource(R.string.ui_system_controls),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(AppSpacing.content),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.ui_control_netguardian_directly_from_your_android_notification),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. Firewall Tile
                TilePreviewItem(
                    title = "Firewall Tile",
                    subtitle = "Toggle on-device protection with permission checking and instant pause",
                    iconRes = R.drawable.ic_tile_firewall,
                    statusBadge = "Main Switch",
                    onAddToShade = {
                        TileHelper.requestAddTile(
                            context = context,
                            tileClass = FirewallTileService::class.java,
                            label = context.getString(R.string.quick_settings_tile_firewall),
                            iconResId = R.drawable.ic_tile_firewall
                        ) { added ->
                            val msg = if (added) "Firewall tile added to Quick Settings" else "Tile setup prompt completed"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                // 2. Block All Tile
                TilePreviewItem(
                    title = "Block All Tile",
                    subtitle = "Emergency lockdown: instantly blocks all non-system user apps while preserving rules",
                    iconRes = R.drawable.ic_tile_block_all,
                    statusBadge = "Strict Mode",
                    onAddToShade = {
                        TileHelper.requestAddTile(
                            context = context,
                            tileClass = BlockAllTileService::class.java,
                            label = context.getString(R.string.quick_settings_tile_block_all),
                            iconResId = R.drawable.ic_tile_block_all
                        ) { added ->
                            val msg = if (added) "Block All tile added to Quick Settings" else "Tile setup prompt completed"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                // 3. Wi-Fi Only Tile
                TilePreviewItem(
                    title = "Wi-Fi Only Tile",
                    subtitle = if (hasCellular) "Restricts data to Wi-Fi networks and suspends mobile data" else "Unsupported on this device (No cellular radio)",
                    iconRes = R.drawable.ic_tile_wifi_only,
                    statusBadge = if (hasCellular) "Network Filter" else "Unsupported",
                    enabled = hasCellular,
                    onAddToShade = {
                        if (hasCellular) {
                            TileHelper.requestAddTile(
                                context = context,
                                tileClass = WifiOnlyTileService::class.java,
                                label = context.getString(R.string.quick_settings_tile_wifi_only),
                                iconResId = R.drawable.ic_tile_wifi_only
                            ) { added ->
                                val msg = if (added) "Wi-Fi Only tile added to Quick Settings" else "Tile setup prompt completed"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )

                // 4. Mobile Data Tile
                TilePreviewItem(
                    title = "Mobile Data Tile",
                    subtitle = if (hasCellular) "App-level firewall cellular rule (independent of Android SIM radio toggle)" else "Unsupported on this device (No cellular radio)",
                    iconRes = R.drawable.ic_tile_mobile_data,
                    statusBadge = if (hasCellular) "Firewall Policy" else "Unsupported",
                    enabled = hasCellular,
                    onAddToShade = {
                        if (hasCellular) {
                            TileHelper.requestAddTile(
                                context = context,
                                tileClass = MobileDataTileService::class.java,
                                label = context.getString(R.string.quick_settings_tile_mobile_data),
                                iconResId = R.drawable.ic_tile_mobile_data
                            ) { added ->
                                val msg = if (added) "Mobile Data tile added to Quick Settings" else "Tile setup prompt completed"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Expandable Instructions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showManualInstructions = !showManualInstructions
                        }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.ui_how_to_add_tiles_manually),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Icon(
                        imageVector = if (showManualInstructions) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(
                    visible = showManualInstructions,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(AppSpacing.medium),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
                    ) {
                        StepGuideRow(step = "1", text = stringResource(R.string.ui_swipe_down_twice_from_the_top_of))
                        StepGuideRow(step = "2", text = stringResource(R.string.ui_tap_the_edit_or_pencil_icon_in))
                        StepGuideRow(step = "3", text = stringResource(R.string.ui_scroll_down_to_locate_the_available_netguardian))
                        StepGuideRow(step = "4", text = stringResource(R.string.ui_drag_and_drop_the_tiles_into_your))
                    }
                }
            }
        }
    }
}

@Composable
private fun TilePreviewItem(
    title: String,
    subtitle: String,
    iconRes: Int,
    statusBadge: String,
    enabled: Boolean = true,
    onAddToShade: () -> Unit
) {
    val netGuardian = MaterialTheme.netGuardian
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
            .padding(AppSpacing.medium),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = CircleShape,
                    color = if (enabled) netGuardian.allowedContainer else MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Text(
                        text = statusBadge,
                        style = MaterialTheme.typography.labelSmall.copy( fontWeight = FontWeight.Bold),
                        color = if (enabled) netGuardian.onAllowedContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (TileHelper.isAddTilePromptSupported() && enabled) {
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAddToShade()
                },
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.ui_add),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun StepGuideRow(step: String, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold,)
            )
        }

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
