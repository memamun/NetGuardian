package com.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.database.BlocklistEntity
import com.example.ui.privacy.CategoryCard
import com.example.ui.privacy.CustomDomainItem
import com.example.ui.theme.NetGuardianTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrivacyScreenUiTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun categoryCard_displaysMetadata_andTogglesOnClick() {
        var toggledState: Boolean? = null

        compose.setContent {
            NetGuardianTheme {
                CategoryCard(
                    title = "Ad Networks",
                    description = "Blocks mobile ad networks, banner requests, and video ads",
                    domainCount = 150,
                    icon = Icons.Default.AdsClick,
                    enabled = true,
                    onToggle = { toggledState = it }
                )
            }
        }

        compose.onNodeWithText("Ad Networks").assertIsDisplayed()
        compose.onNodeWithText("Blocks mobile ad networks, banner requests, and video ads").assertIsDisplayed()
        compose.onNodeWithText("150 patterns").assertIsDisplayed()

        // Clicking the card should toggle it
        compose.onNodeWithText("Ad Networks").performClick()
        assertEquals(false, toggledState)
    }

    @Test
    fun customDomainItem_rendersDomain_andTriggersDeleteAndToggle() {
        var deleteTriggered = false
        var toggleTriggered = false

        val item = BlocklistEntity(
            id = 7L,
            domain = "custom.tracker.org",
            category = "CUSTOM",
            isEnabled = true
        )

        compose.setContent {
            NetGuardianTheme {
                CustomDomainItem(
                    item = item,
                    onToggle = { toggleTriggered = true },
                    onDelete = { deleteTriggered = true }
                )
            }
        }

        compose.onNodeWithText("custom.tracker.org").assertIsDisplayed()

        compose.onNodeWithContentDescription("Delete").performClick()
        assertTrue("Delete action should be invoked", deleteTriggered)

        compose.onNodeWithContentDescription("custom.tracker.org").performClick()
        assertTrue("Toggle action should be invoked", toggleTriggered)
    }
}
