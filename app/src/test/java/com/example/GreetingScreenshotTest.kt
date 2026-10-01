package com.example

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.firewall.NetworkType
import com.example.ui.home.HeroStatusCard
import com.example.ui.theme.NetGuardianTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun dashboard_hero_screenshot() {
        composeTestRule.setContent {
            NetGuardianTheme(darkTheme = false) {
                Surface {
                    HeroStatusCard(
                        isActive = true,
                        networkType = NetworkType.WIFI,
                        onToggle = {}
                    )
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/hero_status_light.png")
    }

    @Test
    fun dashboard_hero_dark_screenshot() {
        composeTestRule.setContent {
            NetGuardianTheme(darkTheme = true) {
                Surface {
                    HeroStatusCard(
                        isActive = true,
                        networkType = NetworkType.WIFI,
                        onToggle = {}
                    )
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/hero_status_dark.png")
    }

    @Test
    fun dashboard_hero_stopped_screenshot() {
        composeTestRule.setContent {
            NetGuardianTheme(darkTheme = true) {
                Surface {
                    HeroStatusCard(
                        isActive = false,
                        networkType = NetworkType.WIFI,
                        onToggle = {}
                    )
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/hero_status_stopped.png")
    }

    @Test
    fun dashboard_stat_tiles_screenshot() {
        composeTestRule.setContent {
            NetGuardianTheme(darkTheme = true) {
                Surface {
                    com.example.ui.home.StatTilesSection(
                        blockedApps = 14,
                        allowedApps = 52,
                        todayBlocks = 247,
                        networkType = NetworkType.WIFI
                    )
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/stat_tiles_dark.png")
    }

    @Test
    fun app_item_allowed_screenshot() {
        val rule = com.example.database.AppRuleEntity(
            packageName = "com.facebook.orca",
            appName = "Messenger",
            uid = 10314,
            isBlocked = false,
            blockWifi = false,
            blockMobile = false
        )
        val item = com.example.data.AppItem(
            packageName = "com.facebook.orca",
            appName = "Messenger",
            uid = 10314,
            isSystemApp = false,
            hasInternetPermission = true,
            versionName = "420.0.0.1",
            rule = rule
        )
        composeTestRule.setContent {
            NetGuardianTheme(darkTheme = true) {
                Surface(modifier = androidx.compose.ui.Modifier.padding(16.dp)) {
                    com.example.ui.apps.AppItemCard(
                        app = item,
                        onToggleBlock = {},
                        onToggleWifi = {},
                        onToggleMobile = {},
                        onClick = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/app_item_allowed.png")
    }

    @Test
    fun app_item_blocked_screenshot() {
        val rule = com.example.database.AppRuleEntity(
            packageName = "com.facebook.orca",
            appName = "Messenger",
            uid = 10314,
            isBlocked = true,
            blockWifi = true,
            blockMobile = true
        )
        val item = com.example.data.AppItem(
            packageName = "com.facebook.orca",
            appName = "Messenger",
            uid = 10314,
            isSystemApp = false,
            hasInternetPermission = true,
            versionName = "420.0.0.1",
            rule = rule
        )
        composeTestRule.setContent {
            NetGuardianTheme(darkTheme = true) {
                Surface(modifier = androidx.compose.ui.Modifier.padding(16.dp)) {
                    com.example.ui.apps.AppItemCard(
                        app = item,
                        onToggleBlock = {},
                        onToggleWifi = {},
                        onToggleMobile = {},
                        onClick = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/app_item_blocked.png")
    }

    @Test
    fun app_item_restricted_screenshot() {
        val rule = com.example.database.AppRuleEntity(
            packageName = "com.spotify.music",
            appName = "Spotify",
            uid = 10420,
            isBlocked = false,
            blockWifi = false,
            blockMobile = true,
            blockBackground = true,
            blockScreenOff = true,
            blockTrackers = true
        )
        val item = com.example.data.AppItem(
            packageName = "com.spotify.music",
            appName = "Spotify",
            uid = 10420,
            isSystemApp = false,
            hasInternetPermission = true,
            versionName = "8.9.12",
            rule = rule
        )
        composeTestRule.setContent {
            NetGuardianTheme(darkTheme = true) {
                Surface(modifier = androidx.compose.ui.Modifier.padding(16.dp)) {
                    com.example.ui.apps.AppItemCard(
                        app = item,
                        onToggleBlock = {},
                        onToggleWifi = {},
                        onToggleMobile = {},
                        onClick = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/app_item_restricted.png")
    }
}
