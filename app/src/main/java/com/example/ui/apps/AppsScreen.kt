package com.example.ui.apps


import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.ui.components.AppSearchField
import com.example.ui.components.ScreenState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppItem
import com.example.database.AppRuleEntity
import com.example.ui.AppFilter
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
private fun rememberAppIcon(packageName: String): Bitmap? {
    val cached = remember(packageName) { AppIconCache.get(packageName) }
    var icon by remember(packageName) { mutableStateOf(cached) }
    val context = LocalContext.current.applicationContext
    if (icon == null) {
        LaunchedEffect(packageName) {
            withContext(Dispatchers.IO) {
                val loaded = AppIconCache.load(context, packageName)
                if (loaded != null) {
                    icon = loaded
                }
            }
        }
    }
    return icon
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val apps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val isAppsLoading by viewModel.isAppsLoading.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val userPrefs by viewModel.userPreferences.collectAsStateWithLifecycle()

    var selectedAppForDetail by remember { mutableStateOf<AppItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    val netGuardian = MaterialTheme.netGuardian

    // Dynamic counts for overflow filter menu
    val allCount = installedApps.size
    val userCount = remember(installedApps) { installedApps.count { !it.isSystemApp } }
    val systemCount = remember(installedApps) { installedApps.count { it.isSystemApp } }
    val blockedCount = remember(installedApps) { installedApps.count { it.rule.isBlocked } }
    val restrictedCount = remember(installedApps) {
        installedApps.count { it.rule.isBlocked || it.rule.blockWifi || it.rule.blockMobile || it.rule.blockBackground || it.rule.blockScreenOff }
    }

    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("apps_screen")
    ) {
        // Dismissible App Access Notice
        if (!userPrefs.hasSeenAppManagerExplanation) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("app_access_explanation_card"),
                shape = M3ShapesTokens.CornerMedium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.ui_tap_any_app_to_customize_per_network),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { viewModel.setHasSeenAppManagerExplanation(true) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.ui_dismiss),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Compact Search & Action Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
        ) {
            // Search Input Field
            AppSearchField(
                query = searchQuery,
                onQueryChange = { viewModel.searchQuery.value = it },
                label = stringResource(R.string.search_apps),
                modifier = Modifier.weight(1f).testTag("apps_search_field")
            )

            // Overflow "..." Menu Button
            Box {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isMenuExpanded = true
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("apps_overflow_menu_button")
                ) {
                    val hasActiveFilter = selectedFilter != AppFilter.ALL || selectedTab != 0
                    BadgedBox(
                        badge = {
                            if (hasActiveFilter) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(8.dp)
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.ui_options_and_filters),
                            tint = if (hasActiveFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Dropdown Menu containing Filters, Batch Actions, and View Mode
                DropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false },
                    modifier = Modifier
                        .widthIn(min = 230.dp, max = 280.dp)
                        .testTag("apps_dropdown_menu"),
                    shape = M3ShapesTokens.CornerMedium,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    // Category 1: Filters
                    Text(
                        text = stringResource(R.string.ui_filter_apps),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold,),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )

                    AppFilter.values().forEach { filter ->
                        val (label, count, icon) = when (filter) {
                            AppFilter.ALL -> Triple("All Apps", allCount, Icons.Default.Apps)
                            AppFilter.USER -> Triple("User Apps", userCount, Icons.Default.Person)
                            AppFilter.SYSTEM -> Triple("System Apps", systemCount, Icons.Default.Android)
                            AppFilter.BLOCKED -> Triple("Blocked Apps", blockedCount, Icons.Default.Block)
                            AppFilter.RESTRICTED -> Triple("Restricted Apps", restrictedCount, Icons.Default.Tune)
                        }
                        val isSelected = selectedFilter == filter
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
                            leadingIcon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = stringResource(R.string.ui_selected),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.selectedFilter.value = filter
                                isMenuExpanded = false
                            }
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    // Category 2: Batch Actions
                    Text(
                        text = stringResource(R.string.ui_batch_actions),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold,),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.ui_block_user_apps),
                                style = MaterialTheme.typography.bodyMedium,
                                color = netGuardian.blocked
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = null,
                                tint = netGuardian.blocked,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.blockAllNonSystem(true)
                            isMenuExpanded = false
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.ui_allow_all_apps),
                                style = MaterialTheme.typography.bodyMedium,
                                color = netGuardian.onAllowedContainer
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = netGuardian.onAllowedContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.allowAllApps()
                            isMenuExpanded = false
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.ui_block_all_background),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.SyncDisabled,
                                contentDescription = null,
                                tint = netGuardian.warning,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setBlockAllBackground(true)
                            isMenuExpanded = false
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.ui_allow_all_background),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setBlockAllBackground(false)
                            isMenuExpanded = false
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    // Category 3: View Mode
                    Text(
                        text = stringResource(R.string.ui_view_mode),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold,),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.ui_unified_app_list),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Apps,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = if (selectedTab == 0) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = stringResource(R.string.ui_selected),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null,
                        onClick = {
                            selectedTab = 0
                            isMenuExpanded = false
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.ui_standby_idle_policies),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.SyncDisabled,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = if (selectedTab == 1) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = stringResource(R.string.ui_selected),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null,
                        onClick = {
                            selectedTab = 1
                            isMenuExpanded = false
                        }
                    )
                }
            }
        }

        // Active Filter & Count Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${apps.size} ${if (apps.size == 1) "app" else "apps"}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedTab == 1) {
                    Surface(
                        onClick = { selectedTab = 0 },
                        shape = M3ShapesTokens.CornerSmall,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.tiny)
                        ) {
                            Text(
                                text = stringResource(R.string.ui_standby_view),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.ui_return_to_standard_view),
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                if (selectedFilter != AppFilter.ALL) {
                    val filterName = when (selectedFilter) {
                        AppFilter.USER -> "User"
                        AppFilter.SYSTEM -> "System"
                        AppFilter.BLOCKED -> "Blocked"
                        AppFilter.RESTRICTED -> "Restricted"
                        AppFilter.ALL -> "All"
                    }
                    Surface(
                        onClick = { viewModel.selectedFilter.value = AppFilter.ALL },
                        shape = M3ShapesTokens.CornerSmall,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.tiny)
                        ) {
                            Text(
                                text = "Filter: $filterName",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.ui_clear_filter),
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        if (selectedTab == 0) {
            // Body List of Apps
            if (isAppsLoading && apps.isEmpty()) {
                ScreenState(
                    title = stringResource(R.string.loading_apps),
                    description = stringResource(R.string.loading_apps_description),
                    loading = true, modifier = Modifier.fillMaxSize()
                )
            } else if (apps.isEmpty()) {
                ScreenState(
                    title = stringResource(R.string.no_apps),
                    description = stringResource(R.string.no_apps_description),
                    icon = Icons.Default.Search, modifier = Modifier.fillMaxSize(),
                    action = {
                        if (searchQuery.isNotBlank() || selectedFilter != AppFilter.ALL) {
                            FilledTonalButton(onClick = {
                                viewModel.searchQuery.value = ""
                                viewModel.selectedFilter.value = AppFilter.ALL
                            }) { Text(stringResource(R.string.clear_filters)) }
                        }
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = apps,
                        key = { it.packageName },
                        contentType = { "app_item" }
                    ) { appItem ->
                        AppItemCard(
                            app = appItem,
                            onToggleBlock = remember(appItem.rule) { { viewModel.toggleAppBlock(appItem.rule) } },
                            onToggleWifi = remember(appItem.rule) { { viewModel.toggleAppWifi(appItem.rule) } },
                            onToggleMobile = remember(appItem.rule) { { viewModel.toggleAppMobile(appItem.rule) } },
                            onToggleBackground = remember(appItem.rule) { { viewModel.toggleAppBackground(appItem.rule) } },
                            onClick = remember(appItem) { { selectedAppForDetail = appItem } }
                        )
                    }
                }
            }
        } else {
            // Tab 1: Background & Standby Policies
            BackgroundActivitySection(
                apps = apps,
                onToggleBackground = { viewModel.toggleAppBackground(it.rule) },
                onToggleScreenOff = { viewModel.toggleAppScreenOff(it.rule) },
                onToggleDeviceIdle = { viewModel.toggleAppDeviceIdle(it.rule) },
                onSetBlockAllBackground = { viewModel.setBlockAllBackground(it) },
                onSetBlockAllScreenOff = { viewModel.setBlockAllScreenOff(it) },
                onSetBlockAllDeviceIdle = { viewModel.setBlockAllDeviceIdle(it) },
                onAppClick = { selectedAppForDetail = it }
            )
        }
    }

    // Detail Bottom Sheet
    selectedAppForDetail?.let { appItem ->
        ModalBottomSheet(
            onDismissRequest = { selectedAppForDetail = null },
            sheetState = sheetState,
            shape = M3ShapesTokens.CornerExtraLargeTop,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            AppDetailSheetContent(
                app = appItem,
                onUpdateRule = { updatedRule ->
                    viewModel.updateAppRule(updatedRule)
                    selectedAppForDetail = appItem.copy(rule = updatedRule)
                }
            )
        }
    }
}

@Composable
private fun AppActionToggle(
    icon: ImageVector,
    contentDescription: String,
    isBlocked: Boolean,
    blockedContainerColor: Color,
    blockedContentColor: Color,
    allowedContainerColor: Color,
    allowedContentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            shape = CircleShape,
            color = if (isBlocked) blockedContainerColor else allowedContainerColor,
            border = BorderStroke(
                1.dp,
                if (isBlocked) blockedContentColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = if (isBlocked) blockedContentColor else allowedContentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AppItemCard(
    app: AppItem,
    onToggleBlock: () -> Unit,
    onToggleWifi: () -> Unit,
    onToggleMobile: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleBackground: () -> Unit = {},
    onToggleScreenOff: () -> Unit = {}
) {
    val rule = app.rule
    val isBlocked = rule.isBlocked
    val isWifiBlocked = rule.blockWifi
    val isMobileBlocked = rule.blockMobile
    val isBgBlocked = rule.blockBackground || isBlocked

    val netGuardian = MaterialTheme.netGuardian

    // Semantic status pill logic
    val (statusLabel, statusBgColor, statusContentColor) = when {
        isBlocked -> Triple("BLOCKED", netGuardian.blockedContainer, netGuardian.blocked)
        isWifiBlocked && !isMobileBlocked -> Triple("MOBILE ONLY", netGuardian.warningContainer, netGuardian.warning)
        !isWifiBlocked && isMobileBlocked -> Triple("WI-FI ONLY", netGuardian.warningContainer, netGuardian.warning)
        (isWifiBlocked && isMobileBlocked) || rule.blockBackground || rule.blockScreenOff -> Triple("RESTRICTED", netGuardian.warningContainer, netGuardian.warning)
        else -> Triple("ALLOWED", netGuardian.allowedContainer, netGuardian.onAllowedContainer)
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_card_${app.packageName}"),
        shape = M3ShapesTokens.CornerLargeIncreased,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isBlocked) netGuardian.blocked.copy(alpha = 0.45f)
            else if (isWifiBlocked || isMobileBlocked || rule.blockBackground) netGuardian.warning.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: App Icon, Names & Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(opticalInnerShape(20.dp, 14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = rememberAppIcon(app.packageName)
                    if (icon != null) {
                        Image(
                            bitmap = icon.asImageBitmap(),
                            contentDescription = "${app.appName} Icon",
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // App Names and Badges
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (app.isSystemApp) {
                            Surface(
                                shape = M3ShapesTokens.CornerExtraSmall,
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Text(
                                    text = stringResource(R.string.ui_system),
                                    style = MaterialTheme.typography.labelSmall.copy( fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Status Pill
                Surface(
                    shape = M3ShapesTokens.CornerSmall,
                    color = statusBgColor,
                    border = BorderStroke(1.dp, statusContentColor.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold,),
                        color = statusContentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Default policies remain available in the controls and detail sheet; highlight exceptions here.
            if (!rule.blockTrackers || isBgBlocked || rule.blockScreenOff || rule.blockDeviceIdle) {
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.tiny)
                ) {
                    if (!rule.blockTrackers) MicroPolicyBadge(
                        icon = Icons.Default.Security, label = stringResource(R.string.trackers_allowed), active = false)
                    if (isBgBlocked) MicroPolicyBadge(
                        icon = Icons.Default.SyncDisabled, label = stringResource(R.string.background_blocked), active = false)
                    if (rule.blockScreenOff) MicroPolicyBadge(
                        icon = Icons.Default.ScreenLockPortrait, label = stringResource(R.string.screen_off_blocked), active = false)
                    if (rule.blockDeviceIdle) MicroPolicyBadge(
                        icon = Icons.Default.HourglassEmpty, label = stringResource(R.string.idle_blocked), active = false)
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                thickness = 0.8.dp
            )

            // Bottom: Quick Action Controls Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.ui_quick_controls),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Wi-Fi Toggle
                    AppActionToggle(
                        icon = Icons.Default.Wifi,
                        contentDescription = if (isWifiBlocked) "Unblock Wi-Fi" else "Block Wi-Fi",
                        isBlocked = isWifiBlocked,
                        blockedContainerColor = netGuardian.blockedContainer,
                        blockedContentColor = netGuardian.blocked,
                        allowedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        allowedContentColor = MaterialTheme.colorScheme.primary,
                        onClick = onToggleWifi
                    )

                    // Mobile Data Toggle
                    AppActionToggle(
                        icon = Icons.Default.SignalCellularAlt,
                        contentDescription = if (isMobileBlocked) "Unblock Mobile Data" else "Block Mobile Data",
                        isBlocked = isMobileBlocked,
                        blockedContainerColor = netGuardian.blockedContainer,
                        blockedContentColor = netGuardian.blocked,
                        allowedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        allowedContentColor = MaterialTheme.colorScheme.primary,
                        onClick = onToggleMobile
                    )

                    // Background Sync Toggle
                    AppActionToggle(
                        icon = if (rule.blockBackground) Icons.Default.SyncDisabled else Icons.Default.Sync,
                        contentDescription = if (rule.blockBackground) "Unblock Background" else "Block Background",
                        isBlocked = rule.blockBackground,
                        blockedContainerColor = netGuardian.blockedContainer,
                        blockedContentColor = netGuardian.blocked,
                        allowedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        allowedContentColor = MaterialTheme.colorScheme.primary,
                        onClick = onToggleBackground
                    )

                    // Master Shield / Block Toggle
                    AppActionToggle(
                        icon = if (isBlocked) Icons.Default.Block else Icons.Default.Security,
                        contentDescription = if (isBlocked) "Unblock App" else "Block App",
                        isBlocked = isBlocked,
                        blockedContainerColor = netGuardian.blocked,
                        blockedContentColor = netGuardian.onBlocked,
                        allowedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        allowedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onToggleBlock
                    )
                }
            }
        }
    }
}

@Composable
private fun MicroPolicyBadge(
    icon: ImageVector,
    label: String,
    active: Boolean,
    activeContainer: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    activeContent: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    inactiveContainer: Color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f),
    inactiveContent: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
) {
    Surface(
        shape = M3ShapesTokens.CornerExtraSmall,
        color = if (active) activeContainer else inactiveContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) activeContent else inactiveContent,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy( fontWeight = FontWeight.Medium),
                color = if (active) activeContent else inactiveContent
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailSheetContent(
    app: AppItem,
    onUpdateRule: (AppRuleEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val rule = app.rule
    val netGuardian = MaterialTheme.netGuardian

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("app_detail_sheet"),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.content)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                val icon = rememberAppIcon(app.packageName)
                if (icon != null) {
                    Image(
                        bitmap = icon.asImageBitmap(),
                        contentDescription = "${app.appName} Icon",
                        modifier = Modifier.size(42.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    val (statusText, statusBg, statusColor) = when {
                        rule.isBlocked -> Triple("BLOCKED", netGuardian.blockedContainer, netGuardian.blocked)
                        rule.blockWifi || rule.blockMobile || rule.blockBackground || rule.blockScreenOff -> Triple("RESTRICTED", netGuardian.warningContainer, netGuardian.warning)
                        else -> Triple("ALLOWED", netGuardian.allowedContainer, netGuardian.onAllowedContainer)
                    }
                    Surface(
                        shape = M3ShapesTokens.CornerExtraSmall,
                        color = statusBg
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "UID ${app.uid} • v${app.versionName}${if (app.isSystemApp) " • System App" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Quick Preset Segmented Row
        val presets = listOf("All Allow", "Block All", "Wi-Fi Only", "Cellular Only")
        val selectedPresetIndex = when {
            rule.isBlocked -> 1
            !rule.isBlocked && rule.blockWifi && !rule.blockMobile -> 3
            else -> -1
        }

        val haptic = LocalHapticFeedback.current

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            presets.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = presets.size),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        when (index) {
                            0 -> onUpdateRule(rule.copy(isBlocked = false, blockWifi = false, blockMobile = false, blockBackground = false))
                            1 -> onUpdateRule(rule.copy(isBlocked = true, blockWifi = false, blockMobile = false))
                            2 -> onUpdateRule(rule.copy(isBlocked = false, blockWifi = false, blockMobile = true))
                            3 -> onUpdateRule(rule.copy(isBlocked = false, blockWifi = true, blockMobile = false))
                        }
                    },
                    selected = index == selectedPresetIndex,
                    label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        // Category 1: Network Interfaces Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = M3ShapesTokens.CornerLargeIncreased,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                Text(
                    text = stringResource(R.string.ui_network_access),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                DetailSwitchRow(
                    icon = Icons.Default.Wifi,
                    title = "Wi-Fi Network",
                    checked = !rule.blockWifi && !rule.isBlocked,
                    onCheckedChange = { allow ->
                        onUpdateRule(rule.copy(blockWifi = !allow))
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                DetailSwitchRow(
                    icon = Icons.Default.SignalCellularAlt,
                    title = "Mobile Cellular Data",
                    checked = !rule.blockMobile && !rule.isBlocked,
                    onCheckedChange = { allow ->
                        onUpdateRule(rule.copy(blockMobile = !allow))
                    }
                )
            }
        }

        // Category 2: Background & Standby Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = M3ShapesTokens.CornerLargeIncreased,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                Text(
                    text = stringResource(R.string.ui_background_standby),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                DetailSwitchRow(
                    icon = Icons.Default.Sync,
                    title = "Background Network Sync",
                    checked = !rule.blockBackground && !rule.isBlocked,
                    onCheckedChange = { allow ->
                        onUpdateRule(rule.copy(blockBackground = !allow))
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                DetailSwitchRow(
                    icon = Icons.Default.ScreenLockPortrait,
                    title = "Block when Screen is Off",
                    checked = rule.blockScreenOff,
                    onCheckedChange = { block ->
                        onUpdateRule(rule.copy(blockScreenOff = block))
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                DetailSwitchRow(
                    icon = Icons.Default.HourglassEmpty,
                    title = "Block in System Standby (Doze)",
                    checked = rule.blockDeviceIdle,
                    onCheckedChange = { block ->
                        onUpdateRule(rule.copy(blockDeviceIdle = block))
                    }
                )
            }
        }

        // Category 3: Privacy & Protection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = M3ShapesTokens.CornerLargeIncreased,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
            ) {
                Text(
                    text = stringResource(R.string.ui_privacy_shield),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                DetailSwitchRow(
                    icon = Icons.Default.Security,
                    title = "Block Trackers & Ads",
                    checked = rule.blockTrackers,
                    onCheckedChange = { blockTrackers ->
                        onUpdateRule(rule.copy(blockTrackers = blockTrackers))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun BackgroundActivitySection(
    apps: List<AppItem>,
    onToggleBackground: (AppItem) -> Unit,
    onToggleScreenOff: (AppItem) -> Unit,
    onToggleDeviceIdle: (AppItem) -> Unit,
    onSetBlockAllBackground: (Boolean) -> Unit,
    onSetBlockAllScreenOff: (Boolean) -> Unit,
    onSetBlockAllDeviceIdle: (Boolean) -> Unit,
    onAppClick: (AppItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val netGuardian = MaterialTheme.netGuardian

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("background_activity_section"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Global Quick Actions Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(AppSpacing.content),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ui_global_background_rules),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.ui_apply_restrictions_to_all_non_system_user),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
                    ) {
                        FilledTonalButton(
                            onClick = { onSetBlockAllBackground(true) },
                            modifier = Modifier.weight(1f),
                            shape = M3ShapesTokens.CornerFull,
                            contentPadding = PaddingValues(vertical = 8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = netGuardian.blockedContainer,
                                contentColor = netGuardian.blocked
                            )
                        ) {
                            Icon(Icons.Default.SyncDisabled, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.ui_background), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = { onSetBlockAllScreenOff(true) },
                            modifier = Modifier.weight(1f),
                            shape = M3ShapesTokens.CornerFull,
                            contentPadding = PaddingValues(vertical = 8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = netGuardian.warningContainer,
                                contentColor = netGuardian.warning
                            )
                        ) {
                            Icon(Icons.Default.ScreenLockPortrait, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.ui_screen_off), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = { onSetBlockAllDeviceIdle(true) },
                            modifier = Modifier.weight(1f),
                            shape = M3ShapesTokens.CornerFull,
                            contentPadding = PaddingValues(vertical = 8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.ui_idle_standby), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { onSetBlockAllBackground(false) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(stringResource(R.string.ui_allow_all_background), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Section Title
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${apps.size} apps",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // List of Per-App Background Items
        items(apps, key = { "bg_${it.packageName}" }) { appItem ->
            AppBackgroundItemCard(
                app = appItem,
                onToggleBackground = { onToggleBackground(appItem) },
                onToggleScreenOff = { onToggleScreenOff(appItem) },
                onToggleDeviceIdle = { onToggleDeviceIdle(appItem) },
                onClick = { onAppClick(appItem) }
            )
        }
    }
}

@Composable
fun AppBackgroundItemCard(
    app: AppItem,
    onToggleBackground: () -> Unit,
    onToggleScreenOff: () -> Unit,
    onToggleDeviceIdle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rule = app.rule
    val isBgBlocked = rule.blockBackground || rule.isBlocked
    val netGuardian = MaterialTheme.netGuardian

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_bg_card_${app.packageName}"),
        shape = M3ShapesTokens.CornerLargeIncreased,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isBgBlocked) netGuardian.blocked.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            val bgIcon = rememberAppIcon(app.packageName)
            if (bgIcon != null) {
                Image(
                    bitmap = bgIcon.asImageBitmap(),
                    contentDescription = "${app.appName} icon",
                    modifier = Modifier
                        .size(46.dp)
                        .clip(opticalInnerShape(20.dp, 14.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(opticalInnerShape(20.dp, 14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.appName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (app.isSystemApp) {
                        Surface(
                            shape = M3ShapesTokens.CornerExtraSmall,
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Text(
                                text = stringResource(R.string.ui_system),
                                style = MaterialTheme.typography.labelSmall.copy( fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 3 Action Toggles: Background Network, Screen-Off cutoff, Standby/Doze cutoff
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppActionToggle(
                    icon = if (rule.blockBackground) Icons.Default.SyncDisabled else Icons.Default.Sync,
                    contentDescription = if (rule.blockBackground) "Unblock Background" else "Block Background",
                    isBlocked = rule.blockBackground,
                    blockedContainerColor = netGuardian.blockedContainer,
                    blockedContentColor = netGuardian.blocked,
                    allowedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    allowedContentColor = MaterialTheme.colorScheme.primary,
                    onClick = onToggleBackground
                )

                AppActionToggle(
                    icon = Icons.Default.ScreenLockPortrait,
                    contentDescription = if (rule.blockScreenOff) "Disable Screen-Off Cutoff" else "Enable Screen-Off Cutoff",
                    isBlocked = rule.blockScreenOff,
                    blockedContainerColor = netGuardian.warningContainer,
                    blockedContentColor = netGuardian.warning,
                    allowedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    allowedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onToggleScreenOff
                )

                AppActionToggle(
                    icon = Icons.Default.HourglassEmpty,
                    contentDescription = if (rule.blockDeviceIdle) "Disable Idle Cutoff" else "Enable Idle Cutoff",
                    isBlocked = rule.blockDeviceIdle,
                    blockedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    blockedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    allowedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    allowedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onToggleDeviceIdle
                )
            }
        }
    }
}

@Composable
fun DetailSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange
        ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        )
    }
}
