package com.example.dns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

/**
 * Unit tests for [DnsPacketParser] — query parsing and sinkhole response generation.
 */
class DnsPacketParserTest {

    /**
     * Builds a minimal DNS query packet for a given domain.
     * Format: 12-byte header + QNAME + QTYPE(2) + QCLASS(2)
     */
    private fun buildDnsQuery(domain: String, transactionId: Short = 0x1234): ByteArray {
        val labels = domain.split('.')
        // Calculate QNAME size: each label has 1-byte length prefix + label bytes, then 1 null terminator
        val qnameSize = labels.sumOf { 1 + it.length } + 1
        val totalSize = 12 + qnameSize + 4 // header + qname + qtype(2) + qclass(2)
        val buffer = ByteBuffer.allocate(totalSize)

        // DNS Header (12 bytes)
        buffer.putShort(transactionId)   // Transaction ID
        buffer.putShort(0x0100)          // Flags: standard query
        buffer.putShort(1)               // QDCOUNT: 1 question
        buffer.putShort(0)               // ANCOUNT
        buffer.putShort(0)               // NSCOUNT
        buffer.putShort(0)               // ARCOUNT

        // QNAME
        for (label in labels) {
            buffer.put(label.length.toByte())
            buffer.put(label.toByteArray(Charsets.US_ASCII))
        }
        buffer.put(0.toByte()) // Null terminator

        // QTYPE (A record = 1) and QCLASS (IN = 1)
        buffer.putShort(1)
        buffer.putShort(1)

        return buffer.array()
    }

    @Test
    fun `parse simple domain`() {
        val query = buildDnsQuery("example.com")
        val domain = DnsPacketParser.parseDomainName(query, 0, query.size)
        assertEquals("example.com", domain)
    }

    @Test
    fun `parse subdomain`() {
        val query = buildDnsQuery("sub.example.com")
        val domain = DnsPacketParser.parseDomainName(query, 0, query.size)
        assertEquals("sub.example.com", domain)
    }

    @Test
    fun `parse deep subdomain`() {
        val query = buildDnsQuery("a.b.c.d.example.com")
        val domain = DnsPacketParser.parseDomainName(query, 0, query.size)
        assertEquals("a.b.c.d.example.com", domain)
    }

    @Test
    fun `parse single label domain`() {
        val query = buildDnsQuery("localhost")
        val domain = DnsPacketParser.parseDomainName(query, 0, query.size)
        assertEquals("localhost", domain)
    }

    @Test
    fun `return null for truncated packet below header size`() {
        val truncated = ByteArray(8) // Less than 12-byte DNS header
        val domain = DnsPacketParser.parseDomainName(truncated, 0, truncated.size)
        assertNull(domain)
    }

    @Test
    fun `return null for empty payload`() {
        val domain = DnsPacketParser.parseDomainName(ByteArray(0), 0, 0)
        assertNull(domain)
    }

    @Test
    fun `sinkhole response has valid structure`() {
        val query = buildDnsQuery("tracker.example.com")
        val response = DnsPacketParser.buildSinkholeResponse(query, query.size)

        assertNotNull(response)
        response!!

        // Verify transaction ID is preserved
        val queryTxId = ByteBuffer.wrap(query, 0, 2).short
        val responseTxId = ByteBuffer.wrap(response, 0, 2).short
        assertEquals(queryTxId, responseTxId)

        // Verify response flags (QR=1, AA=1, RCODE=0 → 0x8180)
        val flags = ByteBuffer.wrap(response, 2, 2).short
        assertEquals(0x8180.toShort(), flags)

        // Verify 1 question, 1 answer
        val qdCount = ByteBuffer.wrap(response, 4, 2).short
        assertEquals(1.toShort(), qdCount)
        val anCount = ByteBuffer.wrap(response, 6, 2).short
        assertEquals(1.toShort(), anCount)
    }

    @Test
    fun `sinkhole response for too-small payload returns null`() {
        val tooSmall = ByteArray(8)
        val response = DnsPacketParser.buildSinkholeResponse(tooSmall, tooSmall.size)
        assertNull(response)
    }

    @Test
    fun `sinkhole response contains 0_0_0_0 address`() {
        val query = buildDnsQuery("ad.example.com")
        val response = DnsPacketParser.buildSinkholeResponse(query, query.size)
        assertNotNull(response)
        response!!

        // The last 4 bytes of the answer should be 0.0.0.0
        val lastFour = response.takeLast(4)
        assertTrue("Expected 0.0.0.0 sinkhole IP", lastFour.all { it == 0.toByte() })
    }
}
