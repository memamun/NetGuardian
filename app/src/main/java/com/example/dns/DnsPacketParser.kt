package com.example.dns

import java.nio.ByteBuffer

/** Defensive parsing for one uncompressed DNS question. Unsupported packets are rejected. */
object DnsPacketParser {
    private data class Question(val domain: String, val end: Int, val type: Int, val recordClass: Int)

    private fun question(bytes: ByteArray, offset: Int, length: Int): Question? {
        if (offset < 0 || length < 12 || offset > bytes.size || length > bytes.size - offset) return null
        val end = offset + length
        fun u16(index: Int) = ((bytes[index].toInt() and 255) shl 8) or (bytes[index + 1].toInt() and 255)
        if (u16(offset + 4) != 1 || bytes[offset + 2].toInt() and 0xF8 != 0) return null
        var cursor = offset + 12
        val labels = mutableListOf<String>()
        while (cursor < end) {
            val size = bytes[cursor++].toInt() and 255
            if (size == 0) {
                if (labels.isEmpty() || end - cursor < 4 || cursor - offset - 12 > 255) return null
                return Question(labels.joinToString("."), cursor + 4, u16(cursor), u16(cursor + 2))
            }
            if (size > 63 || size > end - cursor) return null
            val label = bytes.copyOfRange(cursor, cursor + size)
            if (label.any { (it.toInt() and 255) !in 33..126 || it == '.'.code.toByte() }) return null
            labels += String(label, Charsets.US_ASCII)
            cursor += size
        }
        return null
    }

    fun parseDomainName(dnsPayload: ByteArray, offset: Int = 0, length: Int = dnsPayload.size): String? =
        question(dnsPayload, offset, length)?.domain

    /** Return zero addresses for A/AAAA, and an empty answer for other query types. */
    fun buildSinkholeResponse(queryPayload: ByteArray, queryLength: Int): ByteArray? {
        val q = question(queryPayload, 0, queryLength) ?: return null
        val addressSize = if (q.recordClass != 1) 0 else when (q.type) { 1 -> 4; 28 -> 16; else -> 0 }
        val response = ByteBuffer.allocate(q.end + if (addressSize > 0) 12 + addressSize else 0)
        response.put(queryPayload, 0, 2)
        response.putShort((0x8080 or ((queryPayload[2].toInt() and 1) shl 8)).toShort())
        response.putShort(1)
        response.putShort(if (addressSize > 0) 1 else 0)
        response.putShort(0)
        response.putShort(0)
        response.put(queryPayload, 12, q.end - 12)
        if (addressSize > 0) {
            response.putShort(0xC00C.toShort())
            response.putShort(q.type.toShort())
            response.putShort(1)
            response.putInt(300)
            response.putShort(addressSize.toShort())
            response.put(ByteArray(addressSize))
        }
        return response.array()
    }
}
