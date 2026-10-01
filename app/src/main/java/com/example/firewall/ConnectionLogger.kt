package com.example.firewall

import com.example.database.ConnectionLogEntity
import com.example.database.FirewallDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

open class ConnectionLogger(private val dao: FirewallDao? = null) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val logChannel = Channel<ConnectionLogEntity>(capacity = 500)

    init {
        if (dao != null) {
            scope.launch {
                for (log in logChannel) {
                    try {
                        dao.insertLog(log)
                    } catch (_: Exception) {
                        // Ignore transient write errors
                    }
                }
            }
        }
    }

    open fun logConnection(
        packageName: String,
        appName: String,
        destinationHost: String,
        port: Int,
        protocol: String,
        isBlocked: Boolean,
        blockReason: String,
        bytes: Long = 0
    ) {
        val entry = ConnectionLogEntity(
            packageName = packageName,
            appName = appName,
            destinationHost = destinationHost,
            port = port,
            protocol = protocol,
            isBlocked = isBlocked,
            blockReason = blockReason,
            timestamp = System.currentTimeMillis(),
            bytesTransferred = bytes
        )
        logChannel.trySend(entry)
    }
}
