package com.example.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "privacy_blocklist",
    indices = [Index(value = ["domain"], unique = true)]
)
data class BlocklistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val category: String, // "AD", "TRACKER", "MALWARE", "CUSTOM"
    val isEnabled: Boolean = true,
    val addedTimestamp: Long = System.currentTimeMillis()
)
