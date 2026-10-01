package com.example.firewall

import com.example.data.QuickMode
import com.example.database.AppRuleEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [RuleEngine] — rule evaluation, quick mode overrides,
 * network type restrictions, and DNS blocklist filtering.
 */
class RuleEngineTest {

    private lateinit var engine: RuleEngine

    private fun baseRule(
        packageName: String = "com.example.app",
        isBlocked: Boolean = false,
        blockWifi: Boolean = false,
        blockMobile: Boolean = false,
        blockScreenOff: Boolean = false,
        blockBackground: Boolean = false,
        blockDeviceIdle: Boolean = false,
        blockTrackers: Boolean = true,
        isSystemApp: Boolean = false
    ) = AppRuleEntity(
        packageName = packageName,
        appName = "Test App",
        isBlocked = isBlocked,
        blockWifi = blockWifi,
        blockMobile = blockMobile,
        blockScreenOff = blockScreenOff,
        blockBackground = blockBackground,
        blockDeviceIdle = blockDeviceIdle,
        blockTrackers = blockTrackers,
        isSystemApp = isSystemApp
    )

    @Before
    fun setUp() {
        engine = RuleEngine()
    }

    // --- Basic allow/block ---

    @Test
    fun `allowed app passes`() {
        val result = engine.evaluate(
            rule = baseRule(isBlocked = false),
            destinationHost = "google.com",
            port = 443,
            protocol = "TCP",
            currentNetwork = NetworkType.WIFI,
            isScreenOn = true,
            isBackground = false,
            quickMode = QuickMode.NORMAL,
            blockedDomains = emptySet(),
            dnsFilteringEnabled = true
        )
        assertTrue(result.isAllowed)
    }

    @Test
    fun `blocked app is denied`() {
        val result = engine.evaluate(
            rule = baseRule(isBlocked = true),
            destinationHost = "google.com",
            port = 443,
            protocol = "TCP",
            currentNetwork = NetworkType.WIFI,
            isScreenOn = true,
            isBackground = false,
            quickMode = QuickMode.NORMAL,
            blockedDomains = emptySet(),
            dnsFilteringEnabled = true
        )
        assertFalse(result.isAllowed)
    }

    @Test
    fun `null rule defaults to allow`() {
        val result = engine.evaluate(
            rule = null,
            destinationHost = "google.com",
            port = 443,
            protocol = "TCP",
            currentNetwork = NetworkType.WIFI,
            isScreenOn = true,
            isBackground = false,
            quickMode = QuickMode.NORMAL,
            blockedDomains = emptySet(),
            dnsFilteringEnabled = true
        )
        assertTrue(result.isAllowed)
    }

    // --- Network restrictions ---

    @Test
    fun `wifi blocked app denied on wifi`() {
        val result = engine.evaluate(
            rule = baseRule(blockWifi = true),
            destinationHost = null,
            port = 0,
            protocol = "ALL",
            currentNetwork = NetworkType.WIFI,
            isScreenOn = true,
            isBackground = false,
            quickMode = QuickMode.NORMAL,
            blockedDomains = emptySet(),
            dnsFilteringEnabled = false
        )
        assertFalse(result.isAllowed)
    }

    @Test
    fun `wifi blocked app allowed on mobile`() {
        val result = engine.evaluate(
            rule = baseRule(blockWifi = true),
            destinationHost = null,
            port = 0,
            protocol = "ALL",
            currentNetwork = NetworkType.MOBILE,
            isScreenOn = true,
            isBackground = false,
            quickMode = QuickMode.NORMAL,
            blockedDomains = emptySet(),
            dnsFilteringEnabled = false
        )
        assertTrue(result.isAllowed)
    }

    @Test
    fun `mobile blocked app denied on mobile`() {
        val result = engine.evaluate(
            rule = baseRule(blockMobile = true),
            destinationHost = null,
            port = 0,
            protocol = "ALL",
            currentNetwork = NetworkType.MOBILE,
            isScreenOn = true,
            isBackground = false,
            quickMode = QuickMode.NORMAL,
            blockedDomains = emptySet(),
            dnsFilteringEnabled = false
        )
        assertFalse(result.isAllowed)
    }

    // --- Quick modes ---

    @Test
    fun `block non-system quick mode blocks user apps`() {
        val result = engine.evaluate(
            rule = baseRule(isSystemApp = false),
            destinationHost = null,
            port = 0,
            protocol = "ALL",
            currentNetwork = NetworkType.WIFI,
            isScreenOn = true,
            isBackground = false,
            quickMode = QuickMode.BLOCK_NON_SYSTEM,
            blockedDomains = emptySet(),
            dnsFilteringEnabled = false
        )
        assertFalse(result.isAllowed)
    }

    @Test
    fun `block non-system quick mode allows system apps`() {
        val result = engine.evaluate(
            rule = baseRule(isSystemApp = true),
            destinationHost = null,
            port = 0,
            protocol = "ALL",
            currentNetwork = NetworkType.WIFI,
            isScreenOn = true,
            isBackground = false,
            quickMode = QuickMode.BLOCK_NON_SYSTEM,
            blockedDomains = emptySet(),
            dnsFilteringEnabled = false
        )
        assertTrue(result.isAllowed)
    }

    @Test
    fun `wifi only mode blocks on mobile`() {
        val result = engine.evaluate(
            rule = baseRule(),
            destinationHost = null,
            port = 0,
            protocol = "ALL",
            currentNetwork = NetworkType.MOBILE,
            isScreenOn = true,
            isBackground = false,
            quickMode = QuickMode.WIFI_ONLY,
            blockedDomains = emptySet(),
            dnsFilteringEnabled = false
        )
        assertFalse(result.isAllowed)
    }

    @Test
    fun `mobile only mode blocks on wifi`() {
        val result = engine.evaluate(
            rule = baseRule(),
            destinationHost = null,
            port = 0,
            protocol = "ALL",
            currentNetwork = NetworkType.WIFI,
            isScreenOn = true,
            isBackground = false,
            quickMode = QuickMode.MOBILE_ONLY,
            blockedDomains = emptySet(),
            dnsFilteringEnabled = false
        )
        assertFalse(result.isAllowed)
    }

    // --- Screen off and device idle ---

    @Test
    fun `screen off policy blocks when screen off`() {
        val result = engine.evaluate(
            rule = baseRule(blockScreenOff = true),
            destinationHost = null,
            port = 0,
            protocol = "ALL",
            currentNetwork = NetworkType.WIFI,
            isScreenOn = false,
            isBackground = false,
            quickMode = QuickMode.NORMAL,
            blockedDomains = emptySet(),
            dnsFilteringEnabled = false
        )
        assertFalse(result.isAllowed)
    }

    @Test
    fun `device idle policy blocks when idle`() {
        val result = engine.evaluate(
            rule = baseRule(blockDeviceIdle = true),
            destinationHost = null,
            port = 0,
            protocol = "ALL",
            currentNetwork = NetworkType.WIFI,
            isScreenOn = true,
            isBackground = false,
            isDeviceIdle = true,
            quickMode = QuickMode.NORMAL,
            blockedDomains = emptySet(),
            dnsFilteringEnabled = false
        )
        assertFalse(result.isAllowed)
    }

    // --- DNS blocklist filtering ---

    @Test
    fun `dns filtering blocks tracker domain`() {
        val result = engine.evaluate(
            rule = baseRule(blockTrackers = true),
            destinationHost = "tracker.example.com",
            port = 53,
            protocol = "DNS",
            currentNetwork = NetworkType.WIFI,
            isScreenOn = true,
            isBackground = false,
            quickMode = QuickMode.NORMAL,
            blockedDomains = setOf("tracker.example.com"),
            dnsFilteringEnabled = true
        )
        assertFalse(result.isAllowed)
        assertTrue(result.reason.contains("Blocklist"))
    }

    @Test
    fun `dns filtering blocks subdomain of tracker`() {
        val result = engine.evaluate(
            rule = baseRule(blockTrackers = true),
            destinationHost = "sub.tracker.example.com",
            port = 53,
            protocol = "DNS",
            currentNetwork = NetworkType.WIFI,
            isScreenOn = true,
            isBackground = false,
            quickMode = QuickMode.NORMAL,
            blockedDomains = setOf("tracker.example.com"),
            dnsFilteringEnabled = true
        )
        assertFalse(result.isAllowed)
    }

    @Test
    fun `dns filtering allows non-blocked domain`() {
        val result = engine.evaluate(
            rule = baseRule(blockTrackers = true),
            destinationHost = "safe.example.com",
            port = 53,
            protocol = "DNS",
            currentNetwork = NetworkType.WIFI,
            isScreenOn = true,
            isBackground = false,
            quickMode = QuickMode.NORMAL,
            blockedDomains = setOf("tracker.example.com"),
            dnsFilteringEnabled = true
        )
        assertTrue(result.isAllowed)
    }

    @Test
    fun `dns filtering disabled allows tracker domain`() {
        val result = engine.evaluate(
            rule = baseRule(blockTrackers = true),
            destinationHost = "tracker.example.com",
            port = 53,
            protocol = "DNS",
            currentNetwork = NetworkType.WIFI,
            isScreenOn = true,
            isBackground = false,
            quickMode = QuickMode.NORMAL,
            blockedDomains = setOf("tracker.example.com"),
            dnsFilteringEnabled = false
        )
        assertTrue(result.isAllowed)
    }

    @Test
    fun `dns filtering with blockTrackers false allows tracker domain`() {
        val result = engine.evaluate(
            rule = baseRule(blockTrackers = false),
            destinationHost = "tracker.example.com",
            port = 53,
            protocol = "DNS",
            currentNetwork = NetworkType.WIFI,
            isScreenOn = true,
            isBackground = false,
            quickMode = QuickMode.NORMAL,
            blockedDomains = setOf("tracker.example.com"),
            dnsFilteringEnabled = true
        )
        assertTrue(result.isAllowed)
    }

    // --- Rule precedence ---

    @Test
    fun `quick mode overrides per-app allow`() {
        // App is allowed by its own rule, but quick mode blocks all non-system
        val result = engine.evaluate(
            rule = baseRule(isBlocked = false, isSystemApp = false),
            destinationHost = null,
            port = 0,
            protocol = "ALL",
            currentNetwork = NetworkType.WIFI,
            isScreenOn = true,
            isBackground = false,
            quickMode = QuickMode.BLOCK_NON_SYSTEM,
            blockedDomains = emptySet(),
            dnsFilteringEnabled = false
        )
        assertFalse(result.isAllowed)
        assertTrue(result.reason.contains("Quick Mode"))
    }

    @Test
    fun `app block takes precedence over dns allow`() {
        // App is blocked, domain is not in blocklist
        val result = engine.evaluate(
            rule = baseRule(isBlocked = true),
            destinationHost = "safe.example.com",
            port = 53,
            protocol = "DNS",
            currentNetwork = NetworkType.WIFI,
            isScreenOn = true,
            isBackground = false,
            quickMode = QuickMode.NORMAL,
            blockedDomains = emptySet(),
            dnsFilteringEnabled = true
        )
        assertFalse(result.isAllowed)
        assertTrue(result.reason.contains("App Blocked"))
    }
}
