package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.AppItem
import com.example.database.AppRuleEntity
import com.example.ui.apps.AppItemCard
import com.example.ui.theme.NetGuardianTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppItemCardTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun appItemCard_rendersAllowedApp_andTriggersToggles() {
        var blockToggled = false
        var wifiToggled = false
        var mobileToggled = false

        val appItem = AppItem(
            packageName = "com.test.app",
            appName = "Test App",
            uid = 10001,
            isSystemApp = false,
            hasInternetPermission = true,
            versionName = "1.0",
            rule = AppRuleEntity(
                packageName = "com.test.app",
                appName = "Test App",
                isBlocked = false
            )
        )

        compose.setContent {
            NetGuardianTheme {
                AppItemCard(
                    app = appItem,
                    onToggleBlock = { blockToggled = true },
                    onToggleWifi = { wifiToggled = true },
                    onToggleMobile = { mobileToggled = true },
                    onClick = {}
                )
            }
        }

        compose.onNodeWithTag("app_card_com.test.app").assertIsDisplayed()
        compose.onNodeWithText("Test App").assertIsDisplayed()
        compose.onNodeWithText("com.test.app").assertIsDisplayed()
        compose.onNodeWithText("ALLOWED").assertIsDisplayed()

        compose.onNodeWithContentDescription("Block Wi-Fi").performClick()
        assertTrue("Wi-Fi toggle should be called", wifiToggled)

        compose.onNodeWithContentDescription("Block Mobile Data").performClick()
        assertTrue("Mobile toggle should be called", mobileToggled)

        compose.onNodeWithContentDescription("Block App").performClick()
        assertTrue("Block toggle should be called", blockToggled)
    }

    @Test
    fun appItemCard_rendersBlockedApp_withUnblockLabel() {
        var unblockToggled = false

        val appItem = AppItem(
            packageName = "com.blocked.app",
            appName = "Blocked App",
            uid = 10002,
            isSystemApp = false,
            hasInternetPermission = true,
            versionName = "1.0",
            rule = AppRuleEntity(
                packageName = "com.blocked.app",
                appName = "Blocked App",
                isBlocked = true
            )
        )

        compose.setContent {
            NetGuardianTheme {
                AppItemCard(
                    app = appItem,
                    onToggleBlock = { unblockToggled = true },
                    onToggleWifi = {},
                    onToggleMobile = {},
                    onClick = {}
                )
            }
        }

        compose.onNodeWithText("BLOCKED").assertIsDisplayed()
        compose.onNodeWithContentDescription("Unblock App").performClick()
        assertTrue("Unblock action should be called", unblockToggled)
    }
}
