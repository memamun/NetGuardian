package com.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.settings.SettingToggleRow
import com.example.ui.theme.NetGuardianTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun labelledRowExposesStateAndTogglesFromItsLabel() {
        val checked = mutableStateOf(false)
        compose.setContent {
            NetGuardianTheme {
                SettingToggleRow(
                    title = "Protection", subtitle = "Protect connections",
                    icon = Icons.Default.Security, checked = checked.value,
                    onCheckedChange = { checked.value = it }
                )
            }
        }
        compose.onNodeWithText("Protection").assertIsOff().performClick().assertIsOn()
    }
}
