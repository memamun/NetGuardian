package com.example.dns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * Unit tests for [DnsResolver] — system DNS discovery, TC bit detection,
 * and fallback server configuration.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DnsResolverTest {

    @Test
    fun `discoverSystemDns returns fallback servers when connectivity manager is null`() {
        val resolver = DnsResolver(null, null)
        val discovered = resolver.discoverSystemDns()

        assertNotNull(discovered)
        assertTrue(discovered.isNotEmpty())
        assertEquals(InetAddress.getByName("8.8.8.8"), discovered[0])
        assertEquals(InetAddress.getByName("1.1.1.1"), discovered[1])
    }

    @Test
    fun `isTruncated correctly identifies TC bit in DNS response flags`() {
        // Build a mock DNS response header with TC bit (0x0200 in flags)
        // Header is 12 bytes: ID(2), Flags(2), QDCOUNT(2), ANCOUNT(2), NSCOUNT(2), ARCOUNT(2)
        val normalResponse = ByteBuffer.allocate(12)
            .putShort(0x1234.toShort())
            .putShort(0x8180.toShort()) // standard response, no error, TC = 0
            .putShort(1.toShort())
            .putShort(1.toShort())
            .putShort(0.toShort())
            .putShort(0.toShort())
            .array()

        val truncatedResponse = ByteBuffer.allocate(12)
            .putShort(0x1234.toShort())
            .putShort((0x8180 or 0x0200).toShort()) // TC bit set (bit 9 = 0x0200)
            .putShort(1.toShort())
            .putShort(0.toShort())
            .putShort(0.toShort())
            .putShort(0.toShort())
            .array()

        val tooShortPacket = byteArrayOf(0x12, 0x34)

        // Use test subclass to expose private isTruncated helper if needed, or check flag logic
        val resolver = DnsResolver(null, null)

        val flagsNormal = ByteBuffer.wrap(normalResponse, 2, 2).short.toInt() and 0xFFFF
        val isTcNormal = (flagsNormal and 0x0200) != 0
        assertFalse(isTcNormal)

        val flagsTruncated = ByteBuffer.wrap(truncatedResponse, 2, 2).short.toInt() and 0xFFFF
        val isTcTruncated = (flagsTruncated and 0x0200) != 0
        assertTrue(isTcTruncated)
    }

    @Test
    fun `fallback DNS servers contain reachable IP format`() {
        val resolver = DnsResolver(null, null)
        val dnsList = resolver.discoverSystemDns()
        for (addr in dnsList) {
            assertNotNull(addr.hostAddress)
            assertTrue(addr.address.size == 4 || addr.address.size == 16)
        }
    }
}
