package com.example.dns

import com.example.firewall.ConnectionLogger
import java.net.InetAddress

/**
 * Intercepts DNS queries, applies blocklist filtering, and forwards
 * permitted queries to the upstream resolver.
 *
 * This is the central decision point for DNS filtering:
 * 1. Parse the queried domain from the raw DNS payload
 * 2. Check against the [DomainMatcher] blocklist
 * 3. If blocked → return a sinkhole response (0.0.0.0)
 * 4. If permitted → forward to the upstream resolver and return the response
 *
 * Separated from the VPN service to enable independent testing and
 * single-responsibility design.
 */
class DnsInterceptor(
    private val domainMatcher: DomainMatcher,
    private val resolver: DnsResolver,
    private val connectionLogger: ConnectionLogger
) {
    companion object {
        private const val TAG = "DnsInterceptor"
    }

    data class InterceptResult(
        val response: ByteArray?,
        val domain: String?,
        val blocked: Boolean,
        val blockReason: String?
    )

    /**
     * Processes a DNS query. Returns the DNS response payload to inject
     * back into the TUN interface.
     *
     * @param dnsQueryPayload Raw DNS query bytes (after UDP/IP headers stripped)
     * @param upstreamAddress The selected upstream DNS server
     * @param sourcePackageName Package name of the querying app (for logging)
     * @param sourceAppName Display name of the querying app (for logging)
     */
    suspend fun intercept(
        dnsQueryPayload: ByteArray,
        upstreamAddress: InetAddress,
        sourcePackageName: String = "Unknown",
        sourceAppName: String = "Unknown"
    ): InterceptResult {
        // 1. Parse the queried domain
        val domain = DnsPacketParser.parseDomainName(dnsQueryPayload, 0, dnsQueryPayload.size)
        if (domain == null) {
            return InterceptResult(null, null, false, "Invalid DNS query")
        }

        // 2. Check blocklist
        val matchResult = domainMatcher.match(domain)

        if (matchResult.blocked) {
            // 3. BLOCKED — return sinkhole response
            val sinkholeResponse = DnsPacketParser.buildSinkholeResponse(dnsQueryPayload, dnsQueryPayload.size)

            connectionLogger.logConnection(
                packageName = sourcePackageName,
                appName = sourceAppName,
                destinationHost = domain,
                port = 53,
                protocol = "DNS",
                isBlocked = true,
                blockReason = "DNS Blocklist (${matchResult.matchedRule})",
                bytes = dnsQueryPayload.size.toLong()
            )

            return InterceptResult(sinkholeResponse, domain, true, "DNS Blocklist (${matchResult.matchedRule})")
        }

        // 4. PERMITTED — forward to upstream resolver
        val response = resolver.resolve(dnsQueryPayload, upstreamAddress)

        if (response == null) {
            val servfailResponse = buildServfailResponse(dnsQueryPayload)

            connectionLogger.logConnection(
                packageName = sourcePackageName,
                appName = sourceAppName,
                destinationHost = domain,
                port = 53,
                protocol = "DNS",
                isBlocked = false,
                blockReason = "Resolver Error (SERVFAIL)",
                bytes = dnsQueryPayload.size.toLong()
            )

            return InterceptResult(servfailResponse, domain, false, "Resolver Error")
        }

        connectionLogger.logConnection(
            packageName = sourcePackageName,
            appName = sourceAppName,
            destinationHost = domain,
            port = 53,
            protocol = "DNS",
            isBlocked = false,
            blockReason = "Allowed",
            bytes = response.size.toLong()
        )

        return InterceptResult(response, domain, false, null)
    }

    /**
     * Builds a SERVFAIL response for when the upstream resolver fails.
     * Preserves the transaction ID from the query.
     */
    private fun buildServfailResponse(queryPayload: ByteArray): ByteArray? {
        if (queryPayload.size < 12) return null
        val response = queryPayload.copyOf()
        // Set QR=1 (response), RCODE=2 (SERVFAIL)
        response[2] = (response[2].toInt() or 0x80).toByte() // QR = 1
        response[3] = (response[3].toInt() and 0xF0 or 0x02).toByte() // RCODE = SERVFAIL
        return response
    }
}
