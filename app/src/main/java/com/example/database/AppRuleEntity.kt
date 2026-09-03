package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_rules")
data class AppRuleEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val uid: Int = 0,
    val isBlocked: Boolean = false,
    val blockWifi: Boolean = false,
    val blockMobile: Boolean = false,
    val blockBackground: Boolean = false,
    val blockScreenOff: Boolean = false,
    val blockDeviceIdle: Boolean = false,
    val blockTrackers: Boolean = true,
    val isSystemApp: Boolean = false,
    val customNotes: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)
