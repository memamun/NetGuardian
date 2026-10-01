package com.example.ui.onboarding

import androidx.compose.ui.res.stringResource
import com.example.R

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
fun OnboardingWizardScreen(
    viewModel: MainViewModel,
    onRequestVpnPermission: (Intent) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val userPrefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val diagnostics by viewModel.systemDiagnostics.collectAsStateWithLifecycle()

    var currentStep by rememberSaveable { mutableIntStateOf(1) }
    val totalSteps = 10
    BackHandler(enabled = currentStep > 1) { currentStep -= 1 }

    // Local check states re-evaluated when returning from system dialogs
    var isVpnGranted by remember { mutableStateOf(DiagnosticsHelper.isVpnAuthorized(context)) }
    var isNotificationGranted by remember { mutableStateOf(DiagnosticsHelper.isNotificationGranted(context)) }
    var isBatteryOptimized by remember { mutableStateOf(!DiagnosticsHelper.isBatteryOptimizationIgnored(context)) }
    var vpnRequestedAttempted by rememberSaveable { mutableStateOf(false) }

    // Re-check permissions whenever app returns to foreground (resumes)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isVpnGranted = DiagnosticsHelper.isVpnAuthorized(context)
                isNotificationGranted = DiagnosticsHelper.isNotificationGranted(context)
                isBatteryOptimized = !DiagnosticsHelper.isBatteryOptimizationIgnored(context)
                viewModel.runDiagnostics(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(currentStep) {
        if (currentStep == 9) {
            viewModel.runDiagnostics(context)
        }
    }

    // Permission launcher for Android 13+ Notifications
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isNotificationGranted = granted || DiagnosticsHelper.isNotificationGranted(context)
        viewModel.runDiagnostics(context)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("onboarding_wizard"),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        IconButton(
                            onClick = { currentStep -= 1 },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.ui_previous_step),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.ui_netguardian_setup),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Step $currentStep of $totalSteps",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (currentStep < totalSteps) {
                        TextButton(
                            onClick = {
                                if (currentStep == 5 && !isVpnGranted) {
                                    // Skip to next with understanding
                                    currentStep += 1
                                } else {
                                    currentStep += 1
                                }
                            }
                        ) {
                            Text(stringResource(R.string.ui_skip), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { currentStep.toFloat() / totalSteps.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                modifier = Modifier.align(Alignment.TopCenter).widthIn(max = 840.dp).fillMaxWidth(),
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "onboarding_step_transition"
            ) { step ->
                when (step) {
                    1 -> Step1Welcome(
                        onGetStarted = { currentStep = 2 },
                        onLearnHowItWorks = { currentStep = 2 }
                    )
                    2 -> Step2ExplainApp(
                        onContinue = { currentStep = 3 }
                    )
                    3 -> Step3PrivacyPromise(
                        onContinue = { currentStep = 4 }
                    )
                    4 -> Step4SetupChecklist(
                        isVpnGranted = isVpnGranted,
                        isNotificationGranted = isNotificationGranted,
                        isBatteryOptimized = !isBatteryOptimized,
                        onBeginSetup = { currentStep = 5 }
                    )
                    5 -> Step5VpnPermission(
                        isGranted = isVpnGranted,
                        hasAttempted = vpnRequestedAttempted,
                        onRequestPermission = {
                            vpnRequestedAttempted = true
                            val prepareIntent = DiagnosticsHelper.isVpnAuthorized(context)
                            if (prepareIntent) {
                                isVpnGranted = true
                            } else {
                                val intent = android.net.VpnService.prepare(context)
                                if (intent != null) {
                                    onRequestVpnPermission(intent)
                                } else {
                                    isVpnGranted = true
                                }
                            }
                        },
                        onNext = { currentStep = 6 },
                        onContinueWithoutVpn = { currentStep = 6 }
                    )
                    6 -> Step6NotificationPermission(
                        isGranted = isNotificationGranted,
                        onRequestPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                isNotificationGranted = true
                            }
                        },
                        onOpenSettings = {
                            DiagnosticsHelper.openAppNotificationSettings(context)
                        },
                        onNext = { currentStep = 7 }
                    )
                    7 -> Step7BatteryOperation(
                        isIgnoringOptimization = !isBatteryOptimized,
                        onOpenBatterySettings = {
                            DiagnosticsHelper.openBatterySettings(context)
                        },
                        onNext = { currentStep = 8 }
                    )
                    8 -> Step8OptionalPrivacyFeatures(
                        blockTrackers = userPrefs.blockTrackersGlobal,
                        blockAds = userPrefs.blockAdsGlobal,
                        blockMalware = userPrefs.blockMalwareGlobal,
                        dnsFiltering = userPrefs.dnsFilteringEnabled,
                        onToggleTrackers = { viewModel.toggleCategory("TRACKER", it) },
                        onToggleAds = { viewModel.toggleCategory("AD", it) },
                        onToggleMalware = { viewModel.toggleCategory("MALWARE", it) },
                        onToggleDns = { viewModel.setDnsFiltering(it) },
                        onNext = { currentStep = 9 }
                    )
                    9 -> Step9SystemAccessCheck(
                        diagnostics = diagnostics,
                        onRefresh = { viewModel.runDiagnostics(context) },
                        onNext = { currentStep = 10 }
                    )
                    10 -> Step10SetupComplete(
                        isVpnReady = isVpnGranted,
                        onOpenFirewall = { policy ->
                            viewModel.completeOnboarding(policy)
                            onComplete()
                        }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 1 — WELCOME
// -------------------------------------------------------------
@Composable
private fun Step1Welcome(
    onGetStarted: () -> Unit,
    onLearnHowItWorks: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.section),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Security / Firewall Illustration
        Box(
            modifier = Modifier
                .size(170.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface)
                    )
                )
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = stringResource(R.string.ui_shield_icon),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(60.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.ui_take_control_of_your_internet),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.ui_control_which_apps_can_access_the_internet),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("onboarding_get_started_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(R.string.ui_get_started), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }

            TextButton(
                onClick = onLearnHowItWorks,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.ui_learn_how_it_works), fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 2 — EXPLAIN THE APP
// -------------------------------------------------------------
@Composable
private fun Step2ExplainApp(
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.section),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.content)) {
            Text(
                text = stringResource(R.string.ui_how_netguardian_works),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.ui_three_core_capabilities_protect_your_device_from),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CapabilityCard(
                icon = Icons.Default.Apps,
                title = "Per-app Firewall",
                description = "Choose which apps can connect to the internet."
            )

            CapabilityCard(
                icon = Icons.Default.Sync,
                title = "Background Protection",
                description = "Restrict network activity when apps are running in the background."
            )

            CapabilityCard(
                icon = Icons.Default.Dns,
                title = "Privacy Protection",
                description = "Optionally block unwanted trackers and domains."
            )

            // Transparency Notice
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.ui_this_app_uses_android_s_vpn_functionality),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("onboarding_step2_continue"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(stringResource(R.string.ui_next_privacy_promise), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CapabilityCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.content),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 3 — PRIVACY PROMISE
// -------------------------------------------------------------
@Composable
private fun Step3PrivacyPromise(
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.section),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = stringResource(R.string.ui_your_traffic_stays_on_your_device),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.ui_netguardian_operates_under_a_strict_offline_first),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(AppSpacing.content),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
                ) {
                    PrivacyCheckItem("No account required")
                    PrivacyCheckItem("No telemetry or analytics")
                    PrivacyCheckItem("No advertising SDK")
                    PrivacyCheckItem("No cloud server required for firewall operation")
                    PrivacyCheckItem("Network filtering happens locally")
                    PrivacyCheckItem("Firewall rules are stored locally")
                }
            }

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.ui_android_requires_a_vpn_permission_because_the),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("onboarding_step3_continue"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(stringResource(R.string.ui_continue), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PrivacyCheckItem(text: String) {
    val netGuardian = MaterialTheme.netGuardian
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
    ) {
        Surface(
            shape = CircleShape,
            color = netGuardian.allowedContainer,
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = netGuardian.allowed,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// -------------------------------------------------------------
// SCREEN 4 — SETUP CHECKLIST
// -------------------------------------------------------------
@Composable
private fun Step4SetupChecklist(
    isVpnGranted: Boolean,
    isNotificationGranted: Boolean,
    isBatteryOptimized: Boolean,
    onBeginSetup: () -> Unit
) {
    val netGuardian = MaterialTheme.netGuardian
    val notifRequired = DiagnosticsHelper.isNotificationRequired()
    val completedRequired = (if (isVpnGranted) 1 else 0) + (if (!notifRequired || isNotificationGranted) 1 else 0)
    val totalRequired = if (notifRequired) 2 else 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.section),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.content)) {
            Text(
                text = stringResource(R.string.ui_setup_checklist),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "$completedRequired of $totalRequired required steps complete",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (completedRequired == totalRequired) netGuardian.allowed else MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.ui_before_activating_the_firewall_we_will_guide),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(AppSpacing.content),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ChecklistRow(
                        title = "Firewall VPN",
                        tag = "REQUIRED",
                        statusText = if (isVpnGranted) "Ready" else "Not configured",
                        isReady = isVpnGranted,
                        isRequired = true
                    )

                    ChecklistRow(
                        title = "Notifications",
                        tag = if (notifRequired) "REQUIRED" else "NOT REQUIRED",
                        statusText = if (!notifRequired) "Ready (automatic on this Android version)" else if (isNotificationGranted) "Ready" else "Not configured",
                        isReady = !notifRequired || isNotificationGranted,
                        isRequired = notifRequired
                    )

                    ChecklistRow(
                        title = "Background Operation",
                        tag = "RECOMMENDED",
                        statusText = if (isBatteryOptimized) "Ready (unrestricted)" else "Not configured",
                        isReady = isBatteryOptimized,
                        isRequired = false
                    )

                    ChecklistRow(
                        title = "Privacy Filters",
                        tag = "OPTIONAL",
                        statusText = "Optional",
                        isReady = true,
                        isRequired = false
                    )
                }
            }
        }

        Button(
            onClick = onBeginSetup,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("onboarding_step4_start_setup"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(stringResource(R.string.ui_begin_setup), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ChecklistRow(
    title: String,
    tag: String,
    statusText: String,
    isReady: Boolean,
    isRequired: Boolean
) {
    val netGuardian = MaterialTheme.netGuardian
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isRequired) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isRequired) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isReady) netGuardian.allowed else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            shape = CircleShape,
            color = if (isReady) netGuardian.allowedContainer else MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.size(26.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isReady) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = netGuardian.allowed, modifier = Modifier.size(16.dp))
                } else {
                    Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 5 — VPN PERMISSION
// -------------------------------------------------------------
@Composable
private fun Step5VpnPermission(
    isGranted: Boolean,
    hasAttempted: Boolean,
    onRequestPermission: () -> Unit,
    onNext: () -> Unit,
    onContinueWithoutVpn: () -> Unit
) {
    val netGuardian = MaterialTheme.netGuardian
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.section),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.content)) {
            Text(
                text = stringResource(R.string.ui_firewall_access),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.ui_android_requires_your_approval_before_netguardian_can),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(AppSpacing.content),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            }
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.ui_local_loopback_vpn),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.ui_no_external_remote_server_required),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.ui_the_vpn_is_used_to_filter_traffic),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            // Real State Indicator
            if (isGranted) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = netGuardian.allowedContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, netGuardian.allowed.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = netGuardian.allowed, modifier = Modifier.size(24.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.ui_vpn_access_enabled),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = netGuardian.allowed
                            )
                            Text(
                                text = stringResource(R.string.ui_android_has_authorized_local_traffic_inspection),
                                style = MaterialTheme.typography.bodySmall,
                                color = netGuardian.onAllowedContainer
                            )
                        }
                    }
                }
            } else if (hasAttempted) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = netGuardian.blockedContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, netGuardian.blocked.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = netGuardian.blocked, modifier = Modifier.size(24.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.ui_vpn_access_wasn_t_granted),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = netGuardian.blocked
                            )
                            Text(
                                text = stringResource(R.string.ui_the_firewall_cannot_filter_connections_without_this),
                                style = MaterialTheme.typography.bodySmall,
                                color = netGuardian.onBlockedContainer
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isGranted) {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("onboarding_vpn_next_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.ui_next_step), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            } else {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("onboarding_allow_vpn_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (hasAttempted) "Try Again" else "Allow VPN Access", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                if (hasAttempted) {
                    OutlinedButton(
                        onClick = onContinueWithoutVpn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.ui_continue_without_firewall), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 6 — NOTIFICATION PERMISSION
// -------------------------------------------------------------
@Composable
private fun Step6NotificationPermission(
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onNext: () -> Unit
) {
    val netGuardian = MaterialTheme.netGuardian
    val isTiramisuPlus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.section),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.content)) {
            Text(
                text = stringResource(R.string.ui_persistent_firewall_status),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.ui_android_requires_notification_permission_so_the_firewall),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(AppSpacing.content),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            }
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.ui_foreground_service_alert),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isTiramisuPlus) "Android 13+ runtime permission" else "Android ${Build.VERSION.RELEASE} (Auto-permitted)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.ui_the_notification_displays_real_time_connection_counters),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            if (isGranted || !isTiramisuPlus) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = netGuardian.allowedContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, netGuardian.allowed.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = netGuardian.allowed, modifier = Modifier.size(24.dp))
                        Text(
                            text = if (isTiramisuPlus) "Notifications enabled" else "Notification permission granted automatically",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = netGuardian.allowed
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isGranted || !isTiramisuPlus) {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("onboarding_notif_next_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.ui_continue), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            } else {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("onboarding_enable_notif_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.ui_enable_notifications), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.ui_app_settings), fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = onNext,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Text(stringResource(R.string.ui_skip_for_now), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 7 — BATTERY / BACKGROUND OPERATION
// -------------------------------------------------------------
@Composable
private fun Step7BatteryOperation(
    isIgnoringOptimization: Boolean,
    onOpenBatterySettings: () -> Unit,
    onNext: () -> Unit
) {
    val netGuardian = MaterialTheme.netGuardian
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.section),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.content)) {
            Text(
                text = stringResource(R.string.ui_keep_protection_running),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.ui_some_android_manufacturers_aggressively_stop_background_applications),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(AppSpacing.content),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = netGuardian.allowedContainer
                        ) {
                            Text(
                                text = stringResource(R.string.ui_recommended),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = netGuardian.allowed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.ui_allow_unrestricted_battery_usage),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = stringResource(R.string.ui_background_whitelist_prevents_oem_battery_optimizers_from),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            if (isIgnoringOptimization) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = netGuardian.allowedContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, netGuardian.allowed.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = netGuardian.allowed, modifier = Modifier.size(22.dp))
                        Text(
                            text = stringResource(R.string.ui_unrestricted_battery_usage_is_active),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = netGuardian.allowed
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onOpenBatterySettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("onboarding_battery_settings_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.BatterySaver, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.ui_open_battery_settings), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }

            TextButton(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(if (isIgnoringOptimization) "Continue" else "Not Now", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 8 — OPTIONAL PRIVACY FEATURES
// -------------------------------------------------------------
@Composable
private fun Step8OptionalPrivacyFeatures(
    blockTrackers: Boolean,
    blockAds: Boolean,
    blockMalware: Boolean,
    dnsFiltering: Boolean,
    onToggleTrackers: (Boolean) -> Unit,
    onToggleAds: (Boolean) -> Unit,
    onToggleMalware: (Boolean) -> Unit,
    onToggleDns: (Boolean) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.section),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.content)) {
            Text(
                text = stringResource(R.string.ui_strengthen_your_privacy),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.ui_these_features_are_optional_the_firewall_itself),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(AppSpacing.content),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PrivacyFeatureToggle(
                        title = "Tracker blocking",
                        subtitle = "Block behavioral analytics and fingerprinting domains",
                        checked = blockTrackers,
                        onCheckedChange = onToggleTrackers
                    )

                    PrivacyFeatureToggle(
                        title = "Ad-domain blocking",
                        subtitle = "Sinkhole known ad-serving hostnames",
                        checked = blockAds,
                        onCheckedChange = onToggleAds
                    )

                    PrivacyFeatureToggle(
                        title = "Malware-domain blocking",
                        subtitle = "Filter phishing, botnet, and dangerous hostnames",
                        checked = blockMalware,
                        onCheckedChange = onToggleMalware
                    )

                    PrivacyFeatureToggle(
                        title = "Custom DNS / blocklists",
                        subtitle = "Enable local DNS filtering engine and custom rules",
                        checked = dnsFiltering,
                        onCheckedChange = onToggleDns
                    )
                }
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("onboarding_step8_continue"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(stringResource(R.string.ui_continue_to_diagnostics), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PrivacyFeatureToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        )
    }
}

// -------------------------------------------------------------
// SCREEN 9 — SYSTEM ACCESS CHECK
// -------------------------------------------------------------
@Composable
private fun Step9SystemAccessCheck(
    diagnostics: List<com.example.data.DiagnosticCheck>,
    onRefresh: () -> Unit,
    onNext: () -> Unit
) {
    val netGuardian = MaterialTheme.netGuardian
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.section),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.ui_checking_your_setup),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.ui_real_system_diagnostics_test),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.ui_refresh_checks), tint = MaterialTheme.colorScheme.primary)
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(diagnostics.size) { index ->
                    val check = diagnostics[index]
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppSpacing.medium),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = check.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = check.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (check.status) {
                                    DiagnosticStatus.READY -> netGuardian.allowedContainer
                                    DiagnosticStatus.NEEDS_ATTENTION -> netGuardian.warning.copy(alpha = 0.15f)
                                    DiagnosticStatus.NOT_REQUIRED -> MaterialTheme.colorScheme.surfaceContainerLow
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.tiny)
                                ) {
                                    Text(
                                        text = when (check.status) {
                                            DiagnosticStatus.READY -> "✓ Ready"
                                            DiagnosticStatus.NEEDS_ATTENTION -> "! Needs attention"
                                            DiagnosticStatus.NOT_REQUIRED -> "— Not required"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (check.status) {
                                            DiagnosticStatus.READY -> netGuardian.allowed
                                            DiagnosticStatus.NEEDS_ATTENTION -> netGuardian.warning
                                            DiagnosticStatus.NOT_REQUIRED -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("onboarding_step9_next"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(stringResource(R.string.ui_proceed_to_final_step), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

// -------------------------------------------------------------
// SCREEN 10 — SETUP COMPLETE
// -------------------------------------------------------------
@Composable
private fun Step10SetupComplete(
    isVpnReady: Boolean,
    onOpenFirewall: (selectedPolicy: String) -> Unit
) {
    val netGuardian = MaterialTheme.netGuardian
    var selectedDefaultPolicy by rememberSaveable { mutableStateOf("ALLOW_ALL") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.section),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.content)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                Surface(
                    shape = CircleShape,
                    color = netGuardian.allowedContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = netGuardian.allowed, modifier = Modifier.size(24.dp))
                    }
                }
                Column {
                    Text(
                        text = stringResource(R.string.ui_you_re_protected),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.ui_initial_setup_complete),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Overview Summary
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(AppSpacing.content),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryCheckRow("Firewall", if (isVpnReady) "✓ Ready" else "! Needs VPN")
                    SummaryCheckRow("App controls", "✓ Ready")
                    SummaryCheckRow("Connection logging", "✓ Ready")
                    SummaryCheckRow("Privacy filters", "○ Optional")
                }
            }

            // Safe Default Policy Choice
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(AppSpacing.content),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
                ) {
                    Text(
                        text = stringResource(R.string.ui_default_firewall_policy),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.ui_choose_how_apps_connect_until_you_customize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    PolicyRadioOption(
                        label = "Allow all apps (Recommended)",
                        description = "All apps can connect normally until you choose to block them.",
                        selected = selectedDefaultPolicy == "ALLOW_ALL",
                        onSelect = { selectedDefaultPolicy = "ALLOW_ALL" }
                    )

                    PolicyRadioOption(
                        label = "Block newly installed apps",
                        description = "Existing apps connect normally; new apps are blocked by default.",
                        selected = selectedDefaultPolicy == "BLOCK_NEW",
                        onSelect = { selectedDefaultPolicy = "BLOCK_NEW" }
                    )
                }
            }

            Text(
                text = stringResource(R.string.ui_note_the_firewall_will_remain_off_until),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }

        Button(
            onClick = { onOpenFirewall(selectedDefaultPolicy) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("onboarding_open_firewall_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(stringResource(R.string.ui_open_firewall), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SummaryCheckRow(title: String, status: String) {
    val netGuardian = MaterialTheme.netGuardian
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(
            status,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (status.startsWith("✓")) netGuardian.allowed else MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PolicyRadioOption(
    label: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)
        }
    }
}
