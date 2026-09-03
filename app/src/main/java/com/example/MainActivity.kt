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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
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
import com.example.ui.MainViewModel
import com.example.ui.apps.AppsScreen
import com.example.ui.connections.ConnectionsScreen
import com.example.ui.home.DashboardScreen
import com.example.ui.onboarding.OnboardingWizardScreen
import com.example.ui.privacy.PrivacyScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.*

private data class StatusBadgeInfo(val bg: Color, val dot: Color, val text: String, val textColor: Color)

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Apps : Screen("apps", "Apps", Icons.Default.Apps)
    object Logs : Screen("logs", "Logs", Icons.Default.History)
    object Privacy : Screen("privacy", "Privacy", Icons.Default.Dns)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private lateinit var mainViewModel: MainViewModel
    private var onRouteRequested: ((String) -> Unit)? = null

    private val vpnLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            mainViewModel.startFirewall(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(com.example.firewall.FirewallNotificationManager.EXTRA_ROUTE)?.let { route ->
            onRouteRequested?.invoke(route)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            mainViewModel = viewModel()
            val firewallActive by mainViewModel.firewallActive.collectAsStateWithLifecycle()
            val snapshot by mainViewModel.firewallSnapshot.collectAsStateWithLifecycle()
            val userPrefs by mainViewModel.userPreferences.collectAsStateWithLifecycle()
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            androidx.compose.runtime.DisposableEffect(navController) {
                onRouteRequested = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                // Handle intent route on initial open
                intent?.getStringExtra(com.example.firewall.FirewallNotificationManager.EXTRA_ROUTE)?.let { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                onDispose {
                    onRouteRequested = null
                }
            }

            val items = listOf(
                Screen.Dashboard,
                Screen.Apps,
                Screen.Logs,
                Screen.Privacy,
                Screen.Settings
            )

            NetGuardianTheme {
                if (!userPrefs.onboardingCompleted) {
                    OnboardingWizardScreen(
                        viewModel = mainViewModel,
                        onRequestVpnPermission = { prepareIntent ->
                            vpnLauncher.launch(prepareIntent)
                        },
                        onComplete = {
                            // Onboarding marked completed in preferences
                        }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "NetGuardian",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = (-0.5).sp
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "PRIVACY FIREWALL",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Medium,
                                                letterSpacing = 1.4.sp
                                            ),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    val netGuardian = MaterialTheme.netGuardian
                                    val (badgeBg, badgeDot, badgeText, badgeTextColor) = when (snapshot.state) {
                                        com.example.firewall.FirewallState.RUNNING -> StatusBadgeInfo(
                                            netGuardian.successContainer, netGuardian.success, "PROTECTION ON", netGuardian.onSuccessContainer
                                        )
                                        com.example.firewall.FirewallState.STARTING -> StatusBadgeInfo(
                                            MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, "STARTING...", MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        com.example.firewall.FirewallState.PAUSED -> StatusBadgeInfo(
                                            netGuardian.warningContainer, netGuardian.warning, "PAUSED", netGuardian.onWarningContainer
                                        )
                                        com.example.firewall.FirewallState.ERROR -> StatusBadgeInfo(
                                            netGuardian.blockedContainer, netGuardian.blocked, "ATTENTION", netGuardian.onBlockedContainer
                                        )
                                        com.example.firewall.FirewallState.VPN_PERMISSION_REQUIRED -> StatusBadgeInfo(
                                            netGuardian.blockedContainer, netGuardian.blocked, "SETUP NEEDED", netGuardian.onBlockedContainer
                                        )
                                        com.example.firewall.FirewallState.STOPPED -> StatusBadgeInfo(
                                            netGuardian.blockedContainer, netGuardian.blocked, "PROTECTION OFF", netGuardian.onBlockedContainer
                                        )
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = badgeBg
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(badgeDot, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = badgeText,
                                                color = badgeTextColor,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.5.sp
                                                ),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        bottomBar = {
                            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 0.dp,
                                    modifier = Modifier.testTag("bottom_nav_bar")
                                ) {
                                    items.forEach { screen ->
                                        val selected = currentRoute == screen.route
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = {
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
                                                    contentDescription = screen.title
                                                )
                                            },
                                            label = {
                                                Text(
                                                    screen.title,
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
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Dashboard.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Dashboard.route) {
                                DashboardScreen(
                                    viewModel = mainViewModel,
                                    onNavigateToApps = {
                                        navController.navigate(Screen.Apps.route)
                                    },
                                    onNavigateToLogs = {
                                        navController.navigate(Screen.Logs.route)
                                    },
                                    onVpnPermissionNeeded = { prepareIntent ->
                                        vpnLauncher.launch(prepareIntent)
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate(Screen.Settings.route)
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
                }
            }
        }
    }
}
