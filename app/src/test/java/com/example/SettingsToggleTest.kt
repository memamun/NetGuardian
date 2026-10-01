package com.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.settings.SettingToggleRow
import com.example.ui.theme.NetGuardianTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsToggleTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun settingToggleRow_displaysTitleAndSubtitle_andTogglesState() {
        var checkedState = false

        compose.setContent {
            NetGuardianTheme {
                SettingToggleRow(
                    title = "Start on Boot",
                    subtitle = "Automatically activate firewall when Android boots up",
                    icon = Icons.Default.PowerSettingsNew,
                    checked = checkedState,
                    onCheckedChange = { checkedState = it }
                )
            }
        }

        compose.onNodeWithText("Start on Boot").assertIsDisplayed()
        compose.onNodeWithText("Automatically activate firewall when Android boots up").assertIsDisplayed()

        // Click row to toggle
        compose.onNodeWithText("Start on Boot").performClick()
        assertEquals(true, checkedState)
    }
}
