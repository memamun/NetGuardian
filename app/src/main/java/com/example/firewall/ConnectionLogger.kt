package com.example.firewall

import com.example.database.ConnectionLogEntity
import com.example.database.FirewallDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CancellationException

open class ConnectionLogger(private val dao: FirewallDao? = null) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val logChannel = Channel<ConnectionLogEntity>(capacity = 500)

    init {
        if (dao != null) {
            scope.launch {
                var written = 0
                for (log in logChannel) {
                    try {
                        dao.insertLog(log)
                        if (written++ % 100 == 0) dao.pruneLogs(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // Ignore transient write errors
                    }
                }
            }
        }
    }

    fun close() {
        logChannel.close()
        scope.cancel()
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
