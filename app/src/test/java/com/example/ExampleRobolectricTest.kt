package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.QuickMode
import com.example.database.AppRuleEntity
import com.example.dns.DnsPacketParser
import com.example.firewall.NetworkType
import com.example.firewall.RuleEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NetGuardian", appName)
    }

    @Test
    fun `rule engine blocks app when isBlocked is true`() {
        val engine = RuleEngine()
        val rule = AppRuleEntity(
            packageName = "com.untrusted.app",
            appName = "Untrusted",
            uid = 10050,
            isBlocked = true
        )
        val decision = engine.evaluate(
            rule = rule,
            destinationHost = "example.com",
            port = 443,
            protocol = "TCP",
            currentNetwork = NetworkType.WIFI,
            isScreenOn = true,
            isBackground = false,
            quickMode = QuickMode.NORMAL,
            blockedDomains = emptySet(),
            dnsFilteringEnabled = false
        )
        assertFalse(decision.isAllowed)
    }

    @Test
    fun `rule engine blocks cellular when blockMobile is true`() {
        val engine = RuleEngine()
        val rule = AppRuleEntity(
            packageName = "com.heavy.streamer",
            appName = "Streamer",
            uid = 10051,
            blockMobile = true
        )
        val wifiDecision = engine.evaluate(
            rule = rule,
            destinationHost = "stream.com",
            port = 443,
            protocol = "TCP",
            currentNetwork = NetworkType.WIFI,
            isScreenOn = true,
            isBackground = false,
            quickMode = QuickMode.NORMAL,
            blockedDomains = emptySet(),
            dnsFilteringEnabled = false
        )
        assertTrue(wifiDecision.isAllowed)

        val mobileDecision = engine.evaluate(
            rule = rule,
            destinationHost = "stream.com",
            port = 443,
            protocol = "TCP",
            currentNetwork = NetworkType.MOBILE,
            isScreenOn = true,
            isBackground = false,
            quickMode = QuickMode.NORMAL,
            blockedDomains = emptySet(),
            dnsFilteringEnabled = false
        )
        assertFalse(mobileDecision.isAllowed)
    }

    @Test
    fun `rule engine blocks screen off when blockScreenOff is true`() {
        val engine = RuleEngine()
        val rule = AppRuleEntity(
            packageName = "com.background.miner",
            appName = "Miner",
            uid = 10052,
            blockScreenOff = true
        )
        val screenOffDecision = engine.evaluate(
            rule = rule,
            destinationHost = "pool.com",
            port = 443,
            protocol = "TCP",
            currentNetwork = NetworkType.WIFI,
            isScreenOn = false,
            isBackground = false,
            quickMode = QuickMode.NORMAL,
            blockedDomains = emptySet(),
            dnsFilteringEnabled = false
        )
        assertFalse(screenOffDecision.isAllowed)
    }

    @Test
    fun `rule engine blocks tracker domain when dns filtering is enabled`() {
        val engine = RuleEngine()
        val rule = AppRuleEntity(
            packageName = "com.sample.app",
            appName = "Sample",
            uid = 10053,
            blockTrackers = true
        )
        val decision = engine.evaluate(
            rule = rule,
            destinationHost = "adservice.google.com",
            port = 53,
            protocol = "UDP",
            currentNetwork = NetworkType.WIFI,
            isScreenOn = true,
            isBackground = false,
            quickMode = QuickMode.NORMAL,
            blockedDomains = setOf("adservice.google.com", "telemetry.bad.com"),
            dnsFilteringEnabled = true
        )
        assertFalse(decision.isAllowed)
    }
}
