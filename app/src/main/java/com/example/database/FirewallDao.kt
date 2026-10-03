package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FirewallDao {

    // --- App Rules ---
    @Query("SELECT * FROM app_rules ORDER BY appName ASC")
    fun getAllRules(): Flow<List<AppRuleEntity>>

    @Query("SELECT * FROM app_rules")
    suspend fun getAllRulesSync(): List<AppRuleEntity>

    @Query("SELECT * FROM app_rules WHERE packageName = :packageName LIMIT 1")
    fun getRule(packageName: String): Flow<AppRuleEntity?>

    @Query("SELECT * FROM app_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun getRuleSync(packageName: String): AppRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AppRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<AppRuleEntity>)

    @Update
    suspend fun updateRule(rule: AppRuleEntity)

    @Query("DELETE FROM app_rules WHERE packageName = :packageName")
    suspend fun deleteRule(packageName: String)

    @Query("DELETE FROM app_rules")
    suspend fun clearAllRules()

    @Query("UPDATE app_rules SET isBlocked = :blocked WHERE isSystemApp = 0")
    suspend fun setBlockAllNonSystem(blocked: Boolean)

    @Query("UPDATE app_rules SET isBlocked = 0, blockWifi = 0, blockMobile = 0, blockBackground = 0, blockScreenOff = 0, blockDeviceIdle = 0")
    suspend fun resetAllRulesToAllow()

    @Query("UPDATE app_rules SET blockBackground = :blocked")
    suspend fun setBlockAllBackground(blocked: Boolean)

    @Query("UPDATE app_rules SET blockScreenOff = :blocked")
    suspend fun setBlockAllScreenOff(blocked: Boolean)

    @Query("UPDATE app_rules SET blockDeviceIdle = :blocked")
    suspend fun setBlockAllDeviceIdle(blocked: Boolean)

    // --- Connection Logs ---
    @Query("SELECT * FROM connection_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<ConnectionLogEntity>>

    @Query("SELECT * FROM connection_logs WHERE isBlocked = 1 ORDER BY timestamp DESC LIMIT :limit")
    fun getBlockedLogs(limit: Int = 100): Flow<List<ConnectionLogEntity>>

    @Query("SELECT * FROM connection_logs WHERE isBlocked = 0 ORDER BY timestamp DESC LIMIT :limit")
    fun getAllowedLogs(limit: Int = 100): Flow<List<ConnectionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ConnectionLogEntity)

    @Query("DELETE FROM connection_logs")
    suspend fun clearAllLogs()

    @Query("SELECT COUNT(*) FROM connection_logs WHERE isBlocked = 1 AND timestamp >= :startOfDay")
    fun getTodayBlockedCount(startOfDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM connection_logs WHERE isBlocked = 0 AND timestamp >= :startOfDay")
    fun getTodayAllowedCount(startOfDay: Long): Flow<Int>

    @Query("SELECT packageName, appName, COUNT(*) as blockedCount FROM connection_logs WHERE isBlocked = 1 GROUP BY packageName, appName ORDER BY blockedCount DESC LIMIT :limit")
    fun getTopBlockedApps(limit: Int = 5): Flow<List<AppBlockedStat>>

    @Query("SELECT SUM(bytesTransferred) FROM connection_logs WHERE isBlocked = 1")
    fun getTotalBytesBlocked(): Flow<Long?>

    // Keep network history bounded; this is a product retention policy, not an Android requirement.
    @Query("DELETE FROM connection_logs WHERE timestamp < :oldest OR id NOT IN (SELECT id FROM connection_logs ORDER BY timestamp DESC, id DESC LIMIT 10000)")
    suspend fun pruneLogs(oldest: Long)

    // --- Privacy Blocklist ---
    @Query("SELECT * FROM privacy_blocklist ORDER BY category ASC, domain ASC")
    fun getAllBlocklist(): Flow<List<BlocklistEntity>>

    @Query("SELECT * FROM privacy_blocklist")
    suspend fun getAllBlocklistSync(): List<BlocklistEntity>

    @Query("SELECT domain FROM privacy_blocklist WHERE isEnabled = 1")
    suspend fun getActiveBlocklistDomainsSync(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBlocklist(item: BlocklistEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBlocklistItems(items: List<BlocklistEntity>)

    @Query("DELETE FROM privacy_blocklist WHERE id = :id")
    suspend fun deleteBlocklistItem(id: Long)

    @Query("UPDATE privacy_blocklist SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun toggleBlocklistItem(id: Long, isEnabled: Boolean)

    @Query("UPDATE privacy_blocklist SET isEnabled = :isEnabled WHERE category = :category")
    suspend fun toggleCategory(category: String, isEnabled: Boolean)

    @Query("SELECT COUNT(*) FROM privacy_blocklist WHERE category = :category AND isEnabled = 1")
    fun getActiveBlocklistCountByCategory(category: String): Flow<Int>

    @Query("DELETE FROM privacy_blocklist WHERE category = 'CUSTOM'")
    suspend fun clearCustomBlocklist()
}
