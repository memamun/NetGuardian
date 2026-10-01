package com.example.dns

import android.net.ConnectivityManager
import android.net.VpnService
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer

/**
 * Forwards DNS queries to an upstream resolver and returns responses.
 *
 * Key design decisions:
 * - Every outbound socket is protected via [VpnService.protect] to prevent
 *   routing loops (our DNS traffic must NOT re-enter the VPN tunnel).
 * - Supports both UDP and TCP DNS.
 * - Handles truncated UDP responses by retrying over TCP.
 * - Handles timeouts and retries.
 * - Discovers system DNS from [ConnectivityManager.getLinkProperties].
 */
open class DnsResolver(
    private val vpnService: VpnService? = null,
    private val connectivityManager: ConnectivityManager? = null
) {
    companion object {
        private const val TAG = "DnsResolver"
        private const val UDP_TIMEOUT_MS = 2500
        private const val TCP_TIMEOUT_MS = 3500
        private const val MAX_DNS_PACKET_SIZE = 4096
        private const val DNS_PORT = 53

        // Fallback DNS servers if system discovery fails
        private val FALLBACK_DNS = listOf(
            InetAddress.getByName("8.8.8.8"),
            InetAddress.getByName("1.1.1.1")
        )
    }

    /**
     * Finds the active underlying physical network (excluding VPN interfaces).
     */
    open fun getPhysicalNetwork(): android.net.Network? {
        val cm = connectivityManager ?: return null
        return try {
            val allNets = cm.allNetworks
            val nonVpnInternet = allNets.firstOrNull { net ->
                val caps = cm.getNetworkCapabilities(net) ?: return@firstOrNull false
                caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                !caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)
            }
            nonVpnInternet ?: cm.activeNetwork
        } catch (_: Exception) {
            cm.activeNetwork
        }
    }

    /**
     * Discovers the DNS servers configured on the device's active physical network.
     *
     * This is the correct implementation for the "System DNS" option —
     * reads from [android.net.LinkProperties] instead of hardcoding an address.
     */
    open fun discoverSystemDns(): List<InetAddress> {
        return try {
            val cm = connectivityManager ?: return FALLBACK_DNS
            val network = getPhysicalNetwork()
            if (network != null) {
                val linkProps = cm.getLinkProperties(network)
                val servers = linkProps?.dnsServers
                if (!servers.isNullOrEmpty()) {
                    Log.d(TAG, "Discovered system DNS from physical network: ${servers.map { it.hostAddress }}")
                    servers
                } else {
                    Log.w(TAG, "No DNS servers in LinkProperties, using fallback")
                    FALLBACK_DNS
                }
            } else {
                Log.w(TAG, "No physical network found, using fallback DNS")
                FALLBACK_DNS
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering system DNS", e)
            FALLBACK_DNS
        }
    }

    /**
     * Resolves a DNS query by forwarding to the specified upstream server.
     *
     * @param queryPayload Raw DNS query bytes (without IP/UDP headers)
     * @param upstream Target DNS server address
     * @param port DNS port (default 53)
     * @param forceTcp Force TCP transport (used for retry after truncation)
     * @return Raw DNS response bytes, or null on failure
     */
    open suspend fun resolve(
        queryPayload: ByteArray,
        upstream: InetAddress,
        port: Int = DNS_PORT,
        forceTcp: Boolean = false
    ): ByteArray? = withContext(Dispatchers.IO) {
        if (forceTcp) {
            resolveTcp(queryPayload, upstream, port)
        } else {
            val udpResponse = resolveUdp(queryPayload, upstream, port)
            if (udpResponse != null && isTruncated(udpResponse)) {
                Log.d(TAG, "UDP response truncated, retrying over TCP")
                resolveTcp(queryPayload, upstream, port) ?: udpResponse
            } else if (udpResponse == null) {
                // Try TCP fallback if UDP timed out (many networks block or degrade UDP 53)
                Log.d(TAG, "UDP resolve timed out for ${upstream.hostAddress}, retrying over TCP")
                val tcpResponse = resolveTcp(queryPayload, upstream, port)
                if (tcpResponse != null) {
                    tcpResponse
                } else {
                    // If chosen upstream fails entirely, fall back to discovered system DNS
                    val systemDnsList = discoverSystemDns()
                    val fallback = systemDnsList.firstOrNull { it != upstream }
                    if (fallback != null) {
                        Log.w(TAG, "Upstream ${upstream.hostAddress} failed, falling back to system DNS ${fallback.hostAddress}")
                        resolveUdp(queryPayload, fallback, port) ?: resolveTcp(queryPayload, fallback, port)
                    } else {
                        null
                    }
                }
            } else {
                udpResponse
            }
        }
    }

    /**
     * Resolves via UDP with timeout, physical network binding, and socket protection.
     */
    private suspend fun resolveUdp(
        queryPayload: ByteArray,
        upstream: InetAddress,
        port: Int
    ): ByteArray? = withTimeoutOrNull(UDP_TIMEOUT_MS.toLong()) {
        try {
            val socket = DatagramSocket()
            try {
                // 1. Explicitly bind socket to physical underlying network to bypass VPN routing
                val physNet = getPhysicalNetwork()
                if (physNet != null) {
                    try {
                        physNet.bindSocket(socket)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to bind UDP socket to physical network: ${e.message}")
                    }
                }

                // 2. Protect socket so it doesn't route through our VPN tunnel
                val isProtected = vpnService?.protect(socket) ?: false
                Log.d(TAG, "UDP socket protected: $isProtected for upstream ${upstream.hostAddress}")

                val requestPacket = DatagramPacket(queryPayload, queryPayload.size, upstream, port)
                socket.soTimeout = UDP_TIMEOUT_MS
                socket.send(requestPacket)

                val responseBuffer = ByteArray(MAX_DNS_PACKET_SIZE)
                val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
                socket.receive(responsePacket)

                Log.d(TAG, "UDP resolve SUCCESS for ${upstream.hostAddress}: ${responsePacket.length} bytes")
                responseBuffer.copyOf(responsePacket.length)
            } finally {
                socket.close()
            }
        } catch (e: IOException) {
            Log.w(TAG, "UDP resolve failed for ${upstream.hostAddress}: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in UDP resolve", e)
            null
        }
    }

    /**
     * Resolves via TCP with 2-byte length prefix per RFC 1035 §4.2.2.
     */
    private suspend fun resolveTcp(
        queryPayload: ByteArray,
        upstream: InetAddress,
        port: Int
    ): ByteArray? = withTimeoutOrNull(TCP_TIMEOUT_MS.toLong()) {
        try {
            val socket = Socket()
            try {
                // 1. Explicitly bind socket to physical underlying network
                val physNet = getPhysicalNetwork()
                if (physNet != null) {
                    try {
                        physNet.bindSocket(socket)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to bind TCP socket to physical network: ${e.message}")
                    }
                }

                // 2. Protect socket so it doesn't route through our VPN tunnel
                val isProtected = vpnService?.protect(socket) ?: false
                Log.d(TAG, "TCP socket protected: $isProtected for upstream ${upstream.hostAddress}")

                socket.soTimeout = TCP_TIMEOUT_MS
                socket.connect(InetSocketAddress(upstream, port), TCP_TIMEOUT_MS)

                val output = socket.getOutputStream()
                val input = socket.getInputStream()

                // Send: 2-byte length prefix + DNS payload
                val lengthPrefix = ByteBuffer.allocate(2).putShort(queryPayload.size.toShort()).array()
                output.write(lengthPrefix)
                output.write(queryPayload)
                output.flush()

                // Receive: 2-byte length prefix
                val lenBuf = ByteArray(2)
                var read = 0
                while (read < 2) {
                    val n = input.read(lenBuf, read, 2 - read)
                    if (n < 0) return@withTimeoutOrNull null
                    read += n
                }
                val responseLen = ByteBuffer.wrap(lenBuf).short.toInt() and 0xFFFF
                if (responseLen > MAX_DNS_PACKET_SIZE || responseLen < 12) {
                    Log.w(TAG, "TCP DNS response has invalid length: $responseLen")
                    return@withTimeoutOrNull null
                }

                // Receive: DNS response payload
                val response = ByteArray(responseLen)
                read = 0
                while (read < responseLen) {
                    val n = input.read(response, read, responseLen - read)
                    if (n < 0) return@withTimeoutOrNull null
                    read += n
                }

                response
            } finally {
                try { socket.close() } catch (_: Exception) {}
            }
        } catch (e: IOException) {
            Log.w(TAG, "TCP resolve failed for ${upstream.hostAddress}: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in TCP resolve", e)
            null
        }
    }

    /**
     * Checks the TC (Truncation) bit in a DNS response header.
     * Bit 1 of the 3rd byte (flags high byte).
     */
    private fun isTruncated(response: ByteArray): Boolean {
        if (response.size < 12) return false
        return (response[2].toInt() and 0x02) != 0
    }
}
