package com.example.firewall

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.IpPrefix
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.util.Log
import java.net.Inet4Address
import com.example.data.PreferencesRepository
import com.example.data.QuickMode
import com.example.data.UpstreamDnsType
import com.example.database.AppDatabase
import com.example.dns.DnsPacketParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.nio.ByteBuffer
import com.example.dns.DnsInterceptor
import com.example.dns.DnsResolver
import com.example.dns.DomainMatcher

class FirewallVpnService : VpnService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var vpnInterface: ParcelFileDescriptor? = null
    private var workerThread: Thread? = null

    private lateinit var firewallManager: FirewallManager
    private lateinit var stateRepo: FirewallStateRepository
    private lateinit var notificationManager: FirewallNotificationManager
    private lateinit var database: AppDatabase
    private lateinit var connectionLogger: ConnectionLogger
    private lateinit var prefsRepo: PreferencesRepository
    private val ruleEngine = RuleEngine()

    private var isRunning = false
    private var isPaused = false
    private var userExplicitStop = false
    private var pauseJob: Job? = null
    private var blockedDomains = setOf<String>()

    // DNS engine components
    private lateinit var dnsResolver: DnsResolver
    private val domainMatcher = DomainMatcher()
    private lateinit var dnsInterceptor: DnsInterceptor
    private var currentUpstreamDns: InetAddress? = null
    private var dnsFilteringActive = false

    private var lastNotificationUpdateMs = 0L

    private val systemEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    firewallManager.setScreenState(true)
                    reconfigureVpn()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    firewallManager.setScreenState(false)
                    reconfigureVpn()
                }
                PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
                        firewallManager.setDeviceIdleState(pm?.isDeviceIdleMode == true)
                        reconfigureVpn()
                    }
                }
            }
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: android.net.Network, capabilities: android.net.NetworkCapabilities) {
            val newType = when {
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> com.example.firewall.NetworkType.WIFI
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> com.example.firewall.NetworkType.MOBILE
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> com.example.firewall.NetworkType.ETHERNET
                else -> com.example.firewall.NetworkType.NONE
            }
            firewallManager.setNetworkType(newType)
            updateUnderlyingNetworks()
            reconfigureVpn()
        }

        override fun onLost(network: android.net.Network) {
            // Don't assume NONE — a cellular network may already be active.
            // reconfigureVpn() will query the real state.
            updateUnderlyingNetworks()
            reconfigureVpn()
        }

        override fun onAvailable(network: android.net.Network) {
            updateUnderlyingNetworks()
            reconfigureVpn()
        }
    }

    override fun onCreate() {
        super.onCreate()
        firewallManager = FirewallManager.getInstance(applicationContext)
        stateRepo = FirewallStateRepository.getInstance(applicationContext)
        notificationManager = FirewallNotificationManager(applicationContext)
        database = AppDatabase.getDatabase(applicationContext)
        connectionLogger = ConnectionLogger(database.firewallDao())
        prefsRepo = PreferencesRepository(applicationContext)

        // Initialize DNS engine
        val connectivityMgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        dnsResolver = DnsResolver(this, connectivityMgr)
        dnsInterceptor = DnsInterceptor(domainMatcher, dnsResolver, connectionLogger)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            firewallManager.setDeviceIdleState(pm?.isDeviceIdleMode == true)
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
            }
        }
        registerReceiver(systemEventReceiver, filter)

        val request = android.net.NetworkRequest.Builder()
            .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityMgr.registerNetworkCallback(request, networkCallback)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startVpn()
            ACTION_STOP -> stopVpn()
            ACTION_PAUSE_10_MIN -> pauseVpn(10)
            ACTION_RESUME -> resumeVpn()
            ACTION_RELOAD_RULES -> reconfigureVpn()
            ACTION_QUICK_BLOCK_NON_SYSTEM -> {
                serviceScope.launch {
                    firewallManager.setQuickMode(QuickMode.BLOCK_NON_SYSTEM)
                    database.firewallDao().setBlockAllNonSystem(true)
                    reconfigureVpn()
                }
            }
            ACTION_QUICK_ALLOW_ALL -> {
                serviceScope.launch {
                    firewallManager.setQuickMode(QuickMode.NORMAL)
                    database.firewallDao().resetAllRulesToAllow()
                    reconfigureVpn()
                }
            }
            ACTION_QUICK_WIFI_ONLY -> {
                serviceScope.launch {
                    val next = if (firewallManager.activeQuickMode.value == QuickMode.WIFI_ONLY) QuickMode.NORMAL else QuickMode.WIFI_ONLY
                    firewallManager.setQuickMode(next)
                    reconfigureVpn()
                }
            }
            ACTION_QUICK_MOBILE_ONLY -> {
                serviceScope.launch {
                    val next = if (firewallManager.activeQuickMode.value == QuickMode.MOBILE_ONLY) QuickMode.NORMAL else QuickMode.MOBILE_ONLY
                    firewallManager.setQuickMode(next)
                    reconfigureVpn()
                }
            }
            else -> startVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (isRunning) return

        // Verify VPN permission first
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            stateRepo.setVpnPermissionRequired()
            stopSelf()
            return
        }

        userExplicitStop = false
        isRunning = true
        isPaused = false
        stateRepo.setStarting()

        // Post foreground notification immediately
        startForeground(
            FirewallNotificationManager.NOTIFICATION_ID_FOREGROUND,
            notificationManager.buildForegroundNotification(stateRepo.state.value)
        )
        notificationManager.dismissAlertNotifications()

        serviceScope.launch {
            prefsRepo.setFirewallEnabled(true)
            loadBlocklists()
            establishVpn()
        }
    }

    private suspend fun loadBlocklists() {
        try {
            AppDatabase.populateDefaultBlocklists(database.firewallDao())
            val list = database.firewallDao().getActiveBlocklistDomainsSync()
            blockedDomains = list.toSet()
            // Update the domain matcher atomically — this is thread-safe
            // and takes effect immediately for in-flight DNS queries
            domainMatcher.updateRules(blockedDomains)
        } catch (_: Exception) {
            blockedDomains = emptySet()
            domainMatcher.updateRules(emptySet())
        }
    }

    private fun reconfigureVpn() {
        if (!isRunning) return
        serviceScope.launch {
            // Actively query the real current network type — don't trust the stored value
            // which may be stale during WiFi→cellular transitions.
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(activeNetwork)
            val realNetworkType = if (caps != null) {
                when {
                    caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> com.example.firewall.NetworkType.WIFI
                    caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> com.example.firewall.NetworkType.MOBILE
                    caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> com.example.firewall.NetworkType.ETHERNET
                    else -> com.example.firewall.NetworkType.NONE
                }
            } else {
                com.example.firewall.NetworkType.NONE
            }
            firewallManager.setNetworkType(realNetworkType)

            loadBlocklists()
            establishVpn()
        }
    }

    private fun pauseVpn(durationMinutes: Int) {
        if (!isRunning) return
        isPaused = true
        val until = System.currentTimeMillis() + durationMinutes * 60 * 1000L
        stateRepo.setPaused(until)
        updateForegroundNotification()

        pauseJob?.cancel()
        pauseJob = serviceScope.launch {
            delay(durationMinutes * 60 * 1000L)
            if (isRunning && isPaused) {
                resumeVpn()
            }
        }
    }

    private fun resumeVpn() {
        if (!isRunning) return
        pauseJob?.cancel()
        isPaused = false
        stateRepo.resumeFromPause()
        serviceScope.launch {
            reconfigureVpn()
        }
    }

    private suspend fun establishVpn() {
        try {
            val rules = database.firewallDao().getAllRulesSync()
            val prefs = prefsRepo.userPreferencesFlow.first()
            val currentNetwork = firewallManager.networkType.value
            val isScreenOn = firewallManager.isScreenOn.value
            val isDeviceIdle = firewallManager.isDeviceIdle.value
            val quickMode = prefs.activeQuickMode

            val (primaryDns, secondaryDns) = when (prefs.upstreamDnsType) {
                UpstreamDnsType.LOCAL_SINKHOLE -> Pair("127.0.0.1", null)
                UpstreamDnsType.SYSTEM_DEFAULT -> {
                    // Discover real system DNS from the underlying network
                    val systemDns = dnsResolver.discoverSystemDns()
                    val primary = systemDns.firstOrNull()?.hostAddress ?: "8.8.8.8"
                    val secondary = systemDns.getOrNull(1)?.hostAddress
                    Pair(primary, secondary)
                }
                UpstreamDnsType.ADGUARD -> Pair("94.140.14.14", "94.140.15.15")
                UpstreamDnsType.CONTROLD -> Pair("76.76.2.2", "76.76.10.2")
                UpstreamDnsType.CUSTOM -> Pair(if (prefs.customDnsIp.isNotBlank()) prefs.customDnsIp else "127.0.0.1", null)
                UpstreamDnsType.CLOUDFLARE -> Pair("1.1.1.1", "1.0.0.1")
                UpstreamDnsType.QUAD9 -> Pair("9.9.9.9", "149.112.112.112")
                UpstreamDnsType.GOOGLE -> Pair("8.8.8.8", "8.8.4.4")
            }

            // Store the resolved upstream address for the DNS interceptor
            currentUpstreamDns = try { InetAddress.getByName(primaryDns) } catch (_: Exception) { null }
            dnsFilteringActive = prefs.dnsFilteringEnabled

            // Evaluate which apps are blocked
            var blockedCount = 0
            val blockedPackages = mutableListOf<String>()

            if (!isPaused) {
                for (rule in rules) {
                    val decision = ruleEngine.evaluate(
                        rule = rule,
                        destinationHost = null,
                        port = 0,
                        protocol = "ALL",
                        currentNetwork = currentNetwork,
                        isScreenOn = isScreenOn,
                        isBackground = false,
                        isDeviceIdle = isDeviceIdle,
                        quickMode = quickMode,
                        blockedDomains = blockedDomains,
                        dnsFilteringEnabled = prefs.dnsFilteringEnabled
                    )

                    if (!decision.isAllowed) {
                        blockedPackages.add(rule.packageName)
                        blockedCount++
                    }
                }
            }

            // Exclude NetGuardian itself from being added to blockedPackages
            blockedPackages.remove(packageName)

            if (blockedPackages.isEmpty()) {
                // No apps need blocking — tear down the VPN so all traffic
                // passes through the real network unimpeded.
                val oldIface = vpnInterface
                vpnInterface = null
                try { oldIface?.close() } catch (_: Exception) {}
                if (!isPaused) {
                    stateRepo.setRunning(blockedApps = 0, blockedConnections = 0, dnsEffective = false)
                }
                updateForegroundNotification()
                return
            }

            val builder = Builder()
                .setSession("NetGuardian")
                .addAddress("10.1.10.1", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer(primaryDns)

            if (secondaryDns != null) {
                builder.addDnsServer(secondaryDns)
            }

            // Exclude local private subnets (RFC 1918) and link-local ranges so local LAN devices
            // (printers, IoT, local routers) are reached directly over physical Wi-Fi even by apps
            // restricted from the public internet (blocking ads while retaining local printing).
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listOf(
                    Pair("192.168.0.0", 16),
                    Pair("172.16.0.0", 12),
                    Pair("169.254.0.0", 16)
                ).forEach { (ip, prefix) ->
                    try {
                        builder.excludeRoute(IpPrefix(InetAddress.getByName(ip), prefix))
                    } catch (e: Exception) {
                        Log.d(TAG, "excludeRoute for $ip/$prefix ignored: ${e.message}")
                    }
                }

                // Also dynamically exclude the active physical Wi-Fi subnet
                try {
                    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    val physNet = getPhysicalNetwork()
                    if (physNet != null && cm != null) {
                        val linkProps = cm.getLinkProperties(physNet)
                        linkProps?.linkAddresses?.forEach { linkAddr ->
                            if (linkAddr.address is Inet4Address && !linkAddr.address.isLoopbackAddress) {
                                val prefix = linkAddr.prefixLength
                                val host = linkAddr.address.hostAddress
                                if (prefix in 8..30 && (host == null || !host.startsWith("10.1.10."))) {
                                    try {
                                        builder.excludeRoute(IpPrefix(linkAddr.address, prefix))
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // Route ONLY blocked apps through the sinkhole VPN.
            // Non-blocked apps bypass the VPN entirely and have normal connectivity.
            for (pkg in blockedPackages) {
                try {
                    builder.addAllowedApplication(pkg)
                } catch (e: PackageManager.NameNotFoundException) {
                    // Package was uninstalled
                }
            }

            val oldInterface = vpnInterface
            val newInterface = builder.establish()

            if (newInterface == null) {
                Log.e(TAG, "VPN establish returned null. Lockdown mode or conflicting VPN active.")
                stateRepo.setError("Cannot establish VPN tunnel. Ensure no other VPN is in lockdown mode.")
                notificationManager.showFirewallErrorAlert("Cannot establish VPN tunnel. Another VPN may be active.")
                stopVpn()
                return
            }

            vpnInterface = newInterface
            oldInterface?.close()

            // Update underlying networks so protected sockets can route directly over physical Wi-Fi/Cellular
            updateUnderlyingNetworks()

            // Fetch total connection stats from database for accurate notification counts
            val totalConns = try {
                val startOfDay = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                database.firewallDao().getTodayBlockedCount(startOfDay).first()
            } catch (_: Exception) {
                0
            }

            if (!isPaused) {
                stateRepo.setRunning(
                    blockedApps = blockedCount,
                    blockedConnections = totalConns,
                    dnsEffective = prefs.dnsFilteringEnabled
                )
            }
            updateForegroundNotification()

            startPacketProcessor()
        } catch (e: Exception) {
            Log.e(TAG, "Error establishing VPN", e)
            stateRepo.setError("Firewall startup error: ${e.localizedMessage ?: "Unknown error"}")
            notificationManager.showFirewallErrorAlert("Failed to start firewall: ${e.localizedMessage}")
            stopVpn()
        }
    }

    private fun startPacketProcessor() {
        workerThread?.interrupt()
        val pfd = vpnInterface ?: return

        workerThread = Thread({
            val inStream = FileInputStream(pfd.fileDescriptor)
            val outStream = FileOutputStream(pfd.fileDescriptor)
            val buffer = ByteArray(32767)

            try {
                while (!Thread.currentThread().isInterrupted && isRunning) {
                    val length = inStream.read(buffer)
                    if (length <= 0) continue

                    if (isPaused) {
                        // In paused state, let all traffic pass through unfiltered
                        continue
                    }

                    val version = (buffer[0].toInt() shr 4) and 0x0F
                    if (version == 4 && length >= 20) {
                        val protocol = buffer[9].toInt() and 0xFF
                        val destIp = InetAddress.getByAddress(buffer.copyOfRange(16, 20)).hostAddress ?: "Unknown"

                        if (protocol == 17) {
                            // UDP packet (Protocol 17) - often DNS (port 53)
                            val ipHeaderLen = (buffer[0].toInt() and 0x0F) * 4
                            if (length >= ipHeaderLen + 8) {
                                val destPort = ByteBuffer.wrap(buffer, ipHeaderLen + 2, 2).short.toInt() and 0xFFFF
                                if (destPort == 53 && length > ipHeaderLen + 8) {
                                    val dnsPayload = buffer.copyOfRange(ipHeaderLen + 8, length)
                                    val queryDomain = com.example.dns.DnsPacketParser.parseDomainName(dnsPayload, 0, dnsPayload.size)

                                    if (queryDomain != null) {
                                        // Use the DNS interceptor for filtering + resolution
                                        val upstream = currentUpstreamDns
                                        if (dnsFilteringActive && upstream != null) {
                                            // DNS engine path: filter, then forward or sinkhole
                                            val packetCopy = dnsPayload.copyOf()
                                            val upstreamCopy = upstream
                                            serviceScope.launch {
                                                try {
                                                    val result = dnsInterceptor.intercept(
                                                        dnsQueryPayload = packetCopy,
                                                        upstreamAddress = upstreamCopy,
                                                        sourcePackageName = "Blocked App",
                                                        sourceAppName = "Firewall Sinkhole"
                                                    )
                                                    val responsePayload = result.response
                                                    if (responsePayload != null) {
                                                        synchronized(outStream) {
                                                            val replyPacket = buildDnsReplyPacket(buffer.copyOf(length), ipHeaderLen, responsePayload)
                                                            try {
                                                                outStream.write(replyPacket)
                                                            } catch (_: Exception) {}
                                                        }
                                                    }
                                                    throttledNotificationUpdate()
                                                } catch (e: Exception) {
                                                    Log.w(TAG, "DNS intercept error for $queryDomain", e)
                                                }
                                            }
                                        } else {
                                            // Legacy sinkhole path: block everything from blocked apps
                                            val responsePayload = com.example.dns.DnsPacketParser.buildSinkholeResponse(dnsPayload, dnsPayload.size)
                                            if (responsePayload != null) {
                                                val replyPacket = buildDnsReplyPacket(buffer, ipHeaderLen, responsePayload)
                                                try {
                                                    outStream.write(replyPacket)
                                                } catch (_: Exception) {}
                                            }

                                            connectionLogger.logConnection(
                                                packageName = "Blocked App DNS",
                                                appName = "Firewall Sinkhole",
                                                destinationHost = queryDomain,
                                                port = 53,
                                                protocol = "DNS",
                                                isBlocked = true,
                                                blockReason = "App Blocked",
                                                bytes = length.toLong()
                                            )
                                            throttledNotificationUpdate()
                                        }
                                    }
                                }
                            }
                        } else if (protocol == 6) {
                            // TCP packet (Protocol 6) — drop all traffic from blocked apps
                            val ipHeaderLen = (buffer[0].toInt() and 0x0F) * 4
                            val destPort = if (length >= ipHeaderLen + 4) {
                                ByteBuffer.wrap(buffer, ipHeaderLen + 2, 2).short.toInt() and 0xFFFF
                            } else 80

                            connectionLogger.logConnection(
                                packageName = "Restricted App",
                                appName = "Blocked Traffic",
                                destinationHost = destIp,
                                port = destPort,
                                protocol = "TCP",
                                isBlocked = true,
                                blockReason = "Firewall Rule Applied",
                                bytes = length.toLong()
                            )
                            throttledNotificationUpdate()
                        }
                        // All other protocols (ICMP, non-DNS UDP, etc.) are silently dropped
                    }
                }
            } catch (_: Exception) {
                // Thread terminated
            }
        }, "NetGuardian-PacketProcessor").apply {
            start()
        }
    }

    private fun throttledNotificationUpdate() {
        val now = System.currentTimeMillis()
        if (now - lastNotificationUpdateMs > 4000) {
            lastNotificationUpdateMs = now
            updateForegroundNotification()
        }
    }

    private fun buildDnsReplyPacket(queryPacket: ByteArray, ipHeaderLen: Int, dnsResponsePayload: ByteArray): ByteArray {
        val totalLen = 20 + 8 + dnsResponsePayload.size
        val reply = ByteArray(totalLen)
        val buf = ByteBuffer.wrap(reply)

        // IPv4 Header
        buf.put(0x45.toByte())
        buf.put(0x00.toByte())
        buf.putShort(totalLen.toShort())
        buf.putShort(0.toShort())
        buf.putShort(0x4000.toShort())
        buf.put(64.toByte())
        buf.put(17.toByte())
        buf.putShort(0.toShort()) // Placeholder for checksum

        // Swap Source and Destination IPs
        buf.put(queryPacket.copyOfRange(16, 20))
        buf.put(queryPacket.copyOfRange(12, 16))

        // Calculate and insert IPv4 Header Checksum (RFC 791 / RFC 1071)
        val ipChecksum = calculateIpChecksum(reply, 0, 20)
        reply[10] = ((ipChecksum.toInt() ushr 8) and 0xFF).toByte()
        reply[11] = (ipChecksum.toInt() and 0xFF).toByte()

        // UDP Header
        val srcPort = ByteBuffer.wrap(queryPacket, ipHeaderLen + 2, 2).short
        val destPort = ByteBuffer.wrap(queryPacket, ipHeaderLen, 2).short
        buf.putShort(srcPort)
        buf.putShort(destPort)
        buf.putShort((8 + dnsResponsePayload.size).toShort())
        buf.putShort(0.toShort()) // 0 indicates checksum unused in IPv4 UDP (RFC 768)

        // DNS Payload
        buf.put(dnsResponsePayload)
        return reply
    }

    private fun calculateIpChecksum(header: ByteArray, offset: Int, length: Int): Short {
        var sum = 0
        var i = offset
        while (i < offset + length) {
            val high = header[i].toInt() and 0xFF
            val low = header[i + 1].toInt() and 0xFF
            sum += (high shl 8) or low
            i += 2
        }
        while (sum > 0xFFFF) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        return (sum.inv() and 0xFFFF).toShort()
    }

    private fun updateUnderlyingNetworks() {
        try {
            val physNet = getPhysicalNetwork()
            if (physNet != null) {
                setUnderlyingNetworks(arrayOf(physNet))
            } else {
                setUnderlyingNetworks(null)
            }
        } catch (_: Exception) {}
    }

    private fun getPhysicalNetwork(): android.net.Network? {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
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

    private fun updateForegroundNotification() {
        if (!isRunning) return
        val notification = notificationManager.buildForegroundNotification(stateRepo.state.value)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(FirewallNotificationManager.NOTIFICATION_ID_FOREGROUND, notification)
    }

    private fun stopVpn() {
        userExplicitStop = true
        isRunning = false
        isPaused = false
        pauseJob?.cancel()
        pauseJob = null

        workerThread?.interrupt()
        workerThread = null

        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null

        stateRepo.setStopped()

        serviceScope.launch {
            prefsRepo.setFirewallEnabled(false)
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        val wasRunning = isRunning
        isRunning = false
        pauseJob?.cancel()

        try {
            unregisterReceiver(systemEventReceiver)
        } catch (_: Exception) {}
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {}
        workerThread?.interrupt()
        try {
            vpnInterface?.close()
        } catch (_: Exception) {}

        // If service was destroyed without user's explicit stop, alert the user!
        if (wasRunning && !userExplicitStop) {
            notificationManager.showUnexpectedDisconnectAlert()
            stateRepo.setError("Firewall VPN was terminated unexpectedly")
        }

        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.example.netguardian.ACTION_START"
        const val ACTION_STOP = "com.example.netguardian.ACTION_STOP"
        const val ACTION_PAUSE_10_MIN = "com.example.netguardian.ACTION_PAUSE_10_MIN"
        const val ACTION_RESUME = "com.example.netguardian.ACTION_RESUME"
        const val ACTION_RELOAD_RULES = "com.example.netguardian.ACTION_RELOAD_RULES"
        const val ACTION_QUICK_BLOCK_NON_SYSTEM = "com.example.netguardian.ACTION_QUICK_BLOCK_NON_SYSTEM"
        const val ACTION_QUICK_ALLOW_ALL = "com.example.netguardian.ACTION_QUICK_ALLOW_ALL"
        const val ACTION_QUICK_WIFI_ONLY = "com.example.netguardian.ACTION_QUICK_WIFI_ONLY"
        const val ACTION_QUICK_MOBILE_ONLY = "com.example.netguardian.ACTION_QUICK_MOBILE_ONLY"

        private const val TAG = "NetGuardianVPN"
    }
}
