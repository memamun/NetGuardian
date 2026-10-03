package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AppRuleEntity::class,
        ConnectionLogEntity::class,
        BlocklistEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun firewallDao(): FirewallDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "netguardian_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun populateDefaultBlocklists(dao: FirewallDao) {
            val defaultBlocklist = listOf(
                // Ads
                BlocklistEntity(domain = "doubleclick.net", category = "AD"),
                BlocklistEntity(domain = "googleadservices.com", category = "AD"),
                BlocklistEntity(domain = "adservice.google.com", category = "AD"),
                BlocklistEntity(domain = "pagead2.googlesyndication.com", category = "AD"),
                BlocklistEntity(domain = "googleads.g.doubleclick.net", category = "AD"),
                BlocklistEntity(domain = "admob.com", category = "AD"),
                BlocklistEntity(domain = "applovin.com", category = "AD"),
                BlocklistEntity(domain = "applvn.com", category = "AD"),
                BlocklistEntity(domain = "unity3d.com", category = "AD"),
                BlocklistEntity(domain = "unityads.unity3d.com", category = "AD"),
                BlocklistEntity(domain = "inner-active.mobi", category = "AD"),
                BlocklistEntity(domain = "moloco.com", category = "AD"),
                BlocklistEntity(domain = "adsmoloco.com", category = "AD"),
                BlocklistEntity(domain = "vungle.com", category = "AD"),
                BlocklistEntity(domain = "chartboost.com", category = "AD"),
                BlocklistEntity(domain = "ironsrc.mobi", category = "AD"),
                BlocklistEntity(domain = "ironsrc.com", category = "AD"),
                BlocklistEntity(domain = "is.com", category = "AD"),
                BlocklistEntity(domain = "fyber.com", category = "AD"),
                BlocklistEntity(domain = "tapjoy.com", category = "AD"),
                BlocklistEntity(domain = "hyprmx.com", category = "AD"),
                BlocklistEntity(domain = "ogury.io", category = "AD"),
                BlocklistEntity(domain = "smaato.net", category = "AD"),
                BlocklistEntity(domain = "supersonicads.com", category = "AD"),
                BlocklistEntity(domain = "bidmachine.io", category = "AD"),
                BlocklistEntity(domain = "startappservice.com", category = "AD"),
                BlocklistEntity(domain = "adtiming.com", category = "AD"),
                BlocklistEntity(domain = "liftoff.io", category = "AD"),
                BlocklistEntity(domain = "pangolin-sdk.com", category = "AD"),
                BlocklistEntity(domain = "nokoprint.com", category = "AD"),
                BlocklistEntity(domain = "mopub.com", category = "AD"),
                BlocklistEntity(domain = "adcolony.com", category = "AD"),
                BlocklistEntity(domain = "inmobi.com", category = "AD"),
                BlocklistEntity(domain = "criteo.com", category = "AD"),
                BlocklistEntity(domain = "criteo.net", category = "AD"),
                BlocklistEntity(domain = "taboola.com", category = "AD"),
                BlocklistEntity(domain = "outbrain.com", category = "AD"),
                BlocklistEntity(domain = "adnxs.com", category = "AD"),
                BlocklistEntity(domain = "rubiconproject.com", category = "AD"),
                BlocklistEntity(domain = "pubmatic.com", category = "AD"),
                BlocklistEntity(domain = "openx.net", category = "AD"),
                BlocklistEntity(domain = "smartadserver.com", category = "AD"),
                BlocklistEntity(domain = "advertising.amazon.com", category = "AD"),
                BlocklistEntity(domain = "aax.amazon-adsystem.com", category = "AD"),
                BlocklistEntity(domain = "ads.tiktok.com", category = "AD"),
                BlocklistEntity(domain = "pangle.io", category = "AD"),
                BlocklistEntity(domain = "mintegral.net", category = "AD"),
                BlocklistEntity(domain = "popads.net", category = "AD"),
                BlocklistEntity(domain = "popcash.net", category = "AD"),

                // Trackers & Telemetry
                BlocklistEntity(domain = "graph.facebook.com", category = "TRACKER"),
                BlocklistEntity(domain = "analytics.google.com", category = "TRACKER"),
                BlocklistEntity(domain = "firebase-settings.crashlytics.com", category = "TRACKER"),
                BlocklistEntity(domain = "app-measurement.com", category = "TRACKER"),
                BlocklistEntity(domain = "telemetry.admob.com", category = "TRACKER"),
                BlocklistEntity(domain = "branch.io", category = "TRACKER"),
                BlocklistEntity(domain = "adjust.com", category = "TRACKER"),
                BlocklistEntity(domain = "appsflyer.com", category = "TRACKER"),
                BlocklistEntity(domain = "mixpanel.com", category = "TRACKER"),
                BlocklistEntity(domain = "flurry.com", category = "TRACKER"),
                BlocklistEntity(domain = "kochava.com", category = "TRACKER"),
                BlocklistEntity(domain = "segment.io", category = "TRACKER"),
                BlocklistEntity(domain = "singular.net", category = "TRACKER"),
                BlocklistEntity(domain = "scorecardresearch.com", category = "TRACKER"),

                // Malware & Threats
                BlocklistEntity(domain = "malware-traffic.com", category = "MALWARE"),
                BlocklistEntity(domain = "phish-bank.com", category = "MALWARE"),
                BlocklistEntity(domain = "crypto-drainer.org", category = "MALWARE"),
                BlocklistEntity(domain = "ransom-c2.net", category = "MALWARE"),
                BlocklistEntity(domain = "trojan-payload.info", category = "MALWARE"),
                BlocklistEntity(domain = "botnet-beacon.xyz", category = "MALWARE")
            )
            dao.insertBlocklistItems(defaultBlocklist)
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    INSTANCE?.let { database ->
                        populateDefaultBlocklists(database.firewallDao())
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                CoroutineScope(Dispatchers.IO).launch {
                    INSTANCE?.let { database ->
                        val dao = database.firewallDao()
                        if (dao.getAllBlocklistSync().isEmpty()) {
                            populateDefaultBlocklists(dao)
                        }
                    }
                }
            }
        }
    }
}
