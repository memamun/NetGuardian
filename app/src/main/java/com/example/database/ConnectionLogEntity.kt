package com.example.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "connection_logs",
    indices = [
        Index("timestamp"),
        Index("packageName"),
        Index("isBlocked")
    ]
)
data class ConnectionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val destinationHost: String,
    val port: Int,
    val protocol: String, // "TCP", "UDP", "DNS"
    val isBlocked: Boolean,
    val blockReason: String, // "Firewall Rule", "Wi-Fi Restriction", "Mobile Data Restriction", "Screen Off Policy", "Tracker/Ad Filter", "Background Policy", "Allowed"
    val timestamp: Long = System.currentTimeMillis(),
    val bytesTransferred: Long = 0
)

data class AppBlockedStat(
    val packageName: String,
    val appName: String,
    val blockedCount: Int
)
