package com.example.dns

import java.nio.ByteBuffer

object DnsPacketParser {

    /**
     * Parses the QNAME (queried domain name) from a raw DNS UDP payload.
     * Returns the domain string (e.g. "adservice.google.com") or null if invalid.
     */
    fun parseDomainName(dnsPayload: ByteArray, offset: Int = 0, length: Int = dnsPayload.size): String? {
        if (length < 12) return null
        val buffer = ByteBuffer.wrap(dnsPayload, offset, length)
        buffer.position(12) // Skip 12-byte DNS header

        val domainParts = StringBuilder()
        while (buffer.hasRemaining()) {
            val labelLength = buffer.get().toInt() and 0xFF
            if (labelLength == 0) break // End of domain
            if (labelLength > 63 || !buffer.hasRemaining() || buffer.remaining() < labelLength) return null

            if (domainParts.isNotEmpty()) {
                domainParts.append('.')
            }
            val labelBytes = ByteArray(labelLength)
            buffer.get(labelBytes)
            domainParts.append(String(labelBytes, Charsets.US_ASCII))
        }
        return if (domainParts.isNotEmpty()) domainParts.toString() else null
    }

    /**
     * Builds a synthetic DNS Response packet returning 0.0.0.0 (Sinkhole/Block) for the given query.
     */
    fun buildSinkholeResponse(queryPayload: ByteArray, queryLength: Int): ByteArray? {
        if (queryLength < 12) return null
        val response = ByteBuffer.allocate(queryLength + 16)

        // Copy Transaction ID
        val txId = ByteBuffer.wrap(queryPayload, 0, 2).short
        response.putShort(txId)

        // Flags: Standard query response, Authoritative, No error (0x8180) or NXDomain
        response.putShort(0x8180.toShort())

        // 1 Question, 1 Answer, 0 Authority, 0 Additional
        response.putShort(1.toShort()) // QDCOUNT
        response.putShort(1.toShort()) // ANCOUNT
        response.putShort(0.toShort()) // NSCOUNT
        response.putShort(0.toShort()) // ARCOUNT

        // Copy Question section from original query
        val questionBytes = queryPayload.copyOfRange(12, queryLength)
        response.put(questionBytes)

        // Answer Section:
        // Name pointer to QNAME at offset 12 (0xC00C)
        response.putShort(0xC00C.toShort())
        response.putShort(1.toShort())      // TYPE A
        response.putShort(1.toShort())      // CLASS IN
        response.putInt(300)                // TTL (5 minutes)
        response.putShort(4.toShort())      // RDLENGTH (4 bytes for IPv4)
        response.put(byteArrayOf(0, 0, 0, 0)) // 0.0.0.0 Sinkhole IP

        return response.array().copyOf(response.position())
    }
}
