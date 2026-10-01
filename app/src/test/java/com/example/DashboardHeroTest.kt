package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.firewall.FirewallSnapshot
import com.example.firewall.FirewallState
import com.example.firewall.NetworkType
import com.example.ui.home.HeroStatusCard
import com.example.ui.theme.NetGuardianTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DashboardHeroTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun heroCard_displaysRunningState_andTriggersToggle() {
        var toggleCalled = false
        val snapshot = FirewallSnapshot(state = FirewallState.RUNNING)

        compose.setContent {
            NetGuardianTheme {
                HeroStatusCard(
                    snapshot = snapshot,
                    networkType = NetworkType.WIFI,
                    appCount = 42,
                    onToggle = { toggleCalled = true }
                )
            }
        }

        compose.onNodeWithTag("hero_status_card").assertIsDisplayed()
        compose.onNodeWithText("Protection is on").assertIsDisplayed()
        compose.onNodeWithText("Stop firewall").assertIsDisplayed()

        compose.onNodeWithTag("firewall_toggle_button").performClick()
        assertTrue("Clicking Stop firewall should invoke onToggle", toggleCalled)
    }

    @Test
    fun heroCard_displaysStoppedState_withStartAction() {
        var toggleCalled = false
        val snapshot = FirewallSnapshot(state = FirewallState.STOPPED)

        compose.setContent {
            NetGuardianTheme {
                HeroStatusCard(
                    snapshot = snapshot,
                    networkType = NetworkType.WIFI,
                    appCount = 42,
                    onToggle = { toggleCalled = true }
                )
            }
        }

        compose.onNodeWithTag("hero_status_card").assertIsDisplayed()
        compose.onNodeWithText("Protection is off").assertIsDisplayed()
        compose.onNodeWithText("Start firewall").assertIsDisplayed()

        compose.onNodeWithTag("firewall_toggle_button").performClick()
        assertTrue("Clicking Start firewall should invoke onToggle", toggleCalled)
    }

    @Test
    fun heroCard_displaysPausedState_withResumeAction() {
        var toggleCalled = false
        val snapshot = FirewallSnapshot(state = FirewallState.PAUSED)

        compose.setContent {
            NetGuardianTheme {
                HeroStatusCard(
                    snapshot = snapshot,
                    networkType = NetworkType.WIFI,
                    appCount = 42,
                    onToggle = { toggleCalled = true }
                )
            }
        }

        compose.onNodeWithTag("hero_status_card").assertIsDisplayed()
        compose.onNodeWithText("Protection is paused").assertIsDisplayed()
        compose.onNodeWithText("Resume protection").assertIsDisplayed()

        compose.onNodeWithTag("firewall_toggle_button").performClick()
        assertTrue("Clicking Resume protection should invoke onToggle", toggleCalled)
    }
}
