package com.example.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import com.example.database.AppDatabase
import com.example.database.AppRuleEntity
import com.example.database.FirewallDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class AppRepository(
    private val context: Context,
    private val firewallDao: FirewallDao = AppDatabase.getDatabase(context).firewallDao()
) {
    private val packageManager: PackageManager = context.packageManager
    private val iconCache = LruCache<String, Bitmap>(150)

    // Combined Flow: Installed apps with dynamic live rules from Room
    // Icons are NOT loaded here — they are fetched lazily in Compose to avoid
    // blocking the main thread and to prevent Bitmap from preventing recomposition skips.
    val appsFlow: Flow<List<AppItem>> = firewallDao.getAllRules().combine(
        kotlinx.coroutines.flow.flow {
            emit(loadInstalledPackages())
        }
    ) { rules, rawApps ->
        val ruleMap = rules.associateBy { it.packageName }
        rawApps.map { raw ->
            val rule = ruleMap[raw.packageName] ?: AppRuleEntity(
                packageName = raw.packageName,
                appName = raw.appName,
                uid = raw.uid,
                isSystemApp = raw.isSystemApp,
                isBlocked = false,
                blockWifi = false,
                blockMobile = false,
                blockBackground = false,
                blockScreenOff = false,
                blockTrackers = true
            )
            AppItem(
                packageName = raw.packageName,
                appName = raw.appName,
                uid = raw.uid,
                isSystemApp = raw.isSystemApp,
                hasInternetPermission = raw.hasInternetPermission,
                versionName = raw.versionName,
                rule = rule
            )
        }.sortedWith(compareBy({ !it.rule.isBlocked }, { it.isSystemApp }, { it.appName.lowercase() }))
    }

    data class RawAppInfo(
        val packageName: String,
        val appName: String,
        val uid: Int,
        val isSystemApp: Boolean,
        val hasInternetPermission: Boolean,
        val versionName: String
    )

    private suspend fun loadInstalledPackages(): List<RawAppInfo> = withContext(Dispatchers.IO) {
        val packages = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }
        } catch (_: Exception) {
            emptyList<PackageInfo>()
        }

        val myPackageName = context.packageName
        val rawList = packages
            .filter { it.packageName != myPackageName }
            .map { pkg ->
                val appInfo = pkg.applicationInfo ?: return@map null
                val appName = try {
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    pkg.packageName
                }
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val hasInternet = pkg.requestedPermissions?.contains(android.Manifest.permission.INTERNET) == true

                RawAppInfo(
                    packageName = pkg.packageName,
                    appName = appName,
                    uid = appInfo.uid,
                    isSystemApp = isSystem,
                    hasInternetPermission = hasInternet,
                    versionName = pkg.versionName ?: "1.0"
                )
            }.filterNotNull()

        // Sync missing entities into database
        val existingRules = firewallDao.getAllRulesSync().associateBy { it.packageName }
        val newEntities = rawList.filter { !existingRules.containsKey(it.packageName) }.map { raw ->
            AppRuleEntity(
                packageName = raw.packageName,
                appName = raw.appName,
                uid = raw.uid,
                isSystemApp = raw.isSystemApp,
                isBlocked = false,
                blockWifi = false,
                blockMobile = false,
                blockBackground = false,
                blockScreenOff = false,
                blockTrackers = true
            )
        }
        if (newEntities.isNotEmpty()) {
            firewallDao.insertRules(newEntities)
        }

        rawList
    }

    fun getAppIcon(packageName: String): Bitmap? {
        iconCache.get(packageName)?.let { return it }
        return try {
            val drawable = packageManager.getApplicationIcon(packageName)
            val bitmap = drawableToBitmap(drawable)
            bitmap?.let { iconCache.put(packageName, it) }
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 72
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 72
        return try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    suspend fun updateRule(rule: AppRuleEntity) = withContext(Dispatchers.IO) {
        firewallDao.insertRule(rule.copy(lastUpdated = System.currentTimeMillis()))
    }

    suspend fun toggleBlock(rule: AppRuleEntity) = withContext(Dispatchers.IO) {
        val updated = rule.copy(
            isBlocked = !rule.isBlocked,
            lastUpdated = System.currentTimeMillis()
        )
        firewallDao.insertRule(updated)
    }

    suspend fun toggleWifi(rule: AppRuleEntity) = withContext(Dispatchers.IO) {
        val updated = rule.copy(
            blockWifi = !rule.blockWifi,
            lastUpdated = System.currentTimeMillis()
        )
        firewallDao.insertRule(updated)
    }

    suspend fun toggleMobile(rule: AppRuleEntity) = withContext(Dispatchers.IO) {
        val updated = rule.copy(
            blockMobile = !rule.blockMobile,
            lastUpdated = System.currentTimeMillis()
        )
        firewallDao.insertRule(updated)
    }

    suspend fun toggleBackground(rule: AppRuleEntity) = withContext(Dispatchers.IO) {
        val updated = rule.copy(
            blockBackground = !rule.blockBackground,
            lastUpdated = System.currentTimeMillis()
        )
        firewallDao.insertRule(updated)
    }

    suspend fun toggleScreenOff(rule: AppRuleEntity) = withContext(Dispatchers.IO) {
        val updated = rule.copy(
            blockScreenOff = !rule.blockScreenOff,
            lastUpdated = System.currentTimeMillis()
        )
        firewallDao.insertRule(updated)
    }

    suspend fun toggleDeviceIdle(rule: AppRuleEntity) = withContext(Dispatchers.IO) {
        val updated = rule.copy(
            blockDeviceIdle = !rule.blockDeviceIdle,
            lastUpdated = System.currentTimeMillis()
        )
        firewallDao.insertRule(updated)
    }

    suspend fun toggleTrackers(rule: AppRuleEntity) = withContext(Dispatchers.IO) {
        val updated = rule.copy(
            blockTrackers = !rule.blockTrackers,
            lastUpdated = System.currentTimeMillis()
        )
        firewallDao.insertRule(updated)
    }

    suspend fun blockAllNonSystem(block: Boolean) = withContext(Dispatchers.IO) {
        firewallDao.setBlockAllNonSystem(block)
    }

    suspend fun allowAllApps() = withContext(Dispatchers.IO) {
        firewallDao.resetAllRulesToAllow()
    }

    suspend fun setBlockAllBackground(block: Boolean) = withContext(Dispatchers.IO) {
        firewallDao.setBlockAllBackground(block)
    }

    suspend fun setBlockAllScreenOff(block: Boolean) = withContext(Dispatchers.IO) {
        firewallDao.setBlockAllScreenOff(block)
    }

    suspend fun setBlockAllDeviceIdle(block: Boolean) = withContext(Dispatchers.IO) {
        firewallDao.setBlockAllDeviceIdle(block)
    }
}
