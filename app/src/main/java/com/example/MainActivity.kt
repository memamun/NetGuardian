package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.example.ui.layout.AdaptiveNavigationScaffold
import com.example.ui.layout.rememberM3LayoutInfo
import com.example.ui.theme.M3ShapesTokens
import com.example.ui.theme.emphasizedTypography
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.AppTopBar
import com.example.ui.MainViewModel
import com.example.ui.RootNavState
import com.example.ui.apps.AppsScreen
import com.example.ui.connections.ConnectionsScreen
import com.example.ui.home.DashboardScreen
import com.example.ui.onboarding.OnboardingWizardScreen
import com.example.ui.privacy.PrivacyScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.*

private data class StatusBadgeInfo(val bg: Color, val dot: Color, val text: String, val textColor: Color)

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", R.string.nav_dashboard, Icons.Default.Dashboard)
    object Apps : Screen("apps", R.string.nav_apps, Icons.Default.Apps)
    object Logs : Screen("logs", R.string.nav_logs, Icons.Default.History)
    object Privacy : Screen("privacy", R.string.nav_privacy, Icons.Default.Dns)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private lateinit var mainViewModel: MainViewModel
    private val requestedRoute = mutableStateOf<String?>(null)

    private val vpnLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            mainViewModel.startFirewall(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(com.example.firewall.FirewallNotificationManager.EXTRA_ROUTE)?.let { route ->
            requestedRoute.value = route
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestedRoute.value = intent?.getStringExtra(com.example.firewall.FirewallNotificationManager.EXTRA_ROUTE)

        setContent {
            mainViewModel = viewModel()
            val firewallActive by mainViewModel.firewallActive.collectAsStateWithLifecycle()
            val snapshot by mainViewModel.firewallSnapshot.collectAsStateWithLifecycle()
            val userPrefs by mainViewModel.userPreferences.collectAsStateWithLifecycle()
            val rootNavState by mainViewModel.rootNavState.collectAsStateWithLifecycle()
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            val items = listOf(
                Screen.Dashboard,
                Screen.Apps,
                Screen.Logs,
                Screen.Privacy,
                Screen.Settings
            )

            androidx.compose.runtime.LaunchedEffect(rootNavState, requestedRoute.value) {
                val route = requestedRoute.value
                if (rootNavState == RootNavState.MainApp && route != null) {
                    if (items.any { it.route == route }) {
                        // Wait for NavHost to create its graph before consuming notification routes.
                        navController.currentBackStackEntryFlow.first()
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    requestedRoute.value = null
                    intent?.removeExtra(com.example.firewall.FirewallNotificationManager.EXTRA_ROUTE)
                }
            }

            NetGuardianTheme {
                when (rootNavState) {
                    RootNavState.Loading -> {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Text(stringResource(R.string.loading_protection),
                                        modifier = Modifier.padding(top = 16.dp),
                                        style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    RootNavState.Onboarding -> {
                        OnboardingWizardScreen(
                            viewModel = mainViewModel,
                            onRequestVpnPermission = { prepareIntent ->
                                vpnLauncher.launch(prepareIntent)
                            },
                            onComplete = {
                                mainViewModel.completeOnboarding()
                            }
                        )
                    }
                    RootNavState.MainApp -> {
                    val layoutInfo = rememberM3LayoutInfo()
                    val haptic = LocalHapticFeedback.current

                    val topBarContent: @Composable () -> Unit = {
                        AppTopBar(
                            title = stringResource(if (currentRoute == Screen.Dashboard.route) R.string.app_name
                                else items.firstOrNull { it.route == currentRoute }?.titleRes ?: R.string.app_name),
                            state = snapshot.state
                        )
                    }

                    val navigationContent: @Composable () -> Unit = {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Dashboard.route,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(Screen.Dashboard.route) {
                                DashboardScreen(
                                    viewModel = mainViewModel,
                                    onNavigateToApps = {
                                        navController.navigate(Screen.Apps.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onNavigateToLogs = {
                                        navController.navigate(Screen.Logs.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onVpnPermissionNeeded = { prepareIntent ->
                                        vpnLauncher.launch(prepareIntent)
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate(Screen.Settings.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                            composable(Screen.Apps.route) {
                                AppsScreen(viewModel = mainViewModel)
                            }
                            composable(Screen.Logs.route) {
                                ConnectionsScreen(viewModel = mainViewModel)
                            }
                            composable(Screen.Privacy.route) {
                                PrivacyScreen(viewModel = mainViewModel)
                            }
                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                    viewModel = mainViewModel,
                                    onRequestVpn = {
                                        val prepare = android.net.VpnService.prepare(this@MainActivity)
                                        if (prepare != null) {
                                            vpnLauncher.launch(prepare)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (layoutInfo.isCompact) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = MaterialTheme.colorScheme.surface,
                            topBar = topBarContent,
                            bottomBar = {
                                Column(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceContainer)

                                ) {
                                    HorizontalDivider(
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        tonalElevation = 0.dp,
                                        modifier = Modifier.testTag("bottom_nav_bar")
                                    ) {
                                        items.forEach { screen ->
                                            val selected = currentRoute == screen.route
                                            NavigationBarItem(
                                                selected = selected,
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    navController.navigate(screen.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                },
                                                icon = {
                                                    Icon(
                                                        imageVector = screen.icon,
                                                        contentDescription = null
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        stringResource(if (screen == Screen.Logs) R.string.nav_logs_short else screen.titleRes),
                                                        fontSize = 11.sp,
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                                ),
                                                modifier = Modifier.testTag("nav_item_${screen.route}")
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding).imePadding()) {
                                navigationContent()
                            }
                        }
                    } else {
                        // Medium / Expanded: Leading NavigationRail + Content
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                header = {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = "NetGuardian",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .testTag("navigation_rail")
                            ) {
                                Spacer(modifier = Modifier.weight(1f))
                                items.forEach { screen ->
                                    val selected = currentRoute == screen.route
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = screen.icon,
                                                contentDescription = null
                                            )
                                        },
                                        label = {
                                            Text(
                                                stringResource(if (screen == Screen.Logs) R.string.nav_logs_short else screen.titleRes),
                                                fontSize = 11.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                        ),
                                        modifier = Modifier.testTag("nav_item_${screen.route}")
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            Scaffold(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                containerColor = MaterialTheme.colorScheme.surface,
                                topBar = topBarContent
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding).imePadding().fillMaxSize(),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    // My design decision (not in M3): cap single-pane reading width.
                                    Box(Modifier.widthIn(max = 840.dp).fillMaxSize()) {
                                        navigationContent()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}
