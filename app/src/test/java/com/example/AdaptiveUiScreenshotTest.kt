package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.firewall.FirewallState
import com.example.firewall.NetworkType
import com.example.ui.components.*
import com.example.ui.home.StatTilesSection
import com.example.ui.theme.NetGuardianTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-mdpi")
class AdaptiveUiScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun compactDarkSearchAndResolver() {
        compose.setContent {
            NetGuardianTheme(darkTheme = true) {
                Surface {
                    Column(Modifier.fillMaxSize()) {
                        AppTopBar("Privacy", FirewallState.STOPPED)
                        AppSearchField("", {}, "Search rules", Modifier.fillMaxWidth().padding(16.dp))
                        DnsAddressField("192.168.1.1", {}, Modifier.padding(16.dp))
                    }
                }
            }
        }
        compose.onNodeWithTag("custom_dns_ip_field").assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath = "build/ui-review/compact-dark.png")
    }

    @Test fun doubledTextKeepsEveryStatisticReachable() {
        compose.setContent {
            NetGuardianTheme(darkTheme = false) {
                CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                    Surface {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                            StatTilesSection(3, 87, 12, NetworkType.WIFI)
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag("stat_network_type").performScrollTo().assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath = "build/ui-review/large-text.png")
    }

    @Test
    @Config(qualifiers = "w840dp-h800dp-mdpi")
    fun tabletStatistics() {
        compose.setContent {
            NetGuardianTheme(darkTheme = true) {
                Surface {
                    Column(Modifier.fillMaxWidth().padding(24.dp)) {
                        AppTopBar("NetGuardian", FirewallState.RUNNING)
                        StatTilesSection(3, 87, 12, NetworkType.WIFI)
                    }
                }
            }
        }
        compose.onNodeWithTag("stat_allowed_apps").assertIsDisplayed()
        compose.onNodeWithTag("stat_network_type").assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath = "build/ui-review/tablet-dark.png")
    }
}
