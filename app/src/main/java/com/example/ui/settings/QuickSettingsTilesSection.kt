package com.example.ui.settings

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
    var showManualInstructions by remember { mutableStateOf(false) }

    val hasCellular = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "QUICK SETTINGS TILES",
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
                    text = "SYSTEM CONTROLS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontSize = 10.sp
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
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Control NetGuardian directly from your Android notification pull-down shade without opening the app.",
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
                        .clickable { showManualInstructions = !showManualInstructions }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "How to add tiles manually",
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
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StepGuideRow(step = "1", text = "Swipe down twice from the top of your screen to expand Quick Settings.")
                        StepGuideRow(step = "2", text = "Tap the Edit or Pencil icon (✏️) in the Quick Settings menu.")
                        StepGuideRow(step = "3", text = "Scroll down to locate the available NetGuardian tiles.")
                        StepGuideRow(step = "4", text = "Drag and drop the tiles into your active quick settings layout.")
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
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
                    shape = RoundedCornerShape(4.dp),
                    color = if (enabled) netGuardian.allowedContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = statusBadge,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = if (enabled) netGuardian.onAllowedContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (TileHelper.isAddTilePromptSupported() && enabled) {
            OutlinedButton(
                onClick = onAddToShade,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
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
                    text = "Add",
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
            )
        }

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
