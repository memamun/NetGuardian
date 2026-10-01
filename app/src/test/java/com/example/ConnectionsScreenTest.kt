package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.database.ConnectionLogEntity
import com.example.ui.connections.ConnectionDetailSheetContent
import com.example.ui.connections.ConnectionLogItemCard
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
class ConnectionsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun connectionLogItemCard_displaysLogInfo_andRespondsToClick() {
        var clicked = false
        val log = ConnectionLogEntity(
            id = 42L,
            packageName = "com.sample.browser",
            appName = "Sample Browser",
            destinationHost = "tracking.analytics.com",
            port = 443,
            protocol = "TCP",
            isBlocked = true,
            blockReason = "Tracker/Ad Filter",
            timestamp = 1700000000000L,
            bytesTransferred = 1024L
        )

        compose.setContent {
            NetGuardianTheme {
                ConnectionLogItemCard(
                    log = log,
                    onClick = { clicked = true }
                )
            }
        }

        compose.onNodeWithTag("connection_log_card_42").assertIsDisplayed()
        compose.onNodeWithText("Sample Browser").assertIsDisplayed()
        compose.onNodeWithText("tracking.analytics.com").assertIsDisplayed()
        compose.onNodeWithText("TCP : 443").assertIsDisplayed()
        compose.onNodeWithText("BLOCKED").assertIsDisplayed()
        compose.onNodeWithText("Tracker/Ad Filter").assertIsDisplayed()

        compose.onNodeWithTag("connection_log_card_42").performClick()
        assertTrue("Log card click should trigger onClick callback", clicked)
    }

    @Test
    fun connectionDetailSheetContent_rendersAllowedConnection_withBlockDomainAction() {
        var blockedDomain: String? = null
        var dismissed = false

        val log = ConnectionLogEntity(
            id = 99L,
            packageName = "com.social.network",
            appName = "Social Network",
            destinationHost = "adserver.social.com",
            port = 443,
            protocol = "TCP",
            isBlocked = false,
            blockReason = "Allowed",
            timestamp = 1700000000000L,
            bytesTransferred = 4096L
        )

        compose.setContent {
            NetGuardianTheme {
                ConnectionDetailSheetContent(
                    log = log,
                    onDismiss = { dismissed = true },
                    onBlockDomain = { domain -> blockedDomain = domain }
                )
            }
        }

        compose.onNodeWithTag("connection_detail_sheet").assertIsDisplayed()
        compose.onNodeWithText("Social Network").assertIsDisplayed()
        compose.onNodeWithText("com.social.network").assertIsDisplayed()
        compose.onNodeWithText("adserver.social.com").assertIsDisplayed()
        compose.onNodeWithText("TCP : 443").assertIsDisplayed()
        // Test Block Domain action button
        compose.onNodeWithTag("block_domain_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        assertEquals("adserver.social.com", blockedDomain)
        assertTrue("Dismiss should be called after blocking domain", dismissed)
    }
}
