package com.example.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirewallStorageTest {
    @Test
    fun `reset preserves package identity and log pruning removes expired history`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        try {
            val dao = db.firewallDao()
            dao.insertRule(AppRuleEntity("android", "System", uid = 1000, isSystemApp = true, isBlocked = true))
            dao.resetAllRulesToAllow()
            val rule = dao.getRuleSync("android")!!
            assertFalse(rule.isBlocked)
            assertTrue(rule.isSystemApp)
            assertEquals(1000, rule.uid)
            dao.insertLog(ConnectionLogEntity(packageName = "android", appName = "System", destinationHost = "old.example", port = 53, protocol = "DNS", isBlocked = true, blockReason = "Test", timestamp = 1))
            dao.insertLog(ConnectionLogEntity(packageName = "android", appName = "System", destinationHost = "new.example", port = 53, protocol = "DNS", isBlocked = true, blockReason = "Test", timestamp = 100))
            dao.pruneLogs(50)
            assertEquals(listOf("new.example"), dao.getRecentLogs().first().map { it.destinationHost })
        } finally {
            db.close()
        }
    }
}
