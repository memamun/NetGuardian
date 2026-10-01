package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.components.AppSearchField
import com.example.ui.components.DnsAddressField
import com.example.ui.components.isIpv4Address
import com.example.ui.theme.NetGuardianTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SharedUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun clearSearchUpdatesQuery() {
        val query = mutableStateOf("browser")
        compose.setContent { NetGuardianTheme {
            AppSearchField(query.value, { query.value = it }, "Search apps")
        } }
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.runOnIdle { assertEquals("", query.value) }
    }

    @Test fun invalidResolverIsNotSavedAndShowsInlineError() {
        var saved: String? = null
        compose.setContent { NetGuardianTheme {
            DnsAddressField("192.168.1.1", { saved = it })
        } }
        compose.onNodeWithTag("custom_dns_ip_field").performTextReplacement("999.1.1.1")
        compose.onNodeWithText("Save resolver").performClick()
        compose.onNodeWithText("Enter four numbers from 0 to 255, separated by dots.").assertExists()
        compose.runOnIdle { assertNull(saved) }
        compose.onNodeWithTag("custom_dns_ip_field").performTextReplacement("1.1.1.1")
        compose.onNodeWithText("Save resolver").performClick()
        compose.runOnIdle { assertEquals("1.1.1.1", saved) }
    }

    @Test fun ipv4ValidationRejectsPartialAndMalformedInput() {
        listOf("", "1.1.1", "1.1.1.256", "-1.2.3.4", "a.b.c.d", "1..2.3").forEach {
            assertFalse(it, isIpv4Address(it))
        }
        assertTrue(isIpv4Address("192.168.1.1"))
    }
}
