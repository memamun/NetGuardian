package com.example.dns

import com.example.firewall.ConnectionLogger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.nio.ByteBuffer

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [DnsInterceptor] — verifying blocking, forwarding,
 * subdomain matching, and error fallback behaviors.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DnsInterceptorTest {

    private lateinit var domainMatcher: DomainMatcher
    private lateinit var fakeResolver: FakeDnsResolver
    private lateinit var fakeLogger: FakeConnectionLogger
    private lateinit var interceptor: DnsInterceptor
    private val upstreamDns = InetAddress.getByName("8.8.8.8")

    class FakeDnsResolver : DnsResolver(null, null) {
        var responseToReturn: ByteArray? = byteArrayOf(0x12, 0x34, 0x81.toByte(), 0x80.toByte())
        var lastReceivedQuery: ByteArray? = null
        var lastReceivedUpstream: InetAddress? = null

        override suspend fun resolve(
            queryPayload: ByteArray,
            upstream: InetAddress,
            port: Int,
            forceTcp: Boolean
        ): ByteArray? {
            lastReceivedQuery = queryPayload
            lastReceivedUpstream = upstream
            return responseToReturn
        }
    }

    class FakeConnectionLogger : ConnectionLogger(null) {
        val loggedEvents = mutableListOf<LoggedEvent>()

        data class LoggedEvent(
            val packageName: String,
            val destinationHost: String,
            val isBlocked: Boolean,
            val blockReason: String
        )

        override fun logConnection(
            packageName: String,
            appName: String,
            destinationHost: String,
            port: Int,
            protocol: String,
            isBlocked: Boolean,
            blockReason: String,
            bytes: Long
        ) {
            loggedEvents.add(LoggedEvent(packageName, destinationHost, isBlocked, blockReason))
        }
    }

    @Before
    fun setUp() {
        domainMatcher = DomainMatcher()
        domainMatcher.updateRules(setOf("doubleclick.net", "analytics.google.com", "badtracker.org"))
        fakeResolver = FakeDnsResolver()
        fakeLogger = FakeConnectionLogger()
        interceptor = DnsInterceptor(domainMatcher, fakeResolver, fakeLogger)
    }

    private fun buildSimpleQueryPacket(domain: String, txId: Short = 0x1234.toShort()): ByteArray {
        val out = ByteArrayOutputStream()
        // 12-byte DNS header
        val header = ByteBuffer.allocate(12)
        header.putShort(txId)
        header.putShort(0x0100.toShort()) // standard query, recursion desired
        header.putShort(1.toShort())      // QDCOUNT = 1
        header.putShort(0.toShort())      // ANCOUNT = 0
        header.putShort(0.toShort())      // NSCOUNT = 0
        header.putShort(0.toShort())      // ARCOUNT = 0
        out.write(header.array())

        // QNAME
        for (label in domain.split('.')) {
            val bytes = label.toByteArray(Charsets.US_ASCII)
            out.write(bytes.size)
            out.write(bytes)
        }
        out.write(0) // root label

        // QTYPE = A (1), QCLASS = IN (1)
        val qFooter = ByteBuffer.allocate(4)
        qFooter.putShort(1.toShort())
        qFooter.putShort(1.toShort())
        out.write(qFooter.array())

        return out.toByteArray()
    }

    @Test
    fun `blocked domain returns sinkhole response and logs blocked connection`() = runBlocking {
        val query = buildSimpleQueryPacket("doubleclick.net")
        val result = interceptor.intercept(
            dnsQueryPayload = query,
            upstreamAddress = upstreamDns,
            sourcePackageName = "com.test.app",
            sourceAppName = "Test App"
        )

        assertTrue(result.blocked)
        assertEquals("doubleclick.net", result.domain)
        assertNotNull(result.response)

        // Verify sinkhole address 0.0.0.0 is present in the response
        val response = result.response!!
        val answerIp = response.takeLast(4).toByteArray()
        assertEquals(0.toByte(), answerIp[0])
        assertEquals(0.toByte(), answerIp[1])
        assertEquals(0.toByte(), answerIp[2])
        assertEquals(0.toByte(), answerIp[3])

        // Verify resolver was not called
        assertNull(fakeResolver.lastReceivedQuery)

        // Verify connection logger recorded the blocked event
        assertEquals(1, fakeLogger.loggedEvents.size)
        val event = fakeLogger.loggedEvents.first()
        assertTrue(event.isBlocked)
        assertEquals("com.test.app", event.packageName)
        assertEquals("doubleclick.net", event.destinationHost)
    }

    @Test
    fun `subdomain of blocked domain is blocked`() = runBlocking {
        val query = buildSimpleQueryPacket("ads.doubleclick.net")
        val result = interceptor.intercept(
            dnsQueryPayload = query,
            upstreamAddress = upstreamDns
        )

        assertTrue(result.blocked)
        assertEquals("ads.doubleclick.net", result.domain)
        assertTrue(result.blockReason?.contains("doubleclick.net") == true)
        assertNotNull(result.response)
        assertNull(fakeResolver.lastReceivedQuery)
    }

    @Test
    fun `permitted domain forwards to resolver and returns resolver response`() = runBlocking {
        val expectedResponse = byteArrayOf(0x12, 0x34, 0x81.toByte(), 0x80.toByte(), 0x00, 0x01)
        fakeResolver.responseToReturn = expectedResponse

        val query = buildSimpleQueryPacket("example.com")
        val result = interceptor.intercept(
            dnsQueryPayload = query,
            upstreamAddress = upstreamDns,
            sourcePackageName = "com.safe.app"
        )

        assertFalse(result.blocked)
        assertEquals("example.com", result.domain)
        assertEquals(expectedResponse, result.response)
        assertNotNull(fakeResolver.lastReceivedQuery)
        assertEquals(upstreamDns, fakeResolver.lastReceivedUpstream)

        // Verify logged as allowed
        assertEquals(1, fakeLogger.loggedEvents.size)
        val event = fakeLogger.loggedEvents.first()
        assertFalse(event.isBlocked)
        assertEquals("com.safe.app", event.packageName)
        assertEquals("example.com", event.destinationHost)
        assertEquals("Allowed", event.blockReason)
    }

    @Test
    fun `resolver failure returns SERVFAIL response with preserved transaction id`() = runBlocking {
        fakeResolver.responseToReturn = null // simulate resolver network timeout

        val txId = 0x4321.toShort()
        val query = buildSimpleQueryPacket("unreachable.org", txId = txId)
        val result = interceptor.intercept(
            dnsQueryPayload = query,
            upstreamAddress = upstreamDns,
            sourcePackageName = "com.safe.app"
        )

        assertFalse(result.blocked)
        assertEquals("Resolver Error", result.blockReason)
        assertNotNull(result.response)

        val response = result.response!!
        // Check transaction ID is preserved
        val respTxId = ByteBuffer.wrap(response, 0, 2).short
        assertEquals(txId, respTxId)

        // Check SERVFAIL (RCODE = 2) in flags
        val flags = ByteBuffer.wrap(response, 2, 2).short.toInt() and 0xFFFF
        val rcode = flags and 0x000F
        assertEquals(2, rcode) // SERVFAIL

        // Check logged as resolver error
        assertEquals(1, fakeLogger.loggedEvents.size)
        assertEquals("Resolver Error (SERVFAIL)", fakeLogger.loggedEvents.first().blockReason)
    }

    @Test
    fun `unparseable query payload forwards blindly to resolver`() = runBlocking {
        val corruptedPayload = byteArrayOf(0x01, 0x02) // truncated header
        val result = interceptor.intercept(
            dnsQueryPayload = corruptedPayload,
            upstreamAddress = upstreamDns
        )

        assertFalse(result.blocked)
        assertNull(result.domain)
        assertNotNull(fakeResolver.lastReceivedQuery)
    }
}
